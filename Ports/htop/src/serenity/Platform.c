/*
htop - serenity/Platform.c
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#include "config.h" // IWYU pragma: keep

#include "serenity/Platform.h"

#include <math.h>
#include <time.h>
#include <unistd.h>
#include <sys/resource.h>
#include <sys/utsname.h>

#include "CPUMeter.h"
#include "ClockMeter.h"
#include "DateTimeMeter.h"
#include "DiskIOMeter.h"
#include "FileDescriptorMeter.h"
#include "HostnameMeter.h"
#include "LoadAverageMeter.h"
#include "Machine.h"
#include "Macros.h"
#include "MemoryMeter.h"
#include "MemorySwapMeter.h"
#include "Meter.h"
#include "NetworkIOMeter.h"
#include "Settings.h"
#include "SwapMeter.h"
#include "SysArchMeter.h"
#include "TasksMeter.h"
#include "UptimeMeter.h"
#include "XUtils.h"
#include "serenity/SerenityMachine.h"


const ScreenDefaults Platform_defaultScreens[] = {
   {
      .name    = "Main",
      .columns = "PID USER PRIORITY M_VIRT M_RESIDENT STATE PERCENT_CPU PERCENT_MEM TIME Command",
      .sortKey = "PERCENT_CPU",
   },
};

const unsigned int Platform_numberOfDefaultScreens = ARRAYSIZE(Platform_defaultScreens);

const SignalItem Platform_signals[] = {
   { .name = " 0 Cancel",   .number =  0 },
   { .name = " 1 SIGHUP",   .number =  1 },
   { .name = " 2 SIGINT",   .number =  2 },
   { .name = " 3 SIGQUIT",  .number =  3 },
   { .name = " 4 SIGILL",   .number =  4 },
   { .name = " 5 SIGTRAP",  .number =  5 },
   { .name = " 6 SIGABRT",  .number =  6 },
   { .name = " 7 SIGBUS",   .number =  7 },
   { .name = " 8 SIGFPE",   .number =  8 },
   { .name = " 9 SIGKILL",  .number =  9 },
   { .name = "10 SIGUSR1",  .number = 10 },
   { .name = "11 SIGSEGV",  .number = 11 },
   { .name = "12 SIGUSR2",  .number = 12 },
   { .name = "13 SIGPIPE",  .number = 13 },
   { .name = "14 SIGALRM",  .number = 14 },
   { .name = "15 SIGTERM",  .number = 15 },
   { .name = "17 SIGCHLD",  .number = 17 },
   { .name = "18 SIGCONT",  .number = 18 },
   { .name = "19 SIGSTOP",  .number = 19 },
   { .name = "20 SIGTSTP",  .number = 20 },
   { .name = "21 SIGTTIN",  .number = 21 },
   { .name = "22 SIGTTOU",  .number = 22 },
   { .name = "23 SIGWINCH", .number = 23 },
};

const unsigned int Platform_numberOfSignals = ARRAYSIZE(Platform_signals);

enum {
   MEMORY_CLASS_USED = 0,
   MEMORY_CLASS_CACHED,
};

const MemoryClass Platform_memoryClasses[] = {
   [MEMORY_CLASS_USED]   = { .label = "used",   .countsAsUsed = true,  .countsAsCache = false, .color = MEMORY_1 },
   [MEMORY_CLASS_CACHED] = { .label = "cached", .countsAsUsed = false, .countsAsCache = true,  .color = MEMORY_2 },
};

const unsigned int Platform_numberOfMemoryClasses = ARRAYSIZE(Platform_memoryClasses);

const MeterClass* const Platform_meterTypes[] = {
   &CPUMeter_class,
   &ClockMeter_class,
   &DateMeter_class,
   &DateTimeMeter_class,
   &LoadAverageMeter_class,
   &LoadMeter_class,
   &MemoryMeter_class,
   &SwapMeter_class,
   &MemorySwapMeter_class,
   &TasksMeter_class,
   &BatteryMeter_class,
   &UptimeMeter_class,
   &SecondsUptimeMeter_class,
   &HostnameMeter_class,
   &SysArchMeter_class,
   &AllCPUsMeter_class,
   &AllCPUs2Meter_class,
   &AllCPUs4Meter_class,
   &AllCPUs8Meter_class,
   &LeftCPUsMeter_class,
   &RightCPUsMeter_class,
   &LeftCPUs2Meter_class,
   &RightCPUs2Meter_class,
   &LeftCPUs4Meter_class,
   &RightCPUs4Meter_class,
   &LeftCPUs8Meter_class,
   &RightCPUs8Meter_class,
   &FileDescriptorMeter_class,
   &DiskIOMeter_class,
   &NetworkIOMeter_class,
   &BlankMeter_class,
   NULL
};

bool Platform_init(void) {
   return true;
}

void Platform_done(void) { }

void Platform_setBindings(Htop_Action* keys) {
   (void)keys;
}

int Platform_getUptime(void) {
   struct timespec ts;
   if (clock_gettime(CLOCK_MONOTONIC, &ts) == 0)
      return (int)ts.tv_sec;
   return 0;
}

void Platform_getLoadAverage(double* one, double* five, double* fifteen) {
   *one = 0.0;
   *five = 0.0;
   *fifteen = 0.0;
}

pid_t Platform_getMaxPid(void) {
   return 32768;
}

double Platform_setCPUValues(Meter* this, unsigned int cpu) {
   const Machine*         host  = this->host;
   const SerenityMachine* shost = (const SerenityMachine*) host;

   unsigned int idx = (cpu < host->existingCPUs + 1) ? cpu : 0;
   const CPUData* d = &shost->cpus[idx];

   double* v = this->values;
   v[CPU_METER_NICE]   = 0.0;
   v[CPU_METER_NORMAL] = d->userPercent;
   v[CPU_METER_KERNEL] = d->kernelPercent;
   this->curItems = 3;

   double percent = d->userPercent + d->kernelPercent;
   percent = CLAMP(percent, 0.0, 100.0);

   v[CPU_METER_FREQUENCY]   = NAN;
   v[CPU_METER_TEMPERATURE] = NAN;

   return percent;
}

void Platform_setMemoryValues(Meter* this) {
   const Machine* host = this->host;
   const SerenityMachine* shost = (const SerenityMachine*) host;

   this->total = host->totalMem;

   double* v = this->values;
   v[MEMORY_CLASS_USED]   = shost->usedMem;
   v[MEMORY_CLASS_CACHED] = shost->cachedMem;
   this->curItems = 2;
}

void Platform_setSwapValues(Meter* this) {
   this->total = 0;
}

char* Platform_getProcessEnv(pid_t pid) {
   (void)pid;
   return NULL;
}

FileLocks_ProcessData* Platform_getProcessLocks(pid_t pid) {
   (void)pid;
   return NULL;
}

void Platform_getFileDescriptors(double* used, double* max) {
   struct rlimit rl;
   if (getrlimit(RLIMIT_NOFILE, &rl) == 0) {
      *max  = (double)rl.rlim_cur;
      *used = NAN;
   } else {
      *used = NAN;
      *max  = NAN;
   }
}

bool Platform_getDiskIO(DiskIOData* data) {
   (void)data;
   return false;
}

bool Platform_getNetworkIO(NetworkIOData* data) {
   (void)data;
   return false;
}

void Platform_getBattery(double* percent, ACPresence* isOnAC) {
   *percent = NAN;
   *isOnAC  = AC_ERROR;
}

void Platform_getHostname(char* buffer, size_t size) {
   if (gethostname(buffer, size) < 0)
      String_safeStrncpy(buffer, "serenity", size);
}

const char* Platform_getRelease(void) {
   static struct utsname uname_info;
   if (uname(&uname_info) == 0)
      return uname_info.release;
   return "SerenityOS";
}
