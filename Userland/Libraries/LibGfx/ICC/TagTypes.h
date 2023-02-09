/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedPoint.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/ICC/DistinctFourCC.h>

namespace Gfx::ICC {

using S15Fixed16 = FixedPoint<16, i32>;
using U16Fixed16 = FixedPoint<16, u32>;

struct XYZ {
    double x { 0 };
    double y { 0 };
    double z { 0 };

    bool operator==(const XYZ&) const = default;
};

TagTypeSignature tag_type(ReadonlyBytes tag_bytes);

class TagData : public RefCounted<TagData> {
public:
    u32 offset() const { return m_offset; }
    u32 size() const { return m_size; }
    TagTypeSignature type() const { return m_type; }

    virtual ~TagData() = default;

protected:
    TagData(u32 offset, u32 size, TagTypeSignature type)
        : m_offset(offset)
        , m_size(size)
        , m_type(type)
    {
    }

private:
    u32 m_offset;
    u32 m_size;
    TagTypeSignature m_type;
};

class UnknownTagData : public TagData {
public:
    UnknownTagData(u32 offset, u32 size, TagTypeSignature type)
        : TagData(offset, size, type)
    {
    }
};

// ICC v4, 10.2 chromaticityType
class ChromaticityTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x6368726D }; // 'chrm'

    static ErrorOr<NonnullRefPtr<ChromaticityTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    // ICC v4, Table 31 — Colorant and phosphor encoding
    enum class PhosphorOrColorantType : u16 {
        Unknown = 0,
        ITU_R_BT_709_2 = 1,
        SMPTE_RP145 = 2,
        EBU_Tech_3213_E = 3,
        P22 = 4,
        P3 = 5,
        ITU_R_BT_2020 = 6,
    };

    static StringView phosphor_or_colorant_type_name(PhosphorOrColorantType);

    struct xyCoordinate {
        U16Fixed16 x;
        U16Fixed16 y;
    };

    ChromaticityTagData(u32 offset, u32 size, PhosphorOrColorantType phosphor_or_colorant_type, Vector<xyCoordinate> xy_coordinates)
        : TagData(offset, size, Type)
        , m_phosphor_or_colorant_type(phosphor_or_colorant_type)
        , m_xy_coordinates(move(xy_coordinates))
    {
    }

    PhosphorOrColorantType phosphor_or_colorant_type() const { return m_phosphor_or_colorant_type; }
    Vector<xyCoordinate> xy_coordinates() const { return m_xy_coordinates; }

private:
    PhosphorOrColorantType m_phosphor_or_colorant_type;
    Vector<xyCoordinate> m_xy_coordinates;
};

// ICC v4, 10.3 cicpType
// "The cicpType specifies Coding-independent code points for video signal type identification."
// See presentations at https://www.color.org/events/HDR_experts.xalter for background.
class CicpTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x63696370 }; // 'cicp'

    static ErrorOr<NonnullRefPtr<CicpTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    CicpTagData(u32 offset, u32 size, u8 color_primaries, u8 transfer_characteristics, u8 matrix_coefficients, u8 video_full_range_flag)
        : TagData(offset, size, Type)
        , m_color_primaries(color_primaries)
        , m_transfer_characteristics(transfer_characteristics)
        , m_matrix_coefficients(matrix_coefficients)
        , m_video_full_range_flag(video_full_range_flag)
    {
    }

    // "The fields ColourPrimaries, TransferCharacteristics, MatrixCoefficients, and VideoFullRangeFlag shall be
    //  encoded as specified in Recommendation ITU-T H.273. Recommendation ITU-T H.273 (ISO/IEC 23091-2)
    //  provides detailed descriptions of the code values and their interpretation."
    u8 color_primaries() const { return m_color_primaries; }
    u8 transfer_characteristics() const { return m_transfer_characteristics; }
    u8 matrix_coefficients() const { return m_matrix_coefficients; }
    u8 video_full_range_flag() const { return m_video_full_range_flag; }

