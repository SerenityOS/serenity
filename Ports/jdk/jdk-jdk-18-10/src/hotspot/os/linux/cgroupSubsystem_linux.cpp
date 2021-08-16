/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>
#include <math.h>
#include <errno.h>
#include "cgroupSubsystem_linux.hpp"
#include "cgroupV1Subsystem_linux.hpp"
#include "cgroupV2Subsystem_linux.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "utilities/globalDefinitions.hpp"

// controller names have to match the *_IDX indices
static const char* cg_controller_name[] = { "cpu", "cpuset", "cpuacct", "memory", "pids" };

CgroupSubsystem* CgroupSubsystemFactory::create() {
  CgroupV1MemoryController* memory = NULL;
  CgroupV1Controller* cpuset = NULL;
  CgroupV1Controller* cpu = NULL;
  CgroupV1Controller* cpuacct = NULL;
  CgroupV1Controller* pids = NULL;
  CgroupInfo cg_infos[CG_INFO_LENGTH];
  u1 cg_type_flags = INVALID_CGROUPS_GENERIC;
  const char* proc_cgroups = "/proc/cgroups";
  const char* proc_self_cgroup = "/proc/self/cgroup";
  const char* proc_self_mountinfo = "/proc/self/mountinfo";

  bool valid_cgroup = determine_type(cg_infos, proc_cgroups, proc_self_cgroup, proc_self_mountinfo, &cg_type_flags);

  if (!valid_cgroup) {
    // Could not detect cgroup type
    return NULL;
  }
  assert(is_valid_cgroup(&cg_type_flags), "Expected valid cgroup type");

  if (is_cgroup_v2(&cg_type_flags)) {
    // Cgroups v2 case, we have all the info we need.
    // Construct the subsystem, free resources and return
    // Note: any index in cg_infos will do as the path is the same for
    //       all controllers.
    CgroupController* unified = new CgroupV2Controller(cg_infos[MEMORY_IDX]._mount_path, cg_infos[MEMORY_IDX]._cgroup_path);
    log_debug(os, container)("Detected cgroups v2 unified hierarchy");
    cleanup(cg_infos);
    return new CgroupV2Subsystem(unified);
  }

  /*
   * Cgroup v1 case:
   *
   * Use info gathered previously from /proc/self/cgroup
   * and map host mount point to
   * local one via /proc/self/mountinfo content above
   *
   * Docker example:
   * 5:memory:/docker/6558aed8fc662b194323ceab5b964f69cf36b3e8af877a14b80256e93aecb044
   *
   * Host example:
   * 5:memory:/user.slice
   *
   * Construct a path to the process specific memory and cpuset
   * cgroup directory.
   *
   * For a container running under Docker from memory example above
   * the paths would be:
   *
   * /sys/fs/cgroup/memory
   *
   * For a Host from memory example above the path would be:
   *
   * /sys/fs/cgroup/memory/user.slice
   *
   */
  assert(is_cgroup_v1(&cg_type_flags), "Cgroup v1 expected");
  for (int i = 0; i < CG_INFO_LENGTH; i++) {
    CgroupInfo info = cg_infos[i];
    if (info._data_complete) { // pids controller might have incomplete data
      if (strcmp(info._name, "memory") == 0) {
        memory = new CgroupV1MemoryController(info._root_mount_path, info._mount_path);
        memory->set_subsystem_path(info._cgroup_path);
      } else if (strcmp(info._name, "cpuset") == 0) {
        cpuset = new CgroupV1Controller(info._root_mount_path, info._mount_path);
        cpuset->set_subsystem_path(info._cgroup_path);
      } else if (strcmp(info._name, "cpu") == 0) {
        cpu = new CgroupV1Controller(info._root_mount_path, info._mount_path);
        cpu->set_subsystem_path(info._cgroup_path);
      } else if (strcmp(info._name, "cpuacct") == 0) {
        cpuacct = new CgroupV1Controller(info._root_mount_path, info._mount_path);
        cpuacct->set_subsystem_path(info._cgroup_path);
      } else if (strcmp(info._name, "pids") == 0) {
        pids = new CgroupV1Controller(info._root_mount_path, info._mount_path);
        pids->set_subsystem_path(info._cgroup_path);
      }
    } else {
      log_debug(os, container)("CgroupInfo for %s not complete", cg_controller_name[i]);
    }
  }
  cleanup(cg_infos);
  return new CgroupV1Subsystem(cpuset, cpu, cpuacct, pids, memory);
}

