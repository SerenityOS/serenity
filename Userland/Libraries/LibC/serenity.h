/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>

__BEGIN_DECLS

int disown(pid_t);

int module_load(const char* path, size_t path_length);
int module_unload(const char* name, size_t name_length);

int profiling_enable(pid_t, uint64_t);
int profiling_disable(pid_t);
int profiling_free_buffer(pid_t);

#define THREAD_PRIORITY_MIN 1
#define THREAD_PRIORITY_LOW 10
#define THREAD_PRIORITY_NORMAL 30
#define THREAD_PRIORITY_HIGH 50
#define THREAD_PRIORITY_MAX 99

#define _FUTEX_OP_SHIFT_OP 28
#define _FUTEX_OP_MASK_OP 0xf
#define _FUTEX_OP_SHIFT_CMP 24
#define _FUTEX_OP_MASK_CMP 0xf
#define _FUTEX_OP_SHIFT_OP_ARG 12
#define _FUTEX_OP_MASK_OP_ARG 0xfff
#define _FUTEX_OP_SHIFT_CMP_ARG 0
#define _FUTEX_OP_MASK_CMP_ARG 0xfff

#define FUTEX_OP(op, op_arg, cmp, cmp_arg) \
    ((((op)&_FUTEX_OP_MASK_OP) << _FUTEX_OP_SHIFT_OP) | (((cmp)&_FUTEX_OP_MASK_CMP) << _FUTEX_OP_SHIFT_CMP) | (((op_arg)&_FUTEX_OP_MASK_OP_ARG) << _FUTEX_OP_SHIFT_OP_ARG) | (((cmp_arg)&_FUTEX_OP_MASK_CMP_ARG) << _FUTEX_OP_SHIFT_CMP_ARG))

#define FUTEX_OP_SET 0
#define FUTEX_OP_ADD 1
#define FUTEX_OP_OR 2
#define FUTEX_OP_ANDN 3
#define FUTEX_OP_XOR 4
#define FUTEX_OP_ARG_SHIFT 8

#define FUTEX_OP_CMP_EQ 0
#define FUTEX_OP_CMP_NE 1
#define FUTEX_OP_CMP_LT 2
#define FUTEX_OP_CMP_LE 3
#define FUTEX_OP_CMP_GT 4
#define FUTEX_OP_CMP_GE 5

#define FUTEX_WAIT 1
#define FUTEX_WAKE 2

#define FUTEX_REQUEUE 3
#define FUTEX_CMP_REQUEUE 4
#define FUTEX_WAKE_OP 5
#define FUTEX_WAIT_BITSET 9
#define FUTEX_WAKE_BITSET 10

#define FUTEX_PRIVATE_FLAG (1 << 7)
#define FUTEX_CLOCK_REALTIME (1 << 8)
#define FUTEX_CMD_MASK ~(FUTEX_PRIVATE_FLAG | FUTEX_CLOCK_REALTIME)

#define FUTEX_BITSET_MATCH_ANY 0xffffffff

int futex(uint32_t* userspace_address, int futex_op, uint32_t value, const struct timespec* timeout, uint32_t* userspace_address2, uint32_t value3);

#define PURGE_ALL_VOLATILE 0x1
#define PURGE_ALL_CLEAN_INODE 0x2

int purge(int mode);

enum {
    PERF_EVENT_SAMPLE = 1,
    PERF_EVENT_MALLOC = 2,
    PERF_EVENT_FREE = 4,
    PERF_EVENT_MMAP = 8,
    PERF_EVENT_MUNMAP = 16,
    PERF_EVENT_PROCESS_CREATE = 32,
    PERF_EVENT_PROCESS_EXEC = 64,
    PERF_EVENT_PROCESS_EXIT = 128,
    PERF_EVENT_THREAD_CREATE = 256,
    PERF_EVENT_THREAD_EXIT = 512,
    PERF_EVENT_CONTEXT_SWITCH = 1024,
    PERF_EVENT_KMALLOC = 2048,
    PERF_EVENT_KFREE = 4096,
    PERF_EVENT_PAGE_FAULT = 8192,
};

#define PERF_EVENT_MASK_ALL (~0ull)

int perf_event(int type, uintptr_t arg1, uintptr_t arg2);

int get_stack_bounds(uintptr_t* user_stack_base, size_t* user_stack_size);

int anon_create(size_t size, int options);

int serenity_readlink(const char* path, size_t path_length, char* buffer, size_t buffer_size);

int getkeymap(char* name_buffer, size_t name_buffer_size, uint32_t* map, uint32_t* shift_map, uint32_t* alt_map, uint32_t* altgr_map, uint32_t* shift_altgr_map);
int setkeymap(const char* name, const uint32_t* map, uint32_t* const shift_map, const uint32_t* alt_map, const uint32_t* altgr_map, const uint32_t* shift_altgr_map);

void set_num_lock(bool on);

uint16_t internet_checksum(const void* ptr, size_t count);

__END_DECLS
