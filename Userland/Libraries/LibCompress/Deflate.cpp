/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/BinaryHeap.h>
#include <AK/BinarySearch.h>
#include <AK/MemoryStream.h>
#include <string.h>

#include <LibCompress/Deflate.h>

namespace Compress {

static constexpr u8 deflate_special_code_length_copy = 16;
static constexpr u8 deflate_special_code_length_zeros = 17;
static constexpr u8 deflate_special_code_length_long_zeros = 18;

CanonicalCode const& CanonicalCode::fixed_literal_codes()
{
    static CanonicalCode code;
    static bool initialized = false;

    if (initialized)
        return code;

    code = CanonicalCode::from_bytes(fixed_literal_bit_lengths).value();
    initialized = true;

    return code;
}

CanonicalCode const& CanonicalCode::fixed_distance_codes()
{
    static CanonicalCode code;
    static bool initialized = false;

    if (initialized)
        return code;

    code = CanonicalCode::from_bytes(fixed_distance_bit_lengths).value();
    initialized = true;

    return code;
}

Optional<CanonicalCode> CanonicalCode::from_bytes(ReadonlyBytes bytes)
{
    // FIXME: I can't quite follow the algorithm here, but it seems to work.

    CanonicalCode code;

    auto non_zero_symbols = 0;
    auto last_non_zero = -1;
    for (size_t i = 0; i < bytes.size(); i++) {
        if (bytes[i] != 0) {
            non_zero_symbols++;
            last_non_zero = i;
        }
    }
    if (non_zero_symbols == 1) { // special case - only 1 symbol
        code.m_symbol_codes.append(0b10);
        code.m_symbol_values.append(last_non_zero);
        code.m_bit_codes[last_non_zero] = 0;
        code.m_bit_code_lengths[last_non_zero] = 1;
        return code;
    }

    auto next_code = 0;
    for (size_t code_length = 1; code_length <= 15; ++code_length) {
        next_code <<= 1;
        auto start_bit = 1 << code_length;

        for (size_t symbol = 0; symbol < bytes.size(); ++symbol) {
            if (bytes[symbol] != code_length)
                continue;

            if (next_code > start_bit)
                return {};

            code.m_symbol_codes.append(start_bit | next_code);
            code.m_symbol_values.append(symbol);
            code.m_bit_codes[symbol] = fast_reverse16(start_bit | next_code, code_length); // DEFLATE writes huffman encoded symbols as lsb-first
            code.m_bit_code_lengths[symbol] = code_length;

            next_code++;
        }
    }

    if (next_code != (1 << 15)) {
        return {};
    }

    return code;
}

u32 CanonicalCode::read_symbol(InputBitStream& stream) const
{
    u32 code_bits = 1;

    for (;;) {
        code_bits = code_bits << 1 | stream.read_bits(1);
        if (code_bits >= (1 << 16))
            return UINT32_MAX; // the maximum symbol in deflate is 288, so we use UINT32_MAX (an impossible value) to indicate an error

        // FIXME: This is very inefficient and could greatly be improved by implementing this
        //        algorithm: https://www.hanshq.net/zip.html#huffdec
        size_t index;
        if (binary_search(m_symbol_codes.span(), code_bits, &index))
            return m_symbol_values[index];
    }
}

void CanonicalCode::write_symbol(OutputBitStream& stream, u32 symbol) const
{
    stream.write_bits(m_bit_codes[symbol], m_bit_code_lengths[symbol]);
}

DeflateDecompressor::CompressedBlock::CompressedBlock(DeflateDecompressor& decompressor, CanonicalCode literal_codes, Optional<CanonicalCode> distance_codes)
    : m_decompressor(decompressor)
    , m_literal_codes(literal_codes)
    , m_distance_codes(distance_codes)
{
}

bool DeflateDecompressor::CompressedBlock::try_read_more()
{
    if (m_eof == true)
        return false;

    auto const symbol = m_literal_codes.read_symbol(m_decompressor.m_input_stream);

    if (symbol >= 286) { // invalid deflate literal/length symbol
        m_decompressor.set_fatal_error();
        return false;
    }

    if (symbol < 256) {
        m_decompressor.m_output_stream << static_cast<u8>(symbol);
        return true;
    } else if (symbol == 256) {
        m_eof = true;
        return false;
    } else {
        if (!m_distance_codes.has_value()) {
            m_decompressor.set_fatal_error();
            return false;
        }

        auto const length = m_decompressor.decode_length(symbol);
        auto const distance_symbol = m_distance_codes.value().read_symbol(m_decompressor.m_input_stream);
        if (distance_symbol >= 30) { // invalid deflate distance symbol
            m_decompressor.set_fatal_error();
            return false;
        }
        auto const distance = m_decompressor.decode_distance(distance_symbol);

        for (size_t idx = 0; idx < length; ++idx) {
            u8 byte = 0;
            m_decompressor.m_output_stream.read({ &byte, sizeof(byte) }, distance);
            if (m_decompressor.m_output_stream.handle_any_error()) {
                m_decompressor.set_fatal_error();
                return false; // a back reference was requested that was too far back (outside our current sliding window)
            }
            m_decompressor.m_output_stream << byte;
        }

        return true;
    }
}

DeflateDecompressor::UncompressedBlock::UncompressedBlock(DeflateDecompressor& decompressor, size_t length)
    : m_decompressor(decompressor)
    , m_bytes_remaining(length)
{
}

bool DeflateDecompressor::UncompressedBlock::try_read_more()
{
    if (m_bytes_remaining == 0)
        return false;

    auto const nread = min(m_bytes_remaining, m_decompressor.m_output_stream.remaining_contiguous_space());
    m_bytes_remaining -= nread;

    m_decompressor.m_input_stream >> m_decompressor.m_output_stream.reserve_contiguous_space(nread);

    return true;
}

DeflateDecompressor::DeflateDecompressor(InputStream& stream)
    : m_input_stream(stream)
{
}

DeflateDecompressor::~DeflateDecompressor()
{
    if (m_state == State::ReadingCompressedBlock)
        m_compressed_block.~CompressedBlock();
    if (m_state == State::ReadingUncompressedBlock)
        m_uncompressed_block.~UncompressedBlock();
}

size_t DeflateDecompressor::read(Bytes bytes)
{
    size_t total_read = 0;
    while (total_read < bytes.size()) {
        if (has_any_error())
            break;

        auto slice = bytes.slice(total_read);

        if (m_state == State::Idle) {
            if (m_read_final_bock)
                break;

            m_read_final_bock = m_input_stream.read_bit();
            auto const block_type = m_input_stream.read_bits(2);

            if (m_input_stream.has_any_error()) {
                set_fatal_error();
                break;
            }

            if (block_type == 0b00) {
                m_input_stream.align_to_byte_boundary();

                LittleEndian<u16> length, negated_length;
                m_input_stream >> length >> negated_length;

                if (m_input_stream.has_any_error()) {
                    set_fatal_error();
                    break;
                }

                if ((length ^ 0xffff) != negated_length) {
                    set_fatal_error();
                    break;
                }

                m_state = State::ReadingUncompressedBlock;
                new (&m_uncompressed_block) UncompressedBlock(*this, length);

                continue;
            }

            if (block_type == 0b01) {
                m_state = State::ReadingCompressedBlock;
                new (&m_compressed_block) CompressedBlock(*this, CanonicalCode::fixed_literal_codes(), CanonicalCode::fixed_distance_codes());

                continue;
            }

            if (block_type == 0b10) {
                CanonicalCode literal_codes;
                Optional<CanonicalCode> distance_codes;
                decode_codes(literal_codes, distance_codes);

                if (m_input_stream.has_any_error()) {
                    set_fatal_error();
                    break;
                }

                m_state = State::ReadingCompressedBlock;
                new (&m_compressed_block) CompressedBlock(*this, literal_codes, distance_codes);

                continue;
            }

            set_fatal_error();
            break;
        }

        if (m_state == State::ReadingCompressedBlock) {
            auto nread = m_output_stream.read(slice);

            while (nread < slice.size() && m_compressed_block.try_read_more()) {
                nread += m_output_stream.read(slice.slice(nread));
            }

            if (m_input_stream.has_any_error()) {
                set_fatal_error();
                break;
            }

            total_read += nread;
            if (nread == slice.size())
                break;

            m_compressed_block.~CompressedBlock();
            m_state = State::Idle;

            continue;
        }

        if (m_state == State::ReadingUncompressedBlock) {
            auto nread = m_output_stream.read(slice);

            while (nread < slice.size() && m_uncompressed_block.try_read_more()) {
                nread += m_output_stream.read(slice.slice(nread));
            }

            if (m_input_stream.has_any_error()) {
                set_fatal_error();
                break;
            }

            total_read += nread;
            if (nread == slice.size())
                break;

            m_uncompressed_block.~UncompressedBlock();
            m_state = State::Idle;

            continue;
        }

        VERIFY_NOT_REACHED();
    }
    return total_read;
}

bool DeflateDecompressor::read_or_error(Bytes bytes)
{
    if (read(bytes) < bytes.size()) {
        set_fatal_error();
        return false;
    }

    return true;
}

bool DeflateDecompressor::discard_or_error(size_t count)
{
    u8 buffer[4096];

    size_t ndiscarded = 0;
    while (ndiscarded < count) {
        if (unreliable_eof()) {
            set_fatal_error();
            return false;
        }

        ndiscarded += read({ buffer, min<size_t>(count - ndiscarded, 4096) });
    }

    return true;
}

bool DeflateDecompressor::unreliable_eof() const { return m_state == State::Idle && m_read_final_bock; }

bool DeflateDecompressor::handle_any_error()
{
    bool handled_errors = m_input_stream.handle_any_error();
    return Stream::handle_any_error() || handled_errors;
}

Optional<ByteBuffer> DeflateDecompressor::decompress_all(ReadonlyBytes bytes)
{
    InputMemoryStream memory_stream { bytes };
    DeflateDecompressor deflate_stream { memory_stream };
    DuplexMemoryStream output_stream;

    u8 buffer[4096];
    while (!deflate_stream.has_any_error() && !deflate_stream.unreliable_eof()) {
        auto const nread = deflate_stream.read({ buffer, sizeof(buffer) });
        output_stream.write_or_error({ buffer, nread });
    }

    if (deflate_stream.handle_any_error())
        return {};

    return output_stream.copy_into_contiguous_buffer();
}

u32 DeflateDecompressor::decode_length(u32 symbol)
{
    // FIXME: I can't quite follow the algorithm here, but it seems to work.

    if (symbol <= 264)
        return symbol - 254;

    if (symbol <= 284) {
        auto extra_bits = (symbol - 261) / 4;
        return (((symbol - 265) % 4 + 4) << extra_bits) + 3 + m_input_stream.read_bits(extra_bits);
    }

    if (symbol == 285)
        return 258;

    VERIFY_NOT_REACHED();
}

u32 DeflateDecompressor::decode_distance(u32 symbol)
{
    // FIXME: I can't quite follow the algorithm here, but it seems to work.

    if (symbol <= 3)
        return symbol + 1;

    if (symbol <= 29) {
        auto extra_bits = (symbol / 2) - 1;
        return ((symbol % 2 + 2) << extra_bits) + 1 + m_input_stream.read_bits(extra_bits);
    }

    VERIFY_NOT_REACHED();
}

void DeflateDecompressor::decode_codes(CanonicalCode& literal_code, Optional<CanonicalCode>& distance_code)
{
    auto literal_code_count = m_input_stream.read_bits(5) + 257;
    auto distance_code_count = m_input_stream.read_bits(5) + 1;
    auto code_length_count = m_input_stream.read_bits(4) + 4;

    // First we have to extract the code lengths of the code that was used to encode the code lengths of
    // the code that was used to encode the block.

    u8 code_lengths_code_lengths[19] = { 0 };
    for (size_t i = 0; i < code_length_count; ++i) {
        code_lengths_code_lengths[code_lengths_code_lengths_order[i]] = m_input_stream.read_bits(3);
    }

    // Now we can extract the code that was used to encode the code lengths of the code that was used to
    // encode the block.

    auto code_length_code_result = CanonicalCode::from_bytes({ code_lengths_code_lengths, sizeof(code_lengths_code_lengths) });
    if (!code_length_code_result.has_value()) {
        set_fatal_error();
        return;
    }
    auto const code_length_code = code_length_code_result.value();

    // Next we extract the code lengths of the code that was used to encode the block.

    Vector<u8> code_lengths;
    while (code_lengths.size() < literal_code_count + distance_code_count) {
        auto symbol = code_length_code.read_symbol(m_input_stream);

        if (symbol == UINT32_MAX) {
            set_fatal_error();
            return;
        }

        if (symbol < deflate_special_code_length_copy) {
            code_lengths.append(static_cast<u8>(symbol));
            continue;
        } else if (symbol == deflate_special_code_length_zeros) {
            auto nrepeat = 3 + m_input_stream.read_bits(3);
            for (size_t j = 0; j < nrepeat; ++j)
                code_lengths.append(0);
            continue;
        } else if (symbol == deflate_special_code_length_long_zeros) {
            auto nrepeat = 11 + m_input_stream.read_bits(7);
            for (size_t j = 0; j < nrepeat; ++j)
                code_lengths.append(0);
            continue;
        } else {
            VERIFY(symbol == deflate_special_code_length_copy);

            if (code_lengths.is_empty()) {
                set_fatal_error();
                return;
            }

            auto nrepeat = 3 + m_input_stream.read_bits(2);
            for (size_t j = 0; j < nrepeat; ++j)
                code_lengths.append(code_lengths.last());
        }
    }

    if (code_lengths.size() != literal_code_count + distance_code_count) {
        set_fatal_error();
        return;
    }

    // Now we extract the code that was used to encode literals and lengths in the block.

    auto literal_code_result = CanonicalCode::from_bytes(code_lengths.span().trim(literal_code_count));
    if (!literal_code_result.has_value()) {
        set_fatal_error();
        return;
    }
    literal_code = literal_code_result.value();

    // Now we extract the code that was used to encode distances in the block.

    if (distance_code_count == 1) {
        auto length = code_lengths[literal_code_count];

        if (length == 0) {
            return;
        } else if (length != 1) {
            set_fatal_error();
            return;
        }
    }

    auto distance_code_result = CanonicalCode::from_bytes(code_lengths.span().slice(literal_code_count));
    if (!distance_code_result.has_value()) {
        set_fatal_error();
        return;
    }
    distance_code = distance_code_result.value();
}

DeflateCompressor::DeflateCompressor(OutputStream& stream, CompressionLevel compression_level)
    : m_compression_level(compression_level)
    , m_compression_constants(compression_constants[static_cast<int>(m_compression_level)])
    , m_output_stream(stream)
{
    m_symbol_frequencies.fill(0);
    m_distance_frequencies.fill(0);
}

DeflateCompressor::~DeflateCompressor()
{
    VERIFY(m_finished);
}

size_t DeflateCompressor::write(ReadonlyBytes bytes)
{
    VERIFY(!m_finished);

    if (bytes.size() == 0)
        return 0; // recursion base case

    auto n_written = bytes.copy_trimmed_to(pending_block().slice(m_pending_block_size));
    m_pending_block_size += n_written;

    if (m_pending_block_size == block_size)
        flush();

    return n_written + write(bytes.slice(n_written));
}

bool DeflateCompressor::write_or_error(ReadonlyBytes bytes)
{
    if (write(bytes) < bytes.size()) {
        set_fatal_error();
        return false;
    }

    return true;
}

// Knuth's multiplicative hash on 4 bytes
u16 DeflateCompressor::hash_sequence(u8 const* bytes)
{
    constexpr const u32 knuth_constant = 2654435761; // shares no common factors with 2^32
    return ((bytes[0] | bytes[1] << 8 | bytes[2] << 16 | bytes[3] << 24) * knuth_constant) >> (32 - hash_bits);
}

size_t DeflateCompressor::compare_match_candidate(size_t start, size_t candidate, size_t previous_match_length, size_t maximum_match_length)
{
    VERIFY(previous_match_length < maximum_match_length);

    // We firstly check that the match is at least (prev_match_length + 1) long, we check backwards as there's a higher chance the end mismatches
    for (ssize_t i = previous_match_length; i >= 0; i--) {
        if (m_rolling_window[start + i] != m_rolling_window[candidate + i])
            return 0;
    }

    // Find the actual length
    auto match_length = previous_match_length + 1;
    while (match_length < maximum_match_length && m_rolling_window[start + match_length] == m_rolling_window[candidate + match_length]) {
        match_length++;
    }

    VERIFY(match_length > previous_match_length);
    VERIFY(match_length <= maximum_match_length);
    return match_length;
}

size_t DeflateCompressor::find_back_match(size_t start, u16 hash, size_t previous_match_length, size_t maximum_match_length, size_t& match_position)
{
    auto max_chain_length = m_compression_constants.max_chain;
    if (previous_match_length == 0)
        previous_match_length = min_match_length - 1; // we only care about matches that are at least min_match_length long
    if (previous_match_length >= maximum_match_length)
        return 0; // we can't improve a maximum length match
    if (previous_match_length >= m_compression_constants.max_lazy_length)
        return 0; // the previous match is already pretty, we shouldn't waste another full search
    if (previous_match_length >= m_compression_constants.good_match_length)
        max_chain_length /= 4; // we already have a pretty good much, so do a shorter search

    auto candidate = m_hash_head[hash];
    auto match_found = false;
    while (max_chain_length--) {
        if (candidate == empty_slot)
            break; // no remaining candidates

        VERIFY(candidate < start);
        if (start - candidate > window_size)
            break; // outside the window

        auto match_length = compare_match_candidate(start, candidate, previous_match_length, maximum_match_length);

        if (match_length != 0) {
            match_found = true;
            match_position = candidate;
            previous_match_length = match_length;

            if (match_length == maximum_match_length)
                return match_length; // bail if we got the maximum possible length
        }

        candidate = m_hash_prev[candidate % window_size];
    }
    if (!match_found)
        return 0;                 // we didn't find any matches
    return previous_match_length; // we found matches, but they were at most previous_match_length long
}

ALWAYS_INLINE u8 DeflateCompressor::distance_to_base(u16 distance)
{
    return (distance <= 256) ? distance_to_base_lo[distance - 1] : distance_to_base_hi[(distance - 1) >> 7];
}

template<size_t Size>
void DeflateCompressor::generate_huffman_lengths(Array<u8, Size>& lengths, Array<u16, Size> const& frequencies, size_t max_bit_length, u16 frequency_cap)
{
    VERIFY((1u << max_bit_length) >= Size);
    u16 heap_keys[Size]; // Used for O(n) heap construction
    u16 heap_values[Size];

    u16 huffman_links[Size * 2 + 1] = { 0 };
    size_t non_zero_freqs = 0;
    for (size_t i = 0; i < Size; i++) {
        auto frequency = frequencies[i];
        if (frequency == 0)
            continue;

        if (frequency > frequency_cap) {
            frequency = frequency_cap;
        }

        heap_keys[non_zero_freqs] = frequency;               // sort symbols by frequency
        heap_values[non_zero_freqs] = Size + non_zero_freqs; // huffman_links "links"
        non_zero_freqs++;
    }

    // special case for only 1 used symbol
    if (non_zero_freqs < 2) {
        for (size_t i = 0; i < Size; i++)
            lengths[i] = (frequencies[i] == 0) ? 0 : 1;
        return;
    }

    BinaryHeap<u16, u16, Size> heap { heap_keys, heap_values, non_zero_freqs };

    // build the huffman tree - binary heap is used for efficient frequency comparisons
    while (heap.size() > 1) {
        u16 lowest_frequency = heap.peek_min_key();
        u16 lowest_link = heap.pop_min();
        u16 second_lowest_frequency = heap.peek_min_key();
        u16 second_lowest_link = heap.pop_min();

        u16 new_link = heap.size() + 2;

        heap.insert(lowest_frequency + second_lowest_frequency, new_link);

        huffman_links[lowest_link] = new_link;
        huffman_links[second_lowest_link] = new_link;
    }

    non_zero_freqs = 0;
    for (size_t i = 0; i < Size; i++) {
        if (frequencies[i] == 0) {
            lengths[i] = 0;
            continue;
        }

        u16 link = huffman_links[Size + non_zero_freqs];
        non_zero_freqs++;

        size_t bit_length = 1;
        while (link != 2) {
            bit_length++;
            link = huffman_links[link];
        }

        if (bit_length > max_bit_length) {
            VERIFY(frequency_cap != 1);
            return generate_huffman_lengths(lengths, frequencies, max_bit_length, frequency_cap / 2);
        }

        lengths[i] = bit_length;
    }
}

void DeflateCompressor::lz77_compress_block()
{
    for (auto& slot : m_hash_head) { // initialize chained hash table
        slot = empty_slot;
    }

    auto insert_hash = [&](auto pos, auto hash) {
        auto window_pos = pos % window_size;
        m_hash_prev[window_pos] = m_hash_head[hash];
        m_hash_head[hash] = window_pos;
    };

    auto emit_literal = [&](auto literal) {
        VERIFY(m_pending_symbol_size <= block_size + 1);
        auto index = m_pending_symbol_size++;
        m_symbol_buffer[index].distance = 0;
        m_symbol_buffer[index].literal = literal;
        m_symbol_frequencies[literal]++;
    };

    auto emit_back_reference = [&](auto distance, auto length) {
        VERIFY(m_pending_symbol_size <= block_size + 1);
        auto index = m_pending_symbol_size++;
        m_symbol_buffer[index].distance = distance;
        m_symbol_buffer[index].length = length;
        m_symbol_frequencies[length_to_symbol[length]]++;
        m_distance_frequencies[distance_to_base(distance)]++;
    };

    size_t previous_match_length = 0;
    size_t previous_match_position = 0;

    VERIFY(m_compression_constants.great_match_length <= max_match_length);

    // our block starts at block_size and is m_pending_block_size in length
    auto block_end = block_size + m_pending_block_size;
    size_t current_position;
    for (current_position = block_size; current_position < block_end - min_match_length + 1; current_position++) {
        auto hash = hash_sequence(&m_rolling_window[current_position]);
        size_t match_position;
        auto match_length = find_back_match(current_position, hash, previous_match_length,
            min(m_compression_constants.great_match_length, block_end - current_position), match_position);

        insert_hash(current_position, hash);

        // if the previous match is as good as the new match, just use it
        if (previous_match_length != 0 && previous_match_length >= match_length) {
            emit_back_reference((current_position - 1) - previous_match_position, previous_match_length);

            // skip all the bytes that are included in this match
            for (size_t j = current_position + 1; j < min(current_position - 1 + previous_match_length, block_end - min_match_length + 1); j++) {
                insert_hash(j, hash_sequence(&m_rolling_window[j]));
            }
            current_position = (current_position - 1) + previous_match_length - 1;
            previous_match_length = 0;
            continue;
        }

        if (match_length == 0) {
            VERIFY(previous_match_length == 0);
            emit_literal(m_rolling_window[current_position]);
            continue;
        }

        // if this is a lazy match, and the new match is better than the old one, output previous as literal
        if (previous_match_length != 0) {
            emit_literal(m_rolling_window[current_position - 1]);
        }

        previous_match_length = match_length;
        previous_match_position = match_position;
    }

    // clean up leftover lazy match
    if (previous_match_length != 0) {
        emit_back_reference((current_position - 1) - previous_match_position, previous_match_length);
        current_position = (current_position - 1) + previous_match_length;
    }

    // output remaining literals
    while (current_position < block_end) {
        emit_literal(m_rolling_window[current_position++]);
    }
}

size_t DeflateCompressor::huffman_block_length(Array<u8, max_huffman_literals> const& literal_bit_lengths, Array<u8, max_huffman_distances> const& distance_bit_lengths)
{
    size_t length = 0;

    for (size_t i = 0; i < 286; i++) {
        auto frequency = m_symbol_frequencies[i];
        length += literal_bit_lengths[i] * frequency;

        if (i >= 257) // back reference length symbols
            length += packed_length_symbols[i - 257].extra_bits * frequency;
    }

    for (size_t i = 0; i < 30; i++) {
        auto frequency = m_distance_frequencies[i];
        length += distance_bit_lengths[i] * frequency;
        length += packed_distances[i].extra_bits * frequency;
    }

    return length;
}

size_t DeflateCompressor::uncompressed_block_length()
{
    auto padding = 8 - ((m_output_stream.bit_offset() + 3) % 8);
    // 3 bit block header + align to byte + 2 * 16 bit length fields + block contents
    return 3 + padding + (2 * 16) + m_pending_block_size * 8;
}

size_t DeflateCompressor::fixed_block_length()
{
    // block header + fixed huffman encoded block contents
    return 3 + huffman_block_length(fixed_literal_bit_lengths, fixed_distance_bit_lengths);
}

size_t DeflateCompressor::dynamic_block_length(Array<u8, max_huffman_literals> const& literal_bit_lengths, Array<u8, max_huffman_distances> const& distance_bit_lengths, Array<u8, 19> const& code_lengths_bit_lengths, Array<u16, 19> const& code_lengths_frequencies, size_t code_lengths_count)
{
    // block header + literal code count + distance code count + code length count
    auto length = 3 + 5 + 5 + 4;

    // 3 bits per code_length
    length += 3 * code_lengths_count;

    for (size_t i = 0; i < code_lengths_frequencies.size(); i++) {
        auto frequency = code_lengths_frequencies[i];
        length += code_lengths_bit_lengths[i] * frequency;

        if (i == deflate_special_code_length_copy) {
            length += 2 * frequency;
        } else if (i == deflate_special_code_length_zeros) {
            length += 3 * frequency;
        } else if (i == deflate_special_code_length_long_zeros) {
            length += 7 * frequency;
        }
    }

    return length + huffman_block_length(literal_bit_lengths, distance_bit_lengths);
}

void DeflateCompressor::write_huffman(CanonicalCode const& literal_code, Optional<CanonicalCode> const& distance_code)
{
    auto has_distances = distance_code.has_value();
    for (size_t i = 0; i < m_pending_symbol_size; i++) {
        if (m_symbol_buffer[i].distance == 0) {
            literal_code.write_symbol(m_output_stream, m_symbol_buffer[i].literal);
            continue;
        }
        VERIFY(has_distances);
        auto symbol = length_to_symbol[m_symbol_buffer[i].length];
        literal_code.write_symbol(m_output_stream, symbol);
        // Emit extra bits if needed
        m_output_stream.write_bits(m_symbol_buffer[i].length - packed_length_symbols[symbol - 257].base_length, packed_length_symbols[symbol - 257].extra_bits);

        auto base_distance = distance_to_base(m_symbol_buffer[i].distance);
        distance_code.value().write_symbol(m_output_stream, base_distance);
        // Emit extra bits if needed
        m_output_stream.write_bits(m_symbol_buffer[i].distance - packed_distances[base_distance].base_distance, packed_distances[base_distance].extra_bits);
    }
}

size_t DeflateCompressor::encode_huffman_lengths(Array<u8, max_huffman_literals + max_huffman_distances> const& lengths, size_t lengths_count, Array<code_length_symbol, max_huffman_literals + max_huffman_distances>& encoded_lengths)
{
    size_t encoded_count = 0;
    size_t i = 0;
    while (i < lengths_count) {
        if (lengths[i] == 0) {
            auto zero_count = 0;
            for (size_t j = i; j < min(lengths_count, i + 138) && lengths[j] == 0; j++)
                zero_count++;

            if (zero_count < 3) { // below minimum repeated zero count
                encoded_lengths[encoded_count++].symbol = 0;
                i++;
                continue;
            }

            if (zero_count <= 10) {
                encoded_lengths[encoded_count].symbol = deflate_special_code_length_zeros;
                encoded_lengths[encoded_count++].count = zero_count;
            } else {
                encoded_lengths[encoded_count].symbol = deflate_special_code_length_long_zeros;
                encoded_lengths[encoded_count++].count = zero_count;
            }
            i += zero_count;
            continue;
        }

        encoded_lengths[encoded_count++].symbol = lengths[i++];

        auto copy_count = 0;
        for (size_t j = i; j < min(lengths_count, i + 6) && lengths[j] == lengths[i - 1]; j++)
            copy_count++;

        if (copy_count >= 3) {
            encoded_lengths[encoded_count].symbol = deflate_special_code_length_copy;
            encoded_lengths[encoded_count++].count = copy_count;
            i += copy_count;
            continue;
        }
    }
    return encoded_count;
}

size_t DeflateCompressor::encode_block_lengths(Array<u8, max_huffman_literals> const& literal_bit_lengths, Array<u8, max_huffman_distances> const& distance_bit_lengths, Array<code_length_symbol, max_huffman_literals + max_huffman_distances>& encoded_lengths, size_t& literal_code_count, size_t& distance_code_count)
{
    literal_code_count = max_huffman_literals;
    distance_code_count = max_huffman_distances;

    VERIFY(literal_bit_lengths[256] != 0); // Make sure at least the EndOfBlock marker is present
    while (literal_bit_lengths[literal_code_count - 1] == 0)
        literal_code_count--;

    // Drop trailing zero lengths, keeping at least one
    while (distance_bit_lengths[distance_code_count - 1] == 0 && distance_code_count > 1)
        distance_code_count--;

    Array<u8, max_huffman_literals + max_huffman_distances> all_lengths {};
    size_t lengths_count = 0;
    for (size_t i = 0; i < literal_code_count; i++) {
        all_lengths[lengths_count++] = literal_bit_lengths[i];
    }
    for (size_t i = 0; i < distance_code_count; i++) {
        all_lengths[lengths_count++] = distance_bit_lengths[i];
    }

    return encode_huffman_lengths(all_lengths, lengths_count, encoded_lengths);
}

void DeflateCompressor::write_dynamic_huffman(CanonicalCode const& literal_code, size_t literal_code_count, Optional<CanonicalCode> const& distance_code, size_t distance_code_count, Array<u8, 19> const& code_lengths_bit_lengths, size_t code_length_count, Array<code_length_symbol, max_huffman_literals + max_huffman_distances> const& encoded_lengths, size_t encoded_lengths_count)
{
    m_output_stream.write_bits(literal_code_count - 257, 5);
    m_output_stream.write_bits(distance_code_count - 1, 5);
    m_output_stream.write_bits(code_length_count - 4, 4);

    for (size_t i = 0; i < code_length_count; i++) {
        m_output_stream.write_bits(code_lengths_bit_lengths[code_lengths_code_lengths_order[i]], 3);
    }

    auto code_lengths_code = CanonicalCode::from_bytes(code_lengths_bit_lengths);
    VERIFY(code_lengths_code.has_value());
    for (size_t i = 0; i < encoded_lengths_count; i++) {
        auto encoded_length = encoded_lengths[i];
        code_lengths_code->write_symbol(m_output_stream, encoded_length.symbol);
        if (encoded_length.symbol == deflate_special_code_length_copy) {
            m_output_stream.write_bits(encoded_length.count - 3, 2);
        } else if (encoded_length.symbol == deflate_special_code_length_zeros) {
            m_output_stream.write_bits(encoded_length.count - 3, 3);
        } else if (encoded_length.symbol == deflate_special_code_length_long_zeros) {
            m_output_stream.write_bits(encoded_length.count - 11, 7);
        }
    }

    write_huffman(literal_code, distance_code);
}

void DeflateCompressor::flush()
{
    if (m_output_stream.handle_any_error()) {
        set_fatal_error();
        return;
    }

    m_output_stream.write_bit(m_finished);

    // if this is just an empty block to signify the end of the deflate stream use the smallest block possible (10 bits total)
    if (m_pending_block_size == 0) {
        VERIFY(m_finished);                       // we shouldn't be writing empty blocks unless this is the final one
        m_output_stream.write_bits(0b01, 2);      // fixed huffman codes
        m_output_stream.write_bits(0b0000000, 7); // end of block symbol
        m_output_stream.align_to_byte_boundary();
        return;
    }

    auto write_uncompressed = [&]() {
        m_output_stream.write_bits(0b00, 2); // no compression
        m_output_stream.align_to_byte_boundary();
        LittleEndian<u16> len = m_pending_block_size;
        m_output_stream << len;
        LittleEndian<u16> nlen = ~m_pending_block_size;
        m_output_stream << nlen;
        m_output_stream.write_or_error(pending_block().slice(0, m_pending_block_size));
    };

    if (m_compression_level == CompressionLevel::STORE) { // disabled compression fast path
        write_uncompressed();
        m_pending_block_size = 0;
        return;
    }

    // The following implementation of lz77 compression and huffman encoding is based on the reference implementation by Hans Wennborg https://www.hanshq.net/zip.html

    // this reads from the pending block and writes to m_symbol_buffer
    lz77_compress_block();

    // insert EndOfBlock marker to the symbol buffer
    m_symbol_buffer[m_pending_symbol_size].distance = 0;
    m_symbol_buffer[m_pending_symbol_size++].literal = 256;
    m_symbol_frequencies[256]++;

    // generate optimal dynamic huffman code lengths
    Array<u8, max_huffman_literals> dynamic_literal_bit_lengths {};
    Array<u8, max_huffman_distances> dynamic_distance_bit_lengths {};
    generate_huffman_lengths(dynamic_literal_bit_lengths, m_symbol_frequencies, 15); // deflate data huffman can use up to 15 bits per symbol
    generate_huffman_lengths(dynamic_distance_bit_lengths, m_distance_frequencies, 15);

    // encode literal and distance lengths together in deflate format
    Array<code_length_symbol, max_huffman_literals + max_huffman_distances> encoded_lengths {};
    size_t literal_code_count;
    size_t distance_code_count;
    auto encoded_lengths_count = encode_block_lengths(dynamic_literal_bit_lengths, dynamic_distance_bit_lengths, encoded_lengths, literal_code_count, distance_code_count);

    // count code length frequencies
    Array<u16, 19> code_lengths_frequencies { 0 };
    for (size_t i = 0; i < encoded_lengths_count; i++) {
        code_lengths_frequencies[encoded_lengths[i].symbol]++;
    }
    // generate optimal huffman code lengths code lengths
    Array<u8, 19> code_lengths_bit_lengths {};
    generate_huffman_lengths(code_lengths_bit_lengths, code_lengths_frequencies, 7); // deflate code length huffman can use up to 7 bits per symbol
    // calculate actual code length code lengths count (without trailing zeros)
    auto code_lengths_count = code_lengths_bit_lengths.size();
    while (code_lengths_bit_lengths[code_lengths_code_lengths_order[code_lengths_count - 1]] == 0)
        code_lengths_count--;

    auto uncompressed_size = uncompressed_block_length();
    auto fixed_huffman_size = fixed_block_length();
    auto dynamic_huffman_size = dynamic_block_length(dynamic_literal_bit_lengths, dynamic_distance_bit_lengths, code_lengths_bit_lengths, code_lengths_frequencies, code_lengths_count);

    // If the compression somehow didn't reduce the size enough, just write out the block uncompressed as it allows for much faster decompression
    if (uncompressed_size <= min(fixed_huffman_size, dynamic_huffman_size)) {
        write_uncompressed();
    } else if (fixed_huffman_size <= dynamic_huffman_size) { // If the fixed and dynamic huffman codes come out the same size, prefer the fixed version, as it takes less time to decode
        m_output_stream.write_bits(0b01, 2);                 // fixed huffman codes
        write_huffman(CanonicalCode::fixed_literal_codes(), CanonicalCode::fixed_distance_codes());
    } else {
        m_output_stream.write_bits(0b10, 2); // dynamic huffman codes
        auto literal_code = CanonicalCode::from_bytes(dynamic_literal_bit_lengths);
        VERIFY(literal_code.has_value());
        auto distance_code = CanonicalCode::from_bytes(dynamic_distance_bit_lengths);
        write_dynamic_huffman(literal_code.value(), literal_code_count, distance_code, distance_code_count, code_lengths_bit_lengths, code_lengths_count, encoded_lengths, encoded_lengths_count);
    }
    if (m_finished)
        m_output_stream.align_to_byte_boundary();

    // reset all block specific members
    m_pending_block_size = 0;
    m_pending_symbol_size = 0;
    m_symbol_frequencies.fill(0);
    m_distance_frequencies.fill(0);
    // On the final block this copy will potentially produce an invalid search window, but since its the final block we dont care
    pending_block().copy_trimmed_to({ m_rolling_window, block_size });
}

void DeflateCompressor::final_flush()
{
    VERIFY(!m_finished);
    m_finished = true;
    flush();
}

Optional<ByteBuffer> DeflateCompressor::compress_all(ReadonlyBytes bytes, CompressionLevel compression_level)
{
    DuplexMemoryStream output_stream;
    DeflateCompressor deflate_stream { output_stream, compression_level };

    deflate_stream.write_or_error(bytes);

    deflate_stream.final_flush();

    if (deflate_stream.handle_any_error())
        return {};

    return output_stream.copy_into_contiguous_buffer();
}

}