private:
    u8 m_color_primaries;
    u8 m_transfer_characteristics;
    u8 m_matrix_coefficients;
    u8 m_video_full_range_flag;
};

// ICC v4, 10.6 curveType
class CurveTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x63757276 }; // 'curv'

    static ErrorOr<NonnullRefPtr<CurveTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    CurveTagData(u32 offset, u32 size, Vector<u16> values)
        : TagData(offset, size, Type)
        , m_values(move(values))
    {
    }

    // "The curveType embodies a one-dimensional function which maps an input value in the domain of the function
    //  to an output value in the range of the function. The domain and range values are in the range of 0,0 to 1,0.
    //  - When n is equal to 0, an identity response is assumed.
    //  - When n is equal to 1, then the curve value shall be interpreted as a gamma value, encoded as a
    //    u8Fixed8Number. Gamma shall be interpreted as the exponent in the equation y = pow(x,γ) and not as an inverse.
    //  - When n is greater than 1, the curve values (which embody a sampled one-dimensional function) shall be
    //    defined as follows:
    //    - The first entry represents the input value 0,0, the last entry represents the input value 1,0, and intermediate
    //      entries are uniformly spaced using an increment of 1,0/(n-1). These entries are encoded as uInt16Numbers
    //      (i.e. the values represented by the entries, which are in the range 0,0 to 1,0 are encoded in the range 0 to
    //      65 535). Function values between the entries shall be obtained through linear interpolation."
    Vector<u16> const& values() const { return m_values; }

private:
    Vector<u16> m_values;
};

struct EMatrix3x3 {
    // A row-major 3x3 matrix:
    // [ e[0] e[1] e[2] ]
    // [ e[3] e[4] e[5] ] * v
    // ] e[6] e[7] e[8] ]
    S15Fixed16 e[9];

    S15Fixed16 const& operator[](unsigned i) const
    {
        VERIFY(i < array_size(e));
        return e[i];
    }
};

// ICC v4, 10.10 lut16Type
class Lut16TagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x6D667432 }; // 'mft2'

    static ErrorOr<NonnullRefPtr<Lut16TagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    Lut16TagData(u32 offset, u32 size, EMatrix3x3 e,
        u8 number_of_input_channels, u8 number_of_output_channels, u8 number_of_clut_grid_points,
        u16 number_of_input_table_entries, u16 number_of_output_table_entries,
        Vector<u16> input_tables, Vector<u16> clut_values, Vector<u16> output_tables)
        : TagData(offset, size, Type)
        , m_e(e)
        , m_number_of_input_channels(number_of_input_channels)
        , m_number_of_output_channels(number_of_output_channels)
        , m_number_of_clut_grid_points(number_of_clut_grid_points)
        , m_number_of_input_table_entries(number_of_input_table_entries)
        , m_number_of_output_table_entries(number_of_output_table_entries)
        , m_input_tables(move(input_tables))
        , m_clut_values(move(clut_values))
        , m_output_tables(move(output_tables))
    {
        VERIFY(m_input_tables.size() == number_of_input_channels * number_of_input_table_entries);
        VERIFY(m_output_tables.size() == number_of_output_channels * number_of_output_table_entries);
    }

    EMatrix3x3 const& e_matrix() const { return m_e; }

    u8 number_of_input_channels() const { return m_number_of_input_channels; }
    u8 number_of_output_channels() const { return m_number_of_output_channels; }
    u8 number_of_clut_grid_points() const { return m_number_of_clut_grid_points; }

    u16 number_of_input_table_entries() const { return m_number_of_input_table_entries; }
    u16 number_of_output_table_entries() const { return m_number_of_output_table_entries; }

    Vector<u16> const& input_tables() const { return m_input_tables; }
    Vector<u16> const& clut_values() const { return m_clut_values; }
    Vector<u16> const& output_tables() const { return m_output_tables; }

