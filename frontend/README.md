# Listan
This is the frontend for [listan-server](https://github.com/axeltlarsson/listan-server).
On first setup do `npm install`.

## Running locally
To run locally i dev mode, uncomment appropriate directives in webpack.config.js, then
use `npm start` which will set up a local dev server with hot code reloading.

## Deploying in Production
- Pull latest code
- `npm install`
- `WS_API_URL=<domain/api/ws> LOGIN_URL=<domain/api/login> npm run build:prod` to generate the JS bundle
in `dist`.

Then the whole project can be built with docker-compose, see [the README.md](../README.md).
