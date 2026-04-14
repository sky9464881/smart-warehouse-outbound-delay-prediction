# WareBorad

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

## Backend / API

This frontend calls the Spring backend under `/api` (default target: `http://localhost:8080`).

- Dev/preview uses Vite proxy: set `VITE_API_PROXY_TARGET` in `.env` (see `.env.example`).
- Direct calls (no proxy): set `VITE_API_BASE_URL` in `.env`.

### Compile and Hot-Reload for Development

```sh
npm run dev
```

If PowerShell blocks `npm` (ExecutionPolicy), use `npm.cmd run dev` instead.

### Compile and Minify for Production

```sh
npm run build
```
