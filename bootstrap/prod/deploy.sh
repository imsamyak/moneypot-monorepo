#!/usr/bin/env bash
# Deploys MoneyPot to one server over ssh: bash bootstrap/prod/deploy.sh <host> <path-to-key.pem> [user]
# Needs on this machine: Java 21 + Maven, pnpm, ssh and tar. The server needs Docker (the first run installs it).
set -euo pipefail
HOST="${1:?host}"; KEY="${2:?key}"; USER_NAME="${3:-ec2-user}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/build/prod"
SSH="ssh -i $KEY -o StrictHostKeyChecking=accept-new $USER_NAME@$HOST"

echo "== build the services"
(cd "$ROOT/server/platform" && mvn -o -q -DskipTests package -pl service/identity,service/wallet,service/pot)
echo "== bundle"
rm -rf "$OUT" && mkdir -p "$OUT/jars" "$OUT/websrc"
for s in identity wallet pot; do cp "$ROOT"/server/platform/service/$s/target/*-SNAPSHOT.jar "$OUT/jars/$s.jar"; done
# the web app is built on the server (Linux), from the workspace sources
tar -C "$ROOT" --exclude=node_modules --exclude=.next --exclude=dist -cf - package.json pnpm-lock.yaml pnpm-workspace.yaml \
  client/packages client/service-sdk client/apps/moneypot | tar -C "$OUT/websrc" -xf -
cp "$ROOT"/bootstrap/prod/{docker-compose.yml,Dockerfile.java,Dockerfile.web,init.sql,nginx.conf} "$OUT/"

echo "== upload"
tar -C "$OUT" -czf - . | $SSH 'rm -rf ~/moneypot && mkdir -p ~/moneypot && tar -xzf - -C ~/moneypot'

echo "== start"
# buildx is needed by `docker compose build` and is not part of Amazon Linux's docker package
$SSH 'PLUGINS=/usr/local/lib/docker/cli-plugins; sudo mkdir -p $PLUGINS; if [ ! -x $PLUGINS/docker-buildx ]; then
  TAG=$(curl -fsSL https://api.github.com/repos/docker/buildx/releases/latest | grep -m1 "\"tag_name\"" | cut -d"\"" -f4)
  sudo curl -fsSL "https://github.com/docker/buildx/releases/download/$TAG/buildx-$TAG.linux-amd64" -o $PLUGINS/docker-buildx
  sudo chmod +x $PLUGINS/docker-buildx
fi'
$SSH "cd ~/moneypot && [ -f ~/.moneypot.env ] || { printf 'DB_PASSWORD=%s\nRABBITMQ_PASSWORD=%s\n' \$(openssl rand -hex 16) \$(openssl rand -hex 16) > ~/.moneypot.env; chmod 600 ~/.moneypot.env; }; cp ~/.moneypot.env .env && echo PUBLIC_HOST=$HOST >> .env"
$SSH "cd ~/moneypot && sudo docker compose up -d --build"
$SSH "cd ~/moneypot && sudo docker compose ps"
echo "Started. Check it: curl -I http://$HOST"
