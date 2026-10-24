# Permissions justification / تبرير الصلاحيات

| Permission | Why (English — paste into Play Console where asked) | السبب (عربي) |
|---|---|---|
| RECORD_AUDIO (Microphone) | Push-to-talk: the microphone is used only while the user holds the talk button, to stream voice to nearby phones. Nothing is recorded or stored. | الميكروفون يشتغل بس وأنت ضاغط زر «اضغط وتكلّم» عشان يوصل صوتك للجوالات القريبة. ما يتسجل ولا ينحفظ شي. |
| ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION | 1) Required by Android to discover nearby devices over Wi-Fi Direct and Bluetooth on Android 8–12. 2) Sending the user's coordinates in an SOS alert or "share my location" message, only when the user taps it. 3) Tagging signal-meter readings so the user can find the best spot. No background location permission is requested. | ١) أندرويد يطلبها عشان يلقى الأجهزة القريبة بالواي فاي المباشر والبلوتوث. ٢) لإرسال موقعك في الاستغاثة أو «شارك موقعي» لما تضغط بس. ٣) لتسجيل قراءات الإشارة مع المكان. |
| NEARBY_WIFI_DEVICES, BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE | Core function: find and connect to nearby phones running Nidaa over Wi-Fi Direct and Bluetooth to exchange messages and voice without internet. | أساس التطبيق: يلقى الجوالات القريبة اللي فيها «نِداء» ويتصل فيها بالواي فاي المباشر والبلوتوث. |
| ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, CHANGE_WIFI_MULTICAST_STATE, ACCESS_NETWORK_STATE, CHANGE_NETWORK_STATE, INTERNET | Create the Wi-Fi Direct group and open local TCP/UDP sockets between nearby phones (hotspot / same Wi-Fi). INTERNET is needed by Android for any socket; the app never contacts an internet server. | لفتح اتصال محلي بين الجوالات. صلاحية INTERNET يطلبها أندرويد لأي اتصال محلي، والتطبيق ما يكلم أي سيرفر. |
| FOREGROUND_SERVICE, FOREGROUND_SERVICE_CONNECTED_DEVICE | Keeps the connection with nearby phones alive when the screen is off so the user keeps receiving relayed messages, SOS alerts and voice. Shown as a persistent notification with a Stop button; started only when the user taps «تشغيل». | يخلي الاتصال شغّال والشاشة مطفية عشان توصلك الرسائل والاستغاثة. يطلع إشعار ثابت فيه زر «إيقاف». |
| FOREGROUND_SERVICE_LOCATION | While the mesh runs with the screen off, the current location is kept up to date so an SOS sent from the lock screen/notification contains accurate coordinates. Started from the foreground only. | عشان لو أرسلت استغاثة والشاشة مطفية يكون موقعك محدّث. |
| POST_NOTIFICATIONS | Notify the user of incoming messages, SOS alerts and someone speaking, and show the running-service notification. | إشعارات الرسائل والاستغاثة والصوت. |
| WAKE_LOCK | Keep the CPU awake while relaying for nearby phones with the screen off. | يخلي الجوال يمرّر الرسائل والشاشة مطفية. |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | Emergency/off-grid communication: battery optimization kills the connection and SOS alerts are lost. The user is asked once and can decline. | توفير البطارية يقطع الاتصال وتضيع الاستغاثة. ينسأل المستخدم مرة وحدة ويقدر يرفض. |
| READ_PHONE_STATE | Show the mobile network type (4G/5G) in the signal meter. Optional. | عشان يعرض نوع الشبكة في مقياس الإشارة. اختيارية. |
| VIBRATE | Vibrate on incoming SOS. | اهتزاز عند وصول استغاثة. |

## Foreground service declaration (Play Console ← App content ← Foreground service permissions)
- **Connected device** — Task: "Maintain Wi-Fi Direct / Bluetooth / local socket connections with nearby phones to receive and relay messages, SOS and voice while the screen is off." User-initiated: Yes (tap «تشغيل»). Impact if deferred/interrupted: messages and SOS alerts from nearby people are lost.
- **Location** — Task: "Keep the location fresh for SOS alerts sent while the screen is off." User-initiated: Yes.
- Play asks for a **short video** (YouTube/Drive link) showing the feature: record the phone pressing «تشغيل», the notification «نِداء شغّال», locking the screen, and a message/SOS arriving from a second phone.
