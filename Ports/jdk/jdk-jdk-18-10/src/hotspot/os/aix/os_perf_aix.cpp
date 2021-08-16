/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "jvm.h"
#include "memory/allocation.inline.hpp"
#include "os_aix.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/os_perf.hpp"
#include "utilities/globalDefinitions.hpp"

#include CPU_HEADER(vm_version_ext)

#include <stdio.h>
#include <stdarg.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/resource.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <pthread.h>
#include <limits.h>

/**
   /proc/[number]/stat
              Status information about the process.  This is used by ps(1).  It is defined in /usr/src/linux/fs/proc/array.c.

              The fields, in order, with their proper scanf(3) format specifiers, are:

              1. pid %d The process id.

              2. comm %s
                     The filename of the executable, in parentheses.  This is visible whether or not the executable is swapped out.

              3. state %c
                     One  character  from  the  string "RSDZTW" where R is running, S is sleeping in an interruptible wait, D is waiting in uninterruptible disk
                     sleep, Z is zombie, T is traced or stopped (on a signal), and W is paging.

              4. ppid %d
                     The PID of the parent.

              5. pgrp %d
                     The process group ID of the process.

              6. session %d
                     The session ID of the process.

              7. tty_nr %d
                     The tty the process uses.

              8. tpgid %d
                     The process group ID of the process which currently owns the tty that the process is connected to.

              9. flags %lu
                     The flags of the process.  The math bit is decimal 4, and the traced bit is decimal 10.

              10. minflt %lu
                     The number of minor faults the process has made which have not required loading a memory page from disk.

              11. cminflt %lu
                     The number of minor faults that the process's waited-for children have made.

              12. majflt %lu
                     The number of major faults the process has made which have required loading a memory page from disk.

              13. cmajflt %lu
                     The number of major faults that the process's waited-for children have made.

              14. utime %lu
                     The number of jiffies that this process has been scheduled in user mode.

              15. stime %lu
                     The number of jiffies that this process has been scheduled in kernel mode.

              16. cutime %ld
                     The number of jiffies that this process's waited-for children have been scheduled in user mode. (See also times(2).)

              17. cstime %ld
                     The number of jiffies that this process' waited-for children have been scheduled in kernel mode.

              18. priority %ld
                     The standard nice value, plus fifteen.  The value is never negative in the kernel.

              19. nice %ld
                     The nice value ranges from 19 (nicest) to -19 (not nice to others).

              20. 0 %ld  This value is hard coded to 0 as a placeholder for a removed field.

              21. itrealvalue %ld
                     The time in jiffies before the next SIGALRM is sent to the process due to an interval timer.

              22. starttime %lu
                     The time in jiffies the process started after system boot.

              23. vsize %lu
                     Virtual memory size in bytes.

              24. rss %ld
                     Resident Set Size: number of pages the process has in real memory, minus 3 for administrative purposes. This is just the pages which  count
                     towards text, data, or stack space.  This does not include pages which have not been demand-loaded in, or which are swapped out.

              25. rlim %lu
                     Current limit in bytes on the rss of the process (usually 4294967295 on i386).

              26. startcode %lu
                     The address above which program text can run.

              27. endcode %lu
                     The address below which program text can run.

              28. startstack %lu
                     The address of the start of the stack.

              29. kstkesp %lu
                     The current value of esp (stack pointer), as found in the kernel stack page for the process.

              30. kstkeip %lu
                     The current EIP (instruction pointer).

              31. signal %lu
                     The bitmap of pending signals (usually 0).

              32. blocked %lu
                     The bitmap of blocked signals (usually 0, 2 for shells).

              33. sigignore %lu
                     The bitmap of ignored signals.

              34. sigcatch %lu
                     The bitmap of catched signals.

              35. wchan %lu
                     This  is the "channel" in which the process is waiting.  It is the address of a system call, and can be looked up in a namelist if you need
                     a textual name.  (If you have an up-to-date /etc/psdatabase, then try ps -l to see the WCHAN field in action.)

              36. nswap %lu
                     Number of pages swapped - not maintained.

              37. cnswap %lu
                     Cumulative nswap for child processes.

              38. exit_signal %d
                     Signal to be sent to parent when we die.

              39. processor %d
                     CPU number last executed on.



 ///// SSCANF FORMAT STRING. Copy and use.

field:        1  2  3  4  5  6  7  8  9   10  11  12  13  14  15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30  31  32  33  34  35  36  37  38 39
format:       %d %s %c %d %d %d %d %d %lu %lu %lu %lu %lu %lu %lu %ld %ld %ld %ld %ld %ld %lu %lu %ld %lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %d %d


*/

