# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CampusTrade — campus second-hand trading platform. Monorepo with Spring Boot 3 backend and Vue 3 frontend.

## Commands

```bash
# One-click startup (H2 in-memory, no MySQL needed)
.\start.ps1 -Dev

# MySQL mode
.\start.ps1

# Stop all services
.\stop.ps1
```

**Manual startup:**

```bash
# Backend (H2 dev mode)
cd campus-trade-server
mvn package -DskipTests -q
java -jar target/campus-trade-server-1.0.0.jar --spring.profiles.active=dev

# Frontend
cd campus-trade-web
npm install
npm run dev
```

**Build only:**
```bash
cd campus-trade-server && mvn compile -q    # backend
cd campus-trade-web && npm run build        # frontend → dist/
```

No automated tests exist in this project.

## Architecture

### Backend Layers (Spring Boot 3.2 + MyBatis-Plus 3.5)

```
controller → service/impl → mapper (extends BaseMapper<T>)
     ↓
  Result<T> / PageResult<T>  ← uniform response wrapper
```

- **Auth flow**: `JwtInterceptor` extracts `Authorization: Bearer <token>` → `LoginUser` ThreadLocal → `LoginUser.getRequiredUserId()` in service layer throws 401 if null. BCrypt via `spring-security-crypto` (not full Spring Security).
- **Profiles**: `dev` = H2 in-memory, `prod` = MySQL. Default `application.yml` has NO datasource config — profiles add their own. The H2 schema is at `db/schema-h2.sql`; MySQL schema at `db/campus_trade.sql`.
- **Dynamic queries**: `LambdaQueryWrapper` with conditional `.eq(cond, column, value)`. Sort via `orderByAsc`/`orderByDesc`. Keyword search uses `.like` on title OR description.

### Frontend Layers (Vue 3 + Element Plus + Vite + Pinia)

```
views/ → components/ → api/*.js → request.js (axios) → backend
                ↓
           store/user.js (Pinia, localStorage-persisted)
```

- **API layer**: `request.js` interceptor extracts `response.data` and returns `{code, message, data}`. Calling code accesses `res.data` for the payload. The production `BASE_URL` is hardcoded to the Render backend URL; dev mode uses Vite proxy.
- **Auth guard**: `router/index.js` `beforeEach` checks localStorage token; redirects to `/login` for `meta.requiresAuth` routes.
- **Hash router**: `createWebHashHistory` — all URLs use `/#/` prefix.

### API Response Format

All backend responses follow `{code: 200, message: "success", data: ...}`.

**Critical pattern** — the axios interceptor already unwraps one level, so calling code sees:
- `res` = `{code, message, data}` (from interceptor)
- `res.data` = the actual payload

For **paginated** endpoints (product list, user products, bookmarks), `res.data` is `{records: [...], total: N, page: N, size: N, pages: N}`. The array is at `res.data.records`, NOT directly at `res.data`.

For **single-object** endpoints (login, user detail, product detail), `res.data` IS the object directly.

### Key Backend Behaviors

- `GET /api/product/{id}` increments `view_count` and fills `bookmarkCount`, `messageCount`, `isBookmarked` (null-safe for anonymous users).
- `POST /api/product/{id}/bookmark` toggles: inserts if not exists, deletes if exists. Returns `{bookmarked: true/false}`.
- Product write operations (update/delete/status) verify `product.userId == LoginUser.getRequiredUserId()`, throw `BusinessException(403)` otherwise.
- `User` entity uses `@TableName("\`user\`")` because `user` is a reserved word in H2.
- `Product.condition` field uses `@TableField("\`condition\`")` for the same reason.

### Deployment

- **Frontend**: Vercel, auto-deploys from GitHub `main` branch. Root Directory must be set to `campus-trade-web` in Vercel project settings.
- **Backend**: Render, Dockerfile-based deployment. Uses H2 dev profile (data lost on restart). Cold start ~30s on free tier.
- **Config files**: `vercel.json` (frontend), `render.yaml` + `Dockerfile` (backend).

## Common Pitfalls

1. **`res.data` vs `res.data.records`**: Paginated API responses nest the array inside `records`. Always use `res.data?.records || res.data || []` as the safe fallback pattern when fetching lists.
2. **NaN price**: `Number(null).toFixed(2)` = `"NaN"`. Always guard with `product.price != null` before `Number()` conversion.
3. **H2 reserved words**: `user` and `condition` need backtick quoting in MyBatis-Plus annotations. The existing entity annotations already handle this — do not remove the backticks.
4. **Profile separation**: Never add datasource config to default `application.yml`. Use `application-dev.yml` (H2) or `application-prod.yml` (MySQL).
5. **Component mounting**: Vue components that fetch data need `onMounted(() => fetchX())`. Components like `MessageSection` rely on this to load data on page entry.
