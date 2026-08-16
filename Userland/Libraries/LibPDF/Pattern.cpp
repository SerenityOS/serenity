/*
 * Copyright (c) 2023, Kyle Pereira <hey@xylepereira.me>
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Pattern.h>
#include <LibPDF/Renderer.h>
#include <LibPDF/Shading.h>

namespace PDF {

namespace {

// Entries present in both:
// TABLE 4.25 Additional entries specific to a type 1 pattern dictionary
// TABLE 4.26 Entries in a type 2 pattern dictionary
struct CommonEntries {
    // "(Optional) An array of six numbers specifying the pattern matrix (see Section
    //  4.6.1, “General Properties of Patterns”). Default value: the identity matrix
    //  [ 1 0 0 1 0 0 ]."
    Gfx::AffineTransform matrix { 1, 0, 0, 1, 0, 0 };
};

PDFErrorOr<CommonEntries> read_common_entries(Document* document, DictObject const& pattern_dict)
{
    // "Type (Optional) The type of PDF object that this dictionary describes;
    //  if present, shall be Pattern for a pattern dictionary.""
    auto const type = pattern_dict.get(CommonNames::Type);
    if (type.has_value()) {
        auto type_name = type->get<NonnullRefPtr<Object>>()->cast<NameObject>();
        if (type_name->name() != CommonNames::Pattern)
            return Error::rendering_unsupported_error("Unsupported pattern type {}", type_name->name());
    }

    CommonEntries common_entries;

    if (pattern_dict.contains(CommonNames::Matrix)) {
        auto pattern_matrix = pattern_dict.get_array(document, CommonNames::Matrix).value()->elements();
        common_entries.matrix = Gfx::AffineTransform(
            pattern_matrix[0].to_float(),
            pattern_matrix[1].to_float(),
            pattern_matrix[2].to_float(),
            pattern_matrix[3].to_float(),
            pattern_matrix[4].to_float(),
            pattern_matrix[5].to_float());
    }

    return common_entries;
}

}

class TilingPattern final : public Pattern {
public:
    static PDFErrorOr<NonnullRefPtr<TilingPattern>> create(Document*, NonnullRefPtr<StreamObject>, CommonEntries, Renderer&);

    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    TilingPattern(CommonEntries common_entries, Page const& page, Gfx::FloatRect bbox, NonnullRefPtr<DictObject> pattern_resources, float x_steps, float y_steps, ReadonlyBytes content_stream, Renderer& renderer)
        : m_common_entries(common_entries)
        , m_page(page)
        , m_bbox(bbox)
        , m_pattern_resources(pattern_resources)
        , m_x_step(x_steps)
        , m_y_step(y_steps)
        , m_content_stream(move(content_stream))
        , m_renderer(renderer)
    {
    }

    CommonEntries m_common_entries;
    Page m_page;
    Gfx::FloatRect m_bbox;
    NonnullRefPtr<DictObject> m_pattern_resources;
    float m_x_step;
    float m_y_step;
    ReadonlyBytes m_content_stream;
    Renderer& m_renderer;
};

PDFErrorOr<NonnullRefPtr<TilingPattern>> TilingPattern::create(Document* document, NonnullRefPtr<StreamObject> pattern, CommonEntries common_entries, Renderer& renderer)
{
    // PDF 1.7 spec, 4.6.2 Tiling Patterns
    // TABLE 4.25 Additional entries specific to a type 1 pattern dictionary

    auto pattern_dict = pattern->dict();

    // "(Required) A code that determines how the colour of the pattern cell shall be specified"
    auto const paint_type = pattern_dict->get("PaintType")->get_u16();
    if (paint_type != 1)
        return Error::rendering_unsupported_error("Unsupported pattern paint type {}", paint_type); // FIXME

    // "(Required) A code that controls adjustments to the spacing of tiles relative to
    //  the device pixel grid"
    auto const tiling_type = pattern_dict->get("TilingType")->get_u16();
    (void)tiling_type; // FIXME: Should we do something with this?

    // "(Required) An array of four numbers in the pattern coordinate system giving
    //  the coordinates of the left, bottom, right, and top edges, respectively, of the
    //  pattern cell’s bounding box."
    auto bbox_object = pattern_dict->get_array(document, CommonNames::BBox).value()->elements();
    auto bbox = Gfx::FloatRect::from_two_points({ bbox_object[0].to_float(), bbox_object[1].to_float() }, { bbox_object[2].to_float(), bbox_object[3].to_float() });

    // "(Required) The desired horizontal spacing between pattern cells, measured in
    //  the pattern coordinate system."
    auto x_step = pattern_dict->get("XStep")->to_float();

    // "(Required) The desired vertical spacing between pattern cells, measured in
    //  the pattern coordinate system."
    auto y_step = pattern_dict->get("YStep")->to_float();

    // "XStep and YStep may be either positive or negative but not zero."
    if (x_step == 0 || y_step == 0)
        return Error::malformed_error("Tiling pattern XStep and YStep must not be zero");

    // "(Required) A resource dictionary containing all of the named resources required by the pattern’s content stream."
    auto pattern_resources = TRY(pattern_dict->get_dict(document, CommonNames::Resources));

    // Matrix is read by read_common_entries().

    return adopt_ref(*new TilingPattern(common_entries, renderer.page(), bbox, pattern_resources, x_step, y_step, pattern->cast<StreamObject>()->bytes(), renderer));
}

[[nodiscard]] static Gfx::IntRect enclosing_tiled_int_rect(Gfx::FloatRect const& float_rect, Gfx::FloatSize tile_size)
{
    int x1 = floorf(float_rect.x() / tile_size.width()) * tile_size.width();
    int y1 = floorf(float_rect.y() / tile_size.height()) * tile_size.height();
    int x2 = ceilf(float_rect.right() / tile_size.width()) * tile_size.width();
    int y2 = ceilf(float_rect.bottom() / tile_size.height()) * tile_size.height();
    return Gfx::IntRect::from_two_points({ x1, y1 }, { x2, y2 });
}

PDFErrorOr<void> TilingPattern::draw(Gfx::Painter& painter, Gfx::AffineTransform const& transform_in)
{
    auto transform = transform_in;
    transform.multiply(m_common_entries.matrix);

    float s = max(transform.x_scale(), transform.y_scale());
    auto bitmap_width = round_to<int>(m_bbox.width() * s);
    auto bitmap_height = round_to<int>(m_bbox.height() * s);

    // FIXME: Cache cell bitmap across pattern paints?
    auto pattern_cell = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { bitmap_width, bitmap_height }));

    m_page.media_box = Rectangle {
        0, 0,
        m_bbox.width(), m_bbox.height()
    };
    m_page.crop_box = m_page.media_box;

    auto pattern_renderer = Renderer(m_renderer.m_document, m_page, pattern_cell, {}, m_renderer.m_rendering_preferences);
    TRY(pattern_renderer.handle_concatenate_matrix(Vector { Value { 1 }, Value { 0 }, Value { 0 }, Value { 1 }, Value { -m_bbox.x() }, Value { -m_bbox.y() } }));
    auto operators = TRY(Parser::parse_operators(m_renderer.m_document, m_content_stream));
    for (auto& op : operators)
        TRY(pattern_renderer.handle_operator(op, m_pattern_resources));

    auto maybe_inverse_transform = transform.inverse();
    if (!maybe_inverse_transform.has_value())
        return Error::malformed_error("Failed to invert tiling pattern transform");
    auto inverse_transform = maybe_inverse_transform.value();

    // Map page's clip rect to pattern space and compute its axis-aligned bounding box in that space.
    auto clip_rect_bounds_in_pattern_space = inverse_transform.map_to_quad(painter.clip_rect().to_type<float>()).bounding_rect();

    auto pattern_rect = enclosing_tiled_int_rect(clip_rect_bounds_in_pattern_space.translated(-m_bbox.location()), { m_x_step, m_y_step });

    for (int y = pattern_rect.top(); y < pattern_rect.bottom(); y += m_y_step) {
        for (int x = pattern_rect.left(); x < pattern_rect.right(); x += m_x_step) {
            Gfx::FloatRect cell_rect = { { m_bbox.x() + x, m_bbox.y() + y }, m_bbox.size() };

            // Cell has (0, 0) in its lower left corner, Gfx::Bitmap in its upper left.
            Gfx::AffineTransform vertical_reflection_matrix = { 1, 0, 0, -1, 0, 2 * cell_rect.y() + cell_rect.height() };
            auto t = transform;
            t.multiply(vertical_reflection_matrix);
            painter.draw_scaled_bitmap_with_transform(Gfx::enclosing_int_rect(cell_rect), pattern_cell, pattern_cell->rect().to_type<float>(), t);
        }
    }

    return {};
}

namespace {

class ShadingPattern final : public Pattern {
public:
    static PDFErrorOr<NonnullRefPtr<ShadingPattern>> create(Document*, NonnullRefPtr<DictObject>, CommonEntries, Renderer&);

    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) override;

private:
    ShadingPattern(CommonEntries common_entries, NonnullRefPtr<Shading> shading)
        : m_common_entries(common_entries)
        , m_shading(move(shading))
    {
    }

    CommonEntries m_common_entries;
    NonnullRefPtr<Shading> m_shading;
};

PDFErrorOr<NonnullRefPtr<ShadingPattern>> ShadingPattern::create(Document* document, NonnullRefPtr<DictObject> pattern_dict, CommonEntries common_entries, Renderer& renderer)
{
    // PDF 1.7 spec, 4.6.3 Shading Patterns
    // TABLE 4.26 Entries in a type 2 pattern dictionary

    // "(Required) A shading object ([...]]) defining the shading pattern’s gradi-
    //  ent fill."
    auto shading_object = TRY(pattern_dict->get_object(document, CommonNames::Shading));

    // Matrix is read by read_common_entries().

    // "(Optional) A graphics state parameter dictionary (see Section 4.3.4, “Graph-
    //  ics State Parameter Dictionaries”) containing graphics state parameters to be
    //  put into effect temporarily while the shading pattern is painted."
    if (pattern_dict->contains(CommonNames::ExtGState)) {
        // FIXME: Implement.
    }

    auto shading = TRY(Shading::create(document, shading_object, renderer));
    return adopt_ref(*new ShadingPattern(common_entries, move(shading)));
}

PDFErrorOr<void> ShadingPattern::draw(Gfx::Painter& painter, Gfx::AffineTransform const& transform)
{
    auto pattern_space_transform = transform;
    pattern_space_transform.multiply(m_common_entries.matrix);
    if (auto background = TRY(m_shading->background_for_pattern()); background.has_value())
        painter.fill_rect(painter.clip_rect(), background.value());
    return m_shading->draw(painter, pattern_space_transform);
}

}

PDFErrorOr<NonnullRefPtr<Pattern>> Pattern::create(Document* document, NonnullRefPtr<Object> pattern, Renderer& renderer)
{
    auto pattern_dict = TRY([&]() -> PDFErrorOr<NonnullRefPtr<DictObject>> {
        if (pattern->is<DictObject>())
            return pattern->cast<DictObject>();
        if (pattern->is<StreamObject>())
            return pattern->cast<StreamObject>()->dict();
        return Error::malformed_error("Pattern must be a dictionary or stream");
    }());

    int pattern_type = TRY(document->resolve(pattern_dict->get_value(CommonNames::PatternType))).to_int();
    auto common_entries = TRY(read_common_entries(document, *pattern_dict));

    switch (pattern_type) {
    case 1:
        if (!pattern->is<StreamObject>())
            return Error::malformed_error("Type 1 pattern has wrong type");
        return TilingPattern::create(document, pattern->cast<StreamObject>(), common_entries, renderer);
    case 2:
        if (!pattern->is<DictObject>())
            return Error::malformed_error("Type 2 pattern has wrong type");
        return ShadingPattern::create(document, pattern_dict, common_entries, renderer);
    }
    dbgln("Pattern type {}", pattern_type);
    return Error::malformed_error("Invalid pattern type");
}

}
