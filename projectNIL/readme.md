# ProjectNIL
**several nomenclature iterations : project(ONE -> ZERO -> NIL)**  

a primal Function-as-a-Service (FaaS) that allows devs to register functions and execute them.

## Quick Links

- [Project Board](https://github.com/orgs/nilenso/projects/24/views/1)

# Dev Setup

## Dependencies

 - openjdk25
 - podman
 - make (optional)
 - compilers :
   - asc : `npm install -g assemblyscript`
 

init dependencies (pgmq + postgres container followed by liquibase migrations) first : `make setup`
``` bash
podman compose -f podman-compose.yaml up -d
```

verify container health with a `podman ps`

```
CONTAINER ID  IMAGE                          COMMAND     CREATED        STATUS                  PORTS                   NAMES
3189c5b3e5fe  ghcr.io/pgmq/pg18-pgmq:v1.8.0  postgres    7 minutes ago  Up 7 minutes (healthy)  0.0.0.0:5432->5432/tcp  projectnil-db
```

this sets up the queue and relational db with the liquibase [migrations](./infra/migrations)  

## Connecting to Database : `make psql`

```bash
podman exec -it projectnil-db psql -U projectnil -d projectnil
```

## Running Services
proceed to run the api and compiler individually via gradlew : `make api; make compiler`

```
# Run the API service (port 8080)
./gradlew :services:api:bootRun

# Run the Compiler service (port 8081)
./gradlew :services:compiler:bootRun

```

check makefile for convenience commands:

run `make help`

