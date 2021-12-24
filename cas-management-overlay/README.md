clone from https://github.com/apereo/cas-management-overlay

CAS Management Overlay
============================

CAS management web application WAR overlay for CAS with externalized configuration.

# Versions

```xml
<cas.version>6.3.x</cas.version>
```

# Requirements

* JDK 11

# Build

To see what commands are available to the build script, run:

```bash
./build.sh help
```

To package the final web application, run:

```bash
./build.sh package
```

To update `SNAPSHOT` versions run:

```bash
./build.sh package -U
```

## Windows Build

On Windows you can run build.cmd instead of build.sh. The usage may differ from build.sh, run "build.cmd help" for usage.

## Note

If you are running the management web application on the same machine as the CAS server web application itself, 
you will need to evaluate the build script and make sure the configuration files don't override each other.

# Deployment

## Embedded Tomcat

CAS will be available at:

* `http://cas.server.name:8080/cas-management`
* `https://cas.server.name:8443/cas-management`

## External

Deploy resultant `target/cas-management.war`  to a servlet container of choice.

### Dockerfile

You can also use the native Docker tooling and the provided `Dockerfile` to build and run CAS Management.

```bash
chmod +x *.sh
./docker-build.sh
./docker-run.sh
```
