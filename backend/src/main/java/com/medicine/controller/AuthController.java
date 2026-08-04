package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.dto.LoginDTO;
import com.medicine.dto.RegisterDTO;
import com.medicine.entity.SysUser;
import com.medicine.service.DashboardService;
import com.medicine.service.StockDeductionService;
import com.medicine.service.StockService;
import com.medicine.service.SysLogService;
import com.medicine.service.SysUserService;
import com.medicine.vo.LoginVO;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockDeductionService stockDeductionService;

    @Autowired
    private SysLogService sysLogService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        LoginVO vo = sysUserService.login(dto);
        Long userId = vo.getUserId();
        String role = vo.getRole();
        sysLogService.log(userId, "用户登录", "用户登录: " + vo.getUsername(), request.getRemoteAddr());
        new Thread(() -> {
            try {
                if ("ADMIN".equals(role)) {
                    stockDeductionService.deductAllWithPeriod();
                } else if ("ELDER".equals(role)) {
                    stockDeductionService.deductAllByUserIdWithPeriod(userId);
                }
            } catch (Exception e) {
                System.err.println("异步库存扣减异常: " + e.getMessage());
            }
        }).start();
        return Result.success(vo);
    }

    @PostMapping("/register")
    public Result<SysUser> register(@Valid @RequestBody RegisterDTO dto) {
        SysUser user = sysUserService.register(dto);
        user.setPassword(null);
        return Result.success(user);
    }
}
