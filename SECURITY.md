# Security policy

Pulse is an early prototype and does not currently publish supported versions or security guarantees.

Please do not open a public issue for a suspected credential leak, authorization bypass, private-data exposure, or vulnerable live service. Contact the repository owner privately through their GitHub profile and include the affected component, reproduction steps, and potential impact. Do not access, modify, or retain data that is not yours.

Client builds must contain only public client configuration. Supabase service-role keys, OAuth client secrets, signing keys, backend credentials, and production data must never be committed or embedded in an APK.

