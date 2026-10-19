# Dnevnik treninga (train-hr)

Jednostavna Android aplikacija (hrvatski, offline, Android 14+) za praćenje treninga snage.

## Što radi
1. **Postavke plana:** broj treninga tjedno (1–5), raspon ponavljanja (6–10, 8–12, 10–15)
   i izbor vježbi po mišićnim skupinama (noge, leđa, prsa, ramena, ruke, trup) + vlastite vježbe.
2. **Plan:** aplikacija raspoređuje odabrane vježbe po treninzima – svaka odabrana mišićna
   skupina dolazi u svakom treningu, a više vježbi iste skupine se izmjenjuje.
3. **Trening:** upis serija (kg + ponavljanja ili sekunde), uz prikaz prošlog rezultata.
4. **Preporuka težine** (3 serije u odabranom rasponu):
   - sve serije na gornjoj granici → povećaj (bučice +2 kg, šipka/trap bar +2,5 kg, mašina +5 kg),
   - dva treninga zaredom ispod donje granice → smanji za isti korak,
   - inače zadrži. Lakše serije za zagrijavanje se ne računaju.
5. **Napredak:** po vježbi početak → sada, graf i zadnji rezultati; popis odrađenih treninga.
6. Izvoz/uvoz podataka (JSON) kao sigurnosna kopija.

## Instalacija
GitHub → **Actions** → zadnji uspješan "Android CI" → artefakt `trening-snage-debug-apk`
→ raspakirati ZIP → instalirati `app-debug.apk`.

## Razvoj
```
./gradlew :core:test            # logika plana i preporuka + testovi
./gradlew :app:assembleDebug    # traži Android SDK
./gradlew -PcoreOnly :core:test # bez Android SDK-a
```
