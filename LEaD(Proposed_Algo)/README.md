# Fog Load Balancing Algorithm

This project is a machine simulation of our research work on load balancing in fog computing. The problem can be framed
as minimizing the total latency of IoT data flows, which involves considering two key components:
- **Traffic Load at Base Stations**: The communication delay incurred due to congestion at the BSs.
- **Computing Load at Fog Nodes**: The delay caused by the computational workload of fog nodes.

When a BS is overloaded with too many IoT devices, the communication latency increases. Similarly, when a fog node
is heavily burdened, the computing latency increases. As IoT devices can be dynamically associated with different BSs
(depending on their coverage areas), the challenge is to find an optimal distribution of devices to BSs and fog nodes
that minimizes both latencies.

In our work, we consider the load distribution of both dependent and independent tasks on fog nodes, which affect
the computing latencies if not properly balanced.

## Features

* Load balancing of offloading both dependent and independent tasks.
* Consider coverage areas of base stations for load balancing.
* Secure fog node authentication at the fog node controller based on Shamir secret sharing scheme.

## Execution Environment

Operating System: Linux or Unix-like 

Physical Memory (RAM): 32GB

## Requirements

* Java JDK (tested on OpenJDK 21)
* Maven 3.8+ (for building the project)

## Usage

If using a Java IDE (like Eclipse or IntelliJ), set up the build configuration as a maven project with entrypoint as
`org.loadbalancer.Main`.

### Quick Build and Run

```shell
mvn clean install
mvn exec:java -Dexec.mainClass="org.loadbalancer.Main"
```

### Package JAR Executable

```shell
mvn package
java -jar target/new-algo-1.0-SNAPSHOT.jar
```

### Input Format

See `input.json` for a sample of the input file. It should be an array of base station objects, each of which is made
up of mainly of a list of IoT devices and an FNC consisting of a list of fog nodes. Each fog node and IoT device JSON
object defines the specifications of the device/node for the load balancing algorithm. Have a look at the following
source files for more information:
* `org.loadbalancer.Defaults`: Fallback values and default parameters of the algorithm. Tweak it up to simulate reality of the use-case
  as closely as possible.
* `Parse` functions of entities in the algorithm is used for parsing the JSON object corresponding to that entity.

### Output Format

Primary useful output of the algorithm is the latency of the fog topology defined and the energy usage. Have a look at
`org.loadbalancer.BaseStation.output` method for how the output is computed. The output is printed in JSON format.

## Project Architecture

See `architecture.md`.
