# Data safety form — answers (Google Play Console ← App content ← Data safety)

> الخلاصة: التطبيق ما عنده سيرفرات ولا حسابات. البيانات تنتقل **مباشرة من جوال لجوال قريب** بطلب المستخدم، وتنحفظ في الجوال بس.
> Google تعتبر أي بيانات تطلع من الجوال «جمع» حتى لو راحت لجوال ثاني، فالإجابات التالية هي الأحوط (conservative) وتطابق سياسة الخصوصية.

## Section 1 — Data collection and security
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **No** (only private-group messages/voice are AES-256-GCM encrypted; public-channel messages are not) |
| Do you provide a way for users to request that their data is deleted? | **Yes** — in-app «مسح كل السجل» / «مسح المحادثة»; no data exists on any server |
| Account creation | App does not have accounts (no login) |

## Section 2 — Data types
For each type below: **Collected = Yes · Shared = No** (transfer is user-initiated to nearby devices, which Play exempts from "sharing") · **Processed ephemerally = No** (messages are stored on device) · **Required/Optional = Optional** (user chooses to send) · **Purpose = App functionality** only.

| Category | Data type | Notes |
|---|---|---|
| Location | Precise location | Only sent when the user taps «شارك موقعي» or the SOS button; also saved with signal-log readings on device |
| Location | Approximate location | Same as above |
| Messages | Other in-app messages | Chat text sent to nearby phones in a group |
| Audio | Voice or sound recordings | Live push-to-talk audio streamed to nearby phones; not recorded or stored |
| Personal info | Name | The display name the user chooses (e.g. «جوال-1234»), shown to nearby phones |
| App info & performance | — | **Not collected** (no analytics, no crash reporting) |
| Device or other IDs | — | **Not collected** (a random mesh ID is generated in-app and is not tied to the device) |

Everything else (financial info, contacts, photos, files, health, web history, calendar, etc.): **Not collected**.

## Section 3 — Security practices to mention
- No data is sent to the developer or any third party; there are no servers.
- Private groups use AES-256-GCM with a key derived from the group PIN (PBKDF2-SHA256).
- Users can delete all history from inside the app; uninstalling removes everything.
