import numpy as np
import random
import math

# Input parameters
Dk_f = 16 * 1e6  # CPU frequency in Hz
Tk_c = 210e6      # Computation workload in cycles
Bl = 10 * 1e6    # Bandwidth in Hz
Bw = 10 * 1e6    # Bandwidth in Hz
Tk_in = 300 * 1e3 # Input data size in bits
alpha = 1e-13        # Energy consumption factor
pk_tx = 0.5      # Transmission power in Watts
Tk_d = 30     # Deadline in seconds
T_out = 10e3
number_of_devices = 1000
number_of_fog_nodes = 5

# Local task completion time
def calc_local_time(Tk_c, Dk_f):
    return Tk_c / Dk_f

# Uplink data rate
def uplink_data_rate(Bw):
    lg = 1 + ((0.5 * 1.577e-8) / (1e-10))
    return Bw * math.log(lg, 2)

# Transmission delay
def transmission_delay(Tk_in):
    rkl = uplink_data_rate(Bw)
    print(f"Uplink data rate: {rkl}")  # Debug print
    return Tk_in / rkl

# Processing delay
def processing_delay(Tk_c):
    return Tk_c / (2.9e9)  # Corrected GHz value

# Queuing delay
def queuing_delay(Tk_proc, q):
    return Tk_proc * q

# Transmission delay of offloaded tasks
def transmission_delay1(Tk_in, T_out):
    Tkl_tx = transmission_delay(Tk_in)
    return Tkl_tx + ((Tk_in + T_out) / 100000)

# Total offloading time
def calc_time(Tkx, Tk_proc, Qd):
    return Tkx + Tk_proc + Qd

# Decision variable calculation
def decision_making(Tk_loc, Tkl_tx, Ek_loc, Ek_tx):
    if Tk_loc <= Tkl_tx and Ek_loc <= Ek_tx:
        return 0
    elif Tkl_tx <= Tk_loc and Ek_tx <= Ek_loc:
        return 1
    elif Tk_loc < Tkl_tx and Ek_loc < Ek_tx:
        return 0 if Tk_loc <= Tk_d else 1
    elif Tkl_tx < Tk_loc and Ek_tx < Ek_loc:
        return 1 if Tkl_tx <= Tk_d else 0
    else:
        return 0 if Ek_loc <= Ek_tx else 1

# Initialize variables
j = 0
latency = 0
energy = 0
count = 0

for i in range(number_of_devices):
    print("Task", i, "is assigned to ECS", (j + 5) % 5)
    j += 1

    Tk_c = random.randint(int(210e6), int(480e6))

    Tk_loc = calc_local_time(Tk_c, Dk_f)
    Tkl_tx = transmission_delay(Tk_in)
    Tk_tx = transmission_delay1(Tk_in, T_out)
    Tk_proc = processing_delay(Tk_c)
    Tk_queue = queuing_delay(Tk_proc, int(j / 5))
    Tk_off = calc_time(Tk_tx, Tk_proc, Tk_queue)

    Ek_tx = pk_tx * Tk_off
    Ek_loc = alpha * (Dk_f ** 2) * Tk_c

    # **Move decision-making before using xk**
    xk = decision_making(Tk_loc, Tkl_tx, Ek_loc, Ek_tx)

    latency += Tk_off

    e = Ek_loc * (1 - xk) + Ek_tx * xk
    energy += e  # Always accumulate energy

    if ((xk == 1 and Tk_off > Tk_d) or (xk == 0 and Tk_loc > Tk_d)):
        print("Process is being outaged")
        count += 1

    print("Processing delay:", Tk_proc)
    print("Tk_off:", Tk_off)
    print("Ek_loc:", Ek_loc)
    print("Ek_tx:", Ek_tx)
    print("Tk_loc:", Tk_loc)
    print("Tkl_tx:", Tkl_tx)
    print("Decision:", xk)

print("Latency:", latency)
print("Number of outaged devices:", count)
print("Total energy:", energy)
