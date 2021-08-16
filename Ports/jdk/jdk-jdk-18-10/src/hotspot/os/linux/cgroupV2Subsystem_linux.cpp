/*
 * Copyright (c) 2020, 2021, Red Hat Inc.
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

#include "cgroupV2Subsystem_linux.hpp"

/* cpu_shares
 *
 * Return the amount of cpu shares available to the process
 *
 * return:
 *    Share number (typically a number relative to 1024)
 *                 (2048 typically expresses 2 CPUs worth of processing)
 *    -1 for no share setup
 *    OSCONTAINER_ERROR for not supported
 */
int CgroupV2Subsystem::cpu_shares() {
  GET_CONTAINER_INFO(int, _unified, "/cpu.weight",
                     "Raw value for CPU shares is: %d", "%d", shares);
  // Convert default value of 100 to no shares setup
  if (shares == 100) {
    log_debug(os, container)("CPU Shares is: %d", -1);
    return -1;
  }

  // CPU shares (OCI) value needs to get translated into
  // a proper Cgroups v2 value. See:
  // https://github.com/containers/crun/blob/master/crun.1.md#cpu-controller
  //
  // Use the inverse of (x == OCI value, y == cgroupsv2 value):
  // ((262142 * y - 1)/9999) + 2 = x
  //
  int x = 262142 * shares - 1;
  double frac = x/9999.0;
  x = ((int)frac) + 2;
  log_trace(os, container)("Scaled CPU shares value is: %d", x);
  // Since the scaled value is not precise, return the closest
  // multiple of PER_CPU_SHARES for a more conservative mapping
  if ( x <= PER_CPU_SHARES ) {
     // will always map to 1 CPU
     log_debug(os, container)("CPU Shares is: %d", x);
     return x;
  }
  int f = x/PER_CPU_SHARES;
  int lower_multiple = f * PER_CPU_SHARES;
  int upper_multiple = (f + 1) * PER_CPU_SHARES;
  int distance_lower = MAX2(lower_multiple, x) - MIN2(lower_multiple, x);
  int distance_upper = MAX2(upper_multiple, x) - MIN2(upper_multiple, x);
  x = distance_lower <= distance_upper ? lower_multiple : upper_multiple;
  log_trace(os, container)("Closest multiple of %d of the CPU Shares value is: %d", PER_CPU_SHARES, x);
  log_debug(os, container)("CPU Shares is: %d", x);
  return x;
}

/* cpu_quota
 *
 * Return the number of microseconds per period
 * process is guaranteed to run.
 *
 * return:
 *    quota time in microseconds
 *    -1 for no quota
 *    OSCONTAINER_ERROR for not supported
 */
int CgroupV2Subsystem::cpu_quota() {
  char * cpu_quota_str = cpu_quota_val();
  int limit = (int)limit_from_str(cpu_quota_str);
  log_trace(os, container)("CPU Quota is: %d", limit);
  return limit;
}

char * CgroupV2Subsystem::cpu_cpuset_cpus() {
  GET_CONTAINER_INFO_CPTR(cptr, _unified, "/cpuset.cpus",
                     "cpuset.cpus is: %s", "%1023s", cpus, 1024);
  if (cpus == NULL) {
    return NULL;
  }
  return os::strdup(cpus);
}

char* CgroupV2Subsystem::cpu_quota_val() {
  GET_CONTAINER_INFO_CPTR(cptr, _unified, "/cpu.max",
                     "Raw value for CPU quota is: %s", "%s %*d", quota, 1024);
  if (quota == NULL) {
    return NULL;
  }
  return os::strdup(quota);
}

char * CgroupV2Subsystem::cpu_cpuset_memory_nodes() {
  GET_CONTAINER_INFO_CPTR(cptr, _unified, "/cpuset.mems",
                     "cpuset.mems is: %s", "%1023s", mems, 1024);
  if (mems == NULL) {
    return NULL;
  }
  return os::strdup(mems);
}

int CgroupV2Subsystem::cpu_period() {
  GET_CONTAINER_INFO(int, _unified, "/cpu.max",
                     "CPU Period is: %d", "%*s %d", period);
  return period;
}

/* memory_usage_in_bytes
 *
 * Return the amount of used memory used by this cgroup and decendents
 *
 * return:
 *    memory usage in bytes or
 *    -1 for unlimited
 *    OSCONTAINER_ERROR for not supported
 */
