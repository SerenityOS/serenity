/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Concepts.h>
#include <AK/Endian.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>

namespace AK::Detail {

class Stream {
public:
    virtual ~Stream() { ASSERT(!has_any_error()); }

    virtual bool has_recoverable_error() const { return m_recoverable_error; }
    virtual bool has_fatal_error() const { return m_fatal_error; }
    virtual bool has_any_error() const { return has_recoverable_error() || has_fatal_error(); }

    virtual bool handle_recoverable_error()
    {
        ASSERT(!has_fatal_error());
        return exchange(m_recoverable_error, false);
    }
    virtual bool handle_fatal_error() { return exchange(m_fatal_error, false); }
    virtual bool handle_any_error()
    {
        if (has_any_error()) {
            m_recoverable_error = false;
            m_fatal_error = false;

            return true;
        }

        return false;
    }

    virtual void set_recoverable_error() const { m_recoverable_error = true; }
    virtual void set_fatal_error() const { m_fatal_error = true; }

private:
    mutable bool m_recoverable_error { false };
    mutable bool m_fatal_error { false };
};

}

namespace AK {

class InputStream : public virtual AK::Detail::Stream {
public:
    // Reads at least one byte unless none are requested or none are available. Does nothing
    // and returns zero if there is already an error.
    virtual size_t read(Bytes) = 0;

    // If this function returns true, then no more data can be read. If read(Bytes) previously
    // returned zero even though bytes were requested, then the inverse is true as well.
    virtual bool unreliable_eof() const = 0;

    // Some streams additionally define a method with the signature:
    //
    //     bool eof() const;
    //
    // This method has the same semantics as unreliable_eof() but returns true if and only if no
    // more data can be read. (A failed read is not necessary.)

    virtual bool read_or_error(Bytes) = 0;
    virtual bool discard_or_error(size_t count) = 0;
};

class OutputStream : public virtual AK::Detail::Stream {
public:
    virtual size_t write(ReadonlyBytes) = 0;
    virtual bool write_or_error(ReadonlyBytes) = 0;
};

class DuplexStream
    : public InputStream
    , public OutputStream {
};

inline InputStream& operator>>(InputStream& stream, Bytes bytes)
{
    stream.read_or_error(bytes);
    return stream;
}
inline OutputStream& operator<<(OutputStream& stream, ReadonlyBytes bytes)
{
    stream.write_or_error(bytes);
    return stream;
}

template<typename T>
InputStream& operator>>(InputStream& stream, LittleEndian<T>& value)
{
    return stream >> Bytes { &value.m_value, sizeof(value.m_value) };
}
template<typename T>
OutputStream& operator<<(OutputStream& stream, LittleEndian<T> value)
{
    return stream << ReadonlyBytes { &value.m_value, sizeof(value.m_value) };
}

template<typename T>
InputStream& operator>>(InputStream& stream, BigEndian<T>& value)
{
    return stream >> Bytes { &value.m_value, sizeof(value.m_value) };
}
template<typename T>
OutputStream& operator<<(OutputStream& stream, BigEndian<T> value)
{
    return stream << ReadonlyBytes { &value.m_value, sizeof(value.m_value) };
}

template<typename T>
InputStream& operator>>(InputStream& stream, Optional<T>& value)
{
    T temporary;
    stream >> temporary;
    value = temporary;
    return stream;
}

template<typename Integral, typename EnableIf<IsIntegral<Integral>::value, int>::Type = 0>
InputStream& operator>>(InputStream& stream, Integral& value)
{
    stream.read_or_error({ &value, sizeof(value) });
    return stream;
}
template<typename Integral, typename EnableIf<IsIntegral<Integral>::value, int>::Type = 0>
OutputStream& operator<<(OutputStream& stream, Integral value)
{
    stream.write_or_error({ &value, sizeof(value) });
    return stream;
}

#ifndef KERNEL

template<typename FloatingPoint, typename EnableIf<IsFloatingPoint<FloatingPoint>::value, int>::Type = 0>
InputStream& operator>>(InputStream& stream, FloatingPoint& value)
{
    stream.read_or_error({ &value, sizeof(value) });
    return stream;
}
template<typename FloatingPoint, typename EnableIf<IsFloatingPoint<FloatingPoint>::value, int>::Type = 0>
OutputStream& operator<<(OutputStream& stream, FloatingPoint value)
{
    stream.write_or_error({ &value, sizeof(value) });
    return stream;
}

#endif

inline InputStream& operator>>(InputStream& stream, bool& value)
{
    stream.read_or_error({ &value, sizeof(value) });
    return stream;
}
inline OutputStream& operator<<(OutputStream& stream, bool value)
{
    stream.write_or_error({ &value, sizeof(value) });
    return stream;
}

}
