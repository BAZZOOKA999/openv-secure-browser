# OpenV — navegador Android

[![Android APK](https://github.com/BAZZOOKA999/openv-secure-browser/actions/workflows/android.yml/badge.svg)](https://github.com/BAZZOOKA999/openv-secure-browser/actions/workflows/android.yml)

Navegador Android em Kotlin e Jetpack Compose, com abas, favoritos, histórico local, pesquisa com diferentes provedores e uma central de atalhos OSINT. Este repositório contém o projeto exportado do AI Studio, adaptado para compilação e distribuição pelo GitHub.

## Instalar

**[Baixar OpenV.apk](https://github.com/BAZZOOKA999/openv-secure-browser/releases/download/apk-latest/OpenV.apk)**

Requer **Android 10 ou superior** e Android System WebView atualizado. Abra o APK e autorize a instalação pela origem escolhida quando solicitado pelo Android. A versão atual é **0.3-alpha**.

## Novidades da versão 0.3-alpha

Importada de `openv-secure-browser - 2026-11-06T201444.526.zip`.

- Menu ampliado com visualização do HTML atual, extração de imagens e detecção de links de mídia presentes na página.
- Modo leitura, modo escuro de páginas, controle do bloqueio de rastreadores e execução manual de JavaScript na página.
- Telas para limpeza de cookies, permissões Android e análise indicativa de URL/cabeçalhos.
- Mantém a migração dos dados, as correções de navegação e a assinatura das versões anteriores; `versionCode=3`.

A análise de site é heurística, não antivírus nem garantia de legitimidade. A detecção de mídia não baixa streams protegidos, DRM ou URLs blob/data. As ferramentas chamadas de extensões são recursos internos, não compatibilidade com extensões de Chrome/Firefox. Conceder permissões ao aplicativo não libera automaticamente câmera, microfone ou localização para sites; o WebView ainda nega esses pedidos. O motor VPN continua indisponível, pois ainda não encaminha nem criptografa os pacotes capturados.

## Novidades da versão 0.2-alpha

Atualização importada do arquivo `openv-secure-browser - 2026-11-06T192938.787.zip`.

- Downloads pelo gerenciador do Android e lista dos downloads iniciados na sessão.
- Busca dentro da página, zoom e bloqueio de uma lista local de rastreadores.
- Consultas DNS-over-HTTPS, inspeção de cabeçalhos HTTP, consultas de IP/RDAP e ferramentas OSINT ampliadas.
- Leitura de EXIF e criação de cópias JPEG sem metadados; relatórios OSINT e perfis salvos localmente.
- Atualização do banco de dados sem apagar favoritos e histórico, com teste de migração.
- Mesma chave de assinatura da versão anterior, `versionCode=2`, permitindo instalar por cima do APK publicado anteriormente.

## Recursos

- Navegação WebView com abas e modo computador.
- Favoritos e histórico armazenados localmente com Room.
- Pesquisa via DuckDuckGo, Google, Brave Search e Bing.
- Página inicial, preferências de interface e controles de cookies.
- Bloqueio de certificados TLS inválidos, conteúdo misto e acesso local pelo WebView.
- Central de links e consultas OSINT e diagnóstico de conectividade.
- Telas de configuração de VPN e DNS preparadas para desenvolvimento futuro.

## O que ainda não está pronto

**VPN, mudança de IP e kill switch não estão implementados.** Os perfis incluídos são exemplos, sem operação verificada. O serviço VPN recebido apenas consumia pacotes da interface TUN, sem encaminhamento ou criptografia; sua ativação foi desabilitada para evitar cortar a conexão e indicar proteção inexistente. As ferramentas DoH fazem consultas próprias, mas a seleção de DNS não altera o tráfego do WebView ou do sistema. Provedores DoH precisam aceitar o formato JSON usado pelo cliente.

As abas privadas evitam o registro no histórico do aplicativo, mas **compartilham cookies com as outras abas**. Não equivalem a uma sessão com isolamento completo nem garantem anonimato. A troca de identificador interno de sessão não troca o IP.

Os downloads usam o gerenciador do Android; a lista interna é apenas da sessão e não acompanha o progresso. Downloads que dependem de login podem falhar. Esta versão não possui integração própria para envio de arquivos, câmera/microfone, geolocalização ou vídeo em tela cheia. HTTP sem criptografia é bloqueado. Há limitações de compatibilidade com sites que exigem recursos de navegadores completos. A disponibilidade de Safe Browsing depende do provedor WebView.

## Compilar e verificar

Ferramentas: Java 17, Gradle 9.3.1 e Android SDK (compile SDK 36.1). O workflow instala as ferramentas, executa `gradle assembleRelease testDebugUnitTest` e publica o APK assinado em Releases quando os testes passam.

Para uma compilação local de desenvolvimento, execute `gradle assembleDebug testDebugUnitTest`. A compilação release usa `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`, com alias `upload`. No GitHub Actions, a chave e a senha ficam nos secrets `ANDROID_KEYSTORE_BASE64` e `ANDROID_KEYSTORE_PASSWORD`; nunca são incluídas no repositório. A chave é persistente para permitir atualizações do APK.

Não é necessário configurar Gemini ou Firebase: o código atual não usa esses serviços e as dependências desnecessárias foram removidas da compilação.

Os testes verificam nome do aplicativo, criação de abas, construção de pesquisas e preservação de favoritos/histórico na migração do banco. Consulte os relatórios do Actions. Testes automatizados não substituem validação de instalação e uso em aparelho; o projeto não oferece garantia de segurança absoluta.

As consultas de rede podem transmitir o alvo pesquisado a serviços externos (por exemplo, provedores DoH, IP/RDAP e sites de perfis). Respostas de perfis por status HTTP não comprovam identidade ou existência de conta. Não é uma auditoria completa de segurança.
