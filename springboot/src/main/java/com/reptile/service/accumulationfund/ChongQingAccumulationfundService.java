package com.reptile.service.accumulationfund;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.reptile.model.AccumulationFlows;
import com.reptile.util.MyCYDMDemo;
import com.reptile.util.PushState;
import com.reptile.util.Resttemplate;
import com.reptile.util.WebClientFactory;
import com.reptile.util.application;

@Service
public class ChongQingAccumulationfundService {
	@Autowired 
	private application applicat;
    private Logger logger = LoggerFactory.getLogger(GuiYangAccumulationfundService.class);
    
    public Map<String, Object> loadImageCode(HttpServletRequest request) {
        logger.warn("获取重庆公积金图片验证码");
        Map<String, Object> map = new HashMap<>();
        Map<String, String> datamap = new HashMap<>();
        String path = request.getServletContext().getRealPath("ImageCode");
        HttpSession session = request.getSession();

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        WebClient webClient = new WebClientFactory().getWebClient();

        HtmlPage page = null;
        try {
            page = webClient.getPage("http://www.cqgjj.cn//Member/UserLogin.aspx?type=null");
            HtmlImage rand = (HtmlImage) page.getElementById("imgCode");
            BufferedImage read = rand.getImageReader().read(0);
            String fileName = "guiyang" + System.currentTimeMillis() + ".png";
            ImageIO.write(read, "png", new File(file, fileName));
            datamap.put("imagePath", request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/ImageCode/" + fileName);
            map.put("errorCode", "0000");
            map.put("errorInfo", "加载验证码成功");
            map.put("data", datamap);
            session.setAttribute("htmlWebClient-chongqing", webClient);
            session.setAttribute("htmlPage-chongqing", page);
        } catch (IOException e) {
            logger.warn("重庆住房公积金 ", e);
            map.put("errorCode", "0001");
            map.put("errorInfo", "当前网络繁忙，请刷新后重试");
            e.printStackTrace();

        }
        return map;
    }
    public Map<String, Object> getDeatilMes(HttpServletRequest request, String userCard, String password, String imageCode,String idCardNum) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> loansdata = new HashMap<>();
        List<Object> beanList=new ArrayList<Object>();
        Date date=new Date();
        List<String> alert=new ArrayList<>();
        CollectingAlertHandler alertHandler=new CollectingAlertHandler(alert);
        HttpSession session = request.getSession();
        Object htmlWebClient = session.getAttribute("htmlWebClient-chongqing");
        Object htmlPage = session.getAttribute("htmlPage-chongqing");

        if (htmlWebClient != null && htmlPage != null) {
            HtmlPage loginPage = (HtmlPage) htmlPage;
            WebClient webClient = (WebClient) htmlWebClient;
            
            webClient.setAlertHandler(alertHandler);
            try {
//            	HtmlPage loginPage = page.getElementById("page").click();
            	loginPage.getElementById("txt_loginname").setAttribute("value",userCard);
            	loginPage.getElementById("txt_pwd").setAttribute("value",password);
            	loginPage.getElementById("txt_code").setAttribute("value",imageCode);
                HtmlPage infoPage = loginPage.getElementById("loginBtn").click();
                Thread.sleep(2000);
                String numMsg = infoPage.getElementsByTagName("div").get(8).getTextContent();//账号密码错误提示
                String imgMsg = infoPage.getElementsByTagName("div").get(9).getTextContent();//验证码错误提示   要是拿不到就在整个页面判断
                if(imgMsg.indexOf("验证码")!=-1){               	
                	map.put("errorInfo", "验证码输入错误，请重新获取验证码");                	
                	logger.warn("验证码输入错误，请重新获取验证码");
                    map.put("errorCode", "0005");
                    return map;
                } 
                if(numMsg.indexOf("密码")!=-1||numMsg.indexOf("登录账号")!=-1){               	
                	map.put("errorInfo", "用户名或密码错误，请重新获取验证码后登陆");                	
                	logger.warn("用户名或密码错误，请重新获取验证码后登陆");
                    map.put("errorCode", "0005");
                    return map;
                }
                /*
                 * 获取基本信息
                 */
                HtmlPage baseInfo = webClient.getPage("http://www.cqgjj.cn/Member/gr/gjjyecx.aspx");//个人基本信息
                Thread.sleep(2000);
                if(baseInfo.asText().indexOf("请输入验证码后继续")!=-1){
                	String url = "http://www.cqgjj.cn/Member/gr/gjjyecx.aspx";
                	getImgCode(webClient,request,url);
                	baseInfo = webClient.getPage("http://www.cqgjj.cn/Member/gr/gjjyecx.aspx");//个人明细信息
                    Thread.sleep(2000);
                }
                if(baseInfo.asText().indexOf("公积金余额查询")==-1){
                	logger.warn("重庆住房公积金基本信息获取失败");
                    map.put("errorCode", "0001");
                    map.put("errorInfo", "当前网络繁忙，请刷新后重试");
                }
                session.setAttribute("htmlWebClient-chongqing", webClient);
                session.setAttribute("htmlPage-chongqing", baseInfo);
                dataMap = getBaseInfos(request);
                
                /*
                 * 	获取明细信息
                 */
                HtmlPage Infos = webClient.getPage("http://www.cqgjj.cn/Member/gr/gjjmxcx.aspx");//个人明细信息
                Thread.sleep(2000);
                if(baseInfo.asText().indexOf("请输入验证码后继续")!=-1){//操作频繁出现再次验证验证码页面
                	String url = "http://www.cqgjj.cn/Member/gr/gjjmxcx.aspx";
                	getImgCode(webClient,request,url);
                	Infos = webClient.getPage("http://www.cqgjj.cn/Member/gr/gjjmxcx.aspx");//个人明细信息
                    Thread.sleep(2000);
                }
                String pageNum = Infos.getElementById("ContentPlaceHolder1_PageNavigator1_LblPageCount").getTextContent();
                AccumulationFlows flows = new AccumulationFlows();
                data = (Map<String, Object>) dataMap.get("basicInfos");
                String companyName = (String) data.get("companyName");
                for(int i=1;i<=Integer.parseInt(pageNum);i++){      
                	HtmlTable tableInfo = (HtmlTable) Infos.getElementsByTagName("table").get(0);
                	DomNodeList trList = tableInfo.getElementsByTagName("tr");
	                for(int tr=1;tr<trList.size();tr++){
	                	String type1 = tableInfo.getCellAt(tr,1).asText();
	    				if(type1.indexOf("汇缴")==-1&&type1.indexOf("补缴")==-1){
	    					continue;
	    				}
	    				if(type1.indexOf("汇缴")!=-1){
	    					type1="汇缴";
	    				}
	    				if(type1.indexOf("补缴")!=-1){
	    					type1="补缴";
	    				}
	    				String time = tableInfo.getCellAt(tr,0).asText().substring(0, 7).replace("-", "");
	    				String bizDesc = type1+time+"公积金";//业务描述
	    				flows.setAmount(tableInfo.getCellAt(tr,3).asText());//操作金额
	    				flows.setBizDesc(bizDesc);
	    				flows.setOperatorDate(tableInfo.getCellAt(tr,0).asText());//操作时间
	    				flows.setPayMonth(time);//缴费月份
	    				flows.setType(type1);//操作类型
	    				flows.setCompanyName(companyName);//公司名
	    				JSONObject jsonObject = JSONObject.fromObject(flows);
		    			String jsonBean = jsonObject.toString();
		    			System.out.println(jsonBean);
	    				beanList.add(jsonBean);
	    			}
	                if(i<Integer.parseInt(pageNum)){//下一页
	                	Infos = Infos.getElementById("ContentPlaceHolder1_PageNavigator1_LnkBtnNext").click();
	                }
                }
    			dataMap.put("flows", beanList);
                /*
                 * 贷款信息
                 */
    			HtmlPage loansInfos = webClient.getPage("http://www.cqgjj.cn/Member/gr/gjjgrdk.aspx");//个人贷款信息
                Thread.sleep(2000);
                if(loansInfos.asText().indexOf("请输入验证码后继续")!=-1){
                	String url = "http://www.cqgjj.cn/Member/gr/gjjgrdk.aspx";
                	getImgCode(webClient,request,url);
                	loansInfos = webClient.getPage("http://www.cqgjj.cn/Member/gr/gjjgrdk.aspx");//个人贷款信息
                    Thread.sleep(2000);
                }
                if(loansInfos.asText().indexOf("没有查询到贷款信息")!=-1){
                	dataMap.put("loans", null);   
                }else {
                	loansdata.put("loanAccNo", "");
                	loansdata.put("loanLimit", "");
                	loansdata.put("openDate", "");
                	loansdata.put("loanAmount", "");
                	loansdata.put("lastPaymentDate", "");
                	loansdata.put("status", "");
                	loansdata.put("loanBalance", "");
                	loansdata.put("paymentMethod", "");
                	dataMap.put("loans", loansdata);   
                }
            }catch (Exception e) {
                logger.warn("重庆住房公积金获取失败",e);
                e.printStackTrace();
                map.put("errorCode", "0001");
                map.put("errorInfo", "当前网络繁忙，请刷新后重试");
                return map;
            }finally {
                webClient.close();
            }
        } else {
            logger.warn("重庆住房公积金登录过程中出错 ");
            map.put("errorCode", "0001");
            map.put("errorInfo", "非法操作！请确认验证码是否正确！");
            return map;
        }
        SimpleDateFormat sdf =  new SimpleDateFormat( "yyyyMMdd hh:mm:ss" );
		String today = sdf.format(date);
        map.put("insertTime", today);
        map.put("cityName", "重庆市");
        map.put("city", "016");
        map.put("userId", idCardNum);
        map.put("data", dataMap);   
        
        Resttemplate resttemplate=new Resttemplate();
        map = resttemplate.SendMessage(map, applicat.getSendip()+"/HSDC/person/accumulationFund");
        if(map!=null&&"0000".equals(map.get("errorCode").toString())){
	    	PushState.state(idCardNum, "accumulationFund",300);
	    	map.put("errorInfo","推送成功");
	    	map.put("errorCode","0000");
          
        }else{
        	//--------------------数据中心推送状态----------------------
        	PushState.state(idCardNum, "accumulationFund",200);
        	//---------------------数据中心推送状态----------------------
        	
            map.put("errorInfo","推送失败");
            map.put("errorCode","0001");
        	
        }
        return map;
    }
    
