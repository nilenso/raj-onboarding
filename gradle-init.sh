#!/usr/bin/env sh

mkdir -p projectNIL && cd projectNIL || exit 1
gradle init \
  --type java-application \
  --dsl groovy \
  --package rnil.enso \
  --project-name ProjectNIL  \
  --no-split-project  \
  --java-version 25

# default test framework : JUnit4