/**
 * For platforms that have them, when declaring
 * a printf-style function,
 *   formatSpec is the parameter number (starting at 1)
 *       that is the format argument ("%d pid %s")
 *   params is the parameter number where the actual args to
 *       the format starts. If the args are in a va_list, this
 *       should be 0.
 */
#ifndef PRINTF_ARGS
#  define PRINTF_ARGS(formatSpec,  params) ATTRIBUTE_PRINTF(formatSpec, params)
#endif

#ifndef SCANF_ARGS
#  define SCANF_ARGS(formatSpec,   params) ATTRIBUTE_SCANF(formatSpec, params)
#endif

#ifndef _PRINTFMT_
#  define _PRINTFMT_
#endif

#ifndef _SCANFMT_
#  define _SCANFMT_
#endif


struct CPUPerfTicks {
  uint64_t  used;
  uint64_t  usedKernel;
  uint64_t  total;
};

typedef enum {
  CPU_LOAD_VM_ONLY,
  CPU_LOAD_GLOBAL,
} CpuLoadTarget;

enum {
  UNDETECTED,
  UNDETECTABLE,
  LINUX26_NPTL,
  BAREMETAL
};

struct CPUPerfCounters {
  int   nProcs;
  CPUPerfTicks jvmTicks;
  CPUPerfTicks* cpus;
};

static double get_cpu_load(int which_logical_cpu, CPUPerfCounters* counters, double* pkernelLoad, CpuLoadTarget target);

/** reads /proc/<pid>/stat data, with some checks and some skips.
 *  Ensure that 'fmt' does _NOT_ contain the first two "%d %s"
 */
static int SCANF_ARGS(2, 0) vread_statdata(const char* procfile, _SCANFMT_ const char* fmt, va_list args) {
  FILE*f;
  int n;
  char buf[2048];

  if ((f = fopen(procfile, "r")) == NULL) {
    return -1;
  }

  if ((n = fread(buf, 1, sizeof(buf), f)) != -1) {
    char *tmp;

    buf[n-1] = '\0';
    /** skip through pid and exec name. */
    if ((tmp = strrchr(buf, ')')) != NULL) {
      // skip the ')' and the following space
      // but check that buffer is long enough
      tmp += 2;
      if (tmp < buf + n) {
        n = vsscanf(tmp, fmt, args);
      }
    }
  }

  fclose(f);

  return n;
}

static int SCANF_ARGS(2, 3) read_statdata(const char* procfile, _SCANFMT_ const char* fmt, ...) {
  int   n;
  va_list args;

  va_start(args, fmt);
  n = vread_statdata(procfile, fmt, args);
  va_end(args);
  return n;
}

/**
 * on Linux we got the ticks related information from /proc/stat
 * this does not work on AIX, libperfstat might be an alternative
 */
static OSReturn get_total_ticks(int which_logical_cpu, CPUPerfTicks* pticks) {
  return OS_ERR;
}

/** read user and system ticks from a named procfile, assumed to be in 'stat' format then. */
static int read_ticks(const char* procfile, uint64_t* userTicks, uint64_t* systemTicks) {
  return read_statdata(procfile, "%*c %*d %*d %*d %*d %*d %*u %*u %*u %*u %*u " UINT64_FORMAT " " UINT64_FORMAT,
    userTicks, systemTicks);
}

/**
 * Return the number of ticks spent in any of the processes belonging
 * to the JVM on any CPU.
 */
static OSReturn get_jvm_ticks(CPUPerfTicks* pticks) {
  return OS_ERR;
}

/**
 * Return the load of the CPU as a double. 1.0 means the CPU process uses all
 * available time for user or system processes, 0.0 means the CPU uses all time
 * being idle.
 *
 * Returns a negative value if there is a problem in determining the CPU load.
 */