    /*
     * 基本信息
     */  
    public Map<String, Object> getBaseInfos(HttpServletRequest request){
    	Map<String, Object> dataMap = new HashMap<>();
    	Map<String, Object> data = new HashMap<>();
        HttpSession session = request.getSession();
    	Object htmlWebClient = session.getAttribute("htmlWebClient-chongqing");
        Object htmlPage = session.getAttribute("htmlPage-chongqing");

        if (htmlWebClient != null && htmlPage != null) {
            HtmlPage page = (HtmlPage) htmlPage;
            WebClient webClient = (WebClient) htmlWebClient;
            data.put("companyName", page.getElementById("ContentPlaceHolder1_lb_dwmc").getTextContent().trim());
            data.put("name", page.getElementById("ContentPlaceHolder1_lb_name").getTextContent().trim());
            data.put("userCard", page.getElementById("ContentPlaceHolder1_Label1").getTextContent().trim());
        	data.put("personDepositAmount", page.getElementById("ContentPlaceHolder1_lb_grjje").getTextContent().trim());//个人缴费金额
        	data.put("personFundAccount", page.getElementById("ContentPlaceHolder1_lb_grjjjzh").getTextContent().trim());//个人公积金账号
        	data.put("baseDeposit", "");//缴费基数
        	String personFundCard = page.getElementById("ContentPlaceHolder1_lb_grxh").getTextContent().trim();//个人公积金卡号
        	String companyDepositAmount = page.getElementById("ContentPlaceHolder1_lb_dwyje").getTextContent().trim();//公司缴费金额
        	data.put("personFundCard", personFundCard);//个人公积金卡号
        	data.put("companyRatio", "");//公司缴费比例//////
        	data.put("personRatio", "");//个人缴费比例/////////////
        	data.put("companyFundAccount", "");//公司公积金账号
        	data.put("companyDepositAmount", companyDepositAmount);//公司缴费金额
        	data.put("lastDepositDate", "");//最后缴费日期
        	data.put("balance", page.getElementById("ContentPlaceHolder1_lb_dqye").getTextContent().trim());//余额
        	data.put("status", page.getElementById("ContentPlaceHolder1_lb_dwzcs").getTextContent().trim());//状态        				
        	dataMap.put("basicInfos", data);
            
        }else{
        	logger.warn("重庆住房公积金登录过程中出错 ");
        	dataMap.put("errorCode", "0001");
        	dataMap.put("errorInfo", "非法操作！");
            return dataMap;
        }
        return dataMap;
    }
    /*
     * 网页提醒操作频繁，重新获取验证码
     */
    public void getImgCode(WebClient webClient,HttpServletRequest request,String url) throws Exception{
    	 HttpSession session = request.getSession();
    	 HtmlPage Infos = webClient.getPage(url);
         Thread.sleep(2000);
         HtmlImage imgCode = (HtmlImage) Infos.getElementById("imgCode");
         BufferedImage read = imgCode.getImageReader().read(0);
         ImageIO.write(read, "png", new File("C://aa.png"));
         Map<String, Object> code = MyCYDMDemo.getCode("C://aa.png");
         String vecCode = code.get("strResult").toString();
         Infos.getElementById("rand").setAttribute("value", vecCode);
         HtmlPage ImgCodePage = Infos.getElementById("ContentPlaceHolder1_Button_Select").click();
    }
    /*
     * 
     */
}
