/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <arpa/inet.h>
#include <errno.h>
#include <serenity.h>
#include <string.h>
#include <syscall.h>

extern "C" {

int disown(pid_t pid)
{
    int rc = syscall(SC_disown, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int module_load(const char* path, size_t path_length)
{
    int rc = syscall(SC_module_load, path, path_length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int module_unload(const char* name, size_t name_length)
{
    int rc = syscall(SC_module_unload, name, name_length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int profiling_enable(pid_t pid, uint64_t event_mask)
{
    int rc = syscall(SC_profiling_enable, pid, event_mask);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int profiling_disable(pid_t pid)
{
    int rc = syscall(SC_profiling_disable, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int profiling_free_buffer(pid_t pid)
{
    int rc = syscall(SC_profiling_free_buffer, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int futex(uint32_t* userspace_address, int futex_op, uint32_t value, const struct timespec* timeout, uint32_t* userspace_address2, uint32_t value3)
{
    int rc;
    switch (futex_op & FUTEX_CMD_MASK) {
    //case FUTEX_CMP_REQUEUE:
    // FUTEX_CMP_REQUEUE_PI:
    case FUTEX_WAKE_OP: {
        // These interpret timeout as a u32 value for val2
        Syscall::SC_futex_params params {
            .userspace_address = userspace_address,
            .futex_op = futex_op,
            .val = value,
            .val2 = (FlatPtr)timeout,
            .userspace_address2 = userspace_address2,
            .val3 = value3
        };
        rc = syscall(SC_futex, &params);
        break;
    }
    default: {
        Syscall::SC_futex_params params {
            .userspace_address = userspace_address,
            .futex_op = futex_op,
            .val = value,
            .timeout = timeout,
            .userspace_address2 = userspace_address2,
            .val3 = value3
        };
        rc = syscall(SC_futex, &params);
        break;
    }
    }
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int purge(int mode)
{
    int rc = syscall(SC_purge, mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int perf_event(int type, uintptr_t arg1, FlatPtr arg2)
{
    int rc = syscall(SC_perf_event, type, arg1, arg2);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int get_stack_bounds(uintptr_t* user_stack_base, size_t* user_stack_size)
{
    int rc = syscall(SC_get_stack_bounds, user_stack_base, user_stack_size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int anon_create(size_t size, int options)
{
    int rc = syscall(SC_anon_create, size, options);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int serenity_readlink(const char* path, size_t path_length, char* buffer, size_t buffer_size)
{
    Syscall::SC_readlink_params small_params {
        { path, path_length },
        { buffer, buffer_size }
    };
    int rc = syscall(SC_readlink, &small_params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setkeymap(const char* name, const u32* map, u32* const shift_map, const u32* alt_map, const u32* altgr_map, const u32* shift_altgr_map)
{
    Syscall::SC_setkeymap_params params { map, shift_map, alt_map, altgr_map, shift_altgr_map, { name, strlen(name) } };
    return syscall(SC_setkeymap, &params);
}

int getkeymap(char* name_buffer, size_t name_buffer_size, u32* map, u32* shift_map, u32* alt_map, u32* altgr_map, u32* shift_altgr_map)
{
    Syscall::SC_getkeymap_params params {
        map,
        shift_map,
        alt_map,
        altgr_map,
        shift_altgr_map,
        { name_buffer, name_buffer_size }
    };
    int rc = syscall(SC_getkeymap, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void set_num_lock(bool on)
{
    syscall(SC_set_num_lock, on);
}

u16 internet_checksum(const void* ptr, size_t count)
{
    u32 checksum = 0;
    auto* w = (const u16*)ptr;
    while (count > 1) {
        checksum += ntohs(*w++);
        if (checksum & 0x80000000)
            checksum = (checksum & 0xffff) | (checksum >> 16);
        count -= 2;
    }
    while (checksum >> 16)
        checksum = (checksum & 0xffff) + (checksum >> 16);
    return htons(~checksum);
}
}
