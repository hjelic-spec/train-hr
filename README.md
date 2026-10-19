# Dnevnik treninga (train-hr)

Jednostavna Android aplikacija (hrvatski, offline, Android 14+) za praćenje treninga snage.

## Što radi
1. **Postavke plana:** cilj (snaga 5–8 pon. / hipertrofija 8–12 pon.), odmor između serija i
   između vježbi (preporuka po cilju: snaga 3 min / 3 min, hipertrofija 90 s / 2 min – može se
   promijeniti), broj treninga tjedno (1–5), trajanje (30–90 min) i izbor vježbi po
   mišićnim skupinama (noge, leđa, prsa, ramena, ruke, trup) + vlastite vježbe.
2. **Plan:** odabrane vježbe raspoređuju se po treninzima tako da stanu u trajanje
   (procjena: 5 min zagrijavanja + serije + odmori). Ako nema mjesta, prednost imaju noge,
   leđa i prsa, a ostale skupine se izmjenjuju; vježbe koje ne stanu prikazuju se kao upozorenje.
3. **Trening sa štopericom:** „Počni seriju“ mjeri rad, „Kraj serije“ pokreće odbrojavanje
   odmora između serija ili između vježbi (vibracija i zvuk na kraju), ukupno vrijeme treninga.
   Upis serija (kg + ponavljanja ili sekunde), uz prikaz prošlog rezultata.
4. **Preporuka težine** (3 serije u rasponu cilja):
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
