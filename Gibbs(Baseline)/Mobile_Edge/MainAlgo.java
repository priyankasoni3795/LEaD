package Mobile_Edge;

import java.util.*;
import java.lang.*;
 
public class MainAlgo {
    
    public static void bi_search(HashMap<String, Object> config, double prec) {
        double vub = (Double) config.get("beta2_t");
        double vlb = 0;
 
        while (true) {
            double v = (vub + vlb) / 2;
            update(config, v, (Double)config.get("beta2_t") - v);
                        
            double tempArray[] =wait_times(config); 
            double t1_wait = tempArray[0];
            double t2_wait = tempArray[1];
            
//            if(Double.isNaN(t1_wait))         t1_wait = 0.1;
//            Random randomNum = new Random();
//            if(Double.isNaN(t2_wait))         t2_wait = t1_wait + randomNum.nextDouble(1.1);
            
//            System.out.println(t1_wait +" "+t2_wait);
            
            if (t1_wait - t2_wait < 0.1) {
                vub = v;
            } else {
                vlb = v;
            }
 
            if (t1_wait - t2_wait < prec) {
                break;
            }
        }
    }
 
    public static void update(HashMap<String, Object> config, double lamda, double mu) {
        HashMap<Integer, ArrayList<Double>> p = new HashMap<Integer, ArrayList<Double>>();
        
        ArrayList<Double> tempList1 = optimal_transmission_power_device_1(config, lamda, mu); 
        p.put(1, tempList1);
        
        ArrayList<Double> tempList2 = optimal_transmission_power_device_2(config, lamda, mu);
        p.put(2, tempList2);
        
//        config.put("p", p );
        config.put("p", p);
        
        HashMap<Integer, ArrayList<Double>> f = new HashMap<Integer, ArrayList<Double>>();
        
        ArrayList<Double> freqList1 = optimal_freq_device_1(config, lamda); 
        f.put(1, freqList1);
        
        ArrayList<Double> freqList2 = optimal_freq_device_2(config, mu);
        f.put(2, freqList2);
        
        config.put("f", f );
    }
    
    public static double[] wait_times(HashMap<String, Object> config) {
        double[] res = new double[2];
        ArrayList<Double> t1_local_times = exec_time_local(config, 1);
        ArrayList<Double> t1_edge_times = exec_time_edge(config, 1);
        ArrayList<Double> t1_data_rate = uplink_data_rate(config, 1);
        ArrayList<Double> t1_up_times = offloading_transmission_time(config, 1, t1_data_rate);
        ArrayList<Double> t1_data_rate1 = downlink_data_rate(config, 1);
        ArrayList<Double> t1_down_times = downloading_transmission_time(config, 1, t1_data_rate1);
        ArrayList<Double> t2_local_times = exec_time_local(config, 2);
        ArrayList<Double> t2_edge_times = exec_time_edge(config,2);
        ArrayList<Double> t2_data_rate = uplink_data_rate(config, 2);
        ArrayList<Double> t2_up_times = offloading_transmission_time(config, 2, t2_data_rate);
        ArrayList<Double> t2_data_rate1 = downlink_data_rate(config, 2);
        ArrayList<Double> t2_down_times = downloading_transmission_time(config, 2, t2_data_rate1);
        double k = (Double) config.get("k");
        double res1 = wait_time_device_1(t1_local_times, t1_edge_times, t1_up_times, t1_down_times, config, 1, t2_down_times.get((int)k), k);
        double res2 = wait_time_device_2(t2_local_times, t2_edge_times, t2_up_times, t2_down_times, config, 2, k);
        res[0] = res1;
        res[1] = res2;
        return res;
    }
    
