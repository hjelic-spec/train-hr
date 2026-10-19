# Train-HR — plan aplikacije (nacrt za pregled)

Android aplikacija za personalizirani trening snage za osobe 50+, rad u teretani s
ograničenim brojem sprava (bučice, trap bar, …), progresivno opterećenje, ciljevi,
analiza napretka i prilagodba sportu (veteransko veslanje, trčanje, biciklizam, opće).

> Status: **NACRT v2 — uključene odluke korisnika, čeka konačnu potvrdu.**

## 0. Donesene odluke (19. 10. 2026.)

| Tema | Odluka | Posljedica za plan |
|---|---|---|
| Jezik | Samo hrvatski | Svi tekstovi u `strings.xml` (HR); struktura ostaje spremna za prijevod, ali ga ne radimo. |
| Android | 14 | `minSdk 34` — pojednostavljuje Health Connect (dio sustava od Androida 14) i Garmin → HC sinkronizaciju. |
| Mreža | Potpuno offline | Nema backenda, nema AI-komentara u oblaku; "trenerska" analiza isključivo po pravilima. Aplikacija ne traži dozvolu `INTERNET`. |
| Korisnici | Vi + odabrani korisnici | Distribucija potpisanim APK-om (ručna instalacija) ili Google Play *zatvoreno testiranje*. Svaki korisnik ima podatke samo na svom telefonu; backup = izvoz/uvoz datoteke. |
| Oprema | Bučice 8–40 kg, lat mašina, privlak sajle (sjedeće veslanje), šipka s pločama (sve težine), trap bar | Biblioteka vježbi i progresija grade se oko ove opreme (vidi 3.1). |
| Odabir vježbi | Korisnik može **ručno** birati vježbe | Novi način rada "Moj plan" (vidi 3.2a). |

### Kritičke napomene uz odluke
- **Bučice počinju od 8 kg.** Za dio vježbi (odručenje u stranu, potisak iznad glave, rotacije
  ramena) to je za početnike 50+ često previše. Aplikacija će za takve vježbe nuditi zamjenu
  (sajla, šipka bez utega, vlastita težina) umjesto da forsira 8 kg.
- **Korak bučica.** Progresija mora znati stvarne dostupne težine (npr. 8, 10, 12 … ili
  8, 9, 10 …). Skok s 8 na 10 kg je +25 % — prevelik za mnoge vježbe; tada aplikacija prvo
  povećava ponavljanja/serije, a tek onda težinu.
- **Offline + više korisnika** znači da nema dijeljenja ni usporedbe među korisnicima i nema
  automatskog backupa — gubitak telefona = gubitak podataka ako korisnik nije izvezao backup.
  Predlažem podsjetnik na izvoz jednom mjesečno.
- **Health Connect bez Google Playa:** treba provjeriti mogu li ručno instalirane (sideload)
  aplikacije dobiti HC dozvole bez Play deklaracije. Provjerit ću u dokumentaciji prije faze 4;
  ako ne mogu, distribucija ide preko Play zatvorenog testiranja.

---

## 1. Kritički osvrt na zahtjeve (što je realno, a što ne)

| Zahtjev | Procjena | Prijedlog |
|---|---|---|
| "Puna analiza profesionalnog trenera" | Aplikacija ne može zamijeniti trenera niti procijeniti tehniku izvođenja. | Pravila temeljena na smjernicama (ACSM) + jasni izvještaji, svaka preporuka s obrazloženjem. Bez AI-komentara (offline). |
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
- Dostupna oprema (zadano prema odluci, korisnik može mijenjati):
  - bučice — popis točnih težina (zadano 8–40 kg, korak se upisuje),
  - šipka + ploče — najmanja ploča određuje korak (npr. 1,25 kg → +2,5 kg),
  - trap bar (+ ploče),
  - lat mašina (pulldown) i sajla za sjedeće veslanje — korak slaganja utega se upisuje,
  - dodatno (provjeriti): klupa (ravna/kosa), stalak za čučanj.
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

### 3.2a Ručni odabir vježbi — "Moj plan"
Tri načina rada, korisnik bira i može ih mijenjati u bilo kojem trenutku:
1. **Automatski** — generator slaže plan (3.2).
2. **Prilagođeni** — generator predloži plan, korisnik dodaje/briše/mijenja vježbe i redoslijed.
3. **Ručni** — korisnik sam slaže treninge iz biblioteke (filter po opremi, mišićnoj skupini,
   obrascu pokreta, sportu) i za svaku vježbu bira broj serija, raspon ponavljanja i odmor
   (s predloženim zadanim vrijednostima).