static double get_cpu_load(int which_logical_cpu, CPUPerfCounters* counters, double* pkernelLoad, CpuLoadTarget target) {
  uint64_t udiff, kdiff, tdiff;
  CPUPerfTicks* pticks;
  CPUPerfTicks  tmp;
  double user_load;

  *pkernelLoad = 0.0;

  if (target == CPU_LOAD_VM_ONLY) {
    pticks = &counters->jvmTicks;
  } else if (-1 == which_logical_cpu) {
    pticks = &counters->cpus[counters->nProcs];
  } else {
    pticks = &counters->cpus[which_logical_cpu];
  }

  tmp = *pticks;

  if (target == CPU_LOAD_VM_ONLY) {
    if (get_jvm_ticks(pticks) != OS_OK) {
      return -1.0;
    }
  } else if (get_total_ticks(which_logical_cpu, pticks) != OS_OK) {
    return -1.0;
  }

  // seems like we sometimes end up with less kernel ticks when
  // reading /proc/self/stat a second time, timing issue between cpus?
  if (pticks->usedKernel < tmp.usedKernel) {
    kdiff = 0;
  } else {
    kdiff = pticks->usedKernel - tmp.usedKernel;
  }
  tdiff = pticks->total - tmp.total;
  udiff = pticks->used - tmp.used;

  if (tdiff == 0) {
    return 0.0;
  } else if (tdiff < (udiff + kdiff)) {
    tdiff = udiff + kdiff;
  }
  *pkernelLoad = (kdiff / (double)tdiff);
  // BUG9044876, normalize return values to sane values
  *pkernelLoad = MAX2<double>(*pkernelLoad, 0.0);
  *pkernelLoad = MIN2<double>(*pkernelLoad, 1.0);

  user_load = (udiff / (double)tdiff);
  user_load = MAX2<double>(user_load, 0.0);
  user_load = MIN2<double>(user_load, 1.0);

  return user_load;
}

static int SCANF_ARGS(1, 2) parse_stat(_SCANFMT_ const char* fmt, ...) {
  return OS_ERR;
}

static int get_noof_context_switches(uint64_t* switches) {
  return parse_stat("ctxt " UINT64_FORMAT "\n", switches);
}

/** returns boot time in _seconds_ since epoch */
static int get_boot_time(uint64_t* time) {
  return parse_stat("btime " UINT64_FORMAT "\n", time);
}

static int perf_context_switch_rate(double* rate) {
  static pthread_mutex_t contextSwitchLock = PTHREAD_MUTEX_INITIALIZER;
  static uint64_t      bootTime;
  static uint64_t      lastTimeNanos;
  static uint64_t      lastSwitches;
  static double        lastRate;

  uint64_t bt = 0;
  int res = 0;

  // First time through bootTime will be zero.
  if (bootTime == 0) {
    uint64_t tmp;
    if (get_boot_time(&tmp) < 0) {
      return OS_ERR;
    }
    bt = tmp * 1000;
  }

  res = OS_OK;

  pthread_mutex_lock(&contextSwitchLock);
  {

    uint64_t sw;
    s8 t, d;

    if (bootTime == 0) {
      // First interval is measured from boot time which is
      // seconds since the epoch. Thereafter we measure the
      // elapsed time using javaTimeNanos as it is monotonic-
      // non-decreasing.
      lastTimeNanos = os::javaTimeNanos();
      t = os::javaTimeMillis();
      d = t - bt;
      // keep bootTime zero for now to use as a first-time-through flag
    } else {
      t = os::javaTimeNanos();
      d = nanos_to_millis(t - lastTimeNanos);
    }

    if (d == 0) {
      *rate = lastRate;
    } else if (get_noof_context_switches(&sw) == 0) {
      *rate      = ( (double)(sw - lastSwitches) / d ) * 1000;
      lastRate     = *rate;
      lastSwitches = sw;
      if (bootTime != 0) {
        lastTimeNanos = t;
      }
    } else {
      *rate = 0;
      res   = OS_ERR;
    }
    if (*rate <= 0) {
      *rate = 0;
      lastRate = 0;
    }

    if (bootTime == 0) {
      bootTime = bt;
    }
  }
  pthread_mutex_unlock(&contextSwitchLock);

  return res;
}