bool CgroupSubsystemFactory::determine_type(CgroupInfo* cg_infos,
                                            const char* proc_cgroups,
                                            const char* proc_self_cgroup,
                                            const char* proc_self_mountinfo,
                                            u1* flags) {
  FILE *mntinfo = NULL;
  FILE *cgroups = NULL;
  FILE *cgroup = NULL;
  char buf[MAXPATHLEN+1];
  char *p;
  bool is_cgroupsV2;
  // true iff all required controllers, memory, cpu, cpuset, cpuacct are enabled
  // at the kernel level.
  // pids might not be enabled on older Linux distros (SLES 12.1, RHEL 7.1)
  bool all_required_controllers_enabled;

  /*
   * Read /proc/cgroups so as to be able to distinguish cgroups v2 vs cgroups v1.
   *
   * For cgroups v1 hierarchy (hybrid or legacy), cpu, cpuacct, cpuset, memory controllers
   * must have non-zero for the hierarchy ID field and relevant controllers mounted.
   * Conversely, for cgroups v2 (unified hierarchy), cpu, cpuacct, cpuset, memory
   * controllers must have hierarchy ID 0 and the unified controller mounted.
   */
  cgroups = fopen(proc_cgroups, "r");
  if (cgroups == NULL) {
    log_debug(os, container)("Can't open %s, %s", proc_cgroups, os::strerror(errno));
    *flags = INVALID_CGROUPS_GENERIC;
    return false;
  }

  while ((p = fgets(buf, MAXPATHLEN, cgroups)) != NULL) {
    char name[MAXPATHLEN+1];
    int  hierarchy_id;
    int  enabled;

    // Format of /proc/cgroups documented via man 7 cgroups
    if (sscanf(p, "%s %d %*d %d", name, &hierarchy_id, &enabled) != 3) {
      continue;
    }
    if (strcmp(name, "memory") == 0) {
      cg_infos[MEMORY_IDX]._name = os::strdup(name);
      cg_infos[MEMORY_IDX]._hierarchy_id = hierarchy_id;
      cg_infos[MEMORY_IDX]._enabled = (enabled == 1);
    } else if (strcmp(name, "cpuset") == 0) {
      cg_infos[CPUSET_IDX]._name = os::strdup(name);
      cg_infos[CPUSET_IDX]._hierarchy_id = hierarchy_id;
      cg_infos[CPUSET_IDX]._enabled = (enabled == 1);
    } else if (strcmp(name, "cpu") == 0) {
      cg_infos[CPU_IDX]._name = os::strdup(name);
      cg_infos[CPU_IDX]._hierarchy_id = hierarchy_id;
      cg_infos[CPU_IDX]._enabled = (enabled == 1);
    } else if (strcmp(name, "cpuacct") == 0) {
      cg_infos[CPUACCT_IDX]._name = os::strdup(name);
      cg_infos[CPUACCT_IDX]._hierarchy_id = hierarchy_id;
      cg_infos[CPUACCT_IDX]._enabled = (enabled == 1);
    } else if (strcmp(name, "pids") == 0) {
      log_debug(os, container)("Detected optional pids controller entry in %s", proc_cgroups);
      cg_infos[PIDS_IDX]._name = os::strdup(name);
      cg_infos[PIDS_IDX]._hierarchy_id = hierarchy_id;
      cg_infos[PIDS_IDX]._enabled = (enabled == 1);
    }
  }
  fclose(cgroups);

  is_cgroupsV2 = true;
  all_required_controllers_enabled = true;
  for (int i = 0; i < CG_INFO_LENGTH; i++) {
    // pids controller is optional. All other controllers are required
    if (i != PIDS_IDX) {
      is_cgroupsV2 = is_cgroupsV2 && cg_infos[i]._hierarchy_id == 0;
      all_required_controllers_enabled = all_required_controllers_enabled && cg_infos[i]._enabled;
    }
    if (log_is_enabled(Debug, os, container) && !cg_infos[i]._enabled) {
      log_debug(os, container)("controller %s is not enabled\n", cg_controller_name[i]);
    }
  }

  if (!all_required_controllers_enabled) {
    // one or more required controllers disabled, disable container support
    log_debug(os, container)("One or more required controllers disabled at kernel level.");
    cleanup(cg_infos);
    *flags = INVALID_CGROUPS_GENERIC;
    return false;
  }

  /*
   * Read /proc/self/cgroup and determine:
   *  - the cgroup path for cgroups v2 or
   *  - on a cgroups v1 system, collect info for mapping
   *    the host mount point to the local one via /proc/self/mountinfo below.
   */
  cgroup = fopen(proc_self_cgroup, "r");
  if (cgroup == NULL) {
    log_debug(os, container)("Can't open %s, %s",
                             proc_self_cgroup, os::strerror(errno));
    cleanup(cg_infos);
    *flags = INVALID_CGROUPS_GENERIC;
    return false;
  }

  while ((p = fgets(buf, MAXPATHLEN, cgroup)) != NULL) {
    char *controllers;
    char *token;
    char *hierarchy_id_str;
    int  hierarchy_id;
    char *cgroup_path;

    hierarchy_id_str = strsep(&p, ":");
    hierarchy_id = atoi(hierarchy_id_str);
    /* Get controllers and base */
    controllers = strsep(&p, ":");
    cgroup_path = strsep(&p, "\n");

    if (controllers == NULL) {
      continue;
    }

    while (!is_cgroupsV2 && (token = strsep(&controllers, ",")) != NULL) {
      if (strcmp(token, "memory") == 0) {
        assert(hierarchy_id == cg_infos[MEMORY_IDX]._hierarchy_id, "/proc/cgroups and /proc/self/cgroup hierarchy mismatch for memory");
        cg_infos[MEMORY_IDX]._cgroup_path = os::strdup(cgroup_path);
      } else if (strcmp(token, "cpuset") == 0) {
        assert(hierarchy_id == cg_infos[CPUSET_IDX]._hierarchy_id, "/proc/cgroups and /proc/self/cgroup hierarchy mismatch for cpuset");
        cg_infos[CPUSET_IDX]._cgroup_path = os::strdup(cgroup_path);
      } else if (strcmp(token, "cpu") == 0) {
        assert(hierarchy_id == cg_infos[CPU_IDX]._hierarchy_id, "/proc/cgroups and /proc/self/cgroup hierarchy mismatch for cpu");
        cg_infos[CPU_IDX]._cgroup_path = os::strdup(cgroup_path);
      } else if (strcmp(token, "cpuacct") == 0) {
        assert(hierarchy_id == cg_infos[CPUACCT_IDX]._hierarchy_id, "/proc/cgroups and /proc/self/cgroup hierarchy mismatch for cpuacc");
        cg_infos[CPUACCT_IDX]._cgroup_path = os::strdup(cgroup_path);
      } else if (strcmp(token, "pids") == 0) {
        assert(hierarchy_id == cg_infos[PIDS_IDX]._hierarchy_id, "/proc/cgroups (%d) and /proc/self/cgroup (%d) hierarchy mismatch for pids",
                                                                 cg_infos[PIDS_IDX]._hierarchy_id, hierarchy_id);
        cg_infos[PIDS_IDX]._cgroup_path = os::strdup(cgroup_path);
      }
    }
    if (is_cgroupsV2) {
      for (int i = 0; i < CG_INFO_LENGTH; i++) {
        cg_infos[i]._cgroup_path = os::strdup(cgroup_path);
      }
    }
  }
  fclose(cgroup);

  // Find various mount points by reading /proc/self/mountinfo
  // mountinfo format is documented at https://www.kernel.org/doc/Documentation/filesystems/proc.txt
  mntinfo = fopen(proc_self_mountinfo, "r");
  if (mntinfo == NULL) {
      log_debug(os, container)("Can't open %s, %s",
                               proc_self_mountinfo, os::strerror(errno));
      cleanup(cg_infos);
      *flags = INVALID_CGROUPS_GENERIC;
      return false;
  }

  bool cgroupv2_mount_point_found = false;
  bool any_cgroup_mounts_found = false;
  while ((p = fgets(buf, MAXPATHLEN, mntinfo)) != NULL) {
    char tmp_mount_point[MAXPATHLEN+1];
    char tmp_fs_type[MAXPATHLEN+1];
    char tmproot[MAXPATHLEN+1];
    char tmpmount[MAXPATHLEN+1];
    char tmpcgroups[MAXPATHLEN+1];
    char *cptr = tmpcgroups;
    char *token;

    // Cgroup v2 relevant info. We only look for the _mount_path iff is_cgroupsV2 so
    // as to avoid memory stomping of the _mount_path pointer later on in the cgroup v1
    // block in the hybrid case.
    //
    if (is_cgroupsV2 && sscanf(p, "%*d %*d %*d:%*d %*s %s %*[^-]- %s %*s %*s", tmp_mount_point, tmp_fs_type) == 2) {
      // we likely have an early match return (e.g. cgroup fs match), be sure we have cgroup2 as fstype
      if (!cgroupv2_mount_point_found && strcmp("cgroup2", tmp_fs_type) == 0) {
        cgroupv2_mount_point_found = true;
        any_cgroup_mounts_found = true;
        for (int i = 0; i < CG_INFO_LENGTH; i++) {
          assert(cg_infos[i]._mount_path == NULL, "_mount_path memory stomping");
          cg_infos[i]._mount_path = os::strdup(tmp_mount_point);
        }
      }
    }

    /* Cgroup v1 relevant info
     *
     * Find the cgroup mount point for memory, cpuset, cpu, cpuacct, pids
     *
     * Example for docker:
     * 219 214 0:29 /docker/7208cebd00fa5f2e342b1094f7bed87fa25661471a4637118e65f1c995be8a34 /sys/fs/cgroup/memory ro,nosuid,nodev,noexec,relatime - cgroup cgroup rw,memory
     *
     * Example for host:
     * 34 28 0:29 / /sys/fs/cgroup/memory rw,nosuid,nodev,noexec,relatime shared:16 - cgroup cgroup rw,memory
     *
     * 44 31 0:39 / /sys/fs/cgroup/pids rw,nosuid,nodev,noexec,relatime shared:23 - cgroup cgroup rw,pids
     */
    if (sscanf(p, "%*d %*d %*d:%*d %s %s %*[^-]- %s %*s %s", tmproot, tmpmount, tmp_fs_type, tmpcgroups) == 4) {
      if (strcmp("cgroup", tmp_fs_type) != 0) {
        // Skip cgroup2 fs lines on hybrid or unified hierarchy.
        continue;
      }
      while ((token = strsep(&cptr, ",")) != NULL) {
        if (strcmp(token, "memory") == 0) {
          any_cgroup_mounts_found = true;
          assert(cg_infos[MEMORY_IDX]._mount_path == NULL, "stomping of _mount_path");
          cg_infos[MEMORY_IDX]._mount_path = os::strdup(tmpmount);
          cg_infos[MEMORY_IDX]._root_mount_path = os::strdup(tmproot);
          cg_infos[MEMORY_IDX]._data_complete = true;
        } else if (strcmp(token, "cpuset") == 0) {
          any_cgroup_mounts_found = true;
          if (cg_infos[CPUSET_IDX]._mount_path != NULL) {
            // On some systems duplicate cpuset controllers get mounted in addition to
            // the main cgroup controllers most likely under /sys/fs/cgroup. In that
            // case pick the one under /sys/fs/cgroup and discard others.
            if (strstr(cg_infos[CPUSET_IDX]._mount_path, "/sys/fs/cgroup") != cg_infos[CPUSET_IDX]._mount_path) {
              log_warning(os, container)("Duplicate cpuset controllers detected. Picking %s, skipping %s.",
                                         tmpmount, cg_infos[CPUSET_IDX]._mount_path);
              os::free(cg_infos[CPUSET_IDX]._mount_path);
              cg_infos[CPUSET_IDX]._mount_path = os::strdup(tmpmount);
            } else {
              log_warning(os, container)("Duplicate cpuset controllers detected. Picking %s, skipping %s.",
                                         cg_infos[CPUSET_IDX]._mount_path, tmpmount);
            }
          } else {
            cg_infos[CPUSET_IDX]._mount_path = os::strdup(tmpmount);
          }
          cg_infos[CPUSET_IDX]._root_mount_path = os::strdup(tmproot);
          cg_infos[CPUSET_IDX]._data_complete = true;
        } else if (strcmp(token, "cpu") == 0) {
          any_cgroup_mounts_found = true;
          assert(cg_infos[CPU_IDX]._mount_path == NULL, "stomping of _mount_path");
          cg_infos[CPU_IDX]._mount_path = os::strdup(tmpmount);
          cg_infos[CPU_IDX]._root_mount_path = os::strdup(tmproot);
          cg_infos[CPU_IDX]._data_complete = true;
        } else if (strcmp(token, "cpuacct") == 0) {
          any_cgroup_mounts_found = true;
          assert(cg_infos[CPUACCT_IDX]._mount_path == NULL, "stomping of _mount_path");
          cg_infos[CPUACCT_IDX]._mount_path = os::strdup(tmpmount);
          cg_infos[CPUACCT_IDX]._root_mount_path = os::strdup(tmproot);
          cg_infos[CPUACCT_IDX]._data_complete = true;
        } else if (strcmp(token, "pids") == 0) {
          any_cgroup_mounts_found = true;
          assert(cg_infos[PIDS_IDX]._mount_path == NULL, "stomping of _mount_path");
          cg_infos[PIDS_IDX]._mount_path = os::strdup(tmpmount);
          cg_infos[PIDS_IDX]._root_mount_path = os::strdup(tmproot);
          cg_infos[PIDS_IDX]._data_complete = true;
        }
      }
    }
  }
  fclose(mntinfo);

  // Neither cgroup2 nor cgroup filesystems mounted via /proc/self/mountinfo
  // No point in continuing.
  if (!any_cgroup_mounts_found) {
    log_trace(os, container)("No relevant cgroup controllers mounted.");
    cleanup(cg_infos);
    *flags = INVALID_CGROUPS_NO_MOUNT;
    return false;
  }

  if (is_cgroupsV2) {
    if (!cgroupv2_mount_point_found) {
      log_trace(os, container)("Mount point for cgroupv2 not found in /proc/self/mountinfo");
      cleanup(cg_infos);
      *flags = INVALID_CGROUPS_V2;
      return false;
    }
    // Cgroups v2 case, we have all the info we need.
    *flags = CGROUPS_V2;
    return true;
  }

  // What follows is cgroups v1
  log_debug(os, container)("Detected cgroups hybrid or legacy hierarchy, using cgroups v1 controllers");

  if (!cg_infos[MEMORY_IDX]._data_complete) {
    log_debug(os, container)("Required cgroup v1 memory subsystem not found");
    cleanup(cg_infos);
    *flags = INVALID_CGROUPS_V1;
    return false;
  }
  if (!cg_infos[CPUSET_IDX]._data_complete) {
    log_debug(os, container)("Required cgroup v1 cpuset subsystem not found");
    cleanup(cg_infos);
    *flags = INVALID_CGROUPS_V1;
    return false;
  }
  if (!cg_infos[CPU_IDX]._data_complete) {
    log_debug(os, container)("Required cgroup v1 cpu subsystem not found");
    cleanup(cg_infos);
    *flags = INVALID_CGROUPS_V1;
    return false;
  }
  if (!cg_infos[CPUACCT_IDX]._data_complete) {
    log_debug(os, container)("Required cgroup v1 cpuacct subsystem not found");
    cleanup(cg_infos);
    *flags = INVALID_CGROUPS_V1;
    return false;
  }
  if (log_is_enabled(Debug, os, container) && !cg_infos[PIDS_IDX]._data_complete) {
    log_debug(os, container)("Optional cgroup v1 pids subsystem not found");
    // keep the other controller info, pids is optional
  }
  // Cgroups v1 case, we have all the info we need.
  *flags = CGROUPS_V1;
  return true;
};

