package com.reptile.contorller;

import io.swagger.annotations.ApiOperation;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.reptile.service.JingDongService;

/**
 * 
 * @ClassName: JingDongController  
 * @Description: TODO (京东)
 * @author: xuesongcui
 * @date 2017年12月29日  
 *
 */
@Controller
@RequestMapping("JingDong")
public class JingDongController {
	@Autowired
    private JingDongService jingDongService;
	
	@ApiOperation(value = "京东",notes = "参数：用户名、密码、验证码")
	@ResponseBody
	@RequestMapping(value = "doGetDetail", method = RequestMethod.POST)
	public  Map<String,Object> doGetDetail(HttpServletRequest request,@RequestParam("userName")String userName,@RequestParam("passWord")String passWord,
				@RequestParam("idCard")String idCard){
		return jingDongService.doGetDetail(request,userName.trim(),passWord.trim(),idCard.trim());
		
	}
}
