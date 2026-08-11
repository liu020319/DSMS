package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.SysConfig;

public interface SysConfigService extends IService<SysConfig> {
    String getConfigValue(String configKey);
}
