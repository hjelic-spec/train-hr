# Train-HR — plan aplikacije (nacrt za pregled)

Android aplikacija za personalizirani trening snage za osobe 50+, rad u teretani s
ograničenim brojem sprava (bučice, trap bar, …), progresivno opterećenje, ciljevi,
analiza napretka i prilagodba sportu (veteransko veslanje, trčanje, biciklizam, opće).

> Status: **NACRT — čeka pregled.** Ništa se ne implementira dok plan nije potvrđen.

---

## 1. Kritički osvrt na zahtjeve (što je realno, a što ne)

| Zahtjev | Procjena | Prijedlog |
|---|---|---|
| "Puna analiza profesionalnog trenera" | Aplikacija ne može zamijeniti trenera niti procijeniti tehniku izvođenja. | Pravila temeljena na smjernicama (ACSM) + jasni izvještaji. Opcionalno kasnije: AI-komentar (zahtijeva internet i slanje podataka u oblak). |
| Direktna veza na Garmin | Garmin Connect Developer Program namijenjen je odobrenim **poslovnim** subjektima; prema izvještajima, prijave za nove pristupe trenutno su pauzirane. Za privatnu aplikaciju nije realno. | Koristiti **Health Connect**: Garmin Connect od 2025. službeno šalje podatke u Health Connect (jednosmjerno, Android 14+). Time indirektno dobivamo Garmin podatke. |
| "Android Health" | Google Fit API se gasi (deprecated 2026.); zamjena je **Health Connect**. | Health Connect od prvog dana integracije. |
| Bilježenje serija/ponavljanja/težine u Health Connect | Health Connect nema tip podatka za serije × ponavljanja × kg (ima sesiju vježbanja tipa `STRENGTH_TRAINING`/`WEIGHTLIFTING`). | Detaljni podaci žive u lokalnoj bazi aplikacije; u Health Connect upisujemo samo sesiju (vrijeme, tip, trajanje). |
| Sigurnost (50+) | Smjernice preporučuju liječničku procjenu prije početka kod osoba s rizicima. | Upitnik spremnosti (npr. PAR-Q+) pri prvom pokretanju + konzervativan početak i oprez kod boli. |

## 2. Stručna osnova (izvori)

- **ACSM Position Stand 2026** (*Med Sci Sports Exerc* 58(3):512–545): sve glavne mišićne skupine
  **najmanje 2× tjedno**, postupna progresija; najveći dobitak je prijelaz s "ništa" na "nešto";
  program mora biti održiv i individualiziran; sportaši trebaju sport-specifičan plan.
- **ACSM 2011 (starije osobe)**: 2–3 dana/tjedno, ~48 h odmora za istu mišićnu skupinu; početnici
  počinju lagano (40–50 % 1RM), 10–15 ponavljanja, postupno prema 60–70 %+ 1RM.

Ove brojke koristimo kao **zadane granice** generatora plana, ne kao medicinski savjet.

## 3. Ključne funkcije (MVP → kasnije)

### 3.1 Profil i početna procjena
- Dob, spol, tjelesna masa, iskustvo (početnik / srednji / napredni), ozljede/ograničenja
  (npr. križa, rame, koljeno → isključuje ili zamjenjuje vježbe).
- Upitnik spremnosti (PAR-Q+); ako ima "DA" odgovora → preporuka konzultacije s liječnikom.
- Dostupna oprema (checkbox): bučice (raspon kg + korak), trap bar, šipka + stalak, klupa,
  sajla/kabel, lat-pulldown, leg press, veslački ergometar, TRX/gume.
- Početna testiranja (opcionalno): procijenjeni 1RM iz submaksimalnog testa (npr. 8–10 pon.),
  hvat, sjedni-ustani 30 s, ravnoteža na jednoj nozi.

### 3.2 Generator plana
- Ulazi: broj treninga tjedno (2–4), trajanje (30/45/60 min), fokus (cijelo tijelo / noge /
  leđa-stražnji lanac / gornji dio / core), sport (veslanje / trčanje / biciklizam / opće),
  faza sezone (pripremna / natjecateljska / prijelazna).
