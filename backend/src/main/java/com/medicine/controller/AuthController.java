package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.dto.LoginDTO;
import com.medicine.dto.PortalRegisterDTO;
import com.medicine.dto.HumanChallengeVO;
import com.medicine.dto.HumanVerifyDTO;
import com.medicine.dto.HumanVerifyVO;
import com.medicine.dto.RegisterDTO;
import com.medicine.entity.SysUser;
import com.medicine.service.SysLogService;
import com.medicine.service.SysUserService;
import com.medicine.service.HumanVerificationService;
import com.medicine.service.PortalRegistrationService;
import com.medicine.util.AccessControl;
import com.medicine.util.ClientAddressResolver;
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

    @Autowired
    private ClientAddressResolver clientAddressResolver;

    @Autowired
    private PortalRegistrationService portalRegistrationService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String clientAddress = clientAddressResolver.resolve(request);
        humanVerificationService.consume(dto.getHumanToken(), clientAddress);
        LoginVO vo = sysUserService.login(dto);
        Long userId = vo.getUserId();
        sysLogService.log(userId, "用户登录", "用户登录: " + vo.getUsername(), clientAddress);
        return Result.success(vo);
    }

    @GetMapping("/human-challenge")
    public Result<HumanChallengeVO> humanChallenge(HttpServletRequest request) {
        return Result.success(humanVerificationService.createChallenge(clientAddressResolver.resolve(request)));
    }

    @PostMapping("/human-challenge/verify")
    public Result<HumanVerifyVO> verifyHuman(@Valid @RequestBody HumanVerifyDTO dto, HttpServletRequest request) {
        return Result.success(humanVerificationService.verify(
                dto.getChallengeId(), clientAddressResolver.resolve(request)));
    }

    @PostMapping("/portal-register")
    public Result<SysUser> portalRegister(@Valid @RequestBody PortalRegisterDTO dto,
                                          HttpServletRequest request) {
        return Result.success(portalRegistrationService.register(
                dto, clientAddressResolver.resolve(request)));
    }

    @PostMapping("/register")
    public Result<SysUser> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        accessControl.requireSystemAdmin(request);
        if ("ADMIN".equals(dto.getRole())) return Result.error("平台管理员账号只能通过受控数据库迁移创建");
        SysUser user = sysUserService.register(dto);
        user.setPassword(null);
        return Result.success(user);
    }
}