U svim načinima vrijedi isto:
- **progresija težine ostaje automatska** (može se isključiti po vježbi),
- moguće je dodati **vlastitu vježbu** (naziv, oprema, mišićne skupine),
- aplikacija **upozorava, ali ne blokira**: npr. "ovaj tjedan nema vježbi povlačenja",
  "noge su trenirane samo 1× tjedno (preporuka ACSM: ≥ 2×)", ili vježba koja je u profilu
  označena kao rizična zbog ozljede.

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
4. **AI trener**: odbačeno (odluka: potpuno offline).

## 4. Tehnička arhitektura

- **Kotlin + Jetpack Compose** (Material 3), jedan modul `app` na početku.
- **Room** (lokalna baza, offline-first), **Hilt** (DI), **WorkManager** (sinkronizacija),
  **DataStore** (postavke), **Health Connect SDK** (`androidx.health.connect:connect-client`).
- Grafovi: Vico (Compose).
- Bez vlastitog backenda u MVP-u; podaci ostaju na uređaju. Izvoz/uvoz (JSON/CSV) za backup.
- Domenska logika (generator plana, progresija, analitika) u čistom Kotlinu — **pokrivena
  unit testovima**, jer je to "mozak" aplikacije.
- minSdk 34 (Android 14), targetSdk najnoviji stabilni.
- Bez dozvole `INTERNET`; jedini izlaz podataka je izvoz datoteke koji pokreće korisnik.

### Model podataka (skica)
`UserProfile`, `Equipment`, `Exercise` (obrazac pokreta, mišići, oprema, kontraindikacije,
sportske oznake), `Program` → `ProgramWeek` → `WorkoutTemplate` → `ExercisePrescription`,
`WorkoutSession` → `SetLog` (kg, pon., RIR, bol), `Goal`, `ProgressSnapshot`,
`ExternalActivity` (iz Health Connecta).

## 5. Faze razvoja

| Faza | Sadržaj | Rezultat |
|---|---|---|
| 0 | Potvrda plana, odgovori na otvorena pitanja, skelet projekta, CI build | Prazna aplikacija se gradi |
| 1 (MVP) | Profil, PAR-Q+, oprema, biblioteka vježbi (~30–40), generator plana **i ručni odabir vježbi**, vođenje treninga, progresija, povijest, izvoz/uvoz | Upotrebljivo u teretani |
| 2 | Ciljevi, analitika, grafovi, tjedni izvještaj | "Trenerski" pregled |
| 3 | Sport-moduli + periodizacija po sezoni | Veslanje / trčanje / bicikl |
| 4 | Health Connect (Garmin preko HC) | Objedinjeni pregled opterećenja |
| 5 (opcija) | Upute/slike vježbi, Concept2 (zahtijeva internet — samo ako se odustane od strogo offline) | Nadogradnje |

## 6. Otvorena pitanja za vas

Odgovoreno: jezik, Android, offline, korisnici, oprema, ručni odabir vježbi.

Preostalo (nije blokirajuće za fazu 0–1, ima zadane vrijednosti):
1. Korak bučica između 8 i 40 kg (zadano: 2 kg)?
2. Imate li klupu (ravnu/kosu) i stalak za čučanj? (zadano: da)
3. Koji Garmin uređaj? (važno tek za fazu 4)
4. Ozljede/ograničenja za pravila odabira vježbi? (zadano: nema; unosi se u profilu)

## Izvori
- ACSM 2026 Position Stand – infografika: https://acsm.org/wp-content/uploads/2026/03/Resistance-Training-Position-Stand-infographic.pdf
- ScienceDaily (ožujak 2026.): https://www.sciencedaily.com/releases/2026/03/260319074552.htm
- ACSM 2011 – sažetak (UNM): https://www.unm.edu/~lkravitz/Article%20folder/ACSMGuidelinesUNM.pdf
- Google Fit → Health Connect migracija: https://developer.android.com/health-and-fitness/health-connect/migration/fit
- Health Connect tipovi podataka: https://developer.android.com/health-and-fitness/health-connect/data-types
- Garmin Connect Developer Program: https://developer.garmin.com/gc-developer-program/overview
- Garmin + Health Connect (Notebookcheck): https://www.notebookcheck.net/Garmin-officially-reveals-Google-Health-Connect-support-for-wearables.1054577.0.html
- Concept2 Logbook API: https://log.concept2.com/developers/documentation/
