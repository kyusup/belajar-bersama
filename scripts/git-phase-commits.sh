#!/usr/bin/env bash
# Create chronological phase commits for Belajar Bersama MVP.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  git init -b main
fi

if ! git remote get-url origin >/dev/null 2>&1; then
  git remote add origin git@github.com:kyusup/belajar-bersama.git
fi

phase_commit() {
  local message="$1"
  shift
  if [ "$#" -eq 0 ]; then
    return 0
  fi
  git add "$@"
  if git diff --cached --quiet; then
    echo "skip (nothing new): ${message%%$'\n'*}"
    return 0
  fi
  git commit -m "$message"
  echo "pushed phase: ${message%%$'\n'*}"
  git push -u origin main 2>/dev/null || git push origin main
}

API="apps/api/src/main/java/id/belajarbersama"
API_RES="apps/api/src/main/resources"
API_TEST="apps/api/src/test/java/id/belajarbersama"
WEB="apps/web"

phase_commit "Phase 0-2: Product foundation and executable skeleton

- Product constitution, domain model, architecture docs, ADRs
- Docker Compose, CI workflow, pnpm workspace, shared package
- Quarkus health/status, Flyway V1, audit and storage ports
- Next.js status page and API connectivity check" \
  .editorconfig .env.example .gitignore LICENSE NOTICE README.md CHANGELOG.md \
  CONTRIBUTING.md SECURITY.md CODE_OF_CONDUCT.md docker-compose.yml \
  .github infra scripts packages pnpm-lock.yaml pnpm-workspace.yaml package.json \
  docs/PRODUCT_CONSTITUTION.md docs/PRODUCT_PRINCIPLES.md docs/GOVERNANCE.md \
  docs/CONTENT_GOVERNANCE.md docs/VERIFICATION.md docs/MAKER_CHECKER.md \
  docs/COPYRIGHT_AND_CONTENT_LICENSE.md docs/PRIVACY_PRINCIPLES.md \
  docs/DOMAIN_MODEL.md docs/DOMAIN_GLOSSARY.md docs/ROLE_PERMISSION_CONCEPT.md \
  docs/CONTENT_LIFECYCLE.md docs/ARCHITECTURE.md docs/ARCHITECTURE_DECISIONS.md \
  docs/ARCHITECTURE_ASSUMPTIONS.md docs/DEVELOPMENT_SETUP.md docs/API_ARCHITECTURE.md \
  docs/SECURITY_ARCHITECTURE.md docs/DEPLOYMENT_ARCHITECTURE.md docs/PERMISSION_MODEL.md \
  docs/OPEN_DECISIONS.md docs/CONSISTENCY_CHECKLIST.md docs/README.md docs/BACKUP.md docs/adr \
  apps/api/pom.xml apps/api/mvnw apps/api/mvnw.cmd apps/api/.mvn apps/api/README.md \
  "$API_RES/application.properties" "$API_RES/db/migration/V1__foundation_audit.sql" \
  "$API/domain/audit" "$API/domain/error" "$API/domain/storage" \
  "$API/domain/search/SearchQuery.java" \
  "$API/application/platform" "$API/infrastructure/storage" \
  "$API/infrastructure/persistence/PostgresAuditRecorder.java" \
  "$API/infrastructure/persistence/JdbcSupport.java" \
  "$API/interfaces/rest/HealthResource.java" "$API/interfaces/rest/StatusResource.java" \
  "$API/interfaces/rest/CorrelationIdFilter.java" \
  "$API/interfaces/rest/DomainExceptionMapper.java" "$API/interfaces/rest/UnexpectedExceptionMapper.java" \
  "$API/interfaces/rest/dto/ApiErrorResponse.java" "$API/interfaces/rest/dto/HealthResponse.java" \
  "$API_TEST/infrastructure/storage" "$API_TEST/infrastructure/persistence/PostgresAuditRecorderTest.java" \
  "$API_TEST/domain/storage" "$API_TEST/domain/search" \
  "$API_TEST/interfaces/rest/PlatformResourceTest.java" \
  "$WEB/package.json" "$WEB/tsconfig.json" "$WEB/next.config.ts" "$WEB/next-env.d.ts" \
  "$WEB/.eslintrc.json" "$WEB/.prettierrc.json" "$WEB/.prettierignore" "$WEB/vitest.config.ts" \
  "$WEB/README.md" "$WEB/src/app/layout.tsx" "$WEB/src/app/globals.css" "$WEB/src/app/page.tsx" \
  "$WEB/src/app/status" "$WEB/src/components/PlatformStatusPanel.tsx" \
  "$WEB/src/components/PlatformStatusView.tsx" "$WEB/src/components/PlatformStatusView.test.tsx" \
  "$WEB/src/components/SiteHeader.tsx" "$WEB/src/lib/api/client.ts" "$WEB/src/lib/api/health.ts" \
  "$WEB/src/lib/i18n/id.ts" "$WEB/src/test"

