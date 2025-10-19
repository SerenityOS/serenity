/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/Enumerate.h>
#include <AK/GenericShorthands.h>
#include <LibGfx/EdgeFlagPathRasterizer.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Vector2.h>
#include <LibPDF/ColorSpace.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Shading.h>

namespace PDF {

namespace {

// TABLE 4.28 Entries common to all shading dictionaries
struct CommonEntries {
    // "(Required) The color space in which color values are expressed. This may be
    //  any device, CIE-based, or special color space except a Pattern space."
    AK::NonnullRefPtr<ColorSpaceWithFloatArgs> color_space;

    // "(Optional) An array of color components appropriate to the color space,
    //  specifying a single background color value. If present, this color is used, be-
    //  fore any painting operation involving the shading, to fill those portions of the
    //  area to be painted that lie outside the bounds of the shading object
    //  Note: The background color is applied only when the shading is used as part of
    //  a shading pattern, not when it is painted directly with the sh operator."
    // We currently don't support shading patterns yet, so we don't use this yet.
    Optional<Vector<float>> background {};

    // "(Optional) An array of four numbers giving the left, bottom, right, and top
    //  coordinates, respectively, of the shading’s bounding box. The coordinates are
    //  interpreted in the shading’s target coordinate space. If present, this bounding
    //  box is applied as a temporary clipping boundary when the shading is painted,
    //  in addition to the current clipping path and any other clipping boundaries in
    //  effect at that time."
    Optional<Gfx::FloatRect> b_box {};

    // "(Optional) A flag indicating whether to filter the shading function to prevent
    //  aliasing artifacts. [...] Anti-aliasing
    //  may not be implemented on some output devices, in which case this flag is
    //  ignored. Default value: false."
    // We currently ignore this.
    bool anti_alias { false };
};

PDFErrorOr<CommonEntries> read_common_entries(Document* document, DictObject const& shading_dict, Renderer& renderer)
{
    // "(Required) The color space in which color values are expressed. This may be
    //  any device, CIE-based, or special color space except a Pattern space. See
    //  “Color Space: Special Considerations” on page 306 for further information."
    auto color_space_object = TRY(shading_dict.get_object(document, CommonNames::ColorSpace));
    auto color_space = TRY(ColorSpace::create(document, move(color_space_object), renderer));
    if (color_space->family() == ColorSpaceFamily::Pattern)
        return Error::malformed_error("Shading color space must not be pattern");

    CommonEntries common_entries { .color_space = verify_cast<ColorSpaceWithFloatArgs>(*color_space) };

    if (shading_dict.contains(CommonNames::Background)) {
        auto background_array = TRY(shading_dict.get_array(document, CommonNames::Background));
        Vector<float> background;
        for (auto const& value : background_array->elements())
            background.append(value.to_float());
        common_entries.background = move(background);
    }

    if (shading_dict.contains(CommonNames::BBox)) {
        auto bbox_array = TRY(shading_dict.get_array(document, CommonNames::BBox));
        if (bbox_array->size() != 4)
            return Error::malformed_error("BBox must have 4 elements");
        auto bbox = Gfx::FloatRect::from_two_points(
            { bbox_array->at(0).to_float(), bbox_array->at(1).to_float() },
            { bbox_array->at(2).to_float(), bbox_array->at(3).to_float() });
        common_entries.b_box = bbox;
    }

    if (shading_dict.contains(CommonNames::AntiAlias))
        common_entries.anti_alias = TRY(document->resolve(shading_dict.get_value(CommonNames::AntiAlias))).get<bool>();

    return common_entries;
}

using ShadingFunctionsType = Variant<Empty, NonnullRefPtr<Function>, Vector<NonnullRefPtr<Function>>>;
using NonemptyShadingFunctionsType = Variant<NonnullRefPtr<Function>, Vector<NonnullRefPtr<Function>>>;

static PDFErrorOr<NonemptyShadingFunctionsType> read_shading_functions(Document* document, NonnullRefPtr<DictObject> shading_dict, NonnullRefPtr<ColorSpace> color_space, ReadonlySpan<float> function_input)
{
    if (color_space->family() == ColorSpaceFamily::Indexed)
        return Error::malformed_error("Function cannot be used with Indexed color space");

    auto function_object = TRY(shading_dict->get_object(document, CommonNames::Function));
    if (function_object->is<ArrayObject>()) {
        auto function_array = function_object->cast<ArrayObject>();
        Vector<NonnullRefPtr<Function>> functions_vector;
        if (function_array->size() != static_cast<size_t>(color_space->number_of_components()))
            return Error::malformed_error("Function array must have as many elements as color space has components");
        for (size_t i = 0; i < function_array->size(); ++i) {
            auto function = TRY(Function::create(document, TRY(document->resolve_to<Object>(function_array->at(i)))));
            if (TRY(function->evaluate(function_input)).size() != 1)
                return Error::malformed_error("Function must have 1 output component");
            TRY(functions_vector.try_append(move(function)));
        }
        return functions_vector;
    }
    auto function = TRY(Function::create(document, function_object));
    if (TRY(function->evaluate(function_input)).size() != static_cast<size_t>(color_space->number_of_components()))
        return Error::malformed_error("Function must have as many output components as color space");
    return function;
}

static PDFErrorOr<NonemptyShadingFunctionsType> read_shading_functions(Document* document, NonnullRefPtr<DictObject> shading_dict, NonnullRefPtr<ColorSpace> color_space, float function_input)
{
    return read_shading_functions(document, shading_dict, color_space, Array { function_input });
}

class FunctionBasedShading final : public Shading {
public:
    static PDFErrorOr<NonnullRefPtr<FunctionBasedShading>> create(Document*, NonnullRefPtr<DictObject>, CommonEntries);

    virtual Optional<Gfx::FloatRect> bounding_box() const override { return m_common_entries.b_box; }
    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    FunctionBasedShading(CommonEntries common_entries, Gfx::FloatRect domain, Gfx::AffineTransform matrix, NonemptyShadingFunctionsType functions)
        : m_common_entries(move(common_entries))
        , m_domain(domain)
        , m_matrix(matrix)
        , m_functions(move(functions))
    {
    }

    CommonEntries m_common_entries;
    Gfx::FloatRect m_domain;
    Gfx::AffineTransform m_matrix;
    NonemptyShadingFunctionsType m_functions;
};

PDFErrorOr<NonnullRefPtr<FunctionBasedShading>> FunctionBasedShading::create(Document* document, NonnullRefPtr<DictObject> shading_dict, CommonEntries common_entries)
{
    // TABLE 4.29 Additional entries specific to a type 1 shading dictionary

    // "(Optional) An array of four numbers [ xmin xmax ymin ymax ] specifying the
    //  rectangular domain of coordinates over which the color function(s) are defined.
    //  Default value: [ 0.0 1.0 0.0 1.0 ]."
    Gfx::FloatRect domain { 0.0f, 0.0f, 1.0f, 1.0f };
    if (shading_dict->contains(CommonNames::Domain)) {
        auto domain_array = TRY(shading_dict->get_array(document, CommonNames::Domain));
        if (domain_array->size() != 4)
            return Error::malformed_error("Domain must have 4 elements");
        float xmin = domain_array->at(0).to_float();
        float xmax = domain_array->at(1).to_float();
        float ymin = domain_array->at(2).to_float();
        float ymax = domain_array->at(3).to_float();
        domain = Gfx::FloatRect::from_two_points({ xmin, ymin }, { xmax, ymax });
    }

    // "(Optional) An array of six numbers specifying a transformation matrix mapping
    //  the coordinate space specified by the Domain entry into the shading’s target co-
    //  ordinate space. For example, to map the domain rectangle [ 0.0 1.0 0.0 1.0 ] to a
    //  1-inch square with lower-left corner at coordinates (100, 100) in default user
    //  space, the Matrix value would be [ 72 0 0 72 100 100 ]. Default value: the iden-
    //  tity matrix [ 1 0 0 1 0 0 ]."
    Gfx::AffineTransform matrix;
    if (shading_dict->contains(CommonNames::Matrix)) {
        auto matrix_array = TRY(shading_dict->get_array(document, CommonNames::Matrix));
        if (matrix_array->size() != 6)
            return Error::malformed_error("Matrix must have 6 elements");
        matrix = Gfx::AffineTransform {
            matrix_array->at(0).to_float(),
            matrix_array->at(1).to_float(),
            matrix_array->at(2).to_float(),
            matrix_array->at(3).to_float(),
            matrix_array->at(4).to_float(),
            matrix_array->at(5).to_float(),
        };
    }

    // "(Required) A 2-in, n-out function or an array of n 2-in, 1-out functions (where n
    //  is the number of color components in the shading dictionary’s color space). Each
    //  function’s domain must be a superset of that of the shading dictionary. If the val-
    //  ue returned by the function for a given color component is out of range, it is ad-
    //  justed to the nearest valid value."
    auto functions = TRY(read_shading_functions(document, shading_dict, common_entries.color_space, Array { domain.x(), domain.y() }));

    return adopt_ref(*new FunctionBasedShading(move(common_entries), domain, matrix, move(functions)));
}

PDFErrorOr<void> FunctionBasedShading::draw(Gfx::Painter& painter, Gfx::AffineTransform const& ctm)
{
    auto maybe_inverse_ctm = ctm.inverse();
    if (!maybe_inverse_ctm.has_value())
        return {};
    auto inverse_ctm = maybe_inverse_ctm.value();

    auto& bitmap = painter.target();

    auto scale = painter.scale();
    auto clip_rect = painter.clip_rect() * scale;

    Vector<float, 4> color_components;
    color_components.resize(m_common_entries.color_space->number_of_components());

    auto maybe_to_domain = m_matrix.inverse();
    if (!maybe_to_domain.has_value())
        return Error::malformed_error("Matrix is not invertible");
    auto to_domain = maybe_to_domain.value();

    for (int y = clip_rect.top(); y < clip_rect.bottom(); ++y) {
        for (int x = clip_rect.left(); x < clip_rect.right(); ++x) {
            Gfx::FloatPoint shading_point = inverse_ctm.map(Gfx::FloatPoint { x, y } / scale);
            auto domain_point = to_domain.map(shading_point);
            if (!m_domain.contains(domain_point))
                continue;

            TRY(m_functions.visit(
                [&](Function const& function) -> PDFErrorOr<void> {
                    auto result = TRY(function.evaluate(to_array({ domain_point.x(), domain_point.y() })));
                    result.copy_to(color_components);
                    return {};
                },
                [&](Vector<NonnullRefPtr<Function>> const& functions) -> PDFErrorOr<void> {
                    for (size_t i = 0; i < functions.size(); ++i) {
                        auto result = TRY(functions[i]->evaluate(to_array({ domain_point.x(), domain_point.y() })));
                        color_components[i] = result[0];
                    }
                    return {};
                }));

            auto color = TRY(m_common_entries.color_space->style(color_components));
            bitmap.scanline(y)[x] = color.get<Gfx::Color>().value();
        }
    }

    return {};
}

class AxialShading final : public Shading {
public:
    static PDFErrorOr<NonnullRefPtr<AxialShading>> create(Document*, NonnullRefPtr<DictObject>, CommonEntries);

    virtual Optional<Gfx::FloatRect> bounding_box() const override { return m_common_entries.b_box; }
    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    AxialShading(CommonEntries common_entries, Gfx::FloatPoint start, Gfx::FloatPoint end, float t0, float t1, NonemptyShadingFunctionsType functions, bool extend_start, bool extend_end)
        : m_common_entries(move(common_entries))
        , m_start(start)
        , m_end(end)
        , m_t0(t0)
        , m_t1(t1)
        , m_functions(move(functions))
        , m_extend_start(extend_start)
        , m_extend_end(extend_end)
    {
    }

