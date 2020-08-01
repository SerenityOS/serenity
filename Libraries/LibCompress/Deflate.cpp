/*
 * Copyright (c) 2020, the SerenityOS developers
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

#include <AK/Assertions.h>
#include <AK/LogStream.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCompress/Deflate.h>

namespace Compress {

Vector<u8> Deflate::decompress()
{
    bool is_final_block = false;

    do {
        is_final_block = m_reader.read();
        auto block_type = m_reader.read_bits(2);

        switch (block_type) {
        case 0:
            decompress_uncompressed_block();
            break;
        case 1:
            decompress_static_block();
            break;
        case 2:
            decompress_dynamic_block();
            break;
        case 3:
            dbg() << "Block contains reserved block type...";
            ASSERT_NOT_REACHED();
            break;
        default:
            dbg() << "Invalid block type was read...";
            ASSERT_NOT_REACHED();
            break;
        }
    } while (!is_final_block);

    return m_output_buffer;
}

void Deflate::decompress_uncompressed_block()
{
    // Align to the next byte boundary.
    while (m_reader.get_bit_byte_offset() != 0) {
        m_reader.read();
    }

    auto length = m_reader.read_bits(16) & 0xFFFF;
    auto negated_length = m_reader.read_bits(16) & 0xFFFF;

    if ((length ^ 0xFFFF) != negated_length) {
        dbg() << "Block length is invalid...";
        ASSERT_NOT_REACHED();
    }

    for (size_t i = 0; i < length; i++) {
        auto byte = m_reader.read_byte();
        if (byte < 0) {
            dbg() << "Ran out of bytes while reading uncompressed block...";
            ASSERT_NOT_REACHED();
        }

        m_output_buffer.append(byte);
        m_history_buffer.enqueue(byte);
    }
}

void Deflate::decompress_static_block()
{
    decompress_huffman_block(m_literal_length_codes, &m_fixed_distance_codes);
}

void Deflate::decompress_dynamic_block()
{
    auto codes = decode_huffman_codes();
    if (codes.size() == 2) {
        decompress_huffman_block(codes[0], &codes[1]);
    } else {
        decompress_huffman_block(codes[0], nullptr);
    }
}

void Deflate::decompress_huffman_block(CanonicalCode& length_codes, CanonicalCode* distance_codes)
{
    for (;;) {
        u32 symbol = length_codes.next_symbol(m_reader);

        // End of block.
        if (symbol == 256) {
            break;
        }

        // literal byte.
        if (symbol < 256) {
            m_history_buffer.enqueue(symbol);
            m_output_buffer.append(symbol);
            continue;
        }

        // Length and distance for copying.
        ASSERT(distance_codes);

        auto run = decode_run_length(symbol);
        if (run < 3 || run > 258) {
            dbg() << "Invalid run length";
            ASSERT_NOT_REACHED();
        }

        auto distance_symbol = distance_codes->next_symbol(m_reader);
        auto distance = decode_distance(distance_symbol);
        if (distance < 1 || distance > 32768) {
            dbg() << "Invalid distance";
            ASSERT_NOT_REACHED();
        }

        copy_from_history(distance, run);
    }
}

Vector<CanonicalCode> Deflate::decode_huffman_codes()
{
    // FIXME: This path is not tested.
    Vector<CanonicalCode> result;

    auto length_code_count = m_reader.read_bits(5) + 257;
    auto distance_code_count = m_reader.read_bits(5) + 1;

    size_t length_code_code_length = m_reader.read_bits(4) + 4;

    Vector<u8> code_length_code_length;
    code_length_code_length.resize(19);
    code_length_code_length[16] = m_reader.read_bits(3);
    code_length_code_length[17] = m_reader.read_bits(3);
    code_length_code_length[18] = m_reader.read_bits(3);
    code_length_code_length[0] = m_reader.read_bits(3);
    for (size_t i = 0; i < length_code_code_length; i++) {
        auto index = (i % 2 == 0) ? (8 + (i / 2)) : (7 - (i / 2));
        code_length_code_length[index] = m_reader.read_bits(3);
    }

    auto code_length_code = CanonicalCode(code_length_code_length);

    Vector<u32> code_lens;
    code_lens.resize(length_code_count + distance_code_count);

    for (size_t index = 0; index < code_lens.capacity();) {
        auto symbol = code_length_code.next_symbol(m_reader);

        if (symbol <= 15) {
            code_lens[index] = symbol;
            index++;
            continue;
        }

        u32 run_length;
        u32 run_value = 0;

        if (symbol == 16) {
            if (index == 0) {
                dbg() << "No code length value avaliable";
                ASSERT_NOT_REACHED();
            }

            run_length = m_reader.read_bits(2) + 3;
            run_value = code_lens[index - 1];
        } else if (symbol == 17) {
            run_length = m_reader.read_bits(3) + 3;
        } else if (symbol == 18) {
            run_length = m_reader.read_bits(7) + 11;
        } else {
            dbg() << "Code symbol is out of range!";
            ASSERT_NOT_REACHED();
        }

        u32 end = index + run_length;
        if (end > code_lens.capacity()) {
            dbg() << "Code run is out of range!";
            ASSERT_NOT_REACHED();
        }

        memset(code_lens.data() + index, run_value, run_length);
        index = end;
    }

    Vector<u8> literal_codes;
    literal_codes.resize(length_code_count);
    memcpy(literal_codes.data(), code_lens.data(), literal_codes.capacity());
    result.append(CanonicalCode(literal_codes));

    Vector<u8> distance_codes;
    distance_codes.resize(distance_code_count);
    memcpy(distance_codes.data(), code_lens.data() + length_code_count, distance_codes.capacity());

    if (distance_code_count == 1 && distance_codes[0] == 0) {
        return result;
    }

    u8 one_count = 0;
    u8 other_count = 0;

    for (size_t i = 0; i < distance_codes.capacity(); i++) {
        u8 value = distance_codes.at(i);

        if (value == 1) {
            one_count++;
        } else if (value > 1) {
            other_count++;
        }
    }

    if (one_count == 1 && other_count == 0) {
        distance_codes.resize(32);
        distance_codes[31] = 1;
    }

    result.append(CanonicalCode(distance_codes));
    return result;
}

u32 Deflate::decode_run_length(u32 symbol)
{
    if (symbol <= 264) {
        return symbol - 254;
    }

    if (symbol <= 284) {
        auto extra_bits = (symbol - 261) / 4;
        return ((((symbol - 265) % 4) + 4) << extra_bits) + 3 + m_reader.read_bits(extra_bits);
    }

    if (symbol == 285) {
        return 258;
    }

    dbg() << "Found invalid symbol in run length " << symbol;
    ASSERT_NOT_REACHED();
}

u32 Deflate::decode_distance(u32 symbol)
{
    if (symbol <= 3) {
        return symbol + 1;
    }

    if (symbol <= 29) {
        auto extra_bits = (symbol / 2) - 1;
        return (((symbol % 2) + 2) << extra_bits) + 1 + m_reader.read_bits(extra_bits);
    }

    dbg() << "Found invalid symbol in distance" << symbol;
    ASSERT_NOT_REACHED();
}

void Deflate::copy_from_history(u32 distance, u32 run)
{
    auto head_index = (m_history_buffer.head_index() + m_history_buffer.size()) % m_history_buffer.capacity();
    auto read_index = (head_index - distance + m_history_buffer.capacity()) % m_history_buffer.capacity();

    for (size_t i = 0; i < run; i++) {
        auto data = m_history_buffer.at(read_index++);
        m_output_buffer.append(data);
        m_history_buffer.enqueue(data);
    }
}

i8 BitStreamReader::read()
{
    if (m_current_byte == -1) {
        return -1;
    }

    if (m_remaining_bits == 0) {
        if (m_data_index + 1 > m_data.size())
            return -1;

        m_current_byte = m_data.at(m_data_index++);
        m_remaining_bits = 8;
    }

    m_remaining_bits--;
    return (m_current_byte >> (7 - m_remaining_bits)) & 1;
}


i8 BitStreamReader::read_byte()
{
    m_current_byte = 0;
    m_remaining_bits = 0;

    if (m_data_index + 1 > m_data.size())
        return -1;

    return m_data.at(m_data_index++);
}


u8 BitStreamReader::get_bit_byte_offset()
{
    return (8 - m_remaining_bits) % 8;
}

u32 BitStreamReader::read_bits(u8 count)
{
    ASSERT(count > 0 && count < 32);

    u32 result = 0;
    for (size_t i = 0; i < count; i++) {
        result |= read() << i;
    }
    return result;
}

Vector<u8> Deflate::generate_literal_length_codes()
{
    Vector<u8> ll_codes;
    ll_codes.resize(288);
    memset(ll_codes.data() + 0, 8, 144 - 0);
    memset(ll_codes.data() + 144, 9, 256 - 144);
    memset(ll_codes.data() + 256, 7, 280 - 256);
    memset(ll_codes.data() + 280, 8, 288 - 280);
    return ll_codes;
}

Vector<u8> Deflate::generate_fixed_distance_codes()
{
    Vector<u8> fd_codes;
    fd_codes.resize(32);
    memset(fd_codes.data(), 5, 32);
    return fd_codes;
}

CanonicalCode::CanonicalCode(Vector<u8> codes)
{
    m_symbol_codes.resize(codes.size());
    m_symbol_values.resize(codes.size());

    auto allocated_symbols_count = 0;
    auto next_code = 0;

    for (size_t code_length = 1; code_length <= 15; code_length++) {
        next_code <<= 1;
        auto start_bit = 1 << code_length;

        for (size_t symbol = 0; symbol < codes.size(); symbol++) {
            if (codes.at(symbol) != code_length) {
                continue;
            }

            if (next_code > start_bit) {
                dbg() << "Canonical code overflows the huffman tree";
                ASSERT_NOT_REACHED();
            }

            m_symbol_codes[allocated_symbols_count] = start_bit | next_code;
            m_symbol_values[allocated_symbols_count] = symbol;

            allocated_symbols_count++;
            next_code++;
        }
    }

    if (next_code != (1 << 15)) {
        dbg() << "Canonical code underflows the huffman tree " << next_code;
        ASSERT_NOT_REACHED();
    }
}

i32 binary_search(Vector<u32>& heystack, u32 needle)
{
    i32 low = 0;
    i32 high = heystack.size();

    while (low <= high) {
        u32 mid = (low + high) >> 1;
        u32 value = heystack.at(mid);

        if (value < needle) {
            low = mid + 1;
        } else if (value > needle) {
            high = mid - 1;
        } else {
            return mid;
        }
    }

    return -1;
}

u32 CanonicalCode::next_symbol(BitStreamReader& reader)
{
    auto code_bits = 1;

    for (;;) {
        code_bits = code_bits << 1 | reader.read();
        i32 index = binary_search(m_symbol_codes, code_bits);
        if (index >= 0) {
            return m_symbol_values.at(index);
        }
    }
}
}
