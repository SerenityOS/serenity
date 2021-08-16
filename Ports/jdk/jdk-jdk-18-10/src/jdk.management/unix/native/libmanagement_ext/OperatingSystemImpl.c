/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include "jvm.h"
#include "management_ext.h"
#include "com_sun_management_internal_OperatingSystemImpl.h"

#include <sys/types.h>
#include <sys/stat.h>
#if defined(_ALLBSD_SOURCE)
#include <sys/sysctl.h>
#ifdef __APPLE__
#include <sys/param.h>
#include <sys/mount.h>
#include <mach/mach.h>
#include <sys/proc_info.h>
#include <libproc.h>
#endif
#elif !defined(_AIX)
#include <sys/swap.h>
#endif
#include <sys/resource.h>
#include <sys/times.h>
#ifndef _ALLBSD_SOURCE
#include <sys/sysinfo.h>
#endif
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdlib.h>
#include <unistd.h>

#if defined(_AIX)
#include <libperfstat.h>
#endif

static jlong page_size = 0;

#if defined(_ALLBSD_SOURCE) || defined(_AIX)
#define MB      (1024UL * 1024UL)
#else

/* This gets us the new structured proc interfaces of 5.6 & later */
/* - see comment in <sys/procfs.h> */
#define _STRUCTURED_PROC 1
#include <sys/procfs.h>

#endif /* _ALLBSD_SOURCE */

#if defined(_AIX)
  #define DIR DIR64
  #define dirent dirent64
  #define opendir opendir64
  #define readdir readdir64
  #define closedir closedir64
#endif

// true = get available swap in bytes
// false = get total swap in bytes
static jlong get_total_or_available_swap_space_size(JNIEnv* env, jboolean available) {
#if defined(__linux__)
    int ret;
    FILE *fp;
    jlong total = 0, avail = 0;

    struct sysinfo si;
    ret = sysinfo(&si);
    if (ret != 0) {
        throw_internal_error(env, "sysinfo failed to get swap size");
    }
    total = (jlong)si.totalswap * si.mem_unit;
    avail = (jlong)si.freeswap * si.mem_unit;

    return available ? avail : total;
#elif defined(__APPLE__)
    struct xsw_usage vmusage;
    size_t size = sizeof(vmusage);
    if (sysctlbyname("vm.swapusage", &vmusage, &size, NULL, 0) != 0) {
        throw_internal_error(env, "sysctlbyname failed");
    }
    return available ? (jlong)vmusage.xsu_avail : (jlong)vmusage.xsu_total;
#else /* _ALLBSD_SOURCE */
    /*
     * XXXBSD: there's no way available to get swap info in
     *         FreeBSD.  Usage of libkvm is not an option here
     */
    // throw_internal_error(env, "Unimplemented in FreeBSD");
    return (0);
#endif
}