    CommonEntries m_common_entries;
    Gfx::FloatPoint m_start;
    Gfx::FloatPoint m_end;
    float m_t0 { 0.0f };
    float m_t1 { 1.0f };
    NonemptyShadingFunctionsType m_functions;
    bool m_extend_start { false };
    bool m_extend_end { false };
};

PDFErrorOr<NonnullRefPtr<AxialShading>> AxialShading::create(Document* document, NonnullRefPtr<DictObject> shading_dict, CommonEntries common_entries)
{
    // TABLE 4.30 Additional entries specific to a type 2 shading dictionary
    // "(Required) An array of four numbers [ x0 y0 x1 y1 ] specifying the starting and
    //  ending coordinates of the axis, expressed in the shading’s target coordinate
    //  space."
    auto coords = TRY(shading_dict->get_array(document, CommonNames::Coords));
    if (coords->size() != 4)
        return Error::malformed_error("Coords must have 4 elements");
    Gfx::FloatPoint start { coords->at(0).to_float(), coords->at(1).to_float() };
    Gfx::FloatPoint end { coords->at(2).to_float(), coords->at(3).to_float() };

    // "(Optional) An array of two numbers [ t0 t1 ] specifying the limiting values of a
    //  parametric variable t. The variable is considered to vary linearly between these
    //  two values as the color gradient varies between the starting and ending points of
    //  the axis. The variable t becomes the input argument to the color function(s). De-
    //  fault value: [ 0.0 1.0 ]."
    float t0 = 0.0f;
    float t1 = 1.0f;
    if (shading_dict->contains(CommonNames::Domain)) {
        auto domain_array = TRY(shading_dict->get_array(document, CommonNames::Domain));
        if (domain_array->size() != 2)
            return Error::malformed_error("Domain must have 2 elements");
        t0 = domain_array->at(0).to_float();
        t1 = domain_array->at(1).to_float();
    }

    // "(Required) A 1-in, n-out function or an array of n 1-in, 1-out functions (where n
    //  is the number of color components in the shading dictionary’s color space). The
    //  function(s) are called with values of the parametric variable t in the domain de-
    //  fined by the Domain entry. Each function’s domain must be a superset of that of
    //  the shading dictionary. If the value returned by the function for a given color
    //  component is out of range, it is adjusted to the nearest valid value."
    auto functions = TRY(read_shading_functions(document, shading_dict, common_entries.color_space, t0));

    // "(Optional) An array of two boolean values specifying whether to extend the
    //  shading beyond the starting and ending points of the axis, respectively. Default
    //  value: [ false false ]."
    bool extend_start = false;
    bool extend_end = false;
    if (shading_dict->contains(CommonNames::Extend)) {
        auto extend_array = TRY(shading_dict->get_array(document, CommonNames::Extend));
        if (extend_array->size() != 2)
            return Error::malformed_error("Extend must have 2 elements");
        extend_start = extend_array->at(0).get<bool>();
        extend_end = extend_array->at(1).get<bool>();
    }

    return adopt_ref(*new AxialShading(move(common_entries), start, end, t0, t1, move(functions), extend_start, extend_end));
}

PDFErrorOr<void> AxialShading::draw(Gfx::Painter& painter, Gfx::AffineTransform const& ctm)
{
    auto maybe_inverse_ctm = ctm.inverse();
    if (!maybe_inverse_ctm.has_value())
        return {};
    auto inverse_ctm = maybe_inverse_ctm.value();

    auto& bitmap = painter.target();

    auto scale = painter.scale();
    auto clip_rect = painter.clip_rect() * scale;

    Vector<float, 4> color_components;
    color_components.resize(m_common_entries.color_space->number_of_components());

    for (int y = clip_rect.top(); y < clip_rect.bottom(); ++y) {
        for (int x = clip_rect.left(); x < clip_rect.right(); ++x) {
            Gfx::FloatPoint pdf = inverse_ctm.map(Gfx::FloatPoint { x, y } / scale);

            // FIXME: Normalize m_end to have unit length from m_start.
            Gfx::FloatVector2 to_point { pdf.x() - m_start.x(), pdf.y() - m_start.y() };
            Gfx::FloatVector2 to_end { m_end.x() - m_start.x(), m_end.y() - m_start.y() };
            float x_prime = to_point.dot(to_end) / to_end.dot(to_end);

            float t;
            if (0 <= x_prime && x_prime <= 1)
                t = m_t0 + (m_t1 - m_t0) * x_prime;
            else if (x_prime < 0) {
                if (!m_extend_start)
                    continue;
                t = m_t0;
            } else {
                if (!m_extend_end)
                    continue;
                t = m_t1;
            }

            TRY(m_functions.visit(
                [&](Function const& function) -> PDFErrorOr<void> {
                    auto result = TRY(function.evaluate(to_array({ t })));
                    result.copy_to(color_components);
                    return {};
                },
                [&](Vector<NonnullRefPtr<Function>> const& functions) -> PDFErrorOr<void> {
                    for (size_t i = 0; i < functions.size(); ++i) {
                        auto result = TRY(functions[i]->evaluate(to_array({ t })));
                        color_components[i] = result[0];
                    }
                    return {};
                }));

            auto color = TRY(m_common_entries.color_space->style(color_components));
            bitmap.scanline(y)[x] = color.get<Gfx::Color>().value();
        }
    }

    return {};
}

class RadialShading final : public Shading {
public:
    static PDFErrorOr<NonnullRefPtr<RadialShading>> create(Document*, NonnullRefPtr<DictObject>, CommonEntries);

    virtual Optional<Gfx::FloatRect> bounding_box() const override { return m_common_entries.b_box; }
    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    RadialShading(CommonEntries common_entries, Gfx::FloatPoint start, float start_radius, Gfx::FloatPoint end, float end_radius, float t0, float t1, NonemptyShadingFunctionsType functions, bool extend_start, bool extend_end)
        : m_common_entries(move(common_entries))
        , m_start(start)
        , m_start_radius(start_radius)
        , m_end(end)
        , m_end_radius(end_radius)
        , m_t0(t0)
        , m_t1(t1)
        , m_functions(move(functions))
        , m_extend_start(extend_start)
        , m_extend_end(extend_end)
    {
    }

    CommonEntries m_common_entries;
    Gfx::FloatPoint m_start;
    float m_start_radius { 0.0f };
    Gfx::FloatPoint m_end;
    float m_end_radius { 0.0f };
    float m_t0 { 0.0f };
    float m_t1 { 1.0f };
    NonemptyShadingFunctionsType m_functions;
    bool m_extend_start { false };
    bool m_extend_end { false };
};

PDFErrorOr<NonnullRefPtr<RadialShading>> RadialShading::create(Document* document, NonnullRefPtr<DictObject> shading_dict, CommonEntries common_entries)
{
    // TABLE 4.31 Additional entries specific to a type 3 shading dictionary
    // "(Required) An array of six numbers [ x0 y0 r0 x1 y1 r1 ] specifying the centers and
    //  radii of the starting and ending circles, expressed in the shading’s target coor-
    //  dinate space. The radii r0 and r1 must both be greater than or equal to 0. If one
    //  radius is 0, the corresponding circle is treated as a point; if both are 0, nothing is
    //  painted."
    auto coords = TRY(shading_dict->get_array(document, CommonNames::Coords));
    if (coords->size() != 6)
        return Error::malformed_error("Coords must have 6 elements");
    Gfx::FloatPoint start { coords->at(0).to_float(), coords->at(1).to_float() };
    float start_radius = coords->at(2).to_float();
    Gfx::FloatPoint end { coords->at(3).to_float(), coords->at(4).to_float() };
    float end_radius = coords->at(5).to_float();

    // "(Optional) An array of two numbers [ t0 t1 ] specifying the limiting values of a
    //  parametric variable t. The variable is considered to vary linearly between these
    //  two values as the color gradient varies between the starting and ending circles.
    //  The variable t becomes the input argument to the color function(s). Default
    //  value: [ 0.0 1.0 ]."
    float t0 = 0.0f;
    float t1 = 1.0f;
    if (shading_dict->contains(CommonNames::Domain)) {
        auto domain_array = TRY(shading_dict->get_array(document, CommonNames::Domain));
        if (domain_array->size() != 2)
            return Error::malformed_error("Domain must have 2 elements");
        t0 = domain_array->at(0).to_float();
        t1 = domain_array->at(1).to_float();
    }

    // "(Required) A 1-in, n-out function or an array of n 1-in, 1-out functions (where n
    //  is the number of color components in the shading dictionary’s color space). The
    //  function(s) are called with values of the parametric variable t in the domain de-
    //  fined by the shading dictionary’s Domain entry. Each function’s domain must be
    //  a superset of that of the shading dictionary. If the value returned by the function
    //  for a given color component is out of range, it is adjusted to the nearest valid val-
    //  ue."
    auto functions = TRY(read_shading_functions(document, shading_dict, common_entries.color_space, t0));

    // "(Optional) An array of two boolean values specifying whether to extend the
    //  shading beyond the starting and ending circles, respectively. Default value:
    //  [ false false ]."
    bool extend_start = false;
    bool extend_end = false;
    if (shading_dict->contains(CommonNames::Extend)) {
        auto extend_array = TRY(shading_dict->get_array(document, CommonNames::Extend));
        if (extend_array->size() != 2)
            return Error::malformed_error("Extend must have 2 elements");
        extend_start = extend_array->at(0).get<bool>();
        extend_end = extend_array->at(1).get<bool>();
    }

    return adopt_ref(*new RadialShading(move(common_entries), start, start_radius, end, end_radius, t0, t1, move(functions), extend_start, extend_end));
}

PDFErrorOr<void> RadialShading::draw(Gfx::Painter& painter, Gfx::AffineTransform const& ctm)
{
    auto maybe_inverse_ctm = ctm.inverse();
    if (!maybe_inverse_ctm.has_value())
        return {};
    auto inverse_ctm = maybe_inverse_ctm.value();

    auto& bitmap = painter.target();

    auto scale = painter.scale();
    auto clip_rect = painter.clip_rect() * scale;

    Vector<float, 4> color_components;
    color_components.resize(m_common_entries.color_space->number_of_components());

    // FIXME: Use smaller box if the circles are nested and the outer circle is not extended.

    for (int y = clip_rect.top(); y < clip_rect.bottom(); ++y) {
        for (int x = clip_rect.left(); x < clip_rect.right(); ++x) {
            Gfx::FloatPoint point = inverse_ctm.map(Gfx::FloatPoint { x, y } / scale);

            // The spec explains how to get a point given s. We want to solve the inverse problem:
            // The current pixel is at p. We want to find the s where (c(s) - p)^2 = r(s)^2 (eq 1).
            // Per spec, the circle depending on s has its center at
            //
            //     c(s) = c0 + s * (c1 - c0)
            //
            // and a radius of
            //
            //     r(s) = r0 + s * (r1 - r0)
            //
            // Putting that into (eq 1):
            //
            //     (c0 + s * (c1 - c0) - p)^2 = (r0 + s * (r1 - r0))^2
            //
            // Rearranging terms, we get a quadratic equation in s:
            //
            //     A * s^2 + B * s + C = 0
            //
            // with:
            //
            //     A = (c1 - c0)^2 - (r1 - r0)^2
            //     B = -2 * ((c1 - c0) * (p - c0) + (r1 - r0) * r0)
            //     C = (c0 - p)^2 - r0^2
            //
            // When both circles touch in one point, A = 0 and we get a linear equation instead.

            // FIXME: Normalize m_end to have unit length from m_start.
            Gfx::FloatVector2 to_point { point.x() - m_start.x(), point.y() - m_start.y() };
            Gfx::FloatVector2 to_end { m_end.x() - m_start.x(), m_end.y() - m_start.y() };
            float dr = m_end_radius - m_start_radius;

            float A = to_end.dot(to_end) - dr * dr;
            float B = -2 * (to_end.dot(to_point) + dr * m_start_radius);
            float C = to_point.dot(to_point) - m_start_radius * m_start_radius;
            float s_0;
            float s_1;
            if (A != 0) {
                float discriminant = B * B - 4 * A * C;
                if (discriminant < 0)
                    continue;

                s_0 = (-B + sqrt(discriminant)) / (2 * A);
                s_1 = (-B - sqrt(discriminant)) / (2 * A);
                if (A < 0)
                    swap(s_0, s_1);
            } else {
                // Linear case: B * s + C = 0
                s_0 = -C / B;
                s_1 = s_0;
            }

            float s;
            if (to_end.length() < max(m_start_radius, m_end_radius) - min(m_start_radius, m_end_radius)) {
                // One circle is inside the other one.
                // One of s_0, s_1 will be 0..1 in the main gradient part, and the other one will be negative in the whole circle.
                s = m_start_radius < m_end_radius ? s_0 : s_1;
                if (s < 0) {
                    if (!m_extend_start)
                        continue;
                    s = 0;
                } else if (s > 1) {
                    if (!m_extend_end)
                        continue;
                    s = 1;
                }
            } else {
                // Two disjoint or overlapping circles. Assuming the start circle is to the left of the end circle,
                // s_0 is the value of s when the left side of the circle touches the current point, while s_1 is the value
                // of s when the right side of the circle touches the current point. The forward formulation in the spec
                // says we're drawing the circles in increasing order of s, so the s_0 value is the one that draws on
                // top for the points drawn by both edges.
                // s_0 is in [0..1] in the start circle up to outside of the end circle (where it's > 1).
                // s_1 is in [0..1] in the end circle up to outside of the start circle (where it's < 0).
                s = s_0 >= 0 && s_0 <= 1 ? s_0 : s_1;

                if (m_extend_start) {
                    if (m_start_radius <= m_end_radius && s < -m_start_radius / dr)
                        continue;
                    if (s < 0)
                        s = 0;
                } else {
                    if (s < 0 && !(m_extend_end && s_0 > 0))
                        continue;
                }

                if (m_extend_end) {
                    if (m_start_radius > m_end_radius && s > -m_start_radius / dr)
                        continue;
                    if (s_0 > 1)
                        s = 1;
                } else {
                    if (s > 1)
                        continue;
                }
            }

            float t = m_t0 + s * (m_t1 - m_t0);

            TRY(m_functions.visit(
                [&](Function const& function) -> PDFErrorOr<void> {
                    auto result = TRY(function.evaluate(to_array({ t })));
                    result.copy_to(color_components);
                    return {};
                },
                [&](Vector<NonnullRefPtr<Function>> const& functions) -> PDFErrorOr<void> {
                    for (size_t i = 0; i < functions.size(); ++i) {
                        auto result = TRY(functions[i]->evaluate(to_array({ t })));
                        color_components[i] = result[0];
                    }
                    return {};
                }));

            auto color = TRY(m_common_entries.color_space->style(color_components));
            bitmap.scanline(y)[x] = color.get<Gfx::Color>().value();
        }
    }

    return {};
}

using GouraudColor = Vector<float, 4>;

struct GouraudBounds {
    GouraudColor min;
    GouraudColor max;
};

static GouraudBounds bounds_from_decode_array(ReadonlySpan<float> decode_array)
{
    VERIFY(decode_array.size() % 2 == 0);
    size_t number_of_components = decode_array.size() / 2;
    GouraudBounds bounds;
    bounds.min.resize(number_of_components);
    bounds.max.resize(number_of_components);
    for (size_t i = 0; i < number_of_components; ++i) {
        bounds.min[i] = decode_array[i * 2];
        bounds.max[i] = decode_array[i * 2 + 1];
    }
    return bounds;
}

class GouraudPaintStyle final : public Gfx::PaintStyle {
public:
    static NonnullRefPtr<GouraudPaintStyle> create(NonnullRefPtr<ColorSpaceWithFloatArgs> color_space, ShadingFunctionsType functions, Array<Gfx::FloatPoint, 3> points, Array<GouraudColor, 3> colors, GouraudBounds bounds)
    {
        return adopt_ref(*new GouraudPaintStyle(move(color_space), move(functions), move(points), move(colors), move(bounds)));
    }

