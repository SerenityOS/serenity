/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitStream.h>
#include <AK/Concepts.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/IntegralMath.h>
#include <AK/MemoryStream.h>
#include <AK/Vector.h>

namespace Compress {

template<InputBitStream InputStream>
class LZWDecoder {
private:
    static constexpr int max_code_size = 12;

public:
    explicit LZWDecoder(MaybeOwned<InputStream> lzw_stream, u8 min_code_size, i32 offset_for_size_change = 0)
        : m_bit_stream(move(lzw_stream))
        , m_code_size(min_code_size)
        , m_original_code_size(min_code_size)
        , m_table_capacity(AK::exp2<u32>(min_code_size))
        , m_offset_for_size_change(offset_for_size_change)

    {
        init_code_table();
    }

    static ErrorOr<ByteBuffer> decode_all(ReadonlyBytes bytes, u8 initial_code_size, i32 offset_for_size_change = 0)
    {
        auto memory_stream = make<FixedMemoryStream>(bytes);
        auto lzw_stream = make<InputStream>(MaybeOwned<Stream>(move(memory_stream)));
        Compress::LZWDecoder lzw_decoder { MaybeOwned<InputStream> { move(lzw_stream) }, initial_code_size, offset_for_size_change };

        ByteBuffer decoded;

        u16 const clear_code = lzw_decoder.add_control_code();
        u16 const end_of_data_code = lzw_decoder.add_control_code();

        while (true) {
            auto const code = TRY(lzw_decoder.next_code());

            if (code == clear_code) {
                lzw_decoder.reset();
                continue;
            }

            if (code == end_of_data_code)
                break;

            TRY(decoded.try_append(lzw_decoder.get_output()));
        }

        return decoded;
    }

    u16 add_control_code()
    {
        u16 const control_code = m_code_table.size();
        m_code_table.append(Vector<u8> {});
        m_original_code_table.append(Vector<u8> {});
        if (m_code_table.size() >= m_table_capacity && m_code_size < max_code_size) {
            ++m_code_size;
            ++m_original_code_size;
            m_table_capacity *= 2;
        }
        return control_code;
    }

    void reset()
    {
        m_code_table.clear();
        m_code_table.extend(m_original_code_table);
        m_code_size = m_original_code_size;
        m_table_capacity = AK::exp2<u32>(m_code_size);
        m_output.clear();
    }

    ErrorOr<u16> next_code()
    {
        m_current_code = TRY(m_bit_stream->template read_bits<u16>(m_code_size));

        if (m_current_code > m_code_table.size()) {
            dbgln_if(LZW_DEBUG, "Corrupted LZW stream, invalid code: {}, code table size: {}",
                m_current_code,
                m_code_table.size());
            return Error::from_string_literal("Corrupted LZW stream, invalid code");
        } else if (m_current_code == m_code_table.size() && m_output.is_empty()) {
            dbgln_if(LZW_DEBUG, "Corrupted LZW stream, valid new code but output buffer is empty: {}, code table size: {}",
                m_current_code,
                m_code_table.size());
            return Error::from_string_literal("Corrupted LZW stream, valid new code but output buffer is empty");
        }

        return m_current_code;
    }

    Vector<u8>& get_output()
    {
        VERIFY(m_current_code <= m_code_table.size());
        if (m_current_code < m_code_table.size()) {
            Vector<u8> new_entry = m_output;
            m_output = m_code_table.at(m_current_code);
            new_entry.append(m_output[0]);
            extend_code_table(new_entry);
        } else if (m_current_code == m_code_table.size()) {
            VERIFY(!m_output.is_empty());
            m_output.append(m_output[0]);
            extend_code_table(m_output);
        }
        return m_output;
    }

private:
    void init_code_table()
    {
        m_code_table.ensure_capacity(m_table_capacity);
        for (u16 i = 0; i < m_table_capacity; ++i) {
            m_code_table.unchecked_append({ (u8)i });
        }
        m_original_code_table = m_code_table;
    }

    void extend_code_table(Vector<u8> const& entry)
    {
        if (entry.size() > 1 && m_code_table.size() < 4096) {
            m_code_table.append(entry);
            if (m_code_table.size() >= (m_table_capacity + m_offset_for_size_change) && m_code_size < max_code_size) {
                ++m_code_size;
                m_table_capacity *= 2;
            }
        }
    }

    MaybeOwned<InputStream> m_bit_stream;

    Vector<Vector<u8>> m_code_table {};
    Vector<Vector<u8>> m_original_code_table {};

    u8 m_code_size { 0 };
    u8 m_original_code_size { 0 };

    u32 m_table_capacity { 0 };
    i32 m_offset_for_size_change {};

    u16 m_current_code { 0 };
    Vector<u8> m_output {};
};

}
