# Friend Setup Guide — Cosmetics Shop Frontend

If you cloned the repo and see errors (especially `NG0203`), follow these steps exactly.

## 1. Verify You Have the Environment Files

These files MUST exist. They are **NOT** ignored by `.gitignore`.

```bash
cd cosmeticsshop-frontend/src/environments
ls -la
```

You should see:
- `environment.ts`
- `environment.prod.ts`

If they are missing, your clone is corrupted. Do a **fresh clone**:

```bash
cd ..
rm -rf cosmeticsshop
git clone https://github.com/mariiafff/cosmeticsshopnew.git
cd cosmeticsshop/cosmeticsshop-frontend
```

## 2. Exact Contents of Environment Files

**`src/environments/environment.ts`** (development — used by `ng serve`):

```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api'
};
```

**`src/environments/environment.prod.ts`** (production build):

```typescript
export const environment = {
  production: true,
  // TODO: Replace with your deployed backend URL (e.g. Render, Railway, AWS, etc.)
  // If backend and frontend are served from the same origin, use '/api'
  apiBaseUrl: 'https://api.your-domain.com/api'
};
```

> **Important:** If you run `ng build` (production), change `apiBaseUrl` to your real backend URL or the app will fail to connect.

## 3. Clean Install (CRITICAL for NG0203)

The `NG0203` error is almost always caused by **stale build cache** or **mismatched Angular packages**.

```bash
cd cosmeticsshop-frontend

# 1. Delete ALL generated files
rm -rf dist/
rm -rf .angular/
rm -rf node_modules/
rm package-lock.json

# 2. Reinstall exact versions
npm install

# 3. Verify Angular version matches the project
cat node_modules/@angular/core/package.json | grep version
# Expected: ~21.1.0 or ~21.2.x

# 4. Start development server (uses environment.ts, NOT environment.prod.ts)
npm start
```

## 4. Common Mistakes to Avoid

| Mistake | Why It Breaks |
|---------|---------------|
| Running `ng build` then opening `dist/` directly | SSR app needs `npm run serve:ssr:cosmeticsshop-frontend` or a proper host |
| Using an old global `ng` command | Always use `npx ng` or `npm run` to use the project's CLI version |
| Skipping `rm -rf .angular/` | Angular's build cache can hide old compilation errors |
| Modifying `environment.ts` for production | Use `environment.prod.ts` for production builds |

## 5. If NG0203 Still Appears

Run this to check if `inject()` is being called outside an injection context:

```bash
grep -rn "inject(" src/app/ --include="*.ts"
```

All `inject()` calls must be inside:
- A class with `@Injectable()`
- A class with `@Component()`
- A factory/provider function

If you see `inject()` at the top level of a file (not inside a class), that is the bug.

