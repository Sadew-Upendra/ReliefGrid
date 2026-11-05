# Jenkins CD

## Why Jenkins does deployment and not GitHub Actions
GitHub Actions (Step 7) already builds, tests, and pushes images on every commit — it has no
credentials for the AWS account. Jenkins runs the deploy stage instead, so cluster-admin AWS
credentials live in exactly one place (Jenkins credentials store / an assumed IAM role), never
in a fork-triggerable GitHub Actions run.

## One-time setup
1. Install plugins: Pipeline, Kubernetes CLI, Amazon Web Services SDK (`withAWS` step), Git.
2. Create an IAM role `jenkins-deploy` that Jenkins can assume, scoped to `eks:DescribeCluster`
   and the ability to act as a Kubernetes user mapped to a namespace-scoped Role in `aws-auth`
   (least privilege — not cluster-admin).
3. New Pipeline job → "Pipeline script from SCM" → this repo → `infra/jenkins/Jenkinsfile`.
4. Trigger: GitHub webhook on push to `main`, after `docker-build.yml` (Step 7) has published
   an image for that commit.
5. Replace `OWNER` and `ACCOUNT_ID` placeholders in the `Jenkinsfile` with real values.

## Flow
```
push to main → GitHub Actions: test, scan, build & push images
                              → Jenkins: deploy to staging → smoke test
                                        → manual approval
                                        → deploy to production → smoke test
```
Rollback: `kubectl -n reliefgrid rollout undo deployment/backend` (or `frontend`) — Kubernetes keeps prior ReplicaSets for exactly this.