jlong CgroupV2Subsystem::memory_usage_in_bytes() {
  GET_CONTAINER_INFO(jlong, _unified, "/memory.current",
                     "Memory Usage is: " JLONG_FORMAT, JLONG_FORMAT, memusage);
  return memusage;
}

jlong CgroupV2Subsystem::memory_soft_limit_in_bytes() {
  char* mem_soft_limit_str = mem_soft_limit_val();
  return limit_from_str(mem_soft_limit_str);
}

jlong CgroupV2Subsystem::memory_max_usage_in_bytes() {
  // Log this string at trace level so as to make tests happy.
  log_trace(os, container)("Maximum Memory Usage is not supported.");
  return OSCONTAINER_ERROR; // not supported
}

char* CgroupV2Subsystem::mem_soft_limit_val() {
  GET_CONTAINER_INFO_CPTR(cptr, _unified, "/memory.low",
                         "Memory Soft Limit is: %s", "%s", mem_soft_limit_str, 1024);
  if (mem_soft_limit_str == NULL) {
    return NULL;
  }
  return os::strdup(mem_soft_limit_str);
}

// Note that for cgroups v2 the actual limits set for swap and
// memory live in two different files, memory.swap.max and memory.max
// respectively. In order to properly report a cgroup v1 like
// compound value we need to sum the two values. Setting a swap limit
// without also setting a memory limit is not allowed.
jlong CgroupV2Subsystem::memory_and_swap_limit_in_bytes() {
  char* mem_swp_limit_str = mem_swp_limit_val();
  jlong swap_limit = limit_from_str(mem_swp_limit_str);
  if (swap_limit >= 0) {
    jlong memory_limit = read_memory_limit_in_bytes();
    assert(memory_limit >= 0, "swap limit without memory limit?");
    return memory_limit + swap_limit;
  }
  return swap_limit;
}

char* CgroupV2Subsystem::mem_swp_limit_val() {
  GET_CONTAINER_INFO_CPTR(cptr, _unified, "/memory.swap.max",
                         "Memory and Swap Limit is: %s", "%s", mem_swp_limit_str, 1024);
  if (mem_swp_limit_str == NULL) {
    return NULL;
  }
  return os::strdup(mem_swp_limit_str);
}

/* memory_limit_in_bytes
 *
 * Return the limit of available memory for this process.
 *
 * return:
 *    memory limit in bytes or
 *    -1 for unlimited, OSCONTAINER_ERROR for an error
 */
jlong CgroupV2Subsystem::read_memory_limit_in_bytes() {
  char * mem_limit_str = mem_limit_val();
  jlong limit = limit_from_str(mem_limit_str);
  if (log_is_enabled(Trace, os, container)) {
    if (limit == -1) {
      log_trace(os, container)("Memory Limit is: Unlimited");
    } else {
      log_trace(os, container)("Memory Limit is: " JLONG_FORMAT, limit);
    }
  }
  return limit;
}

char* CgroupV2Subsystem::mem_limit_val() {
  GET_CONTAINER_INFO_CPTR(cptr, _unified, "/memory.max",
                         "Raw value for memory limit is: %s", "%s", mem_limit_str, 1024);
  if (mem_limit_str == NULL) {
    return NULL;
  }
  return os::strdup(mem_limit_str);
}

char* CgroupV2Controller::construct_path(char* mount_path, char *cgroup_path) {
  char buf[MAXPATHLEN+1];
  int buflen;
  strncpy(buf, mount_path, MAXPATHLEN);
  buf[MAXPATHLEN] = '\0';
  buflen = strlen(buf);
  if ((buflen + strlen(cgroup_path)) > MAXPATHLEN) {
    return NULL;
  }
  strncat(buf, cgroup_path, MAXPATHLEN-buflen);
  buf[MAXPATHLEN] = '\0';
  return os::strdup(buf);
}

char* CgroupV2Subsystem::pids_max_val() {
  GET_CONTAINER_INFO_CPTR(cptr, _unified, "/pids.max",
                     "Maximum number of tasks is: %s", "%s %*d", pidsmax, 1024);
  if (pidsmax == NULL) {
    return NULL;
  }
  return os::strdup(pidsmax);
}

/* pids_max
 *
 * Return the maximum number of tasks available to the process
 *
 * return:
 *    maximum number of tasks
 *    -1 for unlimited
 *    OSCONTAINER_ERROR for not supported
 */
jlong CgroupV2Subsystem::pids_max() {
  char * pidsmax_str = pids_max_val();
  return limit_from_str(pidsmax_str);
}

