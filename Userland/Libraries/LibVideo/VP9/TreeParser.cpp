/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TreeParser.h"
#include "Decoder.h"
#include "LookupTables.h"

namespace Video::VP9 {

template<typename T>
T TreeParser::parse_tree(SyntaxElementType type)
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
    return static_cast<T>(value);
}

template int TreeParser::parse_tree(SyntaxElementType);
template bool TreeParser::parse_tree(SyntaxElementType);
template u8 TreeParser::parse_tree(SyntaxElementType);
template IntraMode TreeParser::parse_tree(SyntaxElementType);
template TXSize TreeParser::parse_tree(SyntaxElementType);
template InterpolationFilter TreeParser::parse_tree(SyntaxElementType);
template ReferenceMode TreeParser::parse_tree(SyntaxElementType);

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
        return { PartitionSplit };
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
        return;
    case SyntaxElementType::IntraMode:
    case SyntaxElementType::SubIntraMode:
        m_decoder.m_syntax_element_counter->m_counts_intra_mode[m_ctx][value]++;
        return;
    case SyntaxElementType::UVMode:
        m_decoder.m_syntax_element_counter->m_counts_uv_mode[m_ctx][value]++;
        return;
    case SyntaxElementType::Skip:
        m_decoder.m_syntax_element_counter->m_counts_skip[m_ctx][value]++;
        return;
    case SyntaxElementType::IsInter:
        m_decoder.m_syntax_element_counter->m_counts_is_inter[m_ctx][value]++;
        return;
    case SyntaxElementType::CompMode:
        m_decoder.m_syntax_element_counter->m_counts_comp_mode[m_ctx][value]++;
        return;
    case SyntaxElementType::CompRef:
        m_decoder.m_syntax_element_counter->m_counts_comp_ref[m_ctx][value]++;
        return;
    case SyntaxElementType::SingleRefP1:
        m_decoder.m_syntax_element_counter->m_counts_single_ref[m_ctx][0][value]++;
        return;
    case SyntaxElementType::SingleRefP2:
        m_decoder.m_syntax_element_counter->m_counts_single_ref[m_ctx][1][value]++;
        return;
    case SyntaxElementType::MVSign:
        break;
    case SyntaxElementType::MVClass0Bit:
        break;
    case SyntaxElementType::MVBit:
        break;
    case SyntaxElementType::TXSize:
        m_decoder.m_syntax_element_counter->m_counts_tx_size[m_decoder.m_max_tx_size][m_ctx][value]++;
        return;
    case SyntaxElementType::InterMode:
        m_decoder.m_syntax_element_counter->m_counts_inter_mode[m_ctx][value]++;
        return;
    case SyntaxElementType::InterpFilter:
        m_decoder.m_syntax_element_counter->m_counts_interp_filter[m_ctx][value]++;
        return;
    case SyntaxElementType::MVJoint:
        m_decoder.m_syntax_element_counter->m_counts_mv_joint[value]++;
        return;
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
        // No counting required
        return;
    }
    VERIFY_NOT_REACHED();
}

TreeParser::TreeSelection::TreeSelection(int const* values)
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
