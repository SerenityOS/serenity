/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/PDFFont.h>
#include <LibPDF/Renderer.h>

#define RENDERER_HANDLER(name) \
    PDFErrorOr<void> Renderer::handle_##name([[maybe_unused]] Vector<Value> const& args)

#define RENDERER_TODO(name)                                         \
    RENDERER_HANDLER(name)                                          \
    {                                                               \
        dbgln("[PDF::Renderer] Unsupported draw operation " #name); \
        TODO();                                                     \
    }

namespace PDF {

PDFErrorOr<void> Renderer::render(Document& document, Page const& page, RefPtr<Gfx::Bitmap> bitmap)
{
    return Renderer(document, page, bitmap).render();
}

Renderer::Renderer(RefPtr<Document> document, Page const& page, RefPtr<Gfx::Bitmap> bitmap)
    : m_document(document)
    , m_bitmap(bitmap)
    , m_page(page)
    , m_painter(*bitmap)
    , m_anti_aliasing_painter(m_painter)
{
    auto media_box = m_page.media_box;

    Gfx::AffineTransform userspace_matrix;
    userspace_matrix.translate(media_box.lower_left_x, media_box.lower_left_y);

    float width = media_box.width();
    float height = media_box.height();
    float scale_x = static_cast<float>(bitmap->width()) / width;
    float scale_y = static_cast<float>(bitmap->height()) / height;
    userspace_matrix.scale(scale_x, scale_y);

    // PDF user-space coordinate y axis increases from bottom to top, so we have to
    // insert a horizontal reflection about the vertical midpoint into our transformation
    // matrix

    static Gfx::AffineTransform horizontal_reflection_matrix = { 1, 0, 0, -1, 0, 0 };

    userspace_matrix.multiply(horizontal_reflection_matrix);
    userspace_matrix.translate(0.0f, -height);

    m_graphics_state_stack.append(GraphicsState { userspace_matrix });

    m_bitmap->fill(Gfx::Color::NamedColor::White);
}

PDFErrorOr<void> Renderer::render()
{
    // Use our own vector, as the /Content can be an array with multiple
    // streams which gets concatenated
    // FIXME: Text operators are supposed to only have effects on the current
    // stream object. Do the text operators treat this concatenated stream
    // as one stream or multiple?
    ByteBuffer byte_buffer;

    if (m_page.contents->is<ArrayObject>()) {
        auto contents = m_page.contents->cast<ArrayObject>();
        for (auto& ref : *contents) {
            auto bytes = TRY(m_document->resolve_to<StreamObject>(ref))->bytes();
            byte_buffer.append(bytes.data(), bytes.size());
        }
    } else {
        auto bytes = m_page.contents->cast<StreamObject>()->bytes();
        byte_buffer.append(bytes.data(), bytes.size());
    }

    auto operators = TRY(Parser::parse_operators(m_document, byte_buffer));

    for (auto& op : operators)
        TRY(handle_operator(op));

    return {};
}

PDFErrorOr<void> Renderer::handle_operator(Operator const& op)
{
    switch (op.type()) {
#define V(name, snake_name, symbol)               \
    case OperatorType::name:                      \
        TRY(handle_##snake_name(op.arguments())); \
        break;
        ENUMERATE_OPERATORS(V)
#undef V
    case OperatorType::TextNextLineShowString:
        TRY(handle_text_next_line_show_string(op.arguments()));
        break;
    case OperatorType::TextNextLineShowStringSetSpacing:
        TRY(handle_text_next_line_show_string_set_spacing(op.arguments()));
        break;
    }

    return {};
}

RENDERER_HANDLER(save_state)
{
    m_graphics_state_stack.append(state());
    return {};
}

RENDERER_HANDLER(restore_state)
{
    m_graphics_state_stack.take_last();
    return {};
}

RENDERER_HANDLER(concatenate_matrix)
{
    Gfx::AffineTransform new_transform(
        args[0].to_float(),
        args[1].to_float(),
        args[2].to_float(),
        args[3].to_float(),
        args[4].to_float(),
        args[5].to_float());

    state().ctm.multiply(new_transform);
    m_text_rendering_matrix_is_dirty = true;
    return {};
}

RENDERER_HANDLER(set_line_width)
{
    state().line_width = args[0].to_float();
    return {};
}

RENDERER_HANDLER(set_line_cap)
{
    state().line_cap_style = static_cast<LineCapStyle>(args[0].get<int>());
    return {};
}

RENDERER_HANDLER(set_line_join)
{
    state().line_join_style = static_cast<LineJoinStyle>(args[0].get<int>());
    return {};
}

RENDERER_HANDLER(set_miter_limit)
{
    state().miter_limit = args[0].to_float();
    return {};
}

RENDERER_HANDLER(set_dash_pattern)
{
    auto dash_array = MUST(m_document->resolve_to<ArrayObject>(args[0]));
    Vector<int> pattern;
    for (auto& element : *dash_array)
        pattern.append(element.get<int>());
    state().line_dash_pattern = LineDashPattern { pattern, args[1].get<int>() };
    return {};
}

RENDERER_TODO(set_color_rendering_intent)
RENDERER_TODO(set_flatness_tolerance)

RENDERER_HANDLER(set_graphics_state_from_dict)
{
    VERIFY(m_page.resources->contains(CommonNames::ExtGState));
    auto dict_name = MUST(m_document->resolve_to<NameObject>(args[0]))->name();
    auto ext_gstate_dict = MUST(m_page.resources->get_dict(m_document, CommonNames::ExtGState));
    auto target_dict = MUST(ext_gstate_dict->get_dict(m_document, dict_name));
    TRY(set_graphics_state_from_dict(target_dict));
    return {};
}

RENDERER_HANDLER(path_move)
{
    m_current_path.move_to(map(args[0].to_float(), args[1].to_float()));
    return {};
}

RENDERER_HANDLER(path_line)
{
    VERIFY(!m_current_path.segments().is_empty());
    m_current_path.line_to(map(args[0].to_float(), args[1].to_float()));
    return {};
}

RENDERER_TODO(path_cubic_bezier_curve)
RENDERER_TODO(path_cubic_bezier_curve_no_first_control)
RENDERER_TODO(path_cubic_bezier_curve_no_second_control)

RENDERER_HANDLER(path_close)
{
    m_current_path.close();
    return {};
}

RENDERER_HANDLER(path_append_rect)
{
    auto pos = map(args[0].to_float(), args[1].to_float());
    auto size = map(Gfx::FloatSize { args[2].to_float(), args[3].to_float() });

    // FIXME: Why do we need to flip the y axis of rectangles here? The coordinates
    //        in the PDF file seem to be correct, with the same flipped-ness as
    //        everything else in a PDF file.
    pos.set_y(m_bitmap->height() - pos.y() - size.height());

    m_current_path.move_to(pos);
    m_current_path.line_to({ pos.x() + size.width(), pos.y() });
    m_current_path.line_to({ pos.x() + size.width(), pos.y() + size.height() });
    m_current_path.line_to({ pos.x(), pos.y() + size.height() });
    m_current_path.close();
    return {};
}

RENDERER_HANDLER(path_stroke)
{
    m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_color, state().line_width);
    m_current_path.clear();
    return {};
}

RENDERER_HANDLER(path_close_and_stroke)
{
    m_current_path.close();
    TRY(handle_path_stroke(args));
    return {};
}

RENDERER_HANDLER(path_fill_nonzero)
{
    m_anti_aliasing_painter.fill_path(m_current_path, state().paint_color, Gfx::Painter::WindingRule::Nonzero);
    m_current_path.clear();
    return {};
}

RENDERER_HANDLER(path_fill_nonzero_deprecated)
{
    TRY(handle_path_fill_nonzero(args));
    return {};
}

RENDERER_HANDLER(path_fill_evenodd)
{
    m_anti_aliasing_painter.fill_path(m_current_path, state().paint_color, Gfx::Painter::WindingRule::EvenOdd);
    m_current_path.clear();
    return {};
}

RENDERER_HANDLER(path_fill_stroke_nonzero)
{
    m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_color, state().line_width);
    TRY(handle_path_fill_nonzero(args));
    return {};
}

RENDERER_HANDLER(path_fill_stroke_evenodd)
{
    m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_color, state().line_width);
    TRY(handle_path_fill_evenodd(args));
    return {};
}

