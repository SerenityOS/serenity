/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/Endian.h>
#include <AK/FixedArray.h>
#include <AK/String.h>
#include <LibCompress/Brotli.h>
#include <LibGfx/ImageFormats/JPEGXLLoader.h>

namespace Gfx {

/// 4.2 - Functions
static ALWAYS_INLINE i32 unpack_signed(u32 u)
{
    if (u % 2 == 0)
        return static_cast<i32>(u / 2);
    return -static_cast<i32>((u + 1) / 2);
}
///

/// B.2 - Field types
// This is defined as a macro in order to get lazy-evaluated parameter
// Note that the lambda will capture your context by reference.
#define U32(d0, d1, d2, d3)                            \
    ({                                                 \
        u8 const selector = TRY(stream.read_bits(2));  \
        auto value = [&, selector]() -> ErrorOr<u32> { \
            if (selector == 0)                         \
                return (d0);                           \
            if (selector == 1)                         \
                return (d1);                           \
            if (selector == 2)                         \
                return (d2);                           \
            if (selector == 3)                         \
                return (d3);                           \
            VERIFY_NOT_REACHED();                      \
        }();                                           \
        TRY(value);                                    \
    })

static ALWAYS_INLINE ErrorOr<u64> U64(LittleEndianInputBitStream& stream)
{
    u8 const selector = TRY(stream.read_bits(2));
    if (selector == 0)
        return 0;
    if (selector == 1)
        return 1 + TRY(stream.read_bits(4));
    if (selector == 2)
        return 17 + TRY(stream.read_bits(8));

    VERIFY(selector == 3);

    u64 value = TRY(stream.read_bits(12));
    u8 shift = 12;
    while (TRY(stream.read_bits(1)) == 1) {
        if (shift == 60) {
            value += TRY(stream.read_bits(4)) << shift;
            break;
        }
        value += TRY(stream.read_bits(8)) << shift;
        shift += 8;
    }

    return value;
}
///

/// D.2 - Image dimensions
struct SizeHeader {
    u32 height {};
    u32 width {};
};

static u32 aspect_ratio(u32 height, u32 ratio)
{
    if (ratio == 1)
        return height;
    if (ratio == 2)
        return height * 12 / 10;
    if (ratio == 3)
        return height * 4 / 3;
    if (ratio == 4)
        return height * 3 / 2;
    if (ratio == 5)
        return height * 16 / 9;
    if (ratio == 6)
        return height * 5 / 4;
    if (ratio == 7)
        return height * 2 / 1;
    VERIFY_NOT_REACHED();
}

static ErrorOr<SizeHeader> read_size_header(LittleEndianInputBitStream& stream)
{
    SizeHeader size {};
    auto const div8 = TRY(stream.read_bit());

    if (div8) {
        auto const h_div8 = 1 + TRY(stream.read_bits(5));
        size.height = 8 * h_div8;
    } else {
        size.height = U32(
            1 + TRY(stream.read_bits(9)),
            1 + TRY(stream.read_bits(13)),
            1 + TRY(stream.read_bits(18)),
            1 + TRY(stream.read_bits(30)));
    }

    auto const ratio = TRY(stream.read_bits(3));

    if (ratio == 0) {
        if (div8) {
            auto const w_div8 = 1 + TRY(stream.read_bits(5));
            size.width = 8 * w_div8;
        } else {
            size.width = U32(
                1 + TRY(stream.read_bits(9)),
                1 + TRY(stream.read_bits(13)),
                1 + TRY(stream.read_bits(18)),
                1 + TRY(stream.read_bits(30)));
        }
    } else {
        size.width = aspect_ratio(size.height, ratio);
    }

    return size;
}
///

/// D.3.5 - BitDepth
struct BitDepth {
    u32 bits_per_sample { 8 };
    u8 exp_bits {};
};

static ErrorOr<BitDepth> read_bit_depth(LittleEndianInputBitStream& stream)
{
    BitDepth bit_depth;
    bool const float_sample = TRY(stream.read_bit());

    if (float_sample) {
        bit_depth.bits_per_sample = U32(32, 16, 24, 1 + TRY(stream.read_bits(6)));
        bit_depth.exp_bits = 1 + TRY(stream.read_bits(4));
    } else {
        bit_depth.bits_per_sample = U32(8, 10, 12, 1 + TRY(stream.read_bits(6)));
    }

    return bit_depth;
}
///

/// E.2 - ColourEncoding
struct ColourEncoding {
    enum class ColourSpace {
        kRGB = 0,
        kGrey = 1,
        kXYB = 2,
        kUnknown = 3,
    };

    enum class WhitePoint {
        kD65 = 1,
        kCustom = 2,
        kE = 10,
        kDCI = 11,
    };

    enum class Primaries {
        kSRGB = 1,
        kCustom = 2,
        k2100 = 3,
        kP3 = 11,
    };

    enum class RenderingIntent {
        kPerceptual = 0,
        kRelative = 1,
        kSaturation = 2,
        kAbsolute = 3,
    };

    struct Customxy {
        u32 ux {};
        u32 uy {};
    };

    bool want_icc = false;
    ColourSpace colour_space { ColourSpace::kRGB };
    WhitePoint white_point { WhitePoint::kD65 };
    Primaries primaries { Primaries::kSRGB };

    Customxy white {};
    Customxy red {};
    Customxy green {};
    Customxy blue {};

    RenderingIntent rendering_intent { RenderingIntent::kRelative };
};

[[maybe_unused]] static ErrorOr<ColourEncoding::Customxy> read_custom_xy(LittleEndianInputBitStream& stream)
{
    ColourEncoding::Customxy custom_xy;

    auto const read_custom = [&stream]() -> ErrorOr<u32> {
        return U32(
            TRY(stream.read_bits(19)),
            524288 + TRY(stream.read_bits(19)),
            1048576 + TRY(stream.read_bits(20)),
            2097152 + TRY(stream.read_bits(21)));
    };

    custom_xy.ux = TRY(read_custom());
    custom_xy.uy = TRY(read_custom());

    return custom_xy;
}

static ErrorOr<ColourEncoding> read_colour_encoding(LittleEndianInputBitStream& stream)
{
    ColourEncoding colour_encoding;
    bool const all_default = TRY(stream.read_bit());

    if (!all_default) {
        TODO();
    }

    return colour_encoding;
}
///

/// B.3 - Extensions
struct Extensions {
    u64 extensions {};
};

static ErrorOr<Extensions> read_extensions(LittleEndianInputBitStream& stream)
{
    Extensions extensions;
    extensions.extensions = TRY(U64(stream));

    if (extensions.extensions != 0)
        TODO();

    return extensions;
}
///

/// K.2 - Non-separable upsampling
Array<double, 15> s_d_up2 {
    -0.01716200, -0.03452303, -0.04022174, -0.02921014, -0.00624645,
    0.14111091, 0.28896755, 0.00278718, -0.01610267, 0.56661550,
    0.03777607, -0.01986694, -0.03144731, -0.01185068, -0.00213539
};
///

/// D.3 - Image metadata

struct PreviewHeader {
};

struct AnimationHeader {
};

struct ExtraChannelInfo {
};

static ErrorOr<ExtraChannelInfo> read_extra_channel_info(LittleEndianInputBitStream&)
{
    TODO();
}

struct ToneMapping {
};

static ErrorOr<ToneMapping> read_tone_mapping(LittleEndianInputBitStream&)
{
    TODO();
}

struct OpsinInverseMatrix {
};

static ErrorOr<OpsinInverseMatrix> read_opsin_inverse_matrix(LittleEndianInputBitStream&)
{
    TODO();
}

struct ImageMetadata {
    u8 orientation { 1 };
    Optional<SizeHeader> intrinsic_size;
    Optional<PreviewHeader> preview;
    Optional<AnimationHeader> animation;
    BitDepth bit_depth;
    bool modular_16bit_buffers { true };
    u16 num_extra_channels {};
    Vector<ExtraChannelInfo, 4> ec_info;
    bool xyb_encoded { true };
    ColourEncoding colour_encoding;
    ToneMapping tone_mapping;
    Extensions extensions;
    bool default_m;
    OpsinInverseMatrix opsin_inverse_matrix;
    u8 cw_mask { 0 };