private:
    EMatrix3x3 m_e;

    u8 m_number_of_input_channels;
    u8 m_number_of_output_channels;
    u8 m_number_of_clut_grid_points;

    u16 m_number_of_input_table_entries;
    u16 m_number_of_output_table_entries;

    Vector<u16> m_input_tables;
    Vector<u16> m_clut_values;
    Vector<u16> m_output_tables;
};

// ICC v4, 10.11 lut8Type
class Lut8TagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x6D667431 }; // 'mft1'

    static ErrorOr<NonnullRefPtr<Lut8TagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    Lut8TagData(u32 offset, u32 size, EMatrix3x3 e,
        u8 number_of_input_channels, u8 number_of_output_channels, u8 number_of_clut_grid_points,
        u16 number_of_input_table_entries, u16 number_of_output_table_entries,
        Vector<u8> input_tables, Vector<u8> clut_values, Vector<u8> output_tables)
        : TagData(offset, size, Type)
        , m_e(e)
        , m_number_of_input_channels(number_of_input_channels)
        , m_number_of_output_channels(number_of_output_channels)
        , m_number_of_clut_grid_points(number_of_clut_grid_points)
        , m_number_of_input_table_entries(number_of_input_table_entries)
        , m_number_of_output_table_entries(number_of_output_table_entries)
        , m_input_tables(move(input_tables))
        , m_clut_values(move(clut_values))
        , m_output_tables(move(output_tables))
    {
        VERIFY(m_input_tables.size() == number_of_input_channels * number_of_input_table_entries);
        VERIFY(m_output_tables.size() == number_of_output_channels * number_of_output_table_entries);
    }

    EMatrix3x3 const& e_matrix() const { return m_e; }

    u8 number_of_input_channels() const { return m_number_of_input_channels; }
    u8 number_of_output_channels() const { return m_number_of_output_channels; }
    u8 number_of_clut_grid_points() const { return m_number_of_clut_grid_points; }

    u16 number_of_input_table_entries() const { return m_number_of_input_table_entries; }
    u16 number_of_output_table_entries() const { return m_number_of_output_table_entries; }

    Vector<u8> const& input_tables() const { return m_input_tables; }
    Vector<u8> const& clut_values() const { return m_clut_values; }
    Vector<u8> const& output_tables() const { return m_output_tables; }

private:
    EMatrix3x3 m_e;

    u8 m_number_of_input_channels;
    u8 m_number_of_output_channels;
    u8 m_number_of_clut_grid_points;

    u16 m_number_of_input_table_entries;
    u16 m_number_of_output_table_entries;

    Vector<u8> m_input_tables;
    Vector<u8> m_clut_values;
    Vector<u8> m_output_tables;
};

struct EMatrix3x4 {
    // A row-major 3x3 matrix followed by a translation vector:
    // [ e[0] e[1] e[2] ]       [ e[9]  ]
    // [ e[3] e[4] e[5] ] * v + [ e[10] ]
    // [ e[6] e[7] e[8] ]       [ e[11] ]
    S15Fixed16 e[12];

    S15Fixed16 const& operator[](unsigned i) const
    {
        VERIFY(i < array_size(e));
        return e[i];
    }
};

struct CLUTData {
    Vector<u8, 4> number_of_grid_points_in_dimension;
    Variant<Vector<u8>, Vector<u16>> values;
};

// ICC v4, 10.12 lutAToBType
class LutAToBTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x6D414220 }; // 'mAB '

    static ErrorOr<NonnullRefPtr<LutAToBTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    LutAToBTagData(u32 offset, u32 size, u8 number_of_input_channels, u8 number_of_output_channels, Optional<CLUTData> clut, Optional<EMatrix3x4> e)
        : TagData(offset, size, Type)
        , m_number_of_input_channels(number_of_input_channels)
        , m_number_of_output_channels(number_of_output_channels)
        , m_clut(move(clut))
        , m_e(e)
    {
    }

    u8 number_of_input_channels() const { return m_number_of_input_channels; }
    u8 number_of_output_channels() const { return m_number_of_output_channels; }

    Optional<CLUTData> const& clut() const { return m_clut; }
    Optional<EMatrix3x4> const& e_matrix() const { return m_e; }