phase_commit "Phase 3: Identity, RBAC, and competency verification

- Google/Apple OIDC BFF and session cookie
- RBAC catalog, verification workflow, authorization policies
- Flyway V2 identity schema" \
  "$API_RES/db/migration/V2__identity_rbac_verification.sql" \
  "$API/domain/identity" "$API/domain/verification" "$API/domain/authorization" "$API/domain/competency" \
  "$API/application/identity" "$API/application/verification" "$API/application/authorization" \
  "$API/infrastructure/auth" \
  "$API/infrastructure/persistence/PostgresUserRepository.java" \
  "$API/infrastructure/persistence/PostgresIdentityRepository.java" \
  "$API/infrastructure/persistence/PostgresAuthSessionRepository.java" \
  "$API/infrastructure/persistence/PostgresVerificationRepository.java" \
  "$API/infrastructure/persistence/PostgresRoleAssignmentRepository.java" \
  "$API/infrastructure/persistence/PostgresCompetencyRepository.java" \
  "$API/infrastructure/persistence/PostgresOauthStateRepository.java" \
  "$API/interfaces/http/SessionAuthFilter.java" "$API/interfaces/http/RequestAuthContext.java" \
  "$API/interfaces/rest/AuthResource.java" "$API/interfaces/rest/AdminResource.java" \
  "$API/interfaces/rest/VerificationResource.java" "$API/interfaces/rest/AuthorizationResource.java" \
  "$API/interfaces/rest/CompetencyResource.java" \
  docs/IDENTITY_ARCHITECTURE.md docs/AUTHENTICATION.md docs/RBAC.md \
  docs/PERMISSION_MATRIX.md docs/VERIFICATION_MODEL.md docs/AUTHORIZATION_POLICIES.md \
  "$WEB/src/app/masuk" "$WEB/src/app/akun" "$WEB/src/components/LoginPanel.tsx" \
  "$WEB/src/components/LoginPanel.test.tsx" "$WEB/src/components/AccountPanel.tsx" \
  "$WEB/src/components/AccountPanel.test.tsx" "$WEB/src/components/VerificationApply.tsx" \
  "$WEB/src/lib/api/auth.ts" "$WEB/src/lib/auth/session.ts" \
  "$API_TEST/domain/identity" "$API_TEST/domain/authorization" \
  "$API_TEST/domain/verification" \
  "$API_TEST/interfaces/rest/IdentityAuthorizationResourceTest.java"

phase_commit "Phase 4: Educational content and maker-checker workflow

- Taxonomy, revisions, submit/review/approve/publish
- Content reports and public published reads
- Flyway V3 content schema" \
  "$API_RES/db/migration/V3__educational_content.sql" \
  "$API/domain/content" "$API/domain/taxonomy" "$API/application/content" \
  "$API/infrastructure/persistence/PostgresEducationalContentRepository.java" \
  "$API/infrastructure/persistence/PostgresContentRevisionRepository.java" \
  "$API/infrastructure/persistence/PostgresContentReviewRepository.java" \
  "$API/infrastructure/persistence/PostgresContentSubmissionRepository.java" \
  "$API/infrastructure/persistence/PostgresSubjectRepository.java" \
  "$API/infrastructure/persistence/PostgresEducationLevelRepository.java" \
  "$API/infrastructure/persistence/PostgresContentReportRepository.java" \
  "$API/infrastructure/persistence/ContentBodyJson.java" \
  "$API/infrastructure/search" \
  "$API/interfaces/rest/ContentResource.java" "$API/interfaces/rest/ContentReviewResource.java" \
  "$API/interfaces/rest/PublicContentResource.java" \
  docs/CONTENT_MODEL.md docs/CONTENT_REVISIONING.md docs/MAKER_WORKFLOW.md \
  docs/CHECKER_WORKFLOW.md docs/REVIEW_MODEL.md docs/PUBLIC_CONTENT.md \
  docs/CONTENT_REPORTING.md docs/CONTENT_API.md \
  "$WEB/src/app/konten-saya" "$WEB/src/app/tinjauan" "$WEB/src/app/subjek" \
  "$WEB/src/app/materi" "$WEB/src/app/jalur" "$WEB/src/app/kursus" \
  "$WEB/src/components/ContentEditor.tsx" "$WEB/src/components/ContentBodyView.tsx" \
  "$WEB/src/components/ContentBodyView.test.tsx" "$WEB/src/components/CourseOutline.tsx" \
  "$WEB/src/components/HomeBrowse.tsx" "$WEB/src/lib/api/content.ts" \
  "$WEB/src/lib/api/contentHref.test.ts" \
  "$API_TEST/domain/content" "$API_TEST/interfaces/rest/ContentWorkflowResourceTest.java"