    Array<double, 15> up2_weight = s_d_up2;
    // TODO: add up[4, 8]_weight
};

static ErrorOr<ImageMetadata> read_metadata_header(LittleEndianInputBitStream& stream)
{
    ImageMetadata metadata;
    bool const all_default = TRY(stream.read_bit());

    if (!all_default) {
        bool const extra_fields = TRY(stream.read_bit());

        if (extra_fields) {
            metadata.orientation = 1 + TRY(stream.read_bits(3));

            bool const have_intr_size = TRY(stream.read_bit());
            if (have_intr_size)
                metadata.intrinsic_size = TRY(read_size_header(stream));

            bool const have_preview = TRY(stream.read_bit());
            if (have_preview)
                TODO();

            bool const have_animation = TRY(stream.read_bit());
            if (have_animation)
                TODO();
        }

        metadata.bit_depth = TRY(read_bit_depth(stream));
        metadata.modular_16bit_buffers = TRY(stream.read_bit());
        metadata.num_extra_channels = U32(0, 1, 2 + TRY(stream.read_bits(4)), 1 + TRY(stream.read_bits(12)));

        for (u16 i {}; i < metadata.num_extra_channels; ++i)
            metadata.ec_info.append(TRY(read_extra_channel_info(stream)));

        metadata.xyb_encoded = TRY(stream.read_bit());

        metadata.colour_encoding = TRY(read_colour_encoding(stream));

        if (extra_fields)
            metadata.tone_mapping = TRY(read_tone_mapping(stream));

        metadata.extensions = TRY(read_extensions(stream));
    }

    metadata.default_m = TRY(stream.read_bit());

    if (!metadata.default_m && metadata.xyb_encoded)
        metadata.opsin_inverse_matrix = TRY(read_opsin_inverse_matrix(stream));

    if (!metadata.default_m)
        metadata.cw_mask = TRY(stream.read_bits(3));

    if (metadata.cw_mask != 0)
        TODO();

    return metadata;
}
///

/// Table F.7 — BlendingInfo bundle
struct BlendingInfo {
    enum class BlendMode {
        kReplace = 0,
        kAdd = 1,
        kBlend = 2,
        kMulAdd = 3,
        kMul = 4,
    };

    BlendMode mode {};
    u8 alpha_channel {};
    u8 clamp {};
    u8 source {};
};

static ErrorOr<BlendingInfo> read_blending_info(LittleEndianInputBitStream& stream, ImageMetadata const& metadata, bool have_crop)
{
    BlendingInfo blending_info;

    blending_info.mode = static_cast<BlendingInfo::BlendMode>(U32(0, 1, 2, 3 + TRY(stream.read_bits(2))));

    bool const extra = metadata.num_extra_channels > 0;
    // FIXME: also consider "cropped" image of the dimension of the frame
    VERIFY(!have_crop);
    bool const full_frame = !have_crop;

    if (extra) {
        TODO();
    }

    if (blending_info.mode != BlendingInfo::BlendMode::kReplace
        || !full_frame) {
        blending_info.source = TRY(stream.read_bits(2));
    }

    return blending_info;
}
///

/// J.1 - General
struct RestorationFilter {
    bool gab { true };
    u8 epf_iters { 2 };
    Extensions extensions;
};

static ErrorOr<RestorationFilter> read_restoration_filter(LittleEndianInputBitStream& stream)
{
    RestorationFilter restoration_filter;

    auto const all_defaults = TRY(stream.read_bit());

    if (!all_defaults) {
        restoration_filter.gab = TRY(stream.read_bit());

        if (restoration_filter.gab) {
            TODO();
        }

        restoration_filter.epf_iters = TRY(stream.read_bits(2));
        if (restoration_filter.epf_iters != 0) {
            TODO();
        }

        restoration_filter.extensions = TRY(read_extensions(stream));
    }

    return restoration_filter;
}
///

/// Table F.6 — Passes bundle
struct Passes {
    u8 num_passes { 1 };
};

static ErrorOr<Passes> read_passes(LittleEndianInputBitStream& stream)
{
    Passes passes;

    passes.num_passes = U32(1, 2, 3, 4 + TRY(stream.read_bits(3)));

    if (passes.num_passes != 1) {
        TODO();
    }

    return passes;
}
///

/// F.2 - FrameHeader
struct FrameHeader {
    enum class FrameType {
        kRegularFrame = 0,
        kLFFrame = 1,
        kReferenceOnly = 2,
        kSkipProgressive = 3,
    };

    enum class Encoding {
        kVarDCT = 0,
        kModular = 1,
    };

    enum class Flags {
        None = 0,
        kNoise = 1,
        kPatches = 1 << 1,
        kSplines = 1 << 4,
        kUseLfFrame = 1 << 5,
        kSkipAdaptiveLFSmoothing = 1 << 7,
    };

    FrameType frame_type { FrameType::kRegularFrame };
    Encoding encoding { Encoding::kVarDCT };
    Flags flags { Flags::None };

    bool do_YCbCr { false };

    Array<u8, 3> jpeg_upsampling {};
    u8 upsampling {};
    Vector<u8> ec_upsampling {};

    u8 group_size_shift { 1 };
    Passes passes {};

    u8 lf_level {};
    bool have_crop { false };

    BlendingInfo blending_info {};

    bool is_last { true };
    bool save_before_ct {};

