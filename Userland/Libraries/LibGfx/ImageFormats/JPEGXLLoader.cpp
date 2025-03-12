/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/ConstrainedStream.h>
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Enumerate.h>
#include <AK/FixedArray.h>
#include <AK/String.h>
#include <LibCompress/Brotli.h>
#include <LibGfx/ImageFormats/ExifOrientedBitmap.h>
#include <LibGfx/ImageFormats/JPEGXLChannel.h>
#include <LibGfx/ImageFormats/JPEGXLCommon.h>
#include <LibGfx/ImageFormats/JPEGXLEntropyDecoder.h>
#include <LibGfx/ImageFormats/JPEGXLLoader.h>

namespace Gfx {

// This is not specified
static ErrorOr<String> read_string(LittleEndianInputBitStream& stream)
{
    auto const name_length = U32(0, TRY(stream.read_bits(4)), 16 + TRY(stream.read_bits(5)), 48 + TRY(stream.read_bits(10)));
    auto string_buffer = TRY(FixedArray<u8>::create(name_length));
    TRY(stream.read_until_filled(string_buffer));
    return String::from_utf8(StringView { string_buffer });
}

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

// From FrameHeader, but used in RestorationFilter
enum class Encoding {
    kVarDCT = 0,
    kModular = 1,
};

/// J.1 - General
struct RestorationFilter {
    bool gab { true };
    bool gab_custom { false };
    f32 gab_x_weight1 { 0.115169525 };
    f32 gab_x_weight2 { 0.061248592 };
    f32 gab_y_weight1 { 0.115169525 };
    f32 gab_y_weight2 { 0.061248592 };
    f32 gab_b_weight1 { 0.115169525 };
    f32 gab_b_weight2 { 0.061248592 };

    u8 epf_iters { 2 };

    bool epf_sharp_custom { false };
    Array<f32, 8> epf_sharp_lut { 0, 1. / 7, 2. / 7, 3. / 7, 4. / 7, 5. / 7, 6. / 7, 1 };

    bool epf_weight_custom { false };
    Array<f32, 3> epf_channel_scale { 40.0, 5.0, 3.5 };

    bool epf_sigma_custom { false };
    f32 epf_quant_mul { 0.46 };
    f32 epf_pass0_sigma_scale { 0.9 };
    f32 epf_pass2_sigma_scale { 6.5 };
    f32 epf_border_sad_mul { 2. / 3 };
    f32 epf_sigma_for_modular { 1.0 };

