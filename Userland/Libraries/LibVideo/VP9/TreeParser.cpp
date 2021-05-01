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

#include "Decoder.h"
#include "LookupTables.h"
#include "TreeParser.h"
#include "assert.h"

namespace Video::VP9 {

int TreeParser::parse_tree(SyntaxElementType type)
{
    auto tree_selection = select_tree(type);
    int value;
    if (tree_selection.is_single_value()) {
        value = tree_selection.get_single_value();
    } else {
        auto tree = tree_selection.get_tree_value();
        int n = 0;
        do {
            n = tree[n + m_decoder.m_bit_stream->read_bool(select_tree_probability(type, n >> 1))];
        } while (n > 0);
        value = -n;
    }
    count_syntax_element(type, value);
    return value;
}

/*
 * Select a tree value based on the type of syntax element being parsed, as well as some parser state, as specified in section 9.3.1
 */
TreeParser::TreeSelection TreeParser::select_tree(SyntaxElementType type)
{
    switch (type) {
    case SyntaxElementType::Partition:
        if (m_decoder.m_has_rows && m_decoder.m_has_cols)
            return { partition_tree };
        if (m_decoder.m_has_cols)
            return { cols_partition_tree };
        if (m_decoder.m_has_rows)
            return { rows_partition_tree };
        return { PARTITION_SPLIT };
    case SyntaxElementType::DefaultIntraMode:
    case SyntaxElementType::DefaultUVMode:
    case SyntaxElementType::IntraMode:
    case SyntaxElementType::SubIntraMode:
    case SyntaxElementType::UVMode:
        return { intra_mode_tree };
    case SyntaxElementType::SegmentID:
        return { segment_tree };
    case SyntaxElementType::Skip:
    case SyntaxElementType::SegIDPredicted:
    case SyntaxElementType::IsInter:
    case SyntaxElementType::CompMode:
    case SyntaxElementType::CompRef:
    case SyntaxElementType::SingleRefP1:
    case SyntaxElementType::SingleRefP2:
    case SyntaxElementType::MVSign:
    case SyntaxElementType::MVClass0Bit:
    case SyntaxElementType::MVBit:
    case SyntaxElementType::MoreCoefs:
        return { binary_tree };
    case SyntaxElementType::TXSize:
        if (m_decoder.m_max_tx_size == TX_32x32)
            return { tx_size_32_tree };
        if (m_decoder.m_max_tx_size == TX_16x16)
            return { tx_size_16_tree };
        return { tx_size_8_tree };
    case SyntaxElementType::InterMode:
        return { inter_mode_tree };
    case SyntaxElementType::InterpFilter:
        return { interp_filter_tree };
    case SyntaxElementType::MVJoint:
        return { mv_joint_tree };
    case SyntaxElementType::MVClass:
        return { mv_class_tree };
    case SyntaxElementType::MVClass0FR:
    case SyntaxElementType::MVFR:
        return { mv_fr_tree };
    case SyntaxElementType::MVClass0HP:
    case SyntaxElementType::MVHP:
        if (m_decoder.m_use_hp)
            return { binary_tree };
        return { 1 };
    case SyntaxElementType::Token:
        return { token_tree };
    }
    VERIFY_NOT_REACHED();
}

/*
 * Select a probability with which to read a boolean when decoding a tree, as specified in section 9.3.2
 */
u8 TreeParser::select_tree_probability(SyntaxElementType type, u8 node)
{
    switch (type) {
    case SyntaxElementType::Partition:
        return calculate_partition_probability(node);
    case SyntaxElementType::DefaultIntraMode:
        break;
    case SyntaxElementType::DefaultUVMode:
        break;
    case SyntaxElementType::IntraMode:
        break;
    case SyntaxElementType::SubIntraMode:
        break;
    case SyntaxElementType::UVMode:
        break;
    case SyntaxElementType::SegmentID:
        return m_decoder.m_segmentation_tree_probs[node];
    case SyntaxElementType::Skip:
        return calculate_skip_probability();
    case SyntaxElementType::SegIDPredicted:
        break;
    case SyntaxElementType::IsInter:
        break;
    case SyntaxElementType::CompMode:
        break;
    case SyntaxElementType::CompRef:
        break;
    case SyntaxElementType::SingleRefP1:
        break;
    case SyntaxElementType::SingleRefP2:
        break;
    case SyntaxElementType::MVSign:
        break;
    case SyntaxElementType::MVClass0Bit:
        break;
    case SyntaxElementType::MVBit:
        break;
    case SyntaxElementType::TXSize:
        break;
    case SyntaxElementType::InterMode:
        break;
    case SyntaxElementType::InterpFilter:
        break;
    case SyntaxElementType::MVJoint:
        break;
    case SyntaxElementType::MVClass:
        break;
    case SyntaxElementType::MVClass0FR:
        break;
    case SyntaxElementType::MVClass0HP:
        break;
    case SyntaxElementType::MVFR:
        break;
    case SyntaxElementType::MVHP:
        break;
    case SyntaxElementType::Token:
        break;
    case SyntaxElementType::MoreCoefs:
        break;
    }
    TODO();
}

u8 TreeParser::calculate_partition_probability(u8 node)
{
    int node2;
    if (m_decoder.m_has_rows && m_decoder.m_has_cols) {
        node2 = node;
    } else if (m_decoder.m_has_cols) {
        node2 = 1;
    } else {
        node2 = 2;
    }

    u32 above = 0;
    u32 left = 0;
    auto bsl = mi_width_log2_lookup[m_decoder.m_block_subsize];
    auto block_offset = mi_width_log2_lookup[Block_64x64] - bsl;
    for (auto i = 0; i < m_decoder.m_num_8x8; i++) {
        // FIXME: This are never non-null, they need to be implemented in the decoder
        above |= m_decoder.m_above_partition_context[m_decoder.m_col + i];
        left |= m_decoder.m_left_partition_context[m_decoder.m_row + i];
    }
    above = (above & (1 << block_offset)) > 0;
    left = (left & (1 << block_offset)) > 0;
    m_ctx = bsl * 4 + left * 2 + above;
    if (m_decoder.m_frame_is_intra)
        return m_decoder.m_probability_tables->kf_partition_probs()[m_ctx][node2];
    return m_decoder.m_probability_tables->partition_probs()[m_ctx][node2];
}

u8 TreeParser::calculate_skip_probability()
{
    m_ctx = 0;
    if (m_decoder.m_available_u) {
        // FIXME: m_ctx += m_skips[m_mi_row - 1][m_mi_col];
    }
    if (m_decoder.m_available_l) {
        // FIXME: m_ctx += m_skips[m_mi_row][m_mi_col - 1];
    }
    return m_decoder.m_probability_tables->skip_prob()[m_ctx];
}

void TreeParser::count_syntax_element(SyntaxElementType type, int value)
{
    switch (type) {
    case SyntaxElementType::Partition:
        m_decoder.m_syntax_element_counter->m_counts_partition[m_ctx][value]++;
        break;
    case SyntaxElementType::IntraMode:
        break;
    case SyntaxElementType::SubIntraMode:
        break;
    case SyntaxElementType::UVMode:
        break;
    case SyntaxElementType::Skip:
        m_decoder.m_syntax_element_counter->m_counts_skip[m_ctx][value]++;
        break;
    case SyntaxElementType::IsInter:
        break;
    case SyntaxElementType::CompMode:
        break;
    case SyntaxElementType::CompRef:
        break;
    case SyntaxElementType::SingleRefP1:
        break;
    case SyntaxElementType::SingleRefP2:
        break;
    case SyntaxElementType::MVSign:
        break;
    case SyntaxElementType::MVClass0Bit:
        break;
    case SyntaxElementType::MVBit:
        break;
    case SyntaxElementType::TXSize:
        break;
    case SyntaxElementType::InterMode:
        break;
    case SyntaxElementType::InterpFilter:
        break;
    case SyntaxElementType::MVJoint:
        break;
    case SyntaxElementType::MVClass:
        break;
    case SyntaxElementType::MVClass0FR:
        break;
    case SyntaxElementType::MVClass0HP:
        break;
    case SyntaxElementType::MVFR:
        break;
    case SyntaxElementType::MVHP:
        break;
    case SyntaxElementType::Token:
        break;
    case SyntaxElementType::MoreCoefs:
        break;
    case SyntaxElementType::DefaultIntraMode:
    case SyntaxElementType::DefaultUVMode:
    case SyntaxElementType::SegmentID:
    case SyntaxElementType::SegIDPredicted:
        break;
    }
}

TreeParser::TreeSelection::TreeSelection(const int* values)
    : m_is_single_value(false)
    , m_value { .m_tree = values }
{
}

TreeParser::TreeSelection::TreeSelection(int value)
    : m_is_single_value(true)
    , m_value { .m_value = value }
{
}

}