    String name {};
    RestorationFilter restoration_filter {};
    Extensions extensions {};
};

static int operator&(FrameHeader::Flags first, FrameHeader::Flags second)
{
    return static_cast<int>(first) & static_cast<int>(second);
}

static ErrorOr<FrameHeader> read_frame_header(LittleEndianInputBitStream& stream, ImageMetadata const& metadata)
{
    FrameHeader frame_header;
    bool const all_default = TRY(stream.read_bit());

    if (!all_default) {
        frame_header.frame_type = static_cast<FrameHeader::FrameType>(TRY(stream.read_bits(2)));
        frame_header.encoding = static_cast<FrameHeader::Encoding>(TRY(stream.read_bits(1)));

        frame_header.flags = static_cast<FrameHeader::Flags>(TRY(U64(stream)));

        if (!metadata.xyb_encoded)
            frame_header.do_YCbCr = TRY(stream.read_bit());

        if (!(frame_header.flags & FrameHeader::Flags::kUseLfFrame)) {
            if (frame_header.do_YCbCr) {
                frame_header.jpeg_upsampling[0] = TRY(stream.read_bits(2));
                frame_header.jpeg_upsampling[1] = TRY(stream.read_bits(2));
                frame_header.jpeg_upsampling[2] = TRY(stream.read_bits(2));
            }

            frame_header.upsampling = U32(1, 2, 4, 8);

            for (u16 i {}; i < metadata.num_extra_channels; ++i)
                TODO();
        }

        if (frame_header.encoding == FrameHeader::Encoding::kModular)
            frame_header.group_size_shift = TRY(stream.read_bits(2));

        if (frame_header.encoding == FrameHeader::Encoding::kVarDCT)
            TODO();

        if (frame_header.frame_type != FrameHeader::FrameType::kReferenceOnly)
            frame_header.passes = TRY(read_passes(stream));

        if (frame_header.frame_type == FrameHeader::FrameType::kLFFrame)
            TODO();

        if (frame_header.frame_type != FrameHeader::FrameType::kLFFrame)
            frame_header.have_crop = TRY(stream.read_bit());

        if (frame_header.have_crop)
            TODO();

        bool const normal_frame = frame_header.frame_type == FrameHeader::FrameType::kRegularFrame
            || frame_header.frame_type == FrameHeader::FrameType::kSkipProgressive;

        if (normal_frame) {
            frame_header.blending_info = TRY(read_blending_info(stream, metadata, frame_header.have_crop));

            for (u16 i {}; i < metadata.num_extra_channels; ++i)
                TODO();

            if (metadata.animation.has_value())
                TODO();

            frame_header.is_last = TRY(stream.read_bit());
        }

        // FIXME: Ensure that is_last has the correct default value
        VERIFY(normal_frame);

        if (frame_header.frame_type != FrameHeader::FrameType::kLFFrame) {
            if (!frame_header.is_last)
                TODO();
            frame_header.save_before_ct = TRY(stream.read_bit());
        }

        // FIXME: Ensure that save_before_ct has the correct default value
        VERIFY(frame_header.frame_type != FrameHeader::FrameType::kLFFrame);

        auto const name_length = U32(0, TRY(stream.read_bits(4)), 16 + TRY(stream.read_bits(5)), 48 + TRY(stream.read_bits(10)));
        auto string_buffer = TRY(FixedArray<u8>::create(name_length));
        TRY(stream.read_until_filled(string_buffer.span()));

        frame_header.name = TRY(String::from_utf8(StringView { string_buffer.span() }));

        frame_header.restoration_filter = TRY(read_restoration_filter(stream));

        frame_header.extensions = TRY(read_extensions(stream));
    }

    return frame_header;
}
///

/// F.3  TOC
struct TOC {
    FixedArray<u32> entries;
    FixedArray<u32> group_offsets;
};

static u64 num_toc_entries(FrameHeader const& frame_header, u64 num_groups, u64 num_lf_groups)
{
    if (num_groups == 1 && frame_header.passes.num_passes == 1)
        return 1;

    // Otherwise, there is one entry for each of the following sections,
    // in the order they are listed: LfGlobal, one per LfGroup in raster
    // order, one for HfGlobal followed by HfPass data for all the passes,
    // and num_groups * frame_header.passes.num_passes for the PassGroup sections.

    auto const hf_contribution = frame_header.encoding == FrameHeader::Encoding::kVarDCT ? (1 + frame_header.passes.num_passes) : 0;

    return 1 + num_lf_groups + hf_contribution + num_groups * frame_header.passes.num_passes;
}

static ErrorOr<TOC> read_toc(LittleEndianInputBitStream& stream, FrameHeader const& frame_header, u64 num_groups, u64 num_lf_groups)
{
    TOC toc;

    bool const permuted_toc = TRY(stream.read_bit());

    if (permuted_toc) {
        // Read permutations
        TODO();
    }

    // F.3.3 - Decoding TOC
    stream.align_to_byte_boundary();

    auto const toc_entries = num_toc_entries(frame_header, num_groups, num_lf_groups);

    toc.entries = TRY(FixedArray<u32>::create(toc_entries));
    toc.group_offsets = TRY(FixedArray<u32>::create(toc_entries));

    for (u32 i {}; i < toc_entries; ++i) {
        auto const new_entry = U32(
            TRY(stream.read_bits(10)),
            1024 + TRY(stream.read_bits(14)),
            17408 + TRY(stream.read_bits(22)),
            4211712 + TRY(stream.read_bits(30)));

        toc.entries[i] = new_entry;
        toc.group_offsets[i] = (i == 0 ? 0 : toc.group_offsets[i - 1]) + new_entry;
    }

    if (permuted_toc)
        TODO();

    stream.align_to_byte_boundary();

    return toc;
}
///

/// G.1.2 - LF channel dequantization weights
struct LfChannelDequantization {
    float m_x_lf_unscaled { 4096 };
    float m_y_lf_unscaled { 512 };
    float m_b_lf_unscaled { 256 };
};

static ErrorOr<LfChannelDequantization> read_lf_channel_dequantization(LittleEndianInputBitStream& stream)
{
    LfChannelDequantization lf_channel_dequantization;

    auto const all_default = TRY(stream.read_bit());

    if (!all_default) {
        TODO();
    }

    return lf_channel_dequantization;
}
///

/// C - Entropy decoding
class EntropyDecoder {
    using BrotliCanonicalCode = Compress::Brotli::CanonicalCode;

public:
    static ErrorOr<EntropyDecoder> create(LittleEndianInputBitStream& stream, u8 initial_num_distrib)
    {
        EntropyDecoder entropy_decoder;
        // C.2 - Distribution decoding
        entropy_decoder.m_lz77_enabled = TRY(stream.read_bit());

        if (entropy_decoder.m_lz77_enabled) {
            TODO();
        }

        TRY(entropy_decoder.read_pre_clustered_distributions(stream, initial_num_distrib));

        bool const use_prefix_code = TRY(stream.read_bit());

        if (!use_prefix_code)
            entropy_decoder.m_log_alphabet_size = 5 + TRY(stream.read_bits(2));

        for (auto& config : entropy_decoder.m_configs)
            config = TRY(entropy_decoder.read_config(stream));

        Vector<u16> counts;
        TRY(counts.try_resize(entropy_decoder.m_configs.size()));
        TRY(entropy_decoder.m_distributions.try_resize(entropy_decoder.m_configs.size()));

        if (use_prefix_code) {
            for (auto& count : counts) {
                if (TRY(stream.read_bit())) {
                    auto const n = TRY(stream.read_bits(4));
                    count = 1 + (1 << n) + TRY(stream.read_bits(n));
                } else {
                    count = 1;
                }
            }

            // After reading the counts, the decoder reads each D[i] (implicitly
            // described by a prefix code) as specified in C.2.4, with alphabet_size = count[i].
            for (u32 i {}; i < entropy_decoder.m_distributions.size(); ++i) {
                // The alphabet size mentioned in the [Brotli] RFC is explicitly specified as parameter alphabet_size
                // when the histogram is being decoded, except in the special case of alphabet_size == 1, where no
                // histogram is read, and all decoded symbols are zero without reading any bits at all.
                if (counts[i] != 1) {
                    entropy_decoder.m_distributions[i] = TRY(BrotliCanonicalCode::read_prefix_code(stream, counts[i]));
                } else {
                    entropy_decoder.m_distributions[i] = BrotliCanonicalCode { { 1 }, { 0 } };
                }
            }
        } else {
            TODO();
        }

        return entropy_decoder;
    }

