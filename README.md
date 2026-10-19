# Dnevnik treninga (train-hr)

Android aplikacija (hrvatski, offline, Android 14+) za praćenje treninga snage.

## Što radi
- **Postavke plana** (kotačić gore desno → Plan i program): cilj (snaga 5–8 / hipertrofija 8–12 pon.),
  početak i kraj programa, broj treninga tjedno (1–5), trajanje (30–90 min), odmor između serija i vježbi
  (preporuka po cilju, može se promijeniti), tjelesna masa, izbor vježbi po mišićnim skupinama + vlastite vježbe
  i **stalne** vježbe.
- **Plan**: svaki tjedan nanovo raspoređene i promiješane vježbe koje stanu u trajanje; stalne vježbe 2× tjedno;
  velike skupine (noge, leđa, prsa) u svakom treningu; datumi i stanje (odrađeno / danas / propušteno);
  „Promiješaj” za neodrađene treninge.
- **Trening**: jedna vježba po ekranu, sličice početnog i završnog položaja, štoperica rada, odbrojavanje odmora
  između serija i vježbi (vibracija i zvuk), −/+ za težinu i ponavljanja, „Sprava zauzeta – kasnije”,
  pregled i sažetak s preporukama za sljedeći put.
- **Početna**: program, sljedeći i posljednji trening. **Treninzi**: popis i detalji. **Analiza**: tjedni
  kilogrami, kalorije (gruba procjena: 3,5 MET × masa × trajanje, 2024 Adult Compendium, 02054) i napredak po vježbi.
- Preporuka težine: dvostruka progresija (sve serije na gornjoj granici → +2 kg bučice, +2,5 kg šipka, +5 kg mašina).
- Izvoz/uvoz podataka (JSON). Aplikacija nema dozvolu za internet.

## Instalacija
GitHub → **Actions** → zadnji uspješan "Android CI" → artefakt `trening-snage-debug-apk`
→ raspakirati ZIP → instalirati `app-debug.apk`.

## Web pregled
`web/index.html` je web pregled iste aplikacije za brzo isprobavanje promjena. Tjedni raspored je
bit po bit isti kao u aplikaciji (provjereno testom `WeekPlannerTest.matchesWebPreview`).

## Razvoj
```
./gradlew :core:test            # logika plana, preporuka i statistike + testovi
./gradlew :app:assembleDebug    # traži Android SDK
./gradlew -PcoreOnly :core:test # bez Android SDK-a
```
