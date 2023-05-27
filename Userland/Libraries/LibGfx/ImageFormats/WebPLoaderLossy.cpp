/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Format.h>
#include <AK/MemoryStream.h>
#include <AK/Vector.h>
#include <LibGfx/ImageFormats/BooleanDecoder.h>
#include <LibGfx/ImageFormats/WebPLoaderLossy.h>
#include <LibGfx/ImageFormats/WebPLoaderLossyTables.h>

// Lossy format: https://datatracker.ietf.org/doc/html/rfc6386

// Summary:
// A lossy webp image is a VP8 keyframe.
// A VP8 keyframe consists of 16x16 pixel tiles called macroblocks. Each macroblock is subdivided into 4x4 pixel tiles called subblocks.
// Pixel values are stored as YUV 4:2:0. That is, each 4x4 luma pixels are covered by 1 pixel U chroma and 1 pixel V chroma.
// This means one macroblock is covered by 4x4 Y subblocks and 2x2 U and V subblocks each.
// VP8 data consists of:
// * A tiny bit of uncompressed data, storing image dimensions and the size of the first compressed chunk of data, called the first partition
// * The first partition, which is a entropy-coded bitstream storing:
//   1. A fixed-size header.
//      The main piece of data this stores is a probability distribution for how pixel values of each metablock are predicted from previously decoded data.
//      It also stores how may independent entropy-coded bitstreams are used to store the actual pixel data (for all images I've seen so far, just one).
//   2. For each metablock, it stores how that metablock's pixel values are predicted from previously decoded data (and some more per-metablock metadata).
//      There are independent prediction modes for Y, U, V.
//      U and V store a single prediction mode per macroblock.
//      Y can store a single prediction mode per macroblock, or it can store one subblock prediction mode for each of the 4x4 luma subblocks.
// * One or more additional entropy-coded bitstreams ("partitions") that store the discrete cosine transform ("DCT") coefficients for the actual pixel data for each metablock.
//   Each metablock is subdivided into 4x4 tiles called "subblocks". A 16x16 pixel metablock consists of:
//   0. If the metablock stores 4x4 luma subblock prediction modes, the 4x4 DC coefficients of each subblock's DCT are stored at the start of the macroblock's data,
//      as coefficients of an inverse Walsh-Hadamard Transform (WHT).
//   1. 4x4 luma subblocks
//   2. 2x2 U chrome subblocks
//   3. 2x2 U chrome subblocks
//   That is, each metablock stores 24 or 25 sets of coefficients.
//   Each set of coefficients stores 16 numbers, using a combination of a custom prefix tree and dequantization.
//   The inverse DCT output is added to the output of the prediction.

