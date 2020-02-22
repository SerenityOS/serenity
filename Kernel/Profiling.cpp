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

#include <AK/Demangle.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/KBuffer.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Profiling.h>
#include <LibELF/ELFLoader.h>

namespace Kernel {

namespace Profiling {

static KBufferImpl* s_profiling_buffer;
static size_t s_slot_count;
static size_t s_next_slot_index;
static Process* s_process;
static u32 s_pid;

String& executable_path()
{
    static String* path;
    if (!path)
        path = new String;
    return *path;
}

u32 pid()
{
    return s_pid;
}

void start(Process& process)
{
    s_process = &process;

    executable_path() = process.executable()->absolute_path().impl();
    s_pid = process.pid();

    if (!s_profiling_buffer) {
        s_profiling_buffer = RefPtr<KBufferImpl>(KBuffer::create_with_size(8 * MB).impl()).leak_ref();
        s_profiling_buffer->region().commit();
        s_slot_count = s_profiling_buffer->size() / sizeof(Sample);
    }

    s_next_slot_index = 0;
}

static Sample& sample_slot(size_t index)
{
    return ((Sample*)s_profiling_buffer->data())[index];
}

Sample& next_sample_slot()
{
    auto& slot = sample_slot(s_next_slot_index++);
    if (s_next_slot_index >= s_slot_count)
        s_next_slot_index = 0;
    return slot;
}

void stop()
{
    s_process = nullptr;
}

void did_exec(const String& new_executable_path)
{
    executable_path() = new_executable_path;
    s_next_slot_index = 0;
}

void for_each_sample(Function<void(Sample&)> callback)
{
    for (size_t i = 0; i < s_next_slot_index; ++i) {
        auto& sample = sample_slot(i);
        callback(sample);
    }
}

}

}