    // We can't override sample_color() because it doesn't receive a useful origin.
    // Instead, override `paint()` and pass the origin to similar function.
    // FIXME: Try changing the signature of sample_color() to receive the actual origin.
    virtual void paint(Gfx::IntRect physical_bounding_box, PaintFunction paint) const override
    {
        paint([this, physical_bounding_box](Gfx::IntPoint point) { return sample_color_in_bbox(physical_bounding_box.location() + point); });
    }

private:
    GouraudPaintStyle(NonnullRefPtr<ColorSpaceWithFloatArgs> color_space, ShadingFunctionsType functions, Array<Gfx::FloatPoint, 3> points, Array<GouraudColor, 3> colors, GouraudBounds bounds)
        : m_functions(move(functions))
        , m_color_space(move(color_space))
        , m_points(move(points))
        , m_colors(move(colors))
        , m_bounds(move(bounds))
    {
    }

    Gfx::Color sample_color_in_bbox(Gfx::IntPoint) const;

    ShadingFunctionsType m_functions;
    NonnullRefPtr<ColorSpaceWithFloatArgs> m_color_space;
    Array<Gfx::FloatPoint, 3> m_points;
    Array<GouraudColor, 3> m_colors;
    GouraudBounds m_bounds;
};

Gfx::Color GouraudPaintStyle::sample_color_in_bbox(Gfx::IntPoint point_in_bbox) const
{
    auto signed_area = [](Gfx::FloatPoint a, Gfx::FloatPoint b, Gfx::FloatPoint c) {
        return (a.x() - c.x()) * (b.y() - c.y()) - (b.x() - c.x()) * (a.y() - c.y());
    };

    auto point = Gfx::FloatPoint { point_in_bbox };

    float area = signed_area(m_points[0], m_points[1], m_points[2]);
    VERIFY(area != 0);
    float alpha = signed_area(point, m_points[1], m_points[2]) / area;
    float beta = signed_area(m_points[0], point, m_points[2]) / area;
    float gamma = signed_area(m_points[0], m_points[1], point) / area;

    GouraudColor color;
    color.resize(m_color_space->number_of_components());

    m_functions.visit(
        [&](Empty) {
            for (int i = 0; i < m_color_space->number_of_components(); ++i)
                color[i] = clamp(alpha * m_colors[0][i] + beta * m_colors[1][i] + gamma * m_colors[2][i], m_bounds.min[i], m_bounds.max[i]);
        },
        [&](Function const& function) {
            float input = clamp(alpha * m_colors[0][0] + beta * m_colors[1][0] + gamma * m_colors[2][0], m_bounds.min[0], m_bounds.max[0]);
            auto result = MUST(function.evaluate(to_array({ input })));
            result.copy_to(color);
        },
        [&](Vector<NonnullRefPtr<Function>> const& functions) {
            float input = clamp(alpha * m_colors[0][0] + beta * m_colors[1][0] + gamma * m_colors[2][0], m_bounds.min[0], m_bounds.max[0]);
            for (size_t i = 0; i < functions.size(); ++i) {
                auto result = MUST(functions[i]->evaluate(to_array({ input })));
                color[i] = result[0];
            }
        });

    return MUST(m_color_space->style(color)).get<Gfx::Color>();
}

void draw_gouraud_triangle(Gfx::Painter& painter, NonnullRefPtr<ColorSpaceWithFloatArgs> color_space, ShadingFunctionsType functions, Array<Gfx::FloatPoint, 3> points, Array<GouraudColor, 3> colors, GouraudBounds const& bounds)
{
    static_assert(points.size() == 3);
    static_assert(colors.size() == 3);

    Gfx::Path triangle_path;
    triangle_path.move_to(points[0]);
    triangle_path.line_to(points[1]);
    triangle_path.line_to(points[2]);
    triangle_path.close();

    auto paint_style = GouraudPaintStyle::create(move(color_space), move(functions), move(points), move(colors), bounds);

    // To hide triangle edges. (Setting this to <Gfx::SampleAA> is useful for debugging; it makes triangle edges visible.)
    painter.fill_path<Gfx::SampleNoAA>(triangle_path, paint_style);
}

struct Triangle {
    u32 a;
    u32 b;
    u32 c;
};

PDFErrorOr<void> draw_gouraud_triangles(Gfx::Painter& painter, Gfx::AffineTransform const& ctm, NonnullRefPtr<ColorSpaceWithFloatArgs> color_space, ShadingFunctionsType const& functions, Vector<Triangle> const& triangles, Vector<float> const& vertex_data, GouraudBounds bounds)
{
    size_t const number_of_components = !functions.has<Empty>() ? 1 : color_space->number_of_components();
    bool is_indexed = color_space->family() == ColorSpaceFamily::Indexed;
    RefPtr<IndexedColorSpace> indexed_color_space;
    if (is_indexed) {
        indexed_color_space = static_ptr_cast<IndexedColorSpace>(color_space);
        color_space = indexed_color_space->base_color_space();
        bounds = bounds_from_decode_array(color_space->default_decode());
    }

    int const n = 2 + number_of_components;
    for (auto const& triangle : triangles) {
        // FIXME: early-out for triangles completely outside clip
        auto a = Gfx::FloatPoint { vertex_data[triangle.a * n], vertex_data[triangle.a * n + 1] };
        auto b = Gfx::FloatPoint { vertex_data[triangle.b * n], vertex_data[triangle.b * n + 1] };
        auto c = Gfx::FloatPoint { vertex_data[triangle.c * n], vertex_data[triangle.c * n + 1] };

        a = ctm.map(a);
        b = ctm.map(b);
        c = ctm.map(c);

        Array<GouraudColor, 3> colors;
        for (auto [i, triangle_index] : enumerate(to_array<u32>({ triangle.a, triangle.b, triangle.c }))) {
            GouraudColor color;
            if (is_indexed) {
                // "If ColorSpace is an Indexed color space, all color values specified in the shading
                //  are immediately converted to the base color space. [...] Interpolation never occurs
                //  in an Indexed color space, which is quantized and therefore inappropriate for calculations
                //  that assume a continuous range of colors."
                color.extend(TRY(indexed_color_space->base_components(vertex_data[triangle_index * n + 2])));
            } else {
                color.resize(number_of_components);
                for (size_t j = 0; j < number_of_components; ++j)
                    color[j] = vertex_data[triangle_index * n + 2 + j];
            }
            colors[i] = color;
        }
        draw_gouraud_triangle(painter, color_space, functions, { a, b, c }, move(colors), move(bounds));
    }

    return {};
}

static void draw_gouraud_quad(Gfx::Painter& painter, NonnullRefPtr<ColorSpaceWithFloatArgs> color_space, ShadingFunctionsType functions, Array<Gfx::FloatPoint, 4> points, Array<GouraudColor, 4> colors, GouraudBounds const& bounds)
{
    // FIXME: https://gpuopen.com/learn/bilinear-interpolation-quadrilateral-barycentric-coordinates/ / https://jcgt.org/published/0011/03/04/paper.pdf instead.
    draw_gouraud_triangle(painter, color_space, functions, { points[0], points[1], points[3] }, { colors[0], colors[1], colors[3] }, bounds);
    draw_gouraud_triangle(painter, color_space, functions, { points[0], points[2], points[3] }, { colors[0], colors[2], colors[3] }, bounds);
}

struct GouraudBezierPatch {
    Array<Gfx::FloatPoint, 16> points {};
    Array<GouraudColor, 4> colors {};
};

static void draw_gouraud_bezier_patch(Gfx::Painter& painter, NonnullRefPtr<ColorSpaceWithFloatArgs> color_space, ShadingFunctionsType functions, GouraudBezierPatch const& patch, GouraudBounds const& bounds, int depth = 0)
{
    auto const& points = patch.points;
    auto const& colors = patch.colors;

    // FIXME: This is very naive. Instead, compute error from linear patch and adaptively subdivide based on that error.
    //        Figure out a way to deal with T-junctions.
    if (depth == 5) {
        draw_gouraud_quad(painter, color_space, functions, { points[0], points[3], points[12], points[15] }, colors, bounds);
        return;
    }

    auto lerp = [](GouraudColor a, GouraudColor b, float t) {
        GouraudColor c;
        c.resize(a.size());
        for (size_t i = 0; i < a.size(); ++i)
            c[i] = mix(a[i], b[i], t);
        return c;
    };

    GouraudBezierPatch new_patch;
    auto& new_points = new_patch.points;
    auto& new_colors = new_patch.colors;

    // FIXME: Use separable De Casteljau's to do fewer additions and multiplications.

    // Lower left.
    // clang-format off
    new_points[0]  =  points[0];
    new_points[1]  = (points[0] + points[1]) / 2.0f;
    new_points[2]  = (points[0] + points[1] * 2 + points[2]) / 4.0f;
    new_points[3]  = (points[0] + points[1] * 3 + points[2] * 3 + points[3]) / 8.0f;

    new_points[4]  = (points[0] + points[4]) / 2.0f;
    new_points[5]  = (points[0] + points[4] +  points[1] + points[5]) / 4.0f;
    new_points[6]  = (points[0] + points[4] + (points[1] + points[5]) * 2 +  points[2] + points[6]) / 8.0f;
    new_points[7]  = (points[0] + points[4] + (points[1] + points[5]) * 3 + (points[2] + points[6]) * 3 + points[3] + points[7]) / 16.0f;

    new_points[8]  = (points[0] + points[4] * 2 + points[8]) / 4.0f;
    new_points[9]  = (points[0] + points[4] * 2 + points[8] +  points[1] + points[5] * 2 + points[9]) / 8.0f;
    new_points[10] = (points[0] + points[4] * 2 + points[8] + (points[1] + points[5] * 2 + points[9]) * 2 +  points[2] + points[6] * 2 + points[10]) / 16.0f;
    new_points[11] = (points[0] + points[4] * 2 + points[8] + (points[1] + points[5] * 2 + points[9]) * 3 + (points[2] + points[6] * 2 + points[10]) * 3 + points[3] + points[7] * 2 + points[11]) / 32.0f;

    new_points[12] = (points[0] + points[4] * 3 + points[8] * 3 + points[12]) / 8.0f;
    new_points[13] = (points[0] + points[4] * 3 + points[8] * 3 + points[12] +  points[1] + points[5] * 3 + points[9] * 3 + points[13]) / 16.0f;
    new_points[14] = (points[0] + points[4] * 3 + points[8] * 3 + points[12] + (points[1] + points[5] * 3 + points[9] * 3 + points[13]) * 2 +  points[2] + points[6] * 3 + points[10] * 3 + points[14]) / 32.0f;
    new_points[15] = (points[0] + points[4] * 3 + points[8] * 3 + points[12] + (points[1] + points[5] * 3 + points[9] * 3 + points[13]) * 3 + (points[2] + points[6] * 3 + points[10] * 3 + points[14]) * 3 + points[3] + points[7] * 3 + points[11] * 3 + points[15]) / 64.0f;
    // clang-format on

    new_colors[0] = colors[0];
    new_colors[1] = lerp(colors[0], colors[1], 0.5f);
    new_colors[2] = lerp(colors[0], colors[2], 0.5f);
    new_colors[3] = lerp(lerp(colors[0], colors[1], 0.5f), lerp(colors[2], colors[3], 0.5f), 0.5f);

    draw_gouraud_bezier_patch(painter, color_space, functions, new_patch, bounds, depth + 1);

    // Lower right.
    // clang-format off
    new_points[0]  = (points[0] + points[1] * 3 + points[2] * 3 + points[3]) / 8.0f;
    new_points[1]  = (points[1] + points[2] * 2 + points[3]) / 4.0f;
    new_points[2]  = (points[2] + points[3]) / 2.0f;
    new_points[3]  =  points[3];

    new_points[4]  = (points[0] + points[4] + (points[1] + points[5]) * 3 + (points[2] + points[6]) * 3 + points[3] + points[7]) / 16.0f;
    new_points[5]  = (points[1] + points[5] + (points[2] + points[6]) * 2 +  points[3] + points[7]) / 8.0f;
    new_points[6]  = (points[2] + points[6] + points[3] + points[7]) / 4.0f;
    new_points[7]  = (points[3] + points[7]) / 2.0f;

    new_points[8]  = (points[0] + points[4] * 2 + points[ 8] + (points[1] + points[5] * 2 + points[ 9]) * 3 + (points[2] + points[6] * 2 + points[10]) * 3 + points[3] + points[7] * 2 + points[11]) / 32.0f;
    new_points[9]  = (points[1] + points[5] * 2 + points[ 9] + (points[2] + points[6] * 2 + points[10]) * 2 +  points[3] + points[7] * 2 + points[11]) / 16.0f;
    new_points[10] = (points[2] + points[6] * 2 + points[10] +  points[3] + points[7] * 2 + points[11]) / 8.0f;
    new_points[11] = (points[3] + points[7] * 2 + points[11]) / 4.0f;

    new_points[12] = (points[0] + points[4] * 3 + points[ 8] * 3 + points[12] + (points[1] + points[5] * 3 + points[9] * 3 + points[13]) * 3 + (points[2] + points[6] * 3 + points[10] * 3 + points[14]) * 3 + points[3] + points[7] * 3 + points[11] * 3 + points[15]) / 64.0f;
    new_points[13] = (points[1] + points[5] * 3 + points[ 9] * 3 + points[13] + (points[2] + points[6] * 3 + points[10] * 3 + points[14]) * 2 + points[3] + points[7] * 3 + points[11] * 3 + points[15]) / 32.0f;
    new_points[14] = (points[2] + points[6] * 3 + points[10] * 3 + points[14] + points[3] + points[7] * 3 + points[11] * 3 + points[15]) / 16.0f;
    new_points[15] = (points[3] + points[7] * 3 + points[11] * 3 + points[15]) / 8.0f;
    // clang-format on

    new_colors[0] = lerp(colors[0], colors[1], 0.5f);
    new_colors[1] = colors[1];
    new_colors[2] = lerp(lerp(colors[0], colors[1], 0.5f), lerp(colors[2], colors[3], 0.5f), 0.5f);
    new_colors[3] = lerp(colors[1], colors[3], 0.5f);

    draw_gouraud_bezier_patch(painter, color_space, functions, new_patch, bounds, depth + 1);

    // Upper left.
    // clang-format off
    new_points[12] =  points[12];
    new_points[13] = (points[12] + points[13]) / 2.0f;
    new_points[14] = (points[12] + points[13] * 2 + points[14]) / 4.0f;
    new_points[15] = (points[12] + points[13] * 3 + points[14] * 3 + points[15]) / 8.0f;

    new_points[8]  = (points[12] + points[8]) / 2.0f;
    new_points[9]  = (points[12] + points[8] +  points[13] + points[9]) / 4.0f;
    new_points[10] = (points[12] + points[8] + (points[13] + points[9]) * 2 + points[14] + points[10]) / 8.0f;
    new_points[11] = (points[12] + points[8] + (points[13] + points[9]) * 3 + (points[14] + points[10]) * 3 + points[15] + points[11]) / 16.0f;

    new_points[4]  = (points[12] + points[8] * 2 + points[4]) / 4.0f;
    new_points[5]  = (points[12] + points[8] * 2 + points[4] +  points[13] + points[9] * 2 + points[5]) / 8.0f;
    new_points[6]  = (points[12] + points[8] * 2 + points[4] + (points[13] + points[9] * 2 + points[5]) * 2 +  points[14] + points[10] * 2 + points[6]) / 16.0f;
    new_points[7]  = (points[12] + points[8] * 2 + points[4] + (points[13] + points[9] * 2 + points[5]) * 3 + (points[14] + points[10] * 2 + points[6]) * 3 + points[15] + points[11] * 2 + points[7]) / 32.0f;

    new_points[0]  = (points[12] + points[8] * 3 + points[4] * 3 + points[0]) / 8.0f;
    new_points[1]  = (points[12] + points[8] * 3 + points[4] * 3 + points[0] +  points[13] + points[9] * 3 + points[5] * 3 + points[1]) / 16.0f;
    new_points[2]  = (points[12] + points[8] * 3 + points[4] * 3 + points[0] + (points[13] + points[9] * 3 + points[5] * 3 + points[1]) * 2 +  points[14] + points[10] * 3 + points[6] * 3 + points[2]) / 32.0f;
    new_points[3]  = (points[12] + points[8] * 3 + points[4] * 3 + points[0] + (points[13] + points[9] * 3 + points[5] * 3 + points[1]) * 3 + (points[14] + points[10] * 3 + points[6] * 3 + points[2]) * 3 + points[15] + points[11] * 3 + points[7] * 3 + points[3]) / 64.0f;
    // clang-format on

    new_colors[0] = lerp(colors[0], colors[2], 0.5f);
    new_colors[1] = lerp(lerp(colors[0], colors[1], 0.5f), lerp(colors[2], colors[3], 0.5f), 0.5f);
    new_colors[2] = colors[2];
    new_colors[3] = lerp(colors[2], colors[3], 0.5f);

    draw_gouraud_bezier_patch(painter, color_space, functions, new_patch, bounds, depth + 1);

    // Upper right.
    // clang-format off
    new_points[12] = (points[12] + points[13] * 3 + points[14] * 3 + points[15]) / 8.0f;
    new_points[13] = (points[13] + points[14] * 2 + points[15]) / 4.0f;
    new_points[14] = (points[14] + points[15]) / 2.0f;
    new_points[15] =  points[15];

    new_points[8]  = (points[12] + points[ 8] + (points[13] + points[ 9]) * 3 + (points[14] + points[10]) * 3 + points[15] + points[11]) / 16.0f;
    new_points[9]  = (points[13] + points[ 9] + (points[14] + points[10]) * 2 +  points[15] + points[11]) / 8.0f;
    new_points[10] = (points[14] + points[10] +  points[15] + points[11]) / 4.0f;
    new_points[11] = (points[15] + points[11]) / 2.0f;

    new_points[4]  = (points[12] + points[ 8] * 2 + points[4] + (points[13] + points[ 9] * 2 + points[5]) * 3 + (points[14] + points[10] * 2 + points[6]) * 3 + points[15] + points[11] * 2 + points[7]) / 32.0f;
    new_points[5]  = (points[13] + points[ 9] * 2 + points[5] + (points[14] + points[10] * 2 + points[6]) * 2 +  points[15] + points[11] * 2 + points[7]) / 16.0f;
    new_points[6]  = (points[14] + points[10] * 2 + points[6] +  points[15] + points[11] * 2 + points[7]) / 8.0f;
    new_points[7]  = (points[15] + points[11] * 2 + points[7]) / 4.0f;

    new_points[0]  = (points[12] + points[ 8] * 3 + points[4] * 3 + points[0] + (points[13] + points[ 9] * 3 + points[5] * 3 + points[1]) * 3 + (points[14] + points[10] * 3 + points[6] * 3 + points[2]) * 3 + points[15] + points[11] * 3 + points[7] * 3 + points[3]) / 64.0f;
    new_points[1]  = (points[13] + points[ 9] * 3 + points[5] * 3 + points[1] + (points[14] + points[10] * 3 + points[6] * 3 + points[2]) * 2 +  points[15] + points[11] * 3 + points[7] * 3 + points[3]) / 32.0f;
    new_points[2]  = (points[14] + points[10] * 3 + points[6] * 3 + points[2] +  points[15] + points[11] * 3 + points[7] * 3 + points[3]) / 16.0f;
    new_points[3]  = (points[15] + points[11] * 3 + points[7] * 3 + points[3]) / 8.0f;
    // clang-format on

    new_colors[0] = lerp(lerp(colors[0], colors[1], 0.5f), lerp(colors[2], colors[3], 0.5f), 0.5f);
    new_colors[1] = lerp(colors[1], colors[3], 0.5f);
    new_colors[2] = lerp(colors[2], colors[3], 0.5f);
    new_colors[3] = colors[3];

    draw_gouraud_bezier_patch(painter, color_space, functions, new_patch, bounds, depth + 1);
}

class FreeFormGouraudShading final : public Shading {
public:
    static PDFErrorOr<NonnullRefPtr<FreeFormGouraudShading>> create(Document*, NonnullRefPtr<StreamObject>, CommonEntries);

