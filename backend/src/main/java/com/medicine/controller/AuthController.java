package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.dto.LoginDTO;
import com.medicine.dto.HumanChallengeVO;
import com.medicine.dto.HumanVerifyDTO;
import com.medicine.dto.HumanVerifyVO;
import com.medicine.dto.RegisterDTO;
import com.medicine.entity.SysUser;
import com.medicine.service.SysLogService;
import com.medicine.service.SysUserService;
import com.medicine.service.HumanVerificationService;
import com.medicine.util.AccessControl;
import com.medicine.vo.LoginVO;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private HumanVerificationService humanVerificationService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private AccessControl accessControl;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        humanVerificationService.consume(dto.getHumanToken(), request.getRemoteAddr());
        LoginVO vo = sysUserService.login(dto);
        Long userId = vo.getUserId();
        sysLogService.log(userId, "用户登录", "用户登录: " + vo.getUsername(), request.getRemoteAddr());
        return Result.success(vo);
    }

    @GetMapping("/human-challenge")
    public Result<HumanChallengeVO> humanChallenge(HttpServletRequest request) {
        return Result.success(humanVerificationService.createChallenge(request.getRemoteAddr()));
    }

    @PostMapping("/human-challenge/verify")
    public Result<HumanVerifyVO> verifyHuman(@Valid @RequestBody HumanVerifyDTO dto, HttpServletRequest request) {
        return Result.success(humanVerificationService.verify(dto.getChallengeId(), request.getRemoteAddr()));
    }

    @PostMapping("/register")
    public Result<SysUser> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        SysUser user = sysUserService.register(dto);
        user.setPassword(null);
        return Result.success(user);
    }
}