class CPUPerformanceInterface::CPUPerformance : public CHeapObj<mtInternal> {
  friend class CPUPerformanceInterface;
 private:
  CPUPerfCounters _counters;

  int cpu_load(int which_logical_cpu, double* cpu_load);
  int context_switch_rate(double* rate);
  int cpu_load_total_process(double* cpu_load);
  int cpu_loads_process(double* pjvmUserLoad, double* pjvmKernelLoad, double* psystemTotalLoad);

 public:
  CPUPerformance();
  bool initialize();
  ~CPUPerformance();
};

CPUPerformanceInterface::CPUPerformance::CPUPerformance() {
  _counters.nProcs = os::active_processor_count();
  _counters.cpus = NULL;
}

bool CPUPerformanceInterface::CPUPerformance::initialize() {
  size_t array_entry_count = _counters.nProcs + 1;
  _counters.cpus = NEW_C_HEAP_ARRAY(CPUPerfTicks, array_entry_count, mtInternal);
  memset(_counters.cpus, 0, array_entry_count * sizeof(*_counters.cpus));

  // For the CPU load total
  get_total_ticks(-1, &_counters.cpus[_counters.nProcs]);

  // For each CPU
  for (int i = 0; i < _counters.nProcs; i++) {
    get_total_ticks(i, &_counters.cpus[i]);
  }
  // For JVM load
  get_jvm_ticks(&_counters.jvmTicks);

  // initialize context switch system
  // the double is only for init
  double init_ctx_switch_rate;
  perf_context_switch_rate(&init_ctx_switch_rate);

  return true;
}

CPUPerformanceInterface::CPUPerformance::~CPUPerformance() {
  if (_counters.cpus != NULL) {
    FREE_C_HEAP_ARRAY(char, _counters.cpus);
  }
}

int CPUPerformanceInterface::CPUPerformance::cpu_load(int which_logical_cpu, double* cpu_load) {
  double u, s;
  u = get_cpu_load(which_logical_cpu, &_counters, &s, CPU_LOAD_GLOBAL);
  if (u < 0) {
    *cpu_load = 0.0;
    return OS_ERR;
  }
  // Cap total systemload to 1.0
  *cpu_load = MIN2<double>((u + s), 1.0);
  return OS_OK;
}

int CPUPerformanceInterface::CPUPerformance::cpu_load_total_process(double* cpu_load) {
  double u, s;
  u = get_cpu_load(-1, &_counters, &s, CPU_LOAD_VM_ONLY);
  if (u < 0) {
    *cpu_load = 0.0;
    return OS_ERR;
  }
  *cpu_load = u + s;
  return OS_OK;
}

int CPUPerformanceInterface::CPUPerformance::cpu_loads_process(double* pjvmUserLoad, double* pjvmKernelLoad, double* psystemTotalLoad) {
  double u, s, t;

  assert(pjvmUserLoad != NULL, "pjvmUserLoad not inited");
  assert(pjvmKernelLoad != NULL, "pjvmKernelLoad not inited");
  assert(psystemTotalLoad != NULL, "psystemTotalLoad not inited");

  u = get_cpu_load(-1, &_counters, &s, CPU_LOAD_VM_ONLY);
  if (u < 0) {
    *pjvmUserLoad = 0.0;
    *pjvmKernelLoad = 0.0;
    *psystemTotalLoad = 0.0;
    return OS_ERR;
  }

  cpu_load(-1, &t);
  // clamp at user+system and 1.0
  if (u + s > t) {
    t = MIN2<double>(u + s, 1.0);
  }

  *pjvmUserLoad = u;
  *pjvmKernelLoad = s;
  *psystemTotalLoad = t;

  return OS_OK;
}

int CPUPerformanceInterface::CPUPerformance::context_switch_rate(double* rate) {
  return perf_context_switch_rate(rate);
}

CPUPerformanceInterface::CPUPerformanceInterface() {
  _impl = NULL;
}

bool CPUPerformanceInterface::initialize() {
  _impl = new CPUPerformanceInterface::CPUPerformance();
  return _impl->initialize();
}

