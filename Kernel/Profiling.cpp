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

void start(Process& process)
{
    s_process = &process;

    if (!s_profiling_buffer) {
        s_profiling_buffer = RefPtr<KBufferImpl>(KBuffer::create_with_size(8 * MB).impl()).leak_ref();
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

static void symbolicate(Sample& stack)
{
    auto& process = *s_process;
    ProcessPagingScope paging_scope(process);
    struct RecognizedSymbol {
        u32 address;
        const KSym* ksym;
    };
    Vector<RecognizedSymbol, max_stack_frame_count> recognized_symbols;
    for (size_t i = 1; i < max_stack_frame_count; ++i) {
        if (stack.frames[i] == 0)
            break;
        recognized_symbols.append({ stack.frames[i], ksymbolicate(stack.frames[i]) });
    }

    size_t i = 1;
    for (auto& symbol : recognized_symbols) {
        if (!symbol.address)
            break;
        auto& symbol_string_slot = stack.symbolicated_frames[i];
        auto& offset_slot = stack.offsets[i];
        ++i;
        if (!symbol.ksym) {
            if (!Scheduler::is_active() && process.elf_loader() && process.elf_loader()->has_symbols())
                symbol_string_slot = process.elf_loader()->symbolicate(symbol.address, &offset_slot);
            else
                symbol_string_slot = String::empty();
            continue;
        }
        u32 offset = symbol.address - symbol.ksym->address;
        if (symbol.ksym->address == ksym_highest_address && offset > 4096) {
            symbol_string_slot = String::empty();
            offset_slot = 0;
        } else {
            symbol_string_slot = demangle(symbol.ksym->name);
            offset_slot = offset;
        }
    }
}

void stop()
{
    for (size_t i = 0; i < s_next_slot_index; ++i) {
        auto& stack = sample_slot(i);
        symbolicate(stack);
    }

    s_process = nullptr;
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