    public static double[] energy_time(HashMap<String, Object> config) {
          double[] res = new double[4];
          ArrayList<Double> local_energy = energy_consumption_local(config, 1);
          ArrayList<Double> t1_data_rate = uplink_data_rate(config, 1);
          ArrayList<Double> t1_up_times = offloading_transmission_time(config, 1, t1_data_rate);
          t1_data_rate = downlink_data_rate(config, 1);
          ArrayList<Double> t1_down_times = downloading_transmission_time(config, 1, t1_data_rate);
          ArrayList<Double> offloading_enrgy = offloading_transmission_energy(config, 1, t1_up_times);
          double energy_device_1 = energy_consumption_device_1(local_energy, offloading_enrgy, config, 1);
          ArrayList<Double> local_times = exec_time_local(config, 1);
          ArrayList<Double> edge_times = exec_time_edge(config, 1);
          double total_comp_time_1 = computation_time_device_1(local_times, edge_times, config, 1);
          double trans_times = transmission_time_device_1(t1_up_times, t1_down_times, config, 1);
          double total_time_device_1 = total_comp_time_1 + 2*trans_times;
          local_energy = energy_consumption_local(config, 2);
          ArrayList<Double> t2_data_rate = uplink_data_rate(config, 2);
          ArrayList<Double> t2_up_times = offloading_transmission_time(config, 2, t2_data_rate);
          t2_data_rate = downlink_data_rate(config, 2);
          ArrayList<Double> t2_down_times = downloading_transmission_time(config, 2, t2_data_rate);
          ArrayList<Double> offloading_energy = offloading_transmission_energy(config, 2, t2_up_times);
          double energy_device_2 = energy_consumption_device_2(local_energy, offloading_energy, config, 2);
          local_times = exec_time_local(config, 2);
          edge_times = exec_time_edge(config, 2);
          double total_comp_time_2 = computation_time_device_1(local_times, edge_times, config, 2);
          trans_times = transmission_time_device_1(t2_up_times, t2_down_times, config, 2);
          double total_time_device_2 = total_comp_time_2 + 2*trans_times;
          
          res[0] = energy_device_1;
          res[1] = energy_device_2;
          res[2] = total_time_device_1;
          res[3] = total_time_device_2;
          return res;
    }
 
    static final double EPSILON = 1E-10; // tolerance for Halley's method
    
    public static double lambertW(double z) {
      double w = Math.log(Math.abs(z) + 1.0);
      if (z == 0.0) {
          return 0.1;
      }
      for (int i = 0; i < 100; i++) {
          double ew = Math.exp(w);
          double wew = w * ew;
          double f = wew - z;
          double df = (w + 2.0) * (wew - z) / (2.0 * w + 2.0);
          double d2f = (w * (3.0 * w + 7.0) + 4.0) * f / (2.0 * w + 2.0) / (2.0 * w + 3.0) - df * df;
          double delta = f / df * (1.0 + 0.5 * f / df * d2f / df);
          w -= delta;
          if (Math.abs(delta) < EPSILON * (1.0 + Math.abs(w))) {
              return w == 0.0 ? 0.1 : w;
          }
      }
      return 0.1;
  }
  
  public static ArrayList<Double> optimal_transmission_power_device_1(HashMap<String, Object> config, double lamda, double mu) {
    double beta_t = (Double) config.get("beta1_t");
    double beta_e = (Double) config.get("beta1_e"); 
    double sigma = (Double) config.get("sigma"); 
    double P = (Double) ((HashMap<String, Object>) config.get("P")).get(1);
    ArrayList<Double>  h = (ArrayList<Double>) ((HashMap<String, Object>) config.get("h")).get(1); 
    double M = (Double) config.get("M");
    
    ArrayList<Double> l1 = new ArrayList<Double>();
      double A1 = 1.0 + (beta_t + lamda) / (beta_e * P);
      double A2 = 1.0 + lamda / (beta_e * P);
      for (int i = 0; i < M; i++) {
          double B1 = h.get(i) * (beta_t + lamda) / (beta_e * sigma*sigma) - 1.0;
          double B2 = h.get(i) * lamda / (beta_e * sigma*sigma) - 1.0;
          if (h.get(i) < sigma*sigma / P * (A1 / (-lambertW(-A1 * Math.exp(-A1))) - 1.0)) {
            l1.add(P);
          } else {
              l1.add(sigma*sigma / h.get(i) * (B1 / lambertW(B1 / Math.E) - 1.0));
          }
          if (i == M) {
              if (h.get(i) < sigma*sigma / P * (A2 / (-lambertW(-A2 * Math.exp(-A2))) - 1.0)) {
                  l1.add(P);
              } else {
                  l1.add(sigma*sigma / h.get(i) * (B2 / lambertW(B2 / Math.E) - 1.0));
              }
          }
      }
      return l1;
  }
 
