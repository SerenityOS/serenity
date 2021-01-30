/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
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

#include "BitStream.h"
#include "ProbabilityTables.h"
#include "SyntaxElementCounter.h"

namespace Video {

class TreeParser {
public:
    explicit TreeParser(ProbabilityTables& probability_tables) : m_probability_tables(probability_tables) {}

    class TreeSelection {
    public:
        union TreeSelectionValue {
            const int* m_tree;
            int m_value;
        };

        TreeSelection(const int* values);
        TreeSelection(int value);

        bool is_single_value() const { return m_is_single_value; }
        int get_single_value() const { return m_value.m_value; }
        const int* get_tree_value() const { return m_value.m_tree; }
    private:
        bool m_is_single_value;
        TreeSelectionValue m_value;
    };

    int parse_tree(SyntaxElementType type);
    TreeSelection select_tree(SyntaxElementType type);
    u8 select_tree_probability(SyntaxElementType type, u8 node);

    void set_bit_stream(BitStream* bit_stream) { m_bit_stream = bit_stream; }
    void set_has_rows(bool has_rows) { m_has_rows = has_rows; }
    void set_has_cols(bool has_cols) { m_has_cols = has_cols; }
    void set_max_tx_size(TXSize max_tx_size) { m_max_tx_size = max_tx_size; }
    void set_use_hp(bool use_hp) { m_use_hp = use_hp; }
    void set_block_subsize(u8 block_subsize) { m_block_subsize = block_subsize; }
    void set_num_8x8(u8 num_8x8) { m_num_8x8 = num_8x8; }
    void set_above_partition_context(u8* above_partition_context) { m_above_partition_context = above_partition_context; }
    void set_left_partition_context(u8* left_partition_context) { m_left_partition_context = left_partition_context; }
    void set_col(u32 col) { m_col = col; }
    void set_row(u32 row) { m_row = row; }
    void set_frame_is_intra(bool frame_is_intra) { m_frame_is_intra = frame_is_intra; }

private:
    u8 calculate_partition_probability(u8 node);

    ProbabilityTables& m_probability_tables;
    BitStream* m_bit_stream { nullptr };

    bool m_has_rows { false };
    bool m_has_cols { false };
    TXSize m_max_tx_size { TX_4x4 };
    bool m_use_hp { false };
    u8 m_block_subsize { 0 };
    u8 m_num_8x8 { 0 };
    u8* m_above_partition_context { nullptr };
    u8* m_left_partition_context { nullptr };
    u32 m_col { 0 };
    u32 m_row { 0 };
    bool m_frame_is_intra { false };
};

}
