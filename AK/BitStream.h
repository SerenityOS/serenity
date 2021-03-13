/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@gmail.com>
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

#include <AK/Optional.h>
#include <AK/Stream.h>

namespace AK {

class InputBitStream final : public InputStream {
public:
    explicit InputBitStream(InputStream& stream)
        : m_stream(stream)
    {
    }

    size_t read(Bytes bytes) override
    {
        if (has_any_error())
            return 0;

        size_t nread = 0;
        if (bytes.size() >= 1) {
            if (m_next_byte.has_value()) {
                bytes[0] = m_next_byte.value();
                m_next_byte.clear();

                ++nread;
            }
        }

        return nread + m_stream.read(bytes.slice(nread));
    }

    bool read_or_error(Bytes bytes) override
    {
        if (read(bytes) != bytes.size()) {
            set_fatal_error();
            return false;
        }

        return true;
    }

    bool unreliable_eof() const override { return !m_next_byte.has_value() && m_stream.unreliable_eof(); }

    bool discard_or_error(size_t count) override
    {
        if (count >= 1) {
            if (m_next_byte.has_value()) {
                m_next_byte.clear();
                --count;
            }
        }

        return m_stream.discard_or_error(count);
    }

    u32 read_bits(size_t count)
    {
        u32 result = 0;

        size_t nread = 0;
        while (nread < count) {
            if (m_stream.has_any_error()) {
                set_fatal_error();
                return 0;
            }

            if (m_next_byte.has_value()) {
                const auto bit = (m_next_byte.value() >> m_bit_offset) & 1;
                result |= bit << nread;
                ++nread;

                if (m_bit_offset++ == 7)
                    m_next_byte.clear();
            } else {
                m_stream >> m_next_byte;
                m_bit_offset = 0;
            }
        }

        return result;
    }

    bool read_bit() { return static_cast<bool>(read_bits(1)); }

    void align_to_byte_boundary()
    {
        if (m_next_byte.has_value())
            m_next_byte.clear();
    }

private:
    Optional<u8> m_next_byte;
    size_t m_bit_offset { 0 };
    InputStream& m_stream;
};

class OutputBitStream final : public OutputStream {
public:
    explicit OutputBitStream(OutputStream& stream)
        : m_stream(stream)
    {
    }

    // WARNING: write aligns to the next byte boundary before writing, if unaligned writes are needed this should be rewritten
    size_t write(ReadonlyBytes bytes) override
    {
        if (has_any_error())
            return 0;
        align_to_byte_boundary();
        if (has_fatal_error()) // if align_to_byte_boundary failed
            return 0;
        return m_stream.write(bytes);
    }

    bool write_or_error(ReadonlyBytes bytes) override
    {
        if (write(bytes) < bytes.size()) {
            set_fatal_error();
            return false;
        }
        return true;
    }

    void write_bits(u32 bits, size_t count)
    {
        VERIFY(count <= 32);

        if (count == 32 && !m_next_byte.has_value()) { // fast path for aligned 32 bit writes
            m_stream << bits;
            return;
        }

        size_t n_written = 0;
        while (n_written < count) {
            if (m_stream.has_any_error()) {
                set_fatal_error();
                return;
            }

            if (m_next_byte.has_value()) {
                m_next_byte.value() |= ((bits >> n_written) & 1) << m_bit_offset;
                ++n_written;

                if (m_bit_offset++ == 7) {
                    m_stream << m_next_byte.value();
                    m_next_byte.clear();
                }
            } else if (count - n_written >= 16) { // fast path for aligned 16 bit writes
                m_stream << (u16)((bits >> n_written) & 0xFFFF);
                n_written += 16;
            } else if (count - n_written >= 8) { // fast path for aligned 8 bit writes
                m_stream << (u8)((bits >> n_written) & 0xFF);
                n_written += 8;
            } else {
                m_bit_offset = 0;
                m_next_byte = 0;
            }
        }
    }

    void write_bit(bool bit)
    {
        write_bits(bit, 1);
    }

    void align_to_byte_boundary()
    {
        if (m_next_byte.has_value()) {
            if (!m_stream.write_or_error(ReadonlyBytes { &m_next_byte.value(), 1 })) {
                set_fatal_error();
            }
            m_next_byte.clear();
        }
    }

    size_t bit_offset() const
    {
        return m_bit_offset;
    }

private:
    Optional<u8> m_next_byte;
    size_t m_bit_offset { 0 };
    OutputStream& m_stream;
};

}

using AK::InputBitStream;
using AK::OutputBitStream;
