/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TreeParser.h"
#include "LookupTables.h"
#include "Parser.h"

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
template u32 TreeParser::parse_tree(SyntaxElementType);
template IntraMode TreeParser::parse_tree(SyntaxElementType);
template TXSize TreeParser::parse_tree(SyntaxElementType);
template InterpolationFilter TreeParser::parse_tree(SyntaxElementType);
template ReferenceMode TreeParser::parse_tree(SyntaxElementType);
template Token TreeParser::parse_tree(SyntaxElementType);
template MvClass TreeParser::parse_tree(SyntaxElementType);
template MvJoint TreeParser::parse_tree(SyntaxElementType);

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
        return calculate_default_intra_mode_probability(node);
    case SyntaxElementType::DefaultUVMode:
        return calculate_default_uv_mode_probability(node);
    case SyntaxElementType::IntraMode:
        return calculate_intra_mode_probability(node);
    case SyntaxElementType::SubIntraMode:
        return calculate_sub_intra_mode_probability(node);
    case SyntaxElementType::UVMode:
        return calculate_uv_mode_probability(node);
    case SyntaxElementType::SegmentID:
        return calculate_segment_id_probability(node);
    case SyntaxElementType::Skip:
        return calculate_skip_probability();
    case SyntaxElementType::SegIDPredicted:
        return calculate_seg_id_predicted_probability();
    case SyntaxElementType::IsInter:
        return calculate_is_inter_probability();
    case SyntaxElementType::CompMode:
        return calculate_comp_mode_probability();
    case SyntaxElementType::CompRef:
        return calculate_comp_ref_probability();
    case SyntaxElementType::SingleRefP1:
        return calculate_single_ref_p1_probability();
    case SyntaxElementType::SingleRefP2:
        return calculate_single_ref_p2_probability();
    case SyntaxElementType::MVSign:
        break;
    case SyntaxElementType::MVClass0Bit:
        break;
    case SyntaxElementType::MVBit:
        break;
    case SyntaxElementType::TXSize:
        return calculate_tx_size_probability(node);
    case SyntaxElementType::InterMode:
        return calculate_inter_mode_probability(node);
    case SyntaxElementType::InterpFilter:
        return calculate_interp_filter_probability(node);
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
        return calculate_token_probability(node);
    case SyntaxElementType::MoreCoefs:
        return calculate_more_coefs_probability();
    }
    TODO();
}

#define ABOVE_FRAME_0 m_decoder.m_above_ref_frame[0]
#define ABOVE_FRAME_1 m_decoder.m_above_ref_frame[1]
#define LEFT_FRAME_0 m_decoder.m_left_ref_frame[0]
#define LEFT_FRAME_1 m_decoder.m_left_ref_frame[1]
#define AVAIL_U m_decoder.m_available_u
#define AVAIL_L m_decoder.m_available_l
#define ABOVE_INTRA m_decoder.m_above_intra
#define LEFT_INTRA m_decoder.m_left_intra
#define ABOVE_SINGLE m_decoder.m_above_single
#define LEFT_SINGLE m_decoder.m_left_single

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

u8 TreeParser::calculate_default_intra_mode_probability(u8 node)
{
    u32 above_mode, left_mode;
    if (m_decoder.m_mi_size >= Block_8x8) {
        above_mode = AVAIL_U
            ? m_decoder.m_sub_modes[(m_decoder.m_mi_row - 1) * m_decoder.m_mi_cols * 4 + m_decoder.m_mi_col * 4 + 2]
            : DcPred;
        left_mode = AVAIL_L
            ? m_decoder.m_sub_modes[m_decoder.m_mi_row * m_decoder.m_mi_cols * 4 + (m_decoder.m_mi_col - 1) * 4 + 1]
            : DcPred;
    } else {
        if (m_idy) {
            above_mode = m_decoder.m_block_sub_modes[m_idx];
        } else {
            above_mode = AVAIL_U
                ? m_decoder.m_sub_modes[(m_decoder.m_mi_row - 1) * m_decoder.m_mi_cols * 4 + m_decoder.m_mi_col * 4 + 2 + m_idx]
                : DcPred;
        }

        if (m_idx) {
            left_mode = m_decoder.m_block_sub_modes[m_idy * 2];
        } else {
            left_mode = AVAIL_L
                ? m_decoder.m_sub_modes[m_decoder.m_mi_row * m_decoder.m_mi_cols * 4 + (m_decoder.m_mi_col - 1) * 4 + 1 + m_idy * 2]
                : DcPred;
        }
    }
    return m_decoder.m_probability_tables->kf_y_mode_probs()[above_mode][left_mode][node];
}