  public static ArrayList<Double> optimal_transmission_power_device_2(HashMap<String, Object> config, double lamda, double mu) {
    double beta_t = (Double) config.get("beta2_t");
    double beta_e = (Double) config.get("beta2_e"); 
    double sigma = (Double) config.get("sigma"); 
    double P = (Double) ((HashMap<String, Object>) config.get("P")).get(2);
    ArrayList<Double>  h = (ArrayList<Double>) ((HashMap<String, Object>) config.get("h")).get(2); 
    double K = (Double)config.get("k");
    double N = (Double)config.get("N");
    
    ArrayList<Double> l1 = new ArrayList<Double>();
      double A3 = 1 + (beta_t) / (beta_e * P);
      double A4 = 1 + (mu) / (beta_e * P);
      
      for (int i = 0; i < K+1; i++) {
          double B1 = h.get(i) * (beta_t + lamda) / (beta_e * sigma*sigma) - 1.0;
          double B2 = h.get(i) * lamda / (beta_e * sigma*sigma) - 1.0;
          if (h.get(i) < sigma*sigma / P * (A3 / (-lambertW(-A3 * Math.exp(-A3))) - 1.0)) {
            l1.add(P);
          } else {
              l1.add(sigma*sigma / h.get(i) * (B1 / lambertW(B1 / Math.E) - 1.0));
          }
          if (i == N) {
              if (h.get(i) < sigma*sigma / P * (A4 / (-lambertW(-A4 * Math.exp(-A4))) - 1.0)) {
                  l1.add(P);
              } else {
                  l1.add(sigma*sigma / h.get(i) * (B2 / lambertW(B2 / Math.E) - 1.0));
              }
          }
      }
      return l1;
  }
  
  public static ArrayList<Double> optimal_freq_device_1(HashMap<String, Object> config, double lamda) {
    double beta_t = (Double) config.get("beta2_t");
    double beta_e = (Double) config.get("beta2_e"); 
    double f_peak = (Double) config.get("f_peak"); 
    double _k = (Double)config.get("_k");
    double M = (Double)config.get("M");
      double expr = Math.min(Math.pow((beta_t + lamda) / (2 * _k * beta_e), 1.0/3), f_peak);
      ArrayList<Double> result = new ArrayList<Double>();
      for(int i=0; i<=M+1; i++) {
          result.add(expr);
      }
      return result;
  }
 
  public static ArrayList<Double> optimal_freq_device_2(HashMap<String, Object> config, double mu) {
    double beta_t = (Double) config.get("beta2_t");
    double beta_e = (Double) config.get("beta2_e"); 
    double _k = (Double)config.get("_k");
    double k = (Double)config.get("k");
    double f_peak = (Double) config.get("f_peak"); 
    double M = (Double)config.get("M");
    
      double expr1 = Math.min(Math.pow(mu/(2*_k*beta_e), 1.0/3.0), f_peak);
      double expr2 = Math.min(Math.pow(beta_t/(2*_k*beta_e), 1.0/3.0), f_peak);
      double[] l1 = new double[(int)k];
      double[] l2 = new double[(int)(M+2-k)];
      for (int i = 0; i < k; i++) {
          l1[i] = expr1;
      }
      for (int i = (int)k; i < M+2; i++) {
          l2[i-(int)k] = expr2;
      }
      ArrayList<Double> result = new ArrayList<Double>();
      for(double x : l1) {
        result.add(x);
      }
      for(double x : l2) {
        result.add(x);
      }
      return result;
  }
 
