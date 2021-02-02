# Spring Boot Reference Application Kotliin

The intention of the Reference Application is to serve as a guide when developing Business Applications
(Fagapplikasjoner) within The Norwegian Tax Administration (Skatteetaten); applications implementing the core business 
rules in the tax domain for which Skatteetaten is responsible.

In this repository your will find examples on how to solve common technical issues and implement requirements for 
applications running within the networks of Skatteetaten and especially on the  Aurora OpenShift platform. This includes logging, handling database 
migrations, testing, security, application versioning, build pipeline, to name a few.

The Reference Application is implemented in [Spring Boot](https://spring.io/projects/spring-boot).


# About the Core Technologies

The recommended technology for all new business applications created within Skatteetaten is Spring Boot. There are
two main versions of the reference application. This version is implemented using Kotlin as the programming language
instead of Java. For the Java version, see https://github.com/Skatteetaten/openshift-reference-springboot-server .


# How to Use the Application

As the intention of the Reference Application is to serve as a guide to how to set up your own application, it is not 
intended to be used directly as a starting point for new applications without any modifications.

You may fork the repository of the application to be able to apply new commits onto your own modified application, or
your may export the code into your own fresh repository and manually keep up to date with changes to the central
repository by inspecting the change logs. Which approach is best will depend on the amount of changes you make to 
central files, like the ```pom.xml``` and ```application.yml```.

Regardless of the approach you use to keep up with changes, you will have to make the following changes to the
fork/export:

 * change the ```iqOrganizationName``` in the `Jenkinsfile` to match your organization
 * change the ```groupId```, ```artifactId```, ```name``` and ```description``` in the ```pom.xml``` to match that of your application
 * rename the main package in ```src/main``` to match that of your application
 * change the ```info.links``` in ```application.yml``` to match that of your application
 * remove the example database code (migrations under ```src/main/resource/db/migrations```), the Counter-classes in the controllers, health and service packages and the database config in ```application.yml```


# What is Covered in the Application?

## Starters

The application has one starter  [aurora-springboot2-starter](https://github.com/Skatteetaten/aurora-springboot2-starter/tree/master) in order to set up normal aurora requirements such as
 - grouping properties into their own property sources
 - setting default properties for actuator
 - instrumenting RestTemplates with metrics
 - instrument ServerFilter with metrics
 - instrument logback with metrics
 

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
</settings>

In Jenkins this will be disabled so that all Leveransepakke that is built on Jenkins will not have devtools included. If
you want to have jenkins in a snapshot build use the development flow and build it locally. Or conditionally add the `devtools`
 profile to the deploy goal in jenkins pipeline.
   	