    virtual Optional<Gfx::FloatRect> bounding_box() const override { return m_common_entries.b_box; }
    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    FreeFormGouraudShading(CommonEntries common_entries, Vector<float> vertex_data, Vector<Triangle> triangles, ShadingFunctionsType functions, GouraudBounds bounds)
        : m_common_entries(move(common_entries))
        , m_vertex_data(move(vertex_data))
        , m_triangles(move(triangles))
        , m_functions(move(functions))
        , m_bounds(move(bounds))
    {
    }

    CommonEntries m_common_entries;

    // Interleaved x, y, c0, c1, c2, ...
    Vector<float> m_vertex_data;
    Vector<Triangle> m_triangles;
    ShadingFunctionsType m_functions;
    GouraudBounds m_bounds;
};

PDFErrorOr<NonnullRefPtr<FreeFormGouraudShading>> FreeFormGouraudShading::create(Document* document, NonnullRefPtr<StreamObject> shading_stream, CommonEntries common_entries)
{
    auto shading_dict = shading_stream->dict();

    // TABLE 4.32 Additional entries specific to a type 4 shading dictionary
    // "(Required) The number of bits used to represent each vertex coordinate.
    //  Valid values are 1, 2, 4, 8, 12, 16, 24, and 32."
    int bits_per_coordinate = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerCoordinate))).to_int();
    if (!first_is_one_of(bits_per_coordinate, 1, 2, 4, 8, 12, 16, 24, 32))
        return Error::malformed_error("BitsPerCoordinate invalid");

    // "(Required) The number of bits used to represent each color component.
    //  Valid values are 1, 2, 4, 8, 12, and 16."
    int bits_per_component = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerComponent))).to_int();
    if (!first_is_one_of(bits_per_component, 1, 2, 4, 8, 12, 16))
        return Error::malformed_error("BitsPerComponent invalid");

    // "(Required) The number of bits used to represent the edge flag for each ver-
    //  tex (see below). Valid values of BitsPerFlag are 2, 4, and 8, but only the
    //  least significant 2 bits in each flag value are used. Valid values for the edge
    //  flag are 0, 1, and 2."
    int bits_per_flag = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerFlag))).to_int();
    if (!first_is_one_of(bits_per_flag, 2, 4, 8))
        return Error::malformed_error("BitsPerFlag invalid");

    // "(Required) An array of numbers specifying how to map vertex coordinates
    //  and color components into the appropriate ranges of values. The decoding
    //  method is similar to that used in image dictionaries (see “Decode Arrays”
    //  on page 344). The ranges are specified as follows:
    //
    //      [ xmin xmax ymin ymax c1,min c1,max … cn,min cn,max ]
    //
    //  Note that only one pair of c values should be specified if a Function entry
    //  is present."
    auto decode_array = TRY(shading_dict->get_array(document, CommonNames::Decode));
    size_t number_of_components = static_cast<size_t>(shading_dict->contains(CommonNames::Function) ? 1 : common_entries.color_space->number_of_components());
    if (decode_array->size() != 4 + 2 * number_of_components)
        return Error::malformed_error("Decode array must have 4 + 2 * number of components elements");
    Vector<float> decode;
    decode.resize(decode_array->size());
    for (size_t i = 0; i < decode_array->size(); ++i)
        decode[i] = decode_array->at(i).to_float();

    // "(Optional) A 1-in, n-out function or an array of n 1-in, 1-out functions
    //  (where n is the number of color components in the shading dictionary’s
    //  color space). If this entry is present, the color data for each vertex must be
    //  specified by a single parametric variable rather than by n separate color
    //  components. The designated function(s) are called with each interpolated
    //  value of the parametric variable to determine the actual color at each
    //  point. Each input value is forced into the range interval specified for the
    //  corresponding color component in the shading dictionary’s Decode array.
    //  Each function’s domain must be a superset of that interval. If the value re-
    //  turned by the function for a given color component is out of range, it is
    //  adjusted to the nearest valid value.
    //  This entry may not be used with an Indexed color space."
    ShadingFunctionsType functions;
    if (shading_dict->contains(CommonNames::Function))
        functions = TRY(read_shading_functions(document, shading_dict, common_entries.color_space, decode[4]));

    // See "Type 4 Shadings (Free-Form Gouraud-Shaded Triangle Meshes)" in the PDF 1.7 spec for a description of the stream contents.
    auto stream = FixedMemoryStream { shading_stream->bytes() };
    BigEndianInputBitStream bitstream { MaybeOwned { stream } };

    Vector<u8> flags;
    Vector<float> vertex_data;
    while (!bitstream.is_eof()) {
        u8 flag = TRY(bitstream.read_bits<u8>(bits_per_flag));
        if (flag > 2)
            return Error::malformed_error("Invalid edge flag");
        TRY(flags.try_append(flag));

        u32 x = TRY(bitstream.read_bits<u32>(bits_per_coordinate));
        u32 y = TRY(bitstream.read_bits<u32>(bits_per_coordinate));
        TRY(vertex_data.try_append(mix(decode[0], decode[1], x / (powf(2.0f, bits_per_coordinate) - 1))));
        TRY(vertex_data.try_append(mix(decode[2], decode[3], y / (powf(2.0f, bits_per_coordinate) - 1))));
        for (size_t i = 0; i < number_of_components; ++i) {
            u16 color = TRY(bitstream.read_bits<u16>(bits_per_component));
            TRY(vertex_data.try_append(mix(decode[4 + 2 * i], decode[4 + 2 * i + 1], color / (powf(2.0f, bits_per_component) - 1))));
        }
        bitstream.align_to_byte_boundary();
    }

    Vector<Triangle> triangles;
    for (u32 i = 0; i < flags.size(); ++i) {
        if (flags[i] == 0) {
            if (i + 2 >= flags.size())
                return Error::malformed_error("Invalid triangle");
            triangles.append({ i, i + 1, i + 2 });
            i += 2;
        } else if (flags[i] == 1) {
            if (triangles.is_empty())
                return Error::malformed_error("Invalid triangle strip");
            triangles.append({ triangles.last().b, triangles.last().c, i });
        } else if (flags[i] == 2) {
            if (triangles.is_empty())
                return Error::malformed_error("Invalid triangle fan");
            triangles.append({ triangles.last().a, triangles.last().c, i });
        }
    }

    GouraudBounds bounds = bounds_from_decode_array(decode.span().slice(4));
    return adopt_ref(*new FreeFormGouraudShading(move(common_entries), move(vertex_data), move(triangles), move(functions), move(bounds)));
}

