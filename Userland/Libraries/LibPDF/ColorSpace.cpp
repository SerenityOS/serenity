/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ICC/WellKnownProfiles.h>
#include <LibPDF/ColorSpace.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/ObjectDerivatives.h>
#include <LibPDF/Renderer.h>

namespace PDF {

RefPtr<Gfx::ICC::Profile> ICCBasedColorSpace::s_srgb_profile;

#define ENUMERATE(name, may_be_specified_directly) \
    ColorSpaceFamily ColorSpaceFamily::name { #name, may_be_specified_directly };
ENUMERATE_COLOR_SPACE_FAMILIES(ENUMERATE);
#undef ENUMERATE

PDFErrorOr<ColorSpaceFamily> ColorSpaceFamily::get(DeprecatedFlyString const& family_name)
{
#define ENUMERATE(f_name, may_be_specified_directly) \
    if (family_name == f_name.name()) {              \
        return ColorSpaceFamily::f_name;             \
    }
    ENUMERATE_COLOR_SPACE_FAMILIES(ENUMERATE)
#undef ENUMERATE
    dbgln_if(PDF_DEBUG, "Unknown ColorSpace family: {}", family_name);
    return Error(Error::Type::MalformedPDF, "Unknown ColorSpace family"_string);
}

PDFErrorOr<NonnullRefPtr<ColorSpace>> ColorSpace::create(Document* document, NonnullRefPtr<Object> color_space_object, Renderer& renderer)
{
    // "A color space is defined by an array object whose first element is a name object identifying the color space family.
    //  The remaining array elements, if any, are parameters that further characterize the color space;
    //  their number and types vary according to the particular family.
    //  For families that do not require parameters, the color space can be specified simply by the family name itself instead of an array."
    if (color_space_object->is<NameObject>())
        return ColorSpace::create(color_space_object->cast<NameObject>()->name(), renderer);
    if (color_space_object->is<ArrayObject>())
        return ColorSpace::create(document, color_space_object->cast<ArrayObject>(), renderer);
    return Error { Error::Type::MalformedPDF, "Color space must be name or array" };
}

PDFErrorOr<NonnullRefPtr<ColorSpace>> ColorSpace::create(DeprecatedFlyString const& name, Renderer&)
{
    // Simple color spaces with no parameters, which can be specified directly
    if (name == CommonNames::DeviceGray)
        return DeviceGrayColorSpace::the();
    if (name == CommonNames::DeviceRGB)
        return DeviceRGBColorSpace::the();
    if (name == CommonNames::DeviceCMYK)
        return TRY(DeviceCMYKColorSpace::the());
    if (name == CommonNames::Pattern)
        return Error::rendering_unsupported_error("Pattern color spaces not yet implemented");
    VERIFY_NOT_REACHED();
}

PDFErrorOr<NonnullRefPtr<ColorSpace>> ColorSpace::create(Document* document, NonnullRefPtr<ArrayObject> color_space_array, Renderer& renderer)
{
    auto color_space_name = TRY(color_space_array->get_name_at(document, 0))->name();

    Vector<Value> parameters;
    parameters.ensure_capacity(color_space_array->size() - 1);
    for (size_t i = 1; i < color_space_array->size(); i++)
        parameters.unchecked_append(color_space_array->at(i));

    if (color_space_name == CommonNames::CalGray)
        return TRY(CalGrayColorSpace::create(document, move(parameters)));

    if (color_space_name == CommonNames::CalRGB)
        return TRY(CalRGBColorSpace::create(document, move(parameters)));

    if (color_space_name == CommonNames::DeviceN)
        return TRY(DeviceNColorSpace::create(document, move(parameters), renderer));

    if (color_space_name == CommonNames::ICCBased)
        return TRY(ICCBasedColorSpace::create(document, move(parameters), renderer));

    if (color_space_name == CommonNames::Indexed)
        return TRY(IndexedColorSpace::create(document, move(parameters), renderer));

    if (color_space_name == CommonNames::Lab)
        return TRY(LabColorSpace::create(document, move(parameters)));

    if (color_space_name == CommonNames::Pattern)
        return Error::rendering_unsupported_error("Pattern color spaces not yet implemented");

    if (color_space_name == CommonNames::Separation)
        return TRY(SeparationColorSpace::create(document, move(parameters), renderer));

    dbgln("Unknown color space: {}", color_space_name);
    return Error::rendering_unsupported_error("unknown color space");
}

NonnullRefPtr<DeviceGrayColorSpace> DeviceGrayColorSpace::the()
{
    static auto instance = adopt_ref(*new DeviceGrayColorSpace());
    return instance;
}

PDFErrorOr<ColorOrStyle> DeviceGrayColorSpace::style(ReadonlySpan<float> arguments) const
{
    VERIFY(arguments.size() == 1);
    auto gray = static_cast<u8>(arguments[0] * 255.0f);
    return Color(gray, gray, gray);
}

Vector<float> DeviceGrayColorSpace::default_decode() const
{
    return { 0.0f, 1.0f };
}

NonnullRefPtr<DeviceRGBColorSpace> DeviceRGBColorSpace::the()
{
    static auto instance = adopt_ref(*new DeviceRGBColorSpace());
    return instance;
}

PDFErrorOr<ColorOrStyle> DeviceRGBColorSpace::style(ReadonlySpan<float> arguments) const
{
    VERIFY(arguments.size() == 3);
    auto r = static_cast<u8>(arguments[0] * 255.0f);
    auto g = static_cast<u8>(arguments[1] * 255.0f);
    auto b = static_cast<u8>(arguments[2] * 255.0f);
    return Color(r, g, b);
}

Vector<float> DeviceRGBColorSpace::default_decode() const
{
    return { 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f };
}

static RefPtr<Gfx::ICC::Profile> s_default_cmyk_profile;
static RefPtr<Core::Resource> s_default_cmyk_resource;

static ErrorOr<void> load_default_cmyk_profile()
{
    auto resource = TRY(Core::Resource::load_from_uri("resource://icc/Adobe/CMYK/USWebCoatedSWOP.icc"sv));
    auto profile = TRY(Gfx::ICC::Profile::try_load_from_externally_owned_memory(resource->data()));
    s_default_cmyk_resource = move(resource);
    s_default_cmyk_profile = move(profile);
    return {};
}

ErrorOr<NonnullRefPtr<DeviceCMYKColorSpace>> DeviceCMYKColorSpace::the()
{
    if (s_default_cmyk_profile.is_null())
        TRY(load_default_cmyk_profile());

    static auto instance = adopt_ref(*new DeviceCMYKColorSpace());
    return instance;
}

PDFErrorOr<ColorOrStyle> DeviceCMYKColorSpace::style(ReadonlySpan<float> arguments) const
{
    VERIFY(arguments.size() == 4);

    u8 bytes[4];
    bytes[0] = static_cast<u8>(arguments[0] * 255.0f);
    bytes[1] = static_cast<u8>(arguments[1] * 255.0f);
    bytes[2] = static_cast<u8>(arguments[2] * 255.0f);
    bytes[3] = static_cast<u8>(arguments[3] * 255.0f);
    auto pcs = TRY(s_default_cmyk_profile->to_pcs(bytes));

    Array<u8, 3> output;
    TRY(ICCBasedColorSpace::sRGB()->from_pcs(*s_default_cmyk_profile, pcs, output.span()));
    return Color(output[0], output[1], output[2]);
}

Vector<float> DeviceCMYKColorSpace::default_decode() const
{
    return { 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f };
}

PDFErrorOr<NonnullRefPtr<DeviceNColorSpace>> DeviceNColorSpace::create(Document* document, Vector<Value>&& parameters, Renderer& renderer)
{
    // "[ /DeviceN names alternateSpace tintTransform ]
    //  or
    //  [ /DeviceN names alternateSpace tintTransform attributes ]"
    // (`/DeviceN` is already stripped from the array by the time we get here.)
    if (parameters.size() != 3 && parameters.size() != 4)
        return Error { Error::Type::MalformedPDF, "DeviceN color space expects 4 or 5 parameters" };

    // "The names parameter is an array of name objects specifying the individual color components.
    //  The length of the array determines the number of components in the DeviceN color space"
    auto names_array = TRY(document->resolve_to<ArrayObject>(parameters[0]));
    Vector<ByteString> names;
    for (size_t i = 0; i < names_array->size(); ++i)
        names.append(names_array->get_name_at(i)->name());

    // "The alternateSpace parameter is an array or name object that can be any device or CIE-based color space
    //  but not another special color space (Pattern, Indexed, Separation, or DeviceN)."
    auto alternate_space_object = TRY(document->resolve_to<Object>(parameters[1]));
    auto alternate_space = TRY(ColorSpace::create(document, alternate_space_object, renderer));

    auto family = alternate_space->family();
    if (family == ColorSpaceFamily::Pattern || family == ColorSpaceFamily::Indexed || family == ColorSpaceFamily::Separation || family == ColorSpaceFamily::DeviceN)
        return Error { Error::Type::MalformedPDF, "DeviceN color space has invalid alternate color space" };

    // "The tintTransform parameter specifies a function"
    auto tint_transform_object = TRY(document->resolve_to<Object>(parameters[2]));
    auto tint_transform = TRY(Function::create(document, tint_transform_object));

    // FIXME: If `attributes` is present and has /Subtype set to /NChannel, possibly
    //        do slightly different processing.

    auto color_space = adopt_ref(*new DeviceNColorSpace(move(alternate_space), move(tint_transform)));
    color_space->m_names = move(names);
    return color_space;
}

DeviceNColorSpace::DeviceNColorSpace(NonnullRefPtr<ColorSpace> alternate_space, NonnullRefPtr<Function> tint_transform)
    : m_alternate_space(move(alternate_space))
    , m_tint_transform(move(tint_transform))
{
}

PDFErrorOr<ColorOrStyle> DeviceNColorSpace::style(ReadonlySpan<float> arguments) const
{
    // FIXME: Does this need handling for the special colorant name "None"?
    // FIXME: When drawing to a printer, do something else.
    auto tint_output = TRY(m_tint_transform->evaluate(arguments));

    m_tint_output_values.resize(tint_output.size());
    for (size_t i = 0; i < tint_output.size(); ++i)
        m_tint_output_values[i] = tint_output[i];

    return m_alternate_space->style(m_tint_output_values);
}

int DeviceNColorSpace::number_of_components() const
{
    return m_names.size();
}

Vector<float> DeviceNColorSpace::default_decode() const
{
    Vector<float> decoding_ranges;
    for (u8 i = 0; i < number_of_components(); i++) {
        decoding_ranges.append(0.0);
        decoding_ranges.append(1.0);
    }
    return decoding_ranges;
}

constexpr Array<float, 3> matrix_multiply(Array<float, 9> a, Array<float, 3> b)
{
    return Array<float, 3> {
        a[0] * b[0] + a[1] * b[1] + a[2] * b[2],
        a[3] * b[0] + a[4] * b[1] + a[5] * b[2],
        a[6] * b[0] + a[7] * b[1] + a[8] * b[2]
    };
}

// Converts to a flat XYZ space with white point = (1, 1, 1)
// Step 2 of https://www.color.org/adobebpc.pdf
constexpr Array<float, 3> flatten_and_normalize_whitepoint(Array<float, 3> whitepoint, Array<float, 3> xyz)
{
    VERIFY(whitepoint[1] == 1.0f);

    return {
        (1.0f / whitepoint[0]) * xyz[0],
        xyz[1],
        (1.0f / whitepoint[2]) * xyz[2],
    };
}

constexpr float decode_l(float input)
{
    constexpr float decode_l_scaling_constant = 0.00110705646f; // (((8 + 16) / 116) ^ 3) / 8

    if (input < 0.0f)
        return -decode_l(-input);
    if (input >= 0.0f && input <= 8.0f)
        return input * decode_l_scaling_constant;
    return powf(((input + 16.0f) / 116.0f), 3.0f);
}

constexpr Array<float, 3> scale_black_point(Array<float, 3> blackpoint, Array<float, 3> xyz)
{
    auto y_dst = decode_l(0); // DestinationBlackPoint is just [0, 0, 0]
    auto y_src = decode_l(blackpoint[0]);
    auto scale = (1 - y_dst) / (1 - y_src);
    auto offset = 1 - scale;

    return {
        xyz[0] * scale + offset,
        xyz[1] * scale + offset,
        xyz[2] * scale + offset,
    };
}

// https://en.wikipedia.org/wiki/Illuminant_D65
constexpr Array<float, 3> convert_to_d65(Array<float, 3> xyz)
{
    constexpr float d65x = 0.95047f;
    constexpr float d65y = 1.0f;
    constexpr float d65z = 1.08883f;

    return { xyz[0] * d65x, xyz[1] * d65y, xyz[2] * d65z };
}

// https://en.wikipedia.org/wiki/SRGB
constexpr Array<float, 3> convert_to_srgb(Array<float, 3> xyz)
{
    // See the sRGB D65 [M]^-1 matrix in the following page
    // http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
    constexpr Array<float, 9> conversion_matrix = {
        3.2404542,
        -1.5371385,
        -0.4985314,
        -0.969266,
        1.8760108,
        0.0415560,
        0.0556434,
        -0.2040259,
        1.0572252,
    };

    auto linear_srgb = matrix_multiply(conversion_matrix, xyz);
    linear_srgb[0] = clamp(linear_srgb[0], 0.0f, 1.0f);
    linear_srgb[1] = clamp(linear_srgb[1], 0.0f, 1.0f);
    linear_srgb[2] = clamp(linear_srgb[2], 0.0f, 1.0f);

    // FIXME: Use the real sRGB curve by replacing this function with Gfx::ICC::sRGB().from_pcs().
    return { pow(linear_srgb[0], 1.0f / 2.2f), pow(linear_srgb[1], 1.0f / 2.2f), pow(linear_srgb[2], 1.0f / 2.2f) };
}

PDFErrorOr<NonnullRefPtr<CalGrayColorSpace>> CalGrayColorSpace::create(Document* document, Vector<Value>&& parameters)
{
    if (parameters.size() != 1)
        return Error { Error::Type::MalformedPDF, "Gray color space expects one parameter" };

    auto dict = TRY(document->resolve_to<DictObject>(parameters[0]));
    if (!dict->contains(CommonNames::WhitePoint))
        return Error { Error::Type::MalformedPDF, "Gray color space expects a Whitepoint key" };

    auto white_point_array = TRY(dict->get_array(document, CommonNames::WhitePoint));
    if (white_point_array->size() != 3)
        return Error { Error::Type::MalformedPDF, "Gray color space expects 3 Whitepoint parameters" };

    auto color_space = adopt_ref(*new CalGrayColorSpace());

    color_space->m_whitepoint[0] = white_point_array->at(0).to_float();
    color_space->m_whitepoint[1] = white_point_array->at(1).to_float();
    color_space->m_whitepoint[2] = white_point_array->at(2).to_float();

    if (color_space->m_whitepoint[1] != 1.0f)
        return Error { Error::Type::MalformedPDF, "Gray color space expects 2nd Whitepoint to be 1.0" };

    if (dict->contains(CommonNames::BlackPoint)) {
        auto black_point_array = TRY(dict->get_array(document, CommonNames::BlackPoint));
        if (black_point_array->size() == 3) {
            color_space->m_blackpoint[0] = black_point_array->at(0).to_float();
            color_space->m_blackpoint[1] = black_point_array->at(1).to_float();
            color_space->m_blackpoint[2] = black_point_array->at(2).to_float();
        }
    }

    if (dict->contains(CommonNames::Gamma)) {
        color_space->m_gamma = TRY(document->resolve(dict->get_value(CommonNames::Gamma))).to_float();
    }

    return color_space;
}

PDFErrorOr<ColorOrStyle> CalGrayColorSpace::style(ReadonlySpan<float> arguments) const
{
    VERIFY(arguments.size() == 1);
    auto a = clamp(arguments[0], 0.0f, 1.0f);

    auto ag = powf(a, m_gamma);

    auto x = m_whitepoint[0] * ag;
    auto y = m_whitepoint[1] * ag;
    auto z = m_whitepoint[2] * ag;

    auto flattened_xyz = flatten_and_normalize_whitepoint(m_whitepoint, { x, y, z });
    auto scaled_black_point_xyz = scale_black_point(m_blackpoint, flattened_xyz);
    auto d65_normalized = convert_to_d65(scaled_black_point_xyz);
    auto srgb = convert_to_srgb(d65_normalized);

    auto red = static_cast<u8>(clamp(srgb[0], 0.0f, 1.0f) * 255.0f);
    auto green = static_cast<u8>(clamp(srgb[1], 0.0f, 1.0f) * 255.0f);
    auto blue = static_cast<u8>(clamp(srgb[2], 0.0f, 1.0f) * 255.0f);

    return Color(red, green, blue);
}

Vector<float> CalGrayColorSpace::default_decode() const
{
    return { 0.0f, 1.0f };
}

PDFErrorOr<NonnullRefPtr<CalRGBColorSpace>> CalRGBColorSpace::create(Document* document, Vector<Value>&& parameters)
{
    if (parameters.size() != 1)
        return Error { Error::Type::MalformedPDF, "RGB color space expects one parameter" };

    auto dict = TRY(document->resolve_to<DictObject>(parameters[0]));
    if (!dict->contains(CommonNames::WhitePoint))
        return Error { Error::Type::MalformedPDF, "RGB color space expects a Whitepoint key" };

    auto white_point_array = TRY(dict->get_array(document, CommonNames::WhitePoint));
    if (white_point_array->size() != 3)
        return Error { Error::Type::MalformedPDF, "RGB color space expects 3 Whitepoint parameters" };

    auto color_space = adopt_ref(*new CalRGBColorSpace());

    color_space->m_whitepoint[0] = white_point_array->at(0).to_float();
    color_space->m_whitepoint[1] = white_point_array->at(1).to_float();
    color_space->m_whitepoint[2] = white_point_array->at(2).to_float();

    if (color_space->m_whitepoint[1] != 1.0f)
        return Error { Error::Type::MalformedPDF, "RGB color space expects 2nd Whitepoint to be 1.0" };

    if (dict->contains(CommonNames::BlackPoint)) {
        auto black_point_array = TRY(dict->get_array(document, CommonNames::BlackPoint));
        if (black_point_array->size() == 3) {
            color_space->m_blackpoint[0] = black_point_array->at(0).to_float();
            color_space->m_blackpoint[1] = black_point_array->at(1).to_float();
            color_space->m_blackpoint[2] = black_point_array->at(2).to_float();
        }
    }

    if (dict->contains(CommonNames::Gamma)) {
        auto gamma_array = TRY(dict->get_array(document, CommonNames::Gamma));
        if (gamma_array->size() == 3) {
            color_space->m_gamma[0] = gamma_array->at(0).to_float();
            color_space->m_gamma[1] = gamma_array->at(1).to_float();
            color_space->m_gamma[2] = gamma_array->at(2).to_float();
        }
    }

    if (dict->contains(CommonNames::Matrix)) {
        auto matrix_array = TRY(dict->get_array(document, CommonNames::Matrix));
        if (matrix_array->size() == 9) {
            color_space->m_matrix[0] = matrix_array->at(0).to_float();
            color_space->m_matrix[1] = matrix_array->at(1).to_float();
            color_space->m_matrix[2] = matrix_array->at(2).to_float();
            color_space->m_matrix[3] = matrix_array->at(3).to_float();
            color_space->m_matrix[4] = matrix_array->at(4).to_float();
            color_space->m_matrix[5] = matrix_array->at(5).to_float();
            color_space->m_matrix[6] = matrix_array->at(6).to_float();
            color_space->m_matrix[7] = matrix_array->at(7).to_float();
            color_space->m_matrix[8] = matrix_array->at(8).to_float();
        }
    }

    return color_space;
}

PDFErrorOr<ColorOrStyle> CalRGBColorSpace::style(ReadonlySpan<float> arguments) const
{
    VERIFY(arguments.size() == 3);
    auto a = clamp(arguments[0], 0.0f, 1.0f);
    auto b = clamp(arguments[1], 0.0f, 1.0f);
    auto c = clamp(arguments[2], 0.0f, 1.0f);

    auto agr = powf(a, m_gamma[0]);
    auto bgg = powf(b, m_gamma[1]);
    auto cgb = powf(c, m_gamma[2]);

    auto x = m_matrix[0] * agr + m_matrix[3] * bgg + m_matrix[6] * cgb;
    auto y = m_matrix[1] * agr + m_matrix[4] * bgg + m_matrix[7] * cgb;
    auto z = m_matrix[2] * agr + m_matrix[5] * bgg + m_matrix[8] * cgb;

    auto flattened_xyz = flatten_and_normalize_whitepoint(m_whitepoint, { x, y, z });
    auto scaled_black_point_xyz = scale_black_point(m_blackpoint, flattened_xyz);
    auto d65_normalized = convert_to_d65(scaled_black_point_xyz);
    auto srgb = convert_to_srgb(d65_normalized);

    auto red = static_cast<u8>(clamp(srgb[0], 0.0f, 1.0f) * 255.0f);
    auto green = static_cast<u8>(clamp(srgb[1], 0.0f, 1.0f) * 255.0f);
    auto blue = static_cast<u8>(clamp(srgb[2], 0.0f, 1.0f) * 255.0f);

    return Color(red, green, blue);
}

Vector<float> CalRGBColorSpace::default_decode() const
{
    return { 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f };
}

PDFErrorOr<NonnullRefPtr<ColorSpace>> ICCBasedColorSpace::create(Document* document, Vector<Value>&& parameters, Renderer& renderer)
{
    if (parameters.is_empty())
        return Error { Error::Type::MalformedPDF, "ICCBased color space expected one parameter" };

    auto stream = TRY(document->resolve_to<StreamObject>(parameters[0]));
    auto dict = stream->dict();

    auto maybe_profile = Gfx::ICC::Profile::try_load_from_externally_owned_memory(stream->bytes());
    if (!maybe_profile.is_error())
        return adopt_ref(*new ICCBasedColorSpace(maybe_profile.release_value()));

    if (dict->contains(CommonNames::Alternate)) {
        auto alternate_color_space_object = MUST(dict->get_object(document, CommonNames::Alternate));
        if (alternate_color_space_object->is<NameObject>())
            return ColorSpace::create(alternate_color_space_object->cast<NameObject>()->name(), renderer);

        return Error { Error::Type::Internal, "Alternate color spaces in array format are not supported" };
    }

    return maybe_profile.release_error();
}

ICCBasedColorSpace::ICCBasedColorSpace(NonnullRefPtr<Gfx::ICC::Profile> profile)
    : m_profile(profile)
{
    m_map = sRGB()->matrix_matrix_conversion(profile);
}

PDFErrorOr<ColorOrStyle> ICCBasedColorSpace::style(ReadonlySpan<float> arguments) const
{
    if (m_profile->data_color_space() == Gfx::ICC::ColorSpace::CIELAB) {
        m_components.resize(arguments.size());
        for (size_t i = 0; i < arguments.size(); ++i) {
            float number = arguments[i];

            // CIELAB channels go from 0..100 and -128..127 instead of from 0..1.
            // FIXME: We should probably have an API on Gfx::ICC::Profile that takes floats instead of bytes and that does this internally instead.
            if (i == 0)
                number /= 100.0f;
            else
                number = (number + 128.0f) / 255.0f;

            m_components[i] = number;
        }
        arguments = m_components;
    }

    if (m_map.has_value())
        return m_map->map(FloatVector3 { arguments[0], arguments[1], arguments[2] });

    m_bytes.resize(arguments.size());
    for (size_t i = 0; i < arguments.size(); ++i)
        m_bytes[i] = static_cast<u8>(arguments[i] * 255.0f);

    auto pcs = TRY(m_profile->to_pcs(m_bytes));
    Array<u8, 3> output;
    TRY(sRGB()->from_pcs(m_profile, pcs, output.span()));

    return Color(output[0], output[1], output[2]);
}

int ICCBasedColorSpace::number_of_components() const
{
    return Gfx::ICC::number_of_components_in_color_space(m_profile->data_color_space());
}

Vector<float> ICCBasedColorSpace::default_decode() const
{
    auto color_space = m_profile->data_color_space();
    switch (color_space) {
    case Gfx::ICC::ColorSpace::Gray:
        return { 0.0, 1.0 };
    case Gfx::ICC::ColorSpace::RGB:
        return { 0.0, 1.0, 0.0, 1.0, 0.0, 1.0 };
    case Gfx::ICC::ColorSpace::CMYK:
        return { 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0 };
    default:
        warnln("PDF: Unknown default_decode params for color space {}", Gfx::ICC::data_color_space_name(color_space));
        Vector<float> decoding_ranges;
        for (u8 i = 0; i < Gfx::ICC::number_of_components_in_color_space(color_space); i++) {
            decoding_ranges.append(0.0);
            decoding_ranges.append(1.0);
        }
        return decoding_ranges;
    }
}

NonnullRefPtr<Gfx::ICC::Profile> ICCBasedColorSpace::sRGB()
{
    if (!s_srgb_profile)
        s_srgb_profile = MUST(Gfx::ICC::sRGB());
    return *s_srgb_profile;
}

PDFErrorOr<NonnullRefPtr<LabColorSpace>> LabColorSpace::create(Document* document, Vector<Value>&& parameters)
{
    if (parameters.size() != 1)
        return Error { Error::Type::MalformedPDF, "Lab color space expects one parameter" };

    auto dict = TRY(document->resolve_to<DictObject>(parameters[0]));
    if (!dict->contains(CommonNames::WhitePoint))
        return Error { Error::Type::MalformedPDF, "Lab color space expects a Whitepoint key" };

    auto white_point_array = TRY(dict->get_array(document, CommonNames::WhitePoint));
    if (white_point_array->size() != 3)
        return Error { Error::Type::MalformedPDF, "Lab color space expects 3 Whitepoint parameters" };

    auto color_space = adopt_ref(*new LabColorSpace());

    color_space->m_whitepoint[0] = white_point_array->at(0).to_float();
    color_space->m_whitepoint[1] = white_point_array->at(1).to_float();
    color_space->m_whitepoint[2] = white_point_array->at(2).to_float();

    if (color_space->m_whitepoint[1] != 1.0f)
        return Error { Error::Type::MalformedPDF, "Lab color space expects 2nd Whitepoint to be 1.0" };

    if (dict->contains(CommonNames::BlackPoint)) {
        auto black_point_array = TRY(dict->get_array(document, CommonNames::BlackPoint));
        if (black_point_array->size() == 3) {
            color_space->m_blackpoint[0] = black_point_array->at(0).to_float();
            color_space->m_blackpoint[1] = black_point_array->at(1).to_float();
            color_space->m_blackpoint[2] = black_point_array->at(2).to_float();
        }
    }

    if (dict->contains(CommonNames::Range)) {
        auto range_array = TRY(dict->get_array(document, CommonNames::Range));
        if (range_array->size() == 4) {
            color_space->m_range[0] = range_array->at(0).to_float();
            color_space->m_range[1] = range_array->at(1).to_float();
            color_space->m_range[2] = range_array->at(2).to_float();
            color_space->m_range[3] = range_array->at(3).to_float();
        }
    }

    return color_space;
}

PDFErrorOr<ColorOrStyle> LabColorSpace::style(ReadonlySpan<float> arguments) const
{
    VERIFY(arguments.size() == 3);
    auto L_star = clamp(arguments[0], 0.0f, 100.0f);
    auto a_star = clamp(arguments[1], m_range[0], m_range[1]);
    auto b_star = clamp(arguments[2], m_range[2], m_range[3]);

    auto L = (L_star + 16) / 116 + a_star / 500;
    auto M = (L_star + 16) / 116;
    auto N = (L_star + 16) / 116 - b_star / 200;

    auto g = [](float x) {
        if (x >= 6.0f / 29.0f)
            return powf(x, 3);
        return 108.0f / 841.0f * (x - 4.0f / 29.0f);
    };

    auto x = m_whitepoint[0] * g(L);
    auto y = m_whitepoint[1] * g(M);
    auto z = m_whitepoint[2] * g(N);

    auto flattened_xyz = flatten_and_normalize_whitepoint(m_whitepoint, { x, y, z });
    auto scaled_black_point_xyz = scale_black_point(m_blackpoint, flattened_xyz);
    auto d65_normalized = convert_to_d65(scaled_black_point_xyz);
    auto srgb = convert_to_srgb(d65_normalized);

    auto red = static_cast<u8>(clamp(srgb[0], 0.0f, 1.0f) * 255.0f);
    auto green = static_cast<u8>(clamp(srgb[1], 0.0f, 1.0f) * 255.0f);
    auto blue = static_cast<u8>(clamp(srgb[2], 0.0f, 1.0f) * 255.0f);

    return Color(red, green, blue);
}

Vector<float> LabColorSpace::default_decode() const
{
    return { 0.0f, 100.0f, m_range[0], m_range[1], m_range[2], m_range[3] };
}

PDFErrorOr<NonnullRefPtr<ColorSpace>> IndexedColorSpace::create(Document* document, Vector<Value>&& parameters, Renderer& renderer)
{
    if (parameters.size() != 3)
        return Error { Error::Type::MalformedPDF, "Indexed color space expected three parameters" };

    // "The base parameter is an array or name that identifies the base color space in which the values
    //  in the color table are to be interpreted. It can be any device or CIE-based color space or (in PDF 1.3)
    //  a Separation or DeviceN space, but not a Pattern space or another Indexed space."

    auto base_object = TRY(document->resolve_to<Object>(parameters[0]));
    auto base = TRY(ColorSpace::create(document, base_object, renderer));

    if (base->family() == ColorSpaceFamily::Pattern || base->family() == ColorSpaceFamily::Indexed)
        return Error { Error::Type::MalformedPDF, "Indexed color space has invalid base color space" };

    // "The hival parameter is an integer that specifies the maximum valid index value. In other words,
    // the color table is to be indexed by integers in the range 0 to hival. hival can be no greater than 255"
    auto hival = TRY(document->resolve_to<int>(parameters[1]));
    if (hival < 0 || hival > 255)
        return Error { Error::Type::MalformedPDF, "Indexed color space hival out of range" };

    // "The color table is defined by the lookup parameter, which can be either a stream or (in PDF 1.2) a byte string.
    //  It provides the mapping between index values and the corresponding colors in the base color space.
    //  The color table data must be m × (hival + 1) bytes long, where m is the number of color components in the
    //  base color space. Each byte is an unsigned integer in the range 0 to 255 that is scaled to the range of
    //  the corresponding color component in the base color space; that is, 0 corresponds to the minimum value
    //  in the range for that component, and 255 corresponds to the maximum."
    auto lookup_object = TRY(document->resolve_to<Object>(parameters[2]));

    Vector<u8> lookup;
    if (lookup_object->is<StreamObject>()) {
        lookup = Vector<u8> { lookup_object->cast<StreamObject>()->bytes() };
    } else if (lookup_object->is<StringObject>()) {
        // FIXME: Check if it's a hex string.
        auto const& string = lookup_object->cast<StringObject>()->string();
        lookup = Vector<u8> { ReadonlyBytes { string.characters(), string.length() } };
    } else {
        return Error { Error::Type::MalformedPDF, "Indexed color space expects stream or string for third arg" };
    }

    size_t needed_size = (hival + 1) * base->number_of_components();
    if (lookup.size() - 1 == needed_size) {
        // FIXME: Could do this if lookup.size() > needed_size generally, but so far I've only seen files that had one byte too much.
        lookup.resize(needed_size);
    }
    if (lookup.size() != needed_size) {
        dbgln("lookup size {} doesn't match hival {} and base components {}", lookup.size(), hival, base->number_of_components());
        return Error { Error::Type::MalformedPDF, "Indexed color space lookup table doesn't match size" };
    }

    auto color_space = adopt_ref(*new IndexedColorSpace(move(base)));
    color_space->m_hival = hival;
    color_space->m_lookup = move(lookup);
    return color_space;
}

IndexedColorSpace::IndexedColorSpace(NonnullRefPtr<ColorSpace> base)
    : m_base(move(base))
{
}

PDFErrorOr<ColorOrStyle> IndexedColorSpace::style(ReadonlySpan<float> arguments) const
{
    VERIFY(arguments.size() == 1);

    auto index = static_cast<int>(arguments[0]);
    if (index < 0 || index > m_hival)
        return Error { Error::Type::MalformedPDF, "Indexed color space index out of range" };

    Vector<Value, 4> components;
    size_t const n = m_base->number_of_components();
    for (size_t i = 0; i < n; ++i)
        TRY(components.try_append(Value(m_lookup[index * n + i] / 255.0f)));

    return m_base->style(components);
}

Vector<float> IndexedColorSpace::default_decode() const
{
    return { 0.0, 255.0 };
}

PDFErrorOr<NonnullRefPtr<SeparationColorSpace>> SeparationColorSpace::create(Document* document, Vector<Value>&& parameters, Renderer& renderer)
{
    if (parameters.size() != 3)
        return Error { Error::Type::MalformedPDF, "Separation color space expected three parameters" };

    // "The name parameter is a name object specifying the name of the colorant that this Separation color space
    //  is intended to represent (or one of the special names All or None; see below)"
    auto name_object = TRY(document->resolve_to<NameObject>(parameters[0]));
    auto name = name_object->cast<NameObject>()->name();

    // "The alternateSpace parameter must be an array or name object that identifies the alternate color space,
    //  which can be any device or CIE-based color space but not another special color space
    //  (Pattern, Indexed, Separation, or DeviceN)."
    auto alternate_space_object = TRY(document->resolve_to<Object>(parameters[1]));
    auto alternate_space = TRY(ColorSpace::create(document, alternate_space_object, renderer));

    auto family = alternate_space->family();
    if (family == ColorSpaceFamily::Pattern || family == ColorSpaceFamily::Indexed || family == ColorSpaceFamily::Separation || family == ColorSpaceFamily::DeviceN)
        return Error { Error::Type::MalformedPDF, "Separation color space has invalid alternate color space" };

    // "The tintTransform parameter must be a function"
    auto tint_transform_object = TRY(document->resolve_to<Object>(parameters[2]));
    auto tint_transform = TRY(Function::create(document, tint_transform_object));

    auto color_space = adopt_ref(*new SeparationColorSpace(move(alternate_space), move(tint_transform)));
    color_space->m_name = move(name);
    return color_space;
}

SeparationColorSpace::SeparationColorSpace(NonnullRefPtr<ColorSpace> alternate_space, NonnullRefPtr<Function> tint_transform)
    : m_alternate_space(move(alternate_space))
    , m_tint_transform(move(tint_transform))
{
}

PDFErrorOr<ColorOrStyle> SeparationColorSpace::style(ReadonlySpan<float> arguments) const
{
    // "For an additive device such as a computer display, a Separation color space never applies a process colorant directly;
    //  it always reverts to the alternate color space as described below."
    // "During subsequent painting operations, an application calls [the tint] function to transform a tint value into
    //  color component values in the alternate color space."
    // FIXME: Does this need handling for the special colorant names "All" and "None"?
    // FIXME: When drawing to a printer, do something else.
    VERIFY(arguments.size() == 1);
    auto a = arguments[0];

    auto tint_output = TRY(m_tint_transform->evaluate(ReadonlySpan<float> { &a, 1 }));

    m_tint_output_values.resize(tint_output.size());
    for (size_t i = 0; i < tint_output.size(); ++i)
        m_tint_output_values[i] = tint_output[i];

    return m_alternate_space->style(m_tint_output_values);
}

Vector<float> SeparationColorSpace::default_decode() const
{
    return { 0.0f, 1.0f };
}
}
