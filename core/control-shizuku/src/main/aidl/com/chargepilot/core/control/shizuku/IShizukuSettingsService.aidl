package com.chargepilot.core.control.shizuku;

interface IShizukuSettingsService {
    void destroy() = 16777114;
    String readSystemSetting(String key) = 1;
    void writeSystemSetting(String key, String value) = 2;
    String identity() = 3;
}
