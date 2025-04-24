# Project Architecture

Generate the javadoc for the project to get high level visibility on the entities used in the project:

```shell
mvn javadoc:javadoc
```

## Fog Network Architecture

A fog network architecture is composed of base stations and large number of IoT devices.

- **Base Station** (BS): BS offer communication services for all IoT devices for all IoT devices. Neighbouring BSs may
  have overlapping coverage areas.
- **Fog Node**: Fog nodes are attached to BS and offer computation services for tasks offloaded from IoT devices.
- **Fog Node Controller** (FNC): Each BS has one FNC which takes care of workload allocation among attached fog nodes.
  Considering security aspects, the FNC authenticates every fog node to allocate tasks.
- **Task**: Task is a computational work item that has a specified computation demand. The effort to execute a task
  locally in an IoT device and on a fog node differ (also considering the communication overhead in case of offloading).
- **Application**: Application is the origin of tasks. This is logical aspect of the IoT device. We consider two kinds:
  * _SimpleApplication_: Consists of single independent task. Either the task is executed locally or on fog.
  * _CompoundApplication_: Consists of two tasks, one of which depends on the other, say A depends on B. Here we have
    several cases: either both are executed locally, or both executed on fog, or one of the two is executed locally and
    the other on fog.
- **IoT Device**: Every IoT device has some computation and communication capability. We consider each IoT device to
  consist of an application (simple or compound).

The main challenges for load balancing is the decision to choose which tasks to execute locally and which ones on fog.
This is essentially a tradeoff between the computing capabilities and communication overhead involved. It is much more
difficult to decide when it involves a CompoundApplication.

The IoT devices that fall under the coverage of a single base station will communicate with the same for offloading.
Waiting times in network and FNC queues will also need to be considered when an IoT device falls under overlapping
coverage of multiple BS. In this case, there is a need for deciding the association of IoT devices among the BSs to
minimize the latency and the overall power consumption.

## Algorithm Execution

We have the algorithm in two parts:
* Base Station Side
* IoT Device Side

### Base Station Side Algorithm

* Consider the current (or initial) association of IoT devices among BSs
* Evaluate the traffic and computing loads in this case
* Update the (intermediate) association accordingly to distribute the load
* Broadcast above values to all IoT devices

### IoT Device Side Algorithm

* Based on the values broadcast by BS, select a suitable BS
* The BS side algorithm will broadcast the values of next iteration based on this association

## Code Organization

For each of the entities described above, we have a class that defines their behavior. For API, refer to the javadoc.