PDFErrorOr<void> FreeFormGouraudShading::draw(Gfx::Painter& painter, Gfx::AffineTransform const& ctm)
{
    return draw_gouraud_triangles(painter, ctm, m_common_entries.color_space, m_functions, m_triangles, m_vertex_data, m_bounds);
}

class LatticeFormGouraudShading final : public Shading {
public:
    static PDFErrorOr<NonnullRefPtr<LatticeFormGouraudShading>> create(Document*, NonnullRefPtr<StreamObject>, CommonEntries);

    virtual Optional<Gfx::FloatRect> bounding_box() const override { return m_common_entries.b_box; }
    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    LatticeFormGouraudShading(CommonEntries common_entries, Vector<float> vertex_data, Vector<Triangle> triangles, ShadingFunctionsType functions, GouraudBounds bounds)
        : m_common_entries(move(common_entries))
        , m_vertex_data(move(vertex_data))
        , m_triangles(move(triangles))
        , m_functions(move(functions))
        , m_bounds(move(bounds))
    {
    }

    CommonEntries m_common_entries;

    // Interleaved x, y, c0, c1, c2, ...
    Vector<float> m_vertex_data;
    Vector<Triangle> m_triangles;
    ShadingFunctionsType m_functions;
    GouraudBounds m_bounds;
};

PDFErrorOr<NonnullRefPtr<LatticeFormGouraudShading>> LatticeFormGouraudShading::create(Document* document, NonnullRefPtr<StreamObject> shading_stream, CommonEntries common_entries)
{
    auto shading_dict = shading_stream->dict();

    // TABLE 4.33 Additional entries specific to a type 5 shading dictionary
    // "(Required) The number of bits used to represent each vertex coordinate.
    //  Valid values are 1, 2, 4, 8, 12, 16, 24, and 32."
    int bits_per_coordinate = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerCoordinate))).to_int();
    if (!first_is_one_of(bits_per_coordinate, 1, 2, 4, 8, 12, 16, 24, 32))
        return Error::malformed_error("BitsPerCoordinate invalid");

    // "(Required) The number of bits used to represent each color component.
    //  Valid values are 1, 2, 4, 8, 12, and 16."
    int bits_per_component = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerComponent))).to_int();
    if (!first_is_one_of(bits_per_component, 1, 2, 4, 8, 12, 16))
        return Error::malformed_error("BitsPerComponent invalid");

    // "(Required) The number of vertices in each row of the lattice; the value
    //  must be greater than or equal to 2. The number of rows need not be
    //  specified."
    int vertices_per_row = TRY(document->resolve(shading_dict->get_value(CommonNames::VerticesPerRow))).to_int();
    if (vertices_per_row < 2)
        return Error::malformed_error("VerticesPerRow invalid");

    // "(Required) An array of numbers specifying how to map vertex coordinates
    //  and color components into the appropriate ranges of values. The decoding
    //  method is similar to that used in image dictionaries (see “Decode Arrays”
    //  on page 344). The ranges are specified as follows:
    //
    //      [ xmin xmax ymin ymax c1,min c1,max … cn,min cn,max ]
    //
    //  Note that only one pair of c values should be specified if a Function entry
    //  is present."
    auto decode_array = TRY(shading_dict->get_array(document, CommonNames::Decode));
    size_t number_of_components = static_cast<size_t>(shading_dict->contains(CommonNames::Function) ? 1 : common_entries.color_space->number_of_components());
    if (decode_array->size() != 4 + 2 * number_of_components)
        return Error::malformed_error("Decode array must have 4 + 2 * number of components elements");
    Vector<float> decode;
    decode.resize(decode_array->size());
    for (size_t i = 0; i < decode_array->size(); ++i)
        decode[i] = decode_array->at(i).to_float();

    // "(Optional) A 1-in, n-out function or an array of n 1-in, 1-out functions
    //  (where n is the number of color components in the shading dictionary’s
    //  color space). If this entry is present, the color data for each vertex must be
    //  specified by a single parametric variable rather than by n separate color
    //  components. The designated function(s) are called with each interpolated
    //  value of the parametric variable to determine the actual color at each
    //  point. Each input value is forced into the range interval specified for the
    //  corresponding color component in the shading dictionary’s Decode array.
    //  Each function’s domain must be a superset of that interval. If the value re-
    //  turned by the function for a given color component is out of range, it is
    //  adjusted to the nearest valid value.
    //  This entry may not be used with an Indexed color space."
    ShadingFunctionsType functions;
    if (shading_dict->contains(CommonNames::Function))
        functions = TRY(read_shading_functions(document, shading_dict, common_entries.color_space, decode[4]));

    // See "Type 5 Shadings (Lattice-Form Gouraud-Shaded Triangle Meshes)" in the PDF 1.7 spec for a description of the stream contents.
    auto stream = FixedMemoryStream { shading_stream->bytes() };
    BigEndianInputBitStream bitstream { MaybeOwned { stream } };

    Vector<float> vertex_data;
    while (!bitstream.is_eof()) {
        u32 x = TRY(bitstream.read_bits<u32>(bits_per_coordinate));
        u32 y = TRY(bitstream.read_bits<u32>(bits_per_coordinate));
        TRY(vertex_data.try_append(mix(decode[0], decode[1], x / (powf(2.0f, bits_per_coordinate) - 1))));
        TRY(vertex_data.try_append(mix(decode[2], decode[3], y / (powf(2.0f, bits_per_coordinate) - 1))));
        for (size_t i = 0; i < number_of_components; ++i) {
            u16 color = TRY(bitstream.read_bits<u16>(bits_per_component));
            TRY(vertex_data.try_append(mix(decode[4 + 2 * i], decode[4 + 2 * i + 1], color / (powf(2.0f, bits_per_component) - 1))));
        }
        bitstream.align_to_byte_boundary();
    }

    size_t number_of_vertices = vertex_data.size() / (2 + number_of_components);
    if (number_of_vertices % vertices_per_row != 0)
        return Error::malformed_error("Number of vertices must be a multiple of vertices per row");
    size_t number_of_rows = number_of_vertices / vertices_per_row;
    if (number_of_rows < 2)
        return Error::malformed_error("Number of rows must be at least 2");

    Vector<Triangle> triangles;
    for (u32 i = 0; i <= number_of_rows - 2; ++i) {
        for (u32 j = 0; j <= static_cast<u32>(vertices_per_row - 2); ++j) {
            triangles.append({ i * vertices_per_row + j, i * vertices_per_row + j + 1, (i + 1) * vertices_per_row + j });
            triangles.append({ i * vertices_per_row + j + 1, (i + 1) * vertices_per_row + j, (i + 1) * vertices_per_row + j + 1 });
        }
    }

    GouraudBounds bounds = bounds_from_decode_array(decode.span().slice(4));
    return adopt_ref(*new LatticeFormGouraudShading(move(common_entries), move(vertex_data), move(triangles), move(functions), move(bounds)));
}

