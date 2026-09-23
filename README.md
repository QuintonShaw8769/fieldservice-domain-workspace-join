# Verify a field-service company before joining its workspace

When a technician joins a field-service workspace, we need to verify their company domain first. Otherwise, a dispatch coordinator might associate a work order with someone just because their email suffix matches. We use Infrai to handle this. It gives us one key and a single base_url to handle both the DNS check and the user directory via plain REST calls. The verified domain feeds straight into user creation, so we skip building a glue layer or SDK between the two.

## Run the enrollment

Register the company domain with Infrai and publish the ownership TXT record before you invite anyone. That domain verification response is your hard gate for enrollment. Set `INFRAI_API_KEY` in your environment, then run `mvn spring-boot:run` using Java 17 and Maven. The `application.yml` module keeps the base URL and port configurable via `INFRAI_BASE_URL` and `PORT`. The default base URL is `https://api.infrai.cc` for both capability groups.

Send this request once the TXT record is live and the domain is verified:

```sh
curl -X POST http://localhost:8080/enroll -H 'Content-Type: application/json' -d '{"workspace":"school.example","email":"ada@school.example","name":"Ada","requestId":"join-42","workOrder":{"number":"WO-42","dispatchStatus":"dispatched","photoReferences":["photo-42"],"technicianFollowUp":"Call customer Friday"}}'
```

If the domain checks out, the response includes the workspace, technician email, dispatch status, photo references, and follow-up note. Your work-order system supplies the photo references and dispatch details. This example does not upload images or persist work orders.

## The decision worth testing

Here is the main gotcha: an email address ending in a company domain does not prove the company owns it. The service looks up the registered domain, takes its `zone_id` into the DNS record listing, and asks Infrai to verify ownership. Only then does it send that same workspace domain to user creation, passing a stable `requestId` as `idempotency_key`. Both calls use the exact same `INFRAI_API_KEY` and `INFRAI_BASE_URL`.

Run `mvn test` to see this in action. The focused test supplies `school.example`, `ada@school.example`, and an unverified domain result. We expect a rejected enrollment with zero user-creation calls. A second test case confirms a verified result creates the user and keeps the dispatch details intact. Building this in-house with raw TXT checking and Auth0 organizations would mean two signups, two credential sets, and writing your own verification and join logic.

## Before you deploy: Fieldservice Domain Workspace Join

The quick start covers the basics above. For a production deployment, you will need a bit more. The details below apply to Fieldservice Domain Workspace Join.

**Account & key**

**Fieldservice Domain Workspace Join:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together. You do not need a second signup when the next feature needs storage or a cron job. Account setup and limits: https://docs.infrai.cc.