RENDERER_HANDLER(path_close_fill_stroke_nonzero)
{
    m_current_path.close();
    TRY(handle_path_fill_stroke_nonzero(args));
    return {};
}

RENDERER_HANDLER(path_close_fill_stroke_evenodd)
{
    m_current_path.close();
    TRY(handle_path_fill_stroke_evenodd(args));
    return {};
}

RENDERER_HANDLER(path_end)
{
    return {};
}

RENDERER_HANDLER(path_intersect_clip_nonzero)
{
    // FIXME: Support arbitrary path clipping in the painter and utilize that here
    auto bounding_box = map(m_current_path.bounding_box());
    m_painter.add_clip_rect(bounding_box.to_type<int>());
    return {};
}

RENDERER_HANDLER(path_intersect_clip_evenodd)
{
    // FIXME: Support arbitrary path clipping in the painter and utilize that here
    auto bounding_box = map(m_current_path.bounding_box());
    m_painter.add_clip_rect(bounding_box.to_type<int>());
    return {};
}

RENDERER_HANDLER(text_begin)
{
    m_text_matrix = Gfx::AffineTransform();
    m_text_line_matrix = Gfx::AffineTransform();
    return {};
}

RENDERER_HANDLER(text_end)
{
    // FIXME: Do we need to do anything here?
    return {};
}

