/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/futex.h>
#include <Kernel/API/POSIX/serenity.h>
#include <stdio.h>
#include <sys/cdefs.h>
#include <time.h>
#include <unistd.h>

__BEGIN_DECLS

int disown(pid_t);

int profiling_enable(pid_t, uint64_t);
int profiling_disable(pid_t);
int profiling_free_buffer(pid_t);

int futex(uint32_t* userspace_address, int futex_op, uint32_t value, const struct timespec* timeout, uint32_t* userspace_address2, uint32_t value3);

#ifndef ALWAYS_INLINE
#    define ALWAYS_INLINE inline __attribute__((always_inline))
#    define ALWAYS_INLINE_SERENITY_H
#endif

static ALWAYS_INLINE int futex_wait(uint32_t* userspace_address, uint32_t value, const struct timespec* abstime, int clockid, int process_shared)
{
    int op;

    if (abstime) {
        // NOTE: FUTEX_WAIT takes a relative timeout, so use FUTEX_WAIT_BITSET instead!
        op = FUTEX_WAIT_BITSET;
        if (clockid == CLOCK_REALTIME || clockid == CLOCK_REALTIME_COARSE)
            op |= FUTEX_CLOCK_REALTIME;
    } else {
        op = FUTEX_WAIT;
    }
    return futex(userspace_address, op | (process_shared ? 0 : FUTEX_PRIVATE_FLAG), value, abstime, NULL, FUTEX_BITSET_MATCH_ANY);
}

static ALWAYS_INLINE int futex_wake(uint32_t* userspace_address, uint32_t count, int process_shared)
{
    return futex(userspace_address, FUTEX_WAKE | (process_shared ? 0 : FUTEX_PRIVATE_FLAG), count, NULL, NULL, 0);
}

#ifdef ALWAYS_INLINE_SERENITY_H
#    undef ALWAYS_INLINE
#endif

int purge(int mode);

int perf_event(int type, uintptr_t arg1, uintptr_t arg2);
int perf_register_string(char const* string, size_t string_length);

int get_stack_bounds(uintptr_t* user_stack_base, size_t* user_stack_size);

int anon_create(size_t size, int options);

int serenity_readlink(char const* path, size_t path_length, char* buffer, size_t buffer_size);

int getkeymap(char* name_buffer, size_t name_buffer_size, uint32_t* map, uint32_t* shift_map, uint32_t* alt_map, uint32_t* altgr_map, uint32_t* shift_altgr_map);
int setkeymap(char const* name, uint32_t const* map, uint32_t* const shift_map, uint32_t const* alt_map, uint32_t const* altgr_map, uint32_t const* shift_altgr_map);

uint16_t internet_checksum(void const* ptr, size_t count);

int serenity_open(char const* path, size_t path_length, int options, ...);

__END_DECLS
