/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Syscall.h>
#include <errno.h>
#include <serenity.h>

extern "C" {

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

int profiling_enable(pid_t pid)
{
    int rc = syscall(SC_profiling_enable, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int profiling_disable(pid_t pid)
{
    int rc = syscall(SC_profiling_disable, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_thread_boost(int tid, int amount)
{
    int rc = syscall(SC_set_thread_boost, tid, amount);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_process_boost(int tid, int amount)
{
    int rc = syscall(SC_set_process_boost, tid, amount);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int futex(int32_t* userspace_address, int futex_op, int32_t value, const struct timespec* timeout)
{
    Syscall::SC_futex_params params { userspace_address, futex_op, value, timeout };
    int rc = syscall(SC_futex, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int purge(int mode)
{
    int rc = syscall(SC_purge, mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int perf_event(int type, uintptr_t arg1, uintptr_t arg2)
{
    int rc = syscall(SC_perf_event, type, arg1, arg2);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void* shbuf_get(int shbuf_id, size_t* size)
{
    int rc = syscall(SC_shbuf_get, shbuf_id, size);
    if (rc < 0 && -rc < EMAXERRNO) {
        errno = -rc;
        return (void*)-1;
    }
    return (void*)rc;
}

int shbuf_release(int shbuf_id)
{
    int rc = syscall(SC_shbuf_release, shbuf_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int shbuf_seal(int shbuf_id)
{
    int rc = syscall(SC_shbuf_seal, shbuf_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int shbuf_create(int size, void** buffer)
{
    int rc = syscall(SC_shbuf_create, size, buffer);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int shbuf_allow_pid(int shbuf_id, pid_t peer_pid)
{
    int rc = syscall(SC_shbuf_allow_pid, shbuf_id, peer_pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int shbuf_allow_all(int shbuf_id)
{
    int rc = syscall(SC_shbuf_allow_all, shbuf_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