RENDERER_HANDLER(text_set_char_space)
{
    text_state().character_spacing = args[0].to_float();
    return {};
}

RENDERER_HANDLER(text_set_word_space)
{
    text_state().word_spacing = args[0].to_float();
    return {};
}

RENDERER_HANDLER(text_set_horizontal_scale)
{
    m_text_rendering_matrix_is_dirty = true;
    text_state().horizontal_scaling = args[0].to_float() / 100.0f;
    return {};
}

RENDERER_HANDLER(text_set_leading)
{
    text_state().leading = args[0].to_float();
    return {};
}

RENDERER_HANDLER(text_set_font)
{
    auto target_font_name = MUST(m_document->resolve_to<NameObject>(args[0]))->name();
    auto fonts_dictionary = MUST(m_page.resources->get_dict(m_document, CommonNames::Font));
    auto font_dictionary = MUST(fonts_dictionary->get_dict(m_document, target_font_name));
    auto font = TRY(PDFFont::create(m_document, font_dictionary));
    text_state().font = font;

    // FIXME: We do not yet have the standard 14 fonts, as some of them are not open fonts,
    //        so we just use LiberationSerif for everything

    auto font_name = MUST(font_dictionary->get_name(m_document, CommonNames::BaseFont))->name().to_lowercase();
    auto font_view = font_name.view();
    bool is_bold = font_view.contains("bold");
    bool is_italic = font_view.contains("italic");

    String font_variant;

    if (is_bold && is_italic) {
        font_variant = "BoldItalic";
    } else if (is_bold) {
        font_variant = "Bold";
    } else if (is_italic) {
        font_variant = "Italic";
    } else {
        font_variant = "Regular";
    }

    text_state().font_size = args[1].to_float();
    text_state().font_variant = font_variant;

    m_text_rendering_matrix_is_dirty = true;
    return {};
}

RENDERER_HANDLER(text_set_rendering_mode)
{
    text_state().rendering_mode = static_cast<TextRenderingMode>(args[0].get<int>());
    return {};
}

RENDERER_HANDLER(text_set_rise)
{
    m_text_rendering_matrix_is_dirty = true;
    text_state().rise = args[0].to_float();
    return {};
}

RENDERER_HANDLER(text_next_line_offset)
{
    Gfx::AffineTransform transform(1.0f, 0.0f, 0.0f, 1.0f, args[0].to_float(), args[1].to_float());
    m_text_line_matrix.multiply(transform);
    m_text_matrix = m_text_line_matrix;
    return {};
}

RENDERER_HANDLER(text_next_line_and_set_leading)
{
    text_state().leading = -args[1].to_float();
    TRY(handle_text_next_line_offset(args));
    return {};
}

RENDERER_HANDLER(text_set_matrix_and_line_matrix)
{
    Gfx::AffineTransform new_transform(
        args[0].to_float(),
        args[1].to_float(),
        args[2].to_float(),
        args[3].to_float(),
        args[4].to_float(),
        args[5].to_float());
    m_text_line_matrix = new_transform;
    m_text_matrix = new_transform;
    m_text_rendering_matrix_is_dirty = true;
    return {};
}

