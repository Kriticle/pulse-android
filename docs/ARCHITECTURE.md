# Architecture

## System boundary

```text
Google account
     |
     v
Google Sign-In ---- ID token ----> Supabase Auth
                                      |
                                      | access token
                                      v
Jetpack Compose UI -> ViewModels -> Retrofit/OkHttp -> Pulse backend API
        |                |                                  |
        |                +-> workout timer/score            +-> persistence (not in this repo)
        +-> Media3 player -> Android Visualizer
```

This repository contains only the Android client. Supabase configuration, the Google OAuth client, the backend implementation, its database, deployment, and operational controls are external dependencies.

## Client components

- `NavGraph.kt` owns the sign-in, level list, profile, preview, play, and results routes.
- Screen ViewModels own loading/error state and call the backend through `RetrofitClient`.
- `SupabaseClient` owns the persisted authentication session.
- `RetrofitClient` attaches the current Supabase access token to API requests.
- `PlayViewModel` owns the workout timer, segment/loop state, client-side score, Media3 player, and FFT samples used by the visualizer.
- DTOs in `data/Models.kt` define the client-side backend contract.

## External API contract used by the app

```text
GET  /levels
GET  /levels/{id}
POST /attempts
GET  /best-attempts?userId={id}
GET  /users/{id}
POST /users
```

Requests include `Authorization: Bearer <Supabase access token>` when a session exists. This client behavior does not prove that the separate backend validates the token, enforces subject ownership, rate-limits requests, or safely stores data; those controls require a separate backend audit.

## Security boundaries

- Treat the Supabase URL, anon key, Google web client ID, and API base URL as public client configuration. Do not put privileged secrets in `BuildConfig`.
- Enforce authorization at the backend for every user and attempt operation. A caller-supplied `userId` is not an authorization boundary.
- Enforce Supabase Row Level Security for any client-accessible table.
- Use HTTPS for release API traffic. The Gradle release validation task rejects local and cleartext API URLs.
- Treat score, duration, and accuracy submitted by this client as untrusted input.

## Trade-offs

### Direct DTO coupling

The small prototype stays simple by mapping backend JSON directly into screen-facing data classes. The cost is tight client/backend coupling and no explicit API version boundary.

### Client-side workout engine

The timer and score respond immediately and work without round trips during a session. The cost is that results can be manipulated, so they must not drive competitive or financial outcomes without server-side validation.

### Bundled media

Local assets make playback deterministic and avoid streaming dependencies. They also increase repository/application size and require explicit redistribution rights.

### External-only data

Keeping user and level data behind the API avoids shipping customer records. It also means the repository cannot demonstrate the main workflow without configured external services or a future synthetic demo mode.

