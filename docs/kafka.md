# ReliefGrid — Kafka Notes (Step 10)

## Topics
Match `docs/architecture.md` exactly, created automatically by `KafkaConfig`'s `NewTopic` beans on startup (local/dev) or by `infra/kafka`'s init job / Terraform's MSK module (staging/production):

| Topic | Producer | Consumer |
|---|---|---|
| `request.created` | backend (`RequestService.create`) | AI service (`app/kafka/consumer.py`) |
| `request.status-changed` | backend (`RequestService.transition`) | backend's own `NotificationEventListener`, analytics |
| `task.assigned` | backend (`TaskService.accept`) | backend's own `NotificationEventListener` |
| `ai.classification-result` | AI service (`app/kafka/producer.py`) | backend (`AiResultConsumer`) |

The AI service (`ai-service/`, FastAPI + scikit-learn) is now built — see `ai-service/README.md`.
Submitting a request end-to-end now really does refine its urgency and need type a moment
after creation, not just in theory.

Notifications (`backend`'s `notification` package) consume their two topics in-process rather
than as a separate microservice — see the class comment on `NotificationEventListener` for why.
`request.status-changed` notifies the reporter; `task.assigned` notifies the volunteer, so the
same event never double-notifies the reporter twice.

## How publishing works
Services never call Kafka directly. They publish a plain Spring application event
(`RequestEvents.RequestCreated`, etc.) inside their `@Transactional` method; `KafkaEventRelay`
forwards it to Kafka only `AFTER_COMMIT`. If the transaction rolls back, no message is sent —
a rejected or invalid request never produces a Kafka event for data that doesn't exist.

If the Kafka broker itself is unreachable, `EventPublisher` logs a warning and moves on — see
ADR 0003. A citizen can still submit and track a request with Kafka fully down; only the
AI-triage and notification side effects are delayed.

## Local development
```bash
docker compose -f infra/docker/docker-compose.yml -f infra/kafka/docker-compose.kafka.yml \
  --env-file infra/docker/.env up --build
```
This starts a single-node KRaft-mode Kafka (no ZooKeeper) plus a one-shot job that creates all four topics. Inspect it:
```bash
docker exec -it reliefgrid-kafka-1 /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic request.created --from-beginning
```
Submitting a request from the frontend (or `POST /api/v1/requests`) should print a JSON message there within a second or two.

## Production (MSK)
`infra/terraform/modules/msk` provisions **MSK Serverless** with IAM authentication (no
username/password, no separate ZooKeeper/Kafka ACLs to manage). The Spring Kafka config in
`application.yml` as shipped talks **plaintext** Kafka — correct for local dev, not for MSK.
To point the backend at MSK, still to do:
1. Add the `software.amazon.msk:aws-msk-iam-auth` dependency to `backend/pom.xml`.
2. Add a `production` Spring profile (`application-production.yml`) setting:
   ```yaml
   spring.kafka.properties.security.protocol: SASL_SSL
   spring.kafka.properties.sasl.mechanism: AWS_MSK_IAM
   spring.kafka.properties.sasl.jaas.config: software.amazon.msk.auth.iam.IAMLoginModule required;
   spring.kafka.properties.sasl.client.callback.handler.class: software.amazon.msk.auth.iam.IAMClientCallbackHandler
   ```
3. Set `KAFKA_BOOTSTRAP_SERVERS` (in the real `reliefgrid-secrets` k8s Secret) to
   `terraform output kafka_bootstrap_brokers_sasl_iam` from Step 10's Terraform.
4. The EKS node IAM role (or a dedicated IRSA role for the backend's service account) needs
   `kafka-cluster:Connect`, `*:WriteData`, `*:ReadData` on the MSK cluster ARN.

This is left as the concrete next step for whoever wires up the AI service in Phase 2, since
that's the point at which Kafka in production actually starts carrying traffic.
