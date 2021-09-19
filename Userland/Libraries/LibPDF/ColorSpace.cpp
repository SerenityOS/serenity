/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/ColorSpace.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/ObjectDerivatives.h>

namespace PDF {

RefPtr<DeviceGrayColorSpace> DeviceGrayColorSpace::the()
{
    static auto instance = adopt_ref(*new DeviceGrayColorSpace());
    return instance;
}

Color DeviceGrayColorSpace::color(Vector<Value> const& arguments) const
{
    VERIFY(arguments.size() == 1);
    auto gray = static_cast<u8>(arguments[0].to_float() * 255.0f);
    return Color(gray, gray, gray);
}

RefPtr<DeviceRGBColorSpace> DeviceRGBColorSpace::the()
{
    static auto instance = adopt_ref(*new DeviceRGBColorSpace());
    return instance;
}

Color DeviceRGBColorSpace::color(Vector<Value> const& arguments) const
{
    VERIFY(arguments.size() == 3);
    auto r = static_cast<u8>(arguments[0].to_float() * 255.0f);
    auto g = static_cast<u8>(arguments[1].to_float() * 255.0f);
    auto b = static_cast<u8>(arguments[2].to_float() * 255.0f);
    return Color(r, g, b);
}

RefPtr<DeviceCMYKColorSpace> DeviceCMYKColorSpace::the()
{
    static auto instance = adopt_ref(*new DeviceCMYKColorSpace());
    return instance;
}

Color DeviceCMYKColorSpace::color(Vector<Value> const& arguments) const
{
    VERIFY(arguments.size() == 4);
    auto c = arguments[0].to_float();
    auto m = arguments[1].to_float();
    auto y = arguments[2].to_float();
    auto k = arguments[3].to_float();
    return Color::from_cmyk(c, m, y, k);
}

RefPtr<CalRGBColorSpace> CalRGBColorSpace::create(RefPtr<Document> document, Vector<Value>&& parameters)
{
    if (parameters.size() != 1)
        return {};

    auto param = parameters[0];
    if (!param.has<NonnullRefPtr<Object>>() || !param.get<NonnullRefPtr<Object>>()->is_dict())
        return {};

    auto dict = object_cast<DictObject>(param.get<NonnullRefPtr<Object>>());
    if (!dict->contains(CommonNames::WhitePoint))
        return {};

    auto white_point_array = dict->get_array(document, CommonNames::WhitePoint);
    if (white_point_array->size() != 3)
        return {};

    auto color_space = adopt_ref(*new CalRGBColorSpace());

    color_space->m_whitepoint[0] = white_point_array->at(0).to_float();
    color_space->m_whitepoint[1] = white_point_array->at(1).to_float();
    color_space->m_whitepoint[2] = white_point_array->at(2).to_float();

    if (color_space->m_whitepoint[1] != 1.0f)
        return {};

    if (dict->contains(CommonNames::BlackPoint)) {
        auto black_point_array = dict->get_array(document, CommonNames::BlackPoint);
        if (black_point_array->size() == 3) {
            color_space->m_blackpoint[0] = black_point_array->at(0).to_float();
            color_space->m_blackpoint[1] = black_point_array->at(1).to_float();
            color_space->m_blackpoint[2] = black_point_array->at(2).to_float();
        }
    }

    if (dict->contains(CommonNames::Gamma)) {
        auto gamma_array = dict->get_array(document, CommonNames::Gamma);
        if (gamma_array->size() == 3) {
            color_space->m_gamma[0] = gamma_array->at(0).to_float();
            color_space->m_gamma[1] = gamma_array->at(1).to_float();
            color_space->m_gamma[2] = gamma_array->at(2).to_float();
        }
    }

    if (dict->contains(CommonNames::Matrix)) {
        auto matrix_array = dict->get_array(document, CommonNames::Matrix);
        if (matrix_array->size() == 3) {
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

constexpr Array<float, 3> matrix_multiply(Array<float, 9> a, Array<float, 3> b)
{
    return Array<float, 3> {
        a[0] * b[0] + a[1] * b[1] + a[2] * b[2],
        a[3] * b[0] + a[4] * b[1] + a[5] * b[2],
        a[6] * b[0] + a[7] * b[1] + a[8] * b[2]
    };
}

// Converts to a flat XYZ space with white point = (1, 1, 1)
// Step 2 of https://www.adobe.com/content/dam/acom/en/devnet/photoshop/sdk/AdobeBPC.pdf
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
constexpr Array<float, 3> convert_to_d65(Array<float, 3> whitepoint, Array<float, 3> xyz)
{
    constexpr float d65x = 0.95047f;
    constexpr float d65y = 1.0f;
    constexpr float d65z = 1.08883f;

    return {
        (xyz[0] * d65x) / whitepoint[0],
        (xyz[1] * d65y) / whitepoint[1],
        (xyz[2] * d65z) / whitepoint[2],
    };
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

    return matrix_multiply(conversion_matrix, xyz);
}

Color CalRGBColorSpace::color(Vector<Value> const& arguments) const
{
    VERIFY(arguments.size() == 3);
    auto a = clamp(arguments[0].to_float(), 0.0f, 1.0f);
    auto b = clamp(arguments[1].to_float(), 0.0f, 1.0f);
    auto c = clamp(arguments[2].to_float(), 0.0f, 1.0f);

    auto agr = powf(a, m_gamma[0]);
    auto bgg = powf(b, m_gamma[1]);
    auto cgb = powf(c, m_gamma[2]);

    auto x = m_matrix[0] * agr + m_matrix[3] * bgg + m_matrix[6] * cgb;
    auto y = m_matrix[1] * agr + m_matrix[4] * bgg + m_matrix[7] * cgb;
    auto z = m_matrix[2] * agr + m_matrix[5] * bgg + m_matrix[8] * cgb;

    auto flattened_xyz = flatten_and_normalize_whitepoint(m_whitepoint, { x, y, z });
    auto scaled_black_point_xyz = scale_black_point(m_blackpoint, flattened_xyz);
    auto d65_normalized = convert_to_d65(m_whitepoint, scaled_black_point_xyz);
    auto srgb = convert_to_srgb(d65_normalized);

    auto red = static_cast<u8>(srgb[0] * 255.0f);
    auto green = static_cast<u8>(srgb[1] * 255.0f);
    auto blue = static_cast<u8>(srgb[2] * 255.0f);

    return Color(red, green, blue);
}

}
