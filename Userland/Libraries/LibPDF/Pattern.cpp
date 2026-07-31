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

namespace PDF {

PDFErrorOr<ColorOrStyle> Pattern::style(Document*, NonnullRefPtr<Object> pattern, Renderer& renderer)
{
    NonnullRefPtr<DictObject> pattern_dict = [&] {
        // Shading patterns do not have a content stream.
        if (pattern->is<DictObject>())
            return pattern->cast<DictObject>();
        return pattern->cast<StreamObject>()->dict();
    }();

    // PDF 1.7 spec, 4.6 Patterns
    // TABLE 4.25 Additional entries specific to a type 1 pattern dictionary
    // TABLE 4.26 Entries in a type 2 pattern dictionary
    // Both have:
    // - /Type (optional)
    // - /PatternType (required)
    // - /Matrix (optional)
    // The other entries are specific to the pattern type.

    // PatternType (Required) A code identifying the type of pattern that this dictionary describes;
    // shall be 1 for a tiling pattern
    auto const pattern_type = pattern_dict->get(CommonNames::PatternType)->get_u16();
    if (pattern_type != 1)
        return Error::rendering_unsupported_error("Unsupported pattern type {}", pattern_type);

    // Type (Optional) The type of PDF object that this dictionary describes;
    // if present, shall be Pattern for a pattern dictionary
    auto const type = pattern_dict->get(CommonNames::Type);
    if (type.has_value()) {
        auto type_name = type->get<NonnullRefPtr<Object>>()->cast<NameObject>();
        if (type_name->name() != CommonNames::Pattern)
            return Error::rendering_unsupported_error("Unsupported pattern type {}", type_name->name());
    }

    // PaintType (Required) A code that determines how the colour of the pattern cell shall be specified
    auto const pattern_paint_type = pattern_dict->get("PaintType")->get_u16();
    if (pattern_paint_type != 1)
        return Error::rendering_unsupported_error("Unsupported pattern paint type {}", pattern_paint_type);

    // Matrix (Optional) An array of six numbers specifying the pattern matrix
    Vector<Value> pattern_matrix;
    if (pattern_dict->contains(CommonNames::Matrix)) {
        pattern_matrix = pattern_dict->get_array(renderer.m_document, CommonNames::Matrix).value()->elements();
    } else {
        pattern_matrix = Vector { Value { 1 }, Value { 0 }, Value { 0 }, Value { 1 }, Value { 0 }, Value { 0 } };
    }

    auto pattern_bounding_box = pattern_dict->get_array(renderer.m_document, "BBox").value()->elements();
    auto pattern_transform = Gfx::AffineTransform(
        pattern_matrix[0].to_float(),
        pattern_matrix[1].to_float(),
        pattern_matrix[2].to_float(),
        pattern_matrix[3].to_float(),
        pattern_matrix[4].to_float(),
        pattern_matrix[5].to_float());

    // To get the device space size for the bitmap, apply the pattern transform to the pattern space bounding box, and then apply the initial ctm.
    // NB: the pattern pattern_matrix maps pattern space to the default (initial) coordinate space of the page. (i.e cannot be updated via cm).

    auto initial_ctm = Gfx::AffineTransform(renderer.m_graphics_state_stack.first().ctm);
    initial_ctm.set_translation(0, 0);
    initial_ctm.set_scale(initial_ctm.x_scale(), initial_ctm.y_scale());

    auto pattern_space_lower_left = Gfx::FloatPoint { pattern_bounding_box[0].to_int(), pattern_bounding_box[1].to_int() };
    auto pattern_space_upper_right = Gfx::FloatPoint { pattern_bounding_box[2].to_int(), pattern_bounding_box[3].to_int() };

    auto device_space_lower_left = initial_ctm.map(pattern_transform.map(pattern_space_lower_left));
    auto device_space_upper_right = initial_ctm.map(pattern_transform.map(pattern_space_upper_right));

    auto bitmap_width = abs((int)device_space_upper_right.x() - (int)device_space_lower_left.x());
    auto bitmap_height = abs((int)device_space_upper_right.y() - (int)device_space_lower_left.y());

    auto pattern_cell = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { bitmap_width, bitmap_height }));
    auto page = Page(renderer.m_page);
    page.media_box = Rectangle {
        pattern_space_lower_left.x(), pattern_space_lower_left.y(),
        pattern_space_upper_right.x(), pattern_space_upper_right.y()
    };
    page.crop_box = page.media_box;

    // (Required) A resource dictionary containing all of the named resources required by the pattern’s content stream.
    // FIXME: This is technically required, but `patterns.pdf` omits it (and it is accepted by Chrome and FF/LibPDF.js).
    Optional<NonnullRefPtr<DictObject>> pattern_resources {};
    if (pattern_dict->contains(CommonNames::Resources))
        pattern_resources = TRY(pattern_dict->get_dict(renderer.m_document, CommonNames::Resources));

    auto pattern_renderer = Renderer(renderer.m_document, page, pattern_cell, {}, renderer.m_rendering_preferences);
    auto operators = TRY(Parser::parse_operators(renderer.m_document, pattern->cast<StreamObject>()->bytes()));
    for (auto& op : operators)
        TRY(pattern_renderer.handle_operator(op, pattern_resources));

    auto x_steps = pattern_dict->get("XStep").value_or(bitmap_width).to_int();
    auto y_steps = pattern_dict->get("YStep").value_or(bitmap_height).to_int();

    auto device_space_steps = initial_ctm.map(pattern_transform.map(Gfx::IntPoint { x_steps, y_steps }));

    NonnullRefPtr<Gfx::PaintStyle> style = MUST(Gfx::RepeatingBitmapPaintStyle::create(
        *pattern_cell,
        device_space_steps,
        {}));

    return style;
}

}
