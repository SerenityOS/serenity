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

#include <AK/Stream.h>

namespace AK {

// FIXME: Implement Buffered<T> for DuplexStream.

template<typename StreamType, size_t Size = 4096, typename = void>
class Buffered;

template<typename StreamType, size_t Size>
class Buffered<StreamType, Size, typename EnableIf<IsBaseOf<InputStream, StreamType>::value>::Type> final : public InputStream {
    AK_MAKE_NONCOPYABLE(Buffered);

public:
    template<typename... Parameters>
    explicit Buffered(Parameters&&... parameters)
        : m_stream(forward<Parameters>(parameters)...)
    {
    }

    Buffered(Buffered&& other)
        : m_stream(move(other.m_stream))
    {
        other.buffer().copy_to(buffer());
        m_buffered = exchange(other.m_buffered, 0);
    }

    bool has_recoverable_error() const override { return m_stream.has_recoverable_error(); }
    bool has_fatal_error() const override { return m_stream.has_fatal_error(); }
    bool has_any_error() const override { return m_stream.has_any_error(); }

    bool handle_recoverable_error() override { return m_stream.handle_recoverable_error(); }
    bool handle_fatal_error() override { return m_stream.handle_fatal_error(); }
    bool handle_any_error() override { return m_stream.handle_any_error(); }

    void set_recoverable_error() const override { return m_stream.set_recoverable_error(); }
    void set_fatal_error() const override { return m_stream.set_fatal_error(); }

    size_t read(Bytes bytes) override
    {
        if (has_any_error())
            return 0;

        auto nread = buffer().trim(m_buffered).copy_trimmed_to(bytes);

        m_buffered -= nread;
        buffer().slice(nread, m_buffered).copy_to(buffer());

        if (nread < bytes.size()) {
            m_buffered = m_stream.read(buffer());

            if (m_buffered == 0)
                return nread;

            nread += read(bytes.slice(nread));
        }

        return nread;
    }

    bool read_or_error(Bytes bytes) override
    {
        if (read(bytes) < bytes.size()) {
            set_fatal_error();
            return false;
        }

        return true;
    }

    bool unreliable_eof() const override { return m_buffered == 0 && m_stream.unreliable_eof(); }

    bool eof() const
    {
        if (m_buffered > 0)
            return false;

        m_buffered = m_stream.read(buffer());

        return m_buffered == 0;
    }

    bool discard_or_error(size_t count) override
    {
        size_t ndiscarded = 0;
        while (ndiscarded < count) {
            u8 dummy[Size];

            if (!read_or_error({ dummy, min(Size, count - ndiscarded) }))
                return false;

            ndiscarded += min(Size, count - ndiscarded);
        }

        return true;
    }

private:
    Bytes buffer() const { return { m_buffer, Size }; }

    mutable StreamType m_stream;
    mutable u8 m_buffer[Size];
    mutable size_t m_buffered { 0 };
};

template<typename StreamType, size_t Size>
class Buffered<StreamType, Size, typename EnableIf<IsBaseOf<OutputStream, StreamType>::value>::Type> final : public OutputStream {
    AK_MAKE_NONCOPYABLE(Buffered);

public:
    template<typename... Parameters>
    explicit Buffered(Parameters&&... parameters)
        : m_stream(forward<Parameters>(parameters)...)
    {
    }

    Buffered(Buffered&& other)
        : m_stream(move(other.m_stream))
    {
        other.buffer().copy_to(buffer());
        m_buffered = exchange(other.m_buffered, 0);
    }

    ~Buffered()
    {
        if (m_buffered > 0)
            flush();
    }

    bool has_recoverable_error() const override { return m_stream.has_recoverable_error(); }
    bool has_fatal_error() const override { return m_stream.has_fatal_error(); }
    bool has_any_error() const override { return m_stream.has_any_error(); }

    bool handle_recoverable_error() override { return m_stream.handle_recoverable_error(); }
    bool handle_fatal_error() override { return m_stream.handle_fatal_error(); }
    bool handle_any_error() override { return m_stream.handle_any_error(); }

    void set_recoverable_error() const override { return m_stream.set_recoverable_error(); }
    void set_fatal_error() const override { return m_stream.set_fatal_error(); }

    size_t write(ReadonlyBytes bytes) override
    {
        if (has_any_error())
            return 0;

        auto nwritten = bytes.copy_trimmed_to(buffer().slice(m_buffered));
        m_buffered += nwritten;

        if (m_buffered == Size) {
            flush();

            if (bytes.size() - nwritten >= Size)
                nwritten += m_stream.write(bytes.slice(nwritten));

            nwritten += write(bytes.slice(nwritten));
        }

        return nwritten;
    }

    bool write_or_error(ReadonlyBytes bytes) override
    {
        write(bytes);
        return true;
    }

    void flush()
    {
        m_stream.write_or_error({ m_buffer, m_buffered });
        m_buffered = 0;
    }

private:
    Bytes buffer() { return { m_buffer, Size }; }

    StreamType m_stream;
    u8 m_buffer[Size];
    size_t m_buffered { 0 };
};

}

using AK::Buffered;
