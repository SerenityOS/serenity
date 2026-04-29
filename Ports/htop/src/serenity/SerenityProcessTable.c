/*
htop - serenity/SerenityProcessTable.c
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#include "config.h" // IWYU pragma: keep

#include "serenity/SerenityProcessTable.h"

#include <stdlib.h>
#include <string.h>

#include "Macros.h"
#include "Object.h"
#include "Process.h"
#include "ProcessTable.h"
#include "Settings.h"
#include "XUtils.h"
#include "serenity/SerenityJSON.h"
#include "serenity/SerenityMachine.h"
#include "serenity/SerenityProcess.h"


/* Map a SerenityOS thread state string to htop ProcessState enum.
 * Covers core thread states and all blocker-derived state strings. */
static ProcessState parseState(const char* s) {
   if (strcmp(s, "Running")       == 0) return RUNNING;
   if (strcmp(s, "Runnable")      == 0) return RUNNABLE;
   if (strcmp(s, "Dying")         == 0) return ZOMBIE;
   if (strcmp(s, "Dead")          == 0) return ZOMBIE;
   if (strcmp(s, "Stopped")       == 0) return STOPPED;

   if (strcmp(s, "Blocked")       == 0) return BLOCKED;
   if (strcmp(s, "Mutex")         == 0) return BLOCKED;
   if (strcmp(s, "Futex")         == 0) return BLOCKED;
   if (strcmp(s, "Queue")         == 0) return QUEUED;
   if (strcmp(s, "Joining")       == 0) return BLOCKED;
   if (strcmp(s, "Locking File")  == 0) return BLOCKED;

   if (strcmp(s, "Reading")       == 0) return WAITING;
   if (strcmp(s, "Writing")       == 0) return WAITING;
   if (strcmp(s, "Accepting")     == 0) return WAITING;
   if (strcmp(s, "Connecting")    == 0) return WAITING;
   if (strcmp(s, "Selecting")     == 0) return WAITING;
   if (strcmp(s, "Routing (ARP)") == 0) return WAITING;

   if (strcmp(s, "Sleeping")      == 0) return SLEEPING;
   if (strcmp(s, "Waiting")       == 0) return WAITING;
   if (strcmp(s, "Pending Signal") == 0) return WAITING;

   return UNKNOWN;
}

/* Context passed into the process callback */
typedef struct {
   ProcessTable*          super;
   const SerenityMachine* shost;
   const Settings*        settings;
   unsigned long long     totalDelta;
   unsigned int           cpuCount;
   bool                   hideKernelThreads;
   bool                   hideUserlandThreads;
} ScanContext;

