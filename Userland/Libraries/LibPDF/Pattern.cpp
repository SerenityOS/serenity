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

    // "(Required) A shading object ([...]]) defining the shading pattern’s gradi-
    //  ent fill."
    auto shading_object = TRY(pattern_dict->get_object(document, CommonNames::Shading));

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
    return m_shading->draw(painter, pattern_space_transform);
}

}

PDFErrorOr<bool> Pattern::is_type2(Document* document, NonnullRefPtr<Object> pattern)
{
    if (!pattern->is<DictObject>())
        return false;
    auto pattern_dict = pattern->cast<DictObject>();
    auto const pattern_type = TRY(document->resolve(pattern_dict->get_value(CommonNames::PatternType))).to_int();
    return pattern_type == 2;
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
        VERIFY_NOT_REACHED(); // Tiling patterns use PatternColorSpace::style() for now.
    case 2:
        if (!pattern->is<DictObject>())
            return Error::malformed_error("Type 2 pattern has wrong type");
        return ShadingPattern::create(document, pattern_dict, common_entries, renderer);
    }
    dbgln("Pattern type {}", pattern_type);
    return Error::malformed_error("Invalid pattern type");
}

PDFErrorOr<ColorOrStyle> Pattern::style(Document*, NonnullRefPtr<Object> pattern, Renderer& renderer)
{
    NonnullRefPtr<DictObject> pattern_dict = [&] {
        // Shading patterns do not have a content stream.
        if (pattern->is<DictObject>())
            return pattern->cast<DictObject>();
        return pattern->cast<StreamObject>()->dict();
    }();

    // PatternType (Required) A code identifying the type of pattern that this dictionary describes;
    // shall be 1 for a tiling pattern
    auto const pattern_type = pattern_dict->get(CommonNames::PatternType)->get_u16();
    if (pattern_type != 1)
        return Error::rendering_unsupported_error("Unsupported pattern type {}", pattern_type);

    auto common_entries = TRY(read_common_entries(renderer.m_document, *pattern_dict));

    // PaintType (Required) A code that determines how the colour of the pattern cell shall be specified
    auto const pattern_paint_type = pattern_dict->get("PaintType")->get_u16();
    if (pattern_paint_type != 1)
        return Error::rendering_unsupported_error("Unsupported pattern paint type {}", pattern_paint_type);

    // To get the device space size for the bitmap, apply the pattern transform to the pattern space bounding box, and then apply the initial ctm.
    // NB: the pattern pattern_matrix maps pattern space to the default (initial) coordinate space of the page. (i.e cannot be updated via cm).

    auto initial_ctm = Gfx::AffineTransform(renderer.m_graphics_state_stack.first().ctm);
    initial_ctm.set_translation(0, 0);
    initial_ctm.set_scale(initial_ctm.x_scale(), initial_ctm.y_scale());

    auto pattern_bounding_box = pattern_dict->get_array(renderer.m_document, CommonNames::BBox).value()->elements();

    auto pattern_space_lower_left = Gfx::FloatPoint { pattern_bounding_box[0].to_int(), pattern_bounding_box[1].to_int() };
    auto pattern_space_upper_right = Gfx::FloatPoint { pattern_bounding_box[2].to_int(), pattern_bounding_box[3].to_int() };

    auto device_space_lower_left = initial_ctm.map(common_entries.matrix.map(pattern_space_lower_left));
    auto device_space_upper_right = initial_ctm.map(common_entries.matrix.map(pattern_space_upper_right));

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

    auto device_space_steps = initial_ctm.map(common_entries.matrix.map(Gfx::IntPoint { x_steps, y_steps }));

    NonnullRefPtr<Gfx::PaintStyle> style = MUST(Gfx::RepeatingBitmapPaintStyle::create(
        *pattern_cell,
        device_space_steps,
        {}));

    return style;
}

}
