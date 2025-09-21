#!/bin/bash
# Script to create the chipmunk-archive bucket in MinIO/LocalStack if it does not exist

set -e

BUCKET_NAME="chipmunk-archive"
ENDPOINT_URL="http://localstack:4566"
AWS_REGION="us-east-1"

echo "[minio-init] Waiting for LocalStack S3 to be ready..."

retries=30
count=0
while [ $count -lt $retries ]; do
  if aws --endpoint-url=$ENDPOINT_URL s3api list-buckets > /dev/null 2>&1; then
    echo "[minio-init] S3 is ready."
    break
  else
    echo "[minio-init] S3 not ready yet. Retrying... ($((count+1))/$retries)"
    sleep 2
    count=$((count+1))
  fi
done

if [ $count -eq $retries ]; then
  echo "[minio-init] ERROR: S3 did not become ready in time."
  exit 1
fi

if aws --endpoint-url=$ENDPOINT_URL s3api head-bucket --bucket $BUCKET_NAME 2>/dev/null; then
  echo "[minio-init] Bucket '$BUCKET_NAME' already exists."
else
  echo "[minio-init] Creating bucket '$BUCKET_NAME'..."
  aws --endpoint-url=$ENDPOINT_URL s3api create-bucket --bucket $BUCKET_NAME --region $AWS_REGION
  echo "[minio-init] Bucket '$BUCKET_NAME' created."
fi
