/*
htop - serenity/SerenityMachine.c
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#include "config.h" // IWYU pragma: keep

#include "serenity/SerenityMachine.h"

#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>

#include "Macros.h"
#include "XUtils.h"
#include "serenity/SerenityJSON.h"


/* Minimum buffer size for /sys/kernel/processes JSON */
#define PROC_BUF_INITIAL_SIZE (64 * 1024)   /* 64 KiB */
#define PROC_JSON_PATH         "/sys/kernel/processes"

/*
 * Read the entire content of PROC_JSON_PATH into host->procBuf,
 * growing the buffer as needed.  Returns 0 on success.
 */
static int SerenityMachine_readProcBuf(SerenityMachine* host) {
   int fd = open(PROC_JSON_PATH, O_RDONLY);
   if (fd < 0)
      return -1;

   if (!host->procBuf || host->procBufCapacity < PROC_BUF_INITIAL_SIZE) {
      free(host->procBuf);
      host->procBuf = xMalloc(PROC_BUF_INITIAL_SIZE);
      host->procBufCapacity = PROC_BUF_INITIAL_SIZE;
   }

   size_t total = 0;
   for (;;) {
      ssize_t n = read(fd, host->procBuf + total, host->procBufCapacity - total - 1);
      if (n < 0) {
         close(fd);
         return -1;
      }
      if (n == 0)
         break;
      total += (size_t)n;
      if (total + 1 >= host->procBufCapacity) {
         size_t newCap = host->procBufCapacity * 2;
         char* newBuf = xRealloc(host->procBuf, newCap);
         host->procBuf = newBuf;
         host->procBufCapacity = newCap;
      }
   }
   close(fd);

   host->procBuf[total] = '\0';
   host->procBufLen = total;
   return 0;
}

/*
 * Update CPU percentages from /sys/kernel/stats timing deltas.
 */
static void SerenityMachine_updateCPUData(SerenityMachine* host) {
   unsigned long long totalDelta  = (host->curTotalTime  > host->prevTotalTime)
                                    ? host->curTotalTime  - host->prevTotalTime : 0;
   unsigned long long userDelta   = (host->curUserTime   > host->prevUserTime)
                                    ? host->curUserTime   - host->prevUserTime : 0;
   unsigned long long idleDelta   = (host->curIdleTime   > host->prevIdleTime)
                                    ? host->curIdleTime   - host->prevIdleTime : 0;
   unsigned long long kernelDelta = (host->curKernelTime > host->prevKernelTime)
                                    ? host->curKernelTime - host->prevKernelTime : 0;

   host->totalTimeDelta = totalDelta;

   CPUData* cpu = &host->cpus[0];

   if (totalDelta > 0) {
      cpu->userPercent   = 100.0 * (double)userDelta / (double)totalDelta;
      /* kernel_time includes idle; subtract idle to get busy kernel */
      unsigned long long busyKernel = (kernelDelta > idleDelta) ? kernelDelta - idleDelta : 0;
      cpu->kernelPercent = 100.0 * (double)busyKernel / (double)totalDelta;
      cpu->idlePercent   = 100.0 * (double)idleDelta / (double)totalDelta;

      cpu->userPercent   = CLAMP(cpu->userPercent,   0.0, 100.0);
      cpu->kernelPercent = CLAMP(cpu->kernelPercent, 0.0, 100.0);
      cpu->idlePercent   = CLAMP(cpu->idlePercent,   0.0, 100.0);
   } else {
      cpu->userPercent   = 0.0;
      cpu->kernelPercent = 0.0;
      cpu->idlePercent   = 100.0;
   }
   cpu->systemAllPercent = cpu->kernelPercent + cpu->userPercent;

   /* Replicate aggregate data to per-core slots (no per-CPU data available) */
   const Machine* super = &host->super;
   for (unsigned int i = 1; i <= super->existingCPUs; i++) {
      host->cpus[i] = *cpu;
   }

   host->prevTotalTime  = host->curTotalTime;
   host->prevKernelTime = host->curKernelTime;
   host->prevUserTime   = host->curUserTime;
   host->prevIdleTime   = host->curIdleTime;
}

/* ---------- htop Machine interface ---------- */

Machine* Machine_new(UsersTable* usersTable, uid_t userId) {
   SerenityMachine* this = xCalloc(1, sizeof(SerenityMachine));
   Machine* super = &this->super;

   Machine_init(super, usersTable, userId);

   /* Detect CPU count via POSIX sysconf */
   long cpus = sysconf(_SC_NPROCESSORS_CONF);
   if (cpus < 1) cpus = 1;

   super->existingCPUs = (unsigned int)cpus;
   super->activeCPUs   = (unsigned int)cpus;

   /* Allocate CPUData: index 0 = aggregate, 1..cpus = per-core */
   this->cpus = xCalloc((size_t)cpus + 1, sizeof(CPUData));

   /* Initial total memory from sysconf */
   long pageSize   = sysconf(_SC_PAGESIZE);
   long totalPages = sysconf(_SC_PHYS_PAGES);
   if (pageSize > 0 && totalPages > 0)
      super->totalMem = (memory_t)totalPages * (memory_t)pageSize / 1024ULL;

   return super;
}

void Machine_delete(Machine* cast) {
   SerenityMachine* this = (SerenityMachine*) cast;
   free(this->cpus);
   free(this->procBuf);
   Machine_done(cast);
   free(this);
}

bool Machine_isCPUonline(const Machine* this, unsigned int id) {
   return id < this->existingCPUs;
}

int Machine_getCPUPhysicalCoreID(const Machine* this, unsigned int id) {
   (void)this;
   return (int)id;
}

int Machine_getCPUThreadIndex(const Machine* this, unsigned int id) {
   (void)this;
   (void)id;
   return 0;
}

void Machine_scan(Machine* cast) {
   SerenityMachine* this = (SerenityMachine*) cast;

   /* Read /sys/kernel/processes into procBuf (shared with ProcessTable) */
   if (SerenityMachine_readProcBuf(this) < 0) {
      this->procBufLen = 0;
   }

   /* Read /sys/kernel/stats for CPU timing */
   SerenitySystemStats stats;
   if (serenity_read_system_stats(&stats) == 0) {
      this->curTotalTime  = stats.total_time;
      this->curKernelTime = stats.kernel_time;
      this->curUserTime   = stats.user_time;
      this->curIdleTime   = stats.idle_time;
   }

   /* Read /sys/kernel/memstat for memory data */
   SerenityMemoryStats mem;
   if (serenity_read_memory_status(&mem) == 0) {
      long pageSize = sysconf(_SC_PAGESIZE);
      if (pageSize <= 0) pageSize = 4096;

      unsigned long long totalPages = mem.physical_allocated_pages + mem.physical_available_pages;
      cast->totalMem = (memory_t)(totalPages * (unsigned long long)pageSize / 1024ULL);
      this->usedMem  = (memory_t)(mem.physical_allocated_pages * (unsigned long long)pageSize / 1024ULL);
      this->cachedMem = 0;
   }

   /* Update CPU percentages from the timing deltas */
   SerenityMachine_updateCPUData(this);
}