    ErrorOr<u32> decode_hybrid_uint(LittleEndianInputBitStream& stream, u16 context)
    {
        // C.3.3 - Hybrid integer decoding

        if (m_lz77_enabled)
            TODO();

        // Read symbol from entropy coded stream using D[clusters[ctx]]
        auto const token = TRY(m_distributions[m_clusters[context]].read_symbol(stream));

        auto r = TRY(read_uint(stream, m_configs[m_clusters[context]], token));
        return r;
    }

private:
    struct HybridUint {
        u32 split_exponent {};
        u32 split {};
        u32 msb_in_token {};
        u32 lsb_in_token {};
    };

    static ErrorOr<u32> read_uint(LittleEndianInputBitStream& stream, HybridUint const& config, u32 token)
    {
        if (token < config.split)
            return token;

        auto const n = config.split_exponent
            - config.msb_in_token
            - config.lsb_in_token
            + ((token - config.split) >> (config.msb_in_token + config.lsb_in_token));

        VERIFY(n < 32);

        u32 const low_bits = token & ((1 << config.lsb_in_token) - 1);
        token = token >> config.lsb_in_token;
        token &= (1 << config.msb_in_token) - 1;
        token |= (1 << config.msb_in_token);

        auto const result = ((token << n | TRY(stream.read_bits(n))) << config.lsb_in_token) | low_bits;

        VERIFY(result < (1ul << 32));

        return result;
    }

    ErrorOr<void> read_pre_clustered_distributions(LittleEndianInputBitStream& stream, u8 num_distrib)
    {
        // C.2.2  Distribution clustering
        if (num_distrib == 1)
            TODO();

        TRY(m_clusters.try_resize(num_distrib));

        bool const is_simple = TRY(stream.read_bit());

        u16 num_clusters = 0;

        if (is_simple) {
            u8 const nbits = TRY(stream.read_bits(2));
            for (u8 i {}; i < num_distrib; ++i) {
                m_clusters[i] = TRY(stream.read_bits(nbits));
                if (m_clusters[i] >= num_clusters)
                    num_clusters = m_clusters[i] + 1;
            }

        } else {
            TODO();
        }
        TRY(m_configs.try_resize(num_clusters));
        return {};
    }

    ErrorOr<HybridUint> read_config(LittleEndianInputBitStream& stream) const
    {
        // C.2.3 - Hybrid integer configuration
        HybridUint config {};
        config.split_exponent = TRY(stream.read_bits(ceil(log2(m_log_alphabet_size + 1))));
        if (config.split_exponent != m_log_alphabet_size) {
            auto nbits = ceil(log2(config.split_exponent + 1));
            config.msb_in_token = TRY(stream.read_bits(nbits));
            nbits = ceil(log2(config.split_exponent - config.msb_in_token + 1));
            config.lsb_in_token = TRY(stream.read_bits(nbits));
        } else {
            config.msb_in_token = 0;
            config.lsb_in_token = 0;
        }

        config.split = 1 << config.split_exponent;
        return config;
    }

    bool m_lz77_enabled {};
    Vector<u32> m_clusters;
    Vector<HybridUint> m_configs;

    u8 m_log_alphabet_size { 15 };

    Vector<BrotliCanonicalCode> m_distributions; // D in the spec
};
///

/// H.4.2 - MA tree decoding
class MATree {
public:
    struct LeafNode {
        u8 ctx {};
        u8 predictor {};
        i32 offset {};
        u32 multiplier {};
    };

    static ErrorOr<MATree> decode(LittleEndianInputBitStream& stream, Optional<EntropyDecoder>& decoder)
    {
        // G.1.3 - GlobalModular
        MATree tree;

        // 1 / 2 Read the 6 pre-clustered distributions
        auto const num_distrib = 6;
        if (!decoder.has_value())
            decoder = TRY(EntropyDecoder::create(stream, num_distrib));

        // 2 / 2 Decode the tree

        u64 ctx_id = 0;
        u64 nodes_left = 1;
        tree.m_tree.clear();

        while (nodes_left > 0) {
            nodes_left--;

            i32 const property = TRY(decoder->decode_hybrid_uint(stream, 1)) - 1;

            if (property >= 0) {
                DecisionNode decision_node;
                decision_node.property = property;
                decision_node.value = unpack_signed(TRY(decoder->decode_hybrid_uint(stream, 0)));
                decision_node.left_child = tree.m_tree.size() + nodes_left + 1;
                decision_node.right_child = tree.m_tree.size() + nodes_left + 2;
                tree.m_tree.empend(decision_node);
                nodes_left += 2;
            } else {
                LeafNode leaf_node;
                leaf_node.ctx = ctx_id++;
                leaf_node.predictor = TRY(decoder->decode_hybrid_uint(stream, 2));
                leaf_node.offset = unpack_signed(TRY(decoder->decode_hybrid_uint(stream, 3)));
                auto const mul_log = TRY(decoder->decode_hybrid_uint(stream, 4));
                auto const mul_bits = TRY(decoder->decode_hybrid_uint(stream, 5));
                leaf_node.multiplier = (mul_bits + 1) << mul_log;
                tree.m_tree.empend(leaf_node);
            }
        }

        // Finally, the decoder reads (tree.size() + 1) / 2 pre-clustered distributions D as specified in C.1.

        auto const num_pre_clustered_distributions = (tree.m_tree.size() + 1) / 2;
        decoder = TRY(decoder->create(stream, num_pre_clustered_distributions));

        return tree;
    }

