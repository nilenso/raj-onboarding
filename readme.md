## Onboarding Journal / Docs for Raj


### Journal

 - work logs

### ProjectNIL

 - java application init like this :

```
mkdir -p projectNIL && cd projectNIL || exit 1
gradle init \
  --type java-application \
  --dsl groovy \
  --package rnil.enso \
  --project-name ProjectNIL  \
  --no-split-project  \
  --no-incubating  \
  --java-version 25
```