JNIEXPORT void JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_initialize0
  (JNIEnv *env, jclass cls)
{
    page_size = sysconf(_SC_PAGESIZE);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getCommittedVirtualMemorySize0
  (JNIEnv *env, jobject mbean)
{
#if defined(__linux__)
    FILE *fp;
    unsigned long vsize = 0;

    if ((fp = fopen("/proc/self/stat", "r")) == NULL) {
        throw_internal_error(env, "Unable to open /proc/self/stat");
        return -1;
    }

    // Ignore everything except the vsize entry
    if (fscanf(fp, "%*d %*s %*c %*d %*d %*d %*d %*d %*u %*u %*u %*u %*u %*d %*d %*d %*d %*d %*d %*u %*u %*d %lu %*[^\n]\n", &vsize) == EOF) {
        throw_internal_error(env, "Unable to get virtual memory usage");
        fclose(fp);
        return -1;
    }

    fclose(fp);
    return (jlong)vsize;
#elif defined(__APPLE__)
    struct task_basic_info t_info;
    mach_msg_type_number_t t_info_count = TASK_BASIC_INFO_COUNT;

    kern_return_t res = task_info(mach_task_self(), TASK_BASIC_INFO, (task_info_t)&t_info, &t_info_count);
    if (res != KERN_SUCCESS) {
        throw_internal_error(env, "task_info failed");
    }
    return t_info.virtual_size;
#else /* _ALLBSD_SOURCE */
    /*
     * XXXBSD: there's no way available to do it in FreeBSD, AFAIK.
     */
    // throw_internal_error(env, "Unimplemented in FreeBSD");
    return (64 * MB);
#endif
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getTotalSwapSpaceSize0
  (JNIEnv *env, jobject mbean)
{
    return get_total_or_available_swap_space_size(env, JNI_FALSE);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getFreeSwapSpaceSize0
  (JNIEnv *env, jobject mbean)
{
    return get_total_or_available_swap_space_size(env, JNI_TRUE);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getProcessCpuTime0
  (JNIEnv *env, jobject mbean)
{
#ifdef __APPLE__
    struct rusage usage;
    if (getrusage(RUSAGE_SELF, &usage) != 0) {
        throw_internal_error(env, "getrusage failed");
        return -1;
    }
    jlong microsecs =
        usage.ru_utime.tv_sec * 1000 * 1000 + usage.ru_utime.tv_usec +
        usage.ru_stime.tv_sec * 1000 * 1000 + usage.ru_stime.tv_usec;
    return microsecs * 1000;
#else
    jlong clk_tck, ns_per_clock_tick;
    jlong cpu_time_ns;
    struct tms time;

    /*
     * BSDNOTE: FreeBSD implements _SC_CLK_TCK since FreeBSD 5, so
     *          add a magic to handle it
     */
#if defined(_SC_CLK_TCK)
    clk_tck = (jlong) sysconf(_SC_CLK_TCK);
#elif defined(__linux__) || defined(_ALLBSD_SOURCE)
    clk_tck = 100;
#endif
    if (clk_tck == -1) {
        throw_internal_error(env,
                             "sysconf failed - not able to get clock tick");
        return -1;
    }

    times(&time);
    ns_per_clock_tick = (jlong) 1000 * 1000 * 1000 / (jlong) clk_tck;
    cpu_time_ns = ((jlong)time.tms_utime + (jlong) time.tms_stime) *
                      ns_per_clock_tick;
    return cpu_time_ns;
#endif
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getFreeMemorySize0
  (JNIEnv *env, jobject mbean)
{
#ifdef __APPLE__
    mach_msg_type_number_t count;
    vm_statistics_data_t vm_stats;
    kern_return_t res;

    count = HOST_VM_INFO_COUNT;
    res = host_statistics(mach_host_self(), HOST_VM_INFO, (host_info_t)&vm_stats, &count);
    if (res != KERN_SUCCESS) {
        throw_internal_error(env, "host_statistics failed");
        return -1;
    }
    return (jlong)vm_stats.free_count * page_size;
#elif defined(_ALLBSD_SOURCE)
    /*
     * XXBSDL no way to do it in FreeBSD
     */
    // throw_internal_error(env, "unimplemented in FreeBSD")
    return (128 * MB);
#elif defined(_AIX)
    perfstat_memory_total_t memory_info;
    if (-1 != perfstat_memory_total(NULL, &memory_info, sizeof(perfstat_memory_total_t), 1)) {
        return (jlong)(memory_info.real_free * 4L * 1024L);
    }
    return -1;
#else // solaris / linux
    jlong num_avail_physical_pages = sysconf(_SC_AVPHYS_PAGES);
    return (num_avail_physical_pages * page_size);
#endif
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getTotalMemorySize0
  (JNIEnv *env, jobject mbean)
{
#ifdef _ALLBSD_SOURCE
    jlong result = 0;
    int mib[2];
    size_t rlen;

    mib[0] = CTL_HW;
    mib[1] = HW_MEMSIZE;
    rlen = sizeof(result);
    if (sysctl(mib, 2, &result, &rlen, NULL, 0) != 0) {
        throw_internal_error(env, "sysctl failed");
        return -1;
    }
    return result;
#elif defined(_AIX)
    perfstat_memory_total_t memory_info;
    if (-1 != perfstat_memory_total(NULL, &memory_info, sizeof(perfstat_memory_total_t), 1)) {
        return (jlong)(memory_info.real_total * 4L * 1024L);
    }
    return -1;
#else // solaris / linux
    jlong num_physical_pages = sysconf(_SC_PHYS_PAGES);
    return (num_physical_pages * page_size);
#endif
}



JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getOpenFileDescriptorCount0
  (JNIEnv *env, jobject mbean)
{
#ifdef __APPLE__
    // This code is influenced by the darwin lsof source
    pid_t my_pid;
    struct proc_bsdinfo bsdinfo;
    struct proc_fdinfo *fds;
    int nfiles;
    kern_return_t kres;
    int res;
    size_t fds_size;

    kres = pid_for_task(mach_task_self(), &my_pid);
    if (kres != KERN_SUCCESS) {
        throw_internal_error(env, "pid_for_task failed");
        return -1;
    }

    // get the maximum number of file descriptors
    res = proc_pidinfo(my_pid, PROC_PIDTBSDINFO, 0, &bsdinfo, PROC_PIDTBSDINFO_SIZE);
    if (res <= 0) {
        throw_internal_error(env, "proc_pidinfo with PROC_PIDTBSDINFO failed");
        return -1;
    }

    // allocate memory to hold the fd information (we don't acutally use this information
    // but need it to get the number of open files)
    fds_size = bsdinfo.pbi_nfiles * sizeof(struct proc_fdinfo);
    fds = malloc(fds_size);
    if (fds == NULL) {
        JNU_ThrowOutOfMemoryError(env, "could not allocate space for file descriptors");
        return -1;
    }

    // get the list of open files - the return value is the number of bytes
    // proc_pidinfo filled in
    res = proc_pidinfo(my_pid, PROC_PIDLISTFDS, 0, fds, fds_size);
    if (res <= 0) {
        free(fds);
        throw_internal_error(env, "proc_pidinfo failed for PROC_PIDLISTFDS");
        return -1;
    }
    nfiles = res / sizeof(struct proc_fdinfo);
    free(fds);

    return nfiles;
#elif defined(_ALLBSD_SOURCE)
    /*
     * XXXBSD: there's no way available to do it in FreeBSD, AFAIK.
     */
    // throw_internal_error(env, "Unimplemented in FreeBSD");
    return (100);
#else /* solaris/linux */
    DIR *dirp;
    struct dirent* dentp;
    jlong fds = 0;

#if defined(_AIX)
/* AIX does not understand '/proc/self' - it requires the real process ID */
#define FD_DIR aix_fd_dir
    char aix_fd_dir[32];     /* the pid has at most 19 digits */
    snprintf(aix_fd_dir, 32, "/proc/%d/fd", getpid());
#else
#define FD_DIR "/proc/self/fd"
#endif

    dirp = opendir(FD_DIR);
    if (dirp == NULL) {
        throw_internal_error(env, "Unable to open directory /proc/self/fd");
        return -1;
    }

    // iterate through directory entries, skipping '.' and '..'
    // each entry represents an open file descriptor.
    while ((dentp = readdir(dirp)) != NULL) {
        if (isdigit(dentp->d_name[0])) {
            fds++;
        }
    }

    closedir(dirp);
    // subtract by 1 which was the fd open for this implementation
    return (fds - 1);
#endif
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getMaxFileDescriptorCount0
  (JNIEnv *env, jobject mbean)
{
    struct rlimit rlp;

    if (getrlimit(RLIMIT_NOFILE, &rlp) == -1) {
        throw_internal_error(env, "getrlimit failed");
        return -1;
    }
    return (jlong) rlp.rlim_cur;
}
