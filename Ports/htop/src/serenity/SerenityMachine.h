/*
htop - serenity/SerenityMachine.h
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#ifndef HEADER_SerenityMachine
#define HEADER_SerenityMachine

#include "Machine.h"

/* Per-CPU data (aggregate at index 0, per-core at 1..n) */
typedef struct CPUData_ {
    double userPercent;
    double kernelPercent;
    double idlePercent;
    double systemAllPercent;
} CPUData;

typedef struct SerenityMachine_ {
    Machine super;

    /* Memory (moved from Machine base in 3.5.0) */
    memory_t usedMem;
    memory_t cachedMem;

    /* CPU data array: index 0 = aggregate, 1..activeCPUs = per-core */
    CPUData* cpus;

    /* Scheduler totals from /sys/kernel/stats */
    unsigned long long prevTotalTime;
    unsigned long long prevKernelTime;
    unsigned long long prevUserTime;
    unsigned long long prevIdleTime;
    unsigned long long curTotalTime;
    unsigned long long curKernelTime;
    unsigned long long curUserTime;
    unsigned long long curIdleTime;

    /* Precomputed delta (curTotal - prevTotal) available to ProcessTable */
    unsigned long long totalTimeDelta;

    /* JSON buffer shared with ProcessTable: populated during Machine_scan,
     * consumed during ProcessTable_goThroughEntries */
    char* procBuf;
    size_t procBufCapacity;
    size_t procBufLen;
} SerenityMachine;

#endif /* HEADER_SerenityMachine */