    LeafNode get_leaf(Vector<i32> const& properties) const
    {
        // To find the MA leaf node, the MA tree is traversed, starting at the root node tree[0]
        // and for each decision node d, testing if property[d.property] > d.value, proceeding to
        // the node tree[d.left_child] if the test evaluates to true and to the node tree[d.right_child]
        // otherwise, until a leaf node is reached.

        DecisionNode node { m_tree[0].get<DecisionNode>() };
        while (true) {
            auto const next_node = [this, &properties, &node]() {
                // Note: The behavior when trying to access a non-existing property is taken from jxl-oxide
                if (node.property < properties.size() && properties[node.property] > node.value)
                    return m_tree[node.left_child];
                return m_tree[node.right_child];
            }();

            if (next_node.has<LeafNode>())
                return next_node.get<LeafNode>();
            node = next_node.get<DecisionNode>();
        }
    }

private:
    struct DecisionNode {
        u64 property {};
        i64 value {};
        u64 left_child {};
        u64 right_child {};
    };

    Vector<Variant<DecisionNode, LeafNode>> m_tree;
};
///

/// H.5 - Self-correcting predictor
struct WPHeader {
    u8 wp_p1 { 16 };
    u8 wp_p2 { 10 };
    u8 wp_p3a { 7 };
    u8 wp_p3b { 7 };
    u8 wp_p3c { 7 };
    u8 wp_p3d { 0 };
    u8 wp_p3e { 0 };
    u8 wp_w0 { 13 };
    u8 wp_w1 { 12 };
    u8 wp_w2 { 12 };
    u8 wp_w3 { 12 };
};

static ErrorOr<WPHeader> read_self_correcting_predictor(LittleEndianInputBitStream& stream)
{
    WPHeader self_correcting_predictor {};

    bool const default_wp = TRY(stream.read_bit());

    if (!default_wp) {
        TODO();
    }

    return self_correcting_predictor;
}
///

///
struct TransformInfo {
    enum class TransformId {
        kRCT = 0,
        kPalette = 1,
        kSqueeze = 2,
    };

    TransformId tr {};
    u32 begin_c {};
    u32 rct_type {};
};

static ErrorOr<TransformInfo> read_transform_info(LittleEndianInputBitStream& stream)
{
    TransformInfo transform_info;

    transform_info.tr = static_cast<TransformInfo::TransformId>(TRY(stream.read_bits(2)));

    if (transform_info.tr != TransformInfo::TransformId::kSqueeze) {
        transform_info.begin_c = U32(
            TRY(stream.read_bits(3)),
            8 + TRY(stream.read_bits(3)),
            72 + TRY(stream.read_bits(10)),
            1096 + TRY(stream.read_bits(13)));
    }

    if (transform_info.tr == TransformInfo::TransformId::kRCT) {
        transform_info.rct_type = U32(
            6,
            TRY(stream.read_bits(2)),
            2 + TRY(stream.read_bits(4)),
            10 + TRY(stream.read_bits(6)));
    }

    if (transform_info.tr != TransformInfo::TransformId::kRCT)
        TODO();

    return transform_info;
}
///

/// Local abstractions to store the decoded image
class Channel {
public:
    static ErrorOr<Channel> create(u32 width, u32 height)
    {
        Channel channel;

        channel.m_width = width;
        channel.m_height = height;

        TRY(channel.m_pixels.try_resize(channel.m_width * channel.m_height));

        return channel;
    }

    i32 get(u32 x, u32 y) const
    {
        return m_pixels[x * m_width + y];
    }

    void set(u32 x, u32 y, i32 value)
    {
        m_pixels[x * m_width + y] = value;
    }

    u32 width() const
    {
        return m_width;
    }

    u32 height() const
    {
        return m_height;
    }

    u32 hshift() const
    {
        return m_hshift;
    }

    u32 vshift() const
    {
        return m_vshift;
    }

private:
    u32 m_width {};
    u32 m_height {};

    u32 m_hshift {};
    u32 m_vshift {};

    Vector<i32> m_pixels {};
};

class Image {
public:
    static ErrorOr<Image> create(IntSize size)
    {
        Image image {};

        // FIXME: Don't assume three channels and a fixed size
        TRY(image.m_channels.try_append(TRY(Channel::create(size.width(), size.height()))));
        TRY(image.m_channels.try_append(TRY(Channel::create(size.width(), size.height()))));
        TRY(image.m_channels.try_append(TRY(Channel::create(size.width(), size.height()))));

        return image;
    }

    ErrorOr<NonnullRefPtr<Bitmap>> to_bitmap(u8 bits_per_sample) const
    {
        // FIXME: which channel size should we use?
        auto const width = m_channels[0].width();
        auto const height = m_channels[0].height();

        auto bitmap = TRY(Bitmap::create(BitmapFormat::BGRx8888, { width, height }));

        // FIXME: This assumes a raw image with RGB channels, other cases are possible
        VERIFY(bits_per_sample >= 8);
        for (u32 y {}; y < height; ++y) {
            for (u32 x {}; x < width; ++x) {
                auto const to_u8 = [&, bits_per_sample](i32 sample) -> u8 {
                    // FIXME: Don't truncate the result to 8 bits
                    static constexpr auto maximum_supported_bit_depth = 8;
                    if (bits_per_sample > maximum_supported_bit_depth)
                        sample >>= (bits_per_sample - maximum_supported_bit_depth);

                    return clamp(sample + .5, 0, (1 << maximum_supported_bit_depth) - 1);
                };

                Color const color {
                    to_u8(m_channels[0].get(x, y)),
                    to_u8(m_channels[1].get(x, y)),
                    to_u8(m_channels[2].get(x, y)),
                };
                bitmap->set_pixel(x, y, color);
            }
        }

        return bitmap;
    }

