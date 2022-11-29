/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGfx/Point.h>
#include <LibGfx/Size.h>

#include "Decoder.h"
#include "Parser.h"
#include "Utilities.h"

namespace Video::VP9 {

#define TRY_READ(expression) DECODER_TRY(DecoderErrorCategory::Corrupted, expression)

Parser::Parser(Decoder& decoder)
    : m_probability_tables(make<ProbabilityTables>())
    , m_decoder(decoder)
{
}

Parser::~Parser()
{
}

Vector<size_t> Parser::parse_superframe_sizes(ReadonlyBytes frame_data)
{
    if (frame_data.size() < 1)
        return {};

    // The decoder determines the presence of a superframe by:
    // 1. parsing the final byte of the chunk and checking that the superframe_marker equals 0b110,

    // If the checks in steps 1 and 3 both pass, then the chunk is determined to contain a superframe and each
    // frame in the superframe is passed to the decoding process in turn.
    // Otherwise, the chunk is determined to not contain a superframe, and the whole chunk is passed to the
    // decoding process.

    // NOTE: Reading from span data will be quicker than spinning up a BitStream.
    u8 superframe_byte = frame_data[frame_data.size() - 1];

    // NOTE: We have to read out of the byte from the little end first, hence the padding bits in the masks below.
    u8 superframe_marker = superframe_byte & 0b1110'0000;
    if (superframe_marker == 0b1100'0000) {
        u8 bytes_per_framesize = ((superframe_byte >> 3) & 0b11) + 1;
        u8 frames_in_superframe = (superframe_byte & 0b111) + 1;
        // 2. setting the total size of the superframe_index SzIndex equal to 2 + NumFrames * SzBytes,
        size_t index_size = 2 + bytes_per_framesize * frames_in_superframe;

        if (index_size > frame_data.size())
            return {};

        auto superframe_header_data = frame_data.data() + frame_data.size() - index_size;

        u8 start_superframe_byte = *(superframe_header_data++);
        // 3. checking that the first byte of the superframe_index matches the final byte.
        if (superframe_byte != start_superframe_byte)
            return {};

        Vector<size_t> result;
        for (u8 i = 0; i < frames_in_superframe; i++) {
            size_t frame_size = 0;
            for (u8 j = 0; j < bytes_per_framesize; j++)
                frame_size |= (static_cast<size_t>(*(superframe_header_data++)) << (j * 8));
            result.append(frame_size);
        }
        return result;
    }

    return {};
}

/* (6.1) */
DecoderErrorOr<FrameContext> Parser::parse_frame(ReadonlyBytes frame_data)
{
    m_bit_stream = make<BitStream>(frame_data.data(), frame_data.size());
    m_syntax_element_counter = make<SyntaxElementCounter>();

    auto frame_context = TRY(uncompressed_header());
    if (!trailing_bits())
        return DecoderError::corrupted("Trailing bits were non-zero"sv);
    // FIXME: This should not be an error. Spec says that we consume padding bits until the end of the sample.
    if (frame_context.header_size_in_bytes == 0)
        return DecoderError::corrupted("Frame header is zero-sized"sv);
    m_probability_tables->load_probs(frame_context.probability_context_index);
    m_probability_tables->load_probs2(frame_context.probability_context_index);
    m_syntax_element_counter->clear_counts();

    TRY_READ(m_bit_stream->init_bool(frame_context.header_size_in_bytes));
    TRY(compressed_header(frame_context));
    TRY_READ(m_bit_stream->exit_bool());

    TRY(m_decoder.allocate_buffers(frame_context));

    TRY(decode_tiles(frame_context));
    TRY(refresh_probs(frame_context));

    m_previous_frame_type = frame_context.type;
    m_previous_frame_size = frame_context.size();
    m_previous_show_frame = frame_context.shows_a_frame();
    m_previous_color_config = frame_context.color_config;
    m_previous_loop_filter_ref_deltas = frame_context.loop_filter_reference_deltas;
    m_previous_loop_filter_mode_deltas = frame_context.loop_filter_mode_deltas;

    return frame_context;
}

bool Parser::trailing_bits()
{
    while (m_bit_stream->bits_remaining() & 7u) {
        if (MUST(m_bit_stream->read_bit()))
            return false;
    }
    return true;
}

DecoderErrorOr<void> Parser::refresh_probs(FrameContext const& frame_context)
{
    if (!frame_context.error_resilient_mode && !frame_context.parallel_decoding_mode) {
        m_probability_tables->load_probs(frame_context.probability_context_index);
        TRY(m_decoder.adapt_coef_probs(frame_context.is_inter_predicted()));
        if (frame_context.is_inter_predicted()) {
            m_probability_tables->load_probs2(frame_context.probability_context_index);
            TRY(m_decoder.adapt_non_coef_probs(frame_context));
        }
    }
    if (frame_context.should_replace_probability_context)
        m_probability_tables->save_probs(frame_context.probability_context_index);
    return {};
}

DecoderErrorOr<ColorRange> Parser::read_color_range()
{
    if (TRY_READ(m_bit_stream->read_bit()))
        return ColorRange::Full;
    return ColorRange::Studio;
}

/* (6.2) */
DecoderErrorOr<FrameContext> Parser::uncompressed_header()
{
    // NOTE: m_reusable_frame_block_contexts does not need to retain any data between frame decodes.
    //       This is only stored so that we don't need to allocate a frame's block contexts on each
    //       call to this function, since it will rarely change sizes.
    FrameContext frame_context { m_reusable_frame_block_contexts };
    frame_context.color_config = m_previous_color_config;

    auto frame_marker = TRY_READ(m_bit_stream->read_bits(2));
    if (frame_marker != 2)
        return DecoderError::corrupted("uncompressed_header: Frame marker must be 2"sv);

    auto profile_low_bit = TRY_READ(m_bit_stream->read_bit());
    auto profile_high_bit = TRY_READ(m_bit_stream->read_bit());
    frame_context.profile = (profile_high_bit << 1u) + profile_low_bit;
    if (frame_context.profile == 3 && TRY_READ(m_bit_stream->read_bit()))
        return DecoderError::corrupted("uncompressed_header: Profile 3 reserved bit was non-zero"sv);

    if (TRY_READ(m_bit_stream->read_bit())) {
        frame_context.set_existing_frame_to_show(TRY_READ(m_bit_stream->read_bits(3)));
        return frame_context;
    }

    bool is_keyframe = !TRY_READ(m_bit_stream->read_bit());

    if (!TRY_READ(m_bit_stream->read_bit()))
        frame_context.set_frame_hidden();

    frame_context.error_resilient_mode = TRY_READ(m_bit_stream->read_bit());

    FrameType type;

    Gfx::Size<u32> frame_size;
    Gfx::Size<u32> render_size;
    u8 reference_frames_to_update_flags = 0xFF; // Save frame to all reference indices by default.

    enum class ResetProbabilities : u8 {
        No = 0,
        // 1 also means No here, but we don't need to do anything with the No case.
        OnlyCurrent = 2,
        All = 3,
    };
    ResetProbabilities reset_frame_context = ResetProbabilities::All;

    if (is_keyframe) {
        type = FrameType::KeyFrame;
        TRY(frame_sync_code());
        frame_context.color_config = TRY(parse_color_config(frame_context));
        frame_size = TRY(parse_frame_size());
        render_size = TRY(parse_render_size(frame_size));
    } else {
        if (!frame_context.shows_a_frame() && TRY_READ(m_bit_stream->read_bit())) {
            type = FrameType::IntraOnlyFrame;
        } else {
            type = FrameType::InterFrame;
            reset_frame_context = ResetProbabilities::No;
        }

        if (!frame_context.error_resilient_mode)
            reset_frame_context = static_cast<ResetProbabilities>(TRY_READ(m_bit_stream->read_bits(2)));

        if (type == FrameType::IntraOnlyFrame) {
            TRY(frame_sync_code());

            frame_context.color_config = frame_context.profile > 0 ? TRY(parse_color_config(frame_context)) : ColorConfig();

            reference_frames_to_update_flags = TRY_READ(m_bit_stream->read_f8());
            frame_size = TRY(parse_frame_size());
            render_size = TRY(parse_render_size(frame_size));
        } else {
            reference_frames_to_update_flags = TRY_READ(m_bit_stream->read_f8());
            for (auto i = 0; i < 3; i++) {
                frame_context.reference_frame_indices[i] = TRY_READ(m_bit_stream->read_bits(3));
                frame_context.reference_frame_sign_biases[LastFrame + i] = TRY_READ(m_bit_stream->read_bit());
            }
            frame_size = TRY(parse_frame_size_with_refs(frame_context.reference_frame_indices));
            render_size = TRY(parse_render_size(frame_size));
            frame_context.high_precision_motion_vectors_allowed = TRY_READ(m_bit_stream->read_bit());
            frame_context.interpolation_filter = TRY(read_interpolation_filter());
        }
    }

    bool should_replace_probability_context = false;
    bool parallel_decoding_mode = true;
    if (!frame_context.error_resilient_mode) {
        should_replace_probability_context = TRY_READ(m_bit_stream->read_bit());
        parallel_decoding_mode = TRY_READ(m_bit_stream->read_bit());
    }

    u8 probability_context_index = TRY_READ(m_bit_stream->read_bits(2));
    switch (reset_frame_context) {
    case ResetProbabilities::All:
        setup_past_independence();
        for (auto i = 0; i < 4; i++) {
            m_probability_tables->save_probs(i);
        }
        probability_context_index = 0;
        break;
    case ResetProbabilities::OnlyCurrent:
        setup_past_independence();
        m_probability_tables->save_probs(probability_context_index);
        probability_context_index = 0;
        break;
    default:
        break;
    }

    frame_context.type = type;
    DECODER_TRY_ALLOC(frame_context.set_size(frame_size));
    frame_context.render_size = render_size;
    TRY(compute_image_size(frame_context));

    frame_context.reference_frames_to_update_flags = reference_frames_to_update_flags;
    frame_context.parallel_decoding_mode = parallel_decoding_mode;

    frame_context.should_replace_probability_context = should_replace_probability_context;
    frame_context.probability_context_index = probability_context_index;

    TRY(loop_filter_params(frame_context));
    TRY(quantization_params(frame_context));
    TRY(segmentation_params());
    TRY(parse_tile_counts(frame_context));

    frame_context.header_size_in_bytes = TRY_READ(m_bit_stream->read_f16());

    return frame_context;
}

DecoderErrorOr<void> Parser::frame_sync_code()
{
    if (TRY_READ(m_bit_stream->read_f8()) != 0x49)
        return DecoderError::corrupted("frame_sync_code: Byte 0 was not 0x49."sv);
    if (TRY_READ(m_bit_stream->read_f8()) != 0x83)
        return DecoderError::corrupted("frame_sync_code: Byte 1 was not 0x83."sv);
    if (TRY_READ(m_bit_stream->read_f8()) != 0x42)
        return DecoderError::corrupted("frame_sync_code: Byte 2 was not 0x42."sv);
    return {};
}

DecoderErrorOr<ColorConfig> Parser::parse_color_config(FrameContext const& frame_context)
{
    // (6.2.2) color_config( )
    u8 bit_depth;
    if (frame_context.profile >= 2) {
        bit_depth = TRY_READ(m_bit_stream->read_bit()) ? 12 : 10;
    } else {
        bit_depth = 8;
    }

    auto color_space = static_cast<ColorSpace>(TRY_READ(m_bit_stream->read_bits(3)));
    VERIFY(color_space <= ColorSpace::RGB);

    ColorRange color_range;
    bool subsampling_x, subsampling_y;

    if (color_space != ColorSpace::RGB) {
        color_range = TRY(read_color_range());
        if (frame_context.profile == 1 || frame_context.profile == 3) {
            subsampling_x = TRY_READ(m_bit_stream->read_bit());
            subsampling_y = TRY_READ(m_bit_stream->read_bit());
            if (TRY_READ(m_bit_stream->read_bit()))
                return DecoderError::corrupted("color_config: Subsampling reserved zero was set"sv);
        } else {
            subsampling_x = true;
            subsampling_y = true;
        }
    } else {
        color_range = ColorRange::Full;
        if (frame_context.profile == 1 || frame_context.profile == 3) {
            subsampling_x = false;
            subsampling_y = false;
            if (TRY_READ(m_bit_stream->read_bit()))
                return DecoderError::corrupted("color_config: RGB reserved zero was set"sv);
        } else {
            // FIXME: Spec does not specify the subsampling value here. Is this an error or should we set a default?
            VERIFY_NOT_REACHED();
        }
    }

    return ColorConfig { bit_depth, color_space, color_range, subsampling_x, subsampling_y };
}

DecoderErrorOr<Gfx::Size<u32>> Parser::parse_frame_size()
{
    return Gfx::Size<u32> { TRY_READ(m_bit_stream->read_f16()) + 1, TRY_READ(m_bit_stream->read_f16()) + 1 };
}

DecoderErrorOr<Gfx::Size<u32>> Parser::parse_render_size(Gfx::Size<u32> frame_size)
{
    // FIXME: This function should save this bit as a value in the FrameContext. The bit can be
    //        used in files where the pixel aspect ratio changes between samples in the video.
    //        If the bit is set, the pixel aspect ratio should be recalculated, whereas if only
    //        the frame size has changed and the render size is unadjusted, then the pixel aspect
    //        ratio should be retained and the new render size determined based on that.
    //        See the Firefox source code here:
    //        https://searchfox.org/mozilla-central/source/dom/media/platforms/wrappers/MediaChangeMonitor.cpp#268-276
    if (!TRY_READ(m_bit_stream->read_bit()))
        return frame_size;
    return Gfx::Size<u32> { TRY_READ(m_bit_stream->read_f16()) + 1, TRY_READ(m_bit_stream->read_f16()) + 1 };
}

DecoderErrorOr<Gfx::Size<u32>> Parser::parse_frame_size_with_refs(Array<u8, 3> const& reference_indices)
{
    Optional<Gfx::Size<u32>> size;
    for (auto frame_index : reference_indices) {
        if (TRY_READ(m_bit_stream->read_bit())) {
            size.emplace(m_ref_frame_size[frame_index]);
            break;
        }
    }

    if (size.has_value())
        return size.value();

    return TRY(parse_frame_size());
}

DecoderErrorOr<void> Parser::compute_image_size(FrameContext& frame_context)
{
    // 7.2.6 Compute image size semantics
    // When compute_image_size is invoked, the following ordered steps occur:
    // 1. If this is the first time compute_image_size is invoked, or if either FrameWidth or FrameHeight have
    // changed in value compared to the previous time this function was invoked, then the segmentation map is
    // cleared to all zeros by setting SegmentId[ row ][ col ] equal to 0 for row = 0..MiRows-1 and col =
    // 0..MiCols-1.
    // FIXME: What does this mean? SegmentIds is scoped to one frame, so it will not contain values here. It's
    //        also suspicious that spec refers to this as SegmentId rather than SegmentIds (plural). Is this
    //        supposed to refer to PrevSegmentIds?
    bool first_invoke = m_is_first_compute_image_size_invoke;
    m_is_first_compute_image_size_invoke = false;
    bool same_size = m_previous_frame_size == frame_context.size();

    // 2. The variable UsePrevFrameMvs is set equal to 1 if all of the following conditions are true:
    // a. This is not the first time compute_image_size is invoked.
    // b. Both FrameWidth and FrameHeight have the same value compared to the previous time this function
    // was invoked.
    // c. show_frame was equal to 1 the previous time this function was invoked.
    // d. error_resilient_mode is equal to 0.
    // e. FrameIsIntra is equal to 0.
    // Otherwise, UsePrevFrameMvs is set equal to 0.
    m_use_prev_frame_mvs = !first_invoke && same_size && m_previous_show_frame && !frame_context.error_resilient_mode && frame_context.is_inter_predicted();
    return {};
}

DecoderErrorOr<InterpolationFilter> Parser::read_interpolation_filter()
{
    if (TRY_READ(m_bit_stream->read_bit())) {
        return InterpolationFilter::Switchable;
    }
    return literal_to_type[TRY_READ(m_bit_stream->read_bits(2))];
}

DecoderErrorOr<void> Parser::loop_filter_params(FrameContext& frame_context)
{
    frame_context.loop_filter_level = TRY_READ(m_bit_stream->read_bits(6));
    frame_context.loop_filter_sharpness = TRY_READ(m_bit_stream->read_bits(3));
    frame_context.loop_filter_delta_enabled = TRY_READ(m_bit_stream->read_bit());

    auto reference_deltas = m_previous_loop_filter_ref_deltas;
    auto mode_deltas = m_previous_loop_filter_mode_deltas;
    if (frame_context.loop_filter_delta_enabled && TRY_READ(m_bit_stream->read_bit())) {
        for (auto& loop_filter_ref_delta : reference_deltas) {
            if (TRY_READ(m_bit_stream->read_bit()))
                loop_filter_ref_delta = TRY_READ(m_bit_stream->read_s(6));
        }
        for (auto& loop_filter_mode_delta : mode_deltas) {
            if (TRY_READ(m_bit_stream->read_bit()))
                loop_filter_mode_delta = TRY_READ(m_bit_stream->read_s(6));
        }
    }
    frame_context.loop_filter_reference_deltas = reference_deltas;
    frame_context.loop_filter_mode_deltas = mode_deltas;

    return {};
}

DecoderErrorOr<void> Parser::quantization_params(FrameContext& frame_context)
{
    frame_context.base_quantizer_index = TRY_READ(m_bit_stream->read_f8());
    frame_context.y_dc_quantizer_index_delta = TRY(read_delta_q());
    frame_context.uv_dc_quantizer_index_delta = TRY(read_delta_q());
    frame_context.uv_ac_quantizer_index_delta = TRY(read_delta_q());
    return {};
}

DecoderErrorOr<i8> Parser::read_delta_q()
{
    if (TRY_READ(m_bit_stream->read_bit()))
        return TRY_READ(m_bit_stream->read_s(4));
    return 0;
}

DecoderErrorOr<void> Parser::segmentation_params()
{
    m_segmentation_enabled = TRY_READ(m_bit_stream->read_bit());
    if (!m_segmentation_enabled)
        return {};

    m_segmentation_update_map = TRY_READ(m_bit_stream->read_bit());
    if (m_segmentation_update_map) {
        for (auto& segmentation_tree_prob : m_segmentation_tree_probs)
            segmentation_tree_prob = TRY(read_prob());
        m_segmentation_temporal_update = TRY_READ(m_bit_stream->read_bit());
        for (auto& segmentation_pred_prob : m_segmentation_pred_prob)
            segmentation_pred_prob = m_segmentation_temporal_update ? TRY(read_prob()) : 255;
    }

    auto segmentation_update_data = (TRY_READ(m_bit_stream->read_bit()));

    if (!segmentation_update_data)
        return {};

    m_segmentation_abs_or_delta_update = TRY_READ(m_bit_stream->read_bit());
    for (auto i = 0; i < MAX_SEGMENTS; i++) {
        for (auto j = 0; j < SEG_LVL_MAX; j++) {
            auto feature_value = 0;
            auto feature_enabled = TRY_READ(m_bit_stream->read_bit());
            m_feature_enabled[i][j] = feature_enabled;
            if (feature_enabled) {
                auto bits_to_read = segmentation_feature_bits[j];
                feature_value = TRY_READ(m_bit_stream->read_bits(bits_to_read));
                if (segmentation_feature_signed[j]) {
                    if (TRY_READ(m_bit_stream->read_bit()))
                        feature_value = -feature_value;
                }
            }
            m_feature_data[i][j] = feature_value;
        }
    }

    return {};
}

DecoderErrorOr<u8> Parser::read_prob()
{
    if (TRY_READ(m_bit_stream->read_bit()))
        return TRY_READ(m_bit_stream->read_f8());
    return 255;
}

static u16 calc_min_log2_of_tile_columns(u32 superblock_columns)
{
    auto min_log_2 = 0u;
    while ((u32)(MAX_TILE_WIDTH_B64 << min_log_2) < superblock_columns)
        min_log_2++;
    return min_log_2;
}

static u16 calc_max_log2_tile_cols(u32 superblock_columns)
{
    u16 max_log_2 = 1;
    while ((superblock_columns >> max_log_2) >= MIN_TILE_WIDTH_B64)
        max_log_2++;
    return max_log_2 - 1;
}

DecoderErrorOr<void> Parser::parse_tile_counts(FrameContext& frame_context)
{
    auto superblock_columns = frame_context.superblock_columns();

    auto log2_of_tile_columns = calc_min_log2_of_tile_columns(superblock_columns);
    auto log2_of_tile_columns_maximum = calc_max_log2_tile_cols(superblock_columns);
    while (log2_of_tile_columns < log2_of_tile_columns_maximum) {
        if (TRY_READ(m_bit_stream->read_bit()))
            log2_of_tile_columns++;
        else
            break;
    }

    u16 log2_of_tile_rows = TRY_READ(m_bit_stream->read_bit());
    if (log2_of_tile_rows > 0) {
        log2_of_tile_rows += TRY_READ(m_bit_stream->read_bit());
    }
    frame_context.log2_of_tile_counts = Gfx::Size<u16>(log2_of_tile_columns, log2_of_tile_rows);
    return {};
}

void Parser::setup_past_independence()
{
    for (auto i = 0; i < 8; i++) {
        for (auto j = 0; j < 4; j++) {
            m_feature_data[i][j] = 0;
            m_feature_enabled[i][j] = false;
        }
    }
    m_previous_block_contexts.reset();
    m_segmentation_abs_or_delta_update = false;
    m_previous_loop_filter_ref_deltas[IntraFrame] = 1;
    m_previous_loop_filter_ref_deltas[LastFrame] = 0;
    m_previous_loop_filter_ref_deltas[GoldenFrame] = -1;
    m_previous_loop_filter_ref_deltas[AltRefFrame] = -1;
    m_previous_loop_filter_mode_deltas.fill(0);
    m_probability_tables->reset_probs();
}

DecoderErrorOr<void> Parser::compressed_header(FrameContext& frame_context)
{
    frame_context.transform_mode = TRY(read_tx_mode(frame_context));
    if (frame_context.transform_mode == TXModeSelect)
        TRY(tx_mode_probs());
    TRY(read_coef_probs(frame_context.transform_mode));
    TRY(read_skip_prob());
    if (frame_context.is_inter_predicted()) {
        TRY(read_inter_mode_probs());
        if (frame_context.interpolation_filter == Switchable)
            TRY(read_interp_filter_probs());
        TRY(read_is_inter_probs());
        TRY(frame_reference_mode(frame_context));
        TRY(frame_reference_mode_probs());
        TRY(read_y_mode_probs());
        TRY(read_partition_probs());
        TRY(mv_probs(frame_context));
    }
    return {};
}

DecoderErrorOr<TXMode> Parser::read_tx_mode(FrameContext const& frame_context)
{
    if (frame_context.is_lossless()) {
        return TXMode::Only_4x4;
    }

    auto tx_mode = TRY_READ(m_bit_stream->read_literal(2));
    if (tx_mode == Allow_32x32)
        tx_mode += TRY_READ(m_bit_stream->read_literal(1));
    return static_cast<TXMode>(tx_mode);
}

DecoderErrorOr<void> Parser::tx_mode_probs()
{
    auto& tx_probs = m_probability_tables->tx_probs();
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 3; j++)
            tx_probs[TX_8x8][i][j] = TRY(diff_update_prob(tx_probs[TX_8x8][i][j]));
    }
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 2; j++)
            tx_probs[TX_16x16][i][j] = TRY(diff_update_prob(tx_probs[TX_16x16][i][j]));
    }
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 1; j++)
            tx_probs[TX_32x32][i][j] = TRY(diff_update_prob(tx_probs[TX_32x32][i][j]));
    }
    return {};
}