namespace Gfx {

// https://developers.google.com/speed/webp/docs/riff_container#simple_file_format_lossy
// https://datatracker.ietf.org/doc/html/rfc6386#section-19 "Annex A: Bitstream Syntax"
ErrorOr<VP8Header> decode_webp_chunk_VP8_header(ReadonlyBytes vp8_data)
{
    if (vp8_data.size() < 10)
        return Error::from_string_literal("WebPImageDecoderPlugin: 'VP8 ' chunk too small");

    // FIXME: Eventually, this should probably call into LibVideo/VP8,
    // and image decoders should move into LibImageDecoders which depends on both LibGfx and LibVideo.
    // (LibVideo depends on LibGfx, so LibGfx can't depend on LibVideo itself.)

    // https://datatracker.ietf.org/doc/html/rfc6386#section-4 "Overview of Compressed Data Format"
    // "The decoder is simply presented with a sequence of compressed frames [...]
    //  The first frame presented to the decompressor is [...] a key frame.  [...]
    //  [E]very compressed frame has three or more pieces. It begins with an uncompressed data chunk comprising 10 bytes in the case of key frames"

    u8 const* data = vp8_data.data();

    // https://datatracker.ietf.org/doc/html/rfc6386#section-9.1 "Uncompressed Data Chunk"
    u32 frame_tag = data[0] | (data[1] << 8) | (data[2] << 16);
    bool is_key_frame = (frame_tag & 1) == 0; // https://www.rfc-editor.org/errata/eid5534
    u8 version = (frame_tag & 0xe) >> 1;
    bool show_frame = (frame_tag & 0x10) != 0;
    u32 size_of_first_partition = frame_tag >> 5;

    if (!is_key_frame)
        return Error::from_string_literal("WebPImageDecoderPlugin: 'VP8 ' chunk not a key frame");

    if (!show_frame)
        return Error::from_string_literal("WebPImageDecoderPlugin: 'VP8 ' chunk has invalid visibility for webp image");

    if (version > 3)
        return Error::from_string_literal("WebPImageDecoderPlugin: unknown version number in 'VP8 ' chunk");

    u32 start_code = data[3] | (data[4] << 8) | (data[5] << 16);
    if (start_code != 0x2a019d) // https://www.rfc-editor.org/errata/eid7370
        return Error::from_string_literal("WebPImageDecoderPlugin: 'VP8 ' chunk invalid start_code");

    // "The scaling specifications for each dimension are encoded as follows.
    //   0     | No upscaling (the most common case).
    //   1     | Upscale by 5/4.
    //   2     | Upscale by 5/3.
    //   3     | Upscale by 2."
    // This is a display-time operation and doesn't affect decoding."
    u16 width_and_horizontal_scale = data[6] | (data[7] << 8);
    u16 width = width_and_horizontal_scale & 0x3fff;
    u8 horizontal_scale = width_and_horizontal_scale >> 14;

    u16 heigth_and_vertical_scale = data[8] | (data[9] << 8);
    u16 height = heigth_and_vertical_scale & 0x3fff;
    u8 vertical_scale = heigth_and_vertical_scale >> 14;

    dbgln_if(WEBP_DEBUG, "version {}, show_frame {}, size_of_first_partition {}, width {}, horizontal_scale {}, height {}, vertical_scale {}",
        version, show_frame, size_of_first_partition, width, horizontal_scale, height, vertical_scale);

    return VP8Header { version, show_frame, size_of_first_partition, width, horizontal_scale, height, vertical_scale, vp8_data.slice(10) };
}

namespace {

// Reads n bits followed by a sign bit (0: positive, 1: negative).
ErrorOr<i8> read_signed_literal(BooleanDecoder& decoder, u8 n)
{
    VERIFY(n <= 7);
    i8 i = TRY(decoder.read_literal(n));
    if (TRY(decoder.read_literal(1)))
        i = -i;
    return i;
}

// https://datatracker.ietf.org/doc/html/rfc6386#section-19 "Annex A: Bitstream Syntax"
#define L(n) decoder.read_literal(n)
#define B(prob) decoder.read_bool(prob)
#define L_signed(n) read_signed_literal(decoder, n)

// https://datatracker.ietf.org/doc/html/rfc6386#section-9.2 "Color Space and Pixel Type (Key Frames Only)"
enum class ColorSpaceAndPixelType {
    YUV = 0,
    ReservedForFutureUse = 1,
};
enum class ClampingSpecification {
    DecoderMustClampTo0To255 = 0,
    NoClampingNecessary = 1,
};

// https://datatracker.ietf.org/doc/html/rfc6386#section-9.3 Segment-Based Adjustments"
// https://datatracker.ietf.org/doc/html/rfc6386#section-19.2 "Frame Header"
enum class SegmentFeatureMode {
    // Spec 19.2 says 0 is delta, 1 absolute; spec 9.3 has it the other way round. 19.2 is correct.
    // https://www.rfc-editor.org/errata/eid7519
    DeltaValueMode = 0,
    AbsoluteValueMode = 1,

};
struct Segmentation {
    bool update_metablock_segmentation_map { false };
    SegmentFeatureMode segment_feature_mode { SegmentFeatureMode::DeltaValueMode };

    i8 quantizer_update_value[4] {};
    i8 loop_filter_update_value[4] {};

    u8 metablock_segment_tree_probabilities[3] = { 255, 255, 255 };
};
ErrorOr<Segmentation> decode_VP8_frame_header_segmentation(BooleanDecoder&);

// Also https://datatracker.ietf.org/doc/html/rfc6386#section-9.6 "Dequantization Indices"
struct QuantizationIndices {
    u8 y_ac { 0 };
    i8 y_dc_delta { 0 };

    i8 y2_dc_delta { 0 };
    i8 y2_ac_delta { 0 };