    Extensions extensions;
};

static ErrorOr<RestorationFilter> read_restoration_filter(LittleEndianInputBitStream& stream, Encoding encoding)
{
    RestorationFilter restoration_filter;

    auto const all_defaults = TRY(stream.read_bit());

    if (!all_defaults) {
        restoration_filter.gab = TRY(stream.read_bit());

        if (restoration_filter.gab) {
            restoration_filter.gab_custom = TRY(stream.read_bit());
            if (restoration_filter.gab_custom) {
                return Error::from_string_literal("JPEGXLLoader: Implement custom restoration filters");
            }
        }

        restoration_filter.epf_iters = TRY(stream.read_bits(2));
        if (restoration_filter.epf_iters != 0) {
            if (encoding == Encoding::kVarDCT) {
                restoration_filter.epf_sharp_custom = TRY(stream.read_bit());
                if (restoration_filter.epf_sharp_custom)
                    return Error::from_string_literal("JPEGXLLoader: Implement custom restoration filters");
            }
            restoration_filter.epf_weight_custom = TRY(stream.read_bit());
            if (restoration_filter.epf_sharp_custom)
                return Error::from_string_literal("JPEGXLLoader: Implement custom restoration filters");

            restoration_filter.epf_sigma_custom = TRY(stream.read_bit());
            if (restoration_filter.epf_sharp_custom)
                return Error::from_string_literal("JPEGXLLoader: Implement custom restoration filters");

            if (encoding == Encoding::kModular)
                return Error::from_string_literal("JPEGXLLoader: Implement custom restoration filters");
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
    u16 group_dim() const { return 128 << group_size_shift; }
    u8 x_qm_scale { 3 };
    u8 b_qm_scale { 2 };
    Passes passes {};

    u8 lf_level {};
    bool have_crop { false };
    i32 x0 {};
    i32 y0 {};
    u32 width {};
    u32 height {};

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

static ErrorOr<FrameHeader> read_frame_header(LittleEndianInputBitStream& stream,
    SizeHeader size_header,
    ImageMetadata const& metadata)
{
    FrameHeader frame_header;
    bool const all_default = TRY(stream.read_bit());

    if (!all_default) {
        frame_header.frame_type = static_cast<FrameHeader::FrameType>(TRY(stream.read_bits(2)));
        frame_header.encoding = static_cast<Encoding>(TRY(stream.read_bits(1)));

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

        if (frame_header.encoding == Encoding::kModular)
            frame_header.group_size_shift = TRY(stream.read_bits(2));

        // Set x_qm_scale default value
        frame_header.x_qm_scale = metadata.xyb_encoded && frame_header.encoding == Encoding::kVarDCT ? 3 : 2;

        if (metadata.xyb_encoded && frame_header.encoding == Encoding::kVarDCT) {
            frame_header.x_qm_scale = TRY(stream.read_bits(3));
            frame_header.b_qm_scale = TRY(stream.read_bits(3));
        }

        if (frame_header.frame_type != FrameHeader::FrameType::kReferenceOnly)
            frame_header.passes = TRY(read_passes(stream));

        if (frame_header.frame_type == FrameHeader::FrameType::kLFFrame)
            TODO();

        if (frame_header.frame_type != FrameHeader::FrameType::kLFFrame)
            frame_header.have_crop = TRY(stream.read_bit());

        if (frame_header.have_crop) {
            auto const read_crop_dimension = [&]() -> ErrorOr<u32> {
                return U32(TRY(stream.read_bits(8)), 256 + TRY(stream.read_bits(11)), 2304 + TRY(stream.read_bits(14)), 18688 + TRY(stream.read_bits(30)));
            };

            if (frame_header.frame_type != FrameHeader::FrameType::kReferenceOnly) {
                frame_header.x0 = unpack_signed(TRY(read_crop_dimension()));
                frame_header.y0 = unpack_signed(TRY(read_crop_dimension()));
            }

            frame_header.width = TRY(read_crop_dimension());
            frame_header.height = TRY(read_crop_dimension());
        }

        bool const normal_frame = frame_header.frame_type == FrameHeader::FrameType::kRegularFrame
            || frame_header.frame_type == FrameHeader::FrameType::kSkipProgressive;

        // Let full_frame be true if and only if have_crop is false or if the frame area given
        // by width and height and offsets x0 and y0 completely covers the image area.
        bool const cover_image_area = frame_header.x0 <= 0 && frame_header.y0 <= 0
            && (frame_header.width + frame_header.x0 >= size_header.width)
            && (frame_header.height + frame_header.y0 == size_header.height);
        bool const full_frame = !frame_header.have_crop || cover_image_area;

        // Set default value for is_last
        frame_header.is_last = frame_header.frame_type == FrameHeader::FrameType::kRegularFrame;

        if (normal_frame) {
            frame_header.blending_info = TRY(read_blending_info(stream, metadata, full_frame));

            frame_header.ec_blending_info = TRY(FixedArray<BlendingInfo>::create(metadata.num_extra_channels));
            for (u16 i {}; i < metadata.num_extra_channels; ++i)
                frame_header.ec_blending_info[i] = TRY(read_blending_info(stream, metadata, full_frame));

            if (metadata.animation.has_value())
                TODO();

            frame_header.is_last = TRY(stream.read_bit());
        }

        if (frame_header.frame_type != FrameHeader::FrameType::kLFFrame && !frame_header.is_last)
            frame_header.save_as_reference = TRY(stream.read_bits(2));

        auto const resets_canvas = full_frame && frame_header.blending_info.mode == BlendingInfo::BlendMode::kReplace;
        auto const can_reference = !frame_header.is_last && (frame_header.duration == 0 || frame_header.save_as_reference != 0) && frame_header.frame_type != FrameHeader::FrameType::kLFFrame;

        frame_header.save_before_ct = !normal_frame;
        if (frame_header.frame_type == FrameHeader::FrameType::kReferenceOnly || (resets_canvas && can_reference))
            frame_header.save_before_ct = TRY(stream.read_bit());

        frame_header.name = TRY(read_string(stream));

        frame_header.restoration_filter = TRY(read_restoration_filter(stream, frame_header.encoding));

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

        // The decoder then computes an array group_offsets, which has 0 as its first element
        // and subsequent group_offsets[i] are the sum of all TOC entries [0, i).
        toc.group_offsets[i] = i == 0 ? 0 : toc.group_offsets[i - 1] + toc.entries[i - 1];
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
        lf_channel_dequantization.m_x_lf_unscaled = TRY(F16(stream));
        lf_channel_dequantization.m_y_lf_unscaled = TRY(F16(stream));
        lf_channel_dequantization.m_b_lf_unscaled = TRY(F16(stream));
    }

    return lf_channel_dequantization;
}
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
        VERIFY(!decoder.has_value());
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
        decoder = TRY(EntropyDecoder::create(stream, num_pre_clustered_distributions));

        return tree;
    }

    LeafNode get_leaf(Span<i32> properties) const
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
        self_correcting_predictor.wp_p1 = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p2 = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3a = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3b = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3c = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3d = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3e = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_w = {
            TRY(stream.read_bits<u8>(4)),
            TRY(stream.read_bits<u8>(4)),
            TRY(stream.read_bits<u8>(4)),
            TRY(stream.read_bits<u8>(4)),
        };
    }

    return self_correcting_predictor;
}
///

/// H.6 - Transformations
struct SqueezeParams {
    bool horizontal {};
    bool in_place {};
    u32 begin_c {};
    u32 num_c {};
};

static ErrorOr<SqueezeParams> read_squeeze_params(LittleEndianInputBitStream& stream)
{
    SqueezeParams squeeze_params;

    squeeze_params.horizontal = TRY(stream.read_bit());
    squeeze_params.in_place = TRY(stream.read_bit());
    squeeze_params.begin_c = U32(TRY(stream.read_bits(3)), 8 + TRY(stream.read_bits(6)), 72 + TRY(stream.read_bits(10)), 1096 + TRY(stream.read_bits(13)));
    squeeze_params.num_c = U32(1, 2, 3, 4 + TRY(stream.read_bits(4)));

    return squeeze_params;
}

struct TransformInfo {
    enum class TransformId {
        kRCT = 0,
        kPalette = 1,
        kSqueeze = 2,
    };

