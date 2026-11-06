# NOVABOMBA – mod do Minecrafta 1.20.1 (Forge + Fabric)

Jeden wspólny kod (`common/`) + cienkie warstwy dla loaderów (`forge/`, `fabric/`).
Oba projekty używają mapowań Mojang (official), dlatego kod jest identyczny.

## Budowanie (JDK 17)
    cd fabric && gradle wrapper --gradle-version 8.5 && ./gradlew build   -> fabric/build/libs/novabomba-fabric-1.0.0.jar
    cd forge  && gradle wrapper --gradle-version 8.5 && ./gradlew build   -> forge/build/libs/novabomba-forge-1.0.0.jar
Fabric wymaga Fabric API 0.92.2+1.20.1. Można też otworzyć folder `fabric` lub `forge` w IntelliJ.

## Obsługa
- Menu: **Prawy Shift** (zmienisz w Ustawienia -> Skróty). R = AutoAttack, G = atak, X = wyczyść cel, H = ESP.
- Gracze: LPM = cel, PPM = podświetl kilku graczy.
- Konfiguracja zapisuje się sama: `config/novabomba.json`, historia: `config/novabomba_history.json`.

## Uwagi
- Kod nie był kompilowany w środowisku, w którym powstał (brak dostępu do sieci/Gradle).
  Możliwe drobne poprawki nazw API przy pierwszym buildzie.
- Blur tła używa shadera `shaders/post/blur.json`; jeśli go nie ma, GUI działa bez rozmycia (tylko przyciemnienie).
- Funkcje ataku/ESP to cheaty – na większości serwerów łamią regulamin i kończą się banem. Używaj na własnym serwerze / za zgodą administracji.