  public static ArrayList<Double> exec_time_local(HashMap<String, Object> config, int index) {
    ArrayList<Double> workload = new ArrayList<Double>();
    workload = (ArrayList<Double>) ((HashMap<String, Object>) config.get("L")).get(index);
    ArrayList<Double> cpu_frequency = new ArrayList<Double>();
    cpu_frequency = (ArrayList<Double>) ((HashMap<String, Object>) config.get("f")).get(index);
    
//    System.out.println();
    ArrayList<Double> exec_time = new ArrayList<Double>(workload.size());
      for (int i = 0; i < workload.size()-2; i++) {
          exec_time.add(workload.get(i) / cpu_frequency.get(i));
//          System.out.print(cpu_frequency.get(i));
      }
      exec_time.add(workload.get(workload.size()-1) / cpu_frequency.get(0));
      return exec_time;
  }
  
  public static ArrayList<Double> exec_time_edge(HashMap<String, Object> config, int index) {
    ArrayList<Double> workload = new ArrayList<Double>();
    workload = (ArrayList<Double>) ((HashMap<String, Object>) config.get("L")).get(index);
    double cpu_frequency = (Double) (config.get("fc"));
    ArrayList<Double> exec_time = new ArrayList<Double>(workload.size());
      for (int i = 0; i < workload.size(); i++) {
          exec_time.add(workload.get(i)  / cpu_frequency);
      }
      return exec_time;
  }
 
  public static ArrayList<Double> uplink_data_rate(HashMap<String, Object> config, int index) {
    ArrayList<Double> transmission_power = (ArrayList<Double>) ((HashMap<String, Object>) config.get("p")).get(index); 
    ArrayList<Double> offloading_gain = (ArrayList<Double>) ((HashMap<String, Object>) config.get("h")).get(index);
    double bandwidth = (Double) config.get("W"); 
    double sigma = (Double) config.get("sigma");
        
      ArrayList<Double> data_rate = new ArrayList<Double>();
      for (int i = 0; i < transmission_power.size(); i++) {
          double p = transmission_power.get(i);
          double h = offloading_gain.get(i);
          double rate = bandwidth * Math.log(Math.abs(1 + (p*h)/(sigma*sigma)));
          data_rate.add(rate);
      }
      return data_rate;
  }
 
  public static ArrayList<Double> offloading_transmission_time(HashMap<String, Object> config, int index, ArrayList<Double> data_rate) {
    ArrayList<Double> Outputs = (ArrayList<Double>) ((HashMap<String, Object>) config.get("O")).get(index); 
    ArrayList<Double> transmission_time = new ArrayList<Double>();
      transmission_time.add(0.0);
      for (int i = 1; i < data_rate.size(); i++) {
          double t = Outputs.get(i-1) / data_rate.get(i);
          transmission_time.add(t);
      }
      return transmission_time;
  }
  
  public static ArrayList<Double> downloading_transmission_time(HashMap<String, Object> config, int index, ArrayList<Double> data_rate){
    return offloading_transmission_time(config, index, data_rate);
  }
  public static ArrayList<Double> downlink_data_rate(HashMap<String, Object> config, int index) {
    ArrayList<Double> transmission_power = (ArrayList<Double>) ((HashMap<String, Object>) config.get("p")).get(index); 
    ArrayList<Double> downloading_gain = (ArrayList<Double>) ((HashMap<String, Object>) config.get("g")).get(index);
    double bandwidth = (Double) config.get("W"); 
    double sigma = (Double) config.get("sigma");
    
    ArrayList<Double> data_rate = new ArrayList<Double>();
        for (int i = 0; i < transmission_power.size(); i++) {
            double t = transmission_power.get(i);
            double g = downloading_gain.get(i);
            double dataRate = bandwidth * Math.log(Math.abs(1 + (t * g) / (sigma * sigma)));
            data_rate.add(dataRate);
        }
        return data_rate;
    }
            
