/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitStream.h"
#include "Enums.h"
#include "ProbabilityTables.h"
#include "SyntaxElementCounter.h"

namespace Video::VP9 {

class TreeParser {
public:
    explicit TreeParser(ProbabilityTables& probability_tables)
        : m_probability_tables(probability_tables)
    {
    }

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
    void count_syntax_element(SyntaxElementType type, int value);

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
    void set_syntax_element_counter(SyntaxElementCounter* syntax_element_counter) { m_syntax_element_counter = syntax_element_counter; }
    void set_segmentation_tree_probs(u8* segmentation_tree_probs) { m_segmentation_tree_probs = segmentation_tree_probs; }
    void set_available_u(bool available_u) { m_available_u = available_u; }
    void set_available_l(bool available_l) { m_available_l = available_l; }
    void set_mi_row(u32 mi_row) { m_mi_row = mi_row; }
    void set_mi_col(u32 mi_col) { m_mi_col = mi_col; }

private:
    u8 calculate_partition_probability(u8 node);
    u8 calculate_skip_probability();

    ProbabilityTables& m_probability_tables;
    BitStream* m_bit_stream { nullptr };
    SyntaxElementCounter* m_syntax_element_counter { nullptr };

    // m_ctx is a member variable because it is required for syntax element counting (section 9.3.4)
    u8 m_ctx { 0 };

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
    u8* m_segmentation_tree_probs { nullptr };
    bool m_available_u { false };
    bool m_available_l { false };
    u32 m_mi_col { 0 };
    u32 m_mi_row { 0 };
};

}