CPUPerformanceInterface::~CPUPerformanceInterface() {
  if (_impl != NULL) {
    delete _impl;
  }
}

int CPUPerformanceInterface::cpu_load(int which_logical_cpu, double* cpu_load) const {
  return _impl->cpu_load(which_logical_cpu, cpu_load);
}

int CPUPerformanceInterface::cpu_load_total_process(double* cpu_load) const {
  return _impl->cpu_load_total_process(cpu_load);
}

int CPUPerformanceInterface::cpu_loads_process(double* pjvmUserLoad, double* pjvmKernelLoad, double* psystemTotalLoad) const {
  return _impl->cpu_loads_process(pjvmUserLoad, pjvmKernelLoad, psystemTotalLoad);
}

int CPUPerformanceInterface::context_switch_rate(double* rate) const {
  return _impl->context_switch_rate(rate);
}

class SystemProcessInterface::SystemProcesses : public CHeapObj<mtInternal> {
  friend class SystemProcessInterface;
 private:
  class ProcessIterator : public CHeapObj<mtInternal> {
    friend class SystemProcessInterface::SystemProcesses;
   private:
    DIR*           _dir;
    struct dirent* _entry;
    bool           _valid;
    char           _exeName[PATH_MAX];
    char           _exePath[PATH_MAX];

    ProcessIterator();
    ~ProcessIterator();
    bool initialize();

    bool is_valid() const { return _valid; }
    bool is_valid_entry(struct dirent* entry) const;
    bool is_dir(const char* name) const;
    int  fsize(const char* name, uint64_t& size) const;

    char* allocate_string(const char* str) const;
    void  get_exe_name();
    char* get_exe_path();
    char* get_cmdline();

    int current(SystemProcess* process_info);
    int next_process();
  };

  ProcessIterator* _iterator;
  SystemProcesses();
  bool initialize();
  ~SystemProcesses();

  //information about system processes
  int system_processes(SystemProcess** system_processes, int* no_of_sys_processes) const;
};

bool SystemProcessInterface::SystemProcesses::ProcessIterator::is_dir(const char* name) const {
  struct stat mystat;
  int ret_val = 0;

  ret_val = stat(name, &mystat);
  if (ret_val < 0) {
    return false;
  }
  ret_val = S_ISDIR(mystat.st_mode);
  return ret_val > 0;
}

int SystemProcessInterface::SystemProcesses::ProcessIterator::fsize(const char* name, uint64_t& size) const {
  assert(name != NULL, "name pointer is NULL!");
  size = 0;
  struct stat fbuf;

  if (stat(name, &fbuf) < 0) {
    return OS_ERR;
  }
  size = fbuf.st_size;
  return OS_OK;
}

// if it has a numeric name, is a directory and has a 'stat' file in it
bool SystemProcessInterface::SystemProcesses::ProcessIterator::is_valid_entry(struct dirent* entry) const {
  char buffer[PATH_MAX];
  uint64_t size = 0;

  if (atoi(entry->d_name) != 0) {
    jio_snprintf(buffer, PATH_MAX, "/proc/%s", entry->d_name);
    buffer[PATH_MAX - 1] = '\0';

    if (is_dir(buffer)) {
      jio_snprintf(buffer, PATH_MAX, "/proc/%s/stat", entry->d_name);
      buffer[PATH_MAX - 1] = '\0';
      if (fsize(buffer, size) != OS_ERR) {
        return true;
      }
    }
  }
  return false;
}

// get exe-name from /proc/<pid>/stat
void SystemProcessInterface::SystemProcesses::ProcessIterator::get_exe_name() {
  FILE* fp;
  char  buffer[PATH_MAX];

  jio_snprintf(buffer, PATH_MAX, "/proc/%s/stat", _entry->d_name);
  buffer[PATH_MAX - 1] = '\0';
  if ((fp = fopen(buffer, "r")) != NULL) {
    if (fgets(buffer, PATH_MAX, fp) != NULL) {
      char* start, *end;
      // exe-name is between the first pair of ( and )
      start = strchr(buffer, '(');
      if (start != NULL && start[1] != '\0') {
        start++;
        end = strrchr(start, ')');
        if (end != NULL) {
          size_t len;
          len = MIN2<size_t>(end - start, sizeof(_exeName) - 1);
          memcpy(_exeName, start, len);
          _exeName[len] = '\0';
        }
      }
    }
    fclose(fp);
  }
}

