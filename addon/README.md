# HBM Nukes via Mekanism (Addon, Forge 1.20.1)

Addon fuer **HBM's Nuclear Tech Mod: Continuum** (1.20.1 Forge, Mod-ID `hbm`).

Benoetigt: Forge 47.x, Continuum + Golem Core, Mekanism 10.4+.
Optional (Client): Distant Horizons 2.3+ (API 3.0).

## Mekanism-Rezepte (alle im Combiner)
Zwischenteile: `bomb_casing` (8 Stahl + 2 Refined Obsidian), `fissile_core` (8 Uran + 2 Blei),
`thermonuclear_stage` (Spaltkern + 4 Atomlegierung), `exotic_core` (2 Antimaterie-Pellets + 4 Atomlegierung).
Bomben: Little Boy, Fat Man, Gadget, Ivy Mike, Tsar Bomba, N2, Fleija, Prototype, Solinium, Balefire.
Alle Rezepte sind JSON in `src/main/resources/data/hbmnukemeka/recipes` und lassen sich dort anpassen.

## Distant Horizons
Der Server meldet jede aktive Continuum-Pilzwolke (Torex) zweimal pro Sekunde an alle Spieler der Dimension.
Ist der Client weit entfernt (die echte Entity wird nicht gerendert), zeichnet der Addon ueber die DH-Generic-Rendering-API
eine vereinfachte Pilzwolke aus Boxen (Feuerball, Stiel, Kappe) in die LODs. Nahe Wolken rendert weiter Continuum selbst.

## Bauen
GitHub: Repo anlegen, Dateien pushen, unter *Actions* den Build `Build` starten, Jar aus dem Artifact `hbm-nuke-meka` nehmen.
Lokal: `gradle build` (Gradle 8.8, Java 17).
