# Onda: plan

A personal Android radio player for a handful of favorite stations, with no ads, no account and no tracking.
It is not for sale and won't be published to the Play Store. It's installed by sideloading the APK that CI builds.

---

## 1. What "no ads" covers

| Ad type | Example | Gone in Onda? |
|---|---|---|
| Aggregator ads | TuneIn pre-rolls, banners, video ads, "Premium" nags | **Yes**. Onda plays the broadcaster's stream directly, so TuneIn isn't involved |
| Station's own on-air commercials | The same ad break you'd hear on FM | **No**. They're part of the broadcast audio |
| Server-side inserted digital ads | Triton/StreamTheWorld or AdsWizz streams that swap the FM break for targeted ads (common with big Spanish groups) | **No**. They're mixed into the stream itself. See the Phase 3 experiment |

Skipping TuneIn removes TuneIn's ads. To remove a station's own ads you'd have to edit its audio, which isn't realistic.

## 2. Why not reverse-engineer TuneIn

- **It isn't needed.** For most stations TuneIn only relays the broadcaster's public stream URL.
  [radio-browser.info](https://www.radio-browser.info) is an open, community-run database of about 50k stations with
  direct stream URLs, a free API, and no key or ads.
- **It breaks.** TuneIn's private API changes without notice, and its terms forbid reverse engineering.

## 3. Existing open-source apps (researched Nov 2026)

| App | License | Notes |
|---|---|---|
| **Transistor** (y20k, Codeberg/GitHub) | MIT | **Closest to the goal.** Minimalist: a short personal list, radio-browser search, no discovery. Active (v4.3.8, Aug 2026). On F-Droid |
| RadioDroid (segler-alex) | GPL-3 | The classic radio-browser client. Java. Upstream has gone quiet; the `morckx` fork and AMARadio continue it |
| RadioWave (darksoon) | GPL-3 | Kotlin, Compose, Media3, Android Auto, Chromecast, EQ. Feature-rich but very new |
| Literal Radio (256x/radio) | MIT | Tiny Compose + Media3 player. Good reference code |
| RadioScout / Open Open Radio | MIT / OSS | On F-Droid. Open Open Radio has Android Auto |

**Verdict:** if the goal were only listening, installing Transistor from F-Droid would do it today.
Onda exists because it's a personal project. It takes Transistor's idea (a small personal list plus radio-browser search)
and keeps the code small enough to understand completely.

## 4. Stack (deliberately small)

- Kotlin and Jetpack Compose (Material 3, dynamic color)
- **Media3 ExoPlayer + MediaSessionService**, which handles background playback, the media notification, lock-screen controls,
  Bluetooth/headset buttons and audio focus. It also plays MP3/AAC/Ogg/Opus and HLS (`.m3u8`)
- radio-browser API through `HttpURLConnection` and kotlinx.serialization, without Retrofit or OkHttp
- Favorites stored as a JSON string in SharedPreferences. There's no Room or DI framework because 10 stations don't need them
- About 600 lines of Kotlin

```
app/src/main/java/io/github/mcello23/onda/
├── MainActivity.kt          Compose entry point
├── data/
│   ├── Station.kt           model + toMediaItem()
│   ├── RadioBrowserApi.kt   search, click counting, mirror selection
│   └── FavoritesStore.kt    JSON in SharedPreferences
├── playback/
│   └── PlaybackService.kt   ExoPlayer + MediaSession (background, notification)
└── ui/
    ├── MainViewModel.kt     MediaController, search state, favorites
    ├── AppScreen.kt         Favorites / Search tabs + mini player
    └── Theme.kt
```

## 5. Phases

### Phase 1: MVP (code already in this repo)
- [x] Search radio-browser by name, sorted by popularity, with broken streams hidden
- [x] Tap a station to play it. Tap ♥ to add or remove a favorite
- [x] Background playback with notification, lock-screen and Bluetooth controls
- [x] Next and previous buttons on the notification or headphones cycle through your favorites
- [x] Stop and play reconnect to the live stream instead of replaying a stale buffer
- [x] Pauses when headphones are unplugged and respects calls and other audio (audio focus)
- [x] CI builds a signed APK on every push and publishes a GitHub Release on `v*` tags
- [x] **Android Auto** (moved up from Phase 3): `MediaLibraryService` browse tree (Favorites / Recent / Popular in your
      country), in-car search, voice ("Hey Google, play Cadena SER on Onda"), ♥ button on the car's now-playing screen,
      station logos through a local content provider
- [x] Survives tunnels and dead zones: ExoPlayer keeps retrying in BUFFERING (the notification and foreground service
      stay up), reconnects when the server closes the stream, and gives up quickly on dead stations (404, geo-block)
- [x] Saved stations refresh their stream URLs from radio-browser on each start, so rotated edge servers don't break favorites
- [x] Builds locally: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
- [x] CI workflow (`.github/workflows/build.yml`)

### v0.2 (done)
- Bigger UI: favorites as a grid of logo cards, a larger mini player, and a full-screen Now Playing with big prev/play/next
- Every station has artwork: the logo on a white square, or a colored monogram tile (phone, notification and Android Auto)
- 42 hand-checked stations in `app/src/main/assets/stations.json` (Brazil, USA incl. NPR/KEXP, UK incl. BBC 320k), each
  with the best stream that worked and a high-res logo. They rank first in browsing, search and voice
- Explore chips Brazil / USA / UK. Android Auto tabs: Favorites | Recent | Brazil | USA & UK

### v1.0 (done)
- **Material You Expressive UI + Dark Mode opcional:** navegação inferior (*Início*, *Explorar*, *Podcasts*, *Opções*), cores dinâmicas do Android 12+ + 4 paletas (*Oceano*, *Terracota*, *Esmeralda*, *Violeta*), tema *Sistema / Claro / Escuro*, mini-player flutuante animado e player tela cheia em tonal gradient
- **Correção definitiva de imagens:** `User-Agent` próprio no Coil (corrigindo 18/42 logos Wikimedia que retornavam HTTP 403), fallback automático pelo domínio da emissora (Google S2 HD favicon) no app e no `ArtworkProvider`, e fallback visual em gradiente
- **Top Espanha + Rock Mundial (64 estações curadas):** 15 estações espanholas verificadas (Rock FM, LOS40, LOS40 Classic, Cadena SER, RNE Radio 3, RNE Radio Nacional, Onda Cero, Europa FM, COPE, Cadena 100, Cadena Dial, Kiss FM España, Radio Marca, RAC1, Ràdio Flaixbac) + 7 clássicos mundiais de Rock/Eclectic (Radio Paradise 320k, Virgin Radio Italy, RADIO BOB!, FIP)
- **Tradução completa para Português (PT-BR)** com troca instantânea para Inglês nas Opções
- **Explorar por Gênero Mundial & 190+ Países:** cards de gêneros (Rock Mundial, Classic Rock, Indie, Pop, Jazz, Eletrônica, MPB, Anos 80, Chill/Lo-Fi, Clássica, Notícias) e seletor pesquisável com todos os países do Radio Browser
- **Podcasts completo (Streaming + Offline):** busca via iTunes Search API, parser RSS nativo, assinaturas, *Continuar ouvindo* com progresso salvo, seekbar, `-15s / +30s`, velocidade (`0.75x`–`2.0x`) e download offline real
- **Qualidade & Áudio avançado:** seletor de qualidade (*Auto HD / Alta 320k / Economia 4G*), fallback automático entre variantes de stream, buffer configurável (*Rápido / Equilibrado / Estável*), Normalização de volume (`LoudnessEnhancer`), Equalizador real de 5 bandas com presets, Sleep Timer com fade-out, Histórico de músicas ICY com atalhos 1 toque para Spotify e YouTube Music, adição de rádio manual por URL, reordenação de favoritos e Exportar/Importar Backup JSON
- **Widget na tela inicial** (`OndaWidgetProvider`) com controle play/pause e acesso direto a 4 favoritos
- **Android Auto turbinado:** pastas *Favoritos*, *Recentes*, *Podcasts*, *Espanha*, *Brasil*, *EUA & Reino Unido* e *Gêneros* (com controles `-15s / +30s` para podcasts)

### v1.1 (done)
- **Novos Widgets Material You (dinâmicos com o wallpaper no Android 12+):**
  - **Onda · Compacto (4×1):** capa da estação atual, título/música ao vivo e controles `⏮ ⏯ ⏭` sem espaços desperdiçados
  - **Onda · Favoritas (4×2 / 4×3):** cabeçalho enxuto + **grade rolável (`GridView`) com ícones/logos reais de TODAS as favoritas** (4 por linha), preenchendo 100% da altura sem espaços em branco e destacando a rádio tocando
- **Android Auto v1.1 (100% APIs oficiais Media3):**
  - Exatamente **4 abas principais com ícones vetoriais** (*Favoritos*, *Explorar*, *Podcasts*, *Recentes*) — elimina o submenu *"Mais ▾"*
  - Layout em **Grade de logos grandes** (`CONTENT_STYLE_GRID`) para rádios + **Category Grid** com capas próprias geradas pelo `ArtworkProvider` para Países (*Espanha*, *Brasil*, *EUA & Reino Unido*) e Gêneros Mundiais (*Rock Mundial*, *Classic Rock*, *Indie*, etc.)
  - Seções agrupadas (`CONTENT_STYLE_GROUP_TITLE`), barra de progresso nativa (`COMPLETION_PERCENTAGE`) e selo oficial `Offline` (`DOWNLOAD_STATUS`) para Podcasts
  - Ações rápidas extras na tela *Tocando Agora* do carro: **Reconectar ao vivo** (Rádio) e **Velocidade `1x–2x`** (Podcast)

### Roadmap (próximas fases)
- [ ] **Login opcional com Google (Firebase Auth + Firestore):** sincronização em nuvem de favoritos, assinaturas e progresso de podcasts, histórico de músicas e preferências entre múltiplos aparelhos — mantendo 100% de funcionamento local sem obrigar login
- [ ] **Chromecast / Google Cast** para reprodução em TVs e caixas de som inteligentes
- [ ] **Despertador / Alarme com rádio**
- [ ] **R8 minify** para reduzir tamanho dos builds assinados (CI/CD no GitHub Actions já configurado em `.github/workflows/build.yml`)
- [ ] **Experimento:** atenuação automática de volume durante marcadores de comerciais Triton/AdsWizz (quando suportado pela emissora)

## 6. Install and updates
- **Required for Android Auto.** Android Auto hides media apps that weren't installed from the Play Store. On the phone:
  open Settings → Connected devices → Connection preferences → Android Auto, tap **Version** about 10 times, then
  ⋮ → Developer settings → check **Unknown sources**. Reconnect the car. If Onda still doesn't show up, enable it in
  Android Auto → Customize launcher. Do this again if Android Auto's data is ever cleared
- Every push to `main` produces a downloadable APK under **Actions → run → Artifacts**
- Tag `v0.1.0` and similar to create a **GitHub Release** with the APK attached
- On the phone, use [Obtainium](https://github.com/ImranR98/Obtainium) pointed at `https://github.com/mcello23/onda` Releases to get
  automatic updates (no token needed since the repo is public)

## 7. Signing
Release APKs are signed with a private keystore kept out of Git (`*.jks` and `keystore.properties` are gitignored):
- **CI:** stored in GitHub Actions Secrets (`SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) and decoded at build time.
- **Local:** configured via a gitignored `keystore.properties` file in the repo root (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).
- Without signing credentials (e.g. external clones or fork PRs), `./gradlew assembleRelease` falls back to an unsigned APK and debug builds work normally.

## 8. Manual test checklist (Phase 1)
1. Search "Cadena SER" (or one of your stations). Results load and tapping one plays within about 3 seconds
2. Add 3 favorites, kill the app and reopen it. The favorites are still there
3. Lock the screen and listen for 30 minutes. Playback continues
4. Use next and previous on the notification and on Bluetooth headphones. It cycles through favorites
5. Unplug wired headphones or disconnect Bluetooth. Playback pauses
6. Take an incoming call. Playback pauses or ducks, then resumes
7. Stop, wait 5 minutes, then play. It resumes at live, not 5 minutes behind

## 9. Decisions
1. **Repo name and visibility:** `mcello23/onda`, public (MIT)
2. **Signing approach:** private keystore in GitHub Actions Secrets + gitignored local `keystore.properties` (section 7)
