# Trening snage 50+ (train-hr)

Android aplikacija (hrvatski, offline) za personalizirani trening snage za osobe 50+.
Plan, odluke i izvori: [PLAN.md](PLAN.md).

## Što radi (faza 1)
- Profil, ograničenja/ozljede, upitnik spremnosti (opća pitanja prema PAR-Q+).
- Oprema teretane: bučice (popis težina), šipka, trap bar, lat mašina, sajla, klupa, stalak, Concept2 ergometri.
- **Obavezni početni test** u dva dana: trap bar mrtvo dizanje, potisak s klupe, lat mašina,
  pregib bučicama (biceps), čučanj šipkom, veslanje na sajli, potisak bučica iznad glave
  + ustajanje sa stolice i ravnoteža. Maksimum (1RM) se **procjenjuje** iz serije od 4–10 ponavljanja.
- Plan: automatski, prilagođeni ili potpuno ručni odabir vježbi, s upozorenjima (npr. mišićna skupina < 2× tjedno).
- Vođenje treninga: predložena težina, unos serija (kg, ponavljanja, RIR, bol), timer odmora.
- Automatska progresija (dvostruka progresija, zaokruženo na stvarnu opremu), lakši tjedan svakih 5 tjedana.
- Napredak: redovitost, procjena snage test → danas, povijest. Izvoz/uvoz podataka (JSON).

Aplikacija nema dozvolu za internet. Podaci su samo na telefonu – koristite izvoz za sigurnosnu kopiju.

## Instalacija (APK)
1. GitHub → **Actions** → zadnji uspješan "Android CI" → artefakt `trening-snage-debug-apk`.
2. Raspakirajte ZIP, prebacite `.apk` na telefon (Android 14+) i otvorite ga
   (dopustite instalaciju iz nepoznatih izvora).

Napomena: to je *debug* verzija potpisana razvojnim ključem – za dijeljenje odabranim korisnicima
dovoljna je, ali za trajnu distribuciju treba napraviti vlastiti ključ za potpisivanje (sljedeći korak).

## Razvoj
```
./gradlew :core:test            # domenska logika (čisti Kotlin) + testovi
./gradlew :app:assembleDebug    # traži Android SDK
./gradlew -PcoreOnly :core:test # bez Android SDK-a
```
- `core/` – biblioteka vježbi, početni test, procjena 1RM, progresija, generator i provjera plana.
- `app/` – Jetpack Compose sučelje i spremanje podataka.

Preporuke se temelje na smjernicama ACSM-a i ne zamjenjuju liječnika ni trenera.