    Vector<Channel>& channels()
    {
        return m_channels;
    }

private:
    Vector<Channel> m_channels;
};
///

/// H.2 - Image decoding
struct ModularHeader {
    bool use_global_tree {};
    WPHeader wp_params {};
    Vector<TransformInfo> transform {};
};

static ErrorOr<Vector<i32>> get_properties(Vector<Channel> const& channels, u16 i, u32 x, u32 y)
{
    Vector<i32> properties;

    // Table H.4 - Property definitions
    TRY(properties.try_append(i));
    // FIXME: Handle other cases than GlobalModular
    TRY(properties.try_append(0));
    TRY(properties.try_append(y));
    TRY(properties.try_append(x));

    i32 const W = x > 0 ? channels[i].get(x - 1, y) : (y > 0 ? channels[i].get(x, y - 1) : 0);
    i32 const N = y > 0 ? channels[i].get(x, y - 1) : W;
    i32 const NW = x > 0 && y > 0 ? channels[i].get(x - 1, y - 1) : W;
    i32 const NE = x + 1 < channels[i].width() && y > 0 ? channels[i].get(x + 1, y - 1) : N;
    i32 const NN = y > 1 ? channels[i].get(x, y - 2) : N;
    i32 const WW = x > 1 ? channels[i].get(x - 2, y) : W;

    TRY(properties.try_append(abs(N)));
    TRY(properties.try_append(abs(W)));
    TRY(properties.try_append(N));
    TRY(properties.try_append(W));

    // x > 0 ? W - /* (the value of property 9 at position (x - 1, y)) */ : W
    i32 x_1 = x - 1;
    i32 const W_x_1 = x_1 > 0 ? channels[i].get(x_1 - 1, y) : (x_1 >= 0 && y > 0 ? channels[i].get(x_1, y - 1) : 0);
    i32 const N_x_1 = x_1 >= 0 && y > 0 ? channels[i].get(x_1, y - 1) : W_x_1;
    i32 const NW_x_1 = x_1 > 0 && y > 0 ? channels[i].get(x_1 - 1, y - 1) : W_x_1;

    TRY(properties.try_append(W_x_1 + N_x_1 - NW_x_1));

    TRY(properties.try_append(W + N - NW));
    TRY(properties.try_append(W - NW));
    TRY(properties.try_append(NW - N));
    TRY(properties.try_append(N - NE));
    TRY(properties.try_append(N - NN));
    TRY(properties.try_append(W - WW));

    // FIXME: Correctly compute max_error
    TRY(properties.try_append(0));

    for (i16 j = i - 1; j >= 0; j--) {
        if (channels[j].width() != channels[i].width())
            continue;
        if (channels[j].height() != channels[i].height())
            continue;
        if (channels[j].hshift() != channels[i].hshift())
            continue;
        if (channels[j].vshift() != channels[i].vshift())
            continue;
        auto rC = channels[j].get(x, y);
        auto rW = (x > 0 ? channels[j].get(x - 1, y) : 0);
        auto rN = (y > 0 ? channels[j].get(x, y - 1) : rW);
        auto rNW = (x > 0 && y > 0 ? channels[j].get(x - 1, y - 1) : rW);
        auto rG = clamp(rW + rN - rNW, min(rW, rN), max(rW, rN));
        TRY(properties.try_append(abs(rC)));
        TRY(properties.try_append(rC));
        TRY(properties.try_append(abs(rC - rG)));
        TRY(properties.try_append(rC - rG));
    }
    return properties;
}

static i32 prediction(Channel const& channel, u32 x, u32 y, u32 predictor)
{
    i32 const W = x > 0 ? channel.get(x - 1, y) : (y > 0 ? channel.get(x, y - 1) : 0);
    i32 const N = y > 0 ? channel.get(x, y - 1) : W;
    i32 const NW = x > 0 && y > 0 ? channel.get(x - 1, y - 1) : W;
    i32 const NE = x + 1 < channel.width() && y > 0 ? channel.get(x + 1, y - 1) : N;
    i32 const NN = y > 1 ? channel.get(x, y - 2) : N;
    i32 const NEE = x + 2 < channel.width() and y > 0 ? channel.get(x + 2, y - 1) : NE;
    i32 const WW = x > 1 ? channel.get(x - 2, y) : W;

    switch (predictor) {
    case 0:
        return 0;
    case 1:
        return W;
    case 2:
        return N;
    case 3:
        return (W + N) / 2;
    case 4:
        return abs(N - NW) < abs(W - NW) ? W : N;
    case 5:
        return clamp(W + N - NW, min(W, N), max(W, N));
    case 6:
        TODO();
        return (0 + 3) >> 3;
    case 7:
        return NE;
    case 8:
        return NW;
    case 9:
        return WW;
    case 10:
        return (W + NW) / 2;
    case 11:
        return (N + NW) / 2;
    case 12:
        return (N + NE) / 2;
    case 13:
        return (6 * N - 2 * NN + 7 * W + WW + NEE + 3 * NE + 8) / 16;
    }
    VERIFY_NOT_REACHED();
}

static ErrorOr<ModularHeader> read_modular_header(LittleEndianInputBitStream& stream,
    Image& image,
    Optional<EntropyDecoder>& decoder,
    MATree const& global_tree,
    u16 num_channels)
{
    ModularHeader modular_header;

    modular_header.use_global_tree = TRY(stream.read_bit());
    modular_header.wp_params = TRY(read_self_correcting_predictor(stream));
    auto const nb_transforms = U32(0, 1, 2 + TRY(stream.read_bits(4)), 18 + TRY(stream.read_bits(8)));

    TRY(modular_header.transform.try_resize(nb_transforms));
    for (u32 i {}; i < nb_transforms; ++i)
        modular_header.transform[i] = TRY(read_transform_info(stream));

    Optional<MATree> local_tree;
    if (!modular_header.use_global_tree)
        TODO();

    // The decoder then starts an entropy-coded stream (C.1) and decodes the data for each channel
    // (in ascending order of index) as specified in H.3, skipping any channels having width or height
    // zero. Finally, the inverse transformations are applied (from last to first) as described in H.6.

    auto const& tree = local_tree.has_value() ? *local_tree : global_tree;
    for (u16 i {}; i < num_channels; ++i) {
        for (u32 y {}; y < image.channels()[i].height(); y++) {
            for (u32 x {}; x < image.channels()[i].width(); x++) {

                auto const properties = TRY(get_properties(image.channels(), i, x, y));
                auto const leaf_node = tree.get_leaf(properties);
                auto diff = unpack_signed(TRY(decoder->decode_hybrid_uint(stream, leaf_node.ctx)));
                diff = (diff * leaf_node.multiplier) + leaf_node.offset;
                auto const total = diff + prediction(image.channels()[i], x, y, leaf_node.predictor);

                image.channels()[i].set(x, y, total);
            }
        }
    }

    return modular_header;
}
///

/// G.1.2 - LF channel dequantization weights
struct GlobalModular {
    MATree ma_tree;
    ModularHeader modular_header;
};

static ErrorOr<GlobalModular> read_global_modular(LittleEndianInputBitStream& stream,
    Image& image,
    FrameHeader const& frame_header,
    ImageMetadata const& metadata,
    Optional<EntropyDecoder>& entropy_decoder)
{
    GlobalModular global_modular;

    auto const decode_ma_tree = TRY(stream.read_bit());

    if (decode_ma_tree)
        global_modular.ma_tree = TRY(MATree::decode(stream, entropy_decoder));

    // The decoder then decodes a modular sub-bitstream (Annex H), where
    // the number of channels is computed as follows:

    auto num_channels = metadata.num_extra_channels;
    if (frame_header.encoding == FrameHeader::Encoding::kModular) {
        if (!frame_header.do_YCbCr && !metadata.xyb_encoded
            && metadata.colour_encoding.colour_space == ColourEncoding::ColourSpace::kGrey) {
            num_channels += 1;
        } else {
            num_channels += 3;
        }
    }

    // FIXME: Ensure this spec comment:
    //        However, the decoder only decodes the first nb_meta_channels channels and any further channels
    //        that have a width and height that are both at most group_dim. At that point, it stops decoding.
    //        No inverse transforms are applied yet.
    global_modular.modular_header = TRY(read_modular_header(stream, image, entropy_decoder, global_modular.ma_tree, num_channels));

    return global_modular;
}
///

/// G.1 - LfGlobal
struct LfGlobal {
    LfChannelDequantization lf_dequant;
    GlobalModular gmodular;
};

static ErrorOr<LfGlobal> read_lf_global(LittleEndianInputBitStream& stream,
    Image& image,
    FrameHeader const& frame_header,
    ImageMetadata const& metadata,
    Optional<EntropyDecoder>& entropy_decoder)
{
    LfGlobal lf_global;

    if (frame_header.flags != FrameHeader::Flags::None)
        TODO();

    lf_global.lf_dequant = TRY(read_lf_channel_dequantization(stream));

    if (frame_header.encoding == FrameHeader::Encoding::kVarDCT)
        TODO();

    lf_global.gmodular = TRY(read_global_modular(stream, image, frame_header, metadata, entropy_decoder));

    return lf_global;
}
///

/// H.6 - Transformations
static void apply_rct(Image& image, TransformInfo const& transformation)
{
    auto& channels = image.channels();
    for (u32 y {}; y < channels[transformation.begin_c].height(); y++) {
        for (u32 x {}; x < channels[transformation.begin_c].width(); x++) {

            auto a = channels[transformation.begin_c + 0].get(x, y);
            auto b = channels[transformation.begin_c + 1].get(x, y);
            auto c = channels[transformation.begin_c + 2].get(x, y);

            i32 d {};
            i32 e {};
            i32 f {};

            auto const permutation = transformation.rct_type / 7;
            auto const type = transformation.rct_type % 7;
            if (type == 6) { // YCgCo
                auto const tmp = a - (c >> 1);
                e = c + tmp;
                f = tmp - (b >> 1);
                d = f + b;
            } else {
                if (type & 1)
                    c = c + a;
                if ((type >> 1) == 1)
                    b = b + a;
                if ((type >> 1) == 2)
                    b = b + ((a + c) >> 1);
                d = a;
                e = b;
                f = c;
            }

            Array<i32, 3> v {};
            v[permutation % 3] = d;
            v[(permutation + 1 + (permutation / 3)) % 3] = e;
            v[(permutation + 2 - (permutation / 3)) % 3] = f;

            channels[transformation.begin_c + 0].set(x, y, v[0]);
            channels[transformation.begin_c + 1].set(x, y, v[1]);
            channels[transformation.begin_c + 2].set(x, y, v[2]);
        }
    }
}

static void apply_transformation(Image& image, TransformInfo const& transformation)
{
    switch (transformation.tr) {
    case TransformInfo::TransformId::kRCT:
        apply_rct(image, transformation);
        break;
    case TransformInfo::TransformId::kPalette:
    case TransformInfo::TransformId::kSqueeze:
        TODO();
    default:
        VERIFY_NOT_REACHED();
    }
}
///

/// G.3.2 - PassGroup
static ErrorOr<void> read_pass_group(LittleEndianInputBitStream& stream,
    Image& image,
    FrameHeader const& frame_header,
    u32 group_dim,
    Vector<TransformInfo> const& transform_infos)
{
    if (frame_header.encoding == FrameHeader::Encoding::kVarDCT) {
        (void)stream;
        TODO();
    }

    auto& channels = image.channels();
    for (u16 i {}; i < channels.size(); ++i) {
        // Skip meta-channels
        // FIXME: Also test if the channel has already been decoded
        //        See: nb_meta_channels in the spec
        bool const is_meta_channel = channels[i].width() <= group_dim
            || channels[i].height() <= group_dim
            || channels[i].hshift() >= 3
            || channels[i].vshift() >= 3;

        if (!is_meta_channel)
            TODO();
    }

    for (auto const& transformation : transform_infos.in_reverse())
        apply_transformation(image, transformation);

    return {};
}
///

/// Table F.1 — Frame bundle
struct Frame {
    FrameHeader frame_header;
    TOC toc;
    LfGlobal lf_global;

