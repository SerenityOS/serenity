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
#include <LibGfx/ImageFormats/ISOBMFF/JPEGXLBoxes.h>
#include <LibGfx/ImageFormats/ISOBMFF/Reader.h>
#include <LibGfx/ImageFormats/JPEGXL/Channel.h>
#include <LibGfx/ImageFormats/JPEGXL/Common.h>
#include <LibGfx/ImageFormats/JPEGXL/DCTNaturalOrder.h>
#include <LibGfx/ImageFormats/JPEGXL/EntropyDecoder.h>
#include <LibGfx/ImageFormats/JPEGXL/ModularTransforms.h>
#include <LibGfx/ImageFormats/JPEGXL/SelfCorrectingPredictor.h>
#include <LibGfx/ImageFormats/JPEGXLLoader.h>
#include <LibGfx/Matrix3x3.h>

namespace Gfx::JPEGXL {

// This is not specified
static ErrorOr<void> read_non_aligned(LittleEndianInputBitStream& stream, Bytes bytes)
{
    for (u8& byte : bytes)
        byte = TRY(stream.read_bits(8));
    return {};
}

static ErrorOr<String> read_string(LittleEndianInputBitStream& stream)
{
    auto const name_length = U32(0, TRY(stream.read_bits(4)), 16 + TRY(stream.read_bits(5)), 48 + TRY(stream.read_bits(10)));
    auto string_buffer = TRY(FixedArray<u8>::create(name_length));
    TRY(read_non_aligned(stream, string_buffer));
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

    if (extra_channel_info.type == ExtraChannelInfo::ExtraChannelType::kSpotColour) {
        return Error::from_string_literal("JPEGXLLoader: Read extra channel info for SpotColour");
    }

    if (extra_channel_info.type == ExtraChannelInfo::ExtraChannelType::kCFA) {
        return Error::from_string_literal("JPEGXLLoader: Read extra channel info for CFA");
    }

    return extra_channel_info;
}

struct ToneMapping {
    f32 intensity_target { 255 };
    f32 min_nits { 0 };
    bool relative_to_max_display { false };
    f32 linear_below { 0 };
};

static ErrorOr<ToneMapping> read_tone_mapping(LittleEndianInputBitStream& stream)
{
    ToneMapping tone_mapping;
    bool const all_default = TRY(stream.read_bit());

    if (!all_default) {
        tone_mapping.intensity_target = TRY(F16(stream));
        tone_mapping.min_nits = TRY(F16(stream));
        tone_mapping.relative_to_max_display = TRY(stream.read_bit());
        tone_mapping.linear_below = TRY(F16(stream));
    }

    return tone_mapping;
}

// L.2.1 - OpsinInverseMatrix
struct OpsinInverseMatrix {
    f32 inv_mat00 = 11.031566901960783;
    f32 inv_mat01 = -9.866943921568629;
    f32 inv_mat02 = -0.16462299647058826;
    f32 inv_mat10 = -3.254147380392157;
    f32 inv_mat11 = 4.418770392156863;
    f32 inv_mat12 = -0.16462299647058826;
    f32 inv_mat20 = -3.6588512862745097;
    f32 inv_mat21 = 2.7129230470588235;
    f32 inv_mat22 = 1.9459282392156863;
    f32 opsin_bias0 = -0.0037930732552754493;
    f32 opsin_bias1 = -0.0037930732552754493;
    f32 opsin_bias2 = -0.0037930732552754493;
    f32 quant_bias0 = 1 - 0.05465007330715401;
    f32 quant_bias1 = 1 - 0.07005449891748593;
    f32 quant_bias2 = 1 - 0.049935103337343655;
    f32 quant_bias_numerator = 0.145;
};

static ErrorOr<OpsinInverseMatrix> read_opsin_inverse_matrix(LittleEndianInputBitStream& stream)
{
    OpsinInverseMatrix matrix;

    bool all_default = TRY(stream.read_bit());

    if (!all_default) {
        matrix.inv_mat00 = TRY(F16(stream));
        matrix.inv_mat01 = TRY(F16(stream));
        matrix.inv_mat02 = TRY(F16(stream));
        matrix.inv_mat10 = TRY(F16(stream));
        matrix.inv_mat11 = TRY(F16(stream));
        matrix.inv_mat12 = TRY(F16(stream));
        matrix.inv_mat20 = TRY(F16(stream));
        matrix.inv_mat21 = TRY(F16(stream));
        matrix.inv_mat22 = TRY(F16(stream));
        matrix.opsin_bias0 = TRY(F16(stream));
        matrix.opsin_bias1 = TRY(F16(stream));
        matrix.opsin_bias2 = TRY(F16(stream));
        matrix.quant_bias0 = TRY(F16(stream));
        matrix.quant_bias1 = TRY(F16(stream));
        matrix.quant_bias2 = TRY(F16(stream));
        matrix.quant_bias_numerator = TRY(F16(stream));
    }

    return matrix;
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

    Optional<u16> black_channel() const
    {
        return first_extra_channel_matching([](auto& info) { return info.type == ExtraChannelInfo::ExtraChannelType::kBlack; });
    }

    Optional<u16> alpha_channel() const
    {
        return first_extra_channel_matching([](auto& info) { return info.type == ExtraChannelInfo::ExtraChannelType::kAlpha; });
    }

private:
    Optional<u16> first_extra_channel_matching(auto&& condition) const
    {
        for (u16 i = 0; i < ec_info.size(); ++i) {
            if (condition(ec_info[i]))
                return i + number_of_color_channels();
        }
        return OptionalNone {};
    }
};

static ErrorOr<void> ensure_metadata_correctness(ImageMetadata const& metadata)
{
    // "This includes CMYK colour spaces; in that case, the RGB components are interpreted as
    // CMY where 0 means full ink, want_icc is true (see Table E.1), and there is an extra channel
    // of type kBlack (see Table D.9)."
    bool should_be_cmyk = any_of(metadata.ec_info, [](auto& info) { return info.type == ExtraChannelInfo::ExtraChannelType::kBlack; });
    if (should_be_cmyk && !metadata.colour_encoding.want_icc)
        return Error::from_string_literal("JPEGXLLoader: Seemingly CMYK image doesn't have an ICC profile");

    return {};
}

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

    TRY(ensure_metadata_correctness(metadata));

    return metadata;
}
///

/// Table F.7 — BlendingInfo bundle
struct BlendingInfo {
    enum class SimpleBlendMode : u8 {
        kReplace = 0,
        kAdd = 1,
        kBlend = 2,
        kMulAdd = 3,
        kMul = 4,
    };

    // This is a superset of `BlendingInfo::SimpleBlendMode` and defined in `Table K.1 — PatchBlendMode.
    // It is only used for patches, but having it here allows us to share some code.
    enum class BlendMode : u8 {
        kNone = 0,
        kReplace = 1,
        kAdd = 2,
        kMul = 3,
        kBlendAbove = 4,
        kBlendBelow = 5,
        kMulAddAbove = 6,
        kMulAddBelow = 7,
    };

    static BlendMode to_general_blend_mode(SimpleBlendMode simple)
    {
        switch (simple) {
        case SimpleBlendMode::kReplace:
            return BlendMode::kReplace;
        case SimpleBlendMode::kAdd:
            return BlendMode::kAdd;
        case SimpleBlendMode::kBlend:
            return BlendMode::kBlendAbove;
        case SimpleBlendMode::kMulAdd:
            return BlendMode::kMulAddAbove;
        case SimpleBlendMode::kMul:
            return BlendMode::kMul;
        }
        VERIFY_NOT_REACHED();
    }

    BlendMode mode {};
    u8 alpha_channel {};
    bool clamp { false };
    u8 source {};
};