u8 TreeParser::calculate_default_uv_mode_probability(u8 node)
{
    return m_decoder.m_probability_tables->kf_uv_mode_prob()[m_decoder.m_y_mode][node];
}

u8 TreeParser::calculate_intra_mode_probability(u8 node)
{
    m_ctx = size_group_lookup[m_decoder.m_mi_size];
    return m_decoder.m_probability_tables->y_mode_probs()[m_ctx][node];
}

u8 TreeParser::calculate_sub_intra_mode_probability(u8 node)
{
    m_ctx = 0;
    return m_decoder.m_probability_tables->y_mode_probs()[m_ctx][node];
}

u8 TreeParser::calculate_uv_mode_probability(u8 node)
{
    m_ctx = m_decoder.m_y_mode;
    return m_decoder.m_probability_tables->uv_mode_probs()[m_ctx][node];
}

u8 TreeParser::calculate_segment_id_probability(u8 node)
{
    return m_decoder.m_segmentation_tree_probs[node];
}

u8 TreeParser::calculate_skip_probability()
{
    m_ctx = 0;
    if (AVAIL_U)
        m_ctx += m_decoder.m_skips[(m_decoder.m_mi_row - 1) * m_decoder.m_mi_cols + m_decoder.m_mi_col];
    if (AVAIL_L)
        m_ctx += m_decoder.m_skips[m_decoder.m_mi_row * m_decoder.m_mi_cols + m_decoder.m_mi_col - 1];
    return m_decoder.m_probability_tables->skip_prob()[m_ctx];
}

u8 TreeParser::calculate_seg_id_predicted_probability()
{
    m_ctx = m_decoder.m_left_seg_pred_context[m_decoder.m_mi_row] + m_decoder.m_above_seg_pred_context[m_decoder.m_mi_col];
    return m_decoder.m_segmentation_pred_prob[m_ctx];
}

u8 TreeParser::calculate_is_inter_probability()
{
    if (AVAIL_U && AVAIL_L) {
        m_ctx = (LEFT_INTRA && ABOVE_INTRA) ? 3 : LEFT_INTRA || ABOVE_INTRA;
    } else if (AVAIL_U || AVAIL_L) {
        m_ctx = 2 * (AVAIL_U ? ABOVE_INTRA : LEFT_INTRA);
    } else {
        m_ctx = 0;
    }
    return m_decoder.m_probability_tables->is_inter_prob()[m_ctx];
}

u8 TreeParser::calculate_comp_mode_probability()
{
    if (AVAIL_U && AVAIL_L) {
        if (ABOVE_SINGLE && LEFT_SINGLE) {
            auto is_above_fixed = ABOVE_FRAME_0 == m_decoder.m_comp_fixed_ref;
            auto is_left_fixed = LEFT_FRAME_0 == m_decoder.m_comp_fixed_ref;
            m_ctx = is_above_fixed ^ is_left_fixed;
        } else if (ABOVE_SINGLE) {
            auto is_above_fixed = ABOVE_FRAME_0 == m_decoder.m_comp_fixed_ref;
            m_ctx = 2 + (is_above_fixed || ABOVE_INTRA);
        } else if (LEFT_SINGLE) {
            auto is_left_fixed = LEFT_FRAME_0 == m_decoder.m_comp_fixed_ref;
            m_ctx = 2 + (is_left_fixed || LEFT_INTRA);
        } else {
            m_ctx = 4;
        }
    } else if (AVAIL_U) {
        if (ABOVE_SINGLE) {
            m_ctx = ABOVE_FRAME_0 == m_decoder.m_comp_fixed_ref;
        } else {
            m_ctx = 3;
        }
    } else if (AVAIL_L) {
        if (LEFT_SINGLE) {
            m_ctx = LEFT_FRAME_0 == m_decoder.m_comp_fixed_ref;
        } else {
            m_ctx = 3;
        }
    } else {
        m_ctx = 1;
    }
    return m_decoder.m_probability_tables->comp_mode_prob()[m_ctx];
}

