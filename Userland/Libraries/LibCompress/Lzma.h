/*
 * Copyright (c) 2023, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularBuffer.h>
#include <AK/FixedArray.h>
#include <AK/MaybeOwned.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Stream.h>

namespace Compress {

// This implementation is mostly based on the LZMA specification contained in the 7-Zip SDK, which has been placed in the public domain.
// LZMA Specification Draft (2015): https://www.7-zip.org/a/lzma-specification.7z

struct LzmaModelProperties {
    u8 literal_context_bits;
    u8 literal_position_bits;
    u8 position_bits;
};

struct LzmaDecompressorOptions {
    u8 literal_context_bits { 0 };
    u8 literal_position_bits { 0 };
    u8 position_bits { 0 };
    u32 dictionary_size { 0 };
    Optional<u64> uncompressed_size;
    bool reject_end_of_stream_marker { false };
};

// Described in section "lzma file format".
struct [[gnu::packed]] LzmaHeader {
    u32 dictionary_size() const;
    Optional<u64> uncompressed_size() const;

    ErrorOr<LzmaDecompressorOptions> as_decompressor_options() const;

    static ErrorOr<LzmaModelProperties> decode_model_properties(u8 input_bits);

    u8 encoded_model_properties;
    u32 unchecked_dictionary_size;
    u64 encoded_uncompressed_size;
};
static_assert(sizeof(LzmaHeader) == 13);

class LzmaState {
protected:
    // LZMA uses 11-bit probability counters, but they are usually stored in 16-bit variables.
    // Therefore, we can model probabilities with a resolution of up to 1 / 2^11 (which is equal to 1 / 2048).
    // The default probability for most counters is 0.5.
    using Probability = u16;
    static constexpr size_t probability_bit_count = 11;
    static constexpr Probability default_probability = (1 << probability_bit_count) / 2;
    static void initialize_to_default_probability(Span<Probability>);

    LzmaState(FixedArray<Probability> literal_probabilities);

    u64 m_total_processed_bytes { 0 };

    static constexpr size_t literal_probability_table_size = 0x300;
    FixedArray<Probability> m_literal_probabilities;

    struct LzmaLengthCoderState {
    public:
        LzmaLengthCoderState();

        Probability m_first_choice_probability { default_probability };
        Probability m_second_choice_probability { default_probability };

        static constexpr size_t maximum_number_of_position_bits = 4;
        Array<Array<Probability, (1 << 3)>, (1 << maximum_number_of_position_bits)> m_low_length_probabilities;
        Array<Array<Probability, (1 << 3)>, (1 << maximum_number_of_position_bits)> m_medium_length_probabilities;
        Array<Probability, (1 << 8)> m_high_length_probabilities;
    };

    LzmaLengthCoderState m_length_coder;
    LzmaLengthCoderState m_rep_length_coder;

    static constexpr u16 normalized_to_real_match_length_offset = 2;
    static constexpr u32 normalized_to_real_match_distance_offset = 1;

    static constexpr size_t number_of_length_to_position_states = 4;
    Array<Array<Probability, (1 << 6)>, number_of_length_to_position_states> m_length_to_position_states;

    static constexpr size_t first_position_slot_with_binary_tree_bits = 4;
    static constexpr size_t first_position_slot_with_direct_encoded_bits = 14;

    // This is a bit wasteful on memory and not in the specification, but it makes the math easier.
    static constexpr size_t number_of_binary_tree_distance_slots = first_position_slot_with_direct_encoded_bits - first_position_slot_with_binary_tree_bits;
    static constexpr size_t largest_number_of_binary_tree_distance_bits = 5;
    Array<Array<Probability, (1 << largest_number_of_binary_tree_distance_bits)>, number_of_binary_tree_distance_slots> m_binary_tree_distance_probabilities;

    static constexpr size_t number_of_alignment_bits = 4;
    Array<Probability, (1 << number_of_alignment_bits)> m_alignment_bit_probabilities;

    // LZ state tracking.
    u16 m_state { 0 };
    u32 m_rep0 { 0 };
    u32 m_rep1 { 0 };
    u32 m_rep2 { 0 };
    u32 m_rep3 { 0 };
    u32 current_repetition_offset() const;

    void update_state_after_literal();
    void update_state_after_match();
    void update_state_after_rep();
    void update_state_after_short_rep();

    static constexpr size_t maximum_number_of_position_bits = 4;
    static constexpr size_t number_of_states = 12;
    Array<Probability, (number_of_states << maximum_number_of_position_bits)> m_is_match_probabilities;
    Array<Probability, number_of_states> m_is_rep_probabilities;
    Array<Probability, number_of_states> m_is_rep_g0_probabilities;
    Array<Probability, number_of_states> m_is_rep_g1_probabilities;
    Array<Probability, number_of_states> m_is_rep_g2_probabilities;
    Array<Probability, (number_of_states << maximum_number_of_position_bits)> m_is_rep0_long_probabilities;
};

class LzmaDecompressor : public Stream
    , LzmaState {
public:
    /// Creates a decompressor from a standalone LZMA container (.lzma file extension, occasionally known as an LZMA 'archive').
    static ErrorOr<NonnullOwnPtr<LzmaDecompressor>> create_from_container(MaybeOwned<Stream>, Optional<MaybeOwned<CircularBuffer>> dictionary = {});

    /// Creates a decompressor from a raw stream of LZMA-compressed data (found inside an LZMA container or embedded in other file formats).
    static ErrorOr<NonnullOwnPtr<LzmaDecompressor>> create_from_raw_stream(MaybeOwned<Stream>, LzmaDecompressorOptions const&, Optional<MaybeOwned<CircularBuffer>> dictionary = {});

    ErrorOr<void> append_input_stream(MaybeOwned<Stream>, Optional<u64> uncompressed_size);

    virtual ErrorOr<Bytes> read_some(Bytes) override;
    virtual ErrorOr<size_t> write_some(ReadonlyBytes) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;

private:
    LzmaDecompressor(MaybeOwned<Stream>, LzmaDecompressorOptions, MaybeOwned<CircularBuffer>, FixedArray<Probability> literal_probabilities);

    MaybeOwned<Stream> m_stream;
    LzmaDecompressorOptions m_options;

    // This doubles as an output buffer, since we have to write all of our results into this anyways.
    MaybeOwned<CircularBuffer> m_dictionary;
    bool m_found_end_of_stream_marker { false };
    bool is_range_decoder_in_clean_state() const;
    bool has_reached_expected_data_size() const;
    Optional<u16> m_leftover_match_length;

    // Range decoder state (initialized with stream data in LzmaDecompressor::create).
    u32 m_range_decoder_range { 0xFFFFFFFF };
    u32 m_range_decoder_code { 0 };

    ErrorOr<void> initialize_range_decoder();
    ErrorOr<void> normalize_range_decoder();
    ErrorOr<u8> decode_direct_bit();
    ErrorOr<u8> decode_bit_with_probability(Probability& probability);

    // Decodes a multi-bit symbol using a given probability tree (either in normal or in reverse order).
    // The specification states that "unsigned" is at least 16 bits in size, our implementation assumes this as the maximum symbol size.
    ErrorOr<u16> decode_symbol_using_bit_tree(size_t bit_count, Span<Probability> probability_tree);
    ErrorOr<u16> decode_symbol_using_reverse_bit_tree(size_t bit_count, Span<Probability> probability_tree);

    ErrorOr<void> decode_literal_to_output_buffer();

    ErrorOr<u16> decode_normalized_match_length(LzmaLengthCoderState&);

    // This deviates from the specification, which states that "unsigned" is at least 16-bit.
    // However, the match distance needs to be at least 32-bit, at the very least to hold the 0xFFFFFFFF end marker value.
    ErrorOr<u32> decode_normalized_match_distance(u16 normalized_match_length);
};

}

template<>
struct AK::Traits<Compress::LzmaHeader> : public AK::GenericTraits<Compress::LzmaHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};
