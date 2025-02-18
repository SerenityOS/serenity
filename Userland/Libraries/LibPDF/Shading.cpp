/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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
    AK::NonnullRefPtr<ColorSpace> color_space;

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
    auto color_space_object = TRY(shading_dict.get_object(document, CommonNames::ColorSpace));
    auto color_space = TRY(ColorSpace::create(document, move(color_space_object), renderer));

    CommonEntries common_entries { .color_space = color_space };

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
        Gfx::FloatRect bbox {
            bbox_array->at(0).to_float(),
            bbox_array->at(1).to_float(),
            bbox_array->at(2).to_float(),
            bbox_array->at(3).to_float(),
        };
        common_entries.b_box = bbox;
    }

    if (shading_dict.contains(CommonNames::AntiAlias))
        common_entries.anti_alias = TRY(document->resolve(shading_dict.get_value(CommonNames::AntiAlias))).get<bool>();

    return common_entries;
}

class AxialShading final : public Shading {
public:
    static PDFErrorOr<NonnullRefPtr<AxialShading>> create(Document*, NonnullRefPtr<DictObject>, CommonEntries);

    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    using FunctionsType = Variant<NonnullRefPtr<Function>, Vector<NonnullRefPtr<Function>>>;

    AxialShading(CommonEntries common_entries, Gfx::FloatPoint start, Gfx::FloatPoint end, float t0, float t1, FunctionsType functions, bool extend_start, bool extend_end)
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
    FunctionsType m_functions;
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
    FunctionsType functions = TRY([&]() -> PDFErrorOr<FunctionsType> {
        auto function_object = TRY(shading_dict->get_object(document, CommonNames::Function));
        if (function_object->is<ArrayObject>()) {
            auto function_array = function_object->cast<ArrayObject>();
            Vector<NonnullRefPtr<Function>> functions_vector;
            if (function_array->size() != static_cast<size_t>(common_entries.color_space->number_of_components()))
                return Error::malformed_error("Function array must have as many elements as color space has components");
            for (size_t i = 0; i < function_array->size(); ++i) {
                auto function = TRY(Function::create(document, TRY(document->resolve_to<Object>(function_array->at(i)))));
                if (TRY(function->evaluate(to_array({ 0.0f }))).size() != 1)
                    return Error::malformed_error("Function must have 1 output component");
                TRY(functions_vector.try_append(move(function)));
            }
            return functions_vector;
        }
        auto function = TRY(Function::create(document, function_object));
        if (TRY(function->evaluate(to_array({ 0.0f }))).size() != static_cast<size_t>(common_entries.color_space->number_of_components()))
            return Error::malformed_error("Function must have as many output components as color space");
        return function;
    }());

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

PDFErrorOr<void> AxialShading::draw(Gfx::Painter& painter, Gfx::AffineTransform const& inverse_ctm)
{
    auto& bitmap = painter.target();

    auto scale = painter.scale();
    auto clip_rect = painter.clip_rect() * scale;

    Vector<float, 4> color_components;
    color_components.resize(m_common_entries.color_space->number_of_components());

    // FIXME: Do something with m_common_entries.b_box if it's set.

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

    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    using FunctionsType = Variant<NonnullRefPtr<Function>, Vector<NonnullRefPtr<Function>>>;

    RadialShading(CommonEntries common_entries, Gfx::FloatPoint start, float start_radius, Gfx::FloatPoint end, float end_radius, float t0, float t1, FunctionsType functions, bool extend_start, bool extend_end)
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
    FunctionsType m_functions;
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
    FunctionsType functions = TRY([&]() -> PDFErrorOr<FunctionsType> {
        auto function_object = TRY(shading_dict->get_object(document, CommonNames::Function));
        if (function_object->is<ArrayObject>()) {
            auto function_array = function_object->cast<ArrayObject>();
            Vector<NonnullRefPtr<Function>> functions_vector;
            if (function_array->size() != static_cast<size_t>(common_entries.color_space->number_of_components()))
                return Error::malformed_error("Function array must have as many elements as color space has components");
            for (size_t i = 0; i < function_array->size(); ++i) {
                auto function = TRY(Function::create(document, TRY(document->resolve_to<Object>(function_array->at(i)))));
                if (TRY(function->evaluate(to_array({ 0.0f }))).size() != 1)
                    return Error::malformed_error("Function must have 1 output component");
                TRY(functions_vector.try_append(move(function)));
            }
            return functions_vector;
        }
        auto function = TRY(Function::create(document, function_object));
        if (TRY(function->evaluate(to_array({ 0.0f }))).size() != static_cast<size_t>(common_entries.color_space->number_of_components()))
            return Error::malformed_error("Function must have as many output components as color space");
        return function;
    }());

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

PDFErrorOr<void> RadialShading::draw(Gfx::Painter&, Gfx::AffineTransform const&)
{
    return Error::rendering_unsupported_error("Cannot draw radial shading yet");
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
        return Error::rendering_unsupported_error("Function-based shading not yet implemented");
    case 2:
        if (!shading_dict_or_stream->is<DictObject>())
            return Error::malformed_error("Axial shading dictionary has wrong type");
        return AxialShading::create(document, shading_dict, move(common_entries));
    case 3:
        if (!shading_dict_or_stream->is<DictObject>())
            return Error::malformed_error("Radial shading dictionary has wrong type");
        return RadialShading::create(document, shading_dict, move(common_entries));
    case 4:
        return Error::rendering_unsupported_error("Free-form Gouraud-shaded triangle mesh not yet implemented");
    case 5:
        return Error::rendering_unsupported_error("Lattice-form Gouraud-shaded triangle mesh not yet implemented");
    case 6:
        return Error::rendering_unsupported_error("Coons patch mesh not yet implemented");
    case 7:
        return Error::rendering_unsupported_error("Tensor-product patch mesh not yet implemented");
    }
    dbgln("Shading type {}", shading_type);
    return Error::malformed_error("Invalid shading type");
}

}
