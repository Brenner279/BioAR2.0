# Assistente Educacional Offline

Aplicativo em desenvolvimento para oferecer apoio educacional e especialmente Matemática, com processamento local no Android.

## Objetivo
Após baixar e instalar os componentes necessários, o assistente deverá funcionar sem internet, com conversa por texto e voz, histórico local e respostas educacionais passo a passo.

## Dispositivo-alvo inicial
- Android: Realme RMX5313
- Memória informada: 8 GB (confirmar se é RAM física ou inclui expansão virtual)
- Desenvolvimento: diretamente no celular

## Arquitetura planejada
- Interface: HTML, CSS e JavaScript
- Ambiente local Android: Termux
- Runtime de IA: llama.cpp
- Modelo: GGUF quantizado, inicialmente entre 1B e 1,5B parâmetros
- Reconhecimento de fala: avaliar Vosk e whisper.cpp no aparelho
- Síntese de voz: Android TextToSpeech na primeira versão; avaliar Piper posteriormente
- Histórico: IndexedDB
- GitHub Pages: demonstração/entrega da interface; não executa o modelo de IA.

## Estado atual
Etapa 1: interface inicial responsiva criada.
O motor de IA ainda não está integrado. A interface publicada no GitHub Pages não deve ser confundida com o assistente offline completo.

## Próximas etapas
1. Preparar Termux no Android.
2. Testar arquitetura e memória disponíveis.
3. Instalar/compilar llama.cpp e testar um modelo pequeno.
4. Criar API local para a interface.
5. Integrar voz e histórico.
6. Validar operação em modo avião.

## Privacidade e segurança
Os modelos e conversas deverão permanecer no dispositivo. Não inserir chaves privadas ou credenciais no repositório.