private:
    u8 m_number_of_input_channels;
    u8 m_number_of_output_channels;

    // "Only the following combinations are permitted:
    //  - B;
    //  - M, Matrix, B;
    //  - A, CLUT, B;
    //  - A, CLUT, M, Matrix, B."
    Optional<CLUTData> m_clut;
    Optional<EMatrix3x4> m_e;
};

// ICC v4, 10.13 lutBToAType
class LutBToATagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x6D424120 }; // 'mBA '

    static ErrorOr<NonnullRefPtr<LutBToATagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    LutBToATagData(u32 offset, u32 size, u8 number_of_input_channels, u8 number_of_output_channels, Optional<EMatrix3x4> e, Optional<CLUTData> clut)
        : TagData(offset, size, Type)
        , m_number_of_input_channels(number_of_input_channels)
        , m_number_of_output_channels(number_of_output_channels)
        , m_e(e)
        , m_clut(move(clut))
    {
    }

    u8 number_of_input_channels() const { return m_number_of_input_channels; }
    u8 number_of_output_channels() const { return m_number_of_output_channels; }

    Optional<EMatrix3x4> const& e_matrix() const { return m_e; }
    Optional<CLUTData> const& clut() const { return m_clut; }

private:
    u8 m_number_of_input_channels;
    u8 m_number_of_output_channels;

    // "Only the following combinations are permitted:
    //  - B;
    //  - B, Matrix, M;
    //  - B, CLUT, A;
    //  - B, Matrix, M, CLUT, A."
    Optional<EMatrix3x4> m_e;
    Optional<CLUTData> m_clut;
};

// ICC v4, 10.15 multiLocalizedUnicodeType
class MultiLocalizedUnicodeTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x6D6C7563 }; // 'mluc'

    static ErrorOr<NonnullRefPtr<MultiLocalizedUnicodeTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    struct Record {
        u16 iso_639_1_language_code;
        u16 iso_3166_1_country_code;
        String text;
    };

    MultiLocalizedUnicodeTagData(u32 offset, u32 size, Vector<Record> records)
        : TagData(offset, size, Type)
        , m_records(move(records))
    {
    }

    Vector<Record> const& records() const { return m_records; }

private:
    Vector<Record> m_records;
};

