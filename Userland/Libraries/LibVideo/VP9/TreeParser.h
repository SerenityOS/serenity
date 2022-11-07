/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitStream.h"
#include "Context.h"
#include "Enums.h"
#include "ProbabilityTables.h"
#include "SyntaxElementCounter.h"

namespace Video::VP9 {

class Parser;

class TreeParser {
public:
    explicit TreeParser(Parser& decoder)
        : m_decoder(decoder)
    {
    }

    // FIXME: Move or remove this class once it is unused in the header.
    class TreeSelection {
    public:
        union TreeSelectionValue {
            int const* m_tree;
            int m_value;
        };

        constexpr TreeSelection(int const* values)
            : m_is_single_value(false)
            , m_value { .m_tree = values }
        {
        }

        constexpr TreeSelection(int value)
            : m_is_single_value(true)
            , m_value { .m_value = value }
        {
        }

        bool is_single_value() const { return m_is_single_value; }
        int single_value() const { return m_value.m_value; }
        int const* tree() const { return m_value.m_tree; }

    private:
        bool m_is_single_value;
        TreeSelectionValue m_value;
    };

    /* (9.3.3) */
    template<typename T = int>
    ErrorOr<T> parse_tree(SyntaxElementType type);
    /* (9.3.1) */
    TreeSelection select_tree(SyntaxElementType type);
    /* (9.3.2) */
    u8 select_tree_probability(SyntaxElementType type, u8 node);
    /* (9.3.4) */
    void count_syntax_element(SyntaxElementType type, int value);

    static ErrorOr<Partition> parse_partition(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, bool has_rows, bool has_columns, BlockSubsize block_subsize, u8 num_8x8, Vector<u8> const& above_partition_context, Vector<u8> const& left_partition_context, u32 row, u32 column, bool frame_is_intra);
    static ErrorOr<PredictionMode> parse_default_intra_mode(BitStream&, ProbabilityTables const&, BlockSubsize mi_size, Optional<Array<PredictionMode, 4> const&> above_context, Optional<Array<PredictionMode, 4> const&> left_context, PredictionMode block_sub_modes[4], u8 index_x, u8 index_y);
    static ErrorOr<PredictionMode> parse_default_uv_mode(BitStream&, ProbabilityTables const&, PredictionMode y_mode);
    static ErrorOr<PredictionMode> parse_intra_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, BlockSubsize mi_size);
    static ErrorOr<PredictionMode> parse_sub_intra_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&);
    static ErrorOr<PredictionMode> parse_uv_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, PredictionMode y_mode);
    static ErrorOr<u8> parse_segment_id(BitStream&, u8 const probabilities[7]);
    static ErrorOr<bool> parse_segment_id_predicted(BitStream&, u8 const probabilities[3], u8 above_seg_pred_context, u8 left_seg_pred_context);
    static ErrorOr<PredictionMode> parse_inter_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 mode_context_for_ref_frame_0);
    static ErrorOr<InterpolationFilter> parse_interpolation_filter(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, Optional<ReferenceFrameType> above_ref_frame, Optional<ReferenceFrameType> left_ref_frame, Optional<InterpolationFilter> above_interpolation_filter, Optional<InterpolationFilter> left_interpolation_filter);
    static ErrorOr<bool> parse_skip(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, Optional<bool> const& above_skip, Optional<bool> const& left_skip);
    static ErrorOr<TXSize> parse_tx_size(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, TXSize max_tx_size, Optional<bool> above_skip, Optional<bool> left_skip, Optional<TXSize> above_tx_size, Optional<TXSize> left_tx_size);
    static ErrorOr<bool> parse_is_inter(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, Optional<bool> above_intra, Optional<bool> left_intra);
    static ErrorOr<ReferenceMode> parse_comp_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, ReferenceFrameType comp_fixed_ref, Optional<bool> above_single, Optional<bool> left_single, Optional<bool> above_intra, Optional<bool> left_intra, Optional<ReferenceFrameType> above_ref_frame_0, Optional<ReferenceFrameType> left_ref_frame_0);
    static ErrorOr<bool> parse_comp_ref(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, ReferenceFrameType comp_fixed_ref, ReferenceFramePair comp_var_ref, Optional<bool> above_single, Optional<bool> left_single, Optional<bool> above_intra, Optional<bool> left_intra, Optional<ReferenceFrameType> above_ref_frame_0, Optional<ReferenceFrameType> left_ref_frame_0, Optional<ReferenceFrameType> above_ref_frame_biased, Optional<ReferenceFrameType> left_ref_frame_biased);
    static ErrorOr<bool> parse_single_ref_part_1(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, Optional<bool> above_single, Optional<bool> left_single, Optional<bool> above_intra, Optional<bool> left_intra, Optional<ReferenceFramePair> above_ref_frame, Optional<ReferenceFramePair> left_ref_frame);
    static ErrorOr<bool> parse_single_ref_part_2(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, Optional<bool> above_single, Optional<bool> left_single, Optional<bool> above_intra, Optional<bool> left_intra, Optional<ReferenceFramePair> above_ref_frame, Optional<ReferenceFramePair> left_ref_frame);

    void set_default_intra_mode_variables(u8 idx, u8 idy)
    {
        m_idx = idx;
        m_idy = idy;
    }

    void set_tokens_variables(u8 band, u32 c, u32 plane, TXSize tx_size, u32 pos);

    void set_start_x_and_y(u32 start_x, u32 start_y)
    {
        m_start_x = start_x;
        m_start_y = start_y;
    }

    void set_mv_component(u8 component)
    {
        m_mv_component = component;
    }

    ErrorOr<bool> parse_mv_bit(u8 bit)
    {
        m_mv_bit = bit;
        return parse_tree<bool>(SyntaxElementType::MVBit);
    }

    ErrorOr<u8> parse_mv_class0_fr(bool mv_class0_bit)
    {
        m_mv_class0_bit = mv_class0_bit;
        return parse_tree<u8>(SyntaxElementType::MVClass0FR);
    }

private:
    u8 calculate_token_probability(u8 node);
    u8 calculate_more_coefs_probability();

    Parser& m_decoder;
    // m_ctx is a member variable because it is required for syntax element counting (section 9.3.4)
    u8 m_ctx { 0 };

    // These are variables necessary for parsing tree data, but aren't useful otherwise, so they're
    // not stored in the Decoder itself.
    u8 m_idx { 0 };
    u8 m_idy { 0 };
    u8 m_band { 0 };
    u32 m_start_x { 0 };
    u32 m_start_y { 0 };
    u32 m_c { 0 };
    u32 m_plane { 0 };
    TXSize m_tx_size;
    u32 m_pos { 0 };
    u8 m_mv_component { 0 };
    // 0xFF indicates the value has not been set.
    // parse_mv_bit should be called to set this.
    u8 m_mv_bit { 0xFF };
    // 0xFF indicates the value has not been set.
    // parse_mv_class0_fr should be called to set this.
    u8 m_mv_class0_bit { 0xFF };
};

struct PartitionTreeContext {
    bool has_rows;
    bool has_columns;
    BlockSubsize block_subsize;
    u8 num_8x8;
    Vector<u8> const& above_partition_context;
    Vector<u8> const& left_partition_context;
    u32 row;
    u32 column;
    bool frame_is_intra;
};

}
