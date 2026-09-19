# Privacy checklist

Official sources reviewed 2026-09-19. Code integration does not configure the AdMob account
or determine the game's target audience.

- [x] Android-only UMP integration; offline startup does not wait for UMP.
- [x] Request updated consent at launch; use SDK permission after both success and error.
- [x] No ad loading/initialization while permission is unknown or false. Prior permission is
  accepted only when UMP reports it after an update request.
- [x] Settings exposes Privacy Options when required and refreshes after choices change.
- [x] Existing ads and stale load callbacks are invalidated when privacy access changes.
- [x] No guessed age, region, child-directed status, or custom consent cache.
- [ ] Owner defines audience, under-age handling, distribution regions, and any mixed-age flow.
- [ ] Publish a public privacy policy with developer identity/contact and accurate SDK disclosures.
- [ ] Create/publish the applicable messages in AdMob Privacy & messaging for the registered app.

Google's [European regulations guidance](https://developers.google.com/admob/android/privacy/gdpr)
addresses the **EEA, United Kingdom, and Switzerland**. Configure those messages in AdMob;
UMP decides whether a message is required. Do not maintain a country allowlist in the game.
For applicable US states, review Google's [US states guidance](https://developers.google.com/admob/android/privacy/us-states)
and configure the relevant UMP messaging/GPP choices. The app does not invent opt-outs or assume
non-personalized advertising eliminates consent obligations.

## Device acceptance matrix

Use only official demo ad IDs. UMP geography overrides belong in debug-only test code and require
registered test devices; follow the [official UMP test procedure](https://developers.google.com/admob/android/privacy).
No geography override, device hash, consent reset, or test-age inference is enabled in production.
The sample App ID may not serve a publisher-configured message; validate configured messages
in an owner-controlled test setup before publication, without putting identifiers in Git.

| Scenario | Required result |
| --- | --- |
| Fresh install, offline, unknown consent | Menus/gameplay open; no ad request; no reward |
| Update error, no prior permission | Remain playable; no SDK initialization/request |
| Update error, UMP permits prior choices | Requests may proceed, without duplicate initialization |
| EEA/UK/Switzerland consent needed | Required form; no speculative permission |
| Rejection or form error | Recheck UMP permission, never treat error as acceptance |
| Privacy Options changed | Drop old cached ads, reevaluate request permission |
| Applicable US state message | Honor SDK/GPP choices and required privacy entry |
| Back/rotation/background during form | No stuck game navigation, no duplicate completion |
| Process killed during rewarded ad | No replayed reward after restart |
| Consent withdrawn then offline restart | No stale app-maintained permission |

Complete the matrix on physical Android devices/emulators before enabling production ads.
The current workspace had no connected device/emulator; unit/build checks do not establish
regional form behavior. Review [Data Safety notes](DATA_SAFETY_NOTES.md) and the actual final
merged manifest when completing Play declarations.
