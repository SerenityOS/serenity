/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AsyncStream.h>
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/MaybeOwned.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/TemporaryChange.h>

namespace AK {

class AsyncInputLittleEndianBitStream;

class BufferBitView {
    // These are defined just to replace some 4s and 8s with meaningful expressions.
    using WordType = u32;
    using DoubleWordType = u64;
    static constexpr size_t bits_in_word = sizeof(WordType) * 8;

public:
    BufferBitView(ReadonlyBytes bytes, u8 bit_position)
    {
        auto ptr = reinterpret_cast<FlatPtr>(bytes.data());
        auto buffer_offset_in_bytes = ptr % alignof(WordType);
        auto bytes_in_current_word_to_fill = sizeof(WordType) - buffer_offset_in_bytes;

        m_bit_position = buffer_offset_in_bytes * 8 + bit_position;
        m_bits_left = bytes.size() * 8 - bit_position;
        memcpy(
            reinterpret_cast<u8*>(&m_current_and_next_word) + buffer_offset_in_bytes,
            bytes.data(),
            min(bytes_in_current_word_to_fill, bytes.size()));

        if (bytes.size() > bytes_in_current_word_to_fill) {
            m_aligned_words = ReadonlySpan<WordType> {
                reinterpret_cast<WordType const*>(ptr + bytes_in_current_word_to_fill),
                (bytes.size() - bytes_in_current_word_to_fill) / sizeof(WordType),
            };
            auto unaligned_end = bytes.slice(bytes_in_current_word_to_fill + m_aligned_words.size() * sizeof(WordType));
            memcpy(&m_unaligned_end, unaligned_end.data(), unaligned_end.size());
            refill_next_word();
        }
    }

    size_t bits_left() const { return m_bits_left; }
    size_t bits_consumed(Badge<AsyncInputLittleEndianBitStream>) const { return m_bits_consumed; }

    WordType peek_bits_possibly_past_end() const
    {
        return m_current_and_next_word >> m_bit_position;
    }

    template<typename T = WordType>
    ErrorOr<T> read_bits(u8 count)
    {
        // FIXME: Teach read_bits to read more than 32 bits. This arbitrary limit is present only
        //        because of performance: we need to have current and next word in a single
        //        variable (m_current_and_next_word) and if I store them in unsigned __int128
        //        instead of u64, performance drops dramatically.
        static_assert(sizeof(T) <= sizeof(WordType));
        VERIFY(count <= sizeof(T) * 8);

        if (bits_left() < count)
            return Error::from_errno(EAGAIN);

        T result = peek_bits_possibly_past_end() & ((1ULL << count) - 1);
        advance_read_head(count);
        return result;
    }

    ErrorOr<bool> read_bit()
    {
        if (!bits_left())
            return Error::from_errno(EAGAIN);
        bool result = m_current_and_next_word >> m_bit_position & 1;
        advance_read_head(1);
        return result;
    }

    void consume_bits(size_t count)
    {
        m_bits_consumed += count;
    }

    template<typename Func>
    auto rollback_group(Func&& func)
    {
        auto bits_left_originally = m_bits_left;
        auto result = func();
        if (!result.is_error())
            consume_bits(bits_left_originally - m_bits_left);
        return result;
    }

private:
    void refill_next_word()
    {
        if (!m_aligned_words.is_empty()) {
            m_current_and_next_word |= static_cast<DoubleWordType>(m_aligned_words[0]) << bits_in_word;
            m_aligned_words = m_aligned_words.slice(1);
        } else {
            m_current_and_next_word |= static_cast<DoubleWordType>(m_unaligned_end) << bits_in_word;
            m_unaligned_end = 0;
        }
    }

    void advance_read_head(u8 bits)
    {
        m_bit_position += bits;
        m_bits_left -= bits;
        if (m_bit_position >= bits_in_word) {
            m_bit_position -= bits_in_word;
            m_current_and_next_word >>= bits_in_word;
            refill_next_word();
        }
    }

    u8 m_bit_position { 0 }; // bit offset inside current word
    DoubleWordType m_current_and_next_word { 0 };
    size_t m_bits_left { 0 };
    size_t m_bits_consumed { 0 };

    ReadonlySpan<WordType> m_aligned_words;
    WordType m_unaligned_end { 0 };
};

class AsyncInputLittleEndianBitStream final : public AsyncInputStream {
    AK_MAKE_NONCOPYABLE(AsyncInputLittleEndianBitStream);
    AK_MAKE_NONMOVABLE(AsyncInputLittleEndianBitStream);

public:
    AsyncInputLittleEndianBitStream(MaybeOwned<AsyncInputStream>&& stream)
        : m_stream(move(stream))
    {
    }

    ~AsyncInputLittleEndianBitStream()
    {
        if (is_open())
            reset();
    }

    void reset() override
    {
        VERIFY(is_open());
        m_is_open = false;
        m_stream->reset();
    }

