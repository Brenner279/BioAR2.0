# BioAR 2.0 — scanner ARCore com marcador A1

Esta etapa integra um scanner nativo ARCore ao APK Android. O Atlas 3D continua sendo a experiência principal e permanece acessível como alternativa.

## Teste no Samsung Galaxy A54

1. Instale o APK gerado pelo workflow mais recente.
2. Imprima `public/ar-markers/heart-marker.png` em **14 × 14 cm**, sem cortar, esticar ou alterar a proporção.
3. Abra o BioAR e toque em **Modo RA**.
4. Autorize a câmera, mantenha o cartão plano e ilumine bem a superfície.
5. Aponte a câmera até aparecer **MARCADOR A1 RECONHECIDO**.
6. Toque em **Finalizar teste** para retornar ao Atlas.

## Escopo desta prova de conceito

Valida inicialização ARCore, feed de câmera da sessão nativa e reconhecimento local do marcador A1. **Ainda não desenha o coração 3D ancorado no marcador**; essa será a próxima etapa, que usará a pose do marcador para integrar o modelo anatômico.

A base de imagens é empacotada no APK e o reconhecimento ocorre no aparelho. O APK permanece sem permissão INTERNET. O modo AR é opcional; o Atlas 3D continua funcionando como fallback.

## Qualidade do marcador

Use a folha plana, sem brilho/reflexos e sem dobras. O alvo deve ocupar uma parte significativa do enquadramento; aproxime o aparelho sem cortar as bordas.
