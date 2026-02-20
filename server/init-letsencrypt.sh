#!/usr/bin/env bash
# Run once on first deploy to obtain the initial Let's Encrypt certificate.
# Usage: ./init-letsencrypt.sh your-domain.com your@email.com

set -e
DOMAIN=$1
EMAIL=$2

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "Usage: $0 <domain> <email>"
  exit 1
fi

# Start nginx (HTTP only) so certbot can complete the ACME challenge
docker compose -f docker-compose.prod.yml up -d nginx

# Obtain certificate
docker compose -f docker-compose.prod.yml run --rm certbot certonly \
  --webroot --webroot-path=/var/www/certbot \
  --email "$EMAIL" --agree-tos --no-eff-email \
  -d "$DOMAIN"

# Restart nginx so it picks up the certificate and enables HTTPS
docker compose -f docker-compose.prod.yml restart nginx

echo "Certificate obtained. Run 'docker compose -f docker-compose.prod.yml up -d' to start all services."
