# Privacy Policy — Onda

**Last updated:** November 27, 2026  
**Package ID:** `io.github.mcello23.onda`

## English

### 1. Summary
Onda is a free, open-source radio and podcast player for Android and Android Auto. **Onda does not collect, store, sell, or share any personal data.** There are no user accounts, no advertisements, no analytics SDKs, and no crash or behavioral trackers.

### 2. Data Stored Locally on Your Device
All app data stays exclusively on your device:
- Favorite stations, recently played stations, and ICY song history
- Podcast subscriptions, playback progress, and offline downloaded episodes
- App appearance, language, audio quality, buffer, and equalizer settings

You may export or import a local JSON backup at any time from **Settings → Local Backup**. Android Auto Backup (if enabled in your device settings) backs up only non-sensitive app preferences (`onda.xml`) and excludes caches and downloaded audio files. Uninstalling Onda removes all locally stored app data.

### 3. Third-Party Network Requests
Because Onda streams audio and discovers stations and podcasts directly from third-party services, your device connects directly to:
- **Radio Browser API (`*.api.radio-browser.info`):** to search radio stations, browse countries/genres, refresh stream URLs, and register anonymous station clicks.
- **Apple iTunes Search API (`itunes.apple.com`):** to search podcasts and fetch featured podcast charts.
- **Google Favicon Service (`www.google.com/s2/favicons`):** as an automatic fallback when a station does not provide a working logo image.
- **Radio Broadcasters & Podcast Hosts:** directly when streaming live audio, fetching RSS feeds, downloading podcast episodes, or loading station/podcast artwork.

Those third-party servers automatically receive standard network information required for HTTP/HTTPS communication (such as your IP address and the app User-Agent `Onda/1.0`) under their own privacy policies. Onda does not operate any proxy or developer backend server.

### 4. Permissions
- `INTERNET` and `ACCESS_NETWORK_STATE`: stream live radio/podcasts and adapt quality between Wi-Fi and mobile data.
- `WAKE_LOCK`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: keep audio playback running reliably in the background and with the screen locked.
- `POST_NOTIFICATIONS`: display playback controls in the notification shade on Android 13+.

### 5. Children's Privacy
Onda is a general-audience media player and is not directed at children under 13. Live radio streams and public podcast RSS feeds are controlled by independent third-party broadcasters.

### 6. Contact
Questions or privacy requests can be submitted via the project repository:  
https://github.com/mcello23/onda/issues

---

## Português (Brasil)

### 1. Resumo
O Onda é um reprodutor gratuito e de código aberto de rádios ao vivo e podcasts para Android e Android Auto. **O Onda não coleta, armazena, vende ou compartilha nenhum dado pessoal.** Não há contas de usuário, anúncios, SDKs de analytics ou rastreadores.

### 2. Dados Armazenados Localmente no Aparelho
Todos os dados permanecem exclusivamente no seu dispositivo:
- Estações favoritas, histórico de estações recentes e histórico de músicas (ICY)
- Assinaturas de podcasts, progresso de reprodução e episódios baixados offline
- Preferências de aparência, idioma, qualidade de áudio, buffer e equalizador

Você pode exportar ou importar um backup JSON local em **Opções → Backup Local**. O Backup do Android (quando ativado no sistema) inclui apenas preferências não sensíveis (`onda.xml`), excluindo caches e áudios baixados. Desinstalar o Onda apaga todos os dados locais.

### 3. Conexões Diretas com Terceiros
Para buscar e reproduzir conteúdo diretamente sem servidor intermediário, seu aparelho se conecta a:
- **Radio Browser API (`*.api.radio-browser.info`):** busca de estações, países, gêneros e contagem anônima de cliques.
- **Apple iTunes Search API (`itunes.apple.com`):** busca e catálogo de podcasts.
- **Google Favicon Service (`www.google.com/s2/favicons`):** fallback automático de logotipos quando a emissora não fornece ícone válido.
- **Emissoras de Rádio e Servidores de Podcast:** diretamente ao reproduzir streams ao vivo, ler feeds RSS, baixar episódios ou carregar capas.

Esses serviços terceiros recebem informações técnicas padrão de rede (como endereço IP e cabeçalho `User-Agent`) conforme suas próprias políticas de privacidade.

### 4. Contato
Dúvidas ou solicitações de privacidade podem ser enviadas pelo repositório do projeto:  
https://github.com/mcello23/onda/issues
