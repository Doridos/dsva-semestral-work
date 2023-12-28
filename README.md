# LE - Ri-Ag delay - Java JMS - shared variable - Semestrální práce pro předmět B2M32DSVA

## Autor 👨‍💻
Vladyslav Babyč

Softwarové inženýrství a technologie, Fakulta elektrotechnická, ČVUT

2023

## Popis aplikace 🖥️

Aplikace nabízí funkcionalitu distribuovaného systému, který má implementován algoritmus Ri-Ag Delay pro přístup do kritické sekce, 
které je realizována pomocí hashmapy a umožňuje zápis osmi prvků klíč - hodnota. Veškerá komunikace mezi jednotlivými uzly probíhá
pomocí JMS, která je zprovozněna v clusteru.

## Jak práci spustit 🔧
Ve složce /out/artifacts najdete dvě složky s JAR archivy, jeden se jmenuje Subscriber.jar druhý Publisher.Jar.
Názvy těchto archivů jsou vypovídající. 
Soubory je nutno spouštět následujícím příkazem:"
```
java -jar Subscriber.jar <ID>
```
Pokud nebude zadán argument ID, bude ID zvoleno na základě IP adresy stanice, kde je proces spouštěn.

```
java -jar Publisher.jar <ID> <write|read>
```
Pokud nebude zadán argument ID bude doplněno automaticky na výchozí hodnotu. V případě,
že bude chybět i druhý argument bude publisher ve výchozím nastavení zprávy publikovat.

V případě subscriberu, najdete logy z posledního běhu programu v souboru, který se jmenuje: subscriber_<Zvolené_ID>.log

Pro spuštění distribuovaného systému je také nutné zprovoznit JMS. V konrkténí implementaci se 
jednotlivé subscribery a publishery, budou připojovat na adresy z lokálního rozsahu. V případě 
změny těchto adres je tedy nutné změnit i jar archivy, jelikož je nutné tuto změnu provést přímo 
v kódu aplikace.


## Popis základních funckcionalit ⚙️
**Co si pamatují uzly?**

Každý uzel si pamatuje svoje ID, které je odvozeno z posledních devíti čísel IP adresy, nebo případně může být nastaveno argumentem z příkazového řádku. Dále každý uzel má svůj dataStore, což 
je hashtable, do které si na základě instrukcí zapisuje hodnoty. Také si uzel pamatuje potřebné proměnné pro implementaci Ri-Ag algoritmu, tedy MyRq, MaxRq, Req, RpCnt. Doplňkově si každý uzel po žádosti o zapsání
zapamatuje klíč na který má zapsat hodnotu a také zvolenou hondotu k zápisu. Každý uzel si také udržuje další proměnné jako je timer a rebuild. Pro napravení topologie.

**Jak se propojují uzly?**

Uzly komunikují skrze JMS. Neexistuje tedy žádné příme propojení mezi uzly.

**Jak probíhá obnova topologie?**

Pokud se v aktuální chvíli nezpracovává žádná zpráva probíha obnova topologie periodicky v této konkrétní implementaci jednou za 20 vteřin. 
Uzly se téměř ihned dozví, že se nový uzel připojil a dle této informace si upraví proměnné potřebné pro fungování Ri-Ag algoritmu.
Pokud se uzel odpojí "gracefully" tak se o tom uzly také dozví a provedou úpravu vybranných proměnných. V případě, že nastane situace, 
že nějaký uzel odpadne a neinformuje o tom ostatní, je implementován algoritmus pro napravení topologie i v této situaci. Pokud živé uzly (každý vybraný z nich)
nezpracovává žádnou zprávu obnoví se topologie automaticky pro 20 vteřinách. Pokud však uzel aktuálně žáda o vstup do kritické sekce a čeká 
na odpovědi od ostatních, které však nemůže dostat kvůli poruše jednoho z uzlu aktivuje se opět časovač na 20 vteřin po jehož uplynutí se topologie opět obnoví a 
algoritmus bude pokračovat.

Co se týče redundance JMS, ta je také implementována. Avšak funguje velmi nepředvídatelně v momentě spouštění. Občas se stane, že cluster se podaří vytvořit, občas naopak ne.
Co přesně tím mám na mysli znázorním během prezentace, kde však doufám, že se mi cluster podaří zprovoznit :).

**Kdy a jak se spouští implementovaný algoritmus?**
Algoritmu se spouští pokaždé, když od "žadatele" přijde zpráva o zápis do kritické sekce.