/*
htop - serenity/SerenityProcess.c
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#include "config.h" // IWYU pragma: keep

#include "serenity/SerenityProcess.h"

#include <stdlib.h>

#include "CRT.h"
#include "Macros.h"
#include "Process.h"
#include "RichString.h"
#include "XUtils.h"
#include "serenity/ProcessField.h"


const ProcessFieldData Process_fields[LAST_PROCESSFIELD] = {
   [0]                    = { .name = "",              .title = NULL,               .description = NULL,                    .flags = 0, },
   [PID]                  = { .name = "PID",           .title = "PID",              .description = "Process/thread ID",     .flags = 0, .pidColumn = true, },
   [COMM]                 = { .name = "Command",       .title = "Command ",         .description = "Command line",          .flags = 0, },
   [STATE]                = { .name = "STATE",         .title = "S ",               .description = "Process state",         .flags = 0, },
   [PPID]                 = { .name = "PPID",          .title = "PPID",             .description = "Parent process ID",     .flags = 0, .pidColumn = true, },
   [PGRP]                 = { .name = "PGRP",          .title = "PGRP",             .description = "Process group ID",      .flags = 0, .pidColumn = true, },
   [SESSION]              = { .name = "SESSION",       .title = "SID",              .description = "Session ID",            .flags = 0, .pidColumn = true, },
   [TTY]                  = { .name = "TTY",           .title = "TTY      ",        .description = "Controlling terminal",  .flags = 0, },
   [TPGID]                = { .name = "TPGID",         .title = "TPGID",            .description = "Terminal fg PGRP",      .flags = 0, .pidColumn = true, },
   [MAJFLT]               = { .name = "MAJFLT",        .title = "     MAJFLT ",     .description = "CoW page faults",       .flags = 0, .defaultSortDesc = true, },
   [PRIORITY]             = { .name = "PRIORITY",      .title = "PRI ",             .description = "Scheduling priority",   .flags = 0, },
   [NICE]                 = { .name = "NICE",          .title = " NI ",             .description = "Nice value",            .flags = 0, },
   [STARTTIME]            = { .name = "STARTTIME",     .title = "START ",           .description = "Process start time",    .flags = 0, },
   [ELAPSED]              = { .name = "ELAPSED",       .title = "ELAPSED  ",        .description = "Time since start",      .flags = 0, },
   [PROCESSOR]            = { .name = "PROCESSOR",     .title = "CPU ",             .description = "Last CPU used",         .flags = 0, },
   [M_VIRT]               = { .name = "M_VIRT",        .title = " VIRT ",           .description = "Virtual memory size",   .flags = 0, .defaultSortDesc = true, },
   [M_RESIDENT]           = { .name = "M_RESIDENT",    .title = "  RES ",           .description = "Resident memory size",  .flags = 0, .defaultSortDesc = true, },
   [ST_UID]               = { .name = "ST_UID",        .title = "UID",              .description = "User ID",               .flags = 0, },
   [PERCENT_CPU]          = { .name = "PERCENT_CPU",   .title = " CPU%",            .description = "CPU usage %",           .flags = 0, .defaultSortDesc = true, .autoWidth = true, .autoTitleRightAlign = true, },
   [PERCENT_NORM_CPU]     = { .name = "PERCENT_NORM_CPU", .title = "NCPU%",         .description = "Normalized CPU %",      .flags = 0, .defaultSortDesc = true, .autoWidth = true, },
   [PERCENT_MEM]          = { .name = "PERCENT_MEM",   .title = "MEM% ",            .description = "Memory usage %",        .flags = 0, .defaultSortDesc = true, },
   [USER]                 = { .name = "USER",          .title = "USER       ",      .description = "Process owner",         .flags = 0, },
   [TIME]                 = { .name = "TIME",          .title = "  TIME+  ",        .description = "CPU time used",         .flags = 0, .defaultSortDesc = true, },
   [NLWP]                 = { .name = "NLWP",          .title = "NLWP ",            .description = "Thread count",          .flags = 0, .defaultSortDesc = true, },
   [TGID]                 = { .name = "TGID",          .title = "TGID",             .description = "Thread group ID",       .flags = 0, .pidColumn = true, },
   [PROC_COMM]            = { .name = "COMM",          .title = "COMM            ", .description = "Process comm string",   .flags = 0, },
   [PROC_EXE]             = { .name = "EXE",           .title = "EXE             ", .description = "Executable basename",   .flags = 0, },
   [CWD]                  = { .name = "CWD",           .title = "CWD                       ", .description = "Current working dir", .flags = PROCESS_FLAG_CWD, },
#ifdef SCHEDULER_SUPPORT
   [SCHEDULERPOLICY]      = { .name = "SCHEDULERPOLICY", .title = "SCHED ", .description = "Scheduling policy", .flags = PROCESS_FLAG_SCHEDPOL, },
#endif
   [PLEDGE]               = { .name = "PLEDGE",        .title = "PLEDGE             ", .description = "Pledge capabilities", .flags = 0, },
   [VEIL]                 = { .name = "VEIL",          .title = "VEIL               ", .description = "Veil path restrictions", .flags = 0, },
   [SERENITY_INODE_FAULTS]= { .name = "INODE_FAULTS",  .title = "  IFLT ",          .description = "Inode page faults",    .flags = 0, .defaultSortDesc = true, },
   [SERENITY_ZERO_FAULTS] = { .name = "ZERO_FAULTS",   .title = "  ZFLT ",          .description = "Zero page faults",     .flags = 0, .defaultSortDesc = true, },
   [SERENITY_COW_FAULTS]  = { .name = "COW_FAULTS",    .title = "  CFLT ",          .description = "Copy-on-write faults",  .flags = 0, .defaultSortDesc = true, },
};

Process* SerenityProcess_new(const Machine* machine) {
   SerenityProcess* this = xCalloc(1, sizeof(SerenityProcess));
   Object_setClass(this, Class(SerenityProcess));
   Process_init(&this->super, machine);
   return (Process*)this;
}

void Process_delete(Object* cast) {
   SerenityProcess* this = (SerenityProcess*) cast;
   Process_done((Process*)cast);
   free(this->pledge);
   free(this->veil);
   free(this);
}

static void SerenityProcess_rowWriteField(const Row* super, RichString* str, ProcessField field) {
   const SerenityProcess* sp = (const SerenityProcess*) super;

   char buffer[256];
   int attr = CRT_colors[DEFAULT_COLOR];
   size_t n = sizeof(buffer) - 1;
   buffer[n] = '\0';

   switch (field) {
   case PLEDGE:
      Row_printLeftAlignedField(str, attr, sp->pledge ? sp->pledge : "", 20);
      return;

   case VEIL:
      Row_printLeftAlignedField(str, attr, sp->veil ? sp->veil : "", 20);
      return;

   case SERENITY_INODE_FAULTS:
      xSnprintf(buffer, n, "%7llu ", sp->inodeFaults);
      break;

   case SERENITY_ZERO_FAULTS:
      xSnprintf(buffer, n, "%7llu ", sp->zeroFaults);
      break;

   case SERENITY_COW_FAULTS:
      xSnprintf(buffer, n, "%7llu ", sp->cowFaults);
      break;

   default:
      Process_writeField(&sp->super, str, field);
      return;
   }

   RichString_appendWide(str, attr, buffer);
}

static int SerenityProcess_compareByKey(const Process* v1, const Process* v2, ProcessField key) {
   const SerenityProcess* p1 = (const SerenityProcess*)v1;
   const SerenityProcess* p2 = (const SerenityProcess*)v2;

   switch (key) {
   case PLEDGE:
      return SPACESHIP_NULLSTR(p1->pledge, p2->pledge);
   case VEIL:
      return SPACESHIP_NULLSTR(p1->veil, p2->veil);
   case SERENITY_INODE_FAULTS:
      return SPACESHIP_NUMBER(p1->inodeFaults, p2->inodeFaults);
   case SERENITY_ZERO_FAULTS:
      return SPACESHIP_NUMBER(p1->zeroFaults, p2->zeroFaults);
   case SERENITY_COW_FAULTS:
      return SPACESHIP_NUMBER(p1->cowFaults, p2->cowFaults);
   default:
      return Process_compareByKey_Base(v1, v2, key);
   }
}

const ProcessClass SerenityProcess_class = {
   .super = {
      .super = {
         .extends = Class(Process),
         .display = Row_display,
         .delete  = Process_delete,
         .compare = Process_compare
      },
      .isHighlighted   = Process_rowIsHighlighted,
      .isVisible       = Process_rowIsVisible,
      .matchesFilter   = Process_rowMatchesFilter,
      .compareByParent = Process_compareByParent,
      .sortKeyString   = Process_rowGetSortKey,
      .writeField      = SerenityProcess_rowWriteField
   },
   .compareByKey = SerenityProcess_compareByKey
};