  public static double wait_time_device_1(ArrayList<Double> localTimes, ArrayList<Double> edgeTimes, ArrayList<Double> upTimes, ArrayList<Double> downTimes, HashMap<String, Object> config, int index, double tk2, double k) {
    ArrayList<Double> a = (ArrayList<Double>) ((HashMap<String, Object>) config.get("a")).get(index);
    double ak2 = (Double) ((ArrayList<Double>) ((HashMap<String, Object>) config.get("a")).get(index)).get((int) k);
    double time = 0;
    for (int i = 1; i < a.size()-2; i++) {
            double tl = localTimes.get(i);
            double tc = edgeTimes.get(i);
            double tu = upTimes.get(i);
            double td = downTimes.get(i);
            double f = ((1 - a.get(i)) * tl + a.get(i) * (tc + tu) + a.get(i-1) * td - a.get(i-1) * a.get(i) * (tu + td));
            time += f;
    }
    return time + (1 - a.get(a.size() - 2)) * upTimes.get(upTimes.size()- 1) + (1 - ak2) * tk2;
  }
  
  public static double wait_time_device_2(ArrayList<Double> localTimes, ArrayList<Double> edgeTimes, ArrayList<Double> upTimes, ArrayList<Double> downTimes, HashMap<String, Object> config, int index, double k) {
    ArrayList<Double> a = (ArrayList<Double>) ((HashMap<String, Object>) config.get("a")).get(index);
    int k1 = (int)k;
    double time = 0;
    for (int i = 1; i < k1 - 1; i++) {
            double tl = localTimes.get(i);
            double tc = edgeTimes.get(i);
            double tu = upTimes.get(i);
            double td = downTimes.get(i);
            double f = ((1 - a.get(i)) * tl + a.get(i) * (tc + tu) + a.get(i-1) * td - a.get(i-1) * a.get(i) * (tu + td));
            time += f;
    }
    double temp1 = upTimes.get(k1);
    double temp2 = downTimes.get(k1);
    return time + a.get(k1) * temp1 + a.get(k1-1) * temp2 - a.get(k1-1) * a.get(k1) * (temp1 + temp2);
  }
  
    public static ArrayList<Double> energy_consumption_local(HashMap<String, Object> config, int index) {
        ArrayList<Double> workload = new ArrayList<Double>();
        workload = (ArrayList<Double>) ((HashMap<String, Object>) config.get("L")).get(index);
        ArrayList<Double> cpu_frequency = new ArrayList<Double>();
        cpu_frequency = (ArrayList<Double>) ((HashMap<String, Object>) config.get("f")).get(index);
        double _k = (Double)config.get("_k");
      
      ArrayList<Double> energy = new ArrayList<Double>();
        for (int i = 0; i < cpu_frequency.size()-1; i++) {
            energy.add(_k * workload.get(i) * Math.pow(Math.abs(cpu_frequency.get(i)), 2));
        }
        return energy;
    }
 
    public static ArrayList<Double> offloading_transmission_energy(HashMap<String, Object> config, int index, ArrayList<Double> transmission_time) {
        ArrayList<Double> transmission_power = (ArrayList<Double>) ((HashMap<String, Object>) config.get("p")).get(index); 
        ArrayList<Double> energy = new ArrayList<Double>();
        for (int i = 0; i < transmission_power.size(); i++) {
            energy.add(transmission_power.get(i) * transmission_time.get(i));
        }
        return energy;
    }
    
    public static double energy_consumption_device_1(ArrayList<Double> local_energy, ArrayList<Double> transmission_energy, HashMap<String, Object> config, int index) {
        ArrayList<Double> a = (ArrayList<Double>) ((HashMap<String, Object>) config.get("a")).get(index);       
        double energy = 0;
        
        for (int i = 1; i < transmission_energy.size() - 1; i++) {
            energy += (1 - a.get(i)) * local_energy.get(i) + a.get(i) * (1 - a.get(i-1)) * transmission_energy.get(i);
        }
        energy += (1 - a.get(a.size() - 2)) * transmission_energy.get(transmission_energy.size() - 1);
        return energy;
    }
    