    i8 uv_dc_delta { 0 };
    i8 uv_ac_delta { 0 };
};
ErrorOr<QuantizationIndices> decode_VP8_frame_header_quantization_indices(BooleanDecoder&);

struct LoopFilterAdjustment {
    bool enable_loop_filter_adjustment { false };
    i8 ref_frame_delta[4] {};
    i8 mb_mode_delta[4] {};
};
ErrorOr<LoopFilterAdjustment> decode_VP8_frame_header_loop_filter_adjustment(BooleanDecoder&);

using CoefficientProbabilities = Prob[4][8][3][num_dct_tokens - 1];
ErrorOr<void> decode_VP8_frame_header_coefficient_probabilities(BooleanDecoder&, CoefficientProbabilities);

// https://datatracker.ietf.org/doc/html/rfc6386#section-15 "Loop Filter"
// "The first is a flag (filter_type) selecting the type of filter (normal or simple)"
enum class FilterType {
    Normal = 0,
    Simple = 1,
};

// https://datatracker.ietf.org/doc/html/rfc6386#section-19.2 "Frame Header"
struct FrameHeader {
    ColorSpaceAndPixelType color_space {};
    ClampingSpecification clamping_type {};

    bool is_segmentation_enabled {};
    Segmentation segmentation {};

    FilterType filter_type {};
    u8 loop_filter_level {};
    u8 sharpness_level {};
    LoopFilterAdjustment loop_filter_adjustment {};

    u8 number_of_dct_partitions {};

    QuantizationIndices quantization_indices {};

    CoefficientProbabilities coefficient_probabilities;

