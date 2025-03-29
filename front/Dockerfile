# 빌드 스테이지
FROM node:20-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# 종속성 파일 복사
COPY package.json package-lock.json ./

# 종속성 설치
RUN npm ci

# 소스 파일 복사
COPY . .

# 환경 변수 설정 (빌드 인자로 전달받음)
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}

# Next.js 애플리케이션 빌드
RUN npm run build

# 실행 스테이지
FROM node:20-alpine AS runner

WORKDIR /app

# 운영 환경을 위한 설정
ENV NODE_ENV=production

# Next.js에 필요한 시스템 종속성 설치
RUN apk add --no-cache libc6-compat

# 애플리케이션 사용자 생성
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# 빌드된 파일만 복사
COPY --from=builder --chown=nextjs:nodejs /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/public ./public

# nextjs 사용자로 전환
USER nextjs

# 포트 3000으로 실행
EXPOSE 3000

# 환경 변수 설정 (런타임에 사용)
ENV PORT=3000

# Next.js 애플리케이션 실행
CMD ["npm", "start"]