PDFErrorOr<void> LatticeFormGouraudShading::draw(Gfx::Painter& painter, Gfx::AffineTransform const& ctm)
{
    return draw_gouraud_triangles(painter, ctm, m_common_entries.color_space, m_functions, m_triangles, m_vertex_data, m_bounds);
}

class CoonsPatchShading final : public Shading {
public:
    static PDFErrorOr<NonnullRefPtr<CoonsPatchShading>> create(Document*, NonnullRefPtr<StreamObject>, CommonEntries);

    virtual Optional<Gfx::FloatRect> bounding_box() const override { return m_common_entries.b_box; }
    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    // Indexes into m_patch_data.
    struct CoonsPatch {
        u32 control_points[12];
        u32 colors[4];
    };

    CoonsPatchShading(CommonEntries common_entries, Vector<float> patch_data, Vector<CoonsPatch> patches, ShadingFunctionsType functions, GouraudBounds bounds)
        : m_common_entries(move(common_entries))
        , m_patch_data(move(patch_data))
        , m_patches(move(patches))
        , m_functions(move(functions))
        , m_bounds(move(bounds))
    {
    }

    CommonEntries m_common_entries;

    // Interleaved x0, y0, x1, y1, ..., x11, y11, c0, c1, c2, c3, ...
    // (For flags 1-3, only 8 coordinates and 2 colors.)
    Vector<float> m_patch_data;
    Vector<CoonsPatch> m_patches;
    ShadingFunctionsType m_functions;
    GouraudBounds m_bounds;
};

PDFErrorOr<NonnullRefPtr<CoonsPatchShading>> CoonsPatchShading::create(Document* document, NonnullRefPtr<StreamObject> shading_stream, CommonEntries common_entries)
{
    auto shading_dict = shading_stream->dict();

    // TABLE 4.34 Additional entries specific to a type 6 shading dictionary
    // "(Required) The number of bits used to represent each geometric coordi-
    //  nate. Valid values are 1, 2, 4, 8, 12, 16, 24, and 32."
    int bits_per_coordinate = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerCoordinate))).to_int();
    if (!first_is_one_of(bits_per_coordinate, 1, 2, 4, 8, 12, 16, 24, 32))
        return Error::malformed_error("BitsPerCoordinate invalid");

    // "(Required) The number of bits used to represent each color component.
    //  Valid values are 1, 2, 4, 8, 12, and 16."
    int bits_per_component = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerComponent))).to_int();
    if (!first_is_one_of(bits_per_component, 1, 2, 4, 8, 12, 16))
        return Error::malformed_error("BitsPerComponent invalid");

    // "(Required) The number of bits used to represent the edge flag for each
    //  patch (see below). Valid values of BitsPerFlag are 2, 4, and 8, but only the
    //  least significant 2 bits in each flag value are used. Valid values for the edge
    //  flag are 0, 1, 2, and 3."
    int bits_per_flag = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerFlag))).to_int();
    if (!first_is_one_of(bits_per_flag, 2, 4, 8))
        return Error::malformed_error("BitsPerFlag invalid");

    // "(Required) An array of numbers specifying how to map vertex coordinates
    //  and color components into the appropriate ranges of values. The decoding
    //  method is similar to that used in image dictionaries (see “Decode Arrays”
    //  on page 344). The ranges are specified as follows:
    //
    //      [ xmin xmax ymin ymax c1,min c1,max … cn,min cn,max ]
    //
    //  Note that only one pair of c values should be specified if a Function entry
    //  is present."
    auto decode_array = TRY(shading_dict->get_array(document, CommonNames::Decode));
    size_t number_of_components = static_cast<size_t>(shading_dict->contains(CommonNames::Function) ? 1 : common_entries.color_space->number_of_components());
    if (decode_array->size() != 4 + 2 * number_of_components)
        return Error::malformed_error("Decode array must have 4 + 2 * number of components elements");
    Vector<float> decode;
    decode.resize(decode_array->size());
    for (size_t i = 0; i < decode_array->size(); ++i)
        decode[i] = decode_array->at(i).to_float();

    // "(Optional) A 1-in, n-out function or an array of n 1-in, 1-out functions
    //  (where n is the number of color components in the shading dictionary’s
    //  color space). If this entry is present, the color data for each vertex must be
    //  specified by a single parametric variable rather than by n separate color
    //  components. The designated function(s) are called with each interpolated
    //  value of the parametric variable to determine the actual color at each
    //  point. Each input value is forced into the range interval specified for the
    //  corresponding color component in the shading dictionary’s Decode array.
    //  Each function’s domain must be a superset of that interval. If the value re-
    //  turned by the function for a given color component is out of range, it is
    //  adjusted to the nearest valid value.
    //  This entry may not be used with an Indexed color space."
    ShadingFunctionsType functions;
    if (shading_dict->contains(CommonNames::Function))
        functions = TRY(read_shading_functions(document, shading_dict, common_entries.color_space, decode[4]));

    // See "Type 6 Shadings (Coons Patch Meshes)" in the PDF 1.7 spec for a description of the stream contents.
    auto stream = FixedMemoryStream { shading_stream->bytes() };
    BigEndianInputBitStream bitstream { MaybeOwned { stream } };

    Vector<float> patch_data;
    Vector<CoonsPatch> patches;

    auto read_point = [&]() -> ErrorOr<void> {
        u32 x = TRY(bitstream.read_bits<u32>(bits_per_coordinate));
        u32 y = TRY(bitstream.read_bits<u32>(bits_per_coordinate));
        TRY(patch_data.try_append(mix(decode[0], decode[1], x / (powf(2.0f, bits_per_coordinate) - 1))));
        TRY(patch_data.try_append(mix(decode[2], decode[3], y / (powf(2.0f, bits_per_coordinate) - 1))));
        return {};
    };

    auto read_points = [&](u32 n) -> ErrorOr<void> {
        for (u32 i = 0; i < n; ++i)
            TRY(read_point());
        return {};
    };

    auto read_color = [&]() -> ErrorOr<void> {
        for (size_t i = 0; i < number_of_components; ++i) {
            u16 color = TRY(bitstream.read_bits<u16>(bits_per_component));
            TRY(patch_data.try_append(mix(decode[4 + 2 * i], decode[4 + 2 * i + 1], color / (powf(2.0f, bits_per_component) - 1))));
        }
        return {};
    };

    auto read_colors = [&](u32 n) -> ErrorOr<void> {
        for (u32 i = 0; i < n; ++i)
            TRY(read_color());
        return {};
    };

    while (!bitstream.is_eof()) {
        u8 flag = TRY(bitstream.read_bits<u8>(bits_per_flag));

        int n = patch_data.size();
        CoonsPatch patch;

        // "TABLE 4.35 Data values in a Coons patch mesh"
        switch (flag) {
        case 0:
            // "x1 y1 x2 y2 x3 y3 x4 y4 x5 y5 x6 y6 x7 y7 x8 y8 x9 y9 x10 y10 x11 y11 x12 y12 c1 c2 c3 c4
            //  New patch; no implicit values"
            TRY(patch_data.try_ensure_capacity(patch_data.size() + 12 * 2 + 4 + number_of_components));
            TRY(read_points(12));
            TRY(read_colors(4));
            for (int i = 0; i < 12; ++i)
                patch.control_points[i] = n + 2 * i;
            for (int i = 0; i < 4; ++i)
                patch.colors[i] = n + 24 + number_of_components * i;
            break;
        case 1:
            if (patches.is_empty())
                return Error::malformed_error("Edge flag 1 without preceding patch");
            // "x5 y5 x6 y6 x7 y7 x8 y8 x9 y9 x10 y10 x11 y11 x12 y12 c3 c4
            //  Implicit values:
            //  (x1, y1) = (x4, y4) previous
            //  (x2, y2) = (x5, y5) previous
            //  (x3, y3) = (x6, y6) previous
            //  (x4, y4) = (x7, y7) previous
            //  c1 = c2 previous
            //  c2 = c3 previous"
            TRY(patch_data.try_ensure_capacity(patch_data.size() + 8 * 2 + 2 + number_of_components));
            TRY(read_points(8));
            TRY(read_colors(2));
            patch.control_points[0] = patches.last().control_points[3];
            patch.control_points[1] = patches.last().control_points[4];
            patch.control_points[2] = patches.last().control_points[5];
            patch.control_points[3] = patches.last().control_points[6];
            for (int i = 0; i < 8; ++i)
                patch.control_points[i + 4] = n + 2 * i;
            patch.colors[0] = patches.last().colors[1];
            patch.colors[1] = patches.last().colors[2];
            for (int i = 0; i < 2; ++i)
                patch.colors[i + 2] = n + 16 + number_of_components * i;
            break;
        case 2:
            if (patches.is_empty())
                return Error::malformed_error("Edge flag 2 without preceding patch");
            // "x5 y5 x6 y6 x7 y7 x8 y8 x9 y9 x10 y10 x11 y11 x12 y12 c3 c4
            //  Implicit values:
            //  (x1, y1) = (x7, y7) previous
            //  (x2, y2) = (x8, y8) previous
            //  (x3, y3) = (x9, y9) previous
            //  (x4, y4) = (x10, y10) previous
            //  c1 = c3 previous
            //  c2 = c4 previous"
            TRY(patch_data.try_ensure_capacity(patch_data.size() + 8 * 2 + 2 + number_of_components));
            TRY(read_points(8));
            TRY(read_colors(2));
            patch.control_points[0] = patches.last().control_points[6];
            patch.control_points[1] = patches.last().control_points[7];
            patch.control_points[2] = patches.last().control_points[8];
            patch.control_points[3] = patches.last().control_points[9];
            for (int i = 0; i < 8; ++i)
                patch.control_points[i + 4] = n + 2 * i;
            patch.colors[0] = patches.last().colors[2];
            patch.colors[1] = patches.last().colors[3];
            for (int i = 0; i < 2; ++i)
                patch.colors[i + 2] = n + 16 + number_of_components * i;
            break;
        case 3:
            if (patches.is_empty())
                return Error::malformed_error("Edge flag 3 without preceding patch");
            // "x5 y5 x6 y6 x7 y7 x8 y8 x9 y9 x10 y10 x11 y11 x12 y12 c3 c4
            //  Implicit values:
            //  (x1, y1) = (x10, y10) previous
            //  (x2, y2) = (x11, y11) previous
            //  (x3, y3) = (x12, y12) previous
            //  (x4, y4) = (x1, y1) previous
            //  c1 = c4 previous
            //  c2 = c1 previous"
            TRY(patch_data.try_ensure_capacity(patch_data.size() + 8 * 2 + 2 + number_of_components));
            TRY(read_points(8));
            TRY(read_colors(2));
            patch.control_points[0] = patches.last().control_points[9];
            patch.control_points[1] = patches.last().control_points[10];
            patch.control_points[2] = patches.last().control_points[11];
            patch.control_points[3] = patches.last().control_points[0];
            for (int i = 0; i < 8; ++i)
                patch.control_points[i + 4] = n + 2 * i;
            patch.colors[0] = patches.last().colors[3];
            patch.colors[1] = patches.last().colors[0];
            for (int i = 0; i < 2; ++i)
                patch.colors[i + 2] = n + 16 + number_of_components * i;
            break;
        default:
            return Error::malformed_error("Invalid edge flag");
        }

        TRY(patches.try_append(patch));
        bitstream.align_to_byte_boundary();
    }

    GouraudBounds bounds = bounds_from_decode_array(decode.span().slice(4));
    return adopt_ref(*new CoonsPatchShading(move(common_entries), move(patch_data), move(patches), move(functions), move(bounds)));
}

