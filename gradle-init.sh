#!/usr/bin/env sh

mkdir -p projectNIL && cd projectNIL || exit 1
gradle init \
  --type java-application \
  --dsl groovy \
  --test-framework junit \
  --package rnil.enso \
  --project-name ProjectNIL  \
  --no-split-project  \
  --no-incubating  \
  --java-version 25
