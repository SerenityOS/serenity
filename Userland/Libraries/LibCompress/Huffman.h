/*
 * Copyright (c) 2024, the SerenityOS developers.
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/BinaryHeap.h>

namespace Compress {

inline void generate_huffman_lengths(Bytes lengths, ReadonlySpan<u16> frequencies, size_t max_bit_length, u16 shift = 0)
{
    VERIFY(lengths.size() == frequencies.size());
    auto const size = lengths.size();
    VERIFY((1u << max_bit_length) >= size);
    u16 heap_keys[size]; // Used for O(n) heap construction
    u16 heap_values[size];

    u16 huffman_links[size * 2];
    size_t non_zero_freqs = 0;
    for (auto frequency : frequencies) {
        if (frequency == 0)
            continue;

        frequency = max(1, frequency >> shift);
        heap_keys[non_zero_freqs] = frequency;               // sort symbols by frequency
        heap_values[non_zero_freqs] = size + non_zero_freqs; // huffman_links "links"
        non_zero_freqs++;
    }

    // special case for only 1 used symbol
    if (non_zero_freqs < 2) {
        for (size_t i = 0; i < size; i++)
            lengths[i] = (frequencies[i] == 0) ? 0 : 1;
        return;
    }

    BinaryHeap<u16, u16, 288> heap { heap_keys, heap_values, non_zero_freqs };

    // build the huffman tree - binary heap is used for efficient frequency comparisons
    while (heap.size() > 1) {
        u16 lowest_frequency = heap.peek_min_key();
        u16 lowest_link = heap.pop_min();
        u16 second_lowest_frequency = heap.peek_min_key();
        u16 second_lowest_link = heap.pop_min();

        u16 new_link = heap.size() + 1;

        u32 sum = lowest_frequency + second_lowest_frequency;
        sum = min(sum, UINT16_MAX);
        heap.insert(sum, new_link);

        huffman_links[lowest_link] = new_link;
        huffman_links[second_lowest_link] = new_link;
    }

    non_zero_freqs = 0;
    for (size_t i = 0; i < size; i++) {
        if (frequencies[i] == 0) {
            lengths[i] = 0;
            continue;
        }

        u16 link = huffman_links[size + non_zero_freqs];
        non_zero_freqs++;

        size_t bit_length = 1;
        while (link != 1) {
            bit_length++;
            link = huffman_links[link];
        }

        if (bit_length > max_bit_length) {
            VERIFY(shift < 15);
            return generate_huffman_lengths(lengths, frequencies, max_bit_length, shift + 1);
        }

        lengths[i] = bit_length;
    }
}

}
