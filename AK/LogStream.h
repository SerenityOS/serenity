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

#include <AK/Types.h>
#include <AK/kstdio.h>

#if !defined(KERNEL) && !defined(BOOTSTRAPPER)
#    include <AK/ScopedValueRollback.h>
#    include <AK/StringView.h>
#    include <errno.h>
#    include <unistd.h>
#endif

namespace AK {

class String;
class StringView;

class LogStream {
public:
    LogStream()
#if !defined(KERNEL) && !defined(BOOTSTRAPPER)
        : m_errno_restorer(errno)
#endif
    {
    }
    virtual ~LogStream() {}

    virtual void write(const char*, int) const = 0;

private:
#if !defined(KERNEL) && !defined(BOOTSTRAPPER)
    ScopedValueRollback<int> m_errno_restorer;
#endif
};

class DebugLogStream final : public LogStream {
public:
    DebugLogStream() {}
    virtual ~DebugLogStream() override;

    virtual void write(const char* characters, int length) const override
    {
        dbgputstr(characters, length);
    }
};

#if !defined(BOOTSTRAPPER) && defined(KERNEL)
class KernelLogStream final : public LogStream {
public:
    KernelLogStream() {}
    virtual ~KernelLogStream() override;

    virtual void write(const char* characters, int length) const override
    {
        kernelputstr(characters, length);
    }
};
#endif

inline const LogStream& operator<<(const LogStream& stream, const char* value)
{
    if (!value)
        return stream << "(null)";
    int length = 0;
    const char* p = value;
    while (*(p++))
        ++length;
    stream.write(value, length);
    return stream;
}

const LogStream& operator<<(const LogStream&, const String&);
const LogStream& operator<<(const LogStream&, const StringView&);
const LogStream& operator<<(const LogStream&, int);
const LogStream& operator<<(const LogStream&, unsigned);
const LogStream& operator<<(const LogStream&, long long);
const LogStream& operator<<(const LogStream&, unsigned long);
const LogStream& operator<<(const LogStream&, unsigned long long);

const LogStream& operator<<(const LogStream&, const void*);

inline const LogStream& operator<<(const LogStream& stream, char value)
{
    stream.write(&value, 1);
    return stream;
}

inline const LogStream& operator<<(const LogStream& stream, bool value)
{
    return stream << (value ? "true" : "false");
}

DebugLogStream dbg();

#if defined(KERNEL)
KernelLogStream klog();
#elif !defined(BOOTSTRAPPER)
DebugLogStream klog();
#endif

}

using AK::dbg;
using AK::klog;
using AK::LogStream;
