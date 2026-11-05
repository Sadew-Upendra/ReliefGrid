# --- Dependencies ---
FROM node:24-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# --- Build ---
FROM node:24-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* vars are inlined into the client bundle at build time, not read at runtime,
# so this must be a build ARG passed in via docker-compose's build.args, not just an env var.
ARG NEXT_PUBLIC_API_URL=""
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
RUN npm run build

# --- Run ---
FROM node:24-alpine
RUN addgroup -S reliefgrid && adduser -S reliefgrid -G reliefgrid
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/public ./public
COPY --from=build /app/.next ./.next
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package.json ./package.json
USER reliefgrid
EXPOSE 3000
CMD ["npm", "start"]
