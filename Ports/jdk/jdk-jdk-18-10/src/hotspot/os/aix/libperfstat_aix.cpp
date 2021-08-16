/*
 * Copyright (c) 2012, 2018 SAP SE. All rights reserved.
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

#include "libperfstat_aix.hpp"
#include "misc_aix.hpp"

#include <dlfcn.h>

// Handle to the libperfstat.
static void* g_libhandle = NULL;

typedef int (*fun_perfstat_cpu_total_t) (perfstat_id_t *name, PERFSTAT_CPU_TOTAL_T_LATEST* userbuff,
                                         int sizeof_userbuff, int desired_number);

typedef int (*fun_perfstat_memory_total_t) (perfstat_id_t *name, perfstat_memory_total_t* userbuff,
                                            int sizeof_userbuff, int desired_number);

typedef int (*fun_perfstat_partition_total_t) (perfstat_id_t *name,
    PERFSTAT_PARTITON_TOTAL_T_LATEST* userbuff, int sizeof_userbuff,
    int desired_number);

typedef int (*fun_perfstat_wpar_total_t) (perfstat_id_wpar_t *name,
    PERFSTAT_WPAR_TOTAL_T_LATEST* userbuff, int sizeof_userbuff,
    int desired_number);

typedef void (*fun_perfstat_reset_t) ();

typedef cid_t (*fun_wpar_getcid_t) ();

static fun_perfstat_cpu_total_t     g_fun_perfstat_cpu_total = NULL;
static fun_perfstat_memory_total_t  g_fun_perfstat_memory_total = NULL;
static fun_perfstat_partition_total_t g_fun_perfstat_partition_total = NULL;
static fun_perfstat_wpar_total_t    g_fun_perfstat_wpar_total = NULL;
static fun_perfstat_reset_t         g_fun_perfstat_reset = NULL;
static fun_wpar_getcid_t            g_fun_wpar_getcid = NULL;

bool libperfstat::init() {

  // Dynamically load the libperfstat porting library.
  g_libhandle = dlopen("/usr/lib/libperfstat.a(shr_64.o)", RTLD_MEMBER | RTLD_NOW);
  if (!g_libhandle) {
    trcVerbose("Cannot load libperfstat.a (dlerror: %s)", dlerror());
    return false;
  }

  // Resolve function pointers

#define RESOLVE_FUN_NO_ERROR(name) \
  g_fun_##name = (fun_##name##_t) dlsym(g_libhandle, #name);

#define RESOLVE_FUN(name) \
  RESOLVE_FUN_NO_ERROR(name) \
  if (!g_fun_##name) { \
    trcVerbose("Cannot resolve " #name "() from libperfstat.a\n" \
                      "   (dlerror: %s)", dlerror()); \
    return false; \
  }

  // These functions may or may not be there depending on the OS release.
  RESOLVE_FUN_NO_ERROR(perfstat_partition_total);
  RESOLVE_FUN_NO_ERROR(perfstat_wpar_total);
  RESOLVE_FUN_NO_ERROR(wpar_getcid);

  // These functions are required for every release.
  RESOLVE_FUN(perfstat_cpu_total);
  RESOLVE_FUN(perfstat_memory_total);
  RESOLVE_FUN(perfstat_reset);

  trcVerbose("libperfstat loaded.");

  return true;
}

void libperfstat::cleanup() {

  if (g_libhandle) {
    dlclose(g_libhandle);
    g_libhandle = NULL;
  }

  g_fun_perfstat_cpu_total = NULL;
  g_fun_perfstat_memory_total = NULL;
  g_fun_perfstat_partition_total = NULL;
  g_fun_perfstat_wpar_total = NULL;
  g_fun_perfstat_reset = NULL;
  g_fun_wpar_getcid = NULL;

}

int libperfstat::perfstat_memory_total(perfstat_id_t *name,
                                       perfstat_memory_total_t* userbuff,
                                       int sizeof_userbuff, int desired_number) {
  if (g_fun_perfstat_memory_total == NULL) {
    return -1;
  }
  return g_fun_perfstat_memory_total(name, userbuff, sizeof_userbuff, desired_number);
}

int libperfstat::perfstat_cpu_total(perfstat_id_t *name, PERFSTAT_CPU_TOTAL_T_LATEST* userbuff,
                                    int sizeof_userbuff, int desired_number) {
  if (g_fun_perfstat_cpu_total == NULL) {
    return -1;
  }
  return g_fun_perfstat_cpu_total(name, userbuff, sizeof_userbuff, desired_number);
}

int libperfstat::perfstat_partition_total(perfstat_id_t *name, PERFSTAT_PARTITON_TOTAL_T_LATEST* userbuff,
                                          int sizeof_userbuff, int desired_number) {
  if (g_fun_perfstat_partition_total == NULL) {
    return -1;
  }
  return g_fun_perfstat_partition_total(name, userbuff, sizeof_userbuff, desired_number);
}

int libperfstat::perfstat_wpar_total(perfstat_id_wpar_t *name, PERFSTAT_WPAR_TOTAL_T_LATEST* userbuff,
                                     int sizeof_userbuff, int desired_number) {
  if (g_fun_perfstat_wpar_total == NULL) {
    return -1;
  }
  return g_fun_perfstat_wpar_total(name, userbuff, sizeof_userbuff, desired_number);
}

void libperfstat::perfstat_reset() {
  if (g_fun_perfstat_reset != NULL) {
    g_fun_perfstat_reset();
  }
}

cid_t libperfstat::wpar_getcid() {
  if (g_fun_wpar_getcid == NULL) {
    return (cid_t) -1;
  }
  return g_fun_wpar_getcid();
}


//////////////////// convenience functions, release-independent /////////////////////////////


// Retrieve global cpu information.
bool libperfstat::get_cpuinfo(cpuinfo_t* pci) {

  assert(pci, "get_cpuinfo: invalid parameter");
  memset(pci, 0, sizeof(cpuinfo_t));

  PERFSTAT_CPU_TOTAL_T_LATEST psct;
  memset (&psct, '\0', sizeof(psct));

  if (-1 == libperfstat::perfstat_cpu_total(NULL, &psct, sizeof(PERFSTAT_CPU_TOTAL_T_LATEST), 1)) {
    if (-1 == libperfstat::perfstat_cpu_total(NULL, &psct, sizeof(perfstat_cpu_total_t_71), 1)) {
      if (-1 == libperfstat::perfstat_cpu_total(NULL, &psct, sizeof(perfstat_cpu_total_t_61), 1)) {
        if (-1 == libperfstat::perfstat_cpu_total(NULL, &psct, sizeof(perfstat_cpu_total_t_53), 1)) {
          trcVerbose("perfstat_cpu_total() failed (errno=%d)", errno);
          return false;
        }
      }
    }
  }

  // Global cpu information.
  strcpy(pci->description, psct.description);
  pci->processorHZ = psct.processorHZ;
  pci->ncpus = psct.ncpus;
  for (int i = 0; i < 3; i++) {
    pci->loadavg[i] = (double) psct.loadavg[i] / (1 << SBITS);
  }

  pci->user_clock_ticks = psct.user;
  pci->sys_clock_ticks  = psct.sys;
  pci->idle_clock_ticks = psct.idle;
  pci->wait_clock_ticks = psct.wait;

  return true;
}

// Retrieve partition information.
bool libperfstat::get_partitioninfo(partitioninfo_t* ppi) {

  assert(ppi, "get_partitioninfo: invalid parameter");
  memset(ppi, 0, sizeof(partitioninfo_t));

  PERFSTAT_PARTITON_TOTAL_T_LATEST pspt;
  memset(&pspt, '\0', sizeof(pspt));

  bool ame_details = true;

  if (-1 == libperfstat::perfstat_partition_total(NULL, &pspt, sizeof(PERFSTAT_PARTITON_TOTAL_T_LATEST), 1)) {
    if (-1 == libperfstat::perfstat_partition_total(NULL, &pspt, sizeof(perfstat_partition_total_t_71), 1)) {
      ame_details = false;
      if (-1 == libperfstat::perfstat_partition_total(NULL, &pspt, sizeof(perfstat_partition_total_t_61), 1)) {
        if (-1 == libperfstat::perfstat_partition_total(NULL, &pspt, sizeof(perfstat_partition_total_t_53), 1)) {
          if (-1 == libperfstat::perfstat_partition_total(NULL, &pspt, sizeof(perfstat_partition_total_t_53_5), 1)) {
            trcVerbose("perfstat_partition_total() failed (errno=%d)", errno);
            return false;
          }
        }
      }
    }
  }

  // partition type info
  ppi->shared_enabled = pspt.type.b.shared_enabled;
  ppi->smt_capable = pspt.type.b.smt_capable;
  ppi->smt_enabled = pspt.type.b.smt_enabled;
  ppi->lpar_capable = pspt.type.b.lpar_capable;
  ppi->lpar_enabled = pspt.type.b.lpar_enabled;
  ppi->dlpar_capable = pspt.type.b.dlpar_capable;
  ppi->capped = pspt.type.b.capped;
  ppi->kernel_is_64 = pspt.type.b.kernel_is_64;
  ppi->pool_util_authority = pspt.type.b.pool_util_authority;
  ppi->donate_capable = pspt.type.b.donate_capable;
  ppi->donate_enabled = pspt.type.b.donate_enabled;
  ppi->ams_capable = pspt.type.b.ams_capable;
  ppi->ams_enabled = pspt.type.b.ams_enabled;
  ppi->power_save = pspt.type.b.power_save;
  ppi->ame_enabled = pspt.type.b.ame_enabled;

  // partition total info
  ppi->online_cpus = pspt.online_cpus;
  ppi->entitled_proc_capacity = pspt.entitled_proc_capacity;
  ppi->var_proc_capacity_weight = pspt.var_proc_capacity_weight;
  ppi->phys_cpus_pool = pspt.phys_cpus_pool;
  ppi->pool_id = pspt.pool_id;
  ppi->entitled_pool_capacity = pspt.entitled_pool_capacity;
  strcpy(ppi->name, pspt.name);

  // Added values to ppi that we need for later computation of cpu utilization
  // ( pool authorization needed for pool_idle_time ??? )
  ppi->timebase_last   = pspt.timebase_last;
  ppi->pool_idle_time  = pspt.pool_idle_time;
  ppi->pcpu_tics_user  = pspt.puser;
  ppi->pcpu_tics_sys   = pspt.psys;
  ppi->pcpu_tics_idle  = pspt.pidle;
  ppi->pcpu_tics_wait  = pspt.pwait;

  // Additional AME information.
  if (ame_details) {
    ppi->true_memory = pspt.true_memory * 4096;
    ppi->expanded_memory = pspt.expanded_memory * 4096;
    ppi->target_memexp_factr = pspt.target_memexp_factr;
    ppi->current_memexp_factr = pspt.current_memexp_factr;
    ppi->cmcs_total_time = pspt.cmcs_total_time;
  }

  return true;
}

// Retrieve wpar information.
bool libperfstat::get_wparinfo(wparinfo_t* pwi) {

  assert(pwi, "get_wparinfo: invalid parameter");
  memset(pwi, 0, sizeof(wparinfo_t));

  if (libperfstat::wpar_getcid() <= 0) {
    return false;
  }

  PERFSTAT_WPAR_TOTAL_T_LATEST pswt;
  memset (&pswt, '\0', sizeof(pswt));

  if (-1 == libperfstat::perfstat_wpar_total(NULL, &pswt, sizeof(PERFSTAT_WPAR_TOTAL_T_LATEST), 1)) {
    if (-1 == libperfstat::perfstat_wpar_total(NULL, &pswt, sizeof(perfstat_wpar_total_t_61), 1)) {
      trcVerbose("perfstat_wpar_total() failed (errno=%d)", errno);
      return false;
    }
  }

  // WPAR type info.
  pwi->app_wpar = pswt.type.b.app_wpar;
  pwi->cpu_rset = pswt.type.b.cpu_rset;
  pwi->cpu_xrset = pswt.type.b.cpu_xrset;
  pwi->cpu_limits = pswt.type.b.cpu_limits;
  pwi->mem_limits = pswt.type.b.mem_limits;
  // WPAR total info.
  strcpy(pwi->name, pswt.name);
  pwi->wpar_id = pswt.wpar_id;
  pwi->cpu_limit = pswt.cpu_limit;
  pwi->mem_limit = pswt.mem_limit;

  return true;
}