phase_commit "Phase 5: Learning experience, quiz, and progress

- Browse published hierarchy, bookmarks, resume, lesson completion
- Server-side quiz scoring with immutable attempts
- Flyway V4 learning schema" \
  "$API_RES/db/migration/V4__learning_quiz_progress.sql" \
  "$API/domain/learning" "$API/application/learning" \
  "$API/infrastructure/persistence/PostgresQuizSpecRepository.java" \
  "$API/infrastructure/persistence/PostgresQuizAttemptRepository.java" \
  "$API/infrastructure/persistence/PostgresLessonCompletionRepository.java" \
  "$API/infrastructure/persistence/PostgresBookmarkRepository.java" \
  "$API/infrastructure/persistence/PostgresLearningActivityRepository.java" \
  "$API/interfaces/rest/LearningResource.java" "$API/interfaces/rest/PublicQuizResource.java" \
  docs/LEARNING_MODEL.md docs/COURSE_MODEL.md docs/LESSON_MODEL.md docs/QUIZ_MODEL.md \
  docs/ASSESSMENT_RULES.md docs/PROGRESS_MODEL.md docs/BOOKMARK_MODEL.md \
  docs/LEARNER_EXPERIENCE.md docs/QUIZ_SECURITY.md \
  "$WEB/src/app/belajar" "$WEB/src/app/kuis" "$WEB/src/app/hasil" \
  "$WEB/src/components/ProgressBar.tsx" "$WEB/src/components/ProgressBar.test.tsx" \
  "$WEB/src/lib/api/learning.ts" \
  "$API_TEST/domain/learning" "$API_TEST/interfaces/rest/LearningExperienceResourceTest.java"

phase_commit "Phase 6: Q&A, moderation, and admin console

- Public Q&A, accept/useful/report, hide-not-delete moderation
- Administrator console for verification, roles, taxonomy
- Flyway V5 community schema" \
  "$API_RES/db/migration/V5__qa_community.sql" \
  "$API/domain/qa" "$API/application/qa" \
  "$API/infrastructure/persistence/PostgresQaQuestionRepository.java" \
  "$API/infrastructure/persistence/PostgresQaAnswerRepository.java" \
  "$API/infrastructure/persistence/PostgresQaReportRepository.java" \
  "$API/interfaces/rest/QaResource.java" "$API/interfaces/rest/PublicQaResource.java" \
  "$API/interfaces/rest/ModerationResource.java" \
  docs/QA_MODEL.md docs/MODERATION.md \
  "$WEB/src/app/tanya" "$WEB/src/app/moderasi" "$WEB/src/app/kelola" \
  "$WEB/src/components/TanyaBoard.tsx" "$WEB/src/components/AdminConsole.tsx" \
  "$WEB/src/lib/api/qa.ts" "$WEB/src/lib/api/admin.ts" \
  "$API_TEST/interfaces/rest/QaResourceTest.java"

phase_commit "Phase 7: Security hardening and admin user directory

- In-memory rate limits and Origin/Referer CSRF checks
- Admin user search by display name (no email)
- Web CSP and Permissions-Policy headers" \
  "$API/domain/security" "$API/infrastructure/security" \
  "$API/interfaces/http/RateLimitFilter.java" "$API/interfaces/http/MutatingOriginFilter.java" \
  "$WEB/next.config.ts" \
  "$API_TEST/domain/security" "$API_TEST/interfaces/rest/SecurityHardeningResourceTest.java"

phase_commit "Phase 8: E2E testing

- API golden-path journey test
- Playwright browser E2E with fixture seeding" \
  docs/E2E_TESTING.md scripts/git-phase-commits.sh \
  "$WEB/playwright.config.ts" "$WEB/e2e" \
  "$API_TEST/interfaces/rest/CriticalJourneyResourceTest.java"

phase_commit "Phase 9: MVP audit and release readiness

- Constitution compliance audit and refreshed release docs
- Dev-login disabled, admin permission, and rate-limit integration tests" \
  docs/MVP_AUDIT.md docs/RELEASE.md docs/CONSISTENCY_CHECKLIST.md \
  docs/DEVELOPMENT_SETUP.md docs/ARCHITECTURE.md docs/API_ARCHITECTURE.md \
  docs/DEPLOYMENT_ARCHITECTURE.md docs/SECURITY_ARCHITECTURE.md \
  README.md CHANGELOG.md \
  "$API_TEST/interfaces/rest/DevLoginDisabledResourceTest.java" \
  "$API_TEST/interfaces/rest/RateLimitIntegrationResourceTest.java"

if [ -n "$(git status --porcelain)" ]; then
  git add -A
  git commit -m "chore: include remaining DTOs, domain ports, and shared contracts"
  git push origin main
fi

echo "Commits on main:"
git log --oneline