/* Called once for each process from serenity_parse_processes() */
static void processCallback(const SerenityProcEntry* entry, void* userdata) {
   ScanContext* ctx = (ScanContext*)userdata;
   ProcessTable* super = ctx->super;
   const Machine* host = super->super.host;

   bool preExisting = false;
   Process* proc = ProcessTable_getProcess(super, entry->pid, &preExisting, SerenityProcess_new);
   SerenityProcess* sp = (SerenityProcess*)proc;

   if (!preExisting) {
      Process_setPid(proc, entry->pid);
      Process_setThreadGroup(proc, entry->pid);
      Process_setParent(proc, entry->ppid);
      proc->isKernelThread   = entry->kernel;
      proc->isUserlandThread = false;
      proc->pgrp             = entry->pgid;
      proc->session          = entry->sid;
      proc->tpgid            = entry->tpgid;
      proc->st_uid           = entry->uid;

      time_t creation_sec = (time_t)(entry->creation_time_ns / 1000000000ULL);
      proc->starttime_ctime = creation_sec > 0 ? creation_sec : (time_t)(host->realtimeMs / 1000);
      Process_fillStarttimeBuffer(proc);
      proc->user = UsersTable_getRef(host->usersTable, proc->st_uid);
      ProcessTable_add(super, proc);

      if (entry->executable[0])
         Process_updateExe(proc, entry->executable);
      if (entry->name[0]) {
         Process_updateComm(proc, entry->name);
         Process_updateCmdline(proc, entry->name, 0, (int)strlen(entry->name));
      }

      if (entry->tty[0])
         free_and_xStrdup(&proc->tty_name, entry->tty);
   } else {
      Process_setParent(proc, entry->ppid);
      proc->pgrp    = entry->pgid;
      proc->session = entry->sid;
      proc->tpgid   = entry->tpgid;
      if (proc->st_uid != entry->uid) {
         proc->st_uid = entry->uid;
         proc->user = UsersTable_getRef(host->usersTable, proc->st_uid);
      }
      if (entry->executable[0])
         Process_updateExe(proc, entry->executable);
      if (ctx->settings->updateProcessNames && entry->name[0]) {
         Process_updateComm(proc, entry->name);
         Process_updateCmdline(proc, entry->name, 0, (int)strlen(entry->name));
      }
      if (entry->tty[0])
         free_and_xStrdup(&proc->tty_name, entry->tty);
      else {
         free(proc->tty_name);
         proc->tty_name = NULL;
      }
   }

   /* Always-updated fields */
   proc->m_virt     = (long)(entry->amount_virtual  / 1024ULL);
   proc->m_resident = (long)(entry->amount_resident / 1024ULL);
   proc->nlwp       = entry->nlwp;

   ProcessState state = parseState(entry->state);
   proc->state = state;

   proc->processor = entry->cpu;
   proc->priority  = entry->priority;
   proc->nice      = PROCESS_NICE_UNKNOWN;

   /* CPU% from per-process time delta vs total scheduler delta.
    * totalDelta covers all CPUs, so multiply by cpuCount to get
    * per-core percentages (a process using one full core = 100%).
    * Skip first sample for new processes to avoid counting lifetime CPU. */
   unsigned long long threadTime = entry->time_user + entry->time_kernel;
   if (!preExisting) {
      sp->prevThreadTime = threadTime;
      proc->percent_cpu = 0.0f;
   } else {
      unsigned long long threadDelta = (threadTime >= sp->prevThreadTime)
                                        ? threadTime - sp->prevThreadTime : 0;
      sp->prevThreadTime = threadTime;
      if (ctx->totalDelta > 0) {
         proc->percent_cpu = 100.0f * (float)threadDelta * ctx->cpuCount / (float)ctx->totalDelta;
         if (proc->percent_cpu < 0.0f)
            proc->percent_cpu = 0.0f;
      } else {
         proc->percent_cpu = 0.0f;
      }
   }

   if (host->totalMem > 0)
      proc->percent_mem = 100.0f * (float)proc->m_resident / (float)host->totalMem;
   else
      proc->percent_mem = 0.0f;

   Process_updateCPUFieldWidths(proc->percent_cpu);

   /* CPU time in centiseconds (thread time is in nanoseconds) */
   proc->time = threadTime / 10000000ULL;

   /* SerenityOS-specific fields */
   if (entry->pledge[0])
      free_and_xStrdup(&sp->pledge, entry->pledge);
   else {
      free(sp->pledge);
      sp->pledge = NULL;
   }
   if (entry->veil[0])
      free_and_xStrdup(&sp->veil, entry->veil);
   else {
      free(sp->veil);
      sp->veil = NULL;
   }
   sp->inodeFaults  = entry->inode_faults;
   sp->zeroFaults   = entry->zero_faults;
   sp->cowFaults    = entry->cow_faults;
   sp->isKernelProc = entry->kernel;

   /* Visibility */
   proc->super.show = !((ctx->hideKernelThreads  && Process_isKernelThread(proc)) ||
                         (ctx->hideUserlandThreads && Process_isUserlandThread(proc)));

   super->totalTasks++;
   if (proc->state == RUNNING)
      super->runningTasks++;
   if (Process_isKernelThread(proc))
      super->kernelThreads++;

   proc->super.updated = true;
}

/* ---------- htop ProcessTable interface ---------- */

ProcessTable* ProcessTable_new(Machine* host, Hashtable* pidMatchList) {
   SerenityProcessTable* this = xCalloc(1, sizeof(SerenityProcessTable));
   Object_setClass(this, Class(ProcessTable));

   ProcessTable* super = &this->super;
   ProcessTable_init(super, Class(SerenityProcess), host, pidMatchList);

   return super;
}

void ProcessTable_delete(Object* cast) {
   SerenityProcessTable* this = (SerenityProcessTable*)cast;
   ProcessTable_done(&this->super);
   free(this);
}

void ProcessTable_goThroughEntries(ProcessTable* super) {
   const Machine*         host  = super->super.host;
   const SerenityMachine* shost = (const SerenityMachine*)host;

   if (!shost->procBuf || shost->procBufLen == 0)
      return;

   ScanContext ctx = {
      .super               = super,
      .shost               = shost,
      .settings            = host->settings,
      .totalDelta          = shost->totalTimeDelta,
      .cpuCount            = host->existingCPUs,
      .hideKernelThreads   = host->settings->hideKernelThreads,
      .hideUserlandThreads = host->settings->hideUserlandThreads,
   };

   serenity_parse_processes(shost->procBuf, shost->procBufLen, processCallback, &ctx);
}
