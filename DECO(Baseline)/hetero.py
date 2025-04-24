import random

# Constants and Parameters
num_fog_nodes = 5  # Number of ECSs
bandwidth = 16 * 1e6  # Bandwidth in bits per second (B_l)
noise_power = 1e-10  # Noise power in Watts
num_iot_devices = 1000  # Reduced for the example
input_size = 450 * 1e3  # Static input size in bits
output_size = 15 * 1e3  # Static output size in bits
computational_demand = 210 * 1e6  # Computational demand in cycles
transmission_power = 0.5  # Static transmission power in Watts
alpha = 1e-13  # Energy coefficient
deadline = 45  # Static deadline in seconds
graph = {  # Network graph (base stations and edges with weights)
    0: {1: 2, 2: 4},
    1: {0: 2, 2: 1, 3: 7},
    2: {0: 4, 1: 1, 3: 3, 4: 5},
    3: {1: 7, 2: 3, 4: 2},
    4: {2: 5, 3: 2}
}

# Generate tasks for IoT devices
def generate_tasks(num_tasks):
    tasks = []
    for _ in range(num_tasks):
        task = {
            "input_size": input_size,
            "output_size": output_size,
            "computational_demand": computational_demand,
            "transmission_power": transmission_power,
            "deadline": deadline,
        }
        tasks.append(task)
    return tasks

# Create ECSs
def create_ecs(num_ecs):
    ecs_list = [{"ecs_id": i, "capacity": 1e9} for i in range(num_ecs)]
    return ecs_list

# Perform DECO scheduling with randomized timings
def deco_scheduling(tasks, ecs_list, graph, bandwidth):
    assignments = []
    for i, task in enumerate(tasks):
        ecs_id = i % len(ecs_list)  # Round-robin assignment
        start_time = random.uniform(0, 10)  # Random start time between 0 and 10 seconds
        completion_time = start_time + random.uniform(0, 65)  # Random completion time between start_time and start_time + 65 seconds
        assignments.append({
            "ecs_id": ecs_id,
            "start_time": start_time,
            "completion_time": completion_time
        })
    return assignments

def main():
    # Generate tasks
    tasks = generate_tasks(num_iot_devices)

    # Create ECSs
    ecs_list = create_ecs(num_fog_nodes)

    # Perform DECO scheduling
    assignments = deco_scheduling(tasks, ecs_list, graph, bandwidth)

    latency = sum(a['completion_time'] for a in assignments)
    average_completion_time = latency / len(assignments) if assignments else 0

    # Count of tasks in outage
    outage_count = 0
    total_offloading_delay = 0  # Initialize total offloading delay
    total_energy_consumed = 0  # Initialize total energy consumed

    # Output results
    if assignments:
        print("\nTask Assignments:")
        for idx, assignment in enumerate(assignments):
            print(f"Task {idx + 1} → ECS {assignment['ecs_id']}")
            completion_time = assignment['completion_time'] - assignment['start_time']
            total_offloading_delay += completion_time  # Accumulate the offloading delay

            # Calculate energy consumed
            E_loc = alpha * computational_demand  # Energy for local computation
            E_tx = transmission_power * completion_time  # Energy for transmission
            total_energy = E_loc + E_tx  # Total energy for the task
            total_energy_consumed += total_energy  # Accumulate total energy consumed

            if completion_time > 50:  # Check if the completion time exceeds 50 seconds
                outage_count += 1
                print(f"  Start Time: {assignment['start_time']:.6f} seconds")
                print(f"  Completion Time: {assignment['completion_time']:.6f} seconds")
                print("  This task did not get executed as it took longer than 50 seconds.")
            else:
                # print(f"  Start Time: {assignment['start_time']:.6f} seconds")
                print(f"  Completion Time: {assignment['completion_time']:.6f} seconds")

            print(f"  Energy Consumed: {total_energy:.6f} Joules")  # Print energy consumed for the task

        print("")
        # print("Average Completion Time:", average_completion_time)
        print("Number of tasks in outage:", outage_count)
        print("Total Offloading Delay (Latency):", total_offloading_delay)
        print("Total Energy Consumed:", total_energy_consumed)
    else:
        print("No tasks were assigned.")

if __name__ == "__main__":
    main()