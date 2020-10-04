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

#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>
#include <AK/kstdio.h>

#if !defined(KERNEL)
#    include <AK/ScopedValueRollback.h>
#    include <AK/StringView.h>
#    include <errno.h>
#    include <unistd.h>
#endif

namespace AK {

class LogStream {
public:
    LogStream()
#if !defined(KERNEL)
        : m_errno_restorer(errno)
#endif
    {
    }
    virtual ~LogStream() { }

    virtual void write(const char*, int) const = 0;

private:
#if !defined(KERNEL)
    ScopedValueRollback<int> m_errno_restorer;
#endif
};

class BufferedLogStream : public LogStream {
    mutable size_t m_size { 0 };
    mutable size_t m_capacity { 128 };
    union {
        mutable u8* m_buffer { nullptr };
        mutable u8 m_local_buffer[128];
    } u;

    void grow(size_t bytes_needed) const
    {
        size_t new_capacity = (m_size + bytes_needed + 0x7F) & ~0x7F;
        u8* new_data = static_cast<u8*>(kmalloc(new_capacity));
        if (m_capacity <= sizeof(u.m_local_buffer)) {
            __builtin_memcpy(new_data, u.m_local_buffer, m_size);
        } else if (u.m_buffer) {
            __builtin_memcpy(new_data, u.m_buffer, m_size);
            kfree(u.m_buffer);
        }
        u.m_buffer = new_data;
        m_capacity = new_capacity;
    }

protected:
    u8* data() const
    {
        if (m_capacity <= sizeof(u.m_local_buffer))
            return u.m_local_buffer;
        return u.m_buffer;
    }

    size_t size() const { return m_size; }

    bool empty() const { return m_size == 0; }

public:
    BufferedLogStream() { }

    virtual ~BufferedLogStream() override
    {
        if (m_capacity > sizeof(u.m_local_buffer))
            kfree(u.m_buffer);
    }

    virtual void write(const char* str, int len) const override
    {
        size_t new_size = m_size + len;
        if (new_size > m_capacity)
            grow(len);
        __builtin_memcpy(data() + m_size, str, len);
        m_size = new_size;
    }
};

class DebugLogStream final : public BufferedLogStream {
public:
    DebugLogStream() { }
    virtual ~DebugLogStream() override;

    // DebugLogStream only checks `enabled` and possibly generates output while the destructor runs.
    static void set_enabled(bool);
    static bool is_enabled();

private:
    static bool s_enabled;
};

#if !defined(KERNEL)
class StdLogStream final : public LogStream {
public:
    StdLogStream(int fd)
        : m_fd(fd)
    {
    }
    virtual ~StdLogStream() override;
    virtual void write(const char* characters, int length) const override;

private:
    int m_fd { -1 };
};

inline StdLogStream out() { return StdLogStream(STDOUT_FILENO); }
inline StdLogStream warn() { return StdLogStream(STDERR_FILENO); }
#endif

#ifdef KERNEL
class KernelLogStream final : public BufferedLogStream {
public:
    KernelLogStream() { }
    virtual ~KernelLogStream() override;
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

const LogStream& operator<<(const LogStream&, const FlyString&);
const LogStream& operator<<(const LogStream&, const String&);
const LogStream& operator<<(const LogStream&, const StringView&);
const LogStream& operator<<(const LogStream&, int);
const LogStream& operator<<(const LogStream&, long);
const LogStream& operator<<(const LogStream&, unsigned);
const LogStream& operator<<(const LogStream&, long long);
const LogStream& operator<<(const LogStream&, unsigned long);
const LogStream& operator<<(const LogStream&, unsigned long long);

#if !defined(KERNEL)
const LogStream& operator<<(const LogStream&, double);
const LogStream& operator<<(const LogStream&, float);
#endif

template<typename T>
const LogStream& operator<<(const LogStream& stream, Span<T> span)
{
    return stream << "{ " << span.data() << ", " << span.size() << " }";
}

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

#ifdef KERNEL
KernelLogStream klog();
#else
DebugLogStream klog();
#endif

void dump_bytes(ReadonlyBytes);

}

using AK::dbg;
using AK::klog;
using AK::LogStream;

#if !defined(KERNEL)
using AK::out;
using AK::warn;
#endif
