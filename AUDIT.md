# LifeCapsule — boshlang'ich audit

Sana: 2026-09-11

## Saqlangan boshlang'ich holat

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8085`
- Ko'rinadigan holat: avvalgi saqlangan sessiya bilan **"Server bilan aloqa uzildi"** xabari.
- Ma'lumotlar manbai: `lifecapsule` PostgreSQL bazasi (o'zgartirilmagan).

## Ajratilgan audit muhiti

`lifecapsule_audit` — 2026-09-11 dagi `lifecapsule` bazasining mahalliy nusxasi.

| Mazmun | Soni |
| --- | ---: |
| Foydalanuvchilar | 6 |
| Oilalar | 1 |
| Odam profillari | 106 |
| Qarindoshlik bog'lanishlari | 188 |
| Ruxsat yozuvlari | 4 |

Profil: `audit` (`src/main/resources/application-audit.yml`). U 8087-portda ishlaydi, `ddl-auto=validate` bilan sxemani o'zgartirmaydi va admin initializer'ni o'chiradi.

Audit backend hozir `http://localhost:8087` da ishlayapti; copied dataset uchun login endpointi 200 qaytardi.

Ishga tushirish:

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=audit"
```

Frontendni shu backendga bog'lash:

```powershell
$env:VITE_API_BASE_URL = 'http://localhost:8087'
npm run dev -- --port 5174
```

`lifecapsule-audit-baseline.dump` nusxani qayta tiklash uchun saqlangan PostgreSQL dumpidir. Audit tugagach uni yoki `lifecapsule_audit` bazasini o'chirishdan oldin alohida tasdiq kerak.

## Natijalar

| Yo'nalish | Holat | Izoh |
| --- | --- | --- |
| Auth → ilova | Qisman | Login API to'g'ri JSON bilan 200 qaytaradi. Saqlangan/yaroqsiz sessiya `Server bilan aloqa uzildi` ekranida qoladi; foydalanuvchi aniq login ekraniga yo'naltirilmaydi. |
| Dashboard ↔ shajara | Koddan tasdiqlandi | Tezkor tugmalar va qidiruv `#/tree` hamda fokuslangan odam route'iga olib boradi. |
| Shajara → profil → shajara | Koddan tasdiqlandi | Odam profili route'i, qarindoshlar ro'yxati va orqaga qaytish mavjud. |
| Oilalar almashinuvi | Koddan tasdiqlandi | Sidebar family switcher dashboardga qaytaradi va faol oilani local storage'da saqlaydi. |
| Takliflar ↔ ruxsatlar | Qisman | Owner taklif yaratadi; foydalanuvchi takliflar sahifasida qabul/rad eta oladi. Email yuborilmaydi — faqat ilova ichidagi taklif. |
| Sozlamalar | Koddan tasdiqlandi | Akkaunt, parol, owner uchun oila sozlamalari va o'chirish tasdiqlash oynasi bor. |

## Muammolar va navbat

1. **P1 — sessiya xatosi noto'g'ri tasniflanadi.** Yaroqsiz token/refresh token uchun chiqish va login ekraniga qaytish o'rniga umumiy server-aloqa xabari chiqadi. Bu joriy baseline'da ko'rindi.
2. **P1 — mobil pastki navigatsiyada `Sozlamalar` va owner uchun `Ruxsatlar` yo'q.** Ular hamburger menyu orqali mavjud, biroq asosiy mobil navigatsiya faqat uch sahifani ko'rsatadi. Tez-tez ishlatiladigan sozlamalarga bir bosqich ortiqcha qoladi.
3. **P2 — real UI oqimlari hali autentifikatsiyadan keyin vizual tekshirilmagan.** Test muhiti tayyor; davom uchun audit akkaunti bilan kirish yoki shu akkauntdan foydalanishga ruxsat kerak.
4. **P2 — testlar haqiqiy bazaga ulanadi.** `FamilySearchQueryTest` standart profil bilan `lifecapsule`ga ulanadi. CI/mahalliy testlar `audit` yoki ephemeral DB profiliga o'tkazilishi kerak.
5. **P3 — backendda `spring.jpa.open-in-view` yoqilgan va PostgreSQL dialect eskirgan explicit sozlamaga ega.** Ishga to'sqinlik qilmaydi, ammo keyingi texnik tozalashga kiritilsin.

## Tekshiruvlar

- Frontend: `npm run typecheck`, `npm run lint`, `npm run build` — muvaffaqiyatli.
- Backend: `./mvnw.cmd test` — 37 test muvaffaqiyatli.