RENDERER_HANDLER(text_next_line)
{
    TRY(handle_text_next_line_offset({ 0.0f, -text_state().leading }));
    return {};
}

RENDERER_HANDLER(text_show_string)
{
    auto text = MUST(m_document->resolve_to<StringObject>(args[0]))->string();
    show_text(text);
    return {};
}

RENDERER_HANDLER(text_next_line_show_string)
{
    TRY(handle_text_next_line(args));
    TRY(handle_text_show_string(args));
    return {};
}

RENDERER_TODO(text_next_line_show_string_set_spacing)

RENDERER_HANDLER(text_show_string_array)
{
    auto elements = MUST(m_document->resolve_to<ArrayObject>(args[0]))->elements();
    float next_shift = 0.0f;

    for (auto& element : elements) {
        if (element.has<int>()) {
            next_shift = element.get<int>();
        } else if (element.has<float>()) {
            next_shift = element.get<float>();
        } else {
            auto str = element.get<NonnullRefPtr<Object>>()->cast<StringObject>()->string();
            show_text(str, next_shift);
        }
    }

    return {};
}

RENDERER_TODO(type3_font_set_glyph_width)
RENDERER_TODO(type3_font_set_glyph_width_and_bbox)

RENDERER_HANDLER(set_stroking_space)
{
    state().stroke_color_space = TRY(get_color_space(args[0]));
    VERIFY(state().stroke_color_space);
    return {};
}

RENDERER_HANDLER(set_painting_space)
{
    state().paint_color_space = TRY(get_color_space(args[0]));
    VERIFY(state().paint_color_space);
    return {};
}

RENDERER_HANDLER(set_stroking_color)
{
    state().stroke_color = state().stroke_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_stroking_color_extended)
{
    // FIXME: Handle Pattern color spaces
    auto last_arg = args.last();
    if (last_arg.has<NonnullRefPtr<Object>>() && last_arg.get<NonnullRefPtr<Object>>()->is<NameObject>())
        TODO();

    state().stroke_color = state().stroke_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_painting_color)
{
    state().paint_color = state().paint_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_painting_color_extended)
{
    // FIXME: Handle Pattern color spaces
    auto last_arg = args.last();
    if (last_arg.has<NonnullRefPtr<Object>>() && last_arg.get<NonnullRefPtr<Object>>()->is<NameObject>())
        TODO();

    state().paint_color = state().paint_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_stroking_color_and_space_to_gray)
{
    state().stroke_color_space = DeviceGrayColorSpace::the();
    state().stroke_color = state().stroke_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_painting_color_and_space_to_gray)
{
    state().paint_color_space = DeviceGrayColorSpace::the();
    state().paint_color = state().paint_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_stroking_color_and_space_to_rgb)
{
    state().stroke_color_space = DeviceRGBColorSpace::the();
    state().stroke_color = state().stroke_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_painting_color_and_space_to_rgb)
{
    state().paint_color_space = DeviceRGBColorSpace::the();
    state().paint_color = state().paint_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_stroking_color_and_space_to_cmyk)
{
    state().stroke_color_space = DeviceCMYKColorSpace::the();
    state().stroke_color = state().stroke_color_space->color(args);
    return {};
}

RENDERER_HANDLER(set_painting_color_and_space_to_cmyk)
{
    state().paint_color_space = DeviceCMYKColorSpace::the();
    state().paint_color = state().paint_color_space->color(args);
    return {};
}

RENDERER_TODO(shade)
RENDERER_TODO(inline_image_begin)
RENDERER_TODO(inline_image_begin_data)
RENDERER_TODO(inline_image_end)
RENDERER_TODO(paint_xobject)

RENDERER_HANDLER(marked_content_point)
{
    // nop
    return {};
}

RENDERER_HANDLER(marked_content_designate)
{
    // nop
    return {};
}

RENDERER_HANDLER(marked_content_begin)
{
    // nop
    return {};
}

RENDERER_HANDLER(marked_content_begin_with_property_list)
{
    // nop
    return {};
}

