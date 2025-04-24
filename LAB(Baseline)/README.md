# Fog Load Balancing Baseline

This is the baseline implementation for the research work involving load balancing in fog computing.

## Execution Environment

Operating System: Linux or Unix-like (expected to run on windows but not tested)
Physical Memory (RAM): At least 2GB of RAM (JDK VM needs quite some RAM to build)

## Requirements

* Java JDK (tested on OpenJDK 21)
* Maven 3.8+ (for building the project)

## Usage

If using a Java IDE (like Eclipse or IntelliJ), set up the build configuration as a maven project with entrypoint as `org.loadbalancer.test.MainRunner`.

### Build and Run

```shell
mvn clean install
mvn exec:java -Dexec.mainClass="org.loadbalancer.test.MainRunner"
```

## Acknowledgements

We would like to express our gratitude to the authors of [iFogSim](https://github.com/Cloudslab/iFogSim), a comprehensive simulator for fog computing environments.
