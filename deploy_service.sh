#!/bin/bash
set -eo pipefail
APP_IMAGE_NAME=$APPNAME
ECS_REPONAME=$AWS_REPOSITORY
ECS_TAG="latest"
eval $(aws ecr get-login --region $AWS_REGION --no-include-email)
docker tag $APP_IMAGE_NAME:$ECS_TAG $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$AWS_REPOSITORY:$CIRCLE_BUILD_NUM
ECS_TAG=$CIRCLE_BUILD_NUM
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECS_REPONAME:$ECS_TAG
ecs-cli configure --region us-east-1 --cluster $AWS_ECS_CLUSTER
ecs-cli compose --project-name "$AWS_ECS_SERVICE" service up --launch-type FARGATE