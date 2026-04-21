/*
htop - serenity/SerenityProcess.h
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#ifndef HEADER_SerenityProcess
#define HEADER_SerenityProcess

#include "Machine.h"
#include "Object.h"
#include "Process.h"

typedef struct SerenityProcess_ {
    Process super;

    /* SerenityOS-specific security strings */
    char* pledge; /* pledge capabilities (may be NULL) */
    char* veil;   /* veil paths (may be NULL) */

    /* Page-fault counters (per-thread, summed for the process) */
    unsigned long long inodeFaults;
    unsigned long long zeroFaults;
    unsigned long long cowFaults;

    /* Whether this is a kernel process (from JSON "kernel": true) */
    bool isKernelProc;

    /* Cumulative thread time from the previous scan (nanoseconds),
     * used to compute the per-process CPU% delta */
    unsigned long long prevThreadTime;
} SerenityProcess;

extern ProcessClass const SerenityProcess_class;

extern ProcessFieldData const Process_fields[LAST_PROCESSFIELD];

Process* SerenityProcess_new(Machine const* machine);

void Process_delete(Object* cast);

#endif /* HEADER_SerenityProcess */