void CgroupSubsystemFactory::cleanup(CgroupInfo* cg_infos) {
  assert(cg_infos != NULL, "Invariant");
  for (int i = 0; i < CG_INFO_LENGTH; i++) {
    os::free(cg_infos[i]._name);
    os::free(cg_infos[i]._cgroup_path);
    os::free(cg_infos[i]._root_mount_path);
    os::free(cg_infos[i]._mount_path);
  }
}

/* active_processor_count
 *
 * Calculate an appropriate number of active processors for the
 * VM to use based on these three inputs.
 *
 * cpu affinity
 * cgroup cpu quota & cpu period
 * cgroup cpu shares
 *
 * Algorithm:
 *
 * Determine the number of available CPUs from sched_getaffinity
 *
 * If user specified a quota (quota != -1), calculate the number of
 * required CPUs by dividing quota by period.
 *
 * If shares are in effect (shares != -1), calculate the number
 * of CPUs required for the shares by dividing the share value
 * by PER_CPU_SHARES.
 *
 * All results of division are rounded up to the next whole number.
 *
 * If neither shares or quotas have been specified, return the
 * number of active processors in the system.
 *
 * If both shares and quotas have been specified, the results are
 * based on the flag PreferContainerQuotaForCPUCount.  If true,
 * return the quota value.  If false return the smallest value
 * between shares or quotas.
 *
 * If shares and/or quotas have been specified, the resulting number
 * returned will never exceed the number of active processors.
 *
 * return:
 *    number of CPUs
 */