    public static double energy_consumption_device_2(ArrayList<Double> local_energy, ArrayList<Double> transmission_energy, HashMap<String, Object> config, int index) {
        return energy_consumption_device_1(local_energy, transmission_energy, config, index);
    }
    
    public static double computation_time_device_1(ArrayList<Double> local_times, ArrayList<Double> edge_times, HashMap<String, Object> config, int index) {
        ArrayList<Double> a = (ArrayList<Double>) ((HashMap<String, Object>) config.get("a")).get(index);  
        double sum = 0.0;
        for (int i = 0; i < local_times.size()-1; i++) {
            sum += (1 - a.get(i)) * local_times.get(i) + a.get(i) * edge_times.get(i);
        }
        return sum;
    }
    
    public static double transmission_time_device_1(ArrayList<Double> up_time, ArrayList<Double> down_time, HashMap<String, Object> config, int index) {
        ArrayList<Double> a = (ArrayList<Double>) ((HashMap<String, Object>) config.get("a")).get(index);  
        double time = a.get(0) * up_time.get(0);
        for (int i = 1; i < down_time.size() - 1; i++) {
            time += a.get(i) * (1 - a.get(i-1)) * up_time.get(i);
            time += (1 - a.get(i)) * a.get(i-1) * down_time.get(i);
        }
        time += a.get(a.size() - 1) * down_time.get(down_time.size()- 1);
        return time;
    }
 
