#pragma once

#include <stdio.h>
#include <unistd.h>

__BEGIN_DECLS

int module_load(const char* path, size_t path_length);
int module_unload(const char* name, size_t name_length);

int profiling_enable(pid_t);
int profiling_disable(pid_t);

#define THREAD_PRIORITY_MIN 1
#define THREAD_PRIORITY_LOW 10
#define THREAD_PRIORITY_NORMAL 30
#define THREAD_PRIORITY_HIGH 50
#define THREAD_PRIORITY_MAX 99

int set_thread_boost(int tid, int amount);
int set_process_boost(pid_t, int amount);

#define FUTEX_WAIT 1
#define FUTEX_WAKE 2

int futex(int32_t* userspace_address, int futex_op, int32_t value, const struct timespec* timeout);

#define PURGE_ALL_VOLATILE 0x1
#define PURGE_ALL_CLEAN_INODE 0x2

int purge(int mode);

__END_DECLS