PDFErrorOr<void> CoonsPatchShading::draw(Gfx::Painter& painter, Gfx::AffineTransform const& ctm)
{
    NonnullRefPtr<ColorSpaceWithFloatArgs> color_space = m_common_entries.color_space;
    size_t const number_of_components = !m_functions.has<Empty>() ? 1 : color_space->number_of_components();

    bool is_indexed = color_space->family() == ColorSpaceFamily::Indexed;
    RefPtr<IndexedColorSpace> indexed_color_space;
    auto bounds = m_bounds;
    if (is_indexed) {
        indexed_color_space = static_ptr_cast<IndexedColorSpace>(color_space);
        color_space = indexed_color_space->base_color_space();
        bounds = bounds_from_decode_array(color_space->default_decode());
    }

    for (auto& patch : m_patches) {
        GouraudBezierPatch bezier_patch;
        auto& control_points = bezier_patch.points;

        for (size_t i = 0; i < 4; ++i)
            control_points[i] = ctm.map(Gfx::FloatPoint { m_patch_data[patch.control_points[i]], m_patch_data[patch.control_points[i] + 1] });

        for (size_t i = 0; i < 3; ++i)
            control_points[7 + i * 4] = ctm.map(Gfx::FloatPoint { m_patch_data[patch.control_points[4 + i]], m_patch_data[patch.control_points[4 + i] + 1] });
        for (size_t i = 0; i < 3; ++i)
            control_points[14 - i] = ctm.map(Gfx::FloatPoint { m_patch_data[patch.control_points[7 + i]], m_patch_data[patch.control_points[7 + i] + 1] });

        control_points[8] = ctm.map(Gfx::FloatPoint { m_patch_data[patch.control_points[10]], m_patch_data[patch.control_points[10] + 1] });
        control_points[4] = ctm.map(Gfx::FloatPoint { m_patch_data[patch.control_points[11]], m_patch_data[patch.control_points[11] + 1] });

        // "The Coons patch (type 6) is actually a special case of the tensor-product patch
        //  (type 7) in which the four internal control points (p11 , p12 , p21 , p22 ) are implicitly
        //  defined by the boundary curves. The values of the internal control points are giv-
        //  en by these equations:"
        auto p = [&](int c, int r) -> Gfx::FloatPoint& { return bezier_patch.points[c + 4 * r]; };

        p(1, 1) = (1.0f / 9.0f)
            * (-4.0f * p(0, 0)
                + 6.0f * (p(0, 1) + p(1, 0))
                - 2.0f * (p(0, 3) + p(3, 0))
                + 3.0f * (p(3, 1) + p(1, 3))
                - 1.0f * p(3, 3));

        p(1, 2) = (1.0f / 9.0f)
            * (-4.0f * p(0, 3)
                + 6.0f * (p(0, 2) + p(1, 3))
                - 2.0f * (p(0, 0) + p(3, 3))
                + 3.0f * (p(3, 2) + p(1, 0))
                - 1.0f * p(3, 0));

        p(2, 1) = (1.0f / 9.0f)
            * (-4.0f * p(3, 0)
                + 6.0f * (p(3, 1) + p(2, 0))
                - 2.0f * (p(3, 3) + p(0, 0))
                + 3.0f * (p(0, 1) + p(2, 3))
                - 1.0f * p(0, 3));

        p(2, 2) = (1.0f / 9.0f)
            * (-4.0f * p(3, 3)
                + 6.0f * (p(3, 2) + p(2, 3))
                - 2.0f * (p(3, 0) + p(0, 3))
                + 3.0f * (p(0, 2) + p(2, 0))
                - 1.0f * p(0, 0));

        for (size_t i = 0; i < 4; ++i) {
            GouraudColor color;

            if (is_indexed) {
                // "If ColorSpace is an Indexed color space, all color values specified in the shading
                //  are immediately converted to the base color space. [...] Interpolation never occurs
                //  in an Indexed color space, which is quantized and therefore inappropriate for calculations
                //  that assume a continuous range of colors."
                color.extend(TRY(indexed_color_space->base_components(m_patch_data[patch.colors[i]])));
            } else {
                color.resize(number_of_components);
                for (size_t j = 0; j < number_of_components; ++j)
                    color[j] = m_patch_data[patch.colors[i] + j];
            }

            bezier_patch.colors[i] = color;
        }

        swap(bezier_patch.colors[2], bezier_patch.colors[3]); // Coons order goes counter-clockwise, bezier patch in scanline order.
        draw_gouraud_bezier_patch(painter, color_space, m_functions, bezier_patch, bounds);
    }
    return {};
}

class TensorProductPatchShading final : public Shading {
public:
    static PDFErrorOr<NonnullRefPtr<TensorProductPatchShading>> create(Document*, NonnullRefPtr<StreamObject>, CommonEntries);

    virtual Optional<Gfx::FloatRect> bounding_box() const override { return m_common_entries.b_box; }
    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    // Indexes into m_patch_data.
    struct TensorProductPatch {
        // Pij (col i, row j) is at index:
        // p03 p13 p23 p33       12 13 14 15
        // p02 p12 p22 p32  <=>   8  9 10 11
        // p01 p11 p21 p31        4  5  6  7
        // p00 p10 p20 p30        0  1  2  3
        u32 control_points[16];

        // cij (col i, row j) is at index:
        // c03 c33       2 3
        // c00 c30  <=>  0 1
        u32 colors[4];
    };

    TensorProductPatchShading(CommonEntries common_entries, Vector<float> patch_data, Vector<TensorProductPatch> patches, ShadingFunctionsType functions, GouraudBounds bounds)
        : m_common_entries(move(common_entries))
        , m_patch_data(move(patch_data))
        , m_patches(move(patches))
        , m_functions(move(functions))
        , m_bounds(move(bounds))
    {
    }

    CommonEntries m_common_entries;

    // Interleaved x0, y0, x1, y1, ..., x15, y15, c0, c1, c2, c3, ...
    // (For flags 1-3, only 12 coordinates and 2 colors.)
    Vector<float> m_patch_data;
    Vector<TensorProductPatch> m_patches;
    ShadingFunctionsType m_functions;
    GouraudBounds m_bounds;
};

