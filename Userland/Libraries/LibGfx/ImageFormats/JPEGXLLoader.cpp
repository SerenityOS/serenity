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
#include <LibGfx/ImageFormats/ExifOrientedBitmap.h>
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

template<Enum E>
ErrorOr<E> read_enum(LittleEndianInputBitStream& stream)
{
    return static_cast<E>(U32(0, 1, 2 + TRY(stream.read_bits(4)), 18 + TRY(stream.read_bits(6))));
}

// This is not specified
static ErrorOr<String> read_string(LittleEndianInputBitStream& stream)
{
    auto const name_length = U32(0, TRY(stream.read_bits(4)), 16 + TRY(stream.read_bits(5)), 48 + TRY(stream.read_bits(10)));
    auto string_buffer = TRY(FixedArray<u8>::create(name_length));
    TRY(stream.read_until_filled(string_buffer.span()));
    return String::from_utf8(StringView { string_buffer.span() });
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

    enum class TransferFunction {
        k709 = 1,
        kUnknown = 2,
        kLinear = 8,
        kSRGB = 13,
        kPQ = 16,
        kDCI = 17,
        kHLG = 18,
    };

    struct CustomTransferFunction {
        bool have_gamma { false };
        u32 gamma {};
        TransferFunction transfer_function { TransferFunction::kSRGB };
    };

    bool want_icc = false;
    ColourSpace colour_space { ColourSpace::kRGB };
    WhitePoint white_point { WhitePoint::kD65 };
    Primaries primaries { Primaries::kSRGB };

    Customxy white {};
    Customxy red {};
    Customxy green {};
    Customxy blue {};

    CustomTransferFunction tf {};

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

static ErrorOr<ColourEncoding::CustomTransferFunction> read_custom_transfer_function(LittleEndianInputBitStream& stream)
{
    ColourEncoding::CustomTransferFunction custom_transfer_function;

    custom_transfer_function.have_gamma = TRY(stream.read_bit());

    if (custom_transfer_function.have_gamma)
        custom_transfer_function.gamma = TRY(stream.read_bits(24));
    else
        custom_transfer_function.transfer_function = TRY(read_enum<ColourEncoding::TransferFunction>(stream));

    return custom_transfer_function;
}

static ErrorOr<ColourEncoding> read_colour_encoding(LittleEndianInputBitStream& stream)
{
    ColourEncoding colour_encoding;
    bool const all_default = TRY(stream.read_bit());

    if (!all_default) {
        colour_encoding.want_icc = TRY(stream.read_bit());
        colour_encoding.colour_space = TRY(read_enum<ColourEncoding::ColourSpace>(stream));

        auto const use_desc = !all_default && !colour_encoding.want_icc;
        auto const not_xyb = colour_encoding.colour_space != ColourEncoding::ColourSpace::kXYB;

        if (use_desc && not_xyb)
            colour_encoding.white_point = TRY(read_enum<ColourEncoding::WhitePoint>(stream));

        if (colour_encoding.white_point == ColourEncoding::WhitePoint::kCustom)
            colour_encoding.white = TRY(read_custom_xy(stream));

        auto const has_primaries = use_desc && not_xyb && colour_encoding.colour_space != ColourEncoding::ColourSpace::kGrey;

        if (has_primaries)
            colour_encoding.primaries = TRY(read_enum<ColourEncoding::Primaries>(stream));

        if (colour_encoding.primaries == ColourEncoding::Primaries::kCustom) {
            colour_encoding.red = TRY(read_custom_xy(stream));
            colour_encoding.green = TRY(read_custom_xy(stream));
            colour_encoding.blue = TRY(read_custom_xy(stream));
        }

        if (use_desc) {
            colour_encoding.tf = TRY(read_custom_transfer_function(stream));
            colour_encoding.rendering_intent = TRY(read_enum<ColourEncoding::RenderingIntent>(stream));
        }
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
Array s_d_up2 {
    -0.01716200, -0.03452303, -0.04022174, -0.02921014, -0.00624645,
    0.14111091, 0.28896755, 0.00278718, -0.01610267, 0.56661550,
    0.03777607, -0.01986694, -0.03144731, -0.01185068, -0.00213539
};

Array s_d_up4 = {
    -0.02419067, -0.03491987, -0.03693351, -0.03094285, -0.00529785,
    -0.01663432, -0.03556863, -0.03888905, -0.03516850, -0.00989469,
    0.23651958, 0.33392945, -0.01073543, -0.01313181, -0.03556694,
    0.13048175, 0.40103025, 0.03951150, -0.02077584, 0.46914198,
    -0.00209270, -0.01484589, -0.04064806, 0.18942530, 0.56279892,
    0.06674400, -0.02335494, -0.03551682, -0.00754830, -0.02267919,
    -0.02363578, 0.00315804, -0.03399098, -0.01359519, -0.00091653,
    -0.00335467, -0.01163294, -0.01610294, -0.00974088, -0.00191622,
    -0.01095446, -0.03198464, -0.04455121, -0.02799790, -0.00645912,
    0.06390599, 0.22963888, 0.00630981, -0.01897349, 0.67537268,
    0.08483369, -0.02534994, -0.02205197, -0.01667999, -0.00384443
};

Array s_d_up8 {
    -0.02928613, -0.03706353, -0.03783812, -0.03324558, -0.00447632, -0.02519406, -0.03752601, -0.03901508, -0.03663285, -0.00646649,
    -0.02066407, -0.03838633, -0.04002101, -0.03900035, -0.00901973, -0.01626393, -0.03954148, -0.04046620, -0.03979621, -0.01224485,
    0.29895328, 0.35757708, -0.02447552, -0.01081748, -0.04314594, 0.23903219, 0.41119301, -0.00573046, -0.01450239, -0.04246845,
    0.17567618, 0.45220643, 0.02287757, -0.01936783, -0.03583255, 0.11572472, 0.47416733, 0.06284440, -0.02685066, 0.42720050,
    -0.02248939, -0.01155273, -0.04562755, 0.28689496, 0.49093869, -0.00007891, -0.01545926, -0.04562659, 0.21238920, 0.53980934,
    0.03369474, -0.02070211, -0.03866988, 0.14229550, 0.56593398, 0.08045181, -0.02888298, -0.03680918, -0.00542229, -0.02920477,
    -0.02788574, -0.02118180, -0.03942402, -0.00775547, -0.02433614, -0.03193943, -0.02030828, -0.04044014, -0.01074016, -0.01930822,
    -0.03620399, -0.01974125, -0.03919545, -0.01456093, -0.00045072, -0.00360110, -0.01020207, -0.01231907, -0.00638988, -0.00071592,
    -0.00279122, -0.00957115, -0.01288327, -0.00730937, -0.00107783, -0.00210156, -0.00890705, -0.01317668, -0.00813895, -0.00153491,
    -0.02128481, -0.04173044, -0.04831487, -0.03293190, -0.00525260, -0.01720322, -0.04052736, -0.05045706, -0.03607317, -0.00738030,
    -0.01341764, -0.03965629, -0.05151616, -0.03814886, -0.01005819, 0.18968273, 0.33063684, -0.01300105, -0.01372950, -0.04017465,
    0.13727832, 0.36402234, 0.01027890, -0.01832107, -0.03365072, 0.08734506, 0.38194295, 0.04338228, -0.02525993, 0.56408126,
    0.00458352, -0.01648227, -0.04887868, 0.24585519, 0.62026135, 0.04314807, -0.02213737, -0.04158014, 0.16637289, 0.65027023,
    0.09621636, -0.03101388, -0.04082742, -0.00904519, -0.02790922, -0.02117818, 0.00798662, -0.03995711, -0.01243427, -0.02231705,
    -0.02946266, 0.00992055, -0.03600283, -0.01684920, -0.00111684, -0.00411204, -0.01297130, -0.01723725, -0.01022545, -0.00165306,
    -0.00313110, -0.01218016, -0.01763266, -0.01125620, -0.00231663, -0.01374149, -0.03797620, -0.05142937, -0.03117307, -0.00581914,
    -0.01064003, -0.03608089, -0.05272168, -0.03375670, -0.00795586, 0.09628104, 0.27129991, -0.00353779, -0.01734151, -0.03153981,
    0.05686230, 0.28500998, 0.02230594, -0.02374955, 0.68214326, 0.05018048, -0.02320852, -0.04383616, 0.18459474, 0.71517975,
    0.10805613, -0.03263677, -0.03637639, -0.01394373, -0.02511203, -0.01728636, 0.05407331, -0.02867568, -0.01893131, -0.00240854,
    -0.00446511, -0.01636187, -0.02377053, -0.01522848, -0.00333334, -0.00819975, -0.02964169, -0.04499287, -0.02745350, -0.00612408,
    0.02727416, 0.19446600, 0.00159832, -0.02232473, 0.74982506, 0.11452620, -0.03348048, -0.01605681, -0.02070339, -0.00458223
};
///

/// D.3 - Image metadata

struct PreviewHeader {
};

struct AnimationHeader {
};

struct ExtraChannelInfo {
    enum class ExtraChannelType {
        kAlpha = 0,
        kDepth = 1,
        kSpotColour = 2,
        kSelectionMask = 3,
        kBlack = 4,
        kCFA = 5,
        kThermal = 6,
        kNonOptional = 15,
        kOptional = 16,
    };

    bool d_alpha { true };
    ExtraChannelType type { ExtraChannelType::kAlpha };
    BitDepth bit_depth {};
    u32 dim_shift {};
    String name;
    bool alpha_associated { false };
};

static ErrorOr<ExtraChannelInfo> read_extra_channel_info(LittleEndianInputBitStream& stream)
{
    ExtraChannelInfo extra_channel_info;

    extra_channel_info.d_alpha = TRY(stream.read_bit());

    if (!extra_channel_info.d_alpha) {
        extra_channel_info.type = TRY(read_enum<ExtraChannelInfo::ExtraChannelType>(stream));
        extra_channel_info.bit_depth = TRY(read_bit_depth(stream));
        extra_channel_info.dim_shift = U32(0, 3, 4, 1 + TRY(stream.read_bits(3)));
        extra_channel_info.name = TRY(read_string(stream));

        if (extra_channel_info.type == ExtraChannelInfo::ExtraChannelType::kAlpha)
            extra_channel_info.alpha_associated = TRY(stream.read_bit());
    }

    if (extra_channel_info.type != ExtraChannelInfo::ExtraChannelType::kAlpha) {
        TODO();
    }

    return extra_channel_info;
}

struct ToneMapping {
    float intensity_target { 255 };
    float min_nits { 0 };
    bool relative_to_max_display { false };
    float linear_below { 0 };
};

static ErrorOr<ToneMapping> read_tone_mapping(LittleEndianInputBitStream& stream)
{
    ToneMapping tone_mapping;
    bool const all_default = TRY(stream.read_bit());

    if (!all_default) {
        TODO();
    }

    return tone_mapping;
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
    Array<double, 55> up4_weight = s_d_up4;
    Array<double, 210> up8_weight = s_d_up8;

    u16 number_of_color_channels() const
    {
        if (!xyb_encoded && colour_encoding.colour_space == ColourEncoding::ColourSpace::kGrey)
            return 1;
        return 3;
    }

    u16 number_of_channels() const
    {
        return number_of_color_channels() + num_extra_channels;
    }

    Optional<u16> alpha_channel() const
    {
        for (u16 i = 0; i < ec_info.size(); ++i) {
            if (ec_info[i].type == ExtraChannelInfo::ExtraChannelType::kAlpha)
                return i + number_of_color_channels();
        }

        return OptionalNone {};
    }
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
    bool clamp { false };
    u8 source {};
};

static ErrorOr<BlendingInfo> read_blending_info(LittleEndianInputBitStream& stream, ImageMetadata const& metadata, bool full_frame)
{
    BlendingInfo blending_info;

    blending_info.mode = static_cast<BlendingInfo::BlendMode>(U32(0, 1, 2, 3 + TRY(stream.read_bits(2))));

    bool const extra = metadata.num_extra_channels > 0;

    if (extra) {
        auto const blend_or_mul_add = blending_info.mode == BlendingInfo::BlendMode::kBlend
            || blending_info.mode == BlendingInfo::BlendMode::kMulAdd;

        if (blend_or_mul_add)
            blending_info.alpha_channel = U32(0, 1, 2, 3 + TRY(stream.read_bits(3)));

        if (blend_or_mul_add || blending_info.mode == BlendingInfo::BlendMode::kMul)
            blending_info.clamp = TRY(stream.read_bit());
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
    FixedArray<u8> ec_upsampling {};

    u8 group_size_shift { 1 };
    Passes passes {};

    u8 lf_level {};
    bool have_crop { false };

    BlendingInfo blending_info {};
    FixedArray<BlendingInfo> ec_blending_info {};

    u32 duration {};

    bool is_last { true };
    u8 save_as_reference {};
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

            frame_header.ec_upsampling = TRY(FixedArray<u8>::create(metadata.num_extra_channels));
            for (u16 i {}; i < metadata.num_extra_channels; ++i)
                frame_header.ec_upsampling[i] = U32(1, 2, 4, 8);
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

        // FIXME: also consider "cropped" image of the dimension of the frame
        VERIFY(!frame_header.have_crop);
        bool const full_frame = !frame_header.have_crop;

        if (normal_frame) {
            frame_header.blending_info = TRY(read_blending_info(stream, metadata, full_frame));

            frame_header.ec_blending_info = TRY(FixedArray<BlendingInfo>::create(metadata.num_extra_channels));
            for (u16 i {}; i < metadata.num_extra_channels; ++i)
                frame_header.ec_blending_info[i] = TRY(read_blending_info(stream, metadata, full_frame));

            if (metadata.animation.has_value())
                TODO();

            frame_header.is_last = TRY(stream.read_bit());
        }

        // FIXME: Ensure that is_last has the correct default value
        VERIFY(normal_frame);

        auto const resets_canvas = full_frame && frame_header.blending_info.mode == BlendingInfo::BlendMode::kReplace;
        auto const can_reference = !frame_header.is_last && (frame_header.duration == 0 || frame_header.save_as_reference != 0) && frame_header.frame_type != FrameHeader::FrameType::kLFFrame;

        if (frame_header.frame_type != FrameHeader::FrameType::kLFFrame) {
            if (!frame_header.is_last)
                TODO();
        }

        frame_header.save_before_ct = !normal_frame;
        if (frame_header.frame_type == FrameHeader::FrameType::kReferenceOnly || (resets_canvas && can_reference))
            frame_header.save_before_ct = TRY(stream.read_bit());

        frame_header.name = TRY(read_string(stream));

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
    // F.3.1 - General
    if (num_groups == 1 && frame_header.passes.num_passes == 1)
        return 1;

    return 1 + num_lf_groups + 1 + num_groups * frame_header.passes.num_passes;
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
class ANSHistogram {
public:
    static ErrorOr<ANSHistogram> read_histogram(LittleEndianInputBitStream& stream, u8 log_alphabet_size)
    {
        ANSHistogram histogram;

        auto const alphabet_size = TRY(histogram.read_ans_distribution(stream, log_alphabet_size));

        // C.2.6 - Alias mapping

        histogram.m_log_bucket_size = 12 - log_alphabet_size;
        histogram.m_bucket_size = 1 << histogram.m_log_bucket_size;
        auto const table_size = 1 << log_alphabet_size;

        Optional<u64> index_of_unique_symbol {};
        for (u64 i {}; i < histogram.m_distribution.size(); ++i) {
            if (histogram.m_distribution[i] == 1 << 12)
                index_of_unique_symbol = i;
        }

        TRY(histogram.m_symbols.try_resize(table_size));
        TRY(histogram.m_offsets.try_resize(table_size));
        TRY(histogram.m_cutoffs.try_resize(table_size));

        if (index_of_unique_symbol.has_value()) {
            auto const s = *index_of_unique_symbol;
            for (i32 i = 0; i < table_size; i++) {
                histogram.m_symbols[i] = s;
                histogram.m_offsets[i] = histogram.m_bucket_size * i;
                histogram.m_cutoffs[i] = 0;
            }
            return histogram;
        }

        Vector<u16> overfull;
        Vector<u16> underfull;

        for (u16 i {}; i < alphabet_size; i++) {
            histogram.m_cutoffs[i] = histogram.m_distribution[i];
            histogram.m_symbols[i] = i;
            if (histogram.m_cutoffs[i] > histogram.m_bucket_size)
                TRY(overfull.try_append(i));
            else if (histogram.m_cutoffs[i] < histogram.m_bucket_size)
                TRY(underfull.try_append(i));
        }

        for (u16 i = alphabet_size; i < table_size; i++) {
            histogram.m_cutoffs[i] = 0;
            TRY(underfull.try_append(i));
        }

        while (overfull.size() > 0) {
            VERIFY(underfull.size() > 0);
            auto const o = overfull.take_last();
            auto const u = underfull.take_last();

            auto const by = histogram.m_bucket_size - histogram.m_cutoffs[u];
            histogram.m_cutoffs[o] -= by;
            histogram.m_symbols[u] = o;
            histogram.m_offsets[u] = histogram.m_cutoffs[o];
            if (histogram.m_cutoffs[o] < histogram.m_bucket_size)
                TRY(underfull.try_append(o));
            else if (histogram.m_cutoffs[o] > histogram.m_bucket_size)
                TRY(overfull.try_append(o));
        }

        for (u16 i {}; i < table_size; i++) {
            if (histogram.m_cutoffs[i] == histogram.m_bucket_size) {
                histogram.m_symbols[i] = i;
                histogram.m_offsets[i] = 0;
                histogram.m_cutoffs[i] = 0;
            } else {
                histogram.m_offsets[i] -= histogram.m_cutoffs[i];
            }
        }

        return histogram;
    }

    ErrorOr<u16> read_symbol(LittleEndianInputBitStream& stream, Optional<u32>& state) const
    {
        if (!state.has_value())
            state = TRY(stream.read_bits(32));

        auto const index = *state & 0xFFF;
        auto const symbol_and_offset = alias_mapping(index);
        state = m_distribution[symbol_and_offset.symbol] * (*state >> 12) + symbol_and_offset.offset;
        if (*state < (1 << 16))
            state = (*state << 16) | TRY(stream.read_bits(16));
        return symbol_and_offset.symbol;
    }

private:
    static ErrorOr<u8> U8(LittleEndianInputBitStream& stream)
    {
        if (TRY(stream.read_bit()) == 0)
            return 0;
        auto const n = TRY(stream.read_bits(3));
        return TRY(stream.read_bits(n)) + (1 << n);
    }

    struct SymbolAndOffset {
        u16 symbol {};
        u16 offset {};
    };

    SymbolAndOffset alias_mapping(u32 x) const
    {
        // C.2.6 - Alias mapping
        auto const i = x >> m_log_bucket_size;
        auto const pos = x & (m_bucket_size - 1);
        u16 const symbol = pos >= m_cutoffs[i] ? m_symbols[i] : i;
        u16 const offset = pos >= m_cutoffs[i] ? m_offsets[i] + pos : pos;

        return { symbol, offset };
    }

    static ErrorOr<u16> read_with_prefix(LittleEndianInputBitStream& stream)
    {
        auto const prefix = TRY(stream.read_bits(3));

        switch (prefix) {
        case 0:
            return 10;
        case 1:
            for (auto const possibility : { 4, 0, 11, 13 }) {
                if (TRY(stream.read_bit()))
                    return possibility;
            }
            return 12;
        case 2:
            return 7;
        case 3:
            return TRY(stream.read_bit()) ? 1 : 3;
        case 4:
            return 6;
        case 5:
            return 8;
        case 6:
            return 9;
        case 7:
            return TRY(stream.read_bit()) ? 2 : 5;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ErrorOr<u16> read_ans_distribution(LittleEndianInputBitStream& stream, u8 log_alphabet_size)
    {
        // C.2.5  ANS distribution decoding
        auto const table_size = 1 << log_alphabet_size;

        m_distribution = TRY(FixedArray<i32>::create(table_size));

        if (TRY(stream.read_bit())) {
            u16 alphabet_size {};
            if (TRY(stream.read_bit())) {
                auto const v1 = TRY(U8(stream));
                auto const v2 = TRY(U8(stream));
                VERIFY(v1 != v2);
                m_distribution[v1] = TRY(stream.read_bits(12));
                m_distribution[v2] = (1 << 12) - m_distribution[v1];
                alphabet_size = 1 + max(v1, v2);
            } else {
                auto const x = TRY(U8(stream));
                m_distribution[x] = 1 << 12;
                alphabet_size = 1 + x;
            }
            return alphabet_size;
        }

        if (TRY(stream.read_bit())) {
            auto const alphabet_size = TRY(U8(stream)) + 1;
            for (u16 i = 0; i < alphabet_size; i++)
                m_distribution[i] = (1 << 12) / alphabet_size;
            for (u16 i = 0; i < ((1 << 12) % alphabet_size); i++)
                m_distribution[i]++;
            return alphabet_size;
        }

        u8 len = 0;
        while (len < 3) {
            if (TRY(stream.read_bit()))
                len++;
            else
                break;
        }

        u8 const shift = TRY(stream.read_bits(len)) + (1 << len) - 1;
        VERIFY(shift <= 13);

        auto const alphabet_size = TRY(U8(stream)) + 3;

        i32 omit_log = -1;
        i32 omit_pos = -1;

        auto same = TRY(FixedArray<i32>::create(alphabet_size));
        auto logcounts = TRY(FixedArray<i32>::create(alphabet_size));

        u8 rle {};
        for (u16 i = 0; i < alphabet_size; i++) {
            logcounts[i] = TRY(read_with_prefix(stream));

            if (logcounts[i] == 13) {
                rle = TRY(U8(stream));
                same[i] = rle + 5;
                i += rle + 3;
                continue;
            }
            if (logcounts[i] > omit_log) {
                omit_log = logcounts[i];
                omit_pos = i;
            }
        }

        VERIFY(m_distribution[omit_pos] >= 0);
        VERIFY(omit_pos + 1 >= alphabet_size || logcounts[omit_pos + 1] != 13);

        i32 prev = 0;
        i32 numsame = 0;
        i64 total_count {};
        for (u16 i = 0; i < alphabet_size; i++) {
            if (same[i] != 0) {
                numsame = same[i] - 1;
                prev = i > 0 ? m_distribution[i - 1] : 0;
            }
            if (numsame > 0) {
                m_distribution[i] = prev;
                numsame--;
            } else {
                auto const code = logcounts[i];
                if (i == omit_pos || code == 0)
                    continue;

                if (code == 1) {
                    m_distribution[i] = 1;
                } else {
                    auto const bitcount = min(max(0, shift - ((12 - code + 1) >> 1)), code - 1);
                    m_distribution[i] = (1 << (code - 1)) + (TRY(stream.read_bits(bitcount)) << (code - 1 - bitcount));
                }
            }
            total_count += m_distribution[i];
        }
        m_distribution[omit_pos] = (1 << 12) - total_count;
        VERIFY(m_distribution[omit_pos] >= 0);

        return alphabet_size;
    }

    Vector<u16> m_symbols;
    Vector<u16> m_offsets;
    Vector<u16> m_cutoffs;

    FixedArray<i32> m_distribution;

    u16 m_log_bucket_size {};
    u16 m_bucket_size {};
};

struct LZ77 {
    bool lz77_enabled {};

    u32 min_symbol {};
    u32 min_length {};
};

static ErrorOr<LZ77> read_lz77(LittleEndianInputBitStream& stream)
{
    LZ77 lz77;

    lz77.lz77_enabled = TRY(stream.read_bit());

    if (lz77.lz77_enabled) {
        lz77.min_symbol = U32(224, 512, 4096, 8 + TRY(stream.read_bits(15)));
        lz77.min_length = U32(3, 4, 5 + TRY(stream.read_bits(2)), 9 + TRY(stream.read_bits(8)));
    }

    return lz77;
}

class EntropyDecoder {
    AK_MAKE_NONCOPYABLE(EntropyDecoder);
    AK_MAKE_DEFAULT_MOVABLE(EntropyDecoder);

public:
    EntropyDecoder() = default;
    ~EntropyDecoder()
    {
        if (m_state.has_value() && *m_state != 0x130000)
            dbgln("JPEGXLLoader: ANS decoder left in invalid state");
    }

    static ErrorOr<EntropyDecoder> create(LittleEndianInputBitStream& stream, u32 initial_num_distrib)
    {
        EntropyDecoder entropy_decoder;
        // C.2 - Distribution decoding
        entropy_decoder.m_lz77 = TRY(read_lz77(stream));

        if (entropy_decoder.m_lz77.lz77_enabled) {
            entropy_decoder.m_lz_dist_ctx = initial_num_distrib++;
            entropy_decoder.m_lz_len_conf = TRY(read_config(stream, 8));

            entropy_decoder.m_lz77_window = TRY(FixedArray<u32>::create(1 << 20));
        }

        TRY(entropy_decoder.read_pre_clustered_distributions(stream, initial_num_distrib));

        bool const use_prefix_code = TRY(stream.read_bit());

        if (!use_prefix_code)
            entropy_decoder.m_log_alphabet_size = 5 + TRY(stream.read_bits(2));

        for (auto& config : entropy_decoder.m_configs)
            config = TRY(read_config(stream, entropy_decoder.m_log_alphabet_size));

        if (use_prefix_code) {
            entropy_decoder.m_distributions = Vector<BrotliCanonicalCode> {};
            auto& distributions = entropy_decoder.m_distributions.get<Vector<BrotliCanonicalCode>>();
            TRY(distributions.try_resize(entropy_decoder.m_configs.size()));

            Vector<u16> counts;
            TRY(counts.try_resize(entropy_decoder.m_configs.size()));

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
            for (u32 i {}; i < distributions.size(); ++i) {
                // The alphabet size mentioned in the [Brotli] RFC is explicitly specified as parameter alphabet_size
                // when the histogram is being decoded, except in the special case of alphabet_size == 1, where no
                // histogram is read, and all decoded symbols are zero without reading any bits at all.
                if (counts[i] != 1)
                    distributions[i] = TRY(BrotliCanonicalCode::read_prefix_code(stream, counts[i]));
                else
                    distributions[i] = BrotliCanonicalCode { { 1 }, { 0 } };
            }
        } else {
            entropy_decoder.m_distributions = Vector<ANSHistogram> {};
            auto& distributions = entropy_decoder.m_distributions.get<Vector<ANSHistogram>>();
            TRY(distributions.try_ensure_capacity(entropy_decoder.m_configs.size()));

            for (u32 i = 0; i < entropy_decoder.m_configs.size(); ++i)
                distributions.empend(TRY(ANSHistogram::read_histogram(stream, entropy_decoder.m_log_alphabet_size)));
        }

        return entropy_decoder;
    }

    ErrorOr<u32> decode_hybrid_uint(LittleEndianInputBitStream& stream, u32 context)
    {
        // C.3.3 - Hybrid integer decoding

        static constexpr Array<Array<i8, 2>, 120> kSpecialDistances = {
            Array<i8, 2> { 0, 1 }, { 1, 0 }, { 1, 1 }, { -1, 1 }, { 0, 2 }, { 2, 0 }, { 1, 2 }, { -1, 2 }, { 2, 1 }, { -2, 1 }, { 2, 2 },
            { -2, 2 }, { 0, 3 }, { 3, 0 }, { 1, 3 }, { -1, 3 }, { 3, 1 }, { -3, 1 }, { 2, 3 }, { -2, 3 }, { 3, 2 },
            { -3, 2 }, { 0, 4 }, { 4, 0 }, { 1, 4 }, { -1, 4 }, { 4, 1 }, { -4, 1 }, { 3, 3 }, { -3, 3 }, { 2, 4 },
            { -2, 4 }, { 4, 2 }, { -4, 2 }, { 0, 5 }, { 3, 4 }, { -3, 4 }, { 4, 3 }, { -4, 3 }, { 5, 0 }, { 1, 5 },
            { -1, 5 }, { 5, 1 }, { -5, 1 }, { 2, 5 }, { -2, 5 }, { 5, 2 }, { -5, 2 }, { 4, 4 }, { -4, 4 }, { 3, 5 },
            { -3, 5 }, { 5, 3 }, { -5, 3 }, { 0, 6 }, { 6, 0 }, { 1, 6 }, { -1, 6 }, { 6, 1 }, { -6, 1 }, { 2, 6 },
            { -2, 6 }, { 6, 2 }, { -6, 2 }, { 4, 5 }, { -4, 5 }, { 5, 4 }, { -5, 4 }, { 3, 6 }, { -3, 6 }, { 6, 3 },
            { -6, 3 }, { 0, 7 }, { 7, 0 }, { 1, 7 }, { -1, 7 }, { 5, 5 }, { -5, 5 }, { 7, 1 }, { -7, 1 }, { 4, 6 },
            { -4, 6 }, { 6, 4 }, { -6, 4 }, { 2, 7 }, { -2, 7 }, { 7, 2 }, { -7, 2 }, { 3, 7 }, { -3, 7 }, { 7, 3 },
            { -7, 3 }, { 5, 6 }, { -5, 6 }, { 6, 5 }, { -6, 5 }, { 8, 0 }, { 4, 7 }, { -4, 7 }, { 7, 4 }, { -7, 4 },
            { 8, 1 }, { 8, 2 }, { 6, 6 }, { -6, 6 }, { 8, 3 }, { 5, 7 }, { -5, 7 }, { 7, 5 }, { -7, 5 }, { 8, 4 }, { 6, 7 },
            { -6, 7 }, { 7, 6 }, { -7, 6 }, { 8, 5 }, { 7, 7 }, { -7, 7 }, { 8, 6 }, { 8, 7 }
        };

        u32 r {};
        if (m_lz77_num_to_copy > 0) {
            r = m_lz77_window[(m_lz77_copy_pos++) & 0xFFFFF];
            m_lz77_num_to_copy--;
        } else {
            // Read symbol from entropy coded stream using D[clusters[ctx]]
            auto token = TRY(read_symbol(stream, context));

            if (m_lz77.lz77_enabled && token >= m_lz77.min_symbol) {
                m_lz77_num_to_copy = TRY(read_uint(stream, m_lz_len_conf, token - m_lz77.min_symbol)) + m_lz77.min_length;
                // Read symbol using D[clusters[lz_dist_ctx]]
                token = TRY(read_symbol(stream, m_lz_dist_ctx));
                auto distance = TRY(read_uint(stream, m_configs[m_clusters[m_lz_dist_ctx]], token));
                if (m_dist_multiplier == 0) {
                    distance++;
                } else if (distance < 120) {
                    auto const offset = kSpecialDistances[distance][0];
                    distance = offset + m_dist_multiplier * kSpecialDistances[distance][1];
                    if (distance < 1)
                        distance = 1;
                } else {
                    distance -= 119;
                }
                distance = min(distance, min(m_lz77_num_decoded, 1 << 20));
                m_lz77_copy_pos = m_lz77_num_decoded - distance;
                return decode_hybrid_uint(stream, m_clusters[context]);
            }
            r = TRY(read_uint(stream, m_configs[m_clusters[context]], token));
        }

        if (m_lz77.lz77_enabled)
            m_lz77_window[(m_lz77_num_decoded++) & 0xFFFFF] = r;

        return r;
    }

    void set_dist_multiplier(u32 dist_multiplier)
    {
        m_dist_multiplier = dist_multiplier;
    }

private:
    using BrotliCanonicalCode = Compress::Brotli::CanonicalCode;

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

    static ErrorOr<HybridUint> read_config(LittleEndianInputBitStream& stream, u8 log_alphabet_size)
    {
        // C.2.3 - Hybrid integer configuration
        HybridUint config {};
        config.split_exponent = TRY(stream.read_bits(ceil(log2(log_alphabet_size + 1))));
        if (config.split_exponent != log_alphabet_size) {
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

    ErrorOr<u32> read_symbol(LittleEndianInputBitStream& stream, u32 context)
    {
        u32 token {};
        TRY(m_distributions.visit(
            [&](Vector<BrotliCanonicalCode> const& distributions) -> ErrorOr<void> {
                token = TRY(distributions[m_clusters[context]].read_symbol(stream));
                return {};
            },
            [&](Vector<ANSHistogram> const& distributions) -> ErrorOr<void> {
                token = TRY(distributions[m_clusters[context]].read_symbol(stream, m_state));
                return {};
            }));
        return token;
    }

    ErrorOr<void> read_pre_clustered_distributions(LittleEndianInputBitStream& stream, u32 num_distrib)
    {
        // C.2.2  Distribution clustering
        if (num_distrib == 1) {
            // If num_dist == 1, then num_clusters = 1 and clusters[0] = 0, and the remainder of this subclause is skipped.
            m_clusters = { 0 };
            TRY(m_configs.try_resize(1));
            return {};
        };

        TRY(m_clusters.try_resize(num_distrib));

        bool const is_simple = TRY(stream.read_bit());

        u16 num_clusters = 0;

        auto const read_clusters = [&](auto&& reader) -> ErrorOr<void> {
            for (u32 i {}; i < num_distrib; ++i) {
                m_clusters[i] = TRY(reader());
                if (m_clusters[i] >= num_clusters)
                    num_clusters = m_clusters[i] + 1;
            }
            return {};
        };

        if (is_simple) {
            u8 const nbits = TRY(stream.read_bits(2));
            TRY(read_clusters([nbits, &stream]() { return stream.read_bits(nbits); }));
        } else {
            auto const use_mtf = TRY(stream.read_bit());
            if (num_distrib == 2)
                TODO();

            auto decoder = TRY(EntropyDecoder::create(stream, 1));

            TRY(read_clusters([&]() { return decoder.decode_hybrid_uint(stream, 0); }));

            if (use_mtf)
                TODO();
        }
        TRY(m_configs.try_resize(num_clusters));
        return {};
    }

    LZ77 m_lz77 {};
    u32 m_lz_dist_ctx {};
    HybridUint m_lz_len_conf {};
    FixedArray<u32> m_lz77_window {};
    u32 m_lz77_num_to_copy {};
    u32 m_lz77_copy_pos {};
    u32 m_lz77_num_decoded {};
    u32 m_dist_multiplier {};

    Vector<u32> m_clusters;
    Vector<HybridUint> m_configs;

    u8 m_log_alphabet_size { 15 };

    Variant<Vector<BrotliCanonicalCode>, Vector<ANSHistogram>> m_distributions { Vector<BrotliCanonicalCode> {} }; // D in the spec
    Optional<u32> m_state {};
};
///

/// H.4.2 - MA tree decoding
class MATree {
public:
    struct LeafNode {
        u32 ctx {};
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
    Array<u8, 4> wp_w { 13, 12, 12, 12 };
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
        return m_pixels[y * m_width + x];
    }

    void set(u32 x, u32 y, i32 value)
    {
        m_pixels[y * m_width + x] = value;
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

    bool decoded() const
    {
        return m_decoded;
    }

    void set_decoded(bool decoded)
    {
        m_decoded = decoded;
    }

private:
    u32 m_width {};
    u32 m_height {};

    u32 m_hshift {};
    u32 m_vshift {};

    bool m_decoded { false };

    Vector<i32> m_pixels {};
};

class Image {
public:
    static ErrorOr<Image> create(IntSize size, ImageMetadata const& metadata)
    {
        Image image {};

        for (u16 i = 0; i < metadata.number_of_channels(); ++i) {
            if (i < metadata.number_of_color_channels()) {
                TRY(image.m_channels.try_append(TRY(Channel::create(size.width(), size.height()))));
            } else {
                auto const dim_shift = metadata.ec_info[i - metadata.number_of_color_channels()].dim_shift;
                TRY(image.m_channels.try_append(TRY(Channel::create(size.width() >> dim_shift, size.height() >> dim_shift))));
            }
        }

        return image;
    }

    ErrorOr<NonnullRefPtr<Bitmap>> to_bitmap(ImageMetadata& metadata) const
    {
        // FIXME: which channel size should we use?
        auto const width = m_channels[0].width();
        auto const height = m_channels[0].height();

        auto const orientation = static_cast<ExifOrientedBitmap::Orientation>(metadata.orientation);
        auto oriented_bitmap = TRY(ExifOrientedBitmap::create(BitmapFormat::BGRA8888, { width, height }, orientation));

        auto const alpha_channel = metadata.alpha_channel();

        auto const bits_per_sample = metadata.bit_depth.bits_per_sample;
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

                auto const color = [&]() -> Color {
                    if (!alpha_channel.has_value()) {
                        return { to_u8(m_channels[0].get(x, y)),
                            to_u8(m_channels[1].get(x, y)),
                            to_u8(m_channels[2].get(x, y)) };
                    }

                    return {
                        to_u8(m_channels[0].get(x, y)),
                        to_u8(m_channels[1].get(x, y)),
                        to_u8(m_channels[2].get(x, y)),
                        to_u8(m_channels[*alpha_channel].get(x, y)),
                    };
                }();
                oriented_bitmap.set_pixel(x, y, color);
            }
        }

        return oriented_bitmap.bitmap();
    }

    Vector<Channel>& channels()
    {
        return m_channels;
    }

private:
    Vector<Channel> m_channels;
};
///

/// H.5 - Self-correcting predictor
struct Neighborhood {
    i32 N {};
    i32 NW {};
    i32 NE {};
    i32 W {};
    i32 NN {};
    i32 WW {};
    i32 NEE {};
};

class SelfCorrectingData {
public:
    struct Predictions {
        i32 prediction {};
        Array<i32, 4> subpred {};

        i32 max_error {};
        i32 true_err {};
        Array<i32, 4> err {};
    };

    static ErrorOr<SelfCorrectingData> create(WPHeader const& wp_params, u32 width)
    {
        SelfCorrectingData self_correcting_data { wp_params };
        self_correcting_data.m_width = width;

        self_correcting_data.m_previous = TRY(FixedArray<Predictions>::create(width));
        self_correcting_data.m_current_row = TRY(FixedArray<Predictions>::create(width));
        self_correcting_data.m_next_row = TRY(FixedArray<Predictions>::create(width));

        return self_correcting_data;
    }

    void register_next_row()
    {
        auto tmp = move(m_previous);
        m_previous = move(m_current_row);
        m_current_row = move(m_next_row);
        // We reuse m_previous to avoid an allocation, no values are kept
        // everything will be overridden.
        m_next_row = move(tmp);
        m_current_row_index++;
    }

    Predictions compute_predictions(Neighborhood const& neighborhood, u32 x)
    {
        auto& current_predictions = m_next_row[x];

        auto const N3 = neighborhood.N << 3;
        auto const NW3 = neighborhood.NW << 3;
        auto const NE3 = neighborhood.NE << 3;
        auto const W3 = neighborhood.W << 3;
        auto const NN3 = neighborhood.NN << 3;

        auto const predictions_W = predictions_for(x, Direction::West);
        auto const predictions_N = predictions_for(x, Direction::North);
        auto const predictions_NE = predictions_for(x, Direction::NorthEast);
        auto const predictions_NW = predictions_for(x, Direction::NorthWest);
        auto const predictions_WW = predictions_for(x, Direction::WestWest);

        current_predictions.subpred[0] = W3 + NE3 - N3;
        current_predictions.subpred[1] = N3 - (((predictions_W.true_err + predictions_N.true_err + predictions_NE.true_err) * wp_params.wp_p1) >> 5);
        current_predictions.subpred[2] = W3 - (((predictions_W.true_err + predictions_N.true_err + predictions_NW.true_err) * wp_params.wp_p2) >> 5);
        current_predictions.subpred[3] = N3 - ((predictions_NW.true_err * wp_params.wp_p3a + predictions_N.true_err * wp_params.wp_p3b + predictions_NE.true_err * wp_params.wp_p3c + (NN3 - N3) * wp_params.wp_p3d + (NW3 - W3) * wp_params.wp_p3e) >> 5);

        auto const error2weight = [](i32 err_sum, u8 maxweight) -> i32 {
            i32 shift = floor(log2(err_sum + 1)) - 5;
            if (shift < 0)
                shift = 0;
            return 4 + ((static_cast<u64>(maxweight) * ((1 << 24) / ((err_sum >> shift) + 1))) >> shift);
        };

        Array<i32, 4> weight {};
        for (u8 i = 0; i < weight.size(); ++i) {
            auto err_sum = predictions_N.err[i] + predictions_W.err[i] + predictions_NW.err[i] + predictions_WW.err[i] + predictions_NE.err[i];
            if (x == m_width - 1)
                err_sum += predictions_W.err[i];
            weight[i] = error2weight(err_sum, wp_params.wp_w[i]);
        }

        auto sum_weights = weight[0] + weight[1] + weight[2] + weight[3];
        i32 const log_weight = floor(log2(sum_weights)) + 1;
        for (u8 i = 0; i < 4; i++)
            weight[i] = weight[i] >> (log_weight - 5);
        sum_weights = weight[0] + weight[1] + weight[2] + weight[3];

        auto s = (sum_weights >> 1) - 1;
        for (u8 i = 0; i < 4; i++)
            s += current_predictions.subpred[i] * weight[i];

        current_predictions.prediction = static_cast<u64>(s) * ((1 << 24) / sum_weights) >> 24;
        // if true_err_N, true_err_W and true_err_NW don't have the same sign
        if (((predictions_N.true_err ^ predictions_W.true_err) | (predictions_N.true_err ^ predictions_NW.true_err)) <= 0) {
            current_predictions.prediction = clamp(current_predictions.prediction, min(W3, min(N3, NE3)), max(W3, max(N3, NE3)));
        }

        auto& max_error = current_predictions.max_error;
        max_error = predictions_W.true_err;
        if (abs(predictions_N.true_err) > abs(max_error))
            max_error = predictions_N.true_err;
        if (abs(predictions_NW.true_err) > abs(max_error))
            max_error = predictions_NW.true_err;
        if (abs(predictions_NE.true_err) > abs(max_error))
            max_error = predictions_NE.true_err;

        return current_predictions;
    }

    // H.5.1 - General
    void compute_errors(u32 x, i32 true_value)
    {
        auto& current_predictions = m_next_row[x];

        current_predictions.true_err = current_predictions.prediction - (true_value << 3);

        for (u8 i = 0; i < 4; ++i)
            current_predictions.err[i] = (abs(current_predictions.subpred[i] - (true_value << 3)) + 3) >> 3;
    }

private:
    SelfCorrectingData(WPHeader const& wp)
        : wp_params(wp)
    {
    }

    enum class Direction {
        North,
        NorthWest,
        NorthEast,
        West,
        NorthNorth,
        WestWest
    };

    Predictions predictions_for(u32 x, Direction direction) const
    {
        // H.5.2 - Prediction
        auto const north = [&]() {
            return m_current_row_index < 1 ? Predictions {} : m_current_row[x];
        };

        switch (direction) {
        case Direction::North:
            return north();
        case Direction::NorthWest:
            return x < 1 ? north() : m_current_row[x - 1];
        case Direction::NorthEast:
            return x + 1 >= m_current_row.size() ? north() : m_current_row[x + 1];
        case Direction::West:
            return x < 1 ? Predictions {} : m_next_row[x - 1];
        case Direction::NorthNorth:
            return m_current_row_index < 2 ? Predictions {} : m_previous[x];
        case Direction::WestWest:
            return x < 2 ? Predictions {} : m_next_row[x - 2];
        }
        VERIFY_NOT_REACHED();
    }

    WPHeader const& wp_params {};

    u32 m_width {};
    u32 m_current_row_index {};

    FixedArray<Predictions> m_previous {};
    FixedArray<Predictions> m_current_row {};

    FixedArray<Predictions> m_next_row {};
};
///

/// H.2 - Image decoding
struct ModularHeader {
    bool use_global_tree {};
    WPHeader wp_params {};
    Vector<TransformInfo> transform {};
};

static ErrorOr<Vector<i32>> get_properties(Vector<Channel> const& channels, u16 i, u32 x, u32 y, i32 max_error)
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
    if (x > 0) {
        auto const x_1 = x - 1;
        i32 const W_x_1 = x_1 > 0 ? channels[i].get(x_1 - 1, y) : (y > 0 ? channels[i].get(x_1, y - 1) : 0);
        i32 const N_x_1 = y > 0 ? channels[i].get(x_1, y - 1) : W_x_1;
        i32 const NW_x_1 = x_1 > 0 && y > 0 ? channels[i].get(x_1 - 1, y - 1) : W_x_1;
        TRY(properties.try_append(W - (W_x_1 + N_x_1 - NW_x_1)));
    } else {
        TRY(properties.try_append(W));
    }

    TRY(properties.try_append(W + N - NW));
    TRY(properties.try_append(W - NW));
    TRY(properties.try_append(NW - N));
    TRY(properties.try_append(N - NE));
    TRY(properties.try_append(N - NN));
    TRY(properties.try_append(W - WW));

    TRY(properties.try_append(max_error));

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

static i32 prediction(Neighborhood const& neighborhood, i32 self_correcting, u32 predictor)
{
    switch (predictor) {
    case 0:
        return 0;
    case 1:
        return neighborhood.W;
    case 2:
        return neighborhood.N;
    case 3:
        return (neighborhood.W + neighborhood.N) / 2;
    case 4:
        return abs(neighborhood.N - neighborhood.NW) < abs(neighborhood.W - neighborhood.NW) ? neighborhood.W : neighborhood.N;
    case 5:
        return clamp(neighborhood.W + neighborhood.N - neighborhood.NW, min(neighborhood.W, neighborhood.N), max(neighborhood.W, neighborhood.N));
    case 6:
        return (self_correcting + 3) >> 3;
    case 7:
        return neighborhood.NE;
    case 8:
        return neighborhood.NW;
    case 9:
        return neighborhood.WW;
    case 10:
        return (neighborhood.W + neighborhood.NW) / 2;
    case 11:
        return (neighborhood.N + neighborhood.NW) / 2;
    case 12:
        return (neighborhood.N + neighborhood.NE) / 2;
    case 13:
        return (6 * neighborhood.N - 2 * neighborhood.NN + 7 * neighborhood.W + neighborhood.WW + neighborhood.NEE + 3 * neighborhood.NE + 8) / 16;
    }
    VERIFY_NOT_REACHED();
}

static Neighborhood retrieve_neighborhood(Channel const& channel, u32 x, u32 y)
{
    i32 const W = x > 0 ? channel.get(x - 1, y) : (y > 0 ? channel.get(x, y - 1) : 0);
    i32 const N = y > 0 ? channel.get(x, y - 1) : W;
    i32 const NW = x > 0 && y > 0 ? channel.get(x - 1, y - 1) : W;
    i32 const NE = x + 1 < channel.width() && y > 0 ? channel.get(x + 1, y - 1) : N;
    i32 const NN = y > 1 ? channel.get(x, y - 2) : N;
    i32 const WW = x > 1 ? channel.get(x - 2, y) : W;
    i32 const NEE = x + 2 < channel.width() && y > 0 ? channel.get(x + 2, y - 1) : NE;

    Neighborhood const neighborhood {
        .N = N,
        .NW = NW,
        .NE = NE,
        .W = W,
        .NN = NN,
        .WW = WW,
        .NEE = NEE,
    };

    return neighborhood;
}

static ErrorOr<ModularHeader> read_modular_header(LittleEndianInputBitStream& stream,
    Image& image,
    ImageMetadata const& metadata,
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

    // where dist_multiplier is set to the largest channel width amongst all channels
    // that are to be decoded, excluding the meta-channels.
    auto const dist_multiplier = [&]() {
        u32 dist_multiplier {};
        // FIXME: This should start at nb_meta_channels not 0
        for (u16 i = 0; i < metadata.number_of_channels(); ++i) {
            if (image.channels()[i].width() > dist_multiplier)
                dist_multiplier = image.channels()[i].width();
        }
        return dist_multiplier;
    }();
    decoder->set_dist_multiplier(dist_multiplier);

    // The decoder then starts an entropy-coded stream (C.1) and decodes the data for each channel
    // (in ascending order of index) as specified in H.3, skipping any channels having width or height
    // zero. Finally, the inverse transformations are applied (from last to first) as described in H.6.

    auto const& tree = local_tree.has_value() ? *local_tree : global_tree;
    for (u16 i {}; i < num_channels; ++i) {

        auto self_correcting_data = TRY(SelfCorrectingData::create(modular_header.wp_params, image.channels()[i].width()));

        for (u32 y {}; y < image.channels()[i].height(); y++) {
            for (u32 x {}; x < image.channels()[i].width(); x++) {
                auto const neighborhood = retrieve_neighborhood(image.channels()[i], x, y);

                auto const self_prediction = self_correcting_data.compute_predictions(neighborhood, x);

                auto const properties = TRY(get_properties(image.channels(), i, x, y, self_prediction.max_error));
                auto const leaf_node = tree.get_leaf(properties);
                auto diff = unpack_signed(TRY(decoder->decode_hybrid_uint(stream, leaf_node.ctx)));
                diff = (diff * leaf_node.multiplier) + leaf_node.offset;
                auto const total = diff + prediction(neighborhood, self_prediction.prediction, leaf_node.predictor);

                self_correcting_data.compute_errors(x, total);
                image.channels()[i].set(x, y, total);
            }

            self_correcting_data.register_next_row();
        }

        image.channels()[i].set_decoded(true);
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
    global_modular.modular_header = TRY(read_modular_header(stream, image, metadata, entropy_decoder, global_modular.ma_tree, num_channels));

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

/// G.2 - LfGroup
static ErrorOr<void> read_lf_group(LittleEndianInputBitStream&,
    Image& image,
    FrameHeader const& frame_header)
{
    // LF coefficients
    if (frame_header.encoding == FrameHeader::Encoding::kVarDCT) {
        TODO();
    }

    // ModularLfGroup
    for (auto const& channel : image.channels()) {
        if (channel.decoded())
            continue;

        if (channel.hshift() < 3 || channel.vshift() < 3)
            continue;

        // This code actually only detect that we need to read a null image
        // so a no-op. It should be fully rewritten when we add proper support
        // for LfGroup.
        TODO();
    }

    // HF metadata
    if (frame_header.encoding == FrameHeader::Encoding::kVarDCT) {
        TODO();
    }

    return {};
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
    u32 group_dim)
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
    // F.1 - General
    // Each Frame is byte-aligned by invoking ZeroPadToByte() (B.2.7)
    stream.align_to_byte_boundary();

    Frame frame;

    frame.frame_header = TRY(read_frame_header(stream, metadata));

    if (!frame.frame_header.have_crop) {
        frame.width = size_header.width;
        frame.height = size_header.height;
    } else {
        TODO();
    }

    if (frame.frame_header.upsampling > 1) {
        frame.width = ceil(static_cast<double>(frame.width) / frame.frame_header.upsampling);
        frame.height = ceil(static_cast<double>(frame.height) / frame.frame_header.upsampling);
    }

    if (frame.frame_header.lf_level > 0)
        TODO();

    // F.2 - FrameHeader
    auto const group_dim = 128 << frame.frame_header.group_size_shift;

    auto const frame_width = static_cast<double>(frame.width);
    auto const frame_height = static_cast<double>(frame.height);
    frame.num_groups = ceil(frame_width / group_dim) * ceil(frame_height / group_dim);
    frame.num_lf_groups = ceil(frame_width / (group_dim * 8)) * ceil(frame_height / (group_dim * 8));

    frame.toc = TRY(read_toc(stream, frame.frame_header, frame.num_groups, frame.num_lf_groups));

    image = TRY(Image::create({ frame.width, frame.height }, metadata));

    frame.lf_global = TRY(read_lf_global(stream, image, frame.frame_header, metadata, entropy_decoder));

    for (u32 i {}; i < frame.num_lf_groups; ++i)
        TRY(read_lf_group(stream, image, frame.frame_header));

    if (frame.frame_header.encoding == FrameHeader::Encoding::kVarDCT) {
        TODO();
    }

    auto const num_pass_group = frame.num_groups * frame.frame_header.passes.num_passes;
    auto const& transform_infos = frame.lf_global.gmodular.modular_header.transform;
    for (u64 i {}; i < num_pass_group; ++i)
        TRY(read_pass_group(stream, image, frame.frame_header, group_dim));

    // G.4.2 - Modular group data
    // When all modular groups are decoded, the inverse transforms are applied to
    // the at that point fully decoded GlobalModular image, as specified in H.6.
    for (auto const& transformation : transform_infos.in_reverse())
        apply_transformation(image, transformation);

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
        if (ec_max.value_or(0) > 2)
            TODO();

        auto const k = frame.frame_header.upsampling;

        auto weight = [k, &metadata](u8 index) -> double {
            if (k == 2)
                return metadata.up2_weight[index];
            if (k == 4)
                return metadata.up4_weight[index];
            return metadata.up8_weight[index];
        };

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

                                    sum += origin_sample * weight(index);
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

/// L.4 - Extra channel rendering
static ErrorOr<void> render_extra_channels(Image&, ImageMetadata const& metadata)
{
    for (u16 i = metadata.number_of_color_channels(); i < metadata.number_of_channels(); ++i) {
        auto const ec_index = i - metadata.number_of_color_channels();
        if (metadata.ec_info[ec_index].dim_shift != 0)
            TODO();
    }

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

        TRY(render_extra_channels(image, m_metadata));

        m_bitmap = TRY(image.to_bitmap(m_metadata));

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

    if (m_context->state() < JPEGXLLoadingContext::State::FrameDecoded)
        TRY(m_context->decode());

    return ImageFrameDescriptor { m_context->bitmap(), 0 };
}

ErrorOr<Optional<ReadonlyBytes>> JPEGXLImageDecoderPlugin::icc_data()
{
    return OptionalNone {};
}
}