    Coroutine<ErrorOr<void>> close() override
    {
        VERIFY(is_open());
        if (m_bit_position != 0) {
            reset();
            co_return Error::from_errno(EBUSY);
        }
        m_is_open = false;
        if (m_stream.is_owned())
            co_return co_await m_stream->close();
        co_return {};
    }

    bool is_open() const override { return m_is_open; }

    Coroutine<ErrorOr<bool>> enqueue_some(Badge<AsyncInputStream>) override
    {
        auto result = co_await m_stream->enqueue_some(badge());
        if (result.is_error())
            m_is_open = false;

        if (m_stream->buffered_data_unchecked(badge()).size() >= NumericLimits<size_t>::max() / 8) [[unlikely]] {
            // Can realistically only trigger on 32-bit.
            m_stream->reset();
            co_return Error::from_string_literal("Too much data buffered");
        }

        co_return result;
    }

    ReadonlyBytes buffered_data_unchecked(Badge<AsyncInputStream>) const override
    {
        VERIFY(m_bit_position == 0);
        return m_stream->buffered_data_unchecked(badge());
    }

    void dequeue(Badge<AsyncInputStream>, size_t bytes) override
    {
        VERIFY(m_bit_position == 0);
        m_stream->dequeue(badge(), bytes);
    }

    size_t buffered_bits_count() const
    {
        return m_stream->buffered_data().size() * 8 - m_bit_position;
    }

    void align_to_byte_boundary()
    {
        if (m_bit_position != 0) {
            m_bit_position = 0;
            m_stream->dequeue(badge(), 1);
        }
    }

    Coroutine<ErrorOr<void>> peek_bits()
    {
        // Since peek_bits doesn't return anything (we don't a suitable unaligned BitmapView class),
        // it doesn't make sense to call it if the stream won't read anything. For the callers of
        // peek_bits, this means they should ensure peek will be reading by some other means (for
        // example, by returning EAGAIN from the callback of `with_bit_view_of_buffer`).
        //
        // NOTE: If we ever need to peek into bit stream (similarly to how we do this for normal
        //       streams), this function will be a good place to return some theoretical unaligned
        //       Bit(map)?View.
        VERIFY(m_is_reading_peek);
        TemporaryChange bit_position_change { m_bit_position, static_cast<u8>(0) };
        CO_TRY(co_await peek());
        co_return {};
    }

    template<typename T = u64>
    Coroutine<ErrorOr<T>> read_bits(size_t count)
    {
        VERIFY(!m_is_reading_peek);
        VERIFY(count <= 57); // FIXME: Teach peek_bits_sync to peek more than 57 bits.

        while (buffered_bits_count() < count) {
            m_is_reading_peek = true;
            CO_TRY(co_await peek_bits());
        }
        m_is_reading_peek = false;

        auto [value, valid_bits] = peek_bits_sync();
        VERIFY(valid_bits >= count);
        discard_bits(count);
        co_return value & ((1ULL << count) - 1);
    }

    Coroutine<ErrorOr<bool>> read_bit()
    {
        return read_bits<bool>(1);
    }

    template<typename Func>
    ErrorOr<void> with_bit_view_of_buffer(Func&& func)
    {
        BufferBitView bit_view { m_stream->buffered_data(), m_bit_position };
        ErrorOr<void> result = func(bit_view);

        VERIFY(m_is_open);

        if (result.is_error()) {
            if (result.error().code() == EAGAIN) {
                m_is_reading_peek = true;
            } else {
                reset();
                return result.release_error();
            }
        } else {
            m_is_reading_peek = false;
        }

        size_t offset = m_bit_position + bit_view.bits_consumed({});
        m_bit_position = offset % 8;
        if (offset >= 8)
            m_stream->dequeue(badge(), offset / 8);

        return {};
    }

private:
    struct PeekBitsSyncResult {
        u64 value;
        size_t valid_bits;
    };

    PeekBitsSyncResult peek_bits_sync()
    {
        VERIFY(!m_is_reading_peek);
        m_is_reading_peek = true;

        auto data = m_stream->buffered_data();

        u64 value = 0;
        static_assert(HostIsLittleEndian);
        if (data.size() > sizeof(value)) [[likely]] {
            memcpy(&value, data.data(), sizeof(value));
            value >>= m_bit_position;
        } else {
            memcpy(&value, data.data(), min(sizeof(value), data.size()));
            value >>= m_bit_position;
        }

        return { .value = value, .valid_bits = min(64U, data.size() * 8) - m_bit_position };
    }

    void discard_bits(size_t count)
    {
        VERIFY(buffered_bits_count() >= count);

        m_is_reading_peek = false;

        size_t bytes_to_read = (m_bit_position + count) / 8;
        if (bytes_to_read)
            m_stream->dequeue(badge(), bytes_to_read);
        m_bit_position = (m_bit_position + count) % 8;
    }

    MaybeOwned<AsyncInputStream> m_stream;
    bool m_is_open { true };

    u8 m_bit_position { 0 };
};

}

#ifdef USING_AK_GLOBALLY
using AK::AsyncInputLittleEndianBitStream;
using AK::BufferBitView;
#endif
