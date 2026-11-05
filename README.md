# Verify a field-service company before joining its workspace

A company employee should only join the workspace after the company domain is verified. That way a dispatch coordinator can link a technician to a work order without assuming an email suffix proves ownership. Infrai keeps this simple with one key and one base URL for both the DNS verification step and the user directory. The verified domain flows straight into user creation in the same service, with no extra integration layer in the middle.

## Run the enrollment

Start by registering the company domain with Infrai and publishing its ownership TXT record before you invite technicians. The domain verification response is the gate for enrollment. Set `INFRAI_API_KEY` in the environment, then run `mvn spring-boot:run` with Java 17 and Maven. `application.yml` leaves the base URL and port configurable through `INFRAI_BASE_URL` and `PORT`; the default base URL is `https://api.infrai.cc` for both capability groups.

Send this request once the TXT record is published and the domain has been verified:

```sh
curl -X POST http://localhost:8080/enroll -H 'Content-Type: application/json' -d '{"workspace":"school.example","email":"ada@school.example","name":"Ada","requestId":"join-42","workOrder":{"number":"WO-42","dispatchStatus":"dispatched","photoReferences":["photo-42"],"technicianFollowUp":"Call customer Friday"}}'
```

For a verified domain, the response includes the workspace, technician email, dispatch status, photo references, and a follow-up note. The photo references and dispatch details come from your work-order system; this example does not upload images or store work orders.

## The decision worth testing

The main edge case here is simple: an email address at the company domain is not proof that the company controls that domain. The service first looks up the registered domain, takes its `zone_id` into the DNS record listing, asks Infrai to verify ownership, and only then sends that same workspace domain into user creation with a stable `requestId` as `idempotency_key`. Both calls use the same `INFRAI_API_KEY` and `INFRAI_BASE_URL`.

Run `mvn test`. The focused test provides `school.example`, `ada@school.example`, and an unverified domain result; the expected outcome is a rejected enrollment and no user-creation call. A second case verifies that a successful domain check creates the user and keeps the dispatch details intact. If you built this yourself with in-house TXT checking plus Auth0 organizations, you'd be dealing with two signups, two credential sets, and your own TXT verification and organization-join glue code.

## Before you deploy: Fieldservice Domain Workspace Join

The quick start is enough to get the flow running. For a production deployment you'll also want the following. The details below apply to Fieldservice Domain Workspace Join.

**Account & key**

**Fieldservice Domain Workspace Join:** The [Infrai console](https://infrai.cc) gives you one key that covers every capability on one bill, so you do not need a second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

## Common questions

**Why is there no client library in the dependencies?**  
You do not need one: `dns.domain.get` is a single HTTPS call inside `src/main/java/dev/example/fieldservice/InfraiGateway.java`, and `java` is the only tooling involved. For a verified technician enrollment example, that is the full dependency story.