PDFErrorOr<NonnullRefPtr<TensorProductPatchShading>> TensorProductPatchShading::create(Document* document, NonnullRefPtr<StreamObject> shading_stream, CommonEntries common_entries)
{
    auto shading_dict = shading_stream->dict();

    // "Type 7 shadings (tensor-product patch meshes) are identical to type 6, except that
    //  they are based on a bicubic tensor-product patch defined by 16 control points in-
    //  stead of the 12 control points that define a Coons patch. The shading dictionaries
    //  representing the two patch types differ only in the value of the ShadingType entry
    //  and in the number of control points specified for each patch in the data stream."

    // FIXME: Extract some common code once we have implemented painting and can make sure
    //        that refactoring doesn't break things.

    // TABLE 4.34 Additional entries specific to a type 6 shading dictionary
    // "(Required) The number of bits used to represent each geometric coordi-
    //  nate. Valid values are 1, 2, 4, 8, 12, 16, 24, and 32."
    int bits_per_coordinate = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerCoordinate))).to_int();
    if (!first_is_one_of(bits_per_coordinate, 1, 2, 4, 8, 12, 16, 24, 32))
        return Error::malformed_error("BitsPerCoordinate invalid");

    // "(Required) The number of bits used to represent each color component.
    //  Valid values are 1, 2, 4, 8, 12, and 16."
    int bits_per_component = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerComponent))).to_int();
    if (!first_is_one_of(bits_per_component, 1, 2, 4, 8, 12, 16))
        return Error::malformed_error("BitsPerComponent invalid");

    // "(Required) The number of bits used to represent the edge flag for each
    //  patch (see below). Valid values of BitsPerFlag are 2, 4, and 8, but only the
    //  least significant 2 bits in each flag value are used. Valid values for the edge
    //  flag are 0, 1, 2, and 3."
    int bits_per_flag = TRY(document->resolve(shading_dict->get_value(CommonNames::BitsPerFlag))).to_int();
    if (!first_is_one_of(bits_per_flag, 2, 4, 8))
        return Error::malformed_error("BitsPerFlag invalid");

    // "(Required) An array of numbers specifying how to map vertex coordinates
    //  and color components into the appropriate ranges of values. The decoding
    //  method is similar to that used in image dictionaries (see “Decode Arrays”
    //  on page 344). The ranges are specified as follows:
    //
    //      [ xmin xmax ymin ymax c1,min c1,max … cn,min cn,max ]
    //
    //  Note that only one pair of c values should be specified if a Function entry
    //  is present."
    auto decode_array = TRY(shading_dict->get_array(document, CommonNames::Decode));
    size_t number_of_components = static_cast<size_t>(shading_dict->contains(CommonNames::Function) ? 1 : common_entries.color_space->number_of_components());
    if (decode_array->size() != 4 + 2 * number_of_components)
        return Error::malformed_error("Decode array must have 4 + 2 * number of components elements");
    Vector<float> decode;
    decode.resize(decode_array->size());
    for (size_t i = 0; i < decode_array->size(); ++i)
        decode[i] = decode_array->at(i).to_float();

    // "(Optional) A 1-in, n-out function or an array of n 1-in, 1-out functions
    //  (where n is the number of color components in the shading dictionary’s
    //  color space). If this entry is present, the color data for each vertex must be
    //  specified by a single parametric variable rather than by n separate color
    //  components. The designated function(s) are called with each interpolated
    //  value of the parametric variable to determine the actual color at each
    //  point. Each input value is forced into the range interval specified for the
    //  corresponding color component in the shading dictionary’s Decode array.
    //  Each function’s domain must be a superset of that interval. If the value re-
    //  turned by the function for a given color component is out of range, it is
    //  adjusted to the nearest valid value.
    //  This entry may not be used with an Indexed color space."
    ShadingFunctionsType functions;
    if (shading_dict->contains(CommonNames::Function))
        functions = TRY(read_shading_functions(document, shading_dict, common_entries.color_space, decode[4]));

    // See "Type 6 Shadings (Coons Patch Meshes)" in the PDF 1.7 spec for a description of the stream contents.
    auto stream = FixedMemoryStream { shading_stream->bytes() };
    BigEndianInputBitStream bitstream { MaybeOwned { stream } };

    Vector<float> patch_data;
    Vector<TensorProductPatch> patches;

    auto read_point = [&]() -> ErrorOr<void> {
        u32 x = TRY(bitstream.read_bits<u32>(bits_per_coordinate));
        u32 y = TRY(bitstream.read_bits<u32>(bits_per_coordinate));
        TRY(patch_data.try_append(mix(decode[0], decode[1], x / (powf(2.0f, bits_per_coordinate) - 1))));
        TRY(patch_data.try_append(mix(decode[2], decode[3], y / (powf(2.0f, bits_per_coordinate) - 1))));
        return {};
    };

    auto read_points = [&](u32 n) -> ErrorOr<void> {
        for (u32 i = 0; i < n; ++i)
            TRY(read_point());
        return {};
    };

    auto read_color = [&]() -> ErrorOr<void> {
        for (size_t i = 0; i < number_of_components; ++i) {
            u16 color = TRY(bitstream.read_bits<u16>(bits_per_component));
            TRY(patch_data.try_append(mix(decode[4 + 2 * i], decode[4 + 2 * i + 1], color / (powf(2.0f, bits_per_component) - 1))));
        }
        return {};
    };

    auto read_colors = [&](u32 n) -> ErrorOr<void> {
        for (u32 i = 0; i < n; ++i)
            TRY(read_color());
        return {};
    };

    // "The coordinates of the control points in a tensor-product patch are actually spec-
    //  ified in the shading’s data stream in the following order:
    //  4 5 6 7
    //  3 14 15 8
    //  2 13 16 9
    //  1 12 11 10"
    // We need to invert this to map data stream index to control point index.
    u32 const patch_index[] = {
        // clang-format off
        0, 4, 8, 12,
        13, 14, 15,
        11, 7, 3,
        2, 1,
        5, 9, 10, 6,
        // clang-format on
    };
    u32 const color_index[] = { 0, 2, 3, 1 };

    // "The 16 control points can be arranged in a
    //  4-by-4 array indexed by row and column, as follows (see Figure 4.24):
    //  p03 p13 p23 p33
    //  p02 p12 p22 p32
    //  p01 p11 p21 p31
    //  p00 p10 p20 p30"

    while (!bitstream.is_eof()) {
        u8 flag = TRY(bitstream.read_bits<u8>(bits_per_flag));

        int n = patch_data.size();
        TensorProductPatch patch;

        // "TABLE 4.36 Data values in a tensor-product patch mesh"
        switch (flag) {
        case 0:
            // "x00 y00 x01 y01 x02 y02 x03 y03 x13 y13 x23 y23 x33 y33 x32 y32
            //  x31 y31 x30 y30 x20 y20 x10 y10 x11 y11 x12 y12 x22 y22 x21 y21
            //  c00 c03 c33 c30
            //  New patch; no implicit values"
            TRY(patch_data.try_ensure_capacity(patch_data.size() + 16 * 2 + 4 + number_of_components));
            TRY(read_points(16));
            TRY(read_colors(4));
            for (int i = 0; i < 16; ++i)
                patch.control_points[patch_index[i]] = n + 2 * i;
            for (int i = 0; i < 4; ++i)
                patch.colors[color_index[i]] = n + 32 + number_of_components * i;
            break;
        case 1:
            if (patches.is_empty())
                return Error::malformed_error("Edge flag 1 without preceding patch");
            // "x13 y13 x23 y23 x33 y33 x32 y32 x31 y31 x30 y30
            //  x20 y20 x10 y10 x11 y11 x12 y12 x22 y22 x21 y21
            //  c33 c30
            //  Implicit values:
            //  (x00, y00) = (x03, y03) previous
            //  (x01, y01) = (x13, y13) previous
            //  (x02, y02) = (x23, y23) previous
            //  (x03, y03) = (x33, y33) previous
            //  c00 = c03 previous
            //  c03 = c33 previous"
            TRY(patch_data.try_ensure_capacity(patch_data.size() + 12 * 2 + 2 + number_of_components));
            TRY(read_points(12));
            TRY(read_colors(2));
            patch.control_points[patch_index[0]] = patches.last().control_points[12];
            patch.control_points[patch_index[1]] = patches.last().control_points[13];
            patch.control_points[patch_index[2]] = patches.last().control_points[14];
            patch.control_points[patch_index[3]] = patches.last().control_points[15];
            for (int i = 0; i < 12; ++i)
                patch.control_points[patch_index[i + 4]] = n + 2 * i;
            patch.colors[color_index[0]] = patches.last().colors[2];
            patch.colors[color_index[1]] = patches.last().colors[3];
            for (int i = 0; i < 2; ++i)
                patch.colors[color_index[i + 2]] = n + 24 + number_of_components * i;
            break;
        case 2:
            if (patches.is_empty())
                return Error::malformed_error("Edge flag 2 without preceding patch");
            // "x13 y13 x23 y23 x33 y33 x32 y32 x31 y31 x30 y30
            //  x20 y20 x10 y10 x11 y11 x12 y12 x22 y22 x21 y21
            //  c33 c30
            //  Implicit values:
            //  (x00, y00) = (x33, y33) previous
            //  (x01, y01) = (x32, y32) previous
            //  (x02, y02) = (x31, y31) previous
            //  (x03, y03) = (x30, y30) previous
            //  c00 = c33 previous
            //  c03 = c30 previous"
            TRY(patch_data.try_ensure_capacity(patch_data.size() + 12 * 2 + 2 + number_of_components));
            TRY(read_points(12));
            TRY(read_colors(2));
            patch.control_points[patch_index[0]] = patches.last().control_points[15];
            patch.control_points[patch_index[1]] = patches.last().control_points[11];
            patch.control_points[patch_index[2]] = patches.last().control_points[7];
            patch.control_points[patch_index[3]] = patches.last().control_points[3];
            for (int i = 0; i < 12; ++i)
                patch.control_points[patch_index[i + 4]] = n + 2 * i;
            patch.colors[color_index[0]] = patches.last().colors[3];
            patch.colors[color_index[1]] = patches.last().colors[1];
            for (int i = 0; i < 2; ++i)
                patch.colors[color_index[i + 2]] = n + 24 + number_of_components * i;
            break;
        case 3:
            if (patches.is_empty())
                return Error::malformed_error("Edge flag 3 without preceding patch");
            // "x13 y13 x23 y23 x33 y33 x32 y32 x31 y31 x30 y30
            //  x20 y20 x10 y10 x11 y11 x12 y12 x22 y22 x21 y21
            //  c33 c30
            //  Implicit values:
            //  (x00, y00) = (x30, y30) previous
            //  (x01, y01) = (x20, y20) previous
            //  (x02, y02) = (x10, y10) previous
            //  (x03, y03) = (x00, y00) previous
            //  c00 = c30 previous
            //  c03 = c00 previous"
            TRY(patch_data.try_ensure_capacity(patch_data.size() + 12 * 2 + 2 + number_of_components));
            TRY(read_points(12));
            TRY(read_colors(2));
            patch.control_points[patch_index[0]] = patches.last().control_points[3];
            patch.control_points[patch_index[1]] = patches.last().control_points[2];
            patch.control_points[patch_index[2]] = patches.last().control_points[1];
            patch.control_points[patch_index[3]] = patches.last().control_points[0];
            for (int i = 0; i < 12; ++i)
                patch.control_points[patch_index[i + 4]] = n + 2 * i;
            patch.colors[color_index[0]] = patches.last().colors[1];
            patch.colors[color_index[1]] = patches.last().colors[0];
            for (int i = 0; i < 2; ++i)
                patch.colors[color_index[i + 2]] = n + 24 + number_of_components * i;
            break;
        default:
            return Error::malformed_error("Invalid edge flag");
        }

        TRY(patches.try_append(patch));
        bitstream.align_to_byte_boundary();
    }

    GouraudBounds bounds = bounds_from_decode_array(decode.span().slice(4));
    return adopt_ref(*new TensorProductPatchShading(move(common_entries), move(patch_data), move(patches), move(functions), move(bounds)));
}

PDFErrorOr<void> TensorProductPatchShading::draw(Gfx::Painter& painter, Gfx::AffineTransform const& ctm)
{
    NonnullRefPtr<ColorSpaceWithFloatArgs> color_space = m_common_entries.color_space;
    size_t const number_of_components = !m_functions.has<Empty>() ? 1 : color_space->number_of_components();
    bool is_indexed = color_space->family() == ColorSpaceFamily::Indexed;
    RefPtr<IndexedColorSpace> indexed_color_space;
    auto bounds = m_bounds;
    if (is_indexed) {
        indexed_color_space = static_ptr_cast<IndexedColorSpace>(color_space);
        color_space = indexed_color_space->base_color_space();
        bounds = bounds_from_decode_array(color_space->default_decode());
    }

    for (auto& patch : m_patches) {
        GouraudBezierPatch bezier_patch;

        for (size_t i = 0; i < 16; ++i)
            bezier_patch.points[i] = ctm.map(Gfx::FloatPoint { m_patch_data[patch.control_points[i]], m_patch_data[patch.control_points[i] + 1] });

        for (size_t i = 0; i < 4; ++i) {
            GouraudColor color;

            if (is_indexed) {
                // "If ColorSpace is an Indexed color space, all color values specified in the shading
                //  are immediately converted to the base color space. [...] Interpolation never occurs
                //  in an Indexed color space, which is quantized and therefore inappropriate for calculations
                //  that assume a continuous range of colors."
                color.extend(TRY(indexed_color_space->base_components(m_patch_data[patch.colors[i]])));
            } else {
                color.resize(number_of_components);
                for (size_t j = 0; j < number_of_components; ++j)
                    color[j] = m_patch_data[patch.colors[i] + j];
            }

            bezier_patch.colors[i] = color;
        }

        draw_gouraud_bezier_patch(painter, color_space, m_functions, bezier_patch, bounds);
    }
    return {};
}

}

PDFErrorOr<NonnullRefPtr<Shading>> Shading::create(Document* document, NonnullRefPtr<Object> shading_dict_or_stream, Renderer& renderer)
{
    // "Shading types 4 to 7 are defined by a stream containing descriptive data charac-
    //  terizing the shading’s gradient fill. In these cases, the shading dictionary is also a
    //  stream dictionary and can contain any of the standard entries common to all
    //  streams"
    auto shading_dict = TRY([&]() -> PDFErrorOr<NonnullRefPtr<DictObject>> {
        if (shading_dict_or_stream->is<DictObject>())
            return shading_dict_or_stream->cast<DictObject>();
        if (shading_dict_or_stream->is<StreamObject>())
            return shading_dict_or_stream->cast<StreamObject>()->dict();
        return Error::malformed_error("Shading must be a dictionary or stream");
    }());

    int shading_type = TRY(document->resolve(shading_dict->get_value(CommonNames::ShadingType))).to_int();
    auto common_entries = TRY(read_common_entries(document, *shading_dict, renderer));

    switch (shading_type) {
    case 1:
        if (!shading_dict_or_stream->is<DictObject>())
            return Error::malformed_error("Function-based shading dictionary has wrong type");
        return FunctionBasedShading::create(document, shading_dict, move(common_entries));
    case 2:
        if (!shading_dict_or_stream->is<DictObject>())
            return Error::malformed_error("Axial shading dictionary has wrong type");
        return AxialShading::create(document, shading_dict, move(common_entries));
    case 3:
        if (!shading_dict_or_stream->is<DictObject>())
            return Error::malformed_error("Radial shading dictionary has wrong type");
        return RadialShading::create(document, shading_dict, move(common_entries));
    case 4:
        if (!shading_dict_or_stream->is<StreamObject>())
            return Error::malformed_error("Free-form Gouraud-shaded triangle mesh stream has wrong type");
        return FreeFormGouraudShading::create(document, shading_dict_or_stream->cast<StreamObject>(), move(common_entries));
    case 5:
        if (!shading_dict_or_stream->is<StreamObject>())
            return Error::malformed_error("Lattice-form Gouraud-shaded triangle mesh stream has wrong type");
        return LatticeFormGouraudShading::create(document, shading_dict_or_stream->cast<StreamObject>(), move(common_entries));
    case 6:
        if (!shading_dict_or_stream->is<StreamObject>())
            return Error::malformed_error("Coons patch mesh stream has wrong type");
        return CoonsPatchShading::create(document, shading_dict_or_stream->cast<StreamObject>(), move(common_entries));
    case 7:
        if (!shading_dict_or_stream->is<StreamObject>())
            return Error::malformed_error("Tensor-product patch mesh stream has wrong type");
        return TensorProductPatchShading::create(document, shading_dict_or_stream->cast<StreamObject>(), move(common_entries));
    }
    dbgln("Shading type {}", shading_type);
    return Error::malformed_error("Invalid shading type");
}

}
