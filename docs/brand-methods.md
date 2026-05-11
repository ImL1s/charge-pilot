# Brand Charging Methods

Last researched: 2026-05-11.

Charge Pilot separates **official guidance** from **direct control**. A brand entry may open the manufacturer settings screen even when Android does not expose a safe app toggle.

| Brand | Current official method | Direct app control | Evidence |
|---|---|---|---|
| Samsung Galaxy S / Z | Gaming Hub -> More -> Game Booster settings -> Pause USB PD charging when gaming. Requires PD/PPS charger, battery 20%+, and an active game. On the verified S24U, Samsung's hidden Game Booster activities require signature permissions (`READ_SEARCH_INDEXABLES` / `LAUNCH_SETTING_GAMES`), so the public button opens Gaming Hub and the app shows the manual in-app guide. | Full flavor only on verified `settings system pass_through` devices through Shizuku; normal app `WRITE_SETTINGS` is blocked on S24U. | Samsung support: https://www.samsung.com/uk/support/mobile-devices/what-is-the-pause-usb-power-delivery-feature/ |
| Google Pixel | Settings -> Battery -> Battery health -> Charging optimization -> Adaptive Charging or Limit to 80%. | None. Official guidance only. | Pixel Help: https://support.google.com/pixelphone/answer/6090612?hl=en |
| OnePlus | OxygenOS 16 supported models add bypass charging for gaming, live streaming, video, navigation, and other high-load use. | None until a model-specific writable key is verified. | OnePlus Community OTA notes: https://community.oneplus.com/thread/2039598900649656328 |
| Xiaomi / Redmi / POCO | Battery protection modes can limit charging to 80% or use optimized/nighttime charging on supported models. POCO F7 is explicitly negative for bypass charging. | None until a model-specific writable key is verified. | Xiaomi FAQ: https://www.mi.com/ph/support/faq/details/KA-491339/ and POCO F7 FAQ: https://www.mi.com/my/support/faq/details/KA-618354/ |
| HONOR | Settings -> Battery -> More battery settings -> Smart Charge / Smart Battery Capacity. | None. Official guidance only. | HONOR support: https://www.honor.com/uk/support/content/en-us15840738/ |
| Huawei | Settings -> Battery -> More battery settings -> Smart Charge or Custom limit. | None. Official guidance only. | Huawei support: https://consumer.huawei.com/en/support/content/en-us15871210/ |

Do not add `SHIZUKU_RPC`, `WRITE_SETTINGS_KEY`, or `ROOT_SHELL` to a capability unless the registry also contains a verified `settingsKey` and a rollback path.