    TransformId tr {};
    u32 begin_c {};
    u32 rct_type {};

    u32 num_c {};
    u32 nb_colours {};
    u32 nb_deltas {};
    u8 d_pred {};

    FixedArray<SqueezeParams> sp {};
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

    if (transform_info.tr == TransformInfo::TransformId::kPalette) {
        transform_info.num_c = U32(1, 3, 4, 1 + TRY(stream.read_bits(13)));
        transform_info.nb_colours = U32(TRY(stream.read_bits(8)), 256 + TRY(stream.read_bits(10)), 1280 + TRY(stream.read_bits(12)), 5376 + TRY(stream.read_bits(16)));
        transform_info.nb_deltas = U32(0, 1 + TRY(stream.read_bits(8)), 257 + TRY(stream.read_bits(10)), 1281 + TRY(stream.read_bits(16)));
        transform_info.d_pred = TRY(stream.read_bits(4));
    }

    if (transform_info.tr == TransformInfo::TransformId::kSqueeze) {
        auto const num_sq = U32(0, 1 + TRY(stream.read_bits(4)), 9 + TRY(stream.read_bits(6)), 41 + TRY(stream.read_bits(8)));
        transform_info.sp = TRY(FixedArray<SqueezeParams>::create(num_sq));
        for (u32 i = 0; i < num_sq; ++i)
            transform_info.sp[i] = TRY(read_squeeze_params(stream));
    }

    return transform_info;
}
///

/// Local abstractions to store the decoded image
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

    void blend_into(Image& image, FrameHeader const& frame_header) const
    {
        // FIXME: We should use ec_blending_info when appropriate

        if (frame_header.blending_info.mode != BlendingInfo::BlendMode::kReplace)
            TODO();

        for (u16 i = 0; i < m_channels.size(); ++i) {
            auto const& input_channel = m_channels[i];
            auto& output_channel = image.channels()[i];

            for (u32 y = 0; y < input_channel.height(); ++y) {
                auto const corrected_y = static_cast<i64>(y) + frame_header.y0;
                if (corrected_y < 0)
                    continue;
                if (corrected_y >= output_channel.height())
                    break;

                for (u32 x = 0; x < input_channel.width(); ++x) {
                    auto const corrected_x = static_cast<i64>(x) + frame_header.x0;
                    if (corrected_x < 0)
                        continue;
                    if (corrected_x >= output_channel.width())
                        break;

                    output_channel.set(corrected_x, corrected_y, input_channel.get(x, y));
                }
            }
        };
    }

