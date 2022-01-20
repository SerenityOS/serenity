/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/LEB128.h>
#include <AK/MemMem.h>
#include <AK/Stream.h>
#include <AK/Vector.h>

namespace AK {

class InputMemoryStream final : public InputStream {
public:
    explicit InputMemoryStream(ReadonlyBytes bytes)
        : m_bytes(bytes)
    {
    }

    bool unreliable_eof() const override { return eof(); }
    bool eof() const { return m_offset >= m_bytes.size(); }

    size_t read(Bytes bytes) override
    {
        if (has_any_error())
            return 0;

        const auto count = min(bytes.size(), remaining());
        __builtin_memcpy(bytes.data(), m_bytes.data() + m_offset, count);
        m_offset += count;
        return count;
    }

    bool read_or_error(Bytes bytes) override
    {
        if (remaining() < bytes.size()) {
            set_recoverable_error();
            return false;
        }

        __builtin_memcpy(bytes.data(), m_bytes.data() + m_offset, bytes.size());
        m_offset += bytes.size();
        return true;
    }

    bool discard_or_error(size_t count) override
    {
        if (remaining() < count) {
            set_recoverable_error();
            return false;
        }

        m_offset += count;
        return true;
    }

    void seek(size_t offset)
    {
        VERIFY(offset < m_bytes.size());
        m_offset = offset;
    }

    u8 peek_or_error() const
    {
        if (remaining() == 0) {
            set_recoverable_error();
            return 0;
        }

        return m_bytes[m_offset];
    }

    template<typename ValueType>
    bool read_LEB128_unsigned(ValueType& result) { return LEB128::read_unsigned(*this, result); }

    template<typename ValueType>
    bool read_LEB128_signed(ValueType& result) { return LEB128::read_signed(*this, result); }

    ReadonlyBytes bytes() const { return m_bytes; }
    size_t offset() const { return m_offset; }
    size_t remaining() const { return m_bytes.size() - m_offset; }

private:
    ReadonlyBytes m_bytes;
    size_t m_offset { 0 };
};

class OutputMemoryStream final : public OutputStream {
public:
    explicit OutputMemoryStream(Bytes bytes)
        : m_bytes(bytes)
    {
    }

    size_t write(ReadonlyBytes bytes) override
    {
        const auto nwritten = bytes.copy_trimmed_to(m_bytes.slice(m_offset));
        m_offset += nwritten;
        return nwritten;
    }

    bool write_or_error(ReadonlyBytes bytes) override
    {
        if (remaining() < bytes.size()) {
            set_recoverable_error();
            return false;
        }

        write(bytes);
        return true;
    }

    size_t fill_to_end(u8 value)
    {
        const auto nwritten = m_bytes.slice(m_offset).fill(value);
        m_offset += nwritten;
        return nwritten;
    }

    bool is_end() const { return remaining() == 0; }

    ReadonlyBytes bytes() const { return { data(), size() }; }
    Bytes bytes() { return { data(), size() }; }

    const u8* data() const { return m_bytes.data(); }
    u8* data() { return m_bytes.data(); }

    size_t size() const { return m_offset; }
    size_t remaining() const { return m_bytes.size() - m_offset; }
    void reset() { m_offset = 0; }

private:
    size_t m_offset { 0 };
    Bytes m_bytes;
};

class DuplexMemoryStream final : public DuplexStream {
public:
    static constexpr size_t chunk_size = 4 * 1024;

    bool unreliable_eof() const override { return eof(); }
    bool eof() const { return m_write_offset == m_read_offset; }

    bool discard_or_error(size_t count) override
    {
        if (m_write_offset - m_read_offset < count) {
            set_recoverable_error();
            return false;
        }

        m_read_offset += count;
        try_discard_chunks();
        return true;
    }

    Optional<size_t> offset_of(ReadonlyBytes value) const
    {
        // We can't directly pass m_chunks to memmem since we have a limited read/write range we want to search in.
        Vector<ReadonlyBytes> spans;
        auto chunk_index = (m_read_offset - m_base_offset) / chunk_size;
        auto chunk_read_offset = (m_read_offset - m_base_offset) % chunk_size;
        auto bytes_to_search = m_write_offset - m_read_offset;
        for (; bytes_to_search > 0;) {
            ReadonlyBytes span = m_chunks[chunk_index];
            if (chunk_read_offset) {
                span = span.slice(chunk_read_offset);
                chunk_read_offset = 0;
            }
            if (bytes_to_search < span.size()) {
                spans.append(span.slice(0, bytes_to_search));
                break;
            }
            bytes_to_search -= span.size();
            spans.append(move(span));
            ++chunk_index;
        }

        return memmem(spans.begin(), spans.end(), value);
    }

    size_t read_without_consuming(Bytes bytes) const
    {
        size_t nread = 0;
        while (bytes.size() - nread > 0 && m_write_offset - m_read_offset - nread > 0) {
            const auto chunk_index = (m_read_offset - m_base_offset + nread) / chunk_size;
            const auto chunk_bytes = m_chunks[chunk_index].bytes().slice((m_read_offset + nread) % chunk_size).trim(m_write_offset - m_read_offset - nread);
            nread += chunk_bytes.copy_trimmed_to(bytes.slice(nread));
        }

        return nread;
    }

    size_t read(Bytes bytes) override
    {
        if (has_any_error())
            return 0;

        const auto nread = read_without_consuming(bytes);

        m_read_offset += nread;
        try_discard_chunks();

        return nread;
    }

    bool read_or_error(Bytes bytes) override
    {
        if (m_write_offset - m_read_offset < bytes.size()) {
            set_recoverable_error();
            return false;
        }

        return read(bytes) == bytes.size();
    }

    size_t write(ReadonlyBytes bytes) override
    {
        // FIXME: This doesn't write around chunk borders correctly?

        size_t nwritten = 0;
        while (bytes.size() - nwritten > 0) {
            if ((m_write_offset + nwritten) % chunk_size == 0)
                m_chunks.append(ByteBuffer::create_uninitialized(chunk_size).release_value_but_fixme_should_propagate_errors()); // FIXME: Handle possible OOM situation.

            nwritten += bytes.slice(nwritten).copy_trimmed_to(m_chunks.last().bytes().slice((m_write_offset + nwritten) % chunk_size));
        }

        m_write_offset += nwritten;
        return nwritten;
    }

    bool write_or_error(ReadonlyBytes bytes) override
    {
        write(bytes);
        return true;
    }

    ByteBuffer copy_into_contiguous_buffer() const
    {
        // FIXME: Handle possible OOM situation.
        auto buffer = ByteBuffer::create_uninitialized(size()).release_value_but_fixme_should_propagate_errors();

        const auto nread = read_without_consuming(buffer);
        VERIFY(nread == buffer.size());

        return buffer;
    }

    size_t roffset() const { return m_read_offset; }
    size_t woffset() const { return m_write_offset; }

    size_t size() const { return m_write_offset - m_read_offset; }

private:
    void try_discard_chunks()
    {
        while (m_read_offset - m_base_offset >= chunk_size) {
            m_chunks.take_first();
            m_base_offset += chunk_size;
        }
    }

    Vector<ByteBuffer> m_chunks;
    size_t m_write_offset { 0 };
    size_t m_read_offset { 0 };
    size_t m_base_offset { 0 };
};

}

using AK::DuplexMemoryStream;
using AK::InputMemoryStream;
using AK::InputStream;
using AK::OutputMemoryStream;
