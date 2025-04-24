# Fog Load Balancing
This project presents a simulation based on our research on Latency and Energy-Aware Secure Load Balancing for Dependent Tasks in IoT-Fog Networks (LEaD). The LEaD model implements an efficient load distribution strategy that balances the workload between base stations (BS) and fog nodes (FNs) for both dependent and independent tasks. To ensure secure and reliable communication among FNs, we employs Shamir’s secret-sharing scheme with a binary offloading mechanism. This approach aims to minimize energy consumption, latency, and outages in a fog network. 


In our work, we consider the load distribution of both dependent and independent tasks on fog nodes, which affect
the computing latencies and system energy if not properly balanced.

## Features

* Load balancing of offloading both dependent and independent tasks.

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

# References
[1] S. Azizi, M. Othman and H. Khamfroush, "DECO: A Deadline-Aware and Energy-Efficient Algorithm for Task Offloading in Mobile Edge Computing," in IEEE Systems Journal, vol. 17, no. 1, pp. 952-963, March 2023, doi: 10.1109/JSYST.2022.3185011.

[2] J. Yan, S. Bi, Y. J. Zhang, M. Tao, Optimal task offloading and resource allocation in mobile-edge computing
with inter-user task dependency, IEEE Transactions on Wireless Communications 19 (1) (2019) 235–250. doi: https://doi.org/10.1109/TWC.2019.2943563

[3] F. Chiti, R. Fantacci, B. Picano, A matching theory framework for tasks offloading in fog computing for IoT
systems, IEEE Internet of Things Journal 5 (6) (2018) 5089–5096. doi: https://doi.org/10.1109/JIOT.2018.2871251

# Contributors

-> Miss Priyanka Soni

   https://scholar.google.com/citations?user=LZKL3o4AAAAJ&hl=en
   
-> Nitin Chaudhary

   https://github.com/nitin2306

-> Manojna K P

   https://github.com/manojnakp

-> Dr. Sourav kanti addya

   https://souravkaddya.in/


# Contact

If you have any questions, simply write a mail to sonipriyanka31994(AT)gmail(DOT)com
