/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdint.h>
#include <stdarg.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/resource.h>
#include <sys/types.h>
#include <dirent.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <pthread.h>
#include <inttypes.h>
#include "com_sun_management_internal_OperatingSystemImpl.h"

#include <assert.h>

struct ticks {
    uint64_t  used;
    uint64_t  usedKernel;
    uint64_t  total;
};

typedef struct ticks ticks;

typedef enum {
    CPU_LOAD_VM_ONLY,
    CPU_LOAD_GLOBAL,
} CpuLoadTarget;

static struct perfbuf {
    int   nProcs;
    ticks jvmTicks;
    ticks cpuTicks;
    ticks *cpus;
} counters;

#define DEC_64 "%"SCNd64
#define NS_PER_SEC 1000000000

static int next_line(FILE *f) {
    int c;
    do {
        c = fgetc(f);
    } while (c != '\n' && c != EOF);

    return c;
}

/**
 * Return the total number of ticks since the system was booted.
 * If the usedTicks parameter is not NULL, it will be filled with
 * the number of ticks spent on actual processes (user, system or
 * nice processes) since system boot. Note that this is the total number
 * of "executed" ticks on _all_ CPU:s, that is on a n-way system it is
 * n times the number of ticks that has passed in clock time.
 *
 * Returns a negative value if the reading of the ticks failed.
 */
static int get_totalticks(int which, ticks *pticks) {
    FILE         *fh;
    uint64_t        userTicks, niceTicks, systemTicks, idleTicks;
    uint64_t        iowTicks = 0, irqTicks = 0, sirqTicks= 0;
    int             n;

    if((fh = fopen("/proc/stat", "r")) == NULL) {
        return -1;
    }

    n = fscanf(fh, "cpu " DEC_64 " " DEC_64 " " DEC_64 " " DEC_64 " " DEC_64 " "
                   DEC_64 " " DEC_64,
           &userTicks, &niceTicks, &systemTicks, &idleTicks,
           &iowTicks, &irqTicks, &sirqTicks);

    // Move to next line
    if (next_line(fh) == EOF) {
        fclose(fh);
        return -2;
    }

    //find the line for requested cpu faster to just iterate linefeeds?
    if (which != -1) {
        int i;
        for (i = 0; i < which; i++) {
            if (fscanf(fh, "cpu%*d " DEC_64 " " DEC_64 " " DEC_64 " " DEC_64 " "
                            DEC_64 " " DEC_64 " " DEC_64,
                   &userTicks, &niceTicks, &systemTicks, &idleTicks,
                   &iowTicks, &irqTicks, &sirqTicks) < 4) {
                fclose(fh);
                return -2;
            }
            if (next_line(fh) == EOF) {
                fclose(fh);
                return -2;
            }
        }
        n = fscanf(fh, "cpu%*d " DEC_64 " " DEC_64 " " DEC_64 " " DEC_64 " "
                       DEC_64 " " DEC_64 " " DEC_64 "\n",
           &userTicks, &niceTicks, &systemTicks, &idleTicks,
           &iowTicks, &irqTicks, &sirqTicks);
    }

    fclose(fh);
    if (n < 4) {
        return -2;
    }

    pticks->used       = userTicks + niceTicks;
    pticks->usedKernel = systemTicks + irqTicks + sirqTicks;
    pticks->total      = userTicks + niceTicks + systemTicks + idleTicks +
                         iowTicks + irqTicks + sirqTicks;

    return 0;
}

static int vread_statdata(const char *procfile, const char *fmt, va_list args) {
    FILE    *f;
    int     n;
    char     buf[2048];

    if ((f = fopen(procfile, "r")) == NULL) {
        return -1;
    }

    if ((n = fread(buf, 1, sizeof(buf), f)) != -1) {
    char *tmp;

    buf[n-1] = '\0';
    /** skip through pid and exec name. the exec name _could be wacky_ (renamed) and
     *  make scanf go mupp.
     */
    if ((tmp = strrchr(buf, ')')) != NULL) {
        // skip the ')' and the following space but check that the buffer is long enough
        tmp += 2;
        if (tmp < buf + n) {
        n = vsscanf(tmp, fmt, args);
        }
    }
    }

    fclose(f);

    return n;
}

static int read_statdata(const char *procfile, const char *fmt, ...) {
    int       n;
    va_list args;

    va_start(args, fmt);
    n = vread_statdata(procfile, fmt, args);
    va_end(args);
    return n;
}

/** read user and system ticks from a named procfile, assumed to be in 'stat' format then. */
static int read_ticks(const char *procfile, uint64_t *userTicks, uint64_t *systemTicks) {
    return read_statdata(procfile, "%*c %*d %*d %*d %*d %*d %*u %*u %*u %*u %*u "DEC_64" "DEC_64,
             userTicks, systemTicks
             );
}

/**
 * Return the number of ticks spent in any of the processes belonging
 * to the JVM on any CPU.
 */
static int get_jvmticks(ticks *pticks) {
    uint64_t userTicks;
    uint64_t systemTicks;

    if (read_ticks("/proc/self/stat", &userTicks, &systemTicks) < 0) {
        return -1;
    }

    // get the total
    if (get_totalticks(-1, pticks) < 0) {
        return -1;
    }

    pticks->used       = userTicks;
    pticks->usedKernel = systemTicks;

    return 0;
}