// get command line from /proc/<pid>/cmdline
char* SystemProcessInterface::SystemProcesses::ProcessIterator::get_cmdline() {
  FILE* fp;
  char  buffer[PATH_MAX];
  char* cmdline = NULL;

  jio_snprintf(buffer, PATH_MAX, "/proc/%s/cmdline", _entry->d_name);
  buffer[PATH_MAX - 1] = '\0';
  if ((fp = fopen(buffer, "r")) != NULL) {
    size_t size = 0;
    char   dummy;

    // find out how long the file is (stat always returns 0)
    while (fread(&dummy, 1, 1, fp) == 1) {
      size++;
    }
    if (size > 0) {
      cmdline = NEW_C_HEAP_ARRAY(char, size + 1, mtInternal);
      cmdline[0] = '\0';
      if (fseek(fp, 0, SEEK_SET) == 0) {
        if (fread(cmdline, 1, size, fp) == size) {
          // the file has the arguments separated by '\0',
          // so we translate '\0' to ' '
          for (size_t i = 0; i < size; i++) {
            if (cmdline[i] == '\0') {
              cmdline[i] = ' ';
            }
          }
          cmdline[size] = '\0';
        }
      }
    }
    fclose(fp);
  }
  return cmdline;
}

// get full path to exe from /proc/<pid>/exe symlink
char* SystemProcessInterface::SystemProcesses::ProcessIterator::get_exe_path() {
  char buffer[PATH_MAX];

  jio_snprintf(buffer, PATH_MAX, "/proc/%s/exe", _entry->d_name);
  buffer[PATH_MAX - 1] = '\0';
  return realpath(buffer, _exePath);
}

char* SystemProcessInterface::SystemProcesses::ProcessIterator::allocate_string(const char* str) const {
  if (str != NULL) {
    return os::strdup_check_oom(str, mtInternal);
  }
  return NULL;
}

int SystemProcessInterface::SystemProcesses::ProcessIterator::current(SystemProcess* process_info) {
  if (!is_valid()) {
    return OS_ERR;
  }

  process_info->set_pid(atoi(_entry->d_name));

  get_exe_name();
  process_info->set_name(allocate_string(_exeName));

  if (get_exe_path() != NULL) {
     process_info->set_path(allocate_string(_exePath));
  }

  char* cmdline = NULL;
  cmdline = get_cmdline();
  if (cmdline != NULL) {
    process_info->set_command_line(allocate_string(cmdline));
    FREE_C_HEAP_ARRAY(char, cmdline);
  }

  return OS_OK;
}

int SystemProcessInterface::SystemProcesses::ProcessIterator::next_process() {
  if (!is_valid()) {
    return OS_ERR;
  }

  do {
    _entry = os::readdir(_dir);
    if (_entry == NULL) {
      // Error or reached end.  Could use errno to distinguish those cases.
      _valid = false;
      return OS_ERR;
    }
  } while(!is_valid_entry(_entry));

  _valid = true;
  return OS_OK;
}

SystemProcessInterface::SystemProcesses::ProcessIterator::ProcessIterator() {
  _dir = NULL;
  _entry = NULL;
  _valid = false;
}

bool SystemProcessInterface::SystemProcesses::ProcessIterator::initialize() {
  // Not yet implemented.
  return false;
}

SystemProcessInterface::SystemProcesses::ProcessIterator::~ProcessIterator() {
  if (_dir != NULL) {
    os::closedir(_dir);
  }
}

SystemProcessInterface::SystemProcesses::SystemProcesses() {
  _iterator = NULL;
}

bool SystemProcessInterface::SystemProcesses::initialize() {
  _iterator = new SystemProcessInterface::SystemProcesses::ProcessIterator();
  return _iterator->initialize();
}

SystemProcessInterface::SystemProcesses::~SystemProcesses() {
  if (_iterator != NULL) {
    delete _iterator;
  }
}

