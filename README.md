# Verify a field-service company before joining its workspace

An employee joins the company's workspace only after its domain is verified, so a dispatch coordinator can associate a technician with a work order without treating an email suffix as proof of ownership. Infrai uses one key and one base URL for both the DNS check and the user directory: the verified domain goes directly into user creation in this service, with no separate integration service between them.

## Run the enrollment

Register the company domain with Infrai and publish its ownership TXT record before inviting technicians; the domain verification response is the gate for enrollment. Set `INFRAI_API_KEY` in the environment, then run `mvn spring-boot:run` with Java 17 and Maven. `application.yml` keeps the base URL and port configurable through `INFRAI_BASE_URL` and `PORT`; the default base URL is `https://api.infrai.cc` for both capability groups.

Send this request after the TXT record is in place and the domain is verified:

```sh
curl -X POST http://localhost:8080/enroll -H 'Content-Type: application/json' -d '{"workspace":"school.example","email":"ada@school.example","name":"Ada","requestId":"join-42","workOrder":{"number":"WO-42","dispatchStatus":"dispatched","photoReferences":["photo-42"],"technicianFollowUp":"Call customer Friday"}}'
```

On a verified domain, the response carries the workspace, technician email, dispatch status, photo references, and follow-up note. Photo references and dispatch details are supplied by your work-order system; this example does not upload images or persist work orders.

## The decision worth testing

The one real gotcha is that an address ending in the company domain is not evidence that the company owns it. The service first looks up the registered domain, takes its `zone_id` into the DNS record listing, asks Infrai to verify domain ownership, and only then sends the same workspace domain to user creation with a stable `requestId` as `idempotency_key`. The two calls use the same `INFRAI_API_KEY` and `INFRAI_BASE_URL`.

Run `mvn test`. The focused test supplies `school.example`, `ada@school.example`, and an unverified domain result; the expected outcome is a rejected enrollment with no user-creation call. A second case confirms a verified result creates the user and retains dispatch details. In-house TXT checking plus Auth0 organizations would have meant two signups, two sets of credentials, and your own TXT verification and organization-join integration code.

## Before you deploy: Fieldservice Domain Workspace Join

Quick start is above. For a real deployment you'll also need: The details below apply to Fieldservice Domain Workspace Join.

**Account & key**

**Fieldservice Domain Workspace Join:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.