u8 TreeParser::calculate_comp_ref_probability()
{
    auto fix_ref_idx = m_decoder.m_ref_frame_sign_bias[m_decoder.m_comp_fixed_ref];
    auto var_ref_idx = !fix_ref_idx;
    if (AVAIL_U && AVAIL_L) {
        if (ABOVE_INTRA && LEFT_INTRA) {
            m_ctx = 2;
        } else if (LEFT_INTRA) {
            if (ABOVE_SINGLE) {
                m_ctx = 1 + 2 * (ABOVE_FRAME_0 != m_decoder.m_comp_var_ref[1]);
            } else {
                m_ctx = 1 + 2 * (m_decoder.m_above_ref_frame[var_ref_idx] != m_decoder.m_comp_var_ref[1]);
            }
        } else if (ABOVE_INTRA) {
            if (LEFT_SINGLE) {
                m_ctx = 1 + 2 * (LEFT_FRAME_0 != m_decoder.m_comp_var_ref[1]);
            } else {
                m_ctx = 1 + 2 * (m_decoder.m_left_ref_frame[var_ref_idx] != m_decoder.m_comp_var_ref[1]);
            }
        } else {
            auto var_ref_above = m_decoder.m_above_ref_frame[ABOVE_SINGLE ? 0 : var_ref_idx];
            auto var_ref_left = m_decoder.m_left_ref_frame[LEFT_SINGLE ? 0 : var_ref_idx];
            if (var_ref_above == var_ref_left && m_decoder.m_comp_var_ref[1] == var_ref_above) {
                m_ctx = 0;
            } else if (LEFT_SINGLE && ABOVE_SINGLE) {
                if ((var_ref_above == m_decoder.m_comp_fixed_ref && var_ref_left == m_decoder.m_comp_var_ref[0])
                    || (var_ref_left == m_decoder.m_comp_fixed_ref && var_ref_above == m_decoder.m_comp_var_ref[0])) {
                    m_ctx = 4;
                } else if (var_ref_above == var_ref_left) {
                    m_ctx = 3;
                } else {
                    m_ctx = 1;
                }
            } else if (LEFT_SINGLE || ABOVE_SINGLE) {
                auto vrfc = LEFT_SINGLE ? var_ref_above : var_ref_left;
                auto rfs = ABOVE_SINGLE ? var_ref_above : var_ref_left;
                if (vrfc == m_decoder.m_comp_var_ref[1] && rfs != m_decoder.m_comp_var_ref[1]) {
                    m_ctx = 1;
                } else if (rfs == m_decoder.m_comp_var_ref[1] && vrfc != m_decoder.m_comp_var_ref[1]) {
                    m_ctx = 2;
                } else {
                    m_ctx = 4;
                }
            } else if (var_ref_above == var_ref_left) {
                m_ctx = 4;
            } else {
                m_ctx = 2;
            }
        }
    } else if (AVAIL_U) {
        if (ABOVE_INTRA) {
            m_ctx = 2;
        } else {
            if (ABOVE_SINGLE) {
                m_ctx = 3 * (ABOVE_FRAME_0 != m_decoder.m_comp_var_ref[1]);
            } else {
                m_ctx = 4 * (m_decoder.m_above_ref_frame[var_ref_idx] != m_decoder.m_comp_var_ref[1]);
            }
        }
    } else if (AVAIL_L) {
        if (LEFT_INTRA) {
            m_ctx = 2;
        } else {
            if (LEFT_SINGLE) {
                m_ctx = 3 * (LEFT_FRAME_0 != m_decoder.m_comp_var_ref[1]);
            } else {
                m_ctx = 4 * (m_decoder.m_left_ref_frame[var_ref_idx] != m_decoder.m_comp_var_ref[1]);
            }
        }
    } else {
        m_ctx = 2;
    }

    return m_decoder.m_probability_tables->comp_ref_prob()[m_ctx];
}