// ICC v4, 10.17 namedColor2Type
class NamedColor2TagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x6E636C32 }; // 'ncl2'

    static ErrorOr<NonnullRefPtr<NamedColor2TagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    // "The encoding is the same as the encodings for the PCS colour spaces
    //  as described in 6.3.4.2 and 10.8. Only PCSXYZ and
    //  legacy 16-bit PCSLAB encodings are permitted. PCS
    //  values shall be relative colorimetric."
    // (Which I suppose implies this type must not be used in DeviceLink profiles unless
    // the device's PCS happens to be PCSXYZ or PCSLAB.)
    struct XYZOrLAB {
        union {
            struct {
                u16 x, y, z;
            } xyz;
            struct {
                u16 L, a, b;
            } lab;
        };
    };

    NamedColor2TagData(u32 offset, u32 size, u32 vendor_specific_flag, u32 number_of_device_coordinates, String prefix, String suffix,
        Vector<String> root_names, Vector<XYZOrLAB> pcs_coordinates, Vector<u16> device_coordinates)
        : TagData(offset, size, Type)
        , m_vendor_specific_flag(vendor_specific_flag)
        , m_number_of_device_coordinates(number_of_device_coordinates)
        , m_prefix(move(prefix))
        , m_suffix(move(suffix))
        , m_root_names(move(root_names))
        , m_pcs_coordinates(move(pcs_coordinates))
        , m_device_coordinates(move(device_coordinates))
    {
        VERIFY(root_names.size() == pcs_coordinates.size());
        VERIFY(root_names.size() * number_of_device_coordinates == device_coordinates.size());
    }

    // "(least-significant 16 bits reserved for ICC use)"
    u32 vendor_specific_flag() const { return m_vendor_specific_flag; }

    // "If this field is 0, device coordinates are not provided."
    u32 number_of_device_coordinates() const { return m_number_of_device_coordinates; }

    u32 size() { return m_root_names.size(); }

    // "In order to maintain maximum portability, it is strongly recommended that
    //  special characters of the 7-bit ASCII set not be used."
    String const& prefix() const { return m_prefix; }                        // "7-bit ASCII"
    String const& suffix() const { return m_suffix; }                        // "7-bit ASCII"
    String const& root_name(u32 index) const { return m_root_names[index]; } // "7-bit ASCII"

    // Returns 7-bit ASCII.
    ErrorOr<String> color_name(u32 index);

    // "The PCS representation corresponds to the header’s PCS field."
    XYZOrLAB const& pcs_coordinates(u32 index) { return m_pcs_coordinates[index]; }

    // "The device representation corresponds to the header’s “data colour space” field."
    u16 const* device_coordinates(u32 index)
    {
        VERIFY((index + 1) * m_number_of_device_coordinates <= m_device_coordinates.size());
        return m_device_coordinates.data() + index * m_number_of_device_coordinates;
    }

private:
    u32 m_vendor_specific_flag;
    u32 m_number_of_device_coordinates;
    String m_prefix;
    String m_suffix;
    Vector<String> m_root_names;
    Vector<XYZOrLAB> m_pcs_coordinates;
    Vector<u16> m_device_coordinates;
};

// ICC v4, 10.18 parametricCurveType
class ParametricCurveTagData : public TagData {
public:
    // Table 68 — parametricCurveType function type encoding
    enum class FunctionType {
        // Y = X**g
        Type0,

        // Y = (a*X + b)**g       if X >= -b/a
        //   = 0                  else
        Type1,
        CIE_122_1966 = Type1,

        // Y = (a*X + b)**g + c   if X >= -b/a
        //   = c                  else
        Type2,
        IEC_61966_1 = Type2,

        // Y = (a*X + b)**g       if X >= d
        //   =  c*X               else
        Type3,
        IEC_61966_2_1 = Type3,
        sRGB = Type3,

        // Y = (a*X + b)**g + e   if X >= d
        //   =  c*X + f           else
        Type4,
    };

    // "The domain and range of each function shall be [0,0 1,0]. Any function value outside the range shall be clipped
    //  to the range of the function."
    // "NOTE 1 The parameters selected for a parametric curve can result in complex or undefined values for the input range
    //  used. This can occur, for example, if d < -b/a. In such cases the behaviour of the curve is undefined."

    static constexpr TagTypeSignature Type { 0x70617261 }; // 'para'

    static ErrorOr<NonnullRefPtr<ParametricCurveTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    ParametricCurveTagData(u32 offset, u32 size, FunctionType function_type, Array<S15Fixed16, 7> parameters)
        : TagData(offset, size, Type)
        , m_function_type(function_type)
        , m_parameters(move(parameters))
    {
    }

    FunctionType function_type() const { return m_function_type; }

    static unsigned parameter_count(FunctionType);