    u64 width {};
    u64 height {};

    u64 num_groups {};
    u64 num_lf_groups {};
};

static ErrorOr<Frame> read_frame(LittleEndianInputBitStream& stream,
    Image& image,
    SizeHeader const& size_header,
    ImageMetadata const& metadata,
    Optional<EntropyDecoder>& entropy_decoder)
{
    Frame frame;

    frame.frame_header = TRY(read_frame_header(stream, metadata));

    if (!frame.frame_header.have_crop) {
        frame.width = size_header.width;
        frame.height = size_header.height;
    } else {
        TODO();
    }

    if (frame.frame_header.upsampling > 1) {
        frame.width = ceil(frame.width / frame.frame_header.upsampling);
        frame.height = ceil(frame.height / frame.frame_header.upsampling);
    }

    if (frame.frame_header.lf_level > 0)
        TODO();

    // F.2 - FrameHeader
    auto const group_dim = 128 << frame.frame_header.group_size_shift;

    frame.num_groups = ceil(frame.width / group_dim) * ceil(frame.height / group_dim);
    frame.num_lf_groups = ceil(frame.width / (group_dim * 8)) * ceil(frame.height / (group_dim * 8));

    frame.toc = TRY(read_toc(stream, frame.frame_header, frame.num_groups, frame.num_lf_groups));

    image = TRY(Image::create({ frame.width, frame.height }));

    frame.lf_global = TRY(read_lf_global(stream, image, frame.frame_header, metadata, entropy_decoder));

    for (u32 i {}; i < frame.num_lf_groups; ++i)
        TODO();

    if (frame.frame_header.encoding == FrameHeader::Encoding::kVarDCT) {
        TODO();
    }

    auto const num_pass_group = frame.num_groups * frame.frame_header.passes.num_passes;
    auto const& transform_info = frame.lf_global.gmodular.modular_header.transform;
    for (u64 i {}; i < num_pass_group; ++i)
        TRY(read_pass_group(stream, image, frame.frame_header, group_dim, transform_info));

    return frame;
}
///

/// 5.2 - Mirroring
static u32 mirror_1d(i32 coord, u32 size)
{
    if (coord < 0)
        return mirror_1d(-coord - 1, size);
    else if (static_cast<u32>(coord) >= size)
        return mirror_1d(2 * size - 1 - coord, size);
    else
        return coord;
}
///

/// K - Image features
static ErrorOr<void> apply_upsampling(Image& image, ImageMetadata const& metadata, Frame const& frame)
{
    Optional<u32> ec_max;
    for (auto upsampling : frame.frame_header.ec_upsampling) {
        if (!ec_max.has_value() || upsampling > *ec_max)
            ec_max = upsampling;
    }

    if (frame.frame_header.upsampling > 1 || ec_max.value_or(0) > 1) {
        if (frame.frame_header.upsampling > 2 || ec_max.value_or(0) > 2)
            TODO();

        auto const k = frame.frame_header.upsampling;

        // FIXME: Use ec_upsampling for extra-channels
        for (auto& channel : image.channels()) {
            auto upsampled = TRY(Channel::create(k * channel.width(), k * channel.height()));

            // Loop over the original image
            for (u32 y {}; y < channel.height(); y++) {
                for (u32 x {}; x < channel.width(); x++) {

                    // Loop over the upsampling factor
                    for (u8 kx {}; kx < k; ++kx) {
                        for (u8 ky {}; ky < k; ++ky) {
                            double sum {};
                            // Loop over the W window
                            double W_min = NumericLimits<double>::max();
                            double W_max = -NumericLimits<double>::max();
                            for (u8 ix {}; ix < 5; ++ix) {
                                for (u8 iy {}; iy < 5; ++iy) {
                                    auto const j = (ky < k / 2) ? (iy + 5 * ky) : ((4 - iy) + 5 * (k - 1 - ky));
                                    auto const i = (kx < k / 2) ? (ix + 5 * kx) : ((4 - ix) + 5 * (k - 1 - kx));
                                    auto const minimum = min(i, j);
                                    auto const maximum = max(i, j);
                                    auto const index = 5 * k * minimum / 2 - minimum * (minimum - 1) / 2 + maximum - minimum;

                                    auto const origin_sample_x = mirror_1d(x + ix - 2, channel.width());
                                    auto const origin_sample_y = mirror_1d(y + iy - 2, channel.height());

                                    auto const origin_sample = channel.get(origin_sample_x, origin_sample_y);

                                    W_min = min(W_min, origin_sample);
                                    W_max = max(W_max, origin_sample);

                                    sum += origin_sample * metadata.up2_weight[index];
                                }
                            }

                            // The resulting sample is clamped to the range [a, b] where a and b are
                            // the minimum and maximum of the samples in W.
                            sum = clamp(sum, W_min, W_max);

                            upsampled.set(x * k + kx, y * k + ky, sum);
                        }
                    }
                }
            }
            channel = move(upsampled);
        }
    }

    return {};
}

static ErrorOr<void> apply_image_features(Image& image, ImageMetadata const& metadata, Frame const& frame)
{
    TRY(apply_upsampling(image, metadata, frame));

    if (frame.frame_header.flags != FrameHeader::Flags::None)
        TODO();
    return {};
}
///

class JPEGXLLoadingContext {
public:
    JPEGXLLoadingContext(NonnullOwnPtr<Stream> stream)
        : m_stream(move(stream))
    {
    }

