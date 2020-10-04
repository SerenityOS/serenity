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

#include <AK/FlyString.h>
#include <AK/LogStream.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>

#ifdef KERNEL
#    include <Kernel/Process.h>
#    include <Kernel/Thread.h>
#endif

#if !defined(KERNEL)
#    include <stdio.h>
#endif

namespace AK {

const LogStream& operator<<(const LogStream& stream, const String& value)
{
    stream.write(value.characters(), value.length());
    return stream;
}

const LogStream& operator<<(const LogStream& stream, const FlyString& value)
{
    return stream << value.view();
}

const LogStream& operator<<(const LogStream& stream, const StringView& value)
{
    stream.write(value.characters_without_null_termination(), value.length());
    return stream;
}

const LogStream& operator<<(const LogStream& stream, int value)
{
    char buffer[32];
    snprintf(buffer, sizeof(buffer), "%d", value);
    return stream << buffer;
}

const LogStream& operator<<(const LogStream& stream, long value)
{
    char buffer[32];
    snprintf(buffer, sizeof(buffer), "%ld", value);
    return stream << buffer;
}

const LogStream& operator<<(const LogStream& stream, long long value)
{
    char buffer[32];
    snprintf(buffer, sizeof(buffer), "%lld", value);
    return stream << buffer;
}

const LogStream& operator<<(const LogStream& stream, unsigned value)
{
    char buffer[32];
    snprintf(buffer, sizeof(buffer), "%u", value);
    return stream << buffer;
}

const LogStream& operator<<(const LogStream& stream, unsigned long long value)
{
    char buffer[32];
    snprintf(buffer, sizeof(buffer), "%llu", value);
    return stream << buffer;
}

const LogStream& operator<<(const LogStream& stream, unsigned long value)
{
    char buffer[32];
    snprintf(buffer, sizeof(buffer), "%lu", value);
    return stream << buffer;
}

const LogStream& operator<<(const LogStream& stream, const void* value)
{
    char buffer[32];
    snprintf(buffer, sizeof(buffer), "%p", value);
    return stream << buffer;
}

#if defined(__serenity__) && !defined(KERNEL)
static TriState got_process_name = TriState::Unknown;
static char process_name_buffer[256];
#endif

DebugLogStream dbg()
{
    DebugLogStream stream;

    // FIXME: This logic is redundant with the stuff in Format.cpp.
#if defined(__serenity__) && !defined(KERNEL)
    if (got_process_name == TriState::Unknown) {
        if (get_process_name(process_name_buffer, sizeof(process_name_buffer)) == 0)
            got_process_name = TriState::True;
        else
            got_process_name = TriState::False;
    }
    if (got_process_name == TriState::True)
        stream << "\033[33;1m" << process_name_buffer << '(' << getpid() << ")\033[0m: ";
#endif
#if defined(__serenity__) && defined(KERNEL)
    if (Kernel::Processor::is_initialized() && Kernel::Thread::current())
        stream << "\033[34;1m[" << *Kernel::Thread::current() << "]\033[0m: ";
    else
        stream << "\033[36;1m[Kernel]\033[0m: ";
#endif
    return stream;
}

#ifdef KERNEL
KernelLogStream klog()
{
    KernelLogStream stream;
    if (Kernel::Processor::is_initialized() && Kernel::Thread::current())
        stream << "\033[34;1m[" << *Kernel::Thread::current() << "]\033[0m: ";
    else
        stream << "\033[36;1m[Kernel]\033[0m: ";
    return stream;
}
#else
DebugLogStream klog()
{
    return dbg();
}
#endif

#ifdef KERNEL
KernelLogStream::~KernelLogStream()
{
    if (!empty()) {
        char newline = '\n';
        write(&newline, 1);
        kernelputstr(reinterpret_cast<char*>(data()), size());
    }
}
#endif

DebugLogStream::~DebugLogStream()
{
    if (!empty() && s_enabled) {
        char newline = '\n';
        write(&newline, 1);
        dbgputstr(reinterpret_cast<char*>(data()), size());
    }
}

void DebugLogStream::set_enabled(bool enabled)
{
    s_enabled = enabled;
}

bool DebugLogStream::is_enabled()
{
    return s_enabled;
}

bool DebugLogStream::s_enabled = true;

#ifndef KERNEL
StdLogStream::~StdLogStream()
{
    char newline = '\n';
    write(&newline, 1);
}

void StdLogStream::write(const char* characters, int length) const
{
    if (::write(m_fd, characters, length) < 0) {
        perror("StdLogStream::write");
        ASSERT_NOT_REACHED();
    }
}

const LogStream& operator<<(const LogStream& stream, double value)
{
    return stream << String::format("%.4f", value);
}

const LogStream& operator<<(const LogStream& stream, float value)
{
    return stream << String::format("%.4f", value);
}

#endif

void dump_bytes(ReadonlyBytes bytes)
{
    StringBuilder builder;

    u8 buffered_byte = 0;
    size_t nrepeat = 0;
    const char* prefix = "";

    auto flush = [&]() {
        if (nrepeat > 0) {
            if (nrepeat == 1)
                builder.appendf("%s0x%02x", prefix, static_cast<int>(buffered_byte));
            else
                builder.appendf("%s%zu * 0x%02x", prefix, nrepeat, static_cast<int>(buffered_byte));

            nrepeat = 0;
            prefix = ", ";
        }
    };

    builder.append("{ ");

    for (auto byte : bytes) {
        if (nrepeat > 0) {
            if (byte != buffered_byte)
                flush();

            buffered_byte = byte;
            nrepeat++;
        } else {
            buffered_byte = byte;
            nrepeat = 1;
        }
    }
    flush();

    builder.append(" }");

    dbg() << builder.to_string();
}

}