- Struktura po obrascima pokreta: **hinge** (trap bar mrtvo dizanje, RDL), **čučanj** (goblet,
  split squat), **potisak** (bench s bučicama, potisak iznad glave), **povlačenje** (veslanje
  bučicom, pulldown), **nošenje** (farmer's walk – trap bar/bučice), **core/anti-rotacija**,
  **ravnoteža**.
- Zagrijavanje i mobilnost prilagođeni dobi uvijek uključeni.

### 3.3 Sport-specifični moduli (prijedlog sadržaja)
- **Veslanje (veteransko)**: naglasak na stražnji lanac i odraz nogama (trap bar, RDL, hip
  thrust), izdržljivost trupa i anti-rotacija (Pallof, bočni plank), balans povlačenje/potisak
  (veslači su dominantno "povlačenje"), zaštita donjeg dijela leđa. Smanjenje volumena snage u
  tjednima regata.
- **Trčanje**: jednonožne vježbe (split squat, step-up, jednonožni RDL), listovi, abduktori kuka.
- **Biciklizam**: vježbe s opterećenjem kostiju i trupa, stražnji lanac, gornji dio tijela i
  držanje (biciklizam je bez udarnog opterećenja).
- **Opće**: ravnomjeran program + ravnoteža, hvat, snaga ustajanja.

> Sport-specifični sadržaj pripremit ću uz navođenje izvora prije implementacije; gornje su
> polazne točke, ne konačan program.

### 3.4 Vođenje treninga (u teretani)
- Ekran "trening danas": vježba, ciljani raspon ponavljanja, predložena težina, odmor (timer).
- Unos po seriji: kg, ponavljanja, **RIR/RPE** (koliko je ostalo "u rezervi"), bol 0–10.
- Veliki gumbi i fontovi, rad jednom rukom, radi bez interneta.
- Zamjena vježbe jednim dodirom (zauzeta sprava, bol).

### 3.5 Progresija (automatska)
- **Dvostruka progresija**: npr. 3×8–12; kada sve serije dostignu gornju granicu uz RIR ≥ 2 →
  sljedeći put povećaj težinu za najmanji korak opreme (bučice 1–2 kg, trap bar 2,5–5 kg).
- Ako ponavljanja padnu ispod donje granice ili bol ≥ 3 → zadrži/smanji težinu.
- Planirani lakši tjedan (deload) svakih 4–6 tjedana ili kad trend pokazuje stagnaciju/umor.
- Opcionalno: povezivanje s podacima oporavka iz Health Connect (puls u mirovanju, san) kao
  *upozorenje*, ne kao automatska odluka.

### 3.6 Ciljevi i analiza ("trenerski izvještaj")
- Ciljevi: snaga (npr. trap bar 100 kg × 5), tjelesna masa, učestalost, funkcionalni testovi,
  sportski cilj (npr. vrijeme na 2000 m ergometra).
- Analitika: procijenjeni 1RM (Epley/Brzycki) po vježbi, tjedni volumen po mišićnoj skupini,
  pridržavanje plana (% odrađenih treninga), trend RIR/boli, napredak prema cilju s prognozom.
- Tjedni/mjesečni izvještaj u obliku: *što ide dobro – što stagnira – preporuka za sljedeći blok*.
  Generira se pravilima; svaka preporuka prikazuje razlog ("težina povećana jer…").

### 3.7 Integracije
1. **Health Connect** (faza 4): čitanje treninga (uklj. Garmin veslanje/trčanje/vožnju), pulsa,
   koraka, sna, tjelesne mase; upis sesija snage. Uvjet za Garmin podatke: Android 14+.
2. **Concept2 Logbook API** (opcija, ako koristite Concept2 ergometar): službeni javni API s
   OAuth-om; zahtijeva odobrenje Concept2 za produkciju.
3. **Garmin direktno**: ne u planu (vidi točku 1).
4. **AI trener (opcija)**: komentar izvještaja putem LLM-a — zahtijeva internet, API ključ i
   pristanak na slanje podataka. Odluka na vama.

## 4. Tehnička arhitektura

- **Kotlin + Jetpack Compose** (Material 3), jedan modul `app` na početku.
- **Room** (lokalna baza, offline-first), **Hilt** (DI), **WorkManager** (sinkronizacija),
  **DataStore** (postavke), **Health Connect SDK** (`androidx.health.connect:connect-client`).
- Grafovi: Vico (Compose).
- Bez vlastitog backenda u MVP-u; podaci ostaju na uređaju. Izvoz/uvoz (JSON/CSV) za backup.
- Domenska logika (generator plana, progresija, analitika) u čistom Kotlinu — **pokrivena
  unit testovima**, jer je to "mozak" aplikacije.
- minSdk 26 (Health Connect zahtjev), targetSdk najnoviji stabilni; Garmin → HC traži Android 14+.

### Model podataka (skica)
`UserProfile`, `Equipment`, `Exercise` (obrazac pokreta, mišići, oprema, kontraindikacije,
sportske oznake), `Program` → `ProgramWeek` → `WorkoutTemplate` → `ExercisePrescription`,
`WorkoutSession` → `SetLog` (kg, pon., RIR, bol), `Goal`, `ProgressSnapshot`,
`ExternalActivity` (iz Health Connecta).

## 5. Faze razvoja

| Faza | Sadržaj | Rezultat |
|---|---|---|
| 0 | Potvrda plana, odgovori na otvorena pitanja, skelet projekta, CI build | Prazna aplikacija se gradi |
| 1 (MVP) | Profil, PAR-Q+, oprema, biblioteka vježbi (~30–40), generator plana, vođenje treninga, progresija, povijest | Upotrebljivo u teretani |
| 2 | Ciljevi, analitika, grafovi, tjedni izvještaj | "Trenerski" pregled |
| 3 | Sport-moduli + periodizacija po sezoni | Veslanje / trčanje / bicikl |
| 4 | Health Connect (Garmin preko HC) | Objedinjeni pregled opterećenja |
| 5 (opcija) | Concept2 API, AI komentar, upute/video vježbi | Nadogradnje |

## 6. Otvorena pitanja za vas

1. Točan popis sprava u vašoj teretani (osim bučica i trap bara)? Raspon i korak bučica?
2. Jezik sučelja: samo hrvatski ili HR + EN?
3. Koju verziju Androida ima vaš telefon (Garmin → Health Connect zahtijeva 14+)? Koji Garmin uređaj?
4. Veslate li i na Concept2 ergometru (za Concept2 integraciju)?
5. AI-komentar u oblaku: želite li ga (uz slanje podataka) ili sve strogo offline?
6. Samo za vas (APK) ili kasnije objava na Google Play (utječe na Health Connect odobrenje i privatnost)?
7. Imate li ozljede/ograničenja koja treba ugraditi u pravila odabira vježbi?

## Izvori
- ACSM 2026 Position Stand – infografika: https://acsm.org/wp-content/uploads/2026/03/Resistance-Training-Position-Stand-infographic.pdf
- ScienceDaily (ožujak 2026.): https://www.sciencedaily.com/releases/2026/03/260319074552.htm
- ACSM 2011 – sažetak (UNM): https://www.unm.edu/~lkravitz/Article%20folder/ACSMGuidelinesUNM.pdf
- Google Fit → Health Connect migracija: https://developer.android.com/health-and-fitness/health-connect/migration/fit
- Health Connect tipovi podataka: https://developer.android.com/health-and-fitness/health-connect/data-types
- Garmin Connect Developer Program: https://developer.garmin.com/gc-developer-program/overview
- Garmin + Health Connect (Notebookcheck): https://www.notebookcheck.net/Garmin-officially-reveals-Google-Health-Connect-support-for-wearables.1054577.0.html
- Concept2 Logbook API: https://log.concept2.com/developers/documentation/