    ErrorOr<void> decode_image_header()
    {
        constexpr auto JPEGXL_SIGNATURE = 0xFF0A;

        auto const signature = TRY(m_stream.read_value<BigEndian<u16>>());
        if (signature != JPEGXL_SIGNATURE)
            return Error::from_string_literal("Unrecognized signature");

        m_header = TRY(read_size_header(m_stream));
        m_metadata = TRY(read_metadata_header(m_stream));

        m_state = State::HeaderDecoded;

        return {};
    }

    ErrorOr<void> decode_frame()
    {
        Image image {};

        auto const frame = TRY(read_frame(m_stream, image, m_header, m_metadata, m_entropy_decoder));

        if (frame.frame_header.restoration_filter.gab || frame.frame_header.restoration_filter.epf_iters != 0)
            TODO();

        TRY(apply_image_features(image, m_metadata, frame));

        // FIXME: Do a proper color transformation with metadata.colour_encoding
        if (m_metadata.xyb_encoded || frame.frame_header.do_YCbCr)
            TODO();

        m_bitmap = TRY(image.to_bitmap(m_metadata.bit_depth.bits_per_sample));

        return {};
    }

    ErrorOr<void> decode()
    {
        auto result = [this]() -> ErrorOr<void> {
            // A.1 - Codestream structure

            // The header is already decoded in JPEGXLImageDecoderPlugin::create()

            if (m_metadata.colour_encoding.want_icc)
                TODO();

            if (m_metadata.preview.has_value())
                TODO();

            TRY(decode_frame());

            return {};
        }();

        m_state = result.is_error() ? State::Error : State::FrameDecoded;

        return result;
    }

    enum class State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        FrameDecoded,
        BitmapDecoded
    };

    State state() const
    {
        return m_state;
    }

    IntSize size() const
    {
        return { m_header.width, m_header.height };
    }

    RefPtr<Bitmap> bitmap() const
    {
        return m_bitmap;
    }

private:
    State m_state { State::NotDecoded };

    LittleEndianInputBitStream m_stream;
    RefPtr<Gfx::Bitmap> m_bitmap;

    Optional<EntropyDecoder> m_entropy_decoder {};

    SizeHeader m_header;
    ImageMetadata m_metadata;

    FrameHeader m_frame_header;
    TOC m_toc;
};

JPEGXLImageDecoderPlugin::JPEGXLImageDecoderPlugin(NonnullOwnPtr<FixedMemoryStream> stream)
{
    m_context = make<JPEGXLLoadingContext>(move(stream));
}

JPEGXLImageDecoderPlugin::~JPEGXLImageDecoderPlugin() = default;

IntSize JPEGXLImageDecoderPlugin::size()
{
    return m_context->size();
}

bool JPEGXLImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    return data.size() > 2
        && data.data()[0] == 0xFF
        && data.data()[1] == 0x0A;
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JPEGXLImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto stream = TRY(try_make<FixedMemoryStream>(data));
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JPEGXLImageDecoderPlugin(move(stream))));
    TRY(plugin->m_context->decode_image_header());
    return plugin;
}

bool JPEGXLImageDecoderPlugin::is_animated()
{
    return false;
}

size_t JPEGXLImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t JPEGXLImageDecoderPlugin::frame_count()
{
    return 1;
}

size_t JPEGXLImageDecoderPlugin::first_animated_frame_index()
{
    return 0;
}

ErrorOr<ImageFrameDescriptor> JPEGXLImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index > 0)
        return Error::from_string_literal("JPEGXLImageDecoderPlugin: Invalid frame index");

    if (m_context->state() == JPEGXLLoadingContext::State::Error)
        return Error::from_string_literal("JPEGXLImageDecoderPlugin: Decoding failed");

    if (m_context->state() < JPEGXLLoadingContext::State::BitmapDecoded)
        TRY(m_context->decode());

    return ImageFrameDescriptor { m_context->bitmap(), 0 };
}

ErrorOr<Optional<ReadonlyBytes>> JPEGXLImageDecoderPlugin::icc_data()
{
    return OptionalNone {};
}
}