int SystemProcessInterface::SystemProcesses::system_processes(SystemProcess** system_processes, int* no_of_sys_processes) const {
  assert(system_processes != NULL, "system_processes pointer is NULL!");
  assert(no_of_sys_processes != NULL, "system_processes counter pointers is NULL!");
  assert(_iterator != NULL, "iterator is NULL!");

  // initialize pointers
  *no_of_sys_processes = 0;
  *system_processes = NULL;

  while (_iterator->is_valid()) {
    SystemProcess* tmp = new SystemProcess();
    _iterator->current(tmp);

    //if already existing head
    if (*system_processes != NULL) {
      //move "first to second"
      tmp->set_next(*system_processes);
    }
    // new head
    *system_processes = tmp;
    // increment
    (*no_of_sys_processes)++;
    // step forward
    _iterator->next_process();
  }
  return OS_OK;
}

int SystemProcessInterface::system_processes(SystemProcess** system_procs, int* no_of_sys_processes) const {
  return _impl->system_processes(system_procs, no_of_sys_processes);
}

SystemProcessInterface::SystemProcessInterface() {
  _impl = NULL;
}

bool SystemProcessInterface::initialize() {
  _impl = new SystemProcessInterface::SystemProcesses();
  return _impl->initialize();
}

SystemProcessInterface::~SystemProcessInterface() {
  if (_impl != NULL) {
    delete _impl;
  }
}

CPUInformationInterface::CPUInformationInterface() {
  _cpu_info = NULL;
}

bool CPUInformationInterface::initialize() {
  _cpu_info = new CPUInformation();
  _cpu_info->set_number_of_hardware_threads(VM_Version_Ext::number_of_threads());
  _cpu_info->set_number_of_cores(VM_Version_Ext::number_of_cores());
  _cpu_info->set_number_of_sockets(VM_Version_Ext::number_of_sockets());
  _cpu_info->set_cpu_name(VM_Version_Ext::cpu_name());
  _cpu_info->set_cpu_description(VM_Version_Ext::cpu_description());
  return true;
}

CPUInformationInterface::~CPUInformationInterface() {
  if (_cpu_info != NULL) {
    if (_cpu_info->cpu_name() != NULL) {
      const char* cpu_name = _cpu_info->cpu_name();
      FREE_C_HEAP_ARRAY(char, cpu_name);
      _cpu_info->set_cpu_name(NULL);
    }
    if (_cpu_info->cpu_description() != NULL) {
       const char* cpu_desc = _cpu_info->cpu_description();
       FREE_C_HEAP_ARRAY(char, cpu_desc);
      _cpu_info->set_cpu_description(NULL);
    }
    delete _cpu_info;
  }
}

int CPUInformationInterface::cpu_information(CPUInformation& cpu_info) {
  if (_cpu_info == NULL) {
    return OS_ERR;
  }

  cpu_info = *_cpu_info; // shallow copy assignment
  return OS_OK;
}

class NetworkPerformanceInterface::NetworkPerformance : public CHeapObj<mtInternal> {
  friend class NetworkPerformanceInterface;
 private:
  NetworkPerformance();
  NONCOPYABLE(NetworkPerformance);
  bool initialize();
  ~NetworkPerformance();
  int network_utilization(NetworkInterface** network_interfaces) const;
};

NetworkPerformanceInterface::NetworkPerformance::NetworkPerformance() {

}

bool NetworkPerformanceInterface::NetworkPerformance::initialize() {
  return true;
}

NetworkPerformanceInterface::NetworkPerformance::~NetworkPerformance() {
}

int NetworkPerformanceInterface::NetworkPerformance::network_utilization(NetworkInterface** network_interfaces) const
{
  return FUNCTIONALITY_NOT_IMPLEMENTED;
}

NetworkPerformanceInterface::NetworkPerformanceInterface() {
  _impl = NULL;
}

NetworkPerformanceInterface::~NetworkPerformanceInterface() {
  if (_impl != NULL) {
    delete _impl;
  }
}

bool NetworkPerformanceInterface::initialize() {
  _impl = new NetworkPerformanceInterface::NetworkPerformance();
  return _impl->initialize();
}

int NetworkPerformanceInterface::network_utilization(NetworkInterface** network_interfaces) const {
  return _impl->network_utilization(network_interfaces);
}
