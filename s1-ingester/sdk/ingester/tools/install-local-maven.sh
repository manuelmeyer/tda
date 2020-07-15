#!/bin/bash

##############################################################################
#
# Copyright (c) 2015 Pivotal Software, Inc.  All rights reserved.
#
##############################################################################

if [[ "${#}" < "1" ]]
then
  echo "Usage: ${0} jar-name [groupId] [artifactId]"
  exit 1
fi

JAR_NAME="${1}"

VERSION_EXTRACTED=`echo $JAR_NAME | grep -o -E '[0-9].[0-9].[0-9]+'`

GROUP_ID="${2:-io.pivotal}"

ARTIFACT_ID="protocols-api"
if [[ $JAR_NAME == *"test-harness"* ]]
then
  ARTIFACT_ID="protocols-api-test-harness"
fi

VERSION="$VERSION_EXTRACTED.RELEASE"

 mvn install:install-file  \
-Dfile=${JAR_NAME} \
-DgroupId=${GROUP_ID} \
-DartifactId=${ARTIFACT_ID} \
-Dversion=${VERSION} \
-Dpackaging=jar \
-DgeneratePom=true
