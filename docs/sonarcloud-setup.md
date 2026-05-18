# SonarCloud setup (Lab 4)

CI uses [SonarCloud](https://sonarcloud.io) (free for public repositories). Self-hosted SonarQube on the cluster (`sonar.app.macsia.fun`) is optional and not used by the pipeline.

## 1. Import the project

1. Sign in at [sonarcloud.io](https://sonarcloud.io) with GitHub.
2. Create or use organization **macsia**.
3. **Analyze new project** → import `MacsiaProduction/DebtsApp`.
4. Confirm project key matches [`sonar-project.properties`](../sonar-project.properties):
   - `sonar.projectKey=MacsiaProduction_DebtsApp`
   - `sonar.organization=macsia`

## 2. Quality Gate `DebtsApp-Lab4`

In SonarCloud: **Quality Gates** → **Create** → name `DebtsApp-Lab4`.

| Metric | Operator | Value |
|--------|----------|-------|
| Coverage | is less than | 80% |
| Coverage on New Code | is less than | 80% |
| Reliability Rating on New Code | is worse than | A |
| Security Rating on New Code | is worse than | A |
| Maintainability Rating on New Code | is worse than | A |
| Security Hotspots Reviewed on New Code | is less than | 100% |

Assign **DebtsApp-Lab4** to project `MacsiaProduction_DebtsApp` (Project Settings → Quality Gate).

## 3. GitHub configuration

| Type | Name | Value |
|------|------|-------|
| Secret | `SONAR_TOKEN` | User token from SonarCloud (My Account → Security) |
| Variable | `SONARQUBE_URL` | `https://sonarcloud.io` |

If `SONARQUBE_URL` is unset, CI defaults to `https://sonarcloud.io`.

## 4. Local verification

```bash
# Backend (requires Java 21)
cd backend
./gradlew test jacocoTestReport jacocoTestCoverageVerification

# Frontend
cd frontend
npm ci
npm test -- --watchAll=false --coverage
```

Dashboard: `https://sonarcloud.io/project/overview?id=MacsiaProduction_DebtsApp`