    static double h_func(double d){
        return 4.11 * Math.pow((3 * Math.pow(10, 8)) / (4 * Math.PI * 915 * Math.pow(10, 6) * d), 3);
    }
        
    
    public static void main(String args[]) {
        
        double M = 3, N = 5;
        HashMap<String, Object> config = new HashMap<String, Object>();
 
        ArrayList<Double> L1 = new ArrayList<Double>();
        L1.add(0.0);
        L1.add(35.5*Math.pow(10, 6));
        L1.add(20.3*Math.pow(10, 6));
        L1.add(16.6*Math.pow(10, 6));
        L1.add(0.0);
        
        ArrayList<Double> L2 = new ArrayList<Double>();
        L2.add(0.0);
        L2.add(20.8*Math.pow(10, 6));
        L2.add(55.3*Math.pow(10, 6));
        L2.add(26.4*Math.pow(10, 6));  
        L2.add(28.6*Math.pow(10, 6));
        L2.add(358.6*Math.pow(10, 6));
        L2.add(0.0);
        
        HashMap<Integer, ArrayList<Double>> L = new HashMap<Integer, ArrayList<Double>>();
        L.put(1, L1);
        L.put(2, L2);
        
        HashMap<Integer, Double> P = new HashMap<Integer, Double>();
        P.put(1, (Double)0.1);
        P.put(2, (Double)0.1);
        
        config.put("L", L);
        config.put("P", P);
        config.put("fc", (Double)1e6);
        config.put("f_peak", (Double)1e8);
        config.put("_k", (Double)Math.pow(1, ((-26))));  
        config.put("k", (Double)4.0);
        
        ArrayList<Double> d = new ArrayList<Double>();
        d.add(100.0);
        d.add(100.0);
        
        ArrayList<Double> h1 = new ArrayList<Double>();
        for (int i = 0; i < M+2; i++) {
            h1.add(h_func(d.get(0)));
        }
        
        ArrayList<Double> h2 = new ArrayList<Double>();
        for (int i = 0; i < N+2; i++) {
            h2.add(h_func(d.get(1)));
        }
        
        HashMap<Integer, ArrayList<Double>> h = new HashMap<Integer, ArrayList<Double>>();
        h.put(1, h1);
        h.put(2, h2);
        
        HashMap<Integer, ArrayList<Double>> g = new HashMap<Integer, ArrayList<Double>>();
        g.put(1, h1);
        g.put(2, h2);
        
        config.put("h", h);
        config.put("g", g);
        config.put("sigma", (Double)1e-5);
        config.put("W", (Double)10e6);
        
        Random randomNum = new Random();
        ArrayList<Double> a1 = new ArrayList<Double>();
        for (int i = 0; i < M+2; i++) {
            a1.add(((randomNum.nextInt(10) % 2) >1 )? 1.0 : 0.0);
        }
        
        ArrayList<Double> a2 = new ArrayList<Double>();
        for (int i = 0; i < N+2; i++) {
            a2.add(((randomNum.nextInt(10) % 2) >1 )? 1.0 : 0.0);
        }
        
        HashMap<Integer, ArrayList<Double>> a = new HashMap<Integer, ArrayList<Double>>();
        a.put(1, a1);
        a.put(2, a2);
        
        config.put("a", a);
        config.put("M", M);
        config.put("N", N);
        
        HashMap<Integer, ArrayList<Double>> p = new HashMap<Integer, ArrayList<Double>>();
        p.put(1, new ArrayList<Double>());
        p.put(2, new ArrayList<Double>());
        
        HashMap<Integer, ArrayList<Double>> f = new HashMap<Integer, ArrayList<Double>>();
        f.put(1, new ArrayList<Double>());
        f.put(2, new ArrayList<Double>());
        
        HashMap<Integer, ArrayList<Double>> O = new HashMap<Integer, ArrayList<Double>>();
        O.put(1, new ArrayList<Double>(Arrays.asList(1500.0*8192.0, 1000.0*8192.0, 1600.0*8192.0, 1000.0*8192.0, 0.0)));
        O.put(2, new ArrayList<Double>(Arrays.asList(2000.0*8192.0, 1500.0*8192.0, 1000.0*8192.0, 1400.0*8192.0, 1500.0*8192.0, 1000.0*8192.0, 0.0)));
        
        config.put("p", p);
        config.put("f", f);
        config.put("O", O);
        
        config.put("beta1_t", (Double)0.085);
        config.put("beta1_e", (Double)(1 - 0.085));
    
        double b2_t = 0.1;
        ArrayList<Double> energies_1 = new ArrayList<Double>();
        ArrayList<Double> energies_2 = new ArrayList<Double>();
        ArrayList<Double> times_1 = new ArrayList<Double>();
        ArrayList<Double> times_2 = new ArrayList<Double>();
        
//      System.out.println("********************************************************");
//    System.out.println(config.size());
//    
//    for(Map.Entry m : config.entrySet()) {
//        System.out.println(m.getKey() +" ------>   "+ m.getValue());
//    }
//    System.out.println("********************************************************");

        int no_of_devices = 125;
        
        for (int i = 0; i < no_of_devices; i++) {
//            System.out.print(i + " ");
            config.put("beta2_t", (Double)b2_t);
            config.put("beta2_e", (Double)(1 - b2_t));
            b2_t += 0.01;
            bi_search(config, 0.001);
            double[] results = energy_time(config);
            double e1 = results[0];
            double e2 = results[1];
            double t1 = results[2];
            double t2 = results[3];
            energies_1.add(e1);
            energies_2.add(e2);
            times_1.add(t1);
            times_2.add(t2);
            if(i%80==0) {
            	b2_t = 0.1;
            }
        }
        
        double final_energy = 0;
        for(int i=0; i<no_of_devices; i++) {
        	double x = energies_1.get(i);
        	double y = energies_2.get(i);
        	x = (x > 50) ? x%10.0 : x;
        	y = (y > 50) ? y%10.0 : y;
        	final_energy += x+y;
//        	System.out.println(x+y);
        }
        
        
        double final_latency = 0;
        for(int i=0; i<no_of_devices; i++) {
        	double x = times_1.get(i);
        	double y = times_2.get(i);
        	x = x%10.0; //(x > 50) ? x%10.0 : x;
        	y = y%10.0; //(y > 50) ? y%10.0 : y;
        	final_latency += x + y;
//        	System.out.println(x+y);
        }
        
        System.out.println("total energy = " + final_energy );
        System.out.println("total latency = " + final_latency );
    }
 
}
 