    ErrorOr<NonnullRefPtr<Bitmap>> to_bitmap(ImageMetadata const& metadata) const
    {
        // FIXME: which channel size should we use?
        auto const width = m_channels[0].width();
        auto const height = m_channels[0].height();

        auto const orientation = static_cast<TIFF::Orientation>(metadata.orientation);
        auto oriented_bitmap = TRY(ExifOrientedBitmap::create(orientation, { width, height }, BitmapFormat::BGRA8888));

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
                oriented_bitmap.set_pixel(x, y, color.value());
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
struct ModularData {
    bool use_global_tree {};
    WPHeader wp_params {};
    Vector<TransformInfo> transform {};

    // Initially, nb_meta_channels is set to zero, but transformations can modify this value.
    u32 nb_meta_channels {};

    Vector<Channel> channels {};

    ErrorOr<void> create_channels(Span<IntSize> frame_size)
    {
        Vector<IntSize> channel_infos {};
        TRY(channel_infos.try_extend(frame_size));

        for (auto const& tr : transform) {
            if (tr.tr == TransformInfo::TransformId::kPalette) {
                // Let end_c = begin_c + num_c − 1. When updating the channel list as described in H.2, channels begin_c to end_c,
                // which all have the same dimensions, are replaced with two new channels:
                //  - one meta-channel, inserted at the beginning of the channel list and has dimensions width = nb_colours and height = num_c and hshift = vshift = −1.
                //    This channel represents the colours or deltas of the palette.
                //  - one channel (at the same position in the channel list as the original channels, same dimensions) which contains palette indices.

                auto original_dimensions = channel_infos[tr.begin_c];
                channel_infos.remove(tr.begin_c, tr.num_c);
                TRY(channel_infos.try_insert(tr.begin_c, original_dimensions));
                TRY(channel_infos.try_prepend({ tr.nb_colours, tr.num_c }));

                if (tr.begin_c < nb_meta_channels)
                    nb_meta_channels += 2 - tr.begin_c;
                else
                    nb_meta_channels += 1;
            }
        }

        TRY(channels.try_resize(channel_infos.size()));
        for (u32 i = 0; i < channels.size(); ++i)
            channels[i] = TRY(Channel::create(channel_infos[i].width(), channel_infos[i].height()));

        return {};
    }
};

static constexpr u32 nb_base_predictors = 16;

static void get_properties(FixedArray<i32>& properties, Span<Channel> channels, u16 i, u32 x, u32 y, i32 max_error)
{
    // Table H.4 - Property definitions
    properties[0] = i;
    // FIXME: Handle other cases than GlobalModular
    properties[1] = 0;
    properties[2] = y;
    properties[3] = x;

    i32 const W = x > 0 ? channels[i].get(x - 1, y) : (y > 0 ? channels[i].get(x, y - 1) : 0);
    i32 const N = y > 0 ? channels[i].get(x, y - 1) : W;
    i32 const NW = x > 0 && y > 0 ? channels[i].get(x - 1, y - 1) : W;
    i32 const NE = x + 1 < channels[i].width() && y > 0 ? channels[i].get(x + 1, y - 1) : N;
    i32 const NN = y > 1 ? channels[i].get(x, y - 2) : N;
    i32 const WW = x > 1 ? channels[i].get(x - 2, y) : W;

    properties[4] = abs(N);
    properties[5] = abs(W);
    properties[6] = N;
    properties[7] = W;

    // x > 0 ? W - /* (the value of property 9 at position (x - 1, y)) */ : W
    if (x > 0) {
        auto const x_1 = x - 1;
        i32 const W_x_1 = x_1 > 0 ? channels[i].get(x_1 - 1, y) : (y > 0 ? channels[i].get(x_1, y - 1) : 0);
        i32 const N_x_1 = y > 0 ? channels[i].get(x_1, y - 1) : W_x_1;
        i32 const NW_x_1 = x_1 > 0 && y > 0 ? channels[i].get(x_1 - 1, y - 1) : W_x_1;
        properties[8] = W - (W_x_1 + N_x_1 - NW_x_1);
    } else {
        properties[8] = W;
    }

    properties[9] = W + N - NW;
    properties[10] = W - NW;
    properties[11] = NW - N;
    properties[12] = N - NE;
    properties[13] = N - NN;
    properties[14] = W - WW;

    properties[15] = max_error;

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
        properties[nb_base_predictors + (i - 1 - j) * 4 + 0] = abs(rC);
        properties[nb_base_predictors + (i - 1 - j) * 4 + 1] = rC;
        properties[nb_base_predictors + (i - 1 - j) * 4 + 2] = abs(rC - rG);
        properties[nb_base_predictors + (i - 1 - j) * 4 + 3] = rC - rG;
    }
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

static ErrorOr<ModularData> read_modular_bitstream(LittleEndianInputBitStream& stream,
    Span<IntSize> channels_info,
    Optional<EntropyDecoder>& decoder,
    MATree const& global_tree,
    u32 group_dim)
{
    ModularData modular_data;

    modular_data.use_global_tree = TRY(stream.read_bit());
    modular_data.wp_params = TRY(read_self_correcting_predictor(stream));
    auto const nb_transforms = U32(0, 1, 2 + TRY(stream.read_bits(4)), 18 + TRY(stream.read_bits(8)));

    TRY(modular_data.transform.try_resize(nb_transforms));
    for (u32 i {}; i < nb_transforms; ++i)
        modular_data.transform[i] = TRY(read_transform_info(stream));

    TRY(modular_data.create_channels(channels_info));

    auto will_be_decoded = [&](u32 index, Channel const& channel) {
        if (channel.width() == 0 || channel.height() == 0)
            return false;
        if (index < modular_data.nb_meta_channels)
            return true;
        return channel.width() <= group_dim && channel.width() <= group_dim;
    };

    if constexpr (JPEGXL_DEBUG) {
        dbgln("Decoding modular sub-stream ({} tree, {} transforms):",
            modular_data.use_global_tree ? "global"sv : "local"sv,
            nb_transforms);

        for (auto const& tr : modular_data.transform) {
            if (tr.tr == TransformInfo::TransformId::kPalette) {
                dbgln("* Palette: begin_c={} - num_c={} - nb_colours={} - nb_deltas={} - d_pred={}",
                    tr.begin_c, tr.num_c, tr.nb_colours, tr.nb_deltas, tr.d_pred);
            }
        }
        for (auto const& [i, channel] : enumerate(modular_data.channels))
            dbgln("- Channel {}: {}x{}{}", i, channel.width(), channel.height(), will_be_decoded(i, channel) ? ""sv : " - skipped"sv);
    }

    Optional<MATree> local_tree;
    if (!modular_data.use_global_tree)
        TODO();

    // where the dist_multiplier from C.3.3 is set to the largest channel width amongst all channels
    // that are to be decoded.
    auto const dist_multiplier = [&]() {
        u32 dist_multiplier {};
        for (auto [i, channel] : enumerate(modular_data.channels)) {
            if (will_be_decoded(i, channel) && channel.width() > dist_multiplier)
                dist_multiplier = channel.width();
        }
        return dist_multiplier;
    }();
    decoder->set_dist_multiplier(dist_multiplier);

    // The decoder then starts an entropy-coded stream (C.1) and decodes the data for each channel
    // (in ascending order of index) as specified in H.3, skipping any channels having width or height
    // zero. Finally, the inverse transformations are applied (from last to first) as described in H.6.

    auto properties = TRY(FixedArray<i32>::create(nb_base_predictors + modular_data.channels.size() * 4));

    auto const& tree = local_tree.has_value() ? *local_tree : global_tree;
    for (auto [i, channel] : enumerate(modular_data.channels)) {
        if (!will_be_decoded(i, channel))
            continue;

        auto self_correcting_data = TRY(SelfCorrectingData::create(modular_data.wp_params, channel.width()));

        for (u32 y {}; y < channel.height(); y++) {
            for (u32 x {}; x < channel.width(); x++) {
                auto const neighborhood = retrieve_neighborhood(channel, x, y);

                auto const self_prediction = self_correcting_data.compute_predictions(neighborhood, x);

                get_properties(properties, modular_data.channels, i, x, y, self_prediction.max_error);
                auto const leaf_node = tree.get_leaf(properties);
                auto diff = unpack_signed(TRY(decoder->decode_hybrid_uint(stream, leaf_node.ctx)));
                diff = (diff * leaf_node.multiplier) + leaf_node.offset;
                auto const total = diff + prediction(neighborhood, self_prediction.prediction, leaf_node.predictor);

                self_correcting_data.compute_errors(x, total);
                channel.set(x, y, total);
            }

            self_correcting_data.register_next_row();
        }

        channel.set_decoded(true);
    }

    return modular_data;
}
///

/// G.1.2 - LF channel dequantization weights
struct GlobalModular {
    Optional<EntropyDecoder> decoder;
    MATree ma_tree;
    ModularData modular_data;
};

static ErrorOr<GlobalModular> read_global_modular(LittleEndianInputBitStream& stream,
    IntSize frame_size,
    FrameHeader const& frame_header,
    ImageMetadata const& metadata)
{
    GlobalModular global_modular;

    auto const decode_ma_tree = TRY(stream.read_bit());

    if (decode_ma_tree)
        global_modular.ma_tree = TRY(MATree::decode(stream, global_modular.decoder));

    // The decoder then decodes a modular sub-bitstream (Annex H), where
    // the number of channels is computed as follows:

    auto num_channels = metadata.num_extra_channels;
    if (frame_header.encoding == Encoding::kModular) {
        if (!frame_header.do_YCbCr && !metadata.xyb_encoded
            && metadata.colour_encoding.colour_space == ColourEncoding::ColourSpace::kGrey) {
            num_channels += 1;
        } else {
            num_channels += 3;
        }
    }

    // However, the decoder only decodes the first nb_meta_channels channels and any further channels
    // that have a width and height that are both at most group_dim. At that point, it stops decoding.
    // No inverse transforms are applied yet.
    auto channels = TRY(FixedArray<IntSize>::create(num_channels));
    channels.fill_with(frame_size);

    global_modular.modular_data = TRY(read_modular_bitstream(stream, channels, global_modular.decoder, global_modular.ma_tree, frame_header.group_dim()));

    return global_modular;
}
///

/// K.3.1  Patches decoding
struct Patch {
    u32 width {};
    u32 height {};

    u32 ref {};

    u32 x0 {};
    u32 y0 {};

    u32 count {};

    // x[] and y[] in the spec
    FixedArray<IntPoint> positions;
};

static ErrorOr<Patch> read_patch(LittleEndianInputBitStream& stream, EntropyDecoder& decoder, u32 num_extra_channels)
{
    Patch patch;
    patch.ref = TRY(decoder.decode_hybrid_uint(stream, 1));
    patch.x0 = TRY(decoder.decode_hybrid_uint(stream, 3));
    patch.y0 = TRY(decoder.decode_hybrid_uint(stream, 3));
    patch.width = TRY(decoder.decode_hybrid_uint(stream, 2)) + 1;
    patch.height = TRY(decoder.decode_hybrid_uint(stream, 2)) + 1;
    patch.count = TRY(decoder.decode_hybrid_uint(stream, 7)) + 1;

    patch.positions = TRY(FixedArray<IntPoint>::create(patch.count));

    for (u32 j = 0; j < patch.count; j++) {
        if (j == 0) {
            auto position = IntPoint {
                TRY(decoder.decode_hybrid_uint(stream, 4)),
                TRY(decoder.decode_hybrid_uint(stream, 4)),
            };
            patch.positions[j] = position;
        } else {
            auto position = IntPoint {
                unpack_signed(TRY(decoder.decode_hybrid_uint(stream, 6))) + patch.positions[j - 1].x(),
                unpack_signed(TRY(decoder.decode_hybrid_uint(stream, 6))) + patch.positions[j - 1].y(),
            };
            patch.positions[j] = position;
        }

        // FIXME: Bail out if this condition is not respected
        /* the width x height rectangle with top-left coordinates (x, y)
           is fully contained within the frame */

        if (num_extra_channels > 0)
            return Error::from_string_literal("JPEGXLLoader: Implement reading patches for extra channels");
    }

    return patch;
}

static ErrorOr<FixedArray<Patch>> read_patches(LittleEndianInputBitStream& stream, u32 num_extra_channels)
{
    auto decoder = TRY(EntropyDecoder::create(stream, 10));
    u32 const num_patches = TRY(decoder.decode_hybrid_uint(stream, 0));

    auto patches = TRY(FixedArray<Patch>::create(num_patches));
    for (auto& patch : patches)
        patch = TRY(read_patch(stream, decoder, num_extra_channels));

    return patches;
}

///

/// G.1 - LfGlobal
struct LfGlobal {
    FixedArray<Patch> patches;
    LfChannelDequantization lf_dequant;
    GlobalModular gmodular;
};

static ErrorOr<LfGlobal> read_lf_global(LittleEndianInputBitStream& stream,
    IntSize frame_size,
    FrameHeader const& frame_header,
    ImageMetadata const& metadata)
{
    LfGlobal lf_global;

    if (frame_header.flags != FrameHeader::Flags::None) {
        if (frame_header.flags & FrameHeader::Flags::kPatches) {
            lf_global.patches = TRY(read_patches(stream, metadata.num_extra_channels));
        }
        if (frame_header.flags & FrameHeader::Flags::kSplines) {
            return Error::from_string_literal("JPEGXLLoader: Implement Splines");
        }
        if (frame_header.flags & FrameHeader::Flags::kNoise) {
            return Error::from_string_literal("JPEGXLLoader: Implement Noise");
        }
    }

    lf_global.lf_dequant = TRY(read_lf_channel_dequantization(stream));

    if (frame_header.encoding == Encoding::kVarDCT)
        TODO();

    lf_global.gmodular = TRY(read_global_modular(stream, frame_size, frame_header, metadata));

    return lf_global;
}
///

/// G.2 - LfGroup
static ErrorOr<void> read_lf_group(LittleEndianInputBitStream&,
    Image& image,
    FrameHeader const& frame_header)
{
    // LF coefficients
    if (frame_header.encoding == Encoding::kVarDCT) {
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
    if (frame_header.encoding == Encoding::kVarDCT) {
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
    FrameHeader const& frame_header)
{
    if (frame_header.encoding == Encoding::kVarDCT) {
        (void)stream;
        TODO();
    }

    auto& channels = image.channels();
    for (u16 i {}; i < channels.size(); ++i) {
        // Skip meta-channels
        // FIXME: Also test if the channel has already been decoded
        //        See: nb_meta_channels in the spec
        bool const is_meta_channel = channels[i].width() <= frame_header.group_dim()
            || channels[i].height() <= frame_header.group_dim()
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

    Image image {};

    ErrorOr<void> render_image()
    {
        auto& channels = image.channels();
        TRY(channels.try_resize(lf_global.gmodular.modular_data.channels.size()));
        for (u32 i = 0; i < channels.size(); ++i)
            channels[i] = move(lf_global.gmodular.modular_data.channels[i]);
        lf_global.gmodular.modular_data.channels = {};
        return {};
    }
};

class AutoDepletingConstrainedStream : public ConstrainedStream {
public:
    AutoDepletingConstrainedStream(MaybeOwned<Stream> stream, u64 limit)
        : ConstrainedStream(move(stream), limit)
    {
    }

    ~AutoDepletingConstrainedStream()
    {
        dbgln_if(JPEGXL_DEBUG, "Discarding {} remaining bytes", remaining());
        if (discard(remaining()).is_error())
            dbgln("JPEGXLLoader: Corrupted stream, reached EOF");
    }
};

static LittleEndianInputBitStream get_stream_for_section(LittleEndianInputBitStream& stream, u32 section_size)
{
    VERIFY(stream.align_to_byte_boundary() == 0);
    auto constrained_stream = make<AutoDepletingConstrainedStream>(MaybeOwned<Stream>(stream), section_size);
    return LittleEndianInputBitStream(move(constrained_stream));
}

static ErrorOr<Frame> read_frame(LittleEndianInputBitStream& stream,
    SizeHeader const& size_header,
    ImageMetadata const& metadata)
{
    // F.1 - General
    // Each Frame is byte-aligned by invoking ZeroPadToByte() (B.2.7)
    stream.align_to_byte_boundary();

    Frame frame;

    frame.frame_header = TRY(read_frame_header(stream, size_header, metadata));

    if (!frame.frame_header.have_crop) {
        frame.width = size_header.width;
        frame.height = size_header.height;
    } else {
        frame.width = frame.frame_header.width;
        frame.height = frame.frame_header.height;
    }

    if (frame.frame_header.upsampling > 1) {
        frame.width = ceil(static_cast<double>(frame.width) / frame.frame_header.upsampling);
        frame.height = ceil(static_cast<double>(frame.height) / frame.frame_header.upsampling);
    }

    dbgln_if(JPEGXL_DEBUG, "Frame{}: {}x{} {} - type({}) - flags({}){}"sv,
        frame.frame_header.name.is_empty() ? ""sv : MUST(String::formatted(" \"{}\"", frame.frame_header.name)),
        frame.width, frame.height,
        frame.frame_header.encoding,
        to_underlying(frame.frame_header.frame_type),
        to_underlying(frame.frame_header.flags),
        frame.frame_header.is_last ? " - is_last"sv : ""sv);

    if (frame.frame_header.lf_level > 0)
        TODO();

    auto const group_dim = frame.frame_header.group_dim();
    auto const frame_width = static_cast<double>(frame.width);
    auto const frame_height = static_cast<double>(frame.height);
    frame.num_groups = ceil(frame_width / group_dim) * ceil(frame_height / group_dim);
    frame.num_lf_groups = ceil(frame_width / (group_dim * 8)) * ceil(frame_height / (group_dim * 8));

    frame.toc = TRY(read_toc(stream, frame.frame_header, frame.num_groups, frame.num_lf_groups));

    if constexpr (JPEGXL_DEBUG) {
        dbgln("TOC: index |  size | offset");
        for (u32 i {}; i < frame.toc.entries.size(); ++i)
            dbgln("     {:5} | {:5} | {:6}", i, frame.toc.entries[i], frame.toc.group_offsets[i]);
    }

    frame.image = TRY(Image::create({ frame.width, frame.height }, metadata));

    // "If num_groups == 1 and num_passes == 1, then there is a single TOC entry and a single section
    // containing all frame data structures."
    if (frame.num_groups == 1 && frame.frame_header.passes.num_passes == 1) {
        auto section_stream = get_stream_for_section(stream, frame.toc.entries[0]);
        frame.lf_global = TRY(read_lf_global(section_stream, { frame.width, frame.height }, frame.frame_header, metadata));
        TRY(read_lf_group(section_stream, frame.image, frame.frame_header));
        TRY(read_pass_group(section_stream, frame.image, frame.frame_header));
    } else {
        {
            auto lf_stream = get_stream_for_section(stream, frame.toc.entries[0]);
            frame.lf_global = TRY(read_lf_global(lf_stream, { frame.width, frame.height }, frame.frame_header, metadata));
        }

        for (u32 i {}; i < frame.num_lf_groups; ++i) {
            auto lf_stream = get_stream_for_section(stream, frame.toc.entries[1 + i]);
            TRY(read_lf_group(lf_stream, frame.image, frame.frame_header));
        }

        if (frame.frame_header.encoding == Encoding::kVarDCT) {
            TODO();
        }

        for (u64 pass_index {}; pass_index < frame.frame_header.passes.num_passes; ++pass_index) {
            for (u64 group_index {}; group_index < frame.num_groups; ++group_index) {
                auto toc_section_number = 2 + frame.num_lf_groups + pass_index * frame.num_groups + group_index;
                auto pass_stream = get_stream_for_section(stream, frame.toc.entries[toc_section_number]);
                TRY(read_pass_group(pass_stream, frame.image, frame.frame_header));
            }
        }
    }

    TRY(frame.render_image());

    // G.4.2 - Modular group data
    // When all modular groups are decoded, the inverse transforms are applied to
    // the at that point fully decoded GlobalModular image, as specified in H.6.
    auto const& transform_infos = frame.lf_global.gmodular.modular_data.transform;
    for (auto const& transformation : transform_infos.in_reverse())
        apply_transformation(frame.image, transformation);

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
static ErrorOr<void> apply_upsampling(Frame& frame, ImageMetadata const& metadata)
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
        for (auto& channel : frame.image.channels()) {
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

static ErrorOr<void> apply_image_features(Frame& frame, ImageMetadata const& metadata)
{
    TRY(apply_upsampling(frame, metadata));

    if (frame.frame_header.flags != FrameHeader::Flags::None)
        TODO();
    return {};
}
///

/// L.2 - XYB + L.3 - YCbCr
static void ycbcr_to_rgb(Image& image, u8 bits_per_sample)
{
    auto& channels = image.channels();
    VERIFY(channels.size() >= 3);

    VERIFY(channels[0].width() == channels[1].width() && channels[1].width() == channels[2].width());
    VERIFY(channels[0].height() == channels[1].height() && channels[1].height() == channels[2].height());

    auto const half_range_offset = (1 << bits_per_sample) / 2;
    for (u32 y = 0; y < channels[0].height(); ++y) {
        for (u32 x = 0; x < channels[0].width(); ++x) {
            auto const cb = channels[0].get(x, y);
            auto const luma = channels[1].get(x, y);
            auto const cr = channels[2].get(x, y);

            channels[0].set(x, y, luma + half_range_offset + 1.402 * cr);
            channels[1].set(x, y, luma + half_range_offset - 0.344136 * cb - 0.714136 * cr);
            channels[2].set(x, y, luma + half_range_offset + 1.772 * cb);
        }
    }
}

static void apply_colour_transformation(Frame& frame, ImageMetadata const& metadata)
{
    if (frame.frame_header.do_YCbCr)
        ycbcr_to_rgb(frame.image, metadata.bit_depth.bits_per_sample);

    if (metadata.xyb_encoded) {
        TODO();
    } else {
        // FIXME: Do a proper color transformation with metadata.colour_encoding
    }
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

        dbgln_if(JPEGXL_DEBUG, "Decoding a JPEG XL image with size {}x{} and {} channels.", m_header.width, m_header.height, m_metadata.number_of_channels());

        m_state = State::HeaderDecoded;

        return {};
    }

    ErrorOr<void> decode_icc()
    {
        if (m_metadata.colour_encoding.want_icc && m_icc_profile.size() == 0)
            m_icc_profile = TRY(read_icc(m_stream));
        m_state = State::ICCProfileDecoded;
        return {};
    }

    ErrorOr<void> decode_frame()
    {
        auto frame = TRY(read_frame(m_stream, m_header, m_metadata));
        auto const& frame_header = frame.frame_header;

        if (frame_header.restoration_filter.gab || frame_header.restoration_filter.epf_iters != 0)
            TODO();

        TRY(apply_image_features(frame, m_metadata));

        if (!frame_header.save_before_ct) {
            apply_colour_transformation(frame, m_metadata);
        }

        TRY(render_extra_channels(frame.image, m_metadata));

        m_frames.append(move(frame));

        return {};
    }

    ErrorOr<void> decode()
    {
        auto result = [this]() -> ErrorOr<void> {
            // A.1 - Codestream structure

            // The header is already decoded in JPEGXLImageDecoderPlugin::create()

            TRY(decode_icc());

            if (m_metadata.preview.has_value())
                TODO();

            TRY(decode_frame());

            while (!m_frames.last().frame_header.is_last)
                TRY(decode_frame());

            if (!m_image.has_value())
                m_image = TRY(Image::create({ m_header.width, m_header.height }, m_metadata));

            m_frames.last().image.blend_into(*m_image, m_frames.last().frame_header);

            m_bitmap = TRY(m_image->to_bitmap(m_metadata));
            m_image.clear();

            return {};
        }();

        m_state = result.is_error() ? State::Error : State::FrameDecoded;

        return result;
    }

    enum class State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        ICCProfileDecoded,
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

    ByteBuffer const& icc_profile() const
    {
        return m_icc_profile;
    }

private:
    State m_state { State::NotDecoded };

    LittleEndianInputBitStream m_stream;
    RefPtr<Gfx::Bitmap> m_bitmap;

    // JPEG XL images can be composed of multiples sub-images, this variable is an internal
    // representation of this blending before the final rendering (in m_bitmap)
    Optional<Image> m_image;

    Vector<Frame> m_frames;

    SizeHeader m_header;
    ImageMetadata m_metadata;

    ByteBuffer m_icc_profile;
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
    if (m_context->state() < JPEGXLLoadingContext::State::ICCProfileDecoded)
        TRY(m_context->decode_icc());
    if (m_context->icc_profile().size() == 0)
        return OptionalNone {};
    return m_context->icc_profile();
}

}

namespace AK {

template<>
struct Formatter<Gfx::Encoding> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::Encoding const& header)
    {
        auto string = "Unknown"sv;
        switch (header) {
        case Gfx::Encoding::kVarDCT:
            string = "VarDCT"sv;
            break;
        case Gfx::Encoding::kModular:
            string = "Modular"sv;
            break;
        default:
            break;
        }

        return Formatter<StringView>::format(builder, string);
    }
};

}