    S15Fixed16 g() const { return m_parameters[0]; }
    S15Fixed16 a() const
    {
        VERIFY(function_type() >= FunctionType::Type1);
        return m_parameters[1];
    }
    S15Fixed16 b() const
    {
        VERIFY(function_type() >= FunctionType::Type1);
        return m_parameters[2];
    }
    S15Fixed16 c() const
    {
        VERIFY(function_type() >= FunctionType::Type2);
        return m_parameters[3];
    }
    S15Fixed16 d() const
    {
        VERIFY(function_type() >= FunctionType::Type3);
        return m_parameters[4];
    }
    S15Fixed16 e() const
    {
        VERIFY(function_type() >= FunctionType::Type4);
        return m_parameters[5];
    }
    S15Fixed16 f() const
    {
        VERIFY(function_type() >= FunctionType::Type4);
        return m_parameters[6];
    }

private:
    FunctionType m_function_type;

    // Contains, in this order, g a b c d e f.
    // Not all FunctionTypes use all parameters.
    Array<S15Fixed16, 7> m_parameters;
};

// ICC v4, 10.22 s15Fixed16ArrayType
class S15Fixed16ArrayTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x73663332 }; // 'sf32'

    static ErrorOr<NonnullRefPtr<S15Fixed16ArrayTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    S15Fixed16ArrayTagData(u32 offset, u32 size, Vector<S15Fixed16, 9> values)
        : TagData(offset, size, Type)
        , m_values(move(values))
    {
    }

    Vector<S15Fixed16, 9> const& values() const { return m_values; }

private:
    Vector<S15Fixed16, 9> m_values;
};

// ICC v2, 6.5.17 textDescriptionType
class TextDescriptionTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x64657363 }; // 'desc'

    static ErrorOr<NonnullRefPtr<TextDescriptionTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    TextDescriptionTagData(u32 offset, u32 size, String ascii_description, u32 unicode_language_code, Optional<String> unicode_description, Optional<String> macintosh_description)
        : TagData(offset, size, Type)
        , m_ascii_description(move(ascii_description))
        , m_unicode_language_code(unicode_language_code)
        , m_unicode_description(move(unicode_description))
        , m_macintosh_description(move(macintosh_description))
    {
    }

    // Guaranteed to be 7-bit ASCII.
    String const& ascii_description() const { return m_ascii_description; }

    u32 unicode_language_code() const { return m_unicode_language_code; }
    Optional<String> const& unicode_description() const { return m_unicode_description; }

    Optional<String> const& macintosh_description() const { return m_macintosh_description; }

private:
    String m_ascii_description;

    u32 m_unicode_language_code { 0 };
    Optional<String> m_unicode_description;

    Optional<String> m_macintosh_description;
};

// ICC v4, 10.23 signatureType
class SignatureTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x73696720 }; // 'sig '

    static ErrorOr<NonnullRefPtr<SignatureTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    SignatureTagData(u32 offset, u32 size, u32 signature)
        : TagData(offset, size, Type)
        , m_signature(signature)
    {
    }

    u32 signature() const { return m_signature; }

private:
    u32 m_signature;
};

// ICC v4, 10.24 textType
class TextTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x74657874 }; // 'text'

    static ErrorOr<NonnullRefPtr<TextTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    TextTagData(u32 offset, u32 size, String text)
        : TagData(offset, size, Type)
        , m_text(move(text))
    {
    }

    // Guaranteed to be 7-bit ASCII.
    String const& text() const { return m_text; }

private:
    String m_text;
};

// ICC v4, 10.31 XYZType
class XYZTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x58595A20 }; // 'XYZ '

    static ErrorOr<NonnullRefPtr<XYZTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    XYZTagData(u32 offset, u32 size, Vector<XYZ, 1> xyzs)
        : TagData(offset, size, Type)
        , m_xyzs(move(xyzs))
    {
    }

    Vector<XYZ, 1> const& xyzs() const { return m_xyzs; }

private:
    Vector<XYZ, 1> m_xyzs;
};

}

template<>
struct AK::Formatter<Gfx::ICC::XYZ> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ICC::XYZ const& xyz)
    {
        return Formatter<FormatString>::format(builder, "X = {}, Y = {}, Z = {}"sv, xyz.x, xyz.y, xyz.z);
    }
};
