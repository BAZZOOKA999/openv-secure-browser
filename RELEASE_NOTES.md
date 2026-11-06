OpenV 0.2-alpha — atualização para Android 10 ou superior.

Baixe **OpenV.apk** e instale por cima da versão anterior. O pacote mantém o identificador e a chave de assinatura, com versionCode 2.

Novidades: downloads, busca na página, zoom, bloqueio de rastreadores por lista, consultas DNS/HTTP/IP/RDAP, ferramentas EXIF, relatórios OSINT e perfis salvos.

Foi adicionada migração do banco para preservar favoritos e histórico, verificada em teste automatizado. As correções anteriores de abas, botão Voltar e Safe Browsing foram preservadas.

**VPN, troca de IP e kill switch continuam indisponíveis.** O serviço recebido não encaminhava os pacotes e foi desabilitado para não interromper a rede. As consultas DoH são independentes e não alteram o DNS do WebView. Abas privadas ainda compartilham cookies com as demais abas.

APK compilado e testado no GitHub Actions. Testes em aparelho e compatibilidade com sites ainda precisam de validação.