static ErrorOr<BlendingInfo> read_blending_info(LittleEndianInputBitStream& stream, ImageMetadata const& metadata, bool full_frame)
{
    BlendingInfo blending_info;

    auto simple = static_cast<BlendingInfo::SimpleBlendMode>(U32(0, 1, 2, 3 + TRY(stream.read_bits(2))));
    blending_info.mode = BlendingInfo::to_general_blend_mode(simple);

    bool const extra = metadata.num_extra_channels > 0;

    if (extra) {
        auto const blend_or_mul_add = blending_info.mode == BlendingInfo::BlendMode::kBlendAbove
            || blending_info.mode == BlendingInfo::BlendMode::kMulAddAbove;

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
                restoration_filter.gab_x_weight1 = TRY(F16(stream));
                restoration_filter.gab_x_weight2 = TRY(F16(stream));
                restoration_filter.gab_y_weight1 = TRY(F16(stream));
                restoration_filter.gab_y_weight2 = TRY(F16(stream));
                restoration_filter.gab_b_weight1 = TRY(F16(stream));
                restoration_filter.gab_b_weight2 = TRY(F16(stream));
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
                restoration_filter.epf_sigma_for_modular = TRY(F16(stream));
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
            frame_header.lf_level = 1 + TRY(stream.read_bits(2));

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

// F.3.2 - Decoding permutations
static ErrorOr<Vector<u32>> decode_permutations(LittleEndianInputBitStream& stream, EntropyDecoder& decoder, u32 size, u32 skip)
{
    // "Let GetContext(x) denote min(7, ceil(log2(x + 1)))."
    auto get_context = [](u32 x) -> u32 {
        return min(7, ceil(log2(x + 1)));
    };

    // "The decoder first decodes an integer end, as specified in C.3.3,
    // using DecodeHybridVarLenUint(GetContext(size))."
    auto end = TRY(decoder.decode_hybrid_uint(stream, get_context(size)));

    // "The value end is at most size − skip."
    if (end > size - skip)
        return Error::from_string_literal("JPEGXLLoader: Invalid value for end when decoding permutations");

    // "Then a sequence lehmer of size elements is produced as follows. It is zero-initialized."
    auto lehmer = TRY(FixedArray<u32>::create(size));

    // "For each index i in range [skip, skip + end), the value lehmer[i] is set to
    // DecodeHybridVarLenUint(GetContext(i > skip ? lehmer[i − 1] : 0));"
    for (u32 i = skip; i < skip + end; ++i) {
        lehmer[i] = TRY(decoder.decode_hybrid_uint(stream, get_context(i > skip ? lehmer[i - 1] : 0)));
        // "this value is strictly less than size − i."
        if (lehmer[i] >= size - i)
            return Error::from_string_literal("JPEGXLLoader: Decoded permutation is invalid");
    }

    // "The decoder then maintains a sequence of elements temp, initially containing
    // the numbers [0, size) in increasing order,"
    Vector<u32> temp;
    TRY(temp.try_ensure_capacity(size));
    for (u32 i = 0; i < size; ++i)
        temp.append(i);

    // "and a sequence of elements permutation, initially empty."
    Vector<u32> permutation;
    TRY(permutation.try_ensure_capacity(size));

    // "Then, for each integer i in the range [0, size), the decoder appends to
    // permutation element temp[lehmer[i]], then removes it from temp, leaving the
    // relative order of other elements unchanged."
    for (u32 i = 0; i < size; ++i) {
        permutation.append(temp[lehmer[i]]);
        temp.remove(lehmer[i]);
    }

    // " Finally, permutation is the decoded permutation."
    return permutation;
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
    f32 m_x_lf_unscaled { 1. / (32 * 128) };
    f32 m_y_lf_unscaled { 1. / (4 * 128) };
    f32 m_b_lf_unscaled { 1. / (2 * 128) };
};

static ErrorOr<LfChannelDequantization> read_lf_channel_dequantization(LittleEndianInputBitStream& stream)
{
    LfChannelDequantization lf_channel_dequantization;

    auto const all_default = TRY(stream.read_bit());

    if (!all_default) {
        lf_channel_dequantization.m_x_lf_unscaled = TRY(F16(stream)) / 128;
        lf_channel_dequantization.m_y_lf_unscaled = TRY(F16(stream)) / 128;
        lf_channel_dequantization.m_b_lf_unscaled = TRY(F16(stream)) / 128;
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
        TRY(decoder->ensure_end_state());

        // Finally, the decoder reads (tree.size() + 1) / 2 pre-clustered distributions D as specified in C.1.

        auto const num_pre_clustered_distributions = (tree.m_tree.size() + 1) / 2;
        decoder = TRY(EntropyDecoder::create(stream, num_pre_clustered_distributions));

        tree.save_self_correction_usage();

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

    bool use_self_correcting_predictor() const
    {
        return m_use_self_correcting_predictor;
    }

private:
    void save_self_correction_usage()
    {
        for (auto const& node : m_tree) {
            // We are looking for usage of the Self Correction predictor, so this includes both the
            // 'max_error' property and the 'Self-correcting' predictor, They are given as index 15
            // in Table H.4 — Property definitions and index 6 in Table H.3 — Modular predictors respectively.
            auto const use_max_error = node.has<DecisionNode>() && node.get<DecisionNode>().property == 15;
            auto const use_self_correcting = node.has<LeafNode>() && node.get<LeafNode>().predictor == 6;
            if (use_max_error || use_self_correcting) {
                m_use_self_correcting_predictor = true;
                return;
            }
        }

        m_use_self_correcting_predictor = false;
    }

    struct DecisionNode {
        u64 property {};
        i64 value {};
        u64 left_child {};
        u64 right_child {};
    };

    Vector<Variant<DecisionNode, LeafNode>> m_tree;

    bool m_use_self_correcting_predictor { true };
};
///

/// Local abstractions to store the decoded image
class BlendedImage {
public:
    ErrorOr<void> blend_into(BlendedImage& image, BlendingInfo::BlendMode mode) const
    {
        if (to_underlying(mode) > 2)
            return Error::from_string_literal("JPEGXLLoder: Unsupported blend mode");

        auto input_rect = active_rectangle();
        auto output_rect = image.active_rectangle();

        if (input_rect.size() != output_rect.size())
            return Error::from_string_literal("JPEGXLLoder: Unable to blend image with a different size");

        for (u32 i = 0; i < channels().size(); ++i) {
            auto const& input_channel = channels()[i];
            auto& output_channel = image.channels()[i];

            if (mode == BlendingInfo::BlendMode::kNone)
                blend_channel<BlendingInfo::BlendMode::kNone>(input_channel, input_rect, output_channel, output_rect);
            else if (mode == BlendingInfo::BlendMode::kReplace)
                blend_channel<BlendingInfo::BlendMode::kReplace>(input_channel, input_rect, output_channel, output_rect);
            else if (mode == BlendingInfo::BlendMode::kAdd)
                blend_channel<BlendingInfo::BlendMode::kAdd>(input_channel, input_rect, output_channel, output_rect);
        }

        return {};
    }

protected:
    virtual ~BlendedImage() = default;

    virtual Vector<Channel>& channels() = 0;
    virtual Vector<Channel> const& channels() const = 0;
    virtual IntRect active_rectangle() const = 0;
    IntSize size() const { return active_rectangle().size(); }

private:
    template<BlendingInfo::BlendMode blend_mode>
    void blend_channel(Channel const& input_channel, IntRect input_rect,
        Channel& output_channel, IntRect output_rect) const
    {
        for (u32 y = 0; y < static_cast<u32>(input_rect.height()); ++y) {
            for (u32 x = 0; x < static_cast<u32>(input_rect.width()); ++x) {
                auto const old_sample = output_channel.get(x + output_rect.x(), y + output_rect.y());
                auto const new_sample = input_channel.get(x + input_rect.x(), y + input_rect.y());

                auto const sample = [&]() {
                    // Table F.8 — BlendMode (BlendingInfo.mode)
                    if constexpr (blend_mode == BlendingInfo::BlendMode::kNone)
                        return old_sample;
                    if constexpr (blend_mode == BlendingInfo::BlendMode::kReplace)
                        return new_sample;
                    if constexpr (blend_mode == BlendingInfo::BlendMode::kAdd)
                        return old_sample + new_sample;
                }();
                output_channel.set(x + output_rect.x(), y + output_rect.y(), sample);
            }
        }
    }
};

class ImageView : public BlendedImage {
public:
    ImageView(Vector<Channel>& channels, IntRect active_rect)
        : m_channels_view(channels)
        , m_active_rect(active_rect)
    {
    }

private:
    virtual Vector<Channel> const& channels() const override
    {
        return m_channels_view;
    }

    virtual Vector<Channel>& channels() override
    {
        return m_channels_view;
    }

    virtual IntRect active_rectangle() const override
    {
        return m_active_rect;
    }

    Vector<Channel>& m_channels_view;
    IntRect m_active_rect;
};

class Image : public BlendedImage {
public:
    static ErrorOr<Image> create(IntSize size, ImageMetadata const& metadata)
    {
        Image image {};

        for (u16 i = 0; i < metadata.number_of_channels(); ++i) {
            if (i < metadata.number_of_color_channels()) {
                TRY(image.m_channels.try_append(TRY(Channel::create(ChannelInfo::from_size(size)))));
            } else {
                auto const dim_shift = metadata.ec_info[i - metadata.number_of_color_channels()].dim_shift;
                TRY(image.m_channels.try_append(TRY(Channel::create(
                    {
                        .width = static_cast<u32>(size.width() >> dim_shift),
                        .height = static_cast<u32>(size.height() >> dim_shift),
                    }))));
            }
        }

        return image;
    }

    static ErrorOr<Image> adopt_channels(Vector<Channel>&& channels)
    {
        if (channels.size() > 1) {
            if (any_of(channels, [&](auto const& channel) {
                    return channel.width() != channels[0].width() || channel.height() != channels[0].height();
                })) {
                return Error::from_string_literal("JPEGXLLoader: One of the Global Modular channel has a different size");
            }
        }
        return Image { move(channels) };
    }

    ErrorOr<ImageView> get_subimage(IntRect rectangle)
    {
        if (rectangle.right() > size().width()
            || rectangle.bottom() > size().height())
            return Error::from_string_literal("JPEGXLLoader: Can't create subimage from out-of-bounds rectangle");

        return ImageView { m_channels, rectangle };
    }

    ErrorOr<NonnullRefPtr<CMYKBitmap>> to_cmyk_bitmap(ImageMetadata const& metadata) const
    {
        auto const width = m_channels[0].width();
        auto const height = m_channels[0].height();

        if (metadata.bit_depth.bits_per_sample != 8)
            return Error::from_string_literal("JPEGXLLoader: Unsupported bit-depth for CMYK image");

        auto const orientation = static_cast<TIFF::Orientation>(metadata.orientation);
        auto oriented_bitmap = TRY(ExifOrientedCMYKBitmap::create(orientation, { width, height }));

        auto const black_channel = *metadata.black_channel();

        for (u32 y {}; y < height; ++y) {
            for (u32 x {}; x < width; ++x) {
                CMYK const color = CMYK(
                    255 - clamp(m_channels[0].get(x, y), 0, 255),
                    255 - clamp(m_channels[1].get(x, y), 0, 255),
                    255 - clamp(m_channels[2].get(x, y), 0, 255),
                    255 - clamp(m_channels[black_channel].get(x, y), 0, 255));
                oriented_bitmap.set_pixel(x, y, color);
            }
        }

        return oriented_bitmap.bitmap();
    }

    ErrorOr<NonnullRefPtr<Bitmap>> to_bitmap(ImageMetadata const& metadata) const
    {
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
                    if (metadata.number_of_color_channels() == 1) {
                        auto gray = to_u8(m_channels[0].get(x, y));
                        return { gray, gray, gray };
                    }

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

    virtual Vector<Channel> const& channels() const override
    {
        return m_channels;
    }

    virtual Vector<Channel>& channels() override
    {
        return m_channels;
    }

    IntRect rect() const
    {
        return active_rectangle();
    }

private:
    Image() = default;

    Image(Vector<Channel>&& channels)
        : m_channels(move(channels))
    {
    }

    IntRect active_rectangle() const override
    {
        return IntRect(0, 0, m_channels[0].width(), m_channels[0].height());
    }

    Vector<Channel> m_channels;
};
///

/// H.2 - Image decoding

static ErrorOr<void> add_default_squeeze_params(TransformInfo& tr, Span<ChannelInfo> channels, u32 nb_meta_channels)
{
    // H.6.2.1  Parameters - "The default parameters (the case when sp.size() == 0) are specified by the following code:"

    auto first = nb_meta_channels;
    auto count = channels.size() - first;
    auto w = channels[first].width;
    auto h = channels[first].height;
    SqueezeParams param;
    if (count > 2 && channels[first + 1].width == w && channels[first + 1].height == h) {
        param.begin_c = first + 1;
        param.num_c = 2;
        param.in_place = false;
        param.horizontal = true;
        tr.sp.append(param);
        param.horizontal = false;
        tr.sp.append(param);
    }
    param.begin_c = first;
    param.num_c = count;
    param.in_place = true;
    if (h >= w && h > 8) {
        param.horizontal = false;
        tr.sp.append(param);
        h = (h + 1) / 2;
    }
    while (w > 8 || h > 8) {
        if (w > 8) {
            param.horizontal = true;
            tr.sp.append(param);
            w = (w + 1) / 2;
        }
        if (h > 8) {
            param.horizontal = false;
            tr.sp.append(param);
            h = (h + 1) / 2;
        }
    }
    return {};
}

struct ModularData {
    bool use_global_tree {};
    WPHeader wp_params {};
    Vector<TransformInfo> transform {};

    // Initially, nb_meta_channels is set to zero, but transformations can modify this value.
    u32 nb_meta_channels {};

    Vector<Channel> channels {};

    ErrorOr<void> create_channels(Span<ChannelInfo> frame_size)
    {
        Vector<ChannelInfo> channel_infos {};
        TRY(channel_infos.try_extend(frame_size));

        for (auto& tr : transform) {
            if (tr.tr == TransformInfo::TransformId::kPalette) {
                // Let end_c = begin_c + num_c − 1. When updating the channel list as described in H.2, channels begin_c to end_c,
                // which all have the same dimensions, are replaced with two new channels:
                //  - one meta-channel, inserted at the beginning of the channel list and has dimensions width = nb_colours and height = num_c and hshift = vshift = −1.
                //    This channel represents the colours or deltas of the palette.
                //  - one channel (at the same position in the channel list as the original channels, same dimensions) which contains palette indices.

                auto original_dimensions = channel_infos[tr.begin_c];
                channel_infos.remove(tr.begin_c, tr.num_c);
                TRY(channel_infos.try_insert(tr.begin_c, original_dimensions));
                TRY(channel_infos.try_prepend({ .width = tr.nb_colours, .height = tr.num_c, .hshift = -1, .vshift = -1 }));

                if (tr.begin_c < nb_meta_channels)
                    nb_meta_channels += 2 - tr.begin_c;
                else
                    nb_meta_channels += 1;
            } else if (tr.tr == TransformInfo::TransformId::kSqueeze) {
                if (tr.sp.is_empty())
                    TRY(add_default_squeeze_params(tr, channel_infos, nb_meta_channels));

                // "Let begin = sp[i].begin_c and end = begin + sp[i].num_c − 1.
                // The channel list is modified as specified by the following code:"
                for (u32 i = 0; i < tr.sp.size(); i++) {
                    auto begin = tr.sp[i].begin_c;
                    auto end = begin + tr.sp[i].num_c - 1;
                    auto r = tr.sp[i].in_place ? end + 1 : channel_infos.size();
                    if (begin < nb_meta_channels) {
                        /* sp[i].in_place is true */
                        /* end < nb_meta_channels */
                        if (!tr.sp[i].in_place || end >= nb_meta_channels)
                            return Error::from_string_literal("JPEGXLLoader: Invalid values in the squeeze transform");
                        nb_meta_channels += tr.sp[i].num_c;
                    }
                    for (u32 c = begin; c <= end; c++) {
                        auto w = channel_infos[c].width;
                        auto h = channel_infos[c].height;
                        /* w > 0 and h > 0 */
                        if (w == 0 || h == 0)
                            return Error::from_string_literal("JPEGXLLoader: Can't apply the squeeze transform on a channel with a null dimension");

                        ChannelInfo residu;
                        if (tr.sp[i].horizontal) {
                            channel_infos[c].width = (w + 1) / 2;
                            if (channel_infos[c].hshift >= 0)
                                channel_infos[c].hshift++;
                            residu = channel_infos[c];
                            residu.width = w / 2;
                        } else {
                            channel_infos[c].height = (h + 1) / 2;
                            if (channel_infos[c].vshift >= 0)
                                channel_infos[c].vshift++;
                            residu = channel_infos[c];
                            residu.height = h / 2;
                        }
                        /* Insert residu into channel at index r + c − begin */
                        TRY(channel_infos.try_insert(r + c - begin, residu));
                    }
                }
            }
        }

        TRY(channels.try_ensure_capacity(channel_infos.size()));
        for (u32 i = 0; i < channel_infos.size(); ++i)
            channels.append(TRY(Channel::create(channel_infos[i])));

        return {};
    }
};

static constexpr u32 nb_base_predictors = 16;

static void get_properties(FixedArray<i32>& properties, Span<Channel> channels, u16 i, u32 x, u32 y, i32 max_error)
{
    // Table H.4 - Property definitions
    properties[0] = i;
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

struct ModularOptions {
    Span<ChannelInfo> channels_info;
    Optional<EntropyDecoder>& decoder;
    MATree const& global_tree;
    u32 group_dim {};
    u32 stream_index {};

    enum class ApplyTransformations : u8 {
        No,
        Yes,
    };

    ApplyTransformations apply_transformations { ApplyTransformations::Yes };
    u32 bit_depth {};
};

static ErrorOr<ModularData> read_modular_bitstream(LittleEndianInputBitStream& stream,
    ModularOptions&& options)
{
    auto [channels_info,
        decoder,
        global_tree,
        group_dim,
        stream_index,
        should_apply_transformation,
        bit_depth]
        = options;

    ModularData modular_data;

    modular_data.use_global_tree = TRY(stream.read_bit());
    modular_data.wp_params = TRY(read_self_correcting_predictor(stream));
    auto const nb_transforms = U32(0, 1, 2 + TRY(stream.read_bits(4)), 18 + TRY(stream.read_bits(8)));

    TRY(modular_data.transform.try_resize(nb_transforms));
    for (u32 i {}; i < nb_transforms; ++i)
        modular_data.transform[i] = TRY(read_transform_info(stream));

    TRY(modular_data.create_channels(channels_info));

    // "However, the decoder only decodes the first nb_meta_channels channels and any further channels
    // that have a width and height that are both at most group_dim. At that point, it stops decoding."
    u32 first_non_decoded_index = NumericLimits<u32>::max();
    auto will_be_decoded = [&](u32 index, Channel const& channel) {
        if (channel.width() == 0 || channel.height() == 0)
            return false;
        if (index < modular_data.nb_meta_channels)
            return true;
        if (index >= first_non_decoded_index)
            return false;
        if (channel.width() <= group_dim && channel.height() <= group_dim)
            return true;
        first_non_decoded_index = index;
        return false;
    };

    if constexpr (JPEGXL_DEBUG) {
        dbgln("Decoding modular sub-stream ({} tree, {} transforms, stream_index={}):",
            modular_data.use_global_tree ? "global"sv : "local"sv,
            nb_transforms,
            stream_index);

        for (auto const& tr : modular_data.transform) {
            switch (tr.tr) {
            case TransformInfo::TransformId::kRCT:
                dbgln("* RCT: begin_c={} - rct_type={}", tr.begin_c, tr.rct_type);
                break;
            case TransformInfo::TransformId::kPalette:
                dbgln("* Palette: begin_c={} - num_c={} - nb_colours={} - nb_deltas={} - d_pred={}",
                    tr.begin_c, tr.num_c, tr.nb_colours, tr.nb_deltas, tr.d_pred);
                break;
            case TransformInfo::TransformId::kSqueeze:
                dbgln("* Squeeze: num_sp={}", tr.sp.size());
                break;
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
    properties[1] = stream_index;

    auto const& tree = local_tree.has_value() ? *local_tree : global_tree;
    for (auto [i, channel] : enumerate(modular_data.channels)) {
        if (!will_be_decoded(i, channel))
            continue;

        auto self_correcting_data = TRY(SelfCorrectingData::create(modular_data.wp_params, channel.width()));

        for (u32 y {}; y < channel.height(); y++) {
            for (u32 x {}; x < channel.width(); x++) {
                auto const neighborhood = retrieve_neighborhood(channel, x, y);

                SelfCorrectingData::Predictions self_prediction {};
                if (tree.use_self_correcting_predictor())
                    self_prediction = self_correcting_data.compute_predictions(neighborhood, x);

                get_properties(properties, modular_data.channels, i, x, y, self_prediction.max_error);
                auto const leaf_node = tree.get_leaf(properties);
                auto diff = unpack_signed(TRY(decoder->decode_hybrid_uint(stream, leaf_node.ctx)));
                diff = (diff * leaf_node.multiplier) + leaf_node.offset;
                auto const total = diff + prediction(neighborhood, self_prediction.prediction, leaf_node.predictor);

                if (tree.use_self_correcting_predictor())
                    self_correcting_data.compute_errors(x, total);
                channel.set(x, y, total);
            }

            self_correcting_data.register_next_row();
        }

        channel.set_decoded(true);
    }
    TRY(decoder->ensure_end_state());

    if (should_apply_transformation == ModularOptions::ApplyTransformations::Yes) {
        for (auto const& tr : modular_data.transform.in_reverse())
            TRY(apply_transformation(modular_data.channels, tr, bit_depth, modular_data.wp_params));
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

    auto channels = TRY(FixedArray<ChannelInfo>::create(num_channels));
    channels.fill_with(ChannelInfo::from_size(frame_size));

    if (channels.is_empty())
        return global_modular;

    // "No inverse transforms are applied yet."
    global_modular.modular_data = TRY(read_modular_bitstream(stream,
        {
            .channels_info = channels,
            .decoder = global_modular.decoder,
            .global_tree = global_modular.ma_tree,
            .group_dim = frame_header.group_dim(),
            .stream_index = 0,
            .apply_transformations = ModularOptions::ApplyTransformations::No,
            .bit_depth = metadata.bit_depth.bits_per_sample,
        }));

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

    // "blending: arrays of count blend mode information structures, which consists of arrays of mode, alpha_channel and clamp"
    FixedArray<FixedArray<BlendingInfo>> blending;
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
    patch.blending = TRY(FixedArray<FixedArray<BlendingInfo>>::create(patch.count));
    for (auto& array : patch.blending)
        array = TRY(FixedArray<BlendingInfo>::create(num_extra_channels + 1));

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

        for (u32 k = 0; k < num_extra_channels + 1; k++) {
            u8 mode = TRY(decoder.decode_hybrid_uint(stream, 5));

            /* mode < 8 */
            if (mode >= 8)
                return Error::from_string_literal("JPEGXLLoader: Invalid mode when reading patches");
            patch.blending[j][k].mode = static_cast<BlendingInfo::BlendMode>(mode);
            // FIXME: The condition is supposed to be "/* there is more than 1 alpha channel */"
            //        rather than num_extra_channels > 1
            if (mode > 3 && num_extra_channels > 1) {
                patch.blending[j][k].alpha_channel = TRY(decoder.decode_hybrid_uint(stream, 8));
                // FIXME: Ensure that condition
                /* this is a valid index of an extra channel */
            }
            if (mode > 2)
                patch.blending[j][k].clamp = TRY(decoder.decode_hybrid_uint(stream, 9));
        }
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

    TRY(decoder.ensure_end_state());
    return patches;
}
///

/// I.2.1 - Quantizer
struct Quantizer {
    u32 global_scale {};
    u32 quant_lf {};
};

static ErrorOr<Quantizer> read_quantizer(LittleEndianInputBitStream& stream)
{
    Quantizer quantizer;
    quantizer.global_scale = U32(1 + TRY(stream.read_bits(11)), 2049 + TRY(stream.read_bits(11)), 4097 + TRY(stream.read_bits(12)), 8193 + TRY(stream.read_bits(16)));
    quantizer.quant_lf = U32(16, 1 + TRY(stream.read_bits(5)), 1 + TRY(stream.read_bits(8)), 1 + TRY(stream.read_bits(16)));

    return quantizer;
}
///

/// I.2.2 - HF block context decoding
struct HFBlockContext {
    Vector<u32> block_ctx_map {};
    Vector<u32> qf_thresholds {};
    Array<Vector<i32>, 3> lf_thresholds {};
};

static ErrorOr<HFBlockContext> read_hf_block_context(LittleEndianInputBitStream& stream)
{
    HFBlockContext hf_block_context;

    if (TRY(stream.read_bit())) {
        hf_block_context.block_ctx_map = { 0, 1, 2, 2, 3, 3, 4, 5, 6, 6, 6, 6, 6,
            7, 8, 9, 9, 10, 11, 12, 13, 14, 14, 14, 14, 14,
            7, 8, 9, 9, 10, 11, 12, 13, 14, 14, 14, 14, 14 };
    } else {
        Array<u8, 3> nb_lf_thr {};

        for (u8 i = 0; i < 3; i++) {
            nb_lf_thr[i] = TRY(stream.read_bits(4));
            for (u8 j = 0; j < nb_lf_thr[i]; j++) {
                i32 t = unpack_signed(U32(TRY(stream.read_bits(4)), 16 + TRY(stream.read_bits(8)), 272 + TRY(stream.read_bits(16)), 65808 + TRY(stream.read_bits(32))));
                TRY(hf_block_context.lf_thresholds[i].try_append(t));
            }
        }

        u8 nb_qf_thr = TRY(stream.read_bits(4));
        for (u8 i = 0; i < nb_qf_thr; i++) {
            u32 t = 1 + U32(TRY(stream.read_bits(2)), 4 + TRY(stream.read_bits(3)), 12 + TRY(stream.read_bits(5)), 44 + TRY(stream.read_bits(8)));
            TRY(hf_block_context.qf_thresholds.try_append(t));
        }

        u32 bsize = 39 * (nb_qf_thr + 1) * (nb_lf_thr[0] + 1) * (nb_lf_thr[1] + 1) * (nb_lf_thr[2] + 1);

        if (bsize > 39 * 64)
            return Error::from_string_literal("JPEGXLLoader: Invalid bsize in read HF Block Context");

        /* num_dist = bsize <= 39 * 64 and the resulting num_clusters <= 16 */
        auto [clusters, num_clusters] = TRY(read_pre_clustered_distributions(stream, bsize));
        hf_block_context.block_ctx_map = move(clusters);
        if (num_clusters > 16)
            return Error::from_string_literal("JPEGXLLoader: Invalid num_clusters in HF Block Context");
    }

    return hf_block_context;
}
///

/// I.2.3 - LF channel correlation factors
struct LfChannelCorrelation {
    u32 colour_factor { 84 };
    f32 base_correlation_x { 0.0 };
    f32 base_correlation_b { 1.0 };
    u8 x_factor_lf { 128 };
    u8 b_factor_lf { 128 };
};

static ErrorOr<LfChannelCorrelation> read_lf_channel_correlation(LittleEndianInputBitStream& stream)
{
    LfChannelCorrelation lf_channel_correlation;

    bool all_default = TRY(stream.read_bit());
    if (!all_default) {
        lf_channel_correlation.colour_factor = U32(84, 256, 2 + TRY(stream.read_bits(8)), 258 + TRY(stream.read_bits(16)));
        lf_channel_correlation.base_correlation_x = TRY(F16(stream));
        lf_channel_correlation.base_correlation_b = TRY(F16(stream));
        lf_channel_correlation.x_factor_lf = TRY(F16(stream));
        lf_channel_correlation.b_factor_lf = TRY(F16(stream));
    }

    return lf_channel_correlation;
}
///

/// G.1 - LfGlobal
struct LfGlobal {
    FixedArray<Patch> patches;
    LfChannelDequantization lf_dequant;
    Quantizer quantizer;
    HFBlockContext hf_block_ctx;
    LfChannelCorrelation lf_chan_corr;
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

    if (frame_header.encoding == Encoding::kVarDCT) {
        lf_global.quantizer = TRY(read_quantizer(stream));
        lf_global.hf_block_ctx = TRY(read_hf_block_context(stream));
        lf_global.lf_chan_corr = TRY(read_lf_channel_correlation(stream));
    }

    lf_global.gmodular = TRY(read_global_modular(stream, frame_size, frame_header, metadata));

    return lf_global;
}
///

/// Helpers to decode groups for the GlobalModular
static IntRect rect_for_group(ChannelInfo const& info, u32 group_dim, u32 group_index)
{
    u32 horizontal_group_dim = group_dim >> info.hshift;
    u32 vertical_group_dim = group_dim >> info.vshift;

    IntRect rect(0, 0, horizontal_group_dim, vertical_group_dim);

    auto nb_groups_per_row = (info.width + horizontal_group_dim - 1) / horizontal_group_dim;
    auto group_x = group_index % nb_groups_per_row;
    rect.set_x(group_x * horizontal_group_dim);
    if (group_x == nb_groups_per_row - 1 && info.width % horizontal_group_dim != 0) {
        rect.set_width(info.width % horizontal_group_dim);
    }

    auto nb_groups_per_column = (info.height + vertical_group_dim - 1) / vertical_group_dim;
    auto group_y = group_index / nb_groups_per_row;
    rect.set_y(group_y * vertical_group_dim);
    if (group_y == nb_groups_per_column - 1 && info.height % vertical_group_dim != 0) {
        rect.set_height(info.height % vertical_group_dim);
    }

    return rect;
}

struct GroupOptions {
    GlobalModular& global_modular;
    FrameHeader const& frame_header;
    u32 group_index {};
    u32 stream_index {};
    u32 bit_depth {};
    u32 group_dim {};
};

template<CallableAs<bool, Channel const&> F1, CallableAs<void, ChannelInfo const&> F2>
static ErrorOr<void> read_group_data(
    LittleEndianInputBitStream& stream,
    GroupOptions&& options,
    F1&& match_decode_conditions,
    F2&& debug_print)
{
    auto& [global_modular, frame_header, group_index, stream_index, bit_depth, group_dim] = options;

    Vector<ChannelInfo> channels_info;
    Vector<Channel&> original_channels;
    auto& channels = global_modular.modular_data.channels;
    for (auto& channel : channels) {
        if (!match_decode_conditions(channel))
            continue;

        auto rect_size = rect_for_group(channel.info(), group_dim, group_index).size();
        TRY(channels_info.try_append({
            .width = static_cast<u32>(rect_size.width()),
            .height = static_cast<u32>(rect_size.height()),
            .hshift = channel.hshift(),
            .vshift = channel.vshift(),
        }));
        TRY(original_channels.try_append(channel));
    }
    if (channels_info.is_empty())
        return {};

    if constexpr (JPEGXL_DEBUG)
        debug_print(original_channels[0].info());

    auto decoded = TRY(read_modular_bitstream(stream,
        {
            .channels_info = channels_info,
            .decoder = global_modular.decoder,
            .global_tree = global_modular.ma_tree,
            .group_dim = group_dim,
            .stream_index = stream_index,
            .apply_transformations = ModularOptions::ApplyTransformations::Yes,
            .bit_depth = bit_depth,
        }));

    // The decoded modular group data is then copied into the partially decoded GlobalModular image in the corresponding positions.
    for (u32 i = 0; i < original_channels.size(); ++i) {
        auto destination = rect_for_group(original_channels[i].info(), group_dim, group_index);
        original_channels[i].copy_from(destination, decoded.channels[i]);
    }

    return {};
}
///

/// G.2 - LfGroup
static constexpr i32 DCT_UNINITIALIZED = -2;
static constexpr i32 DCT_COVERED = -1;

struct VarDCTLfGroup {
    Channel x_from_y;
    Channel b_from_y;
    // dct_select hold DCT information in the top-left corner of every varblock.
    // -1 means occupied by a varblock but non top-left.
    // -2 is the default value, which shouldn't be found after proper initialization.
    Channel dct_select;
    Channel hf_mul;
    Channel sharpness;
};

struct LFGroupOptions {
    GlobalModular& global_modular;
    FrameHeader const& frame_header;
    u32 group_index {};
    u32 stream_index {};
    u32 bit_depth {};
};

// G.2.2 - LF coefficients
static ErrorOr<void> read_lf_coefficients(LittleEndianInputBitStream&, FrameHeader const& frame_header)
{
    // "If the kUseLfFrame flag in frame_header is set, this subclause is skipped"
    if (frame_header.flags & FrameHeader::Flags::kUseLfFrame)
        return {};

    return Error::from_string_literal("JPEGXLLoader: Implement reading LF coefficients");
}

// I.1 - Transform types
enum class TransformType : u8 {
    DCT8x8 = 0,
    Hornuss = 1,
    DCT2x2 = 2,
    DCT4x4 = 3,
    DCT16x16 = 4,
    DCT32x32 = 5,
    DCT16x8 = 6,
    DCT8x16 = 7,
    DCT32x8 = 8,
    DCT8x32 = 9,
    DCT32x16 = 10,
    DCT16x32 = 11,
    DCT4x8 = 12,
    DCT8x4 = 13,
    AFV0 = 14,
    AFV1 = 15,
    AFV2 = 16,
    AFV3 = 17,
    DCT64x64 = 18,
    DCT64x32 = 19,
    DCT32x64 = 20,
    DCT128x128 = 21,
    DCT128x64 = 22,
    DCT64x128 = 23,
    DCT256x256 = 24,
    DCT256x128 = 25,
    DCT128x256 = 26,
};

// NOTE: In the spec, DCT matrices use "matrices order" so DCT16x8 is actually
//       16 rows and 8 columns. This function return the size in "image order"
//       with columns first and rows in second.
static Size<u32> dct_select_to_dct_size(TransformType t)
{
    switch (t) {
    case TransformType::DCT8x8:
    case TransformType::Hornuss:
    case TransformType::DCT2x2:
    case TransformType::DCT4x4:
        return { 1, 1 };
    case TransformType::DCT16x16:
        return { 2, 2 };
    case TransformType::DCT32x32:
        return { 4, 4 };
    case TransformType::DCT16x8:
        return { 1, 2 };
    case TransformType::DCT8x16:
        return { 2, 1 };
    case TransformType::DCT32x8:
        return { 1, 4 };
    case TransformType::DCT8x32:
        return { 4, 1 };
    case TransformType::DCT32x16:
        return { 2, 4 };
    case TransformType::DCT16x32:
        return { 4, 2 };
    case TransformType::DCT4x8:
    case TransformType::DCT8x4:
        return { 1, 1 };
    case TransformType::AFV0:
    case TransformType::AFV1:
    case TransformType::AFV2:
    case TransformType::AFV3:
        return { 1, 1 };
    case TransformType::DCT64x64:
        return { 8, 8 };
    case TransformType::DCT64x32:
        return { 4, 8 };
    case TransformType::DCT32x64:
        return { 8, 4 };
    case TransformType::DCT128x128:
        return { 16, 16 };
    case TransformType::DCT128x64:
        return { 8, 16 };
    case TransformType::DCT64x128:
        return { 16, 8 };
    case TransformType::DCT256x256:
        return { 32, 32 };
    case TransformType::DCT256x128:
        return { 16, 32 };
    case TransformType::DCT128x256:
        return { 32, 16 };
    default:
        VERIFY_NOT_REACHED();
    }
}

static Size<u32> dct_select_to_image_size(TransformType t)
{
    return dct_select_to_dct_size(t).scaled(8);
}

// Table I.7 — Order ID for DctSelect values
static u8 dct_select_to_order_id(TransformType t)
{
    switch (t) {
    case TransformType::DCT8x8:
        return 0;
    case TransformType::Hornuss:
    case TransformType::DCT2x2:
    case TransformType::DCT4x4:
    case TransformType::DCT4x8:
    case TransformType::DCT8x4:
    case TransformType::AFV0:
    case TransformType::AFV1:
    case TransformType::AFV2:
    case TransformType::AFV3:
        return 1;
    case TransformType::DCT16x16:
        return 2;
    case TransformType::DCT32x32:
        return 3;
    case TransformType::DCT16x8:
    case TransformType::DCT8x16:
        return 4;
    case TransformType::DCT32x8:
    case TransformType::DCT8x32:
        return 5;
    case TransformType::DCT32x16:
    case TransformType::DCT16x32:
        return 6;
    case TransformType::DCT64x64:
        return 7;
    case TransformType::DCT64x32:
    case TransformType::DCT32x64:
        return 8;
    case TransformType::DCT128x128:
        return 9;
    case TransformType::DCT128x64:
    case TransformType::DCT64x128:
        return 10;
    case TransformType::DCT256x256:
        return 11;
    case TransformType::DCT256x128:
    case TransformType::DCT128x256:
        return 12;
    default:
        VERIFY_NOT_REACHED();
    }
}

struct LFGroupVarDCTOptions {
    Vector<Optional<VarDCTLfGroup>>& group_data;
    IntSize frame_size;
    u32 num_lf_group {};
};

// G.2.4 - HF metadata
static ErrorOr<void> read_hf_metadata(LittleEndianInputBitStream& stream,
    LFGroupOptions& options,
    LFGroupVarDCTOptions const& var_dct_options,
    u32 lf_group_dim)
{

    auto group_size = rect_for_group(ChannelInfo::from_size(var_dct_options.frame_size), lf_group_dim, options.group_index).size();

    // "The decoder reads nb_blocks = 1 + u(ceil(log2(ceil(width / 8) * ceil(height / 8))))."
    u32 nb_blocks = 1 + TRY(stream.read_bits(ceil(log2(ceil_div(group_size.width(), 8) * ceil_div(group_size.height(), 8)))));

    // "Then, the decoder reads a Modular sub-bitstream as described in Annex H, for an image with four channels."
    Vector<ChannelInfo> channels_info;
    TRY(channels_info.try_ensure_capacity(4));
    // "the first two channels have ceil(height / 64) rows and ceil(width / 64) columns"
    auto color_correlation_channels_size = IntSize { ceil_div(group_size.width(), 64), ceil_div(group_size.height(), 64) };
    channels_info.unchecked_append(ChannelInfo::from_size(color_correlation_channels_size));
    channels_info.unchecked_append(ChannelInfo::from_size(color_correlation_channels_size));
    // "the third channel has two rows and nb_blocks columns"
    channels_info.unchecked_append(ChannelInfo::from_size(IntSize(nb_blocks, 2)));
    // "and the fourth channel has ceil(height / 8) rows and ceil(width / 8) columns"
    channels_info.unchecked_append(ChannelInfo::from_size({ ceil_div(group_size.width(), 8), ceil_div(group_size.height(), 8) }));

    // "The stream index is defined as follows:
    //  - for ModularLfGroup: 1 + num_lf_groups + LF group index;
    //  - for HFMetadata: 1 + 2 * num_lf_groups + LF group index;"
    // We pass ModularLfGroup's stream index in LFGroupOptions, so we
    // just need to add `num_lf_groups` here.
    auto stream_index = options.stream_index + var_dct_options.num_lf_group;

    auto decoded_channels = TRY(read_modular_bitstream(stream,
                                    {
                                        .channels_info = channels_info,
                                        .decoder = options.global_modular.decoder,
                                        .global_tree = options.global_modular.ma_tree,
                                        .group_dim = lf_group_dim,
                                        .stream_index = stream_index,
                                        .apply_transformations = ModularOptions::ApplyTransformations::Yes,
                                        .bit_depth = options.bit_depth,
                                    }))
                                .channels;

    // "The DctSelect and HfMul fields are derived from the first and second rows of BlockInfo.
    // These two fields have ceil(height / 8) rows and ceil(width / 8) columns."
    auto derived_size = IntSize(ceil_div(group_size.width(), 8), ceil_div(group_size.height(), 8));
    auto dct_select = TRY(Channel::create(ChannelInfo::from_size(derived_size)));
    auto hf_mul = TRY(Channel::create(ChannelInfo::from_size(derived_size)));

    dct_select.fill(DCT_UNINITIALIZED);

    i32 x = 0;
    i32 y = 0;
    auto update_next_valid_position = [&]() {
        // "This position is the earliest block in raster order that is not already covered by
        // other varblocks. The positioned varblock is completely contained in the current LF
        // group, does not cross group boundaries, and also does not overlap with
        // already-positioned varblocks."

        // FIXME: There has to be a smarter way of doing this.
        while (dct_select.get(x, y) != DCT_UNINITIALIZED) {
            if (x == derived_size.width() - 1) {
                x = 0;
                y += 1;
                continue;
            }
            ++x;
        }
    };

    // "They are reconstructed by iterating over the columns of BlockInfo to obtain a varblock
    // transform type type (the sample at the first row) and a quantization multiplier mul (the
    // sample at the second row)."
    auto const& block_info = decoded_channels[2];
    for (u32 column = 0; column < nb_blocks; ++column) {
        auto type = block_info.get(column, 0);
        if (type > 26)
            return Error::from_string_literal("JPEGXLLoader: Invalid DctSelect value");

        auto mul = block_info.get(column, 1);

        // "The type is a DctSelect sample and is stored at the coordinates of the top-left
        // 8 × 8 rectangle of the varblock."
        dct_select.set(x, y, type);
        // "The HfMul sample is stored at the same position and gets the value 1 + mul."
        hf_mul.set(x, y, 1 + mul);

        // We fill the whole surface of the varblock as a way to check that
        // varblocks don't overlap.
        auto dct_size = dct_select_to_dct_size(static_cast<TransformType>(type));
        for (u8 y_offset = 0; y_offset < dct_size.height(); ++y_offset) {
            for (u8 x_offset = 0; x_offset < dct_size.width(); ++x_offset) {
                if (y_offset == 0 && x_offset == 0)
                    continue;
                if (dct_select.get(x + x_offset, y + y_offset) != DCT_UNINITIALIZED)
                    return Error::from_string_literal("JPEGXLLoader: Invalid varblocks pattern");
                dct_select.set(x + x_offset, y + y_offset, DCT_COVERED);
            }
        }
        if (column != nb_blocks - 1)
            update_next_valid_position();
    }

    // FIXME: Ensure that dct_select contains no DCT_UNINITIALIZED.

    var_dct_options.group_data[options.group_index] = VarDCTLfGroup {
        .x_from_y = move(decoded_channels[0]),
        .b_from_y = move(decoded_channels[1]),
        .dct_select = move(dct_select),
        .hf_mul = move(hf_mul),
        .sharpness = move(decoded_channels[2]),
    };
    return {};
}

static ErrorOr<void> read_lf_group(LittleEndianInputBitStream& stream,
    LFGroupOptions&& options,
    LFGroupVarDCTOptions&& var_dct_options)
{
    auto const& [global_modular, frame_header, group_index, stream_index, bit_depth] = options;

    if (options.frame_header.encoding == Encoding::kVarDCT) {
        if (var_dct_options.group_data.is_empty())
            TRY(var_dct_options.group_data.try_resize(var_dct_options.num_lf_group));
    }

    // LF coefficients
    if (frame_header.encoding == Encoding::kVarDCT)
        TRY(read_lf_coefficients(stream, frame_header));

    // ModularLfGroup
    u32 lf_group_dim = frame_header.group_dim() * 8;

    auto match_decoding_conditions = [](Channel const& channel) {
        if (channel.decoded())
            return false;
        if (channel.hshift() < 3 || channel.vshift() < 3)
            return false;
        return true;
    };
    TRY(read_group_data(
        stream,
        GroupOptions {
            .global_modular = global_modular,
            .frame_header = frame_header,
            .group_index = group_index,
            .stream_index = stream_index,
            .bit_depth = bit_depth,
            .group_dim = lf_group_dim },
        move(match_decoding_conditions),
        [&](auto const& first_channel) { dbgln("Decoding LFGroup {} for rectangle {}", group_index, rect_for_group(first_channel, lf_group_dim, group_index)); }));

    // HF metadata
    if (options.frame_header.encoding == Encoding::kVarDCT)
        TRY(read_hf_metadata(stream, options, var_dct_options, lf_group_dim));

    return {};
}
///

/// G.3 - HfGlobal
struct HfGlobalPassMetadata {
    // I.3.1 - HF coefficient order
    // 13 Order ID and 3 color component.
    // These spans refer to either the static, default values or
    // a Vector of backing_data.
    DCTOrderDescription order;
    Vector<Vector<Point<u32>>> backing_data;

    // I.3.3 - HF coefficient histograms
    u32 nb_block_ctx {};
    EntropyDecoder decoder;
};

struct HfGlobal {
    // Dequantization matrices.
    u32 num_hf_presets {};
    FixedArray<HfGlobalPassMetadata> hf_passes;
};

// I.2.4 - Dequantization matrices
static ErrorOr<void> read_quantization_matrices(LittleEndianInputBitStream& stream)
{
    // "First, the decoder reads a Bool(). If this is true, all matrices have their default encoding."
    bool is_default = TRY(stream.read_bit());

    if (!is_default)
        return Error::from_string_literal("JPEGXLLoader: Implement reading quantization matrices");

    return {};
}

// I.3 - HfPass
static ErrorOr<void> read_hf_passes(LittleEndianInputBitStream& stream, LfGlobal const& lf_global, HfGlobal& hf_global)
{
    // I.3.1 - HF coefficient order

    // "The decoder first reads used_orders as U32(0x5F, 0x13, 0x00, u(13))."
    u32 used_orders = U32(0x5F, 0x13, 0x00, TRY(stream.read_bits(13)));

    // "If used_orders != 0, it reads 8 pre-clustered distributions as specified in C.1."
    Optional<EntropyDecoder> decoder;
    if (used_orders != 0)
        decoder = TRY(EntropyDecoder::create(stream, 8));

    // "It then reads HF coefficient orders order[p][b][c] as specified by the code below,
    // where p is the index of the current pass, b is an Order ID (see Table I.7), c is a
    // component index, and natural_coeff_order[b] is the natural coefficient order for Order
    // ID b, as specified in I.3.2."
    auto const& natural_coeff_order = *TRY(DCTNaturalOrder::the());
    for (auto& pass_data : hf_global.hf_passes) {
        for (u8 b = 0; b < 13; b++) {
            for (u8 c = 0; c < 3; c++) {
                if ((used_orders & (1 << b)) != 0) {
                    // "DecodePermutation(b) is defined as follows. The decoder reads a permutation
                    // nat_ord_perm from a single stream (shared during the above loop) as specified
                    // in F.3.2, where size is the number of coefficients covered by transforms with
                    // Order ID b (so size == natural_coeff_order[b].size()) and skip = size / 64.
                    auto size = natural_coeff_order[b][c].size();
                    auto nat_ord_perm = TRY(decode_permutations(stream, *decoder, size, size / 64));

                    Vector<Point<u32>> local_order;
                    TRY(local_order.try_resize(size));
                    pass_data.order[b][c] = local_order.span();
                    TRY(pass_data.backing_data.try_append(move(local_order)));

                    for (u32 i = 0; i < nat_ord_perm.size(); ++i)
                        pass_data.order[b][c][i] = natural_coeff_order[b][c][nat_ord_perm[i]];
                } else {
                    pass_data.order[b][c] = natural_coeff_order[b][c];
                }
            }
        }

        // I.3.3 - HF coefficient histograms
        // "Let nb_block_ctx be equal to max(block_ctx_map) + 1."
        auto max = lf_global.hf_block_ctx.block_ctx_map[0];
        for (auto v : lf_global.hf_block_ctx.block_ctx_map) {
            if (v > max)
                max = v;
        }
        pass_data.nb_block_ctx = max + 1;

        // "The decoder reads a histogram with 495 * num_hf_presets * nb_block_ctx
        // pre-clustered distributions D from the codestream as specified in C.1."
        auto distributions = 495 * hf_global.num_hf_presets * pass_data.nb_block_ctx;
        pass_data.decoder = TRY(EntropyDecoder::create(stream, distributions));
    }

    if (decoder.has_value())
        TRY(decoder->ensure_end_state());

    return {};
}

static ErrorOr<HfGlobal> read_hf_global(LittleEndianInputBitStream& stream, LfGlobal const& lf_global, u32 num_groups, u32 num_passes)
{
    HfGlobal hf_global;

    TRY(read_quantization_matrices(stream));

    // I.2.6 - Number of HF decoding presets
    // "The decoder reads num_hf_presets as u(ceil(log2(num_groups))) + 1."
    hf_global.num_hf_presets = TRY(stream.read_bits(ceil(log2(num_groups)))) + 1;

    hf_global.hf_passes = TRY(FixedArray<HfGlobalPassMetadata>::create(num_passes));
    TRY(read_hf_passes(stream, lf_global, hf_global));

    return hf_global;
}
///

/// G.3.2 - PassGroup
struct PassGroupOptions {
    GlobalModular& global_modular;
    FrameHeader const& frame_header;
    u32 group_index;
    u32 pass_index;
    u32 stream_index;
};

struct PassGroupModularOptions {
    u32 bit_depth {};
};

// G.4.2 - Modular group data
static ErrorOr<void> read_modular_group_data(LittleEndianInputBitStream& stream,
    PassGroupOptions& options,
    PassGroupModularOptions const& modular_options)
{
    auto& [global_modular, frame_header, group_index, pass_index, stream_index] = options;

    i8 max_shift = 3;
    i8 min_shift = 0;

    if (pass_index != 0)
        return Error::from_string_literal("JPEGXLLoader: Subsequent passes are not supported yet");

    // for every remaining channel in the partially decoded GlobalModular image (i.e. it is not a meta-channel,
    // the channel dimensions exceed group_dim × group_dim, and hshift < 3 or vshift < 3, and the channel has
    // not been already decoded in a previous pass)
    auto match_decoding_conditions = [&](auto const& channel) {
        if (channel.decoded())
            return false;
        auto channel_min_shift = min(channel.hshift(), channel.vshift());
        if (channel_min_shift < min_shift || channel_min_shift >= max_shift)
            return false;
        return true;
    };

    TRY(read_group_data(stream,
        {
            .global_modular = global_modular,
            .frame_header = frame_header,
            .group_index = group_index,
            .stream_index = stream_index,
            .bit_depth = modular_options.bit_depth,
            .group_dim = frame_header.group_dim(),
        },
        move(match_decoding_conditions),
        [&](auto const& first_channel) { dbgln_if(JPEGXL_DEBUG, "Decoding pass {} for rectangle {}", options.pass_index, rect_for_group(first_channel, frame_header.group_dim(), group_index)); }));

    return {};
}

struct PassGroupVarDCTOptions {
    LfGlobal const& lf_global;
    Vector<Optional<VarDCTLfGroup>> const& lf_groups;
    HfGlobal& hf_global;
};

static constexpr Array CoeffFreqContext = to_array<u8>({ 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
    15, 15, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22,
    23, 23, 23, 23, 24, 24, 24, 24, 25, 25, 25, 25, 26, 26, 26, 26,
    27, 27, 27, 27, 28, 28, 28, 28, 29, 29, 29, 29, 30, 30, 30, 30 });

static constexpr Array CoeffNumNonzeroContext = to_array<u8>({ 0, 0, 31, 62, 62, 93, 93, 93, 93, 123, 123, 123, 123,
    152, 152, 152, 152, 152, 152, 152, 152, 180, 180, 180, 180, 180,
    180, 180, 180, 180, 180, 180, 180, 206, 206, 206, 206, 206, 206,
    206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206,
    206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206 });

// I.4 - Decoding of quantized HF coefficients
static ErrorOr<void> read_hf_coefficients(LittleEndianInputBitStream& stream,
    PassGroupOptions const& options,
    PassGroupVarDCTOptions&& var_dct_options)
{
    auto& hf_global = var_dct_options.hf_global;
    auto& hf_pass = hf_global.hf_passes[options.pass_index];
    auto const& hf_group = *var_dct_options.lf_groups[options.group_index];

    auto nb_block_ctx = hf_pass.nb_block_ctx;

    auto hfp = TRY(stream.read_bits(ceil(log2(hf_global.num_hf_presets))));
    u32 clusters_size = 495 * nb_block_ctx;
    u32 offset = clusters_size * hfp;

    Optional<ScopeGuard<Function<void()>>> restore_histogram;
    TRY(hf_pass.decoder.temporarily_restrict_histogram(restore_histogram, offset, clusters_size));
    auto& decoder = hf_pass.decoder;

    // "After selecting the histogram and coefficient order, the decoder reads symbols
    // from an entropy-coded stream, as specified in C.3.3."

    // "The decoder proceeds by decoding varblocks in raster order;"
    auto const& varblock_description = hf_group.dct_select;
    auto const& order = hf_pass.order;

    Array<Channel, 3> non_zeros_channels = {
        TRY(Channel::create(varblock_description.info())),
        TRY(Channel::create(varblock_description.info())),
        TRY(Channel::create(varblock_description.info()))
    };

    // "If the kUseLfFrame flag in frame_header is set [...] the quantized LF coefficients LfQuant are all set to −∞, that is,
    // regardless of lf_thresholds, the value of lf_idx at the end of the function BlockContext() (I.4) is always equal to zero."
    Array<i32, 3> qdc;
    qdc.fill(NumericLimits<i32>::min());

    for (u32 y = 0; y < varblock_description.height(); ++y) {
        for (u32 x = 0; x < varblock_description.width(); ++x) {
            auto dct_type = varblock_description.get(x, y);
            if (dct_type == DCT_UNINITIALIZED || dct_type == DCT_COVERED)
                continue;
            // "For each varblock of size W × H,"
            auto transform_type = static_cast<TransformType>(dct_type);
            auto varblock_size = dct_select_to_image_size(transform_type);
            auto W = varblock_size.width();
            auto H = varblock_size.height();
            // "covering num_blocks = (W / 8) * (H / 8) blocks,"
            u32 num_blocks = (W / 8) * (H / 8);

            // "s is the Order ID (see Table I.7) of the DctSelect value"
            auto s = dct_select_to_order_id(transform_type);
            // "qf is the HfMul value for the current varblock"
            u32 qf = hf_group.hf_mul.get(x, y);

            // FIXME: Implement this for in-frame LF coefficients.
            // "qdc[3] are the quantized LF values of LfQuant (G.2.2) corresponding to
            // (the top-left 8×8 block within) the current varblock (taking into account jpeg_upsampling if needed)."

            // "The lists of thresholds qf_thresholds and lf_thresholds[3], and block_ctx_map are as decoded in LfGlobal"
            auto const& qf_thresholds = var_dct_options.lf_global.hf_block_ctx.qf_thresholds;
            auto const& lf_thresholds = var_dct_options.lf_global.hf_block_ctx.lf_thresholds;
            auto const& block_ctx_map = var_dct_options.lf_global.hf_block_ctx.block_ctx_map;

            // "for each varblock it reads channels Y, X, then B;"
            // "where c is the current channel (with 0=X, 1=Y, 2=B)" - from the second paragraph of I.4
            for (u8 c : { 1, 0, 2 }) {
                auto BlockContext = [&]() -> u32 {
                    u32 idx = (c < 2 ? c ^ 1 : 2) * 13 + s;
                    idx *= (qf_thresholds.size() + 1);
                    for (auto t : qf_thresholds)
                        if (qf > t)
                            idx++;
                    for (u8 i = 0; i < 3; i++)
                        idx *= (lf_thresholds[i].size() + 1);
                    u32 lf_idx = 0;
                    for (auto t : lf_thresholds[0])
                        if (qdc[0] > t)
                            lf_idx++;
                    lf_idx *= (lf_thresholds[2].size() + 1);
                    for (auto t : lf_thresholds[2])
                        if (qdc[2] > t)
                            lf_idx++;
                    lf_idx *= (lf_thresholds[1].size() + 1);
                    for (auto t : lf_thresholds[1])
                        if (qdc[1] > t)
                            lf_idx++;
                    return block_ctx_map[idx + lf_idx];
                };

                auto NonZerosContext = [&](u32 predicted) -> u32 {
                    if (predicted > 64)
                        predicted = 64;
                    if (predicted < 8)
                        return BlockContext() + nb_block_ctx * predicted;
                    return BlockContext() + nb_block_ctx * (4 + predicted / 2);
                };

                auto NonZeros = [&](u32 x, u32 y) -> i32& {
                    return non_zeros_channels[c].get(x, y);
                };

                auto PredictedNonZeros = [&](u32 x, u32 y) -> u32 {
                    if (x == 0 and y == 0)
                        return 32;
                    if (x == 0)
                        return NonZeros(x, y - 1);
                    if (y == 0)
                        return NonZeros(x - 1, y);
                    return (NonZeros(x, y - 1) + NonZeros(x - 1, y) + 1) >> 1;
                };

                // "the decoder reads an integer non_zeros using
                // DecodeHybridVarLenUint(NonZerosContext(PredictedNonZeros(x, y)) + offset)."
                u32 context = NonZerosContext(PredictedNonZeros(x, y));
                auto non_zeros = TRY(decoder.decode_hybrid_uint(stream, context));

                // The decoder then sets the NonZeros(x, y) value for each block in the
                // current varblock as follows: for each i in [0, W / 8) and j in [0, H / 8),
                // NonZeros(x + i, y + j) is set to (non_zeros + num_blocks − 1) Idiv num_blocks.
                for (u32 j = 0; j < H / 8; ++j) {
                    for (u32 i = 0; i < W / 8; ++i)
                        NonZeros(x + i, y + j) = (non_zeros + num_blocks - 1) / num_blocks;
                }

                // "If non_zeros reaches 0, the decoder stops decoding further coefficients for the current block."
                if (non_zeros == 0)
                    continue;

                auto CoefficientContext = [&](u32 k, u32 non_zeros, u32 num_blocks, u32 prev) -> u32 {
                    non_zeros = (non_zeros + num_blocks - 1) / num_blocks;
                    k = k / num_blocks;
                    return (CoeffNumNonzeroContext[non_zeros] + CoeffFreqContext[k]) * 2 + prev + BlockContext() * 458 + 37 * nb_block_ctx;
                };

                // "Let size = W * H."
                auto size = W * H;
                // "For k in the range [num_blocks, size)"
                u32 last_ucoeff {};
                for (u32 k = num_blocks; k < size; ++k) {
                    // "the decoder reads an integer ucoeff from the codestream, using
                    // DecodeHybridVarLenUint(CoefficientContext(k, non_zeros, num_blocks, size, prev) + offset),
                    // where prev is computed as specified in the following code:"
                    auto prev = [&]() -> u32 {
                        if (k == num_blocks) {
                            if (non_zeros > size / 16)
                                return 0;
                            else
                                return 1;
                        } else {
                            if (last_ucoeff == 0)
                                return 0;
                            else
                                return 1;
                        }
                    }();

                    auto ucoeff = TRY(decoder.decode_hybrid_uint(stream, CoefficientContext(k, non_zeros, num_blocks, prev) + offset));
                    last_ucoeff = ucoeff;

                    // "The decoder then sets the quantized HF coefficient in the position corresponding to index
                    // order[p][s][c][k] to UnpackSigned(ucoeff), where p is the index of the current pass and s
                    // and c are the Order ID and current channel index as above."
                    auto destination = order[s][c][k];
                    // FIXME: Actually do something with the decoded data.
                    (void)destination;

                    // "If ucoeff != 0, the decoder decreases non_zeros by 1."
                    if (ucoeff != 0)
                        non_zeros -= 1;
                    // "If non_zeros reaches 0, the decoder stops decoding further coefficients for the current block."
                    if (non_zeros == 0)
                        break;
                }
            }
        }
    }

    TRY(decoder.ensure_end_state());

    return {};
}

// G.4.1 - General
static ErrorOr<void> read_pass_group(LittleEndianInputBitStream& stream,
    PassGroupOptions&& options,
    PassGroupModularOptions&& modular_options,
    PassGroupVarDCTOptions&& var_dct_options)
{
    if (options.frame_header.encoding == Encoding::kVarDCT)
        TRY(read_hf_coefficients(stream, options, move(var_dct_options)));

    TRY(read_modular_group_data(stream, options, modular_options));

    return {};
}
///

/// Table F.1 — Frame bundle
struct Frame {
    FrameHeader frame_header;
    TOC toc;
    LfGlobal lf_global;
    Vector<Optional<VarDCTLfGroup>> lf_groups;
    HfGlobal hf_global;

    u64 width {};
    u64 height {};

    u32 num_groups {};
    u32 num_lf_groups {};

    Optional<Image> image {};
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
        frame.width = ceil_div(frame.width, frame.frame_header.upsampling);
        frame.height = ceil_div(frame.height, frame.frame_header.upsampling);
    }

    // "If lf_level > 0 (which is also a field in frame_header), then
    // width = ceil(width / (1 << (3 * lf_level))) and height = ceil(height / (1 << (3 * lf_level)))."
    if (frame.frame_header.lf_level > 0) {
        frame.width = ceil_div(frame.width, 1u << (3 * frame.frame_header.lf_level));
        frame.height = ceil_div(frame.height, 1u << (3 * frame.frame_header.lf_level));
    }

    dbgln_if(JPEGXL_DEBUG, "Frame{}: {}x{} {} - {} - flags({}){}"sv,
        frame.frame_header.name.is_empty() ? ""sv : MUST(String::formatted(" \"{}\"", frame.frame_header.name)),
        frame.width, frame.height,
        frame.frame_header.encoding,
        frame.frame_header.frame_type,
        to_underlying(frame.frame_header.flags),
        frame.frame_header.is_last ? " - is_last"sv : ""sv);

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

    auto bits_per_sample = metadata.bit_depth.bits_per_sample;
    IntSize frame_size { frame.width, frame.height };

    auto get_stream_for_section = [&](LittleEndianInputBitStream& stream, u32 section_index) -> ErrorOr<MaybeOwned<LittleEndianInputBitStream>> {
        // "If num_groups == 1 and num_passes == 1, then there is a single TOC entry and a single section
        // containing all frame data structures."
        if (frame.num_groups == 1 && frame.frame_header.passes.num_passes == 1)
            return MaybeOwned(stream);
        auto section_size = frame.toc.entries[section_index];
        if (stream.align_to_byte_boundary() != 0)
            return Error::from_string_literal("JPEGXLLoader: Padding bits between sections must all be zeros");
        auto constrained_stream = make<AutoDepletingConstrainedStream>(MaybeOwned<Stream>(stream), section_size);
        return TRY(try_make<LittleEndianInputBitStream>(move(constrained_stream)));
    };

    {
        auto lf_stream = TRY(get_stream_for_section(stream, 0));
        frame.lf_global = TRY(read_lf_global(*lf_stream, frame_size, frame.frame_header, metadata));
    }

    for (u32 i {}; i < frame.num_lf_groups; ++i) {
        auto lf_stream = TRY(get_stream_for_section(stream, 1 + i));
        // From H.4.1, "The stream index is defined as follows: [...] for ModularLfGroup: 1 + num_lf_groups + LF group index;"
        TRY(read_lf_group(*lf_stream,
            {
                .global_modular = frame.lf_global.gmodular,
                .frame_header = frame.frame_header,
                .group_index = i,
                .stream_index = 1 + frame.num_lf_groups + i,
                .bit_depth = bits_per_sample,
            },
            {
                .group_data = frame.lf_groups,
                .frame_size = frame_size,
                .num_lf_group = frame.num_lf_groups,
            }));
    }

    {
        auto hf_global_stream = TRY(get_stream_for_section(stream, 1 + frame.num_lf_groups));
        if (frame.frame_header.encoding == Encoding::kVarDCT)
            frame.hf_global = TRY(read_hf_global(stream, frame.lf_global, frame.num_groups, frame.frame_header.passes.num_passes));
    }

    for (u32 pass_index {}; pass_index < frame.frame_header.passes.num_passes; ++pass_index) {
        for (u32 group_index {}; group_index < frame.num_groups; ++group_index) {
            auto toc_section_number = 2 + frame.num_lf_groups + pass_index * frame.num_groups + group_index;
            auto pass_stream = TRY(get_stream_for_section(stream, toc_section_number));

            // From H.4.1, ModularGroup: 1 + 3 * num_lf_groups + 17 + num_groups * pass index + group index
            u32 stream_index = 1 + 3 * frame.num_lf_groups + 17 + frame.num_groups * pass_index + group_index;
            TRY(read_pass_group(*pass_stream,
                {
                    .global_modular = frame.lf_global.gmodular,
                    .frame_header = frame.frame_header,
                    .group_index = group_index,
                    .pass_index = pass_index,
                    .stream_index = stream_index,
                },
                { .bit_depth = bits_per_sample },
                {
                    .lf_global = frame.lf_global,
                    .lf_groups = frame.lf_groups,
                    .hf_global = frame.hf_global,
                }));
        }
    }

    // G.4.2 - Modular group data
    // When all modular groups are decoded, the inverse transforms are applied to
    // the at that point fully decoded GlobalModular image, as specified in H.6.
    auto& channels = frame.lf_global.gmodular.modular_data.channels;
    auto const& transform_infos = frame.lf_global.gmodular.modular_data.transform;
    for (auto const& transformation : transform_infos.in_reverse())
        TRY(apply_transformation(channels, transformation, bits_per_sample, frame.lf_global.gmodular.modular_data.wp_params));

    if (frame.frame_header.encoding == Encoding::kVarDCT) {
        channels.prepend(TRY(Channel::create(ChannelInfo::from_size(frame_size))));
        channels.prepend(TRY(Channel::create(ChannelInfo::from_size(frame_size))));
        channels.prepend(TRY(Channel::create(ChannelInfo::from_size(frame_size))));
    }

    frame.image = TRY(Image::adopt_channels(move(channels)));

    return frame;
}
///

/// J - Restoration filters

// J.3  Gabor-like transform
using GaborWeights = Array<float, 2>;

static FloatMatrix3x3 construct_gabor_like_filter(GaborWeights weights)
{
    FloatMatrix3x3 filter {};

    // "the unnormalized weight for the center is 1"
    filter(1, 1) = 1;

    // "its four neighbours (top, bottom, left, right) are restoration_filter.gab_C_weight1"
    filter(0, 1) = weights[0];
    filter(1, 0) = weights[0];
    filter(1, 2) = weights[0];
    filter(2, 1) = weights[0];

    // "and the four corners (top-left, top-right, bottom-left, bottom-right) are restoration_filter.gab_C_weight2."
    filter(0, 0) = weights[1];
    filter(0, 2) = weights[1];
    filter(2, 0) = weights[1];
    filter(2, 2) = weights[1];

    // These weights are rescaled uniformly before convolution, such that the nine kernel weights sum to 1.
    return filter / filter.element_sum();
}

static FloatMatrix3x3 extract_matrix_from_channel(FloatChannel const& channel, u32 x, u32 y)
{
    FloatMatrix3x3 m;
    auto x_minus_1 = x == 0 ? mirror_1d(x, channel.width()) : x - 1;
    auto x_plus_1 = x == channel.width() - 1 ? mirror_1d(x, channel.width()) : x + 1;

    auto y_minus_1 = y == 0 ? mirror_1d(y, channel.height()) : y - 1;
    auto y_plus_1 = y == channel.height() - 1 ? mirror_1d(y, channel.height()) : y + 1;

    m(0, 0) = channel.get(x_minus_1, y_minus_1);
    m(0, 1) = channel.get(x, y_minus_1);
    m(0, 2) = channel.get(x_plus_1, y_minus_1);
    m(1, 0) = channel.get(x_minus_1, y);
    m(1, 1) = channel.get(x, y);
    m(1, 2) = channel.get(x_plus_1, y);
    m(2, 0) = channel.get(x_minus_1, y_plus_1);
    m(2, 1) = channel.get(x, y_plus_1);
    m(2, 2) = channel.get(x_plus_1, y_plus_1);

    return m;
}

static ErrorOr<void> apply_gabor_like_on_channel(FloatChannel& channel, GaborWeights weights)
{
    auto filter = construct_gabor_like_filter(weights);
    auto out = TRY(channel.copy());
    for (u32 y = 0; y < channel.height(); ++y) {
        for (u32 x = 0; x < channel.width(); ++x) {
            auto source = extract_matrix_from_channel(channel, x, y);
            auto result = source.hadamard_product(filter).element_sum();
            out.set(x, y, result);
        }
    }
    channel = move(out);
    return {};
}

static ErrorOr<void> apply_gabor_like_filter(RestorationFilter const& restoration_filter, Span<FloatChannel> channels)
{
    VERIFY(channels.size() == 3);

    Array<GaborWeights, 3> weights {
        GaborWeights { restoration_filter.gab_x_weight1, restoration_filter.gab_x_weight2 },
        GaborWeights { restoration_filter.gab_y_weight1, restoration_filter.gab_y_weight2 },
        GaborWeights { restoration_filter.gab_b_weight1, restoration_filter.gab_b_weight2 },
    };
    for (auto [i, channel] : enumerate(channels))
        TRY(apply_gabor_like_on_channel(channel, weights[i]));
    return {};
}

// J.4 - Edge-preserving filter

// J.4.2 - Distances
static f32 DistanceStep0and1(RestorationFilter const& rf, Span<FloatChannel const> input, u32 x, u32 y, i8 cx, i8 cy)
{
    f32 dist = 0;
    auto coords = to_array<IntPoint>({ { 0, 0 }, { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } });
    for (u8 c = 0; c < 3; c++) {
        for (auto coord : coords) {
            auto ix = coord.x();
            auto iy = coord.y();
            dist += abs(input[c].get_mirrored(x + ix, y + iy) - input[c].get_mirrored(x + cx + ix, y + cy + iy)) * rf.epf_channel_scale[c];
        }
    }
    return dist;
}

static f32 DistanceStep2(RestorationFilter const& rf, Span<FloatChannel const> input, u32 x, u32 y, i8 cx, i8 cy)
{
    f32 dist = 0;
    for (u8 c = 0; c < 3; c++) {
        dist += abs(input[c].get_mirrored(x, y) - input[c].get_mirrored(x + cx, y + cy)) * rf.epf_channel_scale[c];
    }
    return dist;
}

// J.4.3 - Weights
static f32 Weight(RestorationFilter const& rf, f32 step, f32 distance, f32 sigma, u32 x, u32 y)
{
    // "step = /* 0 if first step, 1 if second step, 2 if third step */;"
    Array<f32, 3> step_multiplier = { 1.65f * rf.epf_pass0_sigma_scale, 1.65f * 1, 1.65f * rf.epf_pass2_sigma_scale };
    f32 position_multiplier {};
    // "either coordinate of the reference sample is 0 or 7 UMod 8."
    if (x % 8 == 0 || x % 8 == 7 || y % 8 == 0 || y % 8 == 7)
        position_multiplier = rf.epf_border_sad_mul;
    else
        position_multiplier = 1;
    f32 inv_sigma = step_multiplier[step] * 4 * (1 - sqrt(0.5f)) / sigma;
    f32 scaled_distance = position_multiplier * distance;
    f32 v = 1 - scaled_distance * inv_sigma;
    if (v <= 0)
        return 0;
    return v;
}

// J.4.4 - Weighted average
static void apply_epf_step_on_pixel(RestorationFilter const& rf, Span<FloatChannel const> input, Span<FloatChannel> output, u32 step, f32 sigma, u32 x, u32 y)
{
    auto kernel_coords = [&]() {
        if (step == 0) {
            static constexpr Array points = to_array<IntPoint>({ { 0, 0 }, { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 },
                { 1, -1 }, { 1, 1 }, { -1, 1 }, { -1, -1 }, { -2, 0 },
                { 2, 0 }, { 0, 2 }, { 0, -2 } });
            return points.span();
        }
        static constexpr Array points = to_array<IntPoint>({ { 0, 0 }, { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } });
        return points.span();
    }();

    f32 sum_weights = 0;
    Array<f32, 3> sum_channels = { 0, 0, 0 };
    for (auto coord : kernel_coords) {
        auto ix = coord.x();
        auto iy = coord.y();
        f32 distance {};
        if (step == 0 || step == 1) {
            distance = DistanceStep0and1(rf, input, x, y, ix, iy);
        } else {
            distance = DistanceStep2(rf, input, x, y, ix, iy);
        }
        f32 weight = Weight(rf, step, distance, sigma, x, y);
        sum_weights += weight;
        for (u8 c = 0; c < 3; c++) {
            sum_channels[c] += input[c].get_mirrored(x + ix, y + iy) * weight;
        }
    }
    for (u8 c = 0; c < 3; c++) {
        output[c].set(x, y, sum_channels[c] / sum_weights);
    }
}

// J.4.1 - General
static void apply_epf_step(RestorationFilter const& rf, Span<FloatChannel const> input, Span<FloatChannel> output, u32 step, f32 sigma)
{
    for (u64 y = 0; y < input[0].height(); ++y) {
        for (u64 x = 0; x < input[0].width(); ++x)
            apply_epf_step_on_pixel(rf, input, output, step, sigma, x, y);
    }
}

static ErrorOr<void> apply_epf_filter(FrameHeader const& frame_header, Span<FloatChannel> channels)
{
    // "sigma is then computed as specified by the following code if the frame encoding is kVarDCT, else it is set to rf.epf_sigma_for_modular."
    if (frame_header.encoding == Encoding::kVarDCT)
        return Error::from_string_literal("FIXME: Compute epf's sigma for VarDCT frames.");
    f32 sigma = frame_header.restoration_filter.epf_sigma_for_modular;

    // "The output of each step is used as an input for the following step."
    Vector<FloatChannel> next_input;
    for (u8 i = 0; i < channels.size(); ++i)
        TRY(next_input.try_append(TRY(channels[i].copy())));

    // "The first step is only done if rf.epf_iters == 3."
    if (frame_header.restoration_filter.epf_iters == 3) {
        apply_epf_step(frame_header.restoration_filter, next_input, channels, 0, sigma);
        next_input.clear();
        for (u8 i = 0; i < channels.size(); ++i)
            TRY(next_input.try_append(TRY(channels[i].copy())));
    }

    // "The second step is always done (if rf.epf_iters > 0)."
    if (frame_header.restoration_filter.epf_iters > 0) {
        apply_epf_step(frame_header.restoration_filter, next_input, channels, 1, sigma);
        next_input.clear();
        for (u8 i = 0; i < channels.size(); ++i)
            TRY(next_input.try_append(TRY(channels[i].copy())));
    }

    // "The third step is only done if rf.epf_iters >= 2."
    if (frame_header.restoration_filter.epf_iters >= 2)
        apply_epf_step(frame_header.restoration_filter, next_input, channels, 2, sigma);

    return {};
}

struct SplitChannels {
    Vector<FloatChannel> color_channels {};
    Vector<Channel> extra_channels {};
};

template<typename T2, typename T1>
static ErrorOr<Vector<Detail::Channel<T2>>> convert_channels(Span<Detail::Channel<T1>> const& channels, u8 bits_per_sample)
{
    Vector<Detail::Channel<T2>> new_channels;
    TRY(new_channels.try_ensure_capacity(channels.size()));
    for (u32 i = 0; i < channels.size(); ++i)
        new_channels.append(TRY(channels[i].template as<T2>(bits_per_sample)));
    return new_channels;
}

static ErrorOr<SplitChannels> extract_color_channels(ImageMetadata const& metadata, Image& image)
{
    auto all_channels = move(image.channels());
    auto f32_color_channels = TRY(convert_channels<f32>(all_channels.span().trim(metadata.number_of_color_channels()), metadata.bit_depth.bits_per_sample));
    all_channels.remove(0, metadata.number_of_color_channels());
    return SplitChannels { move(f32_color_channels), move(all_channels) };
}

static ErrorOr<void> ensure_enough_color_channels(Vector<FloatChannel>& channels)
{
    if (channels.size() == 3)
        return {};
    VERIFY(channels.size() == 1);
    TRY(channels.try_append(TRY(channels[0].copy())));
    TRY(channels.try_append(TRY(channels[0].copy())));
    return {};
}

// J.1 - General
static ErrorOr<void> apply_restoration_filters(Frame& frame, ImageMetadata const& metadata)
{
    auto const& frame_header = frame.frame_header;

    if (frame_header.restoration_filter.gab || frame_header.restoration_filter.epf_iters != 0) {
        if constexpr (JPEGXL_DEBUG) {
            dbgln("Restoration filters:");
            dbgln(" * Gab: {}", frame_header.restoration_filter.gab);
            dbgln(" * EPF: {}", frame_header.restoration_filter.epf_iters);
        }

        // FIXME: Clarify where we should actually do the i32 -> f32 convertion.
        auto channels = TRY(extract_color_channels(metadata, *frame.image));
        TRY(ensure_enough_color_channels(channels.color_channels));

        if (frame_header.restoration_filter.gab)
            TRY(apply_gabor_like_filter(frame.frame_header.restoration_filter, channels.color_channels));
        if (frame_header.restoration_filter.epf_iters != 0)
            TRY(apply_epf_filter(frame_header, channels.color_channels));

        // Remove unwanted color channels if the image is greyscale.
        if (metadata.number_of_color_channels() == 1)
            channels.color_channels.remove(1, 2);
        auto i32_channels = TRY(convert_channels<i32>(channels.color_channels.span(), metadata.bit_depth.bits_per_sample));
        TRY(i32_channels.try_extend(move(channels.extra_channels)));
        frame.image = TRY(Image::adopt_channels(move(i32_channels)));
    }

    return {};
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
        for (auto& channel : frame.image->channels()) {
            auto upsampled = TRY(Channel::create({ .width = k * channel.width(), .height = k * channel.height() }));

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

                                    auto const origin_sample = channel.get_mirrored(x + ix - 2, y + iy - 2);

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

/// K.3.2  Patches rendering
static ErrorOr<void> apply_patches(Span<Frame> previous_frames, Frame& frame)
{
    auto& destination_image = frame.image;
    for (auto const& [i, patch] : enumerate(frame.lf_global.patches)) {
        if (patch.ref > previous_frames.size())
            return Error::from_string_literal("JPEGXLLoader: Unable to find the requested reference frame");

        auto& source_image = previous_frames[patch.ref].image;
        auto source_rect = IntRect({ patch.x0, patch.y0 }, { patch.width, patch.height });
        auto source_patch = TRY(source_image->get_subimage(source_rect));

        for (u32 j = 0; j < patch.count; ++j) {
            auto destination = IntRect(patch.positions[j], { patch.width, patch.height });
            auto destination_patch = TRY(destination_image->get_subimage(destination));
            // FIXME: "iterates over the three colour channels if c == 0 and refers to the extra channel with index c−1 otherwise"
            TRY(source_patch.blend_into(destination_patch, patch.blending[j][0].mode));
        }
    }

    return {};
}

static ErrorOr<void> apply_image_features(Span<Frame> previous_frames, Frame& frame, ImageMetadata const& metadata)
{
    TRY(apply_upsampling(frame, metadata));

    auto flags = frame.frame_header.flags;
    if (flags & FrameHeader::Flags::kPatches) {
        TRY(apply_patches(previous_frames, frame));
    } else if (flags != FrameHeader::Flags::None) {
        dbgln("JPEGXLLoader: Unsupported image features");
    }
    return {};
}
///

/// L.2 - XYB + L.3 - YCbCr
template<typename F>
static void for_each_pixel_of_color_channels(Image& image, F color_conversion)
{
    auto& channels = image.channels();
    VERIFY(channels.size() >= 3);

    VERIFY(channels[0].width() == channels[1].width() && channels[1].width() == channels[2].width());
    VERIFY(channels[0].height() == channels[1].height() && channels[1].height() == channels[2].height());

    for (u32 y = 0; y < channels[0].height(); ++y) {
        for (u32 x = 0; x < channels[0].width(); ++x) {
            color_conversion(channels[0].get(x, y), channels[1].get(x, y), channels[2].get(x, y));
        }
    }
}

static void ycbcr_to_rgb(Image& image, u8 bits_per_sample)
{
    auto const half_range_offset = (1 << bits_per_sample) / 2;
    auto color_conversion = [half_range_offset](i32& c1, i32& c2, i32& c3) {
        auto const cb = c1;
        auto const luma = c2;
        auto const cr = c3;

        c1 = luma + half_range_offset + 1.402 * cr;
        c2 = luma + half_range_offset - 0.344136 * cb - 0.714136 * cr;
        c3 = luma + half_range_offset + 1.772 * cb;
    };

    for_each_pixel_of_color_channels(image, move(color_conversion));
}

// L.2.2  Inverse XYB transform
static void xyb_to_rgb(Frame& frame, ImageMetadata const& metadata)
{
    // "X, Y, B samples are converted to an RGB colour encoding as specified in this subclause,
    // in which oim denotes metadata.opsin_inverse_matrix."
    auto const& oim = metadata.opsin_inverse_matrix;
    f32 to_int = (1 << metadata.bit_depth.bits_per_sample) - 1;
    auto linear_to_srgb = [](f32 c) {
        return c >= 0.0031308f ? 1.055f * pow(c, 0.4166666f) - 0.055f : 12.92f * c;
    };
    auto color_conversion = [&](i32& c1, i32& c2, i32& c3) {
        f32 const y_ = c1;
        f32 const x_ = c2;
        f32 const b_ = c3;

        f32 y {}, x {}, b {};
        if (frame.frame_header.encoding == Encoding::kModular) {
            y = y_ * frame.lf_global.lf_dequant.m_y_lf_unscaled;
            x = x_ * frame.lf_global.lf_dequant.m_x_lf_unscaled;
            b = (b_ + y_) * frame.lf_global.lf_dequant.m_b_lf_unscaled;
        } else {
            y = y_;
            x = x_;
            b = b_;
        }

        f32 Lgamma = y + x;
        f32 Mgamma = y - x;
        f32 Sgamma = b;
        f32 itscale = 255 / metadata.tone_mapping.intensity_target;
        f32 Lmix = (powf(Lgamma - cbrt(oim.opsin_bias0), 3) + oim.opsin_bias0) * itscale;
        f32 Mmix = (powf(Mgamma - cbrt(oim.opsin_bias1), 3) + oim.opsin_bias1) * itscale;
        f32 Smix = (powf(Sgamma - cbrt(oim.opsin_bias2), 3) + oim.opsin_bias2) * itscale;
        f32 R = oim.inv_mat00 * Lmix + oim.inv_mat01 * Mmix + oim.inv_mat02 * Smix;
        f32 G = oim.inv_mat10 * Lmix + oim.inv_mat11 * Mmix + oim.inv_mat12 * Smix;
        f32 B = oim.inv_mat20 * Lmix + oim.inv_mat21 * Mmix + oim.inv_mat22 * Smix;

        // "The resulting RGB samples correspond to sRGB primaries and a D65 white point, and the transfer function is linear."
        // We assume sRGB everywhere, so let's apply the transfer function here.
        R = linear_to_srgb(R);
        G = linear_to_srgb(G);
        B = linear_to_srgb(B);

        c1 = round_to<i32>(R * to_int);
        c2 = round_to<i32>(G * to_int);
        c3 = round_to<i32>(B * to_int);
    };

    for_each_pixel_of_color_channels(*frame.image, move(color_conversion));
}

static void apply_colour_transformation(Frame& frame, ImageMetadata const& metadata)
{
    if (frame.frame_header.do_YCbCr)
        ycbcr_to_rgb(*frame.image, metadata.bit_depth.bits_per_sample);

    if (metadata.xyb_encoded) {
        xyb_to_rgb(frame, metadata);
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

class LoadingContext {
public:
    LoadingContext(NonnullOwnPtr<Stream> stream)
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

        dbgln_if(JPEGXL_DEBUG, "Decoding a JPEG XL image with size {}x{} and {} channels, bit-depth={}{}.",
            m_header.width, m_header.height, m_metadata.number_of_channels(), m_metadata.bit_depth.bits_per_sample,
            m_metadata.colour_encoding.want_icc ? ", icc_profile"sv : ""sv);

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

        TRY(apply_restoration_filters(frame, m_metadata));

        TRY(apply_image_features(m_frames, frame, m_metadata));

        // "If lf_level != 0, the samples of the frame (before any colour transform is applied)
        // are recorded as LFFrame[lf_level−1] and may be referenced by subsequent frames."
        if (frame.frame_header.lf_level != 0) {
            m_lf_frames[frame.frame_header.lf_level - 1] = move(frame);
            return {};
        }

        if (!frame_header.save_before_ct) {
            apply_colour_transformation(frame, m_metadata);
        }

        TRY(render_extra_channels(*frame.image, m_metadata));

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

            while (m_frames.is_empty() || !m_frames.last().frame_header.is_last)
                TRY(decode_frame());

            TRY(render_frame());

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

    RefPtr<CMYKBitmap> cmyk_bitmap() const
    {
        return m_cmyk_bitmap;
    }

    ByteBuffer const& icc_profile() const
    {
        return m_icc_profile;
    }

    bool is_cmyk() const
    {
        return any_of(m_metadata.ec_info, [](auto& info) { return info.type == ExtraChannelInfo::ExtraChannelType::kBlack; });
    }

private:
    ErrorOr<void> render_frame()
    {
        auto final_image = TRY(Image::create({ m_header.width, m_header.height }, m_metadata));

        for (auto& frame : m_frames) {
            if (frame.frame_header.frame_type != FrameHeader::FrameType::kRegularFrame)
                continue;

            auto blending_mode = frame.frame_header.blending_info.mode;

            // "If x0 or y0 is negative, or the frame extends beyond the right or bottom
            // edge of the image, only the intersection of the frame with the image is
            // updated and contributes to the decoded image."
            IntRect frame_rect = frame.image->rect();
            auto image_rect = IntRect::intersection(frame_rect.translated(IntPoint { frame.frame_header.x0, frame.frame_header.y0 }), final_image.rect());
            frame_rect.set_x(-min(frame.frame_header.x0, 0));
            frame_rect.set_y(-min(frame.frame_header.y0, 0));
            frame_rect.set_size(image_rect.size());

            auto frame_out = TRY(frame.image->get_subimage(frame_rect));
            auto image_out = TRY(final_image.get_subimage(image_rect));
            TRY(frame_out.blend_into(image_out, blending_mode));
        }

        if (is_cmyk())
            m_cmyk_bitmap = TRY(final_image.to_cmyk_bitmap(m_metadata));
        else
            m_bitmap = TRY(final_image.to_bitmap(m_metadata));
        return {};
    }

    State m_state { State::NotDecoded };

    LittleEndianInputBitStream m_stream;
    RefPtr<Gfx::Bitmap> m_bitmap;
    RefPtr<Gfx::CMYKBitmap> m_cmyk_bitmap;

    Vector<Frame> m_frames;
    Array<Optional<Frame>, 4> m_lf_frames;

    SizeHeader m_header;
    ImageMetadata m_metadata;

    ByteBuffer m_icc_profile;
};

}

namespace Gfx {

JPEGXLImageDecoderPlugin::JPEGXLImageDecoderPlugin(Optional<Vector<u8>>&& jxlc_content, NonnullOwnPtr<FixedMemoryStream> stream)
    : m_context(make<JPEGXL::LoadingContext>(move(stream)))
    , m_jxlc_content(move(jxlc_content))
{
}

JPEGXLImageDecoderPlugin::~JPEGXLImageDecoderPlugin() = default;

IntSize JPEGXLImageDecoderPlugin::size()
{
    return m_context->size();
}

static bool is_raw_codestream(ReadonlyBytes data)
{
    return data.starts_with(to_array<u8>({ 0xFF, 0x0A }));
}

bool JPEGXLImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    // 18181-2: 9.1  JPEG XL Signature box (JXL␣)
    static constexpr Array signature = to_array<u8>({
        // clang-format off
        0x00, 0x00, 0x00, 0x0C,
        0x4A, 0x58, 0x4C, 0x20,
        0x0D, 0x0A, 0x87, 0x0A,
        // clang-format on
    });
    bool is_container = data.starts_with(signature);
    return is_raw_codestream(data) || is_container;
}

static ErrorOr<Vector<u8>> extract_codestream_from_container(NonnullOwnPtr<FixedMemoryStream> input)
{
    auto box_reader = TRY(ISOBMFF::Reader::create(move(input)));
    auto box_list = TRY(box_reader.read_entire_file());

    size_t jxlc_box_count = 0;
    size_t jxlp_box_count = 0;
    for (auto& box : box_list) {
        if (box->box_type() == ISOBMFF::BoxType::JPEGXLCodestreamBox)
            jxlc_box_count++;
        else if (box->box_type() == ISOBMFF::BoxType::JPEGXLPartialCodestreamBox)
            jxlp_box_count++;
    }

    // "A JPEG XL file shall contain either exactly one JPEG XL codestream box, or one or more JPEG XL partial
    //  codestream boxes, but not both."
    if (jxlc_box_count == 0 && jxlp_box_count == 0)
        return Error::from_string_literal("JPEGXLLoader: No jxlc box and no jxlp boxes found");
    if (jxlc_box_count > 1)
        return Error::from_string_literal("JPEGXLLoader: Multiple jxlc boxes found");
    if (jxlp_box_count > 0 && jxlc_box_count > 0)
        return Error::from_string_literal("JPEGXLLoader: Both jxlc box and jxlp boxes found");

    if (jxlc_box_count > 0) {
        auto& box = *box_list.find_if([](auto& box) { return box->box_type() == ISOBMFF::BoxType::JPEGXLCodestreamBox; });
        auto& codestream_box = static_cast<ISOBMFF::JPEGXLCodestreamBox&>(*box);
        return move(codestream_box.codestream);
    }

    // "The index modulo 2^31 shall be 0 for the first partial
    //  codestream box, and incremented by 1 for each next partial codestream box. The index shall be lower
    //  than 2^31, except for the last partial codestream box, which shall have an index of at least 2^31. The boxes
    //  shall appear in the file in order of increasing index. The full concatenation of all partial codestream
    //  boxes in this order shall form exactly one complete and valid JPEG XL codestream."
    // FIXME: Try to prevent the extra copy, maybe with a non-contiguous steam class.
    VERIFY(jxlp_box_count > 0);
    size_t next_part_index = 0;
    Vector<u8> codestream;
    for (auto& box : box_list) {
        if (box->box_type() != ISOBMFF::BoxType::JPEGXLPartialCodestreamBox)
            continue;
        auto& partial_box = static_cast<ISOBMFF::JPEGXLPartialCodestreamBox&>(*box);

        if (partial_box.index() != next_part_index)
            return Error::from_string_literal("JPEGXLLoader: Partial box indices not sequential");
        ++next_part_index;

        bool is_last_box = next_part_index == jxlp_box_count;
        if (partial_box.is_last() != is_last_box)
            return Error::from_string_literal("JPEGXLLoader: Invalid is_last bit on partial box");

        TRY(codestream.try_extend(partial_box.codestream));
    }
    return codestream;
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JPEGXLImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto stream = TRY(try_make<FixedMemoryStream>(data));
    Optional<Vector<u8>> jxlc_content;
    if (!is_raw_codestream(data)) {
        jxlc_content = TRY(extract_codestream_from_container(move(stream)));
        stream = TRY(try_make<FixedMemoryStream>(jxlc_content->span()));
    }
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JPEGXLImageDecoderPlugin(move(jxlc_content), move(stream))));
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

    if (m_context->state() == JPEGXL::LoadingContext::State::Error)
        return Error::from_string_literal("JPEGXLImageDecoderPlugin: Decoding failed");

    if (m_context->state() < JPEGXL::LoadingContext::State::FrameDecoded)
        TRY(m_context->decode());

    if (m_context->cmyk_bitmap() && !m_context->bitmap())
        return ImageFrameDescriptor { TRY(m_context->cmyk_bitmap()->to_low_quality_rgb()), 0 };

    return ImageFrameDescriptor { m_context->bitmap(), 0 };
}

ErrorOr<NonnullRefPtr<CMYKBitmap>> JPEGXLImageDecoderPlugin::cmyk_frame()
{
    if (m_context->state() == JPEGXL::LoadingContext::State::Error)
        return Error::from_string_literal("JPEGXLImageDecoderPlugin: Decoding failed");

    if (m_context->state() < JPEGXL::LoadingContext::State::FrameDecoded)
        TRY(m_context->decode());

    VERIFY(m_context->cmyk_bitmap() && !m_context->bitmap());
    return *m_context->cmyk_bitmap();
}

NaturalFrameFormat JPEGXLImageDecoderPlugin::natural_frame_format() const
{
    return m_context->is_cmyk() ? NaturalFrameFormat::CMYK : NaturalFrameFormat::RGB;
}

ErrorOr<Optional<ReadonlyBytes>> JPEGXLImageDecoderPlugin::icc_data()
{
    if (m_context->state() < JPEGXL::LoadingContext::State::ICCProfileDecoded)
        TRY(m_context->decode_icc());
    if (m_context->icc_profile().size() == 0)
        return OptionalNone {};
    return m_context->icc_profile();
}

}

namespace AK {

template<>
struct Formatter<Gfx::JPEGXL::Encoding> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::JPEGXL::Encoding const& header)
    {
        auto string = "Unknown"sv;
        switch (header) {
        case Gfx::JPEGXL::Encoding::kVarDCT:
            string = "VarDCT"sv;
            break;
        case Gfx::JPEGXL::Encoding::kModular:
            string = "Modular"sv;
            break;
        default:
            break;
        }

        return Formatter<StringView>::format(builder, string);
    }
};

template<>
struct Formatter<Gfx::JPEGXL::FrameHeader::FrameType> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::JPEGXL::FrameHeader::FrameType const& header)
    {
        switch (header) {
        case Gfx::JPEGXL::FrameHeader::FrameType::kRegularFrame:
            return Formatter<StringView>::format(builder, "RegularFrame"sv);
        case Gfx::JPEGXL::FrameHeader::FrameType::kLFFrame:
            return Formatter<StringView>::format(builder, "LFFrame"sv);
        case Gfx::JPEGXL::FrameHeader::FrameType::kReferenceOnly:
            return Formatter<StringView>::format(builder, "ReferenceOnly"sv);
        case Gfx::JPEGXL::FrameHeader::FrameType::kSkipProgressive:
            return Formatter<StringView>::format(builder, "SkipProgressive"sv);
        }
        VERIFY_NOT_REACHED();
    }
};

}