int CgroupSubsystem::active_processor_count() {
  int quota_count = 0, share_count = 0;
  int cpu_count, limit_count;
  int result;

  // We use a cache with a timeout to avoid performing expensive
  // computations in the event this function is called frequently.
  // [See 8227006].
  CachingCgroupController* contrl = cpu_controller();
  CachedMetric* cpu_limit = contrl->metrics_cache();
  if (!cpu_limit->should_check_metric()) {
    int val = (int)cpu_limit->value();
    log_trace(os, container)("CgroupSubsystem::active_processor_count (cached): %d", val);
    return val;
  }

  cpu_count = limit_count = os::Linux::active_processor_count();
  int quota  = cpu_quota();
  int period = cpu_period();
  int share  = cpu_shares();

  if (quota > -1 && period > 0) {
    quota_count = ceilf((float)quota / (float)period);
    log_trace(os, container)("CPU Quota count based on quota/period: %d", quota_count);
  }
  if (share > -1) {
    share_count = ceilf((float)share / (float)PER_CPU_SHARES);
    log_trace(os, container)("CPU Share count based on shares: %d", share_count);
  }

  // If both shares and quotas are setup results depend
  // on flag PreferContainerQuotaForCPUCount.
  // If true, limit CPU count to quota
  // If false, use minimum of shares and quotas
  if (quota_count !=0 && share_count != 0) {
    if (PreferContainerQuotaForCPUCount) {
      limit_count = quota_count;
    } else {
      limit_count = MIN2(quota_count, share_count);
    }
  } else if (quota_count != 0) {
    limit_count = quota_count;
  } else if (share_count != 0) {
    limit_count = share_count;
  }

  result = MIN2(cpu_count, limit_count);
  log_trace(os, container)("OSContainer::active_processor_count: %d", result);

  // Update cached metric to avoid re-reading container settings too often
  cpu_limit->set_value(result, OSCONTAINER_CACHE_TIMEOUT);

  return result;
}

