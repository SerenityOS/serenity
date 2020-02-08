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

#include <AK/LogStream.h>
#include <AK/String.h>
#include <AK/StringView.h>

#ifdef KERNEL
#    include <Kernel/Process.h>
#    include <Kernel/Thread.h>
#endif

namespace AK {

const LogStream& operator<<(const LogStream& stream, const String& value)
{
    stream.write(value.characters(), value.length());
    return stream;
}

const LogStream& operator<<(const LogStream& stream, const StringView& value)
{
    stream.write(value.characters_without_null_termination(), value.length());
    return stream;
}

const LogStream& operator<<(const LogStream& stream, int value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, long value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, long long value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, unsigned value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, unsigned long long value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, unsigned long value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, const void* value)
{
    return stream << String::format("%p", value);
}

#if defined(__serenity__) && !defined(KERNEL)
static TriState got_process_name = TriState::Unknown;
static char process_name_buffer[256];
#endif

DebugLogStream dbg()
{
    DebugLogStream stream;
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
    if (current)
        stream << "\033[34;1m[" << *current << "]\033[0m: ";
    else
        stream << "\033[36;1m[Kernel]\033[0m: ";
#endif
    return stream;
}

}