/**
 * This method must be called first, before any data can be gathererd.
 */
int perfInit() {
    static int initialized = 0;

    if (!initialized) {
        int  i;
        // We need to allocate counters for all CPUs, including ones that
        // are currently offline as they could be turned online later.
        int n = sysconf(_SC_NPROCESSORS_CONF);
        if (n <= 0) {
            n = 1;
        }

        counters.cpus = calloc(n,sizeof(ticks));
        counters.nProcs = n;
        if (counters.cpus != NULL)  {
            // For the CPU load
            get_totalticks(-1, &counters.cpuTicks);

            for (i = 0; i < n; i++) {
                get_totalticks(i, &counters.cpus[i]);
            }
            // For JVM load
            get_jvmticks(&counters.jvmTicks);
            initialized = 1;
        }
    }

    return initialized ? 0 : -1;
}

#define MAX(a,b) (a>b?a:b)
#define MIN(a,b) (a<b?a:b)

static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;

/**
 * Return the load of the CPU as a double. 1.0 means the CPU process uses all
 * available time for user or system processes, 0.0 means the CPU uses all time
 * being idle.
 *
 * Returns a negative value if there is a problem in determining the CPU load.
 */

static double get_cpuload_internal(int which, double *pkernelLoad, CpuLoadTarget target) {
    uint64_t udiff, kdiff, tdiff;
    ticks *pticks, tmp;
    double user_load = -1.0;
    int failed = 0;

    *pkernelLoad = 0.0;

    pthread_mutex_lock(&lock);

    if (perfInit() == 0) {

        if (target == CPU_LOAD_VM_ONLY) {
            pticks = &counters.jvmTicks;
        } else if (which == -1) {
            pticks = &counters.cpuTicks;
        } else {
            pticks = &counters.cpus[which];
        }

        tmp = *pticks;

        if (target == CPU_LOAD_VM_ONLY) {
            if (get_jvmticks(pticks) != 0) {
                failed = 1;
            }
        } else if (get_totalticks(which, pticks) < 0) {
            failed = 1;
        }

        if (!failed) {

            assert(pticks->usedKernel >= tmp.usedKernel);
            kdiff = pticks->usedKernel - tmp.usedKernel;
            tdiff = pticks->total - tmp.total;
            udiff = pticks->used - tmp.used;

            if (tdiff == 0) {
                user_load = 0;
            } else {
                if (tdiff < (udiff + kdiff)) {
                    tdiff = udiff + kdiff;
                }
                *pkernelLoad = (kdiff / (double)tdiff);
                // BUG9044876, normalize return values to sane values
                *pkernelLoad = MAX(*pkernelLoad, 0.0);
                *pkernelLoad = MIN(*pkernelLoad, 1.0);

                user_load = (udiff / (double)tdiff);
                user_load = MAX(user_load, 0.0);
                user_load = MIN(user_load, 1.0);
            }
        }
    }
    pthread_mutex_unlock(&lock);
    return user_load;
}

double get_cpu_load(int which) {
    double u, s;
    u = get_cpuload_internal(which, &s, CPU_LOAD_GLOBAL);
    if (u < 0) {
        return -1.0;
    }
    // Cap total systemload to 1.0
    return MIN((u + s), 1.0);
}

double get_process_load() {
    double u, s;
    u = get_cpuload_internal(-1, &s, CPU_LOAD_VM_ONLY);
    if (u < 0) {
        return -1.0;
    }
    return u + s;
}

JNIEXPORT jdouble JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getCpuLoad0
(JNIEnv *env, jobject dummy)
{
    if (perfInit() == 0) {
        return get_cpu_load(-1);
    } else {
        return -1.0;
    }
}

JNIEXPORT jdouble JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getProcessCpuLoad0
(JNIEnv *env, jobject dummy)
{
    if (perfInit() == 0) {
        return get_process_load();
    } else {
        return -1.0;
    }
}

JNIEXPORT jdouble JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getSingleCpuLoad0
(JNIEnv *env, jobject mbean, jint cpu_number)
{
    if (perfInit() == 0 && cpu_number >= 0 && cpu_number < counters.nProcs) {
        return get_cpu_load(cpu_number);
    } else {
        return -1.0;
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getHostConfiguredCpuCount0
(JNIEnv *env, jobject mbean)
{
    if (perfInit() == 0) {
        return counters.nProcs;
    } else {
       return -1;
    }
}

// Return the host cpu ticks since boot in nanoseconds
JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getHostTotalCpuTicks0
(JNIEnv *env, jobject mbean)
{
    if (perfInit() == 0) {
        if (get_totalticks(-1, &counters.cpuTicks) < 0) {
            return -1;
        } else {
            long ticks_per_sec = sysconf(_SC_CLK_TCK);
            jlong result = (jlong)counters.cpuTicks.total;
            if (ticks_per_sec <= NS_PER_SEC) {
                long scale_factor = NS_PER_SEC/ticks_per_sec;
                result = result * scale_factor;
            } else {
                long scale_factor = ticks_per_sec/NS_PER_SEC;
                result = result / scale_factor;
            }
            return result;
        }
    } else {
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getHostOnlineCpuCount0
(JNIEnv *env, jobject mbean)
{
    int n = sysconf(_SC_NPROCESSORS_ONLN);
    if (n <= 0) {
        n = 1;
    }
    return n;
}