/* memory_limit_in_bytes
 *
 * Return the limit of available memory for this process.
 *
 * return:
 *    memory limit in bytes or
 *    -1 for unlimited
 *    OSCONTAINER_ERROR for not supported
 */
jlong CgroupSubsystem::memory_limit_in_bytes() {
  CachingCgroupController* contrl = memory_controller();
  CachedMetric* memory_limit = contrl->metrics_cache();
  if (!memory_limit->should_check_metric()) {
    return memory_limit->value();
  }
  jlong mem_limit = read_memory_limit_in_bytes();
  // Update cached metric to avoid re-reading container settings too often
  memory_limit->set_value(mem_limit, OSCONTAINER_CACHE_TIMEOUT);
  return mem_limit;
}

jlong CgroupSubsystem::limit_from_str(char* limit_str) {
  if (limit_str == NULL) {
    return OSCONTAINER_ERROR;
  }
  // Unlimited memory in cgroups is the literal string 'max' for
  // some controllers, for example the pids controller.
  if (strcmp("max", limit_str) == 0) {
    os::free(limit_str);
    return (jlong)-1;
  }
  julong limit;
  if (sscanf(limit_str, JULONG_FORMAT, &limit) != 1) {
    os::free(limit_str);
    return OSCONTAINER_ERROR;
  }
  os::free(limit_str);
  return (jlong)limit;
}
