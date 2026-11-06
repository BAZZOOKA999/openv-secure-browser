# OpenV — navegador Android

[![Android APK](https://github.com/BAZZOOKA999/openv-secure-browser/actions/workflows/android.yml/badge.svg)](https://github.com/BAZZOOKA999/openv-secure-browser/actions/workflows/android.yml)

Navegador Android em Kotlin e Jetpack Compose, com abas, favoritos, histórico local, pesquisa com diferentes provedores e uma central de atalhos OSINT. Este repositório contém o projeto exportado do AI Studio, adaptado para compilação e distribuição pelo GitHub.

## Instalar

**[Baixar OpenV.apk](https://github.com/BAZZOOKA999/openv-secure-browser/releases/download/apk-latest/OpenV.apk)**

Requer **Android 10 ou superior** e Android System WebView atualizado. Abra o APK e autorize a instalação pela origem escolhida quando solicitado pelo Android. A versão atual é **0.1-alpha**.

## Recursos

- Navegação WebView com abas e modo computador.
- Favoritos e histórico armazenados localmente com Room.
- Pesquisa via DuckDuckGo, Google, Brave Search e Bing.
- Página inicial, preferências de interface e controles de cookies.
- Bloqueio de certificados TLS inválidos, conteúdo misto e acesso local pelo WebView.
- Central de links e consultas OSINT e diagnóstico de conectividade.
- Telas de configuração de VPN e DNS preparadas para desenvolvimento futuro.

## O que ainda não está pronto

**VPN, mudança de IP e kill switch não estão implementados.** Os perfis incluídos são exemplos sem servidores operacionais. A seleção de DNS é salva, mas não altera as consultas de rede ou do WebView.

As abas privadas evitam o registro no histórico do aplicativo, mas **compartilham cookies com as outras abas**. Não equivalem a uma sessão com isolamento completo nem garantem anonimato. A troca de identificador interno de sessão não troca o IP.

Esta versão não possui integração própria para downloads, envio de arquivos, câmera/microfone, geolocalização ou vídeo em tela cheia. HTTP sem criptografia é bloqueado. Há limitações de compatibilidade com sites que exigem recursos de navegadores completos. A disponibilidade de Safe Browsing depende do provedor WebView.

## Compilar e verificar

Ferramentas: Java 17, Gradle 9.3.1 e Android SDK (compile SDK 36.1). O workflow instala as ferramentas, executa `gradle assembleRelease testDebugUnitTest` e publica o APK assinado em Releases quando os testes passam.

Para uma compilação local de desenvolvimento, execute `gradle assembleDebug testDebugUnitTest`. A compilação release usa `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`, com alias `upload`. No GitHub Actions, a chave e a senha ficam nos secrets `ANDROID_KEYSTORE_BASE64` e `ANDROID_KEYSTORE_PASSWORD`; nunca são incluídas no repositório. A chave é persistente para permitir atualizações do APK.

Não é necessário configurar Gemini ou Firebase: o código atual não usa esses serviços e as dependências desnecessárias foram removidas da compilação.

Os testes existentes verificam nome do aplicativo, criação de abas e construção de pesquisas. Consulte os relatórios do Actions. Testes automatizados não substituem validação de instalação e uso em aparelho; o projeto não oferece garantia de segurança absoluta.
