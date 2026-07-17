package com.qiujie.controller;

import com.qiujie.dto.Response;
import com.qiujie.entity.Staff;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.annotation.OperationLog;
import com.qiujie.service.LoginService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录注册接口
 *
 * @Author : qiujie
 * @Date : 2022/1/30
 */
@RestController
public class LoginController {

    @Autowired
    private LoginService loginService;

    @OperationLog(module = "登录", action = "用户登录")
    @PostMapping("/login/{validateCode}")
    public ResponseDTO login(@RequestBody Staff staff, @PathVariable String validateCode) {
        return this.loginService.login(staff, validateCode);
    }

    @GetMapping("/validate/code")
    public void getValidateCode(HttpServletResponse response) throws IOException {
        this.loginService.getValidateCode(response);
    }
}