    bool enable_skipping_of_metablocks_containing_only_zero_coefficients {};
    u8 probability_skip_false;
};

ErrorOr<FrameHeader> decode_VP8_frame_header(BooleanDecoder& decoder)
{
    // https://datatracker.ietf.org/doc/html/rfc6386#section-19.2 "Frame Header"
    FrameHeader header;

    // In the VP8 spec, this is in an `if (key_frames)`, but webp files only have key frames.
    header.color_space = ColorSpaceAndPixelType { TRY(L(1)) };
    header.clamping_type = ClampingSpecification { TRY(L(1)) };
    dbgln_if(WEBP_DEBUG, "color_space {} clamping_type {}", (int)header.color_space, (int)header.clamping_type);

    // https://datatracker.ietf.org/doc/html/rfc6386#section-9.3 "Segment-Based Adjustments"
    header.is_segmentation_enabled = TRY(L(1));
    dbgln_if(WEBP_DEBUG, "segmentation_enabled {}", header.is_segmentation_enabled);

    if (header.is_segmentation_enabled)
        header.segmentation = TRY(decode_VP8_frame_header_segmentation(decoder));

    header.filter_type = FilterType { TRY(L(1)) };
    header.loop_filter_level = TRY(L(6));
    header.sharpness_level = TRY(L(3));
    dbgln_if(WEBP_DEBUG, "filter_type {} loop_filter_level {} sharpness_level {}", (int)header.filter_type, header.loop_filter_level, header.sharpness_level);

    header.loop_filter_adjustment = TRY(decode_VP8_frame_header_loop_filter_adjustment(decoder));

    u8 log2_nbr_of_dct_partitions = TRY(L(2));
    dbgln_if(WEBP_DEBUG, "log2_nbr_of_dct_partitions {}", log2_nbr_of_dct_partitions);
    header.number_of_dct_partitions = 1 << log2_nbr_of_dct_partitions;

    header.quantization_indices = TRY(decode_VP8_frame_header_quantization_indices(decoder));

    // In the VP8 spec, this is in an `if (key_frames)` followed by a lengthy `else`, but webp files only have key frames.
    u8 refresh_entropy_probs = TRY(L(1)); // Has no effect in webp files.
    dbgln_if(WEBP_DEBUG, "refresh_entropy_probs {}", refresh_entropy_probs);

    memcpy(header.coefficient_probabilities, default_coeff_probs, sizeof(header.coefficient_probabilities));
    TRY(decode_VP8_frame_header_coefficient_probabilities(decoder, header.coefficient_probabilities));

    // https://datatracker.ietf.org/doc/html/rfc6386#section-9.11 "Remaining Frame Header Data (Key Frame)"
    header.enable_skipping_of_metablocks_containing_only_zero_coefficients = TRY(L(1));
    dbgln_if(WEBP_DEBUG, "mb_no_skip_coeff {}", header.enable_skipping_of_metablocks_containing_only_zero_coefficients);
    if (header.enable_skipping_of_metablocks_containing_only_zero_coefficients) {
        header.probability_skip_false = TRY(L(8));
        dbgln_if(WEBP_DEBUG, "prob_skip_false {}", header.probability_skip_false);
    }

    // In the VP8 spec, there is a length `if (!key_frames)` here, but webp files only have key frames.

    return header;
}

ErrorOr<Segmentation> decode_VP8_frame_header_segmentation(BooleanDecoder& decoder)
{
    // Corresponds to "update_segmentation()" in section 19.2 of the spec.
    Segmentation segmentation;

    segmentation.update_metablock_segmentation_map = TRY(L(1));
    u8 update_segment_feature_data = TRY(L(1));

    dbgln_if(WEBP_DEBUG, "update_mb_segmentation_map {} update_segment_feature_data {}",
        segmentation.update_metablock_segmentation_map, update_segment_feature_data);

    if (update_segment_feature_data) {
        segmentation.segment_feature_mode = static_cast<SegmentFeatureMode>(TRY(L(1)));
        dbgln_if(WEBP_DEBUG, "segment_feature_mode {}", (int)segmentation.segment_feature_mode);

        for (int i = 0; i < 4; ++i) {
            u8 quantizer_update = TRY(L(1));
            dbgln_if(WEBP_DEBUG, "quantizer_update {}", quantizer_update);
            if (quantizer_update) {
                i8 quantizer_update_value = TRY(L_signed(7));
                dbgln_if(WEBP_DEBUG, "quantizer_update_value {}", quantizer_update_value);
                segmentation.quantizer_update_value[i] = quantizer_update_value;
            }
        }
        for (int i = 0; i < 4; ++i) {
            u8 loop_filter_update = TRY(L(1));
            dbgln_if(WEBP_DEBUG, "loop_filter_update {}", loop_filter_update);
            if (loop_filter_update) {
                i8 loop_filter_update_value = TRY(L_signed(6));
                dbgln_if(WEBP_DEBUG, "loop_filter_update_value {}", loop_filter_update_value);
                segmentation.loop_filter_update_value[i] = loop_filter_update_value;
            }
        }
    }

    if (segmentation.update_metablock_segmentation_map) {
        // This reads mb_segment_tree_probs for https://datatracker.ietf.org/doc/html/rfc6386#section-10.
        for (int i = 0; i < 3; ++i) {
            u8 segment_prob_update = TRY(L(1));
            dbgln_if(WEBP_DEBUG, "segment_prob_update {}", segment_prob_update);
            if (segment_prob_update) {
                u8 segment_prob = TRY(L(8));
                dbgln_if(WEBP_DEBUG, "segment_prob {}", segment_prob);
                segmentation.metablock_segment_tree_probabilities[i] = segment_prob;
            }
        }
    }

    return segmentation;
}

ErrorOr<QuantizationIndices> decode_VP8_frame_header_quantization_indices(BooleanDecoder& decoder)
{
    // Corresponds to "quant_indices()" in section 19.2 of the spec.
    QuantizationIndices quantization_indices;

    // "The first 7-bit index gives the dequantization table index for
    //  Y-plane AC coefficients, called yac_qi.  It is always coded and acts
    //  as a baseline for the other 5 quantization indices, each of which is
    //  represented by a delta from this baseline index."
    quantization_indices.y_ac = TRY(L(7));
    dbgln_if(WEBP_DEBUG, "y_ac_qi {}", quantization_indices.y_ac);

    auto read_delta = [&decoder](StringView name, i8* destination) -> ErrorOr<void> {
        u8 is_present = TRY(L(1));
        dbgln_if(WEBP_DEBUG, "{}_present {}", name, is_present);
        if (is_present) {
            i8 delta = TRY(L_signed(4));
            dbgln_if(WEBP_DEBUG, "{} {}", name, delta);
            *destination = delta;
        }
        return {};
    };
    TRY(read_delta("y_dc_delta"sv, &quantization_indices.y_dc_delta));
    TRY(read_delta("y2_dc_delta"sv, &quantization_indices.y2_dc_delta));
    TRY(read_delta("y2_ac_delta"sv, &quantization_indices.y2_ac_delta));
    TRY(read_delta("uv_dc_delta"sv, &quantization_indices.uv_dc_delta));
    TRY(read_delta("uv_ac_delta"sv, &quantization_indices.uv_ac_delta));

    return quantization_indices;
}

ErrorOr<LoopFilterAdjustment> decode_VP8_frame_header_loop_filter_adjustment(BooleanDecoder& decoder)
{
    // Corresponds to "mb_lf_adjustments()" in section 19.2 of the spec.
    LoopFilterAdjustment adjustment;

    adjustment.enable_loop_filter_adjustment = TRY(L(1));
    if (adjustment.enable_loop_filter_adjustment) {
        u8 mode_ref_lf_delta_update = TRY(L(1));
        dbgln_if(WEBP_DEBUG, "mode_ref_lf_delta_update {}", mode_ref_lf_delta_update);
        if (mode_ref_lf_delta_update) {
            for (int i = 0; i < 4; ++i) {
                u8 ref_frame_delta_update_flag = TRY(L(1));
                dbgln_if(WEBP_DEBUG, "ref_frame_delta_update_flag {}", ref_frame_delta_update_flag);
                if (ref_frame_delta_update_flag) {
                    i8 delta = TRY(L_signed(6));
                    dbgln_if(WEBP_DEBUG, "delta {}", delta);
                    adjustment.ref_frame_delta[i] = delta;
                }
            }
            for (int i = 0; i < 4; ++i) {
                u8 mb_mode_delta_update_flag = TRY(L(1));
                dbgln_if(WEBP_DEBUG, "mb_mode_delta_update_flag {}", mb_mode_delta_update_flag);
                if (mb_mode_delta_update_flag) {
                    i8 delta = TRY(L_signed(6));
                    dbgln_if(WEBP_DEBUG, "delta {}", delta);
                    adjustment.mb_mode_delta[i] = delta;
                }
            }
        }
    }

    return adjustment;
}

ErrorOr<void> decode_VP8_frame_header_coefficient_probabilities(BooleanDecoder& decoder, CoefficientProbabilities coefficient_probabilities)
{
    // Corresponds to "token_prob_update()" in section 19.2 of the spec.
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 8; j++) {
            for (int k = 0; k < 3; k++) {
                for (int l = 0; l < 11; l++) {
                    // token_prob_update() says L(1) and L(8), but it's actually B(p) and L(8).
                    // https://datatracker.ietf.org/doc/html/rfc6386#section-13.4 "Token Probability Updates" describes it correctly.
                    if (TRY(B(coeff_update_probs[i][j][k][l])))
                        coefficient_probabilities[i][j][k][l] = TRY(L(8));
                }
            }
        }
    }

