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
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/KBuffer.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Profiling.h>

namespace Kernel {

namespace Profiling {

static size_t s_slot_count;
static AK::Singleton<KBuffer, []() -> KBuffer* {
    auto buffer = KBuffer::try_create_with_size(8 * MiB, Region::Access::Read | Region::Access::Write, "Profiling Buffer", AllocationStrategy::AllocateNow);
    s_slot_count = buffer->size() / sizeof(Sample);
    return buffer.leak_ptr();
}>
    s_profiling_buffer;
static size_t s_next_slot_index;
static ProcessID s_pid { -1 };

String& executable_path()
{
    static String* path;
    if (!path)
        path = new String;
    return *path;
}

ProcessID pid()
{
    return s_pid;
}

void start(Process& process)
{
    if (process.executable())
        executable_path() = process.executable()->absolute_path().impl();
    else
        executable_path() = {};
    s_pid = process.pid();

    s_profiling_buffer.ensure_instance();

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
    // FIXME: This probably shouldn't be empty.
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