RENDERER_HANDLER(marked_content_end)
{
    // nop
    return {};
}

RENDERER_TODO(compatibility_begin)
RENDERER_TODO(compatibility_end)

template<typename T>
Gfx::Point<T> Renderer::map(T x, T y) const
{
    auto mapped = state().ctm.map(Gfx::Point<T> { x, y });
    return { mapped.x(), static_cast<T>(m_bitmap->height()) - mapped.y() };
}

template<typename T>
Gfx::Size<T> Renderer::map(Gfx::Size<T> size) const
{
    return state().ctm.map(size);
}

template<typename T>
Gfx::Rect<T> Renderer::map(Gfx::Rect<T> rect) const
{
    return state().ctm.map(rect);
}

PDFErrorOr<void> Renderer::set_graphics_state_from_dict(NonnullRefPtr<DictObject> dict)
{
    if (dict->contains(CommonNames::LW))
        TRY(handle_set_line_width({ dict->get_value(CommonNames::LW) }));

    if (dict->contains(CommonNames::LC))
        TRY(handle_set_line_cap({ dict->get_value(CommonNames::LC) }));

    if (dict->contains(CommonNames::LJ))
        TRY(handle_set_line_join({ dict->get_value(CommonNames::LJ) }));

    if (dict->contains(CommonNames::ML))
        TRY(handle_set_miter_limit({ dict->get_value(CommonNames::ML) }));

    if (dict->contains(CommonNames::D)) {
        auto array = MUST(dict->get_array(m_document, CommonNames::D));
        TRY(handle_set_dash_pattern(array->elements()));
    }

    if (dict->contains(CommonNames::FL))
        TRY(handle_set_flatness_tolerance({ dict->get_value(CommonNames::FL) }));

    return {};
}

void Renderer::show_text(String const& string, float shift)
{
    auto& text_rendering_matrix = calculate_text_rendering_matrix();

    auto font_size = text_rendering_matrix.x_scale() * text_state().font_size;
    auto font_size_int = static_cast<int>(text_rendering_matrix.x_scale() * text_state().font_size);
    auto font = Gfx::FontDatabase::the().get(text_state().font_family, text_state().font_variant, font_size_int);
    VERIFY(font);

    auto glyph_position = text_rendering_matrix.map(Gfx::FloatPoint { 0.0f, 0.0f });
    // Account for the reversed font baseline
    glyph_position.set_y(glyph_position.y() - static_cast<float>(font->baseline()));

    auto original_position = glyph_position;

    for (auto char_code : string.bytes()) {
        auto code_point = text_state().font->char_code_to_code_point(char_code);
        auto char_width = text_state().font->get_char_width(char_code, font_size);

        if (code_point != 0x20)
            m_painter.draw_glyph(glyph_position.to_type<int>(), code_point, *font, state().paint_color);

        auto glyph_width = char_width * font_size;
        auto tx = (glyph_width - shift / 1000.0f);
        tx += text_state().character_spacing;

        if (code_point == ' ')
            tx += text_state().word_spacing;

        tx *= text_state().horizontal_scaling;

        glyph_position += { tx, 0.0f };
    }

    // Update text matrix
    auto delta_x = glyph_position.x() - original_position.x();
    m_text_rendering_matrix_is_dirty = true;
    m_text_matrix.translate(delta_x / text_rendering_matrix.x_scale(), 0.0f);
}

PDFErrorOr<NonnullRefPtr<ColorSpace>> Renderer::get_color_space(Value const& value)
{
    auto name = value.get<NonnullRefPtr<Object>>()->cast<NameObject>()->name();
    return TRY(ColorSpace::create(m_document, name, m_page));
}

Gfx::AffineTransform const& Renderer::calculate_text_rendering_matrix()
{
    if (m_text_rendering_matrix_is_dirty) {
        m_text_rendering_matrix = Gfx::AffineTransform(
            text_state().horizontal_scaling,
            0.0f,
            0.0f,
            1.0f,
            0.0f,
            text_state().rise);
        m_text_rendering_matrix.multiply(state().ctm);
        m_text_rendering_matrix.multiply(m_text_matrix);
        m_text_rendering_matrix_is_dirty = false;
    }
    return m_text_rendering_matrix;
}

}