DecoderErrorOr<u8> Parser::diff_update_prob(u8 prob)
{
    auto update_prob = TRY_READ(m_bit_stream->read_bool(252));
    if (update_prob) {
        auto delta_prob = TRY(decode_term_subexp());
        prob = inv_remap_prob(delta_prob, prob);
    }
    return prob;
}

DecoderErrorOr<u8> Parser::decode_term_subexp()
{
    if (TRY_READ(m_bit_stream->read_literal(1)) == 0)
        return TRY_READ(m_bit_stream->read_literal(4));
    if (TRY_READ(m_bit_stream->read_literal(1)) == 0)
        return TRY_READ(m_bit_stream->read_literal(4)) + 16;
    if (TRY_READ(m_bit_stream->read_literal(1)) == 0)
        return TRY_READ(m_bit_stream->read_literal(5)) + 32;

    auto v = TRY_READ(m_bit_stream->read_literal(7));
    if (v < 65)
        return v + 64;
    return (v << 1u) - 1 + TRY_READ(m_bit_stream->read_literal(1));
}

u8 Parser::inv_remap_prob(u8 delta_prob, u8 prob)
{
    u8 m = prob - 1;
    auto v = inv_map_table[delta_prob];
    if ((m << 1u) <= 255)
        return 1 + inv_recenter_nonneg(v, m);
    return 255 - inv_recenter_nonneg(v, 254 - m);
}

