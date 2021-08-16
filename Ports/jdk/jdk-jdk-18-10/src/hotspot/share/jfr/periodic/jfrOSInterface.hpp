/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_JFR_PERIODIC_JFROSINTERFACE_HPP
#define SHARE_JFR_PERIODIC_JFROSINTERFACE_HPP

#include "jfr/utilities/jfrAllocation.hpp"

class CPUInformation;
class EnvironmentVariable;
class NetworkInterface;
class SystemProcess;

class JfrOSInterface: public JfrCHeapObj {
  friend class JfrRecorder;
 private:
  class JfrOSInterfaceImpl;
  JfrOSInterfaceImpl* _impl;

  JfrOSInterface();
  ~JfrOSInterface();
  bool initialize();
  static JfrOSInterface& instance();
  static JfrOSInterface* create();
  static void destroy();

 public:
  static int cpu_information(CPUInformation& cpu_info);
  static int cpu_load(int which_logical_cpu, double* cpu_load);
  static int context_switch_rate(double* rate);
  static int cpu_load_total_process(double* cpu_load);
  static int cpu_loads_process(double* pjvmUserLoad, double* pjvmKernelLoad, double* psystemTotalLoad);
  static int os_version(char** os_version);
  static const char* virtualization_name();
  static int generate_initial_environment_variable_events();
  static int system_processes(SystemProcess** system_processes, int* no_of_sys_processes);
  static int network_utilization(NetworkInterface** network_interfaces);
};

#endif // SHARE_JFR_PERIODIC_JFROSINTERFACE_HPP
