/*
htop - serenity/SerenityJSON.h
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#ifndef HEADER_SerenityJSON
#define HEADER_SerenityJSON

#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* CPU timing from /sys/kernel/stats */
typedef struct SerenitySystemStats_ {
    unsigned long long total_time;
    unsigned long long kernel_time;
    unsigned long long user_time;
    unsigned long long idle_time;
} SerenitySystemStats;

/* Physical memory from /sys/kernel/memstat */
typedef struct SerenityMemoryStats_ {
    unsigned long long physical_allocated_pages;
    unsigned long long physical_available_pages;
} SerenityMemoryStats;

/* Per-process info (threads already aggregated) */
typedef struct SerenityProcEntry_ {
    int pid;
    int ppid;
    int pgid;
    int sid;
    int tpgid;
    unsigned int uid;
    bool kernel;
    char name[128];
    char executable[512];
    char tty[64];
    char pledge[256];
    char veil[256];
    unsigned long long amount_virtual;
    unsigned long long amount_resident;
    unsigned long long creation_time_ns;
    unsigned long long time_user;
    unsigned long long time_kernel;
    unsigned long long inode_faults;
    unsigned long long zero_faults;
    unsigned long long cow_faults;
    int nlwp;
    char state[32];
    unsigned int cpu;
    int priority;
} SerenityProcEntry;

/* Read /sys/kernel/system_statistics. Returns 0 on success. */
int serenity_read_system_stats(SerenitySystemStats* out);

/* Read /sys/kernel/memory_status. Returns 0 on success. */
int serenity_read_memory_status(SerenityMemoryStats* out);

/* Parse processes JSON buffer. Calls cb for each process.
 * Returns 0 on success. */
typedef void (*SerenityProcCallback)(SerenityProcEntry const* entry, void* userdata);
int serenity_parse_processes(char const* json, size_t len,
    SerenityProcCallback cb, void* userdata);

#ifdef __cplusplus
}
#endif

#endif /* HEADER_SerenityJSON */
