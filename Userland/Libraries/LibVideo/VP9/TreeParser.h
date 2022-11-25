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

    static ErrorOr<Partition> parse_partition(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, bool has_rows, bool has_columns, BlockSubsize block_subsize, u8 num_8x8, Vector<u8> const& above_partition_context, Vector<u8> const& left_partition_context, u32 row, u32 column, bool frame_is_intra);
    static ErrorOr<PredictionMode> parse_default_intra_mode(BitStream&, ProbabilityTables const&, BlockSubsize mi_size, FrameBlockContext above, FrameBlockContext left, Array<PredictionMode, 4> const& block_sub_modes, u8 index_x, u8 index_y);
    static ErrorOr<PredictionMode> parse_default_uv_mode(BitStream&, ProbabilityTables const&, PredictionMode y_mode);
    static ErrorOr<PredictionMode> parse_intra_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, BlockSubsize mi_size);
    static ErrorOr<PredictionMode> parse_sub_intra_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&);
    static ErrorOr<PredictionMode> parse_uv_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, PredictionMode y_mode);
    static ErrorOr<u8> parse_segment_id(BitStream&, u8 const probabilities[7]);
    static ErrorOr<bool> parse_segment_id_predicted(BitStream&, u8 const probabilities[3], u8 above_seg_pred_context, u8 left_seg_pred_context);
    static ErrorOr<PredictionMode> parse_inter_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 mode_context_for_ref_frame_0);
    static ErrorOr<InterpolationFilter> parse_interpolation_filter(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, FrameBlockContext above, FrameBlockContext left);
    static ErrorOr<bool> parse_skip(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, FrameBlockContext above, FrameBlockContext left);
    static ErrorOr<TXSize> parse_tx_size(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, TXSize max_tx_size, FrameBlockContext above, FrameBlockContext left);
    static ErrorOr<bool> parse_block_is_inter_predicted(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, FrameBlockContext above, FrameBlockContext left);
    static ErrorOr<ReferenceMode> parse_comp_mode(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, ReferenceFrameType comp_fixed_ref, FrameBlockContext above, FrameBlockContext left);
    static ErrorOr<ReferenceIndex> parse_comp_ref(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, ReferenceFrameType comp_fixed_ref, ReferenceFramePair comp_var_ref, ReferenceIndex variable_reference_index, FrameBlockContext above, FrameBlockContext left);
    static ErrorOr<bool> parse_single_ref_part_1(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, FrameBlockContext above, FrameBlockContext left);
    static ErrorOr<bool> parse_single_ref_part_2(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, FrameBlockContext above, FrameBlockContext left);

    static ErrorOr<MvJoint> parse_motion_vector_joint(BitStream&, ProbabilityTables const&, SyntaxElementCounter&);
    static ErrorOr<bool> parse_motion_vector_sign(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 component);
    static ErrorOr<MvClass> parse_motion_vector_class(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 component);
    static ErrorOr<bool> parse_motion_vector_class0_bit(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 component);
    static ErrorOr<u8> parse_motion_vector_class0_fr(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 component, bool class_0_bit);
    static ErrorOr<bool> parse_motion_vector_class0_hp(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 component, bool use_hp);
    static ErrorOr<bool> parse_motion_vector_bit(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 component, u8 bit_index);
    static ErrorOr<u8> parse_motion_vector_fr(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 component);
    static ErrorOr<bool> parse_motion_vector_hp(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, u8 component, bool use_hp);

    static TokensContext get_tokens_context(bool subsampling_x, bool subsampling_y, u32 rows, u32 columns, Array<Vector<bool>, 3> const& above_nonzero_context, Array<Vector<bool>, 3> const& left_nonzero_context, u8 token_cache[1024], TXSize tx_size, u8 tx_type, u8 plane, u32 start_x, u32 start_y, u32 position, bool is_inter, u8 band, u32 c);
    static ErrorOr<bool> parse_more_coefficients(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, TokensContext const& context);
    static ErrorOr<Token> parse_token(BitStream&, ProbabilityTables const&, SyntaxElementCounter&, TokensContext const& context);
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