    return {};
}

}

ErrorOr<NonnullRefPtr<Bitmap>> decode_webp_chunk_VP8_contents(VP8Header const& vp8_header, bool include_alpha_channel)
{
    // The first partition stores header, per-segment state, and macroblock metadata.

    FixedMemoryStream memory_stream { vp8_header.lossy_data };
    BigEndianInputBitStream bit_stream { MaybeOwned<Stream>(memory_stream) };
    auto decoder = TRY(BooleanDecoder::initialize(MaybeOwned { bit_stream }, vp8_header.lossy_data.size() * 8));

    auto header = TRY(decode_VP8_frame_header(decoder));

    if (header.number_of_dct_partitions > 1)
        return Error::from_string_literal("WebPImageDecoderPlugin: decoding lossy webps with more than one dct partition not yet implemented");

    auto bitmap_format = include_alpha_channel ? BitmapFormat::BGRA8888 : BitmapFormat::BGRx8888;

    // Uncomment this to test ALPH decoding for WebP-lossy-with-alpha images while lossy decoding isn't implemented yet.
#if 0
    return Bitmap::create(bitmap_format, { vp8_header.width, vp8_header.height });
#else
    // FIXME: Implement webp lossy decoding.
    (void)bitmap_format;
    return Error::from_string_literal("WebPImageDecoderPlugin: decoding lossy webps not yet implemented");
#endif
}

}