u8 TreeParser::calculate_single_ref_p1_probability()
{
    if (AVAIL_U && AVAIL_L) {
        if (ABOVE_INTRA && LEFT_INTRA) {
            m_ctx = 2;
        } else if (LEFT_INTRA) {
            if (ABOVE_SINGLE) {
                m_ctx = 4 * (ABOVE_FRAME_0 == LastFrame);
            } else {
                m_ctx = 1 + (ABOVE_FRAME_0 == LastFrame || ABOVE_FRAME_1 == LastFrame);
            }
        } else if (ABOVE_INTRA) {
            if (LEFT_SINGLE) {
                m_ctx = 4 * (LEFT_FRAME_0 == LastFrame);
            } else {
                m_ctx = 1 + (LEFT_FRAME_0 == LastFrame || LEFT_FRAME_1 == LastFrame);
            }
        } else {
            if (LEFT_SINGLE && ABOVE_SINGLE) {
                m_ctx = 2 * (ABOVE_FRAME_0 == LastFrame) + 2 * (LEFT_FRAME_0 == LastFrame);
            } else if (!LEFT_SINGLE && !ABOVE_SINGLE) {
                auto above_is_last = ABOVE_FRAME_0 == LastFrame || ABOVE_FRAME_1 == LastFrame;
                auto left_is_last = LEFT_FRAME_0 == LastFrame || LEFT_FRAME_1 == LastFrame;
                m_ctx = 1 + (above_is_last || left_is_last);
            } else {
                auto rfs = ABOVE_SINGLE ? ABOVE_FRAME_0 : LEFT_FRAME_0;
                auto crf1 = ABOVE_SINGLE ? LEFT_FRAME_0 : ABOVE_FRAME_0;
                auto crf2 = ABOVE_SINGLE ? LEFT_FRAME_1 : ABOVE_FRAME_1;
                m_ctx = crf1 == LastFrame || crf2 == LastFrame;
                if (rfs == LastFrame)
                    m_ctx += 3;
            }
        }
    } else if (AVAIL_U) {
        if (ABOVE_INTRA) {
            m_ctx = 2;
        } else {
            if (ABOVE_SINGLE) {
                m_ctx = 4 * (ABOVE_FRAME_0 == LastFrame);
            } else {
                m_ctx = 1 + (ABOVE_FRAME_0 == LastFrame || ABOVE_FRAME_1 == LastFrame);
            }
        }
    } else if (AVAIL_L) {
        if (LEFT_INTRA) {
            m_ctx = 2;
        } else {
            if (LEFT_SINGLE) {
                m_ctx = 4 * (LEFT_FRAME_0 == LastFrame);
            } else {
                m_ctx = 1 + (LEFT_FRAME_0 == LastFrame || LEFT_FRAME_1 == LastFrame);
            }
        }
    } else {
        m_ctx = 2;
    }
    return m_decoder.m_probability_tables->single_ref_prob()[m_ctx][0];
}

