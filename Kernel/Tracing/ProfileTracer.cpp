/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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
#include <AK/JsonArraySerializer.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Process.h>
#include <Kernel/Tracing/ProfileTracer.h>
#include <Kernel/Tracing/SimpleBufferBuilder.h>
#include <LibELF/ELFLoader.h>

ProfileTracer::ProfileTracer(Process& process)
    : ProcessTracer(process)
    , m_buffer(KBuffer::create_with_size(buffer_size))
    , m_queue(*(CircularQueue<Sample, queue_capacity>*)m_buffer.data())
{
    new (&m_queue) CircularQueue<Sample, queue_capacity>();
    process.notify_profile_tracer_attached();
}

ProfileTracer::~ProfileTracer()
{
    m_queue.clear();

    if (process())
        process()->notify_profile_tracer_detached();
}

ProfileTracer::Sample& ProfileTracer::next_sample_slot()
{
    ASSERT_INTERRUPTS_DISABLED();

    m_queue.enqueue({});
    return const_cast<Sample&>(m_queue.last());
}

void ProfileTracer::symbolicate(Sample& stack) const
{
    if (is_dead())
        return;

    auto& process = *this->process();
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

void ProfileTracer::read_item(SimpleBufferBuilder& builder) const
{
    InterruptDisabler disabler;

    bool mask_kernel_addresses = !current->process().is_superuser();

    Sample sample = m_queue.first();
    symbolicate(sample);

    JsonObjectSerializer object(builder);
    object.add("pid", sample.pid);
    object.add("tid", sample.tid);
    object.add("timestamp", sample.timestamp);
    auto frames_array = object.add_array("frames");
    for (size_t i = 0; i < max_stack_frame_count; i++) {
        if (sample.frames[i] == 0)
            break;
        auto frame_object = frames_array.add_object();
        u32 address = (u32)sample.frames[i];
        if (mask_kernel_addresses && !is_user_address(VirtualAddress(address)))
            address = 0xdeadc0de;
        frame_object.add("address", address);
        frame_object.add("symbol", sample.symbolicated_frames[i]);
        frame_object.add("offset", JsonValue((u32)sample.offsets[i]));
    }
}

void ProfileTracer::dequeue_item()
{
    InterruptDisabler disabler;

    m_queue.dequeue();
}

bool ProfileTracer::have_more_items() const
{
    InterruptDisabler disabler;

    return !m_queue.is_empty();
}
