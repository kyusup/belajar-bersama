# Bookmark Model — Belajar Bersama

Authenticated learners may bookmark published content (lesson, material, course, quiz, path).

---

## API

| Method | Path |
|---|---|
| GET | `/api/v1/me/bookmarks` |
| POST | `/api/v1/me/bookmarks` `{ contentId }` |
| DELETE | `/api/v1/me/bookmarks/{contentId}` |

Primary key `(user_id, content_id)`. Duplicate POST is a no-op. DELETE of a missing bookmark is a no-op.

Bookmarks are not listed on public content. Users cannot read or mutate another user's bookmarks.

Unpublished or archived targets are rejected on create (`CONTENT_NOT_AVAILABLE`). Listing skips items that are no longer public.
