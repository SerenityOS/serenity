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

#include <AK/kstdio.h>
#include <Kernel/Process.h>
#include <Kernel/Tracing/ProcessTracer.h>
#include <Kernel/Tracing/SimpleBufferBuilder.h>

ProcessTracer::ProcessTracer(Process& process)
    : m_process(&process)
{
    process.add_tracer(*this);
}

ProcessTracer::~ProcessTracer()
{
    if (m_process)
        m_process->remove_tracer(*this);
}

String ProcessTracer::absolute_path(const FileDescription&) const
{
    if (!m_process)
        return "tracer:(dead)";
    return String::format("tracer:%d", m_process->pid());
}

int ProcessTracer::read(FileDescription&, u8* buffer, int buffer_size)
{
    if (buffer_size == 0)
        return 0;

    int nread = 0;
    SimpleBufferBuilder builder(buffer, buffer_size);

    while (have_more_items()) {
        // Try to append the next item.
        if (!m_read_first_item)
            builder.append('[');
        else
            builder.append(',');

        read_item(builder);

        // See if that worked.
        if (builder.overflown())
            break;

        // If it did, commit the new state.
        m_read_first_item = true;
        dequeue_item();
        nread = builder.nwritten();
    }

    if (!have_more_items() && is_dead() && !m_read_closing_bracket) {
        builder.append(']');
        if (!builder.overflown()) {
            nread = builder.nwritten();
            m_read_closing_bracket = true;
        }
    }

    if (have_more_items() && nread == 0) {
        dbg() << "Buffer is too small to read even a single item, not cool :(";
        return -EIO;
    }

    return nread;
}