u8 TreeParser::calculate_single_ref_p2_probability()
{
    if (AVAIL_U && AVAIL_L) {
        if (ABOVE_INTRA && LEFT_INTRA) {
            m_ctx = 2;
        } else if (LEFT_INTRA) {
            if (ABOVE_SINGLE) {
                if (ABOVE_FRAME_0 == LastFrame) {
                    m_ctx = 3;
                } else {
                    m_ctx = 4 * (ABOVE_FRAME_0 == GoldenFrame);
                }
            } else {
                m_ctx = 1 + 2 * (ABOVE_FRAME_0 == GoldenFrame || ABOVE_FRAME_1 == GoldenFrame);
            }
        } else if (ABOVE_INTRA) {
            if (LEFT_SINGLE) {
                if (LEFT_FRAME_0 == LastFrame) {
                    m_ctx = 3;
                } else {
                    m_ctx = 4 * (LEFT_FRAME_0 == GoldenFrame);
                }
            } else {
                m_ctx = 1 + 2 * (LEFT_FRAME_0 == GoldenFrame || LEFT_FRAME_1 == GoldenFrame);
            }
        } else {
            if (LEFT_SINGLE && ABOVE_SINGLE) {
                auto above_last = ABOVE_FRAME_0 == LastFrame;
                auto left_last = LEFT_FRAME_0 == LastFrame;
                if (above_last && left_last) {
                    m_ctx = 3;
                } else if (above_last) {
                    m_ctx = 4 * (LEFT_FRAME_0 == GoldenFrame);
                } else if (left_last) {
                    m_ctx = 4 * (ABOVE_FRAME_0 == GoldenFrame);
                } else {
                    m_ctx = 2 * (ABOVE_FRAME_0 == GoldenFrame) + 2 * (LEFT_FRAME_0 == GoldenFrame);
                }
            } else if (!LEFT_SINGLE && !ABOVE_SINGLE) {
                if (ABOVE_FRAME_0 == LEFT_FRAME_0 && ABOVE_FRAME_1 == LEFT_FRAME_1) {
                    m_ctx = 3 * (ABOVE_FRAME_0 == GoldenFrame || ABOVE_FRAME_1 == GoldenFrame);
                } else {
                    m_ctx = 2;
                }
            } else {
                auto rfs = ABOVE_SINGLE ? ABOVE_FRAME_0 : LEFT_FRAME_0;
                auto crf1 = ABOVE_SINGLE ? LEFT_FRAME_0 : ABOVE_FRAME_0;
                auto crf2 = ABOVE_SINGLE ? LEFT_FRAME_1 : ABOVE_FRAME_1;
                m_ctx = crf1 == GoldenFrame || crf2 == GoldenFrame;
                if (rfs == GoldenFrame) {
                    m_ctx += 3;
                } else if (rfs != AltRefFrame) {
                    m_ctx = 1 + (2 * m_ctx);
                }
            }
        }
    } else if (AVAIL_U) {
        if (ABOVE_INTRA || (ABOVE_FRAME_0 == LastFrame && ABOVE_SINGLE)) {
            m_ctx = 2;
        } else if (ABOVE_SINGLE) {
            m_ctx = 4 * (ABOVE_FRAME_0 == GoldenFrame);
        } else {
            m_ctx = 3 * (ABOVE_FRAME_0 == GoldenFrame || ABOVE_FRAME_1 == GoldenFrame);
        }
    } else if (AVAIL_L) {
        if (LEFT_INTRA || (LEFT_FRAME_0 == LastFrame && LEFT_SINGLE)) {
            m_ctx = 2;
        } else if (LEFT_SINGLE) {
            m_ctx = 4 * (LEFT_FRAME_0 == GoldenFrame);
        } else {
            m_ctx = 3 * (LEFT_FRAME_0 == GoldenFrame || LEFT_FRAME_1 == GoldenFrame);
        }
    } else {
        m_ctx = 2;
    }
    return m_decoder.m_probability_tables->single_ref_prob()[m_ctx][1];
}

u8 TreeParser::calculate_tx_size_probability(u8 node)
{
    auto above = m_decoder.m_max_tx_size;
    auto left = m_decoder.m_max_tx_size;
    auto u_pos = (m_decoder.m_mi_row - 1) * m_decoder.m_mi_cols + m_decoder.m_mi_col;
    if (AVAIL_U && !m_decoder.m_skips[u_pos])
        above = m_decoder.m_tx_sizes[u_pos];
    auto l_pos = m_decoder.m_mi_row * m_decoder.m_mi_cols + m_decoder.m_mi_col - 1;
    if (AVAIL_L && !m_decoder.m_skips[l_pos])
        left = m_decoder.m_tx_sizes[l_pos];
    if (!AVAIL_L)
        left = above;
    if (!AVAIL_U)
        above = left;
    m_ctx = (above + left) > m_decoder.m_max_tx_size;
    return m_decoder.m_probability_tables->tx_probs()[m_decoder.m_max_tx_size][m_ctx][node];
}

u8 TreeParser::calculate_inter_mode_probability(u8 node)
{
    //FIXME: Implement when ModeContext is implemented
    // m_ctx = m_decoder.m_mode_context[m_decoder.m_ref_frame[0]]
    return m_decoder.m_probability_tables->inter_mode_probs()[m_ctx][node];
}

