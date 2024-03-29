# Fergus
<img align="right" src="https://static.wikia.nocookie.net/muppet/images/5/5f/Fergus.jpg/revision/latest/scale-to-width-down/200">

NB! This application is under development.

## What is it?

Fergus is a http based REST service to conveniently set up buckets and users with 
a standard set of policies in a StorageGrid S3 solution.
The main purpose is to provision separately available storage areas for specific
users/applications to make simple object storage available for clients.

The component is named after the Fergus Fraggle (https://muppet.fandom.com/wiki/Fergus_Fraggle).

## Building Fergus

Fergus is a Spring Boot application written i Kotlin, using Gradle build automation.

```
git clone https://github.com/Skatteetaten/fergus.git
cd fergus
./gradlew build
```

## S3 server - a prerequisite for testing and deployment

Fergus is developed to operate on a StorageGrid S3 installation. Since the purpose of Fergus is to perform administrative 
tasks on such a server, a running StorageGrid server is needed to use Fergus.

## Deployment

There are some configuration needed for deploying and running Fergus

### Configuration settings 

Fergus need to be configured to connect to the S3 server. This is done by environment variables. All variables have
defined defaults, so for very basic testing, Fergus can start without them, but only as long as the S3 server conforms
to the defaults.

Here is a summary of the environment variables used by Fergus:

| Environment variable | Default | Description |
| ---| ---| ---|
| FERGUS_MANAGEMENT_URL | - | The URL of the StorageGRID management API |
| FERGUS_S3_URL | http://uia0ins-netapp-storagegrid01.skead.no:10880/ | The URL of the StorageGRID S3 API |
| FERGUS_S3_REGION | no-skatt-1 | The default region for buckets created |
| FERGUS_RANDOMPASS | true | Set to false if provisioned users should get a fixed password (NB: Testing only)|
| FERGUS_DEFAULT_PASSWORD | S3userpass | The returned userpass if FERGUS_RANDOMPASS is false |
| FERGUS_DEBUG | false | Set to true to enable debug logging |

## Using Fergus - API - NEEDS WORK

Fergus provides an http based API as a service.  [The API is described here](./TOBEDECIDED.md)

## Versioning

We use [Semantic versioning](http://semver.org/) for our release versions.

## Authors

* **Rune Offerdal** - *Initial work*

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](./LICENSE) file for details


## Log Configuration

All Skatteetaten applications should log using the same standard logging pattern. Also, all applications running
on Openshift has to log to a specific folder in the container for the logs to picked up and automatically indexed in
Splunk. Logfiles put in the container need to be rolled before reaching a predetermined size to avoid the platform
terminating the application (in self defence; log files filling up disk on the Openshift nodes may wreak havoc in a 
cluster).

To avoid every application having to maintain their own ```logback.xml```-file implementing all the requirements, one
is provided by the platform in the runtime Docker image. The reference application is configured to use this Logback
file when it exists, while still preserving the convenient Spring Boot features for setting the log level. This is done
in the ```src/main/assembly/metadata/openshift.json```-file. The ```application.yml```-file also contains an example on
how to set the log levels.


## HTTP Header Handling

According to the Aurora Requirements all applications should use the ```Klientid``` header for identifying themeselves
when they operate as a client to other services. They should also send a ```Korrelasjonsid``` header for traceability.
They may also optionally send a ```Meldingsid``` header as an identifier for the current message.

The value of the ```Klientid```, ```Korrelasjonsid``` and ```Meldingsid``` headers should be logged on every request.
This is implemented by using a filter that will extract the values of these headers and putting them on 
[SLF4J MDC](http://www.slf4j.org/api/org/slf4j/MDC.html). The artifact implementing the filter is 
[aurora-header-mdc-filter](https://github.com/skatteetaten/aurora-header-mdc-filter) and is included
as a dependency in the pom.xml-file. See the ```ApplicationConfig```-class for details on how the filter is configured.

 
## Database Migrations with Flyway

Databases are migrated using the database migration tool [Flyway](https://flywaydb.org/). Migrations should be put in
the ```src/main/resources/db/migration```-folder. Flyway is included as a dependency in the pom.xml-file and Spring
Boot automatically picks it up and configures it using the application DataSource.

Flyways out-of-order mode is enabled by default which allows migrations to be created in branches with concurrent
development activity and hot fixes without running the risk of creating a series of migrations that
cannot be applied because they have been created with an index/timestamp older than migrations already run against
the database.

The recommended naming scheme for migration files is ```VyyyyMMddHHmm__Migration_short_description.sql```, for
example ```V201609232312__CounterTableInit.sql```. Using
timestamps in preference of indexes is recommended to avoid having to coordinate migration indexes across branches
and developers.

For details, see:
* [Flyway SpringBoot](https://flywaydb.org/documentation/plugins/springboot)
* [Spring Doc: Use a higher-level database migration tool](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-database-initialization.html#howto-execute-flyway-database-migrations-on-startup)


## HTTP Endpoint JSON serialization

The format for Dates in JSON responses has been set to com.fasterxml.jackson.databind.util.ISO8601DateFormat in
preference of the default format which is just milliseconds since Epoch.


## Management interface

The management interface is exposed on a seperate port (MANAGEMENT_HTTP_PORT) for security reasons. The main part of
this is Spring Boot Actuator (with HATEOAS). Most of the endpoints are turned off by default. See the ```resources/application.yml``` file for more details. 

A central component Marjory is deployed in the OpenShift cluster that aggregate the management interfaces for all 
applications and works as a proxy to the hidden management interface. 

The standard management interface consist of the following urls

###  /info - Information 

The /info endpoint is particularly relevant because it is used to collect and display information about the application. 
Some of that information is maintained and set in the ```application.yml``` file
(everything under ```info.*``` is exposed as properties), and some is set via maven plugins;
  
  * the spring-boot-maven plugin has a build-info goal that is configured to run by default. This will goal will create
  a file ```META-INF/build-info.properties``` which includes  information like build time, groupId and artifactId. This
  will be added to the info section by spring actuator. 
  See [Spring Boot Maven Plugin](http://docs.spring.io/spring-boot/docs/current/maven-plugin/examples/build-info.html)
  for details.
  * the git-commit-id-plugin will create a file ```git.properties``` which includes information like committer, 
  commit id, tags, commit time etc from the commit currently being built. Spring actuator will add this information to
  the info section.

The info endpoint has a dependencies section that list the name of all the external dependencies and their static base URL. 
This information will be stored in a CMDB for cause analysis and to chart dependencies in Skatteetatens infrastructure.

The links part of the info endpoint show application specific links to other part of the internal infrastructure in Skatteeaten. 
The links contains some placeholders that are replaced marked with ```{}``` fences.

###  /health - Health status 

The health endpoint is used to communicate to the platform the status of your application. This information is scraped by the 
aurora-console and used in the overall status of you application.

For more info see:
* [Spring Doc: Health information](http://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html#production-ready-health)


###  /env - Configuration 
This endpoint prints out the given configuration for an application. Sensitive fields are masked using standard Spring Boot
mechanisms. 

### /prometheus - Metrics   

The reference application sets up metrics as described in the 
[aurora-springboot2-starter](https://github.com/Skatteetaten/aurora-springboot2-starter/tree/master)

For applications that are deployed to OpenShift, metrics exposed at ```/prometheus``` (default, configurable) in the
format required by Prometheus will be automatically scraped.

In order to deploy this application on the [AuroraPlattform](https://skatteetaten.github.io/aurora) using [AuroraConfig](https://skatteetaten.github.io/aurora/documentation/aurora-config/) the following must be specified in the base file:

```
prometheus:
  path: /actuator/prometheus
```

The standard value is /prometheus that works for spring boot 1 based applications but not boot2 based applications.

## Security

Management interface is exposed on a separate port that is not accessible outside of the cluster. This means that no 
information about metrics, env vars, health status is available to the the outside world. 

Tomcat is the application server used in the standard spring boot starter web, it by default disables the TRACE endpoint 
closing another security issue.

All application calls between applications are secured with an application level token that is provided by a central authority.


## Unit Testing

Unit testing has been set up and configured to use Junit 5.


## Documentation

For documentation, the Reference Application configured to use [spring-rest-docs](https://spring.io/projects/spring-restdocs) 
which is an approach to documentation that combines hand-written documentation with auto-generated snippets produced 
with Spring MVC Test. Please read the spring-rest-docs documentation for an overview of how the technology works.

The ```pom.xml``` is set up with the necessary plugins to build the documentation. This is basically including
spring-rest-docs on the test classpath to generate snippets and configuring the asciidoctor-maven-plugin to process
documentation in the ```src/main/asciidoc```-folder and the output folder of the spring-rest-docs tests. The 
maven-resources-plugin is configured to include the generated documentation in ```/static/docs``` in the final jar
which results in spring-boot exposing the files over HTTP on ```/docs``` (see [Static Content](http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-developing-web-applications.html#boot-features-spring-mvc-static-content)
from the spring-boot documentation for details).

A class, ```AbstractControllerTest```, is included as an example base class for tests that use spring-rest-docs.

When working with the documentation it can be convenient to enable continuous rendering of the asciidoc source.
This can be done by running the maven goal

    ./mvnw asciidoctor:auto-refresh
   

# How an application is built

## Application Configuration and Spring Profiles

The Reference Application is set up to work with two Spring configuration profiles  one called
```local``` and one called ```openshift```. The ```local``` profile is active by default (see the 
```spring.profiles.active``` property in ```application.yml```), and the intention is that this profile should be used
when running the application locally during development, and the application should ideally be startable from the IDE
without modifications after cloning it from Git.

In the ```metadata/openshift.json```-file the ```jvmOpts``` property is set to ```-Dspring.profiles.active=openshift```,
which will disable the local profile and activate a profile called ```openshift``` (it could be called pretty much
whatever, as long as it is something different from local). This allows you to have configuration that is active only
when you develop locally, or only when you run the application from the Docker image (for instance on Openshift).

Obviously, this dual profile setup does not help if you need different configuration for different deployed instances of
your application (for instance different environment/namespaces). Openshift supports many methods for applying 
configuration to Docker containers, and Aurora Openshift in particular has guidelines to how environment specific 
configuration should be done.


## Build Configuration

This section relates to how the ```pom.xml``` file is set up to produce artifacts and reports. Some of this have already
been covered in detail in other sections and will hence not be covered here again. In fact, most of the features of
the Reference Application have one or more entires in the pom. This section covers what is not explicitly covered 
elsewhere.

### Leveransepakke

The maven-assembly-plugin is configured to create a Leveransepakke compatible artifact from the build (basically just
a .zip-file with all application jars). What is included in the file is configured in 
```src/main/assembly/leveransepakke_descriptor.xml```.

Note that the spring-boot-maven-plugin is configured to exclude the ```repackage``` goal (which is enabled by
default). The repackage goal will create an "uberjar" that includes the application and all its dependencies instead
of a jar file with only the application classes and resources. If this behaviour is not disabled all application
dependencies will be included in the Leveransepakke twice; once in the application jar from the repackage goal, and
once from the maven-assembly-plugin.

### Versioning

The pom.xml is configured with the aurora-cd-plugin for versioning. aurora-cd, in turn, uses the aurora-git-version
component. See [aurora-git-version](https://github.com/skatteetaten/aurora-git-version) for details.

Maven does not allow plugins to change the version of the current build, so the plugin has to be triggered once
before the actual "main" build is started. This is handled by Jenkins via the Jenkinsfile. See the ```Jenkinsfile``` in
the root folder. The pipeline scripts will be opensource soon.


### Code Analysis

Plugins for code analysis via Checkstyle, Sonar, Jacoco and PiTest are included in the pom. Checkstyle is configured 
with the default rule set for Skatteetaten. All code analysis is runn via the standard Jenkins pipeline scripts. See 
section on Jenkinsfile for more details.


### Build Metadata for Docker Images
Build data for docker images is read from the docker part of the ```src/main/assembly/metadata/openshift.json```-file. 
 
 * maintainer will be set as the MAINTAINER instruction in the generated docker image
 * all the labels in the labels object will be added as LABEL instructions in the generated docker image
  
 
## Development Tools

It is recommended to take a look at the productivity features of spring boot developer tools. See
[Developer Tools](http://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-devtools.html).

Developement tools are activated via a profile. In order to always activate the profile when building locally add the following to your
`~/.m2/settings.xml`

    <...>
		<profile>
			<id>enable-devtools</id>
			<properties>
				<springBootDevtools>true</springBootDevtools>
			</properties>
		</profile>
	</profiles>

	<activeProfiles>
		<activeProfile>enable-devtools</activeProfile>
	</activeProfiles>


In Jenkins this will be disabled so that all Leveransepakke that is built on Jenkins will not have devtools included. If
you want to have jenkins in a snapshot build use the development flow and build it locally. Or conditionally add the `devtools`
 profile to the deploy goal in jenkins pipeline.
   	
