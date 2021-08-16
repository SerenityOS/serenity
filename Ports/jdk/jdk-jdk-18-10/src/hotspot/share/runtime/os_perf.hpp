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

#ifndef SHARE_RUNTIME_OS_PERF_HPP
#define SHARE_RUNTIME_OS_PERF_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

#define FUNCTIONALITY_NOT_IMPLEMENTED -8

class EnvironmentVariable : public CHeapObj<mtInternal> {
 public:
  char* _key;
  char* _value;

  EnvironmentVariable() {
    _key = NULL;
    _value = NULL;
  }

  ~EnvironmentVariable() {
    FREE_C_HEAP_ARRAY(char, _key);
    FREE_C_HEAP_ARRAY(char, _value);
  }

  EnvironmentVariable(char* key, char* value) {
    _key = key;
    _value = value;
  }

};


class CPUInformation : public CHeapObj<mtInternal> {
 private:
  int   _no_of_sockets;
  int   _no_of_cores;
  int   _no_of_hw_threads;
  const char* _description;
  const char* _name;

 public:
  CPUInformation() {
    _no_of_sockets = 0;
    _no_of_cores = 0;
    _no_of_hw_threads = 0;
    _description = NULL;
    _name = NULL;
  }

  int number_of_sockets(void) const {
    return _no_of_sockets;
  }

  void set_number_of_sockets(int no_of_sockets) {
    _no_of_sockets = no_of_sockets;
  }

  int number_of_cores(void) const {
    return _no_of_cores;
  }

  void set_number_of_cores(int no_of_cores) {
    _no_of_cores = no_of_cores;
  }

  int number_of_hardware_threads(void) const {
    return _no_of_hw_threads;
  }

  void set_number_of_hardware_threads(int no_of_hw_threads) {
    _no_of_hw_threads = no_of_hw_threads;
  }

  const char* cpu_name(void)  const {
    return _name;
  }

  void set_cpu_name(const char* cpu_name) {
    _name = cpu_name;
  }

  const char* cpu_description(void) const {
    return _description;
  }

  void set_cpu_description(const char* cpu_description) {
    _description = cpu_description;
  }
};

class SystemProcess : public CHeapObj<mtInternal> {
 private:
  int   _pid;
  char* _name;
  char* _path;
  char* _command_line;
  SystemProcess* _next;

 public:
  SystemProcess() {
    _pid  = 0;
    _name = NULL;
    _path = NULL;
    _command_line = NULL;
    _next = NULL;
  }

  SystemProcess(int pid, char* name, char* path, char* command_line, SystemProcess* next) {
    _pid = pid;
    _name = name;
    _path = path;
    _command_line = command_line;
    _next = next;
  }

  void set_next(SystemProcess* sys_process) {
    _next = sys_process;
  }

  SystemProcess* next(void) const {
    return _next;
  }

  int pid(void) const {
    return _pid;
  }

  void set_pid(int pid) {
    _pid = pid;
  }

  const char* name(void) const {
    return _name;
  }

  void set_name(char* name) {
    _name = name;
  }

  const char* path(void) const {
    return _path;
  }

  void set_path(char* path) {
    _path = path;
  }

  const char* command_line(void) const {
    return _command_line;
  }

  void set_command_line(char* command_line) {
    _command_line = command_line;
  }

  virtual ~SystemProcess(void) {
    FREE_C_HEAP_ARRAY(char, _name);
    FREE_C_HEAP_ARRAY(char, _path);
    FREE_C_HEAP_ARRAY(char, _command_line);
  }
};

class NetworkInterface : public ResourceObj {
 private:
  char* _name;
  uint64_t _bytes_in;
  uint64_t _bytes_out;
  NetworkInterface* _next;

  NONCOPYABLE(NetworkInterface);

 public:
  NetworkInterface(const char* name, uint64_t bytes_in, uint64_t bytes_out, NetworkInterface* next) :
  _name(NULL),
  _bytes_in(bytes_in),
  _bytes_out(bytes_out),
  _next(next) {
    assert(name != NULL, "invariant");
    const size_t length = strlen(name);
    assert(allocated_on_res_area(), "invariant");
    _name = NEW_RESOURCE_ARRAY(char, length + 1);
    strncpy(_name, name, length + 1);
    assert(strncmp(_name, name, length) == 0, "invariant");
  }

  NetworkInterface* next() const {
    return _next;
  }

  const char* get_name() const {
    return _name;
  }

  uint64_t get_bytes_out() const {
    return _bytes_out;
  }

  uint64_t get_bytes_in() const {
    return _bytes_in;
  }
};

class CPUInformationInterface : public CHeapObj<mtInternal> {
 private:
  CPUInformation* _cpu_info;
 public:
  CPUInformationInterface();
  bool initialize();
  ~CPUInformationInterface();
  int cpu_information(CPUInformation& cpu_info);
};

class CPUPerformanceInterface : public CHeapObj<mtInternal> {
 private:
  class CPUPerformance;
  CPUPerformance* _impl;
 public:
  CPUPerformanceInterface();
  ~CPUPerformanceInterface();
  bool initialize();

  int cpu_load(int which_logical_cpu, double* const cpu_load) const;
  int context_switch_rate(double* const rate) const;
  int cpu_load_total_process(double* const cpu_load) const;
  int cpu_loads_process(double* const pjvmUserLoad,
                        double* const pjvmKernelLoad,
                        double* const psystemTotalLoad) const;
};

class SystemProcessInterface : public CHeapObj<mtInternal> {
 private:
   class SystemProcesses;
   SystemProcesses* _impl;
 public:
   SystemProcessInterface();
   ~SystemProcessInterface();
   bool initialize();

  // information about system processes
  int system_processes(SystemProcess** system_procs, int* const no_of_sys_processes) const;
};

class NetworkPerformanceInterface : public CHeapObj<mtInternal> {
 private:
  class NetworkPerformance;
  NetworkPerformance* _impl;
  NONCOPYABLE(NetworkPerformanceInterface);

 public:
  NetworkPerformanceInterface();
  bool initialize();
  ~NetworkPerformanceInterface();
  int network_utilization(NetworkInterface** network_interfaces) const;
};

#endif // SHARE_RUNTIME_OS_PERF_HPP