u8 TreeParser::calculate_interp_filter_probability(u8 node)
{
    auto left_interp = (AVAIL_L && m_decoder.m_left_ref_frame[0] > IntraFrame)
        ? m_decoder.m_interp_filters[m_decoder.m_mi_row * m_decoder.m_mi_cols + m_decoder.m_mi_col - 1]
        : 3;
    auto above_interp = (AVAIL_U && m_decoder.m_above_ref_frame[0] > IntraFrame)
        ? m_decoder.m_interp_filters[m_decoder.m_mi_row * m_decoder.m_mi_cols + m_decoder.m_mi_col - 1]
        : 3;
    if (left_interp == above_interp || (left_interp != 3 && above_interp == 3))
        m_ctx = left_interp;
    else if (left_interp == 3 && above_interp != 3)
        m_ctx = above_interp;
    else
        m_ctx = 3;
    return m_decoder.m_probability_tables->interp_filter_probs()[m_ctx][node];
}

u8 TreeParser::calculate_token_probability(u8 node)
{
    auto prob = m_decoder.m_probability_tables->coef_probs()[m_tx_size][m_plane > 0][m_decoder.m_is_inter][m_band][m_ctx][min(2, 1 + node)];
    if (node < 2)
        return prob;
    auto x = (prob - 1) / 2;
    auto& pareto_table = m_decoder.m_probability_tables->pareto_table();
    if (prob & 1)
        return pareto_table[x][node - 2];
    return (pareto_table[x][node - 2] + pareto_table[x + 1][node - 2]) >> 1;
}

u8 TreeParser::calculate_more_coefs_probability()
{
    if (m_c == 0) {
        auto sx = m_plane > 0 ? m_decoder.m_subsampling_x : 0;
        auto sy = m_plane > 0 ? m_decoder.m_subsampling_y : 0;
        auto max_x = (2 * m_decoder.m_mi_cols) >> sx;
        auto max_y = (2 * m_decoder.m_mi_rows) >> sy;
        u8 numpts = 1 << m_tx_size;
        auto x4 = m_start_x >> 2;
        auto y4 = m_start_y >> 2;
        u32 above = 0;
        u32 left = 0;
        for (size_t i = 0; i < numpts; i++) {
            if (x4 + i < max_x)
                above |= m_decoder.m_above_nonzero_context[m_plane][x4 + i];
            if (y4 + i < max_y)
                left |= m_decoder.m_left_nonzero_context[m_plane][y4 + i];
        }
        m_ctx = above + left;
    } else {
        u32 neighbor_0, neighbor_1;
        auto n = 4 << m_tx_size;
        auto i = m_pos / n;
        auto j = m_pos % n;
        auto a = (i - 1) * n + j;
        auto a2 = i * n + j - 1;
        if (i > 0 && j > 0) {
            if (m_decoder.m_tx_type == DCT_ADST) {
                neighbor_0 = a;
                neighbor_1 = a;
            } else if (m_decoder.m_tx_type == ADST_DCT) {
                neighbor_0 = a2;
                neighbor_1 = a2;
            } else {
                neighbor_0 = a;
                neighbor_1 = a2;
            }
        } else if (i > 0) {
            neighbor_0 = a;
            neighbor_1 = a;
        } else {
            neighbor_0 = a2;
            neighbor_1 = a2;
        }
        m_ctx = (1 + m_decoder.m_token_cache[neighbor_0] + m_decoder.m_token_cache[neighbor_1]) >> 1;
    }
    return m_decoder.m_probability_tables->coef_probs()[m_tx_size][m_plane > 0][m_decoder.m_is_inter][m_band][m_ctx][0];
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
        m_decoder.m_syntax_element_counter->m_counts_token[m_tx_size][m_plane > 0][m_decoder.m_is_inter][m_band][m_ctx][min(2, value)]++;
        return;
    case SyntaxElementType::MoreCoefs:
        m_decoder.m_syntax_element_counter->m_counts_more_coefs[m_tx_size][m_plane > 0][m_decoder.m_is_inter][m_band][m_ctx][value]++;
        return;
    case SyntaxElementType::DefaultIntraMode:
    case SyntaxElementType::DefaultUVMode:
    case SyntaxElementType::SegmentID:
    case SyntaxElementType::SegIDPredicted:
        // No counting required
        return;
    }
    TODO();
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