u8 Parser::inv_recenter_nonneg(u8 v, u8 m)
{
    if (v > 2 * m)
        return v;
    if (v & 1u)
        return m - ((v + 1u) >> 1u);
    return m + (v >> 1u);
}

DecoderErrorOr<void> Parser::read_coef_probs(TXMode transform_mode)
{
    auto max_tx_size = tx_mode_to_biggest_tx_size[transform_mode];
    for (u8 tx_size = 0; tx_size <= max_tx_size; tx_size++) {
        auto update_probs = TRY_READ(m_bit_stream->read_literal(1));
        if (update_probs == 1) {
            for (auto i = 0; i < 2; i++) {
                for (auto j = 0; j < 2; j++) {
                    for (auto k = 0; k < 6; k++) {
                        auto max_l = (k == 0) ? 3 : 6;
                        for (auto l = 0; l < max_l; l++) {
                            for (auto m = 0; m < 3; m++) {
                                auto& prob = m_probability_tables->coef_probs()[tx_size][i][j][k][l][m];
                                prob = TRY(diff_update_prob(prob));
                            }
                        }
                    }
                }
            }
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::read_skip_prob()
{
    for (auto i = 0; i < SKIP_CONTEXTS; i++)
        m_probability_tables->skip_prob()[i] = TRY(diff_update_prob(m_probability_tables->skip_prob()[i]));
    return {};
}

DecoderErrorOr<void> Parser::read_inter_mode_probs()
{
    for (auto i = 0; i < INTER_MODE_CONTEXTS; i++) {
        for (auto j = 0; j < INTER_MODES - 1; j++)
            m_probability_tables->inter_mode_probs()[i][j] = TRY(diff_update_prob(m_probability_tables->inter_mode_probs()[i][j]));
    }
    return {};
}

DecoderErrorOr<void> Parser::read_interp_filter_probs()
{
    for (auto i = 0; i < INTERP_FILTER_CONTEXTS; i++) {
        for (auto j = 0; j < SWITCHABLE_FILTERS - 1; j++)
            m_probability_tables->interp_filter_probs()[i][j] = TRY(diff_update_prob(m_probability_tables->interp_filter_probs()[i][j]));
    }
    return {};
}

DecoderErrorOr<void> Parser::read_is_inter_probs()
{
    for (auto i = 0; i < IS_INTER_CONTEXTS; i++)
        m_probability_tables->is_inter_prob()[i] = TRY(diff_update_prob(m_probability_tables->is_inter_prob()[i]));
    return {};
}

DecoderErrorOr<void> Parser::frame_reference_mode(FrameContext& frame_context)
{
    // FIXME: These fields and the ones set in setup_compound_reference_mode should probably be contained by a field,
    //        since they are all used to set the reference frames later in one function (I think).
    auto compound_reference_allowed = false;
    for (size_t i = 2; i <= REFS_PER_FRAME; i++) {
        if (frame_context.reference_frame_sign_biases[i] != frame_context.reference_frame_sign_biases[1])
            compound_reference_allowed = true;
    }
    if (compound_reference_allowed) {
        auto non_single_reference = TRY_READ(m_bit_stream->read_literal(1));
        if (non_single_reference == 0) {
            m_reference_mode = SingleReference;
        } else {
            auto reference_select = TRY_READ(m_bit_stream->read_literal(1));
            if (reference_select == 0)
                m_reference_mode = CompoundReference;
            else
                m_reference_mode = ReferenceModeSelect;
            setup_compound_reference_mode(frame_context);
        }
    } else {
        m_reference_mode = SingleReference;
    }
    return {};
}

DecoderErrorOr<void> Parser::frame_reference_mode_probs()
{
    if (m_reference_mode == ReferenceModeSelect) {
        for (auto i = 0; i < COMP_MODE_CONTEXTS; i++) {
            auto& comp_mode_prob = m_probability_tables->comp_mode_prob();
            comp_mode_prob[i] = TRY(diff_update_prob(comp_mode_prob[i]));
        }
    }
    if (m_reference_mode != CompoundReference) {
        for (auto i = 0; i < REF_CONTEXTS; i++) {
            auto& single_ref_prob = m_probability_tables->single_ref_prob();
            single_ref_prob[i][0] = TRY(diff_update_prob(single_ref_prob[i][0]));
            single_ref_prob[i][1] = TRY(diff_update_prob(single_ref_prob[i][1]));
        }
    }
    if (m_reference_mode != SingleReference) {
        for (auto i = 0; i < REF_CONTEXTS; i++) {
            auto& comp_ref_prob = m_probability_tables->comp_ref_prob();
            comp_ref_prob[i] = TRY(diff_update_prob(comp_ref_prob[i]));
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::read_y_mode_probs()
{
    for (auto i = 0; i < BLOCK_SIZE_GROUPS; i++) {
        for (auto j = 0; j < INTRA_MODES - 1; j++) {
            auto& y_mode_probs = m_probability_tables->y_mode_probs();
            y_mode_probs[i][j] = TRY(diff_update_prob(y_mode_probs[i][j]));
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::read_partition_probs()
{
    for (auto i = 0; i < PARTITION_CONTEXTS; i++) {
        for (auto j = 0; j < PARTITION_TYPES - 1; j++) {
            auto& partition_probs = m_probability_tables->partition_probs();
            partition_probs[i][j] = TRY(diff_update_prob(partition_probs[i][j]));
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::mv_probs(FrameContext const& frame_context)
{
    for (auto j = 0; j < MV_JOINTS - 1; j++) {
        auto& mv_joint_probs = m_probability_tables->mv_joint_probs();
        mv_joint_probs[j] = TRY(update_mv_prob(mv_joint_probs[j]));
    }

    for (auto i = 0; i < 2; i++) {
        auto& mv_sign_prob = m_probability_tables->mv_sign_prob();
        mv_sign_prob[i] = TRY(update_mv_prob(mv_sign_prob[i]));
        for (auto j = 0; j < MV_CLASSES - 1; j++) {
            auto& mv_class_probs = m_probability_tables->mv_class_probs();
            mv_class_probs[i][j] = TRY(update_mv_prob(mv_class_probs[i][j]));
        }
        auto& mv_class0_bit_prob = m_probability_tables->mv_class0_bit_prob();
        mv_class0_bit_prob[i] = TRY(update_mv_prob(mv_class0_bit_prob[i]));
        for (auto j = 0; j < MV_OFFSET_BITS; j++) {
            auto& mv_bits_prob = m_probability_tables->mv_bits_prob();
            mv_bits_prob[i][j] = TRY(update_mv_prob(mv_bits_prob[i][j]));
        }
    }

    for (auto i = 0; i < 2; i++) {
        for (auto j = 0; j < CLASS0_SIZE; j++) {
            for (auto k = 0; k < MV_FR_SIZE - 1; k++) {
                auto& mv_class0_fr_probs = m_probability_tables->mv_class0_fr_probs();
                mv_class0_fr_probs[i][j][k] = TRY(update_mv_prob(mv_class0_fr_probs[i][j][k]));
            }
        }
        for (auto k = 0; k < MV_FR_SIZE - 1; k++) {
            auto& mv_fr_probs = m_probability_tables->mv_fr_probs();
            mv_fr_probs[i][k] = TRY(update_mv_prob(mv_fr_probs[i][k]));
        }
    }

    if (frame_context.high_precision_motion_vectors_allowed) {
        for (auto i = 0; i < 2; i++) {
            auto& mv_class0_hp_prob = m_probability_tables->mv_class0_hp_prob();
            auto& mv_hp_prob = m_probability_tables->mv_hp_prob();
            mv_class0_hp_prob[i] = TRY(update_mv_prob(mv_class0_hp_prob[i]));
            mv_hp_prob[i] = TRY(update_mv_prob(mv_hp_prob[i]));
        }
    }

    return {};
}

DecoderErrorOr<u8> Parser::update_mv_prob(u8 prob)
{
    if (TRY_READ(m_bit_stream->read_bool(252))) {
        return (TRY_READ(m_bit_stream->read_literal(7)) << 1u) | 1u;
    }
    return prob;
}

void Parser::setup_compound_reference_mode(FrameContext& frame_context)
{
    if (frame_context.reference_frame_sign_biases[LastFrame] == frame_context.reference_frame_sign_biases[GoldenFrame]) {
        m_comp_fixed_ref = AltRefFrame;
        m_comp_var_ref[0] = LastFrame;
        m_comp_var_ref[1] = GoldenFrame;
    } else if (frame_context.reference_frame_sign_biases[LastFrame] == frame_context.reference_frame_sign_biases[AltRefFrame]) {
        m_comp_fixed_ref = GoldenFrame;
        m_comp_var_ref[0] = LastFrame;
        m_comp_var_ref[1] = AltRefFrame;
    } else {
        m_comp_fixed_ref = LastFrame;
        m_comp_var_ref[0] = GoldenFrame;
        m_comp_var_ref[1] = AltRefFrame;
    }
}

DecoderErrorOr<void> Parser::decode_tiles(FrameContext& frame_context)
{
    auto log2_dimensions = frame_context.log2_of_tile_counts;
    auto tile_cols = 1 << log2_dimensions.width();
    auto tile_rows = 1 << log2_dimensions.height();
    clear_above_context(frame_context);

    for (auto tile_row = 0; tile_row < tile_rows; tile_row++) {
        for (auto tile_col = 0; tile_col < tile_cols; tile_col++) {
            auto last_tile = (tile_row == tile_rows - 1) && (tile_col == tile_cols - 1);
            u64 tile_size;
            if (last_tile)
                tile_size = m_bit_stream->bytes_remaining();
            else
                tile_size = TRY_READ(m_bit_stream->read_bits(32));

            auto rows_start = get_tile_offset(tile_row, frame_context.rows(), log2_dimensions.height());
            auto rows_end = get_tile_offset(tile_row + 1, frame_context.rows(), log2_dimensions.height());
            auto columns_start = get_tile_offset(tile_col, frame_context.columns(), log2_dimensions.width());
            auto columns_end = get_tile_offset(tile_col + 1, frame_context.columns(), log2_dimensions.width());

            auto tile_context = TileContext(frame_context, rows_start, rows_end, columns_start, columns_end);

            TRY_READ(m_bit_stream->init_bool(tile_size));
            TRY(decode_tile(tile_context));
            TRY_READ(m_bit_stream->exit_bool());
        }
    }
    return {};
}

template<typename T>
void Parser::clear_context(Vector<T>& context, size_t size)
{
    context.resize_and_keep_capacity(size);
    __builtin_memset(context.data(), 0, sizeof(T) * size);
}

template<typename T>
void Parser::clear_context(Vector<Vector<T>>& context, size_t outer_size, size_t inner_size)
{
    if (context.size() < outer_size)
        context.resize(outer_size);
    for (auto& sub_vector : context)
        clear_context(sub_vector, inner_size);
}

void Parser::clear_above_context(FrameContext& frame_context)
{
    for (auto i = 0u; i < m_above_nonzero_context.size(); i++)
        clear_context(m_above_nonzero_context[i], 2 * frame_context.columns());
    clear_context(m_above_seg_pred_context, frame_context.columns());
    clear_context(m_above_partition_context, frame_context.superblock_columns() * 8);
}

u32 Parser::get_tile_offset(u32 tile_num, u32 mis, u32 tile_size_log2)
{
    u32 super_blocks = (mis + 7) >> 3u;
    u32 offset = ((tile_num * super_blocks) >> tile_size_log2) << 3u;
    return min(offset, mis);
}

DecoderErrorOr<void> Parser::decode_tile(TileContext& tile_context)
{
    for (auto row = tile_context.rows_start; row < tile_context.rows_end; row += 8) {
        clear_left_context(tile_context);
        for (auto col = tile_context.columns_start; col < tile_context.columns_end; col += 8) {
            TRY(decode_partition(tile_context, row, col, Block_64x64));
        }
    }
    return {};
}

void Parser::clear_left_context(TileContext& tile_context)
{
    for (auto i = 0u; i < m_left_nonzero_context.size(); i++)
        clear_context(m_left_nonzero_context[i], 2 * tile_context.frame_context.rows());
    clear_context(m_left_seg_pred_context, tile_context.frame_context.rows());
    clear_context(m_left_partition_context, tile_context.frame_context.superblock_rows() * 8);
}

DecoderErrorOr<void> Parser::decode_partition(TileContext& tile_context, u32 row, u32 column, BlockSubsize subsize)
{
    if (row >= tile_context.frame_context.rows() || column >= tile_context.frame_context.columns())
        return {};
    u8 num_8x8 = num_8x8_blocks_wide_lookup[subsize];
    auto half_block_8x8 = num_8x8 >> 1;
    bool has_rows = (row + half_block_8x8) < tile_context.frame_context.rows();
    bool has_cols = (column + half_block_8x8) < tile_context.frame_context.columns();
    auto partition = TRY_READ(TreeParser::parse_partition(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, has_rows, has_cols, subsize, num_8x8, m_above_partition_context, m_left_partition_context, row, column, !tile_context.frame_context.is_inter_predicted()));

    auto child_subsize = subsize_lookup[partition][subsize];
    if (child_subsize < Block_8x8 || partition == PartitionNone) {
        TRY(decode_block(tile_context, row, column, child_subsize));
    } else if (partition == PartitionHorizontal) {
        TRY(decode_block(tile_context, row, column, child_subsize));
        if (has_rows)
            TRY(decode_block(tile_context, row + half_block_8x8, column, child_subsize));
    } else if (partition == PartitionVertical) {
        TRY(decode_block(tile_context, row, column, child_subsize));
        if (has_cols)
            TRY(decode_block(tile_context, row, column + half_block_8x8, child_subsize));
    } else {
        TRY(decode_partition(tile_context, row, column, child_subsize));
        TRY(decode_partition(tile_context, row, column + half_block_8x8, child_subsize));
        TRY(decode_partition(tile_context, row + half_block_8x8, column, child_subsize));
        TRY(decode_partition(tile_context, row + half_block_8x8, column + half_block_8x8, child_subsize));
    }
    if (subsize == Block_8x8 || partition != PartitionSplit) {
        auto above_context = 15 >> b_width_log2_lookup[child_subsize];
        auto left_context = 15 >> b_height_log2_lookup[child_subsize];
        for (size_t i = 0; i < num_8x8; i++) {
            m_above_partition_context[column + i] = above_context;
            m_left_partition_context[row + i] = left_context;
        }
    }
    return {};
}

size_t Parser::get_image_index(FrameContext const& frame_context, u32 row, u32 column) const
{
    VERIFY(row < frame_context.rows() && column < frame_context.columns());
    return row * frame_context.columns() + column;
}

DecoderErrorOr<void> Parser::decode_block(TileContext& tile_context, u32 row, u32 column, BlockSubsize subsize)
{
    auto above_context = row > 0 ? tile_context.frame_block_contexts().at(row - 1, column) : FrameBlockContext();
    auto left_context = column > tile_context.columns_start ? tile_context.frame_block_contexts().at(row, column - 1) : FrameBlockContext();
    auto block_context = BlockContext(tile_context, row, column, subsize);

    TRY(mode_info(block_context, above_context, left_context));
    auto had_residual_tokens = TRY(residual(block_context, above_context.is_available, left_context.is_available));
    if (block_context.is_inter_predicted() && subsize >= Block_8x8 && !had_residual_tokens)
        block_context.should_skip_residuals = true;

    for (size_t y = 0; y < block_context.contexts_view.height(); y++) {
        for (size_t x = 0; x < block_context.contexts_view.width(); x++) {
            auto sub_block_context = FrameBlockContext { true, block_context.should_skip_residuals, block_context.tx_size, block_context.y_prediction_mode(), block_context.sub_block_prediction_modes, block_context.interpolation_filter, block_context.reference_frame_types, block_context.sub_block_motion_vectors, block_context.segment_id };
            block_context.contexts_view.at(y, x) = sub_block_context;
            VERIFY(block_context.frame_block_contexts().at(row + y, column + x).tx_size == sub_block_context.tx_size);
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::mode_info(BlockContext& block_context, FrameBlockContext above_context, FrameBlockContext left_context)
{
    if (block_context.frame_context.is_inter_predicted())
        TRY(inter_frame_mode_info(block_context, above_context, left_context));
    else
        TRY(intra_frame_mode_info(block_context, above_context, left_context));
    return {};
}

DecoderErrorOr<void> Parser::intra_frame_mode_info(BlockContext& block_context, FrameBlockContext above_context, FrameBlockContext left_context)
{
    block_context.reference_frame_types = { ReferenceFrameType::None, ReferenceFrameType::None };
    VERIFY(!block_context.is_inter_predicted());
    TRY(set_intra_segment_id(block_context));
    block_context.should_skip_residuals = TRY(read_should_skip_residuals(block_context, above_context, left_context));
    block_context.tx_size = TRY(read_tx_size(block_context, above_context, left_context, true));
    // FIXME: This if statement is also present in parse_default_intra_mode. The selection of parameters for
    //        the probability table lookup should be inlined here.
    if (block_context.size >= Block_8x8) {
        auto mode = TRY_READ(TreeParser::parse_default_intra_mode(*m_bit_stream, *m_probability_tables, block_context.size, above_context, left_context, block_context.sub_block_prediction_modes, 0, 0));
        for (auto& block_sub_mode : block_context.sub_block_prediction_modes)
            block_sub_mode = mode;
    } else {
        auto size_in_4x4_blocks = block_context.get_size_in_4x4_blocks();
        for (auto idy = 0; idy < 2; idy += size_in_4x4_blocks.height()) {
            for (auto idx = 0; idx < 2; idx += size_in_4x4_blocks.width()) {
                auto sub_mode = TRY_READ(TreeParser::parse_default_intra_mode(*m_bit_stream, *m_probability_tables, block_context.size, above_context, left_context, block_context.sub_block_prediction_modes, idx, idy));

                for (auto y = 0; y < size_in_4x4_blocks.height(); y++) {
                    for (auto x = 0; x < size_in_4x4_blocks.width(); x++) {
                        auto index = (idy + y) * 2 + idx + x;
                        block_context.sub_block_prediction_modes[index] = sub_mode;
                    }
                }
            }
        }
    }
    block_context.uv_prediction_mode = TRY_READ(TreeParser::parse_default_uv_mode(*m_bit_stream, *m_probability_tables, block_context.y_prediction_mode()));
    return {};
}

DecoderErrorOr<void> Parser::set_intra_segment_id(BlockContext& block_context)
{
    if (m_segmentation_enabled && m_segmentation_update_map)
        block_context.segment_id = TRY_READ(TreeParser::parse_segment_id(*m_bit_stream, m_segmentation_tree_probs));
    else
        block_context.segment_id = 0;
    return {};
}

DecoderErrorOr<bool> Parser::read_should_skip_residuals(BlockContext& block_context, FrameBlockContext above_context, FrameBlockContext left_context)
{
    if (seg_feature_active(block_context, SEG_LVL_SKIP))
        return true;
    return TRY_READ(TreeParser::parse_skip(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, above_context, left_context));
}

bool Parser::seg_feature_active(BlockContext const& block_context, u8 feature)
{
    return m_segmentation_enabled && m_feature_enabled[block_context.segment_id][feature];
}

DecoderErrorOr<TXSize> Parser::read_tx_size(BlockContext& block_context, FrameBlockContext above_context, FrameBlockContext left_context, bool allow_select)
{
    auto max_tx_size = max_txsize_lookup[block_context.size];
    if (allow_select && block_context.frame_context.transform_mode == TXModeSelect && block_context.size >= Block_8x8)
        return (TRY_READ(TreeParser::parse_tx_size(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, max_tx_size, above_context, left_context)));
    return min(max_tx_size, tx_mode_to_biggest_tx_size[block_context.frame_context.transform_mode]);
}

DecoderErrorOr<void> Parser::inter_frame_mode_info(BlockContext& block_context, FrameBlockContext above_context, FrameBlockContext left_context)
{
    TRY(set_inter_segment_id(block_context));
    block_context.should_skip_residuals = TRY(read_should_skip_residuals(block_context, above_context, left_context));
    auto is_inter = TRY(read_is_inter(block_context, above_context, left_context));
    block_context.tx_size = TRY(read_tx_size(block_context, above_context, left_context, !block_context.should_skip_residuals || !is_inter));
    if (is_inter) {
        TRY(inter_block_mode_info(block_context, above_context, left_context));
    } else {
        TRY(intra_block_mode_info(block_context));
    }
    return {};
}

DecoderErrorOr<void> Parser::set_inter_segment_id(BlockContext& block_context)
{
    if (!m_segmentation_enabled) {
        block_context.segment_id = 0;
        return {};
    }
    auto predicted_segment_id = get_segment_id(block_context);
    if (!m_segmentation_update_map) {
        block_context.segment_id = predicted_segment_id;
        return {};
    }
    if (!m_segmentation_temporal_update) {
        block_context.segment_id = TRY_READ(TreeParser::parse_segment_id(*m_bit_stream, m_segmentation_tree_probs));
        return {};
    }

    auto seg_id_predicted = TRY_READ(TreeParser::parse_segment_id_predicted(*m_bit_stream, m_segmentation_pred_prob, m_left_seg_pred_context[block_context.row], m_above_seg_pred_context[block_context.column]));
    if (seg_id_predicted)
        block_context.segment_id = predicted_segment_id;
    else
        block_context.segment_id = TRY_READ(TreeParser::parse_segment_id(*m_bit_stream, m_segmentation_tree_probs));

    for (size_t i = 0; i < num_8x8_blocks_wide_lookup[block_context.size]; i++) {
        auto index = block_context.column + i;
        // (7.4.1) AboveSegPredContext[ i ] only needs to be set to 0 for i = 0..MiCols-1.
        if (index < m_above_seg_pred_context.size())
            m_above_seg_pred_context[index] = seg_id_predicted;
    }
    for (size_t i = 0; i < num_8x8_blocks_high_lookup[block_context.size]; i++) {
        auto index = block_context.row + i;
        // (7.4.1) LeftSegPredContext[ i ] only needs to be set to 0 for i = 0..MiRows-1.
        if (index < m_above_seg_pred_context.size())
            m_left_seg_pred_context[block_context.row + i] = seg_id_predicted;
    }
    return {};
}

u8 Parser::get_segment_id(BlockContext const& block_context)
{
    auto bw = num_8x8_blocks_wide_lookup[block_context.size];
    auto bh = num_8x8_blocks_high_lookup[block_context.size];
    auto xmis = min(block_context.frame_context.columns() - block_context.column, (u32)bw);
    auto ymis = min(block_context.frame_context.rows() - block_context.row, (u32)bh);
    u8 segment = 7;
    for (size_t y = 0; y < ymis; y++) {
        for (size_t x = 0; x < xmis; x++) {
            segment = min(segment, m_previous_block_contexts.index_at(block_context.row + y, block_context.column + x));
        }
    }
    return segment;
}

DecoderErrorOr<bool> Parser::read_is_inter(BlockContext& block_context, FrameBlockContext above_context, FrameBlockContext left_context)
{
    if (seg_feature_active(block_context, SEG_LVL_REF_FRAME))
        return m_feature_data[block_context.segment_id][SEG_LVL_REF_FRAME] != IntraFrame;
    return TRY_READ(TreeParser::parse_block_is_inter_predicted(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, above_context, left_context));
}

DecoderErrorOr<void> Parser::intra_block_mode_info(BlockContext& block_context)
{
    block_context.reference_frame_types = { ReferenceFrameType::None, ReferenceFrameType::None };
    VERIFY(!block_context.is_inter_predicted());
    auto& sub_modes = block_context.sub_block_prediction_modes;
    if (block_context.size >= Block_8x8) {
        auto mode = TRY_READ(TreeParser::parse_intra_mode(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, block_context.size));
        for (auto& block_sub_mode : sub_modes)
            block_sub_mode = mode;
    } else {
        auto size_in_4x4_blocks = block_context.get_size_in_4x4_blocks();
        for (auto idy = 0; idy < 2; idy += size_in_4x4_blocks.height()) {
            for (auto idx = 0; idx < 2; idx += size_in_4x4_blocks.width()) {
                auto sub_intra_mode = TRY_READ(TreeParser::parse_sub_intra_mode(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter));
                for (auto y = 0; y < size_in_4x4_blocks.height(); y++) {
                    for (auto x = 0; x < size_in_4x4_blocks.width(); x++)
                        sub_modes[(idy + y) * 2 + idx + x] = sub_intra_mode;
                }
            }
        }
    }
    block_context.uv_prediction_mode = TRY_READ(TreeParser::parse_uv_mode(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, block_context.y_prediction_mode()));
    return {};
}

static void select_best_reference_motion_vectors(BlockContext& block_context, MotionVectorPair reference_motion_vectors, BlockMotionVectorCandidates& candidates, u8 ref_list);

DecoderErrorOr<void> Parser::inter_block_mode_info(BlockContext& block_context, FrameBlockContext above_context, FrameBlockContext left_context)
{
    TRY(read_ref_frames(block_context, above_context, left_context));
    VERIFY(block_context.is_inter_predicted());
    BlockMotionVectorCandidates motion_vector_candidates;
    for (auto j = 0; j < 2; j++) {
        if (block_context.reference_frame_types[j] > IntraFrame) {
            auto reference_motion_vectors = find_reference_motion_vectors(block_context, block_context.reference_frame_types[j], -1);
            select_best_reference_motion_vectors(block_context, reference_motion_vectors, motion_vector_candidates, j);
        }
    }
    if (seg_feature_active(block_context, SEG_LVL_SKIP)) {
        block_context.y_prediction_mode() = PredictionMode::ZeroMv;
    } else if (block_context.size >= Block_8x8) {
        block_context.y_prediction_mode() = TRY_READ(TreeParser::parse_inter_mode(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, m_mode_context[block_context.reference_frame_types[0]]));
    }
    if (block_context.frame_context.interpolation_filter == Switchable)
        block_context.interpolation_filter = TRY_READ(TreeParser::parse_interpolation_filter(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, above_context, left_context));
    else
        block_context.interpolation_filter = block_context.frame_context.interpolation_filter;
    if (block_context.size < Block_8x8) {
        auto size_in_4x4_blocks = block_context.get_size_in_4x4_blocks();
        for (auto idy = 0; idy < 2; idy += size_in_4x4_blocks.height()) {
            for (auto idx = 0; idx < 2; idx += size_in_4x4_blocks.width()) {
                block_context.y_prediction_mode() = TRY_READ(TreeParser::parse_inter_mode(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, m_mode_context[block_context.reference_frame_types[0]]));
                if (block_context.y_prediction_mode() == PredictionMode::NearestMv || block_context.y_prediction_mode() == PredictionMode::NearMv) {
                    for (auto j = 0; j < 1 + block_context.is_compound(); j++)
                        select_best_sub_block_reference_motion_vectors(block_context, motion_vector_candidates, idy * 2 + idx, j);
                }
                auto new_motion_vector_pair = TRY(get_motion_vector(block_context, motion_vector_candidates));
                for (auto y = 0; y < size_in_4x4_blocks.height(); y++) {
                    for (auto x = 0; x < size_in_4x4_blocks.width(); x++) {
                        auto sub_block_index = (idy + y) * 2 + idx + x;
                        block_context.sub_block_motion_vectors[sub_block_index] = new_motion_vector_pair;
                    }
                }
            }
        }
        return {};
    }
    auto new_motion_vector_pair = TRY(get_motion_vector(block_context, motion_vector_candidates));
    for (auto block = 0; block < 4; block++)
        block_context.sub_block_motion_vectors[block] = new_motion_vector_pair;
    return {};
}

DecoderErrorOr<void> Parser::read_ref_frames(BlockContext& block_context, FrameBlockContext above_context, FrameBlockContext left_context)
{
    if (seg_feature_active(block_context, SEG_LVL_REF_FRAME)) {
        block_context.reference_frame_types = { static_cast<ReferenceFrameType>(m_feature_data[block_context.segment_id][SEG_LVL_REF_FRAME]), None };
        return {};
    }
    ReferenceMode comp_mode;
    if (m_reference_mode == ReferenceModeSelect)
        comp_mode = TRY_READ(TreeParser::parse_comp_mode(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, m_comp_fixed_ref, above_context, left_context));
    else
        comp_mode = m_reference_mode;
    if (comp_mode == CompoundReference) {
        // FIXME: Make reference frame pairs be indexed by an enum of FixedReference or VariableReference?
        auto fixed_reference_index = block_context.frame_context.reference_frame_sign_biases[m_comp_fixed_ref];
        auto variable_reference_index = !fixed_reference_index;

        // FIXME: Create an enum for compound frame references using names Primary and Secondary.
        auto comp_ref = TRY_READ(TreeParser::parse_comp_ref(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, m_comp_fixed_ref, m_comp_var_ref, variable_reference_index, above_context, left_context));

        block_context.reference_frame_types[fixed_reference_index] = m_comp_fixed_ref;
        block_context.reference_frame_types[variable_reference_index] = m_comp_var_ref[comp_ref];
        return {};
    }
    // FIXME: Maybe consolidate this into a tree. Context is different between part 1 and 2 but still, it would look nice here.
    auto single_ref_p1 = TRY_READ(TreeParser::parse_single_ref_part_1(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, above_context, left_context));
    if (single_ref_p1) {
        auto single_ref_p2 = TRY_READ(TreeParser::parse_single_ref_part_2(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, above_context, left_context));
        block_context.reference_frame_types[0] = single_ref_p2 ? AltRefFrame : GoldenFrame;
    } else {
        block_context.reference_frame_types[0] = LastFrame;
    }
    block_context.reference_frame_types[1] = None;
    return {};
}

// assign_mv( isCompound ) in the spec.
DecoderErrorOr<MotionVectorPair> Parser::get_motion_vector(BlockContext const& block_context, BlockMotionVectorCandidates const& candidates)
{
    MotionVectorPair result;
    for (auto i = 0; i < 1 + block_context.is_compound(); i++) {
        switch (block_context.y_prediction_mode()) {
        case PredictionMode::NewMv:
            result[i] = TRY(read_motion_vector(block_context, candidates, i));
            break;
        case PredictionMode::NearestMv:
            result[i] = candidates[i].nearest_vector;
            break;
        case PredictionMode::NearMv:
            result[i] = candidates[i].near_vector;
            break;
        default:
            result[i] = {};
            break;
        }
    }
    return result;
}

// use_mv_hp( deltaMv ) in the spec.
static bool should_use_high_precision_motion_vector(MotionVector const& delta_vector)
{
    return (abs(delta_vector.row()) >> 3) < COMPANDED_MVREF_THRESH && (abs(delta_vector.column()) >> 3) < COMPANDED_MVREF_THRESH;
}

// read_mv( ref ) in the spec.
DecoderErrorOr<MotionVector> Parser::read_motion_vector(BlockContext const& block_context, BlockMotionVectorCandidates const& candidates, u8 reference_index)
{
    m_use_hp = block_context.frame_context.high_precision_motion_vectors_allowed && should_use_high_precision_motion_vector(candidates[reference_index].best_vector);
    MotionVector diff_mv;
    auto mv_joint = TRY_READ(TreeParser::parse_motion_vector_joint(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter));
    if (mv_joint == MvJointHzvnz || mv_joint == MvJointHnzvnz)
        diff_mv.set_row(TRY(read_single_motion_vector_component(0)));
    if (mv_joint == MvJointHnzvz || mv_joint == MvJointHnzvnz)
        diff_mv.set_column(TRY(read_single_motion_vector_component(1)));

    return candidates[reference_index].best_vector + diff_mv;
}

// read_mv_component( comp ) in the spec.
DecoderErrorOr<i32> Parser::read_single_motion_vector_component(u8 component)
{
    auto mv_sign = TRY_READ(TreeParser::parse_motion_vector_sign(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, component));
    auto mv_class = TRY_READ(TreeParser::parse_motion_vector_class(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, component));
    u32 magnitude;
    if (mv_class == MvClass0) {
        auto mv_class0_bit = TRY_READ(TreeParser::parse_motion_vector_class0_bit(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, component));
        auto mv_class0_fr = TRY_READ(TreeParser::parse_motion_vector_class0_fr(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, component, mv_class0_bit));
        auto mv_class0_hp = TRY_READ(TreeParser::parse_motion_vector_class0_hp(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, component, m_use_hp));
        magnitude = ((mv_class0_bit << 3) | (mv_class0_fr << 1) | mv_class0_hp) + 1;
    } else {
        u32 bits = 0;
        for (u8 i = 0; i < mv_class; i++) {
            auto mv_bit = TRY_READ(TreeParser::parse_motion_vector_bit(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, component, i));
            bits |= mv_bit << i;
        }
        magnitude = CLASS0_SIZE << (mv_class + 2);
        auto mv_fr = TRY_READ(TreeParser::parse_motion_vector_fr(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, component));
        auto mv_hp = TRY_READ(TreeParser::parse_motion_vector_hp(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, component, m_use_hp));
        magnitude += ((bits << 3) | (mv_fr << 1) | mv_hp) + 1;
    }
    return (mv_sign ? -1 : 1) * static_cast<i32>(magnitude);
}

Gfx::Point<size_t> Parser::get_decoded_point_for_plane(FrameContext const& frame_context, u32 column, u32 row, u8 plane)
{
    (void)frame_context;
    if (plane == 0)
        return { column * 8, row * 8 };
    return { (column * 8) >> frame_context.color_config.subsampling_x, (row * 8) >> frame_context.color_config.subsampling_y };
}

Gfx::Size<size_t> Parser::get_decoded_size_for_plane(FrameContext const& frame_context, u8 plane)
{
    auto point = get_decoded_point_for_plane(frame_context, frame_context.columns(), frame_context.rows(), plane);
    return { point.x(), point.y() };
}

static BlockSubsize get_plane_block_size(bool subsampling_x, bool subsampling_y, u32 subsize, u8 plane)
{
    auto sub_x = (plane > 0) ? subsampling_x : 0;
    auto sub_y = (plane > 0) ? subsampling_y : 0;
    return ss_size_lookup[subsize][sub_x][sub_y];
}

static TXSize get_uv_tx_size(bool subsampling_x, bool subsampling_y, TXSize tx_size, BlockSubsize size)
{
    if (size < Block_8x8)
        return TX_4x4;
    return min(tx_size, max_txsize_lookup[get_plane_block_size(subsampling_x, subsampling_y, size, 1)]);
}

DecoderErrorOr<bool> Parser::residual(BlockContext& block_context, bool has_block_above, bool has_block_left)
{
    bool had_residual_tokens = false;
    auto block_size = block_context.size < Block_8x8 ? Block_8x8 : block_context.size;
    for (u8 plane = 0; plane < 3; plane++) {
        auto tx_size = (plane > 0) ? get_uv_tx_size(block_context.frame_context.color_config.subsampling_x, block_context.frame_context.color_config.subsampling_y, block_context.tx_size, block_context.size) : block_context.tx_size;
        auto step = 1 << tx_size;
        auto plane_size = get_plane_block_size(block_context.frame_context.color_config.subsampling_x, block_context.frame_context.color_config.subsampling_y, block_size, plane);
        auto num_4x4_w = num_4x4_blocks_wide_lookup[plane_size];
        auto num_4x4_h = num_4x4_blocks_high_lookup[plane_size];
        auto sub_x = (plane > 0) ? block_context.frame_context.color_config.subsampling_x : 0;
        auto sub_y = (plane > 0) ? block_context.frame_context.color_config.subsampling_y : 0;
        auto base_x = (block_context.column * 8) >> sub_x;
        auto base_y = (block_context.row * 8) >> sub_y;
        if (block_context.is_inter_predicted()) {
            if (block_context.size < Block_8x8) {
                for (auto y = 0; y < num_4x4_h; y++) {
                    for (auto x = 0; x < num_4x4_w; x++) {
                        TRY(m_decoder.predict_inter(plane, block_context, base_x + (4 * x), base_y + (4 * y), 4, 4, (y * num_4x4_w) + x));
                    }
                }
            } else {
                TRY(m_decoder.predict_inter(plane, block_context, base_x, base_y, num_4x4_w * 4, num_4x4_h * 4, 0));
            }
        }
        auto max_x = (block_context.frame_context.columns() * 8) >> sub_x;
        auto max_y = (block_context.frame_context.rows() * 8) >> sub_y;
        auto block_index = 0;
        for (auto y = 0; y < num_4x4_h; y += step) {
            for (auto x = 0; x < num_4x4_w; x += step) {
                auto start_x = base_x + (4 * x);
                auto start_y = base_y + (4 * y);
                auto non_zero = false;
                if (start_x < max_x && start_y < max_y) {
                    if (!block_context.is_inter_predicted())
                        TRY(m_decoder.predict_intra(plane, block_context, start_x, start_y, has_block_left || x > 0, has_block_above || y > 0, (x + step) < num_4x4_w, tx_size, block_index));
                    if (!block_context.should_skip_residuals) {
                        non_zero = TRY(tokens(block_context, plane, start_x, start_y, tx_size, block_index));
                        had_residual_tokens = had_residual_tokens || non_zero;
                        TRY(m_decoder.reconstruct(plane, block_context, start_x, start_y, tx_size));
                    }
                }

                auto& above_sub_context = m_above_nonzero_context[plane];
                auto above_sub_context_index = start_x >> 2;
                auto above_sub_context_end = min(above_sub_context_index + step, above_sub_context.size());
                for (; above_sub_context_index < above_sub_context_end; above_sub_context_index++)
                    above_sub_context[above_sub_context_index] = non_zero;

                auto& left_sub_context = m_left_nonzero_context[plane];
                auto left_sub_context_index = start_y >> 2;
                auto left_sub_context_end = min(left_sub_context_index + step, left_sub_context.size());
                for (; left_sub_context_index < left_sub_context_end; left_sub_context_index++)
                    left_sub_context[left_sub_context_index] = non_zero;

                block_index++;
            }
        }
    }
    return had_residual_tokens;
}

DecoderErrorOr<bool> Parser::tokens(BlockContext& block_context, size_t plane, u32 start_x, u32 start_y, TXSize tx_size, u32 block_index)
{
    u32 segment_eob = 16 << (tx_size << 1);
    auto const* scan = get_scan(block_context, plane, tx_size, block_index);
    auto check_eob = true;
    u32 c = 0;
    for (; c < segment_eob; c++) {
        auto pos = scan[c];
        auto band = (tx_size == TX_4x4) ? coefband_4x4[c] : coefband_8x8plus[c];
        auto tokens_context = TreeParser::get_tokens_context(block_context.frame_context.color_config.subsampling_x, block_context.frame_context.color_config.subsampling_y, block_context.frame_context.rows(), block_context.frame_context.columns(), m_above_nonzero_context, m_left_nonzero_context, m_token_cache, tx_size, m_tx_type, plane, start_x, start_y, pos, block_context.is_inter_predicted(), band, c);
        if (check_eob) {
            auto more_coefs = TRY_READ(TreeParser::parse_more_coefficients(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, tokens_context));
            if (!more_coefs)
                break;
        }
        auto token = TRY_READ(TreeParser::parse_token(*m_bit_stream, *m_probability_tables, *m_syntax_element_counter, tokens_context));
        m_token_cache[pos] = energy_class[token];
        if (token == ZeroToken) {
            m_tokens[pos] = 0;
            check_eob = false;
        } else {
            i32 coef = TRY(read_coef(block_context.frame_context.color_config.bit_depth, token));
            bool sign_bit = TRY_READ(m_bit_stream->read_literal(1));
            m_tokens[pos] = sign_bit ? -coef : coef;
            check_eob = true;
        }
    }
    for (u32 i = c; i < segment_eob; i++)
        m_tokens[scan[i]] = 0;
    return c > 0;
}

u32 const* Parser::get_scan(BlockContext const& block_context, size_t plane, TXSize tx_size, u32 block_index)
{
    if (plane > 0 || tx_size == TX_32x32) {
        m_tx_type = DCT_DCT;
    } else if (tx_size == TX_4x4) {
        if (block_context.frame_context.is_lossless() || block_context.is_inter_predicted())
            m_tx_type = DCT_DCT;
        else
            m_tx_type = mode_to_txfm_map[to_underlying(block_context.size < Block_8x8 ? block_context.sub_block_prediction_modes[block_index] : block_context.y_prediction_mode())];
    } else {
        m_tx_type = mode_to_txfm_map[to_underlying(block_context.y_prediction_mode())];
    }
    if (tx_size == TX_4x4) {
        if (m_tx_type == ADST_DCT)
            return row_scan_4x4;
        if (m_tx_type == DCT_ADST)
            return col_scan_4x4;
        return default_scan_4x4;
    }
    if (tx_size == TX_8x8) {
        if (m_tx_type == ADST_DCT)
            return row_scan_8x8;
        if (m_tx_type == DCT_ADST)
            return col_scan_8x8;
        return default_scan_8x8;
    }
    if (tx_size == TX_16x16) {
        if (m_tx_type == ADST_DCT)
            return row_scan_16x16;
        if (m_tx_type == DCT_ADST)
            return col_scan_16x16;
        return default_scan_16x16;
    }
    return default_scan_32x32;
}

DecoderErrorOr<i32> Parser::read_coef(u8 bit_depth, Token token)
{
    auto cat = extra_bits[token][0];
    auto num_extra = extra_bits[token][1];
    u32 coef = extra_bits[token][2];
    if (token == DctValCat6) {
        for (size_t e = 0; e < (u8)(bit_depth - 8); e++) {
            auto high_bit = TRY_READ(m_bit_stream->read_bool(255));
            coef += high_bit << (5 + bit_depth - e);
        }
    }
    for (size_t e = 0; e < num_extra; e++) {
        auto coef_bit = TRY_READ(m_bit_stream->read_bool(cat_probs[cat][e]));
        coef += coef_bit << (num_extra - 1 - e);
    }
    return coef;
}

// is_inside( candidateR, candidateC ) in the spec.
static bool motion_vector_is_inside_tile(TileContext const& tile_context, MotionVector vector)
{
    if (vector.row() < 0)
        return false;
    if (vector.column() < 0)
        return false;
    u32 row_positive = vector.row();
    u32 column_positive = vector.column();
    return row_positive < tile_context.frame_context.rows() && column_positive >= tile_context.columns_start && column_positive < tile_context.columns_end;
}

// add_mv_ref_list( refList ) in the spec.
static void add_motion_vector_to_list_deduped(MotionVector const& vector, Vector<MotionVector, 2>& list)
{
    if (list.size() >= 2)
        return;
    if (list.size() == 1 && vector == list[0])
        return;

    list.append(vector);
}

// get_block_mv( candidateR, candidateC, refList, usePrev ) in the spec.
MotionVectorCandidate Parser::get_motion_vector_from_current_or_previous_frame(BlockContext const& block_context, MotionVector candidate_vector, u8 ref_list, bool use_prev)
{
    if (use_prev) {
        auto const& prev_context = m_previous_block_contexts.at(candidate_vector.row(), candidate_vector.column());
        return { prev_context.ref_frames[ref_list], prev_context.primary_motion_vector_pair[ref_list] };
    }

    auto const& current_context = block_context.frame_block_contexts().at(candidate_vector.row(), candidate_vector.column());
    return { current_context.ref_frames[ref_list], current_context.primary_motion_vector_pair()[ref_list] };
}

// if_same_ref_frame_add_mv( candidateR, candidateC, refFrame, usePrev ) in the spec.
void Parser::add_motion_vector_if_reference_frame_type_is_same(BlockContext const& block_context, MotionVector candidate_vector, ReferenceFrameType ref_frame, Vector<MotionVector, 2>& list, bool use_prev)
{
    for (auto ref_list = 0u; ref_list < 2; ref_list++) {
        auto candidate = get_motion_vector_from_current_or_previous_frame(block_context, candidate_vector, ref_list, use_prev);
        if (candidate.type == ref_frame) {
            add_motion_vector_to_list_deduped(candidate.vector, list);
            return;
        }
    }
}

// scale_mv( refList, refFrame ) in the spec.
static void apply_sign_bias_to_motion_vector(FrameContext const& frame_context, MotionVectorCandidate& candidate, ReferenceFrameType ref_frame)
{
    if (frame_context.reference_frame_sign_biases[candidate.type] != frame_context.reference_frame_sign_biases[ref_frame])
        candidate.vector *= -1;
}

// if_diff_ref_frame_add_mv( candidateR, candidateC, refFrame, usePrev ) in the spec.
void Parser::add_motion_vector_if_reference_frame_type_is_different(BlockContext const& block_context, MotionVector candidate_vector, ReferenceFrameType ref_frame, Vector<MotionVector, 2>& list, bool use_prev)
{
    auto first_candidate = get_motion_vector_from_current_or_previous_frame(block_context, candidate_vector, 0, use_prev);
    if (first_candidate.type > ReferenceFrameType::IntraFrame && first_candidate.type != ref_frame) {
        apply_sign_bias_to_motion_vector(block_context.frame_context, first_candidate, ref_frame);
        add_motion_vector_to_list_deduped(first_candidate.vector, list);
    }

    auto second_candidate = get_motion_vector_from_current_or_previous_frame(block_context, candidate_vector, 1, use_prev);
    auto mvs_are_same = first_candidate.vector == second_candidate.vector;
    if (second_candidate.type > ReferenceFrameType::IntraFrame && second_candidate.type != ref_frame && !mvs_are_same) {
        apply_sign_bias_to_motion_vector(block_context.frame_context, second_candidate, ref_frame);
        add_motion_vector_to_list_deduped(second_candidate.vector, list);
    }
}

// This function handles both clamp_mv_row( mvec, border ) and clamp_mv_col( mvec, border ) in the spec.
static MotionVector clamp_motion_vector(BlockContext const& block_context, MotionVector vector, i32 border)
{
    i32 blocks_high = num_8x8_blocks_high_lookup[block_context.size];
    // Casts must be done here to prevent subtraction underflow from wrapping the values.
    i32 mb_to_top_edge = -8 * (static_cast<i32>(block_context.row) * MI_SIZE);
    i32 mb_to_bottom_edge = 8 * ((static_cast<i32>(block_context.frame_context.rows()) - blocks_high - static_cast<i32>(block_context.row)) * MI_SIZE);

    i32 blocks_wide = num_8x8_blocks_wide_lookup[block_context.size];
    i32 mb_to_left_edge = -8 * (static_cast<i32>(block_context.column) * MI_SIZE);
    i32 mb_to_right_edge = 8 * ((static_cast<i32>(block_context.frame_context.columns()) - blocks_wide - static_cast<i32>(block_context.column)) * MI_SIZE);

    return {
        clip_3(mb_to_top_edge - border, mb_to_bottom_edge + border, vector.row()),
        clip_3(mb_to_left_edge - border, mb_to_right_edge + border, vector.column())
    };
}

// 6.5.1 Find MV refs syntax
// find_mv_refs( refFrame, block ) in the spec.
MotionVectorPair Parser::find_reference_motion_vectors(BlockContext const& block_context, ReferenceFrameType reference_frame, i32 block)
{
    bool different_ref_found = false;
    u8 context_counter = 0;

    Vector<MotionVector, 2> list;

    MotionVector base_coordinates = MotionVector(block_context.row, block_context.column);

    for (auto i = 0u; i < 2; i++) {
        auto offset_vector = mv_ref_blocks[block_context.size][i];
        auto candidate = base_coordinates + offset_vector;

        if (motion_vector_is_inside_tile(block_context.tile_context, candidate)) {
            different_ref_found = true;
            auto context = block_context.frame_block_contexts().at(candidate.row(), candidate.column());
            context_counter += mode_2_counter[to_underlying(context.y_mode)];

            for (auto ref_list = 0u; ref_list < 2; ref_list++) {
                if (context.ref_frames[ref_list] == reference_frame) {
                    // This section up until add_mv_ref_list() is defined in spec as get_sub_block_mv().
                    constexpr u8 idx_n_column_to_subblock[4][2] = {
                        { 1, 2 },
                        { 1, 3 },
                        { 3, 2 },
                        { 3, 3 }
                    };
                    auto index = block >= 0 ? idx_n_column_to_subblock[block][offset_vector.column() == 0] : 3;

                    add_motion_vector_to_list_deduped(context.sub_block_motion_vectors[index][ref_list], list);
                    break;
                }
            }
        }
    }

    for (auto i = 2u; i < MVREF_NEIGHBOURS; i++) {
        MotionVector candidate = base_coordinates + mv_ref_blocks[block_context.size][i];
        if (motion_vector_is_inside_tile(block_context.tile_context, candidate)) {
            different_ref_found = true;
            add_motion_vector_if_reference_frame_type_is_same(block_context, candidate, reference_frame, list, false);
        }
    }
    if (m_use_prev_frame_mvs)
        add_motion_vector_if_reference_frame_type_is_same(block_context, base_coordinates, reference_frame, list, true);

    if (different_ref_found) {
        for (auto i = 0u; i < MVREF_NEIGHBOURS; i++) {
            MotionVector candidate = base_coordinates + mv_ref_blocks[block_context.size][i];
            if (motion_vector_is_inside_tile(block_context.tile_context, candidate))
                add_motion_vector_if_reference_frame_type_is_different(block_context, candidate, reference_frame, list, false);
        }
    }
    if (m_use_prev_frame_mvs)
        add_motion_vector_if_reference_frame_type_is_different(block_context, base_coordinates, reference_frame, list, true);

    m_mode_context[reference_frame] = counter_to_context[context_counter];
    for (auto i = 0u; i < list.size(); i++) {
        // clamp_mv_ref( i ) in the spec.
        list[i] = clamp_motion_vector(block_context, list[i], MV_BORDER);
    }

    MotionVectorPair result;
    for (auto i = 0u; i < list.size(); i++)
        result[i] = list[i];
    return result;
}

// find_best_ref_mvs( refList ) in the spec.
static void select_best_reference_motion_vectors(BlockContext& block_context, MotionVectorPair reference_motion_vectors, BlockMotionVectorCandidates& candidates, u8 reference_index)
{
    for (auto i = 0u; i < MAX_MV_REF_CANDIDATES; i++) {
        auto delta = reference_motion_vectors[i];
        auto delta_row = delta.row();
        auto delta_column = delta.column();
        if (!block_context.frame_context.high_precision_motion_vectors_allowed || !should_use_high_precision_motion_vector(delta)) {
            if ((delta_row & 1) != 0)
                delta_row += delta_row > 0 ? -1 : 1;
            if ((delta_column & 1) != 0)
                delta_column += delta_column > 0 ? -1 : 1;
        }
        delta = { delta_row, delta_column };
        reference_motion_vectors[i] = clamp_motion_vector(block_context, delta, (BORDERINPIXELS - INTERP_EXTEND) << 3);
    }

    candidates[reference_index].nearest_vector = reference_motion_vectors[0];
    candidates[reference_index].near_vector = reference_motion_vectors[1];
    candidates[reference_index].best_vector = reference_motion_vectors[0];
}

// append_sub8x8_mvs( block, refList ) in the spec.
void Parser::select_best_sub_block_reference_motion_vectors(BlockContext const& block_context, BlockMotionVectorCandidates& candidates, i32 block, u8 reference_index)
{
    MotionVector sub_8x8_mvs[2];
    MotionVectorPair reference_motion_vectors = find_reference_motion_vectors(block_context, block_context.reference_frame_types[reference_index], block);
    auto destination_index = 0;
    if (block == 0) {
        for (auto i = 0u; i < 2; i++)
            sub_8x8_mvs[destination_index++] = reference_motion_vectors[i];
    } else if (block <= 2) {
        sub_8x8_mvs[destination_index++] = block_context.sub_block_motion_vectors[0][reference_index];
    } else {
        sub_8x8_mvs[destination_index++] = block_context.sub_block_motion_vectors[2][reference_index];
        for (auto index = 1; index >= 0 && destination_index < 2; index--) {
            auto block_vector = block_context.sub_block_motion_vectors[index][reference_index];
            if (block_vector != sub_8x8_mvs[0])
                sub_8x8_mvs[destination_index++] = block_vector;
        }
    }

    for (auto n = 0u; n < 2 && destination_index < 2; n++) {
        auto ref_list_vector = reference_motion_vectors[n];
        if (ref_list_vector != sub_8x8_mvs[0])
            sub_8x8_mvs[destination_index++] = ref_list_vector;
    }

    if (destination_index < 2)
        sub_8x8_mvs[destination_index++] = {};
    candidates[reference_index].nearest_vector = sub_8x8_mvs[0];
    candidates[reference_index].near_vector = sub_8x8_mvs[1];
}

}
