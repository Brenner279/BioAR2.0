# BioAR — Atlas de Anatomia 3D (Android offline)

O BioAR mantém os quatro modelos anatômicos construídos em Three.js (coração, cérebro, pulmões e sistema digestório), animações, seleção de estruturas, pinos de identificação, quiz e narração.

## Site e aplicativo

O `index.html` da raiz continua sendo a versão web publicada no GitHub Pages. O build Android gera uma cópia isolada em `.offline-build`, substitui as referências externas por dependências locais e compila essa cópia para dentro do APK. Assim, a publicação web não é afetada pela preparação do aplicativo.

## Funcionamento offline

O APK empacota:
- Three.js;
- Tailwind CSS compilado em arquivo estático;
- fontes Nunito;
- ícones Phosphor (pesos Bold e Fill);
- HTML, CSS e JavaScript do Atlas.

O workflow configura a permissão de câmera e remove a permissão de internet do manifesto principal do aplicativo. O Atlas 3D, as animações e o quiz funcionam sem rede.

### Narração

A narração usa a voz de síntese de fala instalada no Android. O BioAR seleciona somente uma voz marcada pelo sistema como local e em português. Para ouvir sem internet, instale previamente uma voz de português (Brasil) offline nas configurações de Texto para fala do aparelho. Se não houver uma voz local compatível, o BioAR informa isso e não tenta usar uma voz online.

### Câmera

A câmera é opcional e depende da permissão concedida pelo Android. Ela mostra a imagem da câmera traseira como fundo; esta versão ainda não inclui rastreamento ARCore de marcadores ou superfícies.

## Gerar o APK pelo GitHub

Cada push na branch `main` que altere arquivos do aplicativo inicia o workflow **Build BioAR Android APK**. Também é possível iniciar manualmente em **Actions → Build BioAR Android APK → Run workflow**.

Quando o workflow terminar:
1. Abra a execução mais recente em **Actions**.
2. Na seção **Artifacts**, baixe `BioAR-offline-APK`.
3. Extraia o arquivo `app-debug.apk` e instale no Android.

O APK de depuração é instalável por sideload. Para distribuição na Play Store, será necessária uma versão release assinada.

## Desenvolvimento local

Requisitos: Node.js 24, JDK 21 e Android SDK 36.

```bash
npm install
npm run build
npx cap add android
npx cap sync android
npx cap open android
```

Para compilar o APK de depuração em Linux/macOS:

```bash
npm run android:apk
```

Após alterações nos arquivos web, execute novamente `npm run android:sync` antes de compilar.

## Identificador Android

`br.com.michelbrenner.bioar`
