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

#pragma once

#include <stdio.h>
#include <unistd.h>

__BEGIN_DECLS

int shbuf_create(int, void** buffer);
int shbuf_allow_pid(int, pid_t peer_pid);
int shbuf_allow_all(int);
void* shbuf_get(int shbuf_id, size_t* size);
int shbuf_release(int shbuf_id);
int shbuf_seal(int shbuf_id);

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

#define PERF_EVENT_MALLOC 1
#define PERF_EVENT_FREE 2

int perf_event(int type, uintptr_t arg1, uintptr_t arg2);

__END_DECLS
