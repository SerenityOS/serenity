/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Renderer.h>
#include <ctype.h>
#include <math.h>

#define RENDERER_HANDLER(name) \
    void Renderer::handle_##name([[maybe_unused]] const Vector<Value>& args)

#define RENDERER_TODO(name)                                         \
    RENDERER_HANDLER(name)                                          \
    {                                                               \
        dbgln("[PDF::Renderer] Unsupported draw operation " #name); \
        TODO();                                                     \
    }

namespace PDF {

Optional<ColorSpace::Type> ColorSpace::color_space_from_string(const FlyString& str)
{
#define ENUM(name)    \
    if (str == #name) \
        return ColorSpace::Type::name;
    ENUMERATE_COLOR_SPACES(ENUM)
#undef ENUM

    return {};
}

Color ColorSpace::default_color_for_color_space(ColorSpace::Type color_space)
{
    switch (color_space) {
    case Type::DeviceGray:
    case Type::DeviceRGB:
        return Color::NamedColor::Black;
    case Type::DeviceCMYK:
        return Color::from_cmyk(1.0f, 1.0f, 1.0f, 0.0f);
    default:
        TODO();
    }
}

Color ColorSpace::color_from_parameters(ColorSpace::Type color_space, const Vector<Value>& args)
{
    switch (color_space) {
    case Type::DeviceGray: {
        VERIFY(args.size() == 1);
        auto gray = static_cast<u8>(args[0].to_float() * 255.0f);
        return Color(gray, gray, gray);
    }
    case Type::DeviceRGB: {
        VERIFY(args.size() == 3);
        auto r = static_cast<u8>(args[0].to_float() * 255.0f);
        auto g = static_cast<u8>(args[1].to_float() * 255.0f);
        auto b = static_cast<u8>(args[2].to_float() * 255.0f);
        return Color(r, g, b);
    }
    case Type::DeviceCMYK: {
        VERIFY(args.size() == 4);
        auto c = args[0].to_float();
        auto m = args[1].to_float();
        auto y = args[2].to_float();
        auto k = args[3].to_float();
        return Color::from_cmyk(c, m, y, k);
    }
    default:
        TODO();
    }
}

void Renderer::render(Document& document, const Page& page, RefPtr<Gfx::Bitmap> bitmap)
{
    Renderer(document, page, bitmap).render();
}

Renderer::Renderer(RefPtr<Document> document, const Page& page, RefPtr<Gfx::Bitmap> bitmap)
    : m_document(document)
    , m_bitmap(bitmap)
    , m_page(page)
    , m_painter(*bitmap)
{
    auto media_box = m_page.media_box;

    m_userspace_matrix.translate(media_box.lower_left_x, media_box.lower_left_y);

    float width = media_box.upper_right_x - media_box.lower_left_x;
    float height = media_box.upper_right_y - media_box.lower_left_y;
    float scale_x = static_cast<float>(bitmap->width()) / width;
    float scale_y = static_cast<float>(bitmap->height()) / height;
    m_userspace_matrix.scale(scale_x, scale_y);

    m_graphics_state_stack.append(GraphicsState { m_userspace_matrix });

    m_bitmap->fill(Gfx::Color::NamedColor::White);
}

void Renderer::render()
{
    // Use our own vector, as the /Content can be an array with multiple
    // streams which gets concatenated
    // FIXME: Text operators are supposed to only have effects on the current
    // stream object. Do the text operators treat this concatenated stream
    // as one stream or multiple?
    ByteBuffer byte_buffer;

    if (m_page.contents->is_array()) {
        auto contents = object_cast<ArrayObject>(m_page.contents);
        for (auto& ref : *contents) {
            auto bytes = m_document->resolve_to<StreamObject>(ref)->bytes();
            byte_buffer.append(bytes.data(), bytes.size());
        }
    } else {
        VERIFY(m_page.contents->is_stream());
        auto bytes = object_cast<StreamObject>(m_page.contents)->bytes();
        byte_buffer.append(bytes.data(), bytes.size());
    }

    auto commands = Parser::parse_graphics_commands(byte_buffer);

    for (auto& command : commands)
        handle_command(command);
}

void Renderer::handle_command(const Command& command)
{
    switch (command.command_type()) {
#define V(name, snake_name, symbol)               \
    case CommandType::name:                       \
        handle_##snake_name(command.arguments()); \
        break;
        ENUMERATE_COMMANDS(V)
#undef V
    case CommandType::TextNextLineShowString:
        handle_text_next_line_show_string(command.arguments());
        break;
    case CommandType::TextNextLineShowStringSetSpacing:
        handle_text_next_line_show_string_set_spacing(command.arguments());
        break;
    }
}

RENDERER_HANDLER(save_state)
{
    m_graphics_state_stack.append(state());
}

RENDERER_HANDLER(restore_state)
{
    m_graphics_state_stack.take_last();
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
}

RENDERER_HANDLER(set_line_width)
{
    state().line_width = args[0].to_float();
}

RENDERER_HANDLER(set_line_cap)
{
    state().line_cap_style = static_cast<LineCapStyle>(args[0].as_int());
}

RENDERER_HANDLER(set_line_join)
{
    state().line_join_style = static_cast<LineJoinStyle>(args[0].as_int());
}

RENDERER_HANDLER(set_miter_limit)
{
    state().miter_limit = args[0].to_float();
}

RENDERER_HANDLER(set_dash_pattern)
{
    auto dash_array = m_document->resolve_to<ArrayObject>(args[0]);
    Vector<int> pattern;
    for (auto& element : *dash_array)
        pattern.append(element.as_int());
    state().line_dash_pattern = LineDashPattern { pattern, args[1].as_int() };
}

RENDERER_TODO(set_color_rendering_intent);
RENDERER_TODO(set_flatness_tolerance);
RENDERER_TODO(set_graphics_state_from_dict);

RENDERER_HANDLER(path_move)
{
    m_current_path.move_to(map(args[0].to_float(), args[1].to_float()));
}

RENDERER_HANDLER(path_line)
{
    VERIFY(!m_current_path.segments().is_empty());
    m_current_path.line_to(map(args[0].to_float(), args[1].to_float()));
}

RENDERER_TODO(path_cubic_bezier_curve);
RENDERER_TODO(path_cubic_bezier_curve_no_first_control);
RENDERER_TODO(path_cubic_bezier_curve_no_second_control);

RENDERER_HANDLER(path_close)
{
    m_current_path.close();
}

RENDERER_HANDLER(path_append_rect)
{
    auto pos = map(args[0].to_float(), args[1].to_float());
    auto size = map(Gfx::FloatSize { args[2].to_float(), args[3].to_float() });

    m_current_path.move_to(pos);
    m_current_path.line_to({ pos.x() + size.width(), pos.y() });
    m_current_path.line_to({ pos.x() + size.width(), pos.y() + size.height() });
    m_current_path.line_to({ pos.x(), pos.y() + size.height() });
    m_current_path.close();
}

RENDERER_HANDLER(path_stroke)
{
    m_painter.stroke_path(m_current_path, state().stroke_color, state().line_width);
    m_current_path.clear();
}

RENDERER_HANDLER(path_close_and_stroke)
{
    m_current_path.close();
    handle_path_stroke(args);
}

RENDERER_HANDLER(path_fill_nonzero)
{
    m_painter.fill_path(m_current_path, state().paint_color, Gfx::Painter::WindingRule::Nonzero);
    m_current_path.clear();
}

RENDERER_HANDLER(path_fill_nonzero_deprecated)
{
    handle_path_fill_nonzero(args);
}

RENDERER_HANDLER(path_fill_evenodd)
{
    m_painter.fill_path(m_current_path, state().paint_color, Gfx::Painter::WindingRule::EvenOdd);
    m_current_path.clear();
}

RENDERER_HANDLER(path_fill_stroke_nonzero)
{
    m_painter.stroke_path(m_current_path, state().stroke_color, state().line_width);
    handle_path_fill_nonzero(args);
}

RENDERER_HANDLER(path_fill_stroke_evenodd)
{
    m_painter.stroke_path(m_current_path, state().stroke_color, state().line_width);
    handle_path_fill_evenodd(args);
}

RENDERER_HANDLER(path_close_fill_stroke_nonzero)
{
    m_current_path.close();
    handle_path_fill_stroke_nonzero(args);
}

RENDERER_HANDLER(path_close_fill_stroke_evenodd)
{
    m_current_path.close();
    handle_path_fill_stroke_evenodd(args);
}

RENDERER_HANDLER(path_end)
{
}

RENDERER_HANDLER(path_intersect_clip_nonzero)
{
    // FIXME: Support arbitrary path clipping in the painter and utilize that here
    auto bounding_box = map(m_current_path.bounding_box());
    m_painter.add_clip_rect(bounding_box.to_type<int>());
}

RENDERER_HANDLER(path_intersect_clip_evenodd)
{
    // FIXME: Support arbitrary path clipping in the painter and utilize that here
    auto bounding_box = map(m_current_path.bounding_box());
    m_painter.add_clip_rect(bounding_box.to_type<int>());
}

RENDERER_HANDLER(text_begin)
{
    m_text_matrix = Gfx::AffineTransform();
    m_text_line_matrix = Gfx::AffineTransform();
}

RENDERER_HANDLER(text_end)
{
    // FIXME: Do we need to do anything here?
}

RENDERER_HANDLER(text_set_char_space)
{
    text_state().character_spacing = args[0].to_float();
}

RENDERER_HANDLER(text_set_word_space)
{
    text_state().word_spacing = args[0].to_float();
}

RENDERER_HANDLER(text_set_horizontal_scale)
{
    m_text_rendering_matrix_is_dirty = true;
    text_state().horizontal_scaling = args[0].to_float() / 100.0f;
}

RENDERER_HANDLER(text_set_leading)
{
    text_state().leading = args[0].to_float();
}

RENDERER_HANDLER(text_set_font)
{
    auto target_font_name = m_document->resolve_to<NameObject>(args[0])->name();
    auto fonts_dictionary = m_page.resources->get_dict(m_document, CommonNames::Font);
    auto font_dictionary = fonts_dictionary->get_dict(m_document, target_font_name);

    // FIXME: We do not yet have the standard 14 fonts, as some of them are not open fonts,
    // so we just use LiberationSerif for everything

    auto font_name = font_dictionary->get_name(m_document, CommonNames::BaseFont)->name().to_lowercase();
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

    auto specified_font_size = args[1].to_float();
    // FIXME: This scaling should occur when drawing the glyph rather than selecting the font.
    // This should be removed when the painter supports arbitrary bitmap scaling.
    specified_font_size *= state().ctm.x_scale();

    text_state().font = Gfx::FontDatabase::the().get("Liberation Serif", font_variant, static_cast<int>(specified_font_size));
    VERIFY(text_state().font);
    m_text_rendering_matrix_is_dirty = true;
}

RENDERER_HANDLER(text_set_rendering_mode)
{
    text_state().rendering_mode = static_cast<TextRenderingMode>(args[0].as_int());
}

RENDERER_HANDLER(text_set_rise)
{
    m_text_rendering_matrix_is_dirty = true;
    text_state().rise = args[0].to_float();
}

RENDERER_HANDLER(text_next_line_offset)
{
    Gfx::AffineTransform transform(1.0f, 0.0f, 0.0f, 1.0f, args[0].to_float(), args[1].to_float());
    transform.multiply(m_text_line_matrix);
    m_text_matrix = transform;
    m_text_line_matrix = transform;
    m_text_rendering_matrix_is_dirty = true;
}

RENDERER_HANDLER(text_next_line_and_set_leading)
{
    text_state().leading = -args[1].to_float();
    handle_text_next_line_offset(args);
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
}

RENDERER_HANDLER(text_next_line)
{
    handle_text_next_line_offset({ 0.0f, -text_state().leading });
}

RENDERER_HANDLER(text_show_string)
{
    auto text = m_document->resolve_to<StringObject>(args[0])->string();
    show_text(text);
}

RENDERER_HANDLER(text_next_line_show_string)
{
    handle_text_next_line(args);
    handle_text_show_string(args);
}

RENDERER_TODO(text_next_line_show_string_set_spacing);
RENDERER_TODO(text_show_string_array);
RENDERER_TODO(type3_font_set_glyph_width);
RENDERER_TODO(type3_font_set_glyph_width_and_bbox);

RENDERER_HANDLER(set_stroking_space)
{
    state().stroke_color_space = get_color_space(args[0]);
    state().stroke_color = ColorSpace::default_color_for_color_space(state().stroke_color_space);
}

RENDERER_HANDLER(set_painting_space)
{
    state().paint_color_space = get_color_space(args[0]);
    state().paint_color = ColorSpace::default_color_for_color_space(state().paint_color_space);
}

RENDERER_HANDLER(set_stroking_color)
{
    state().stroke_color = ColorSpace::color_from_parameters(state().stroke_color_space, args);
}

RENDERER_TODO(set_stroking_color_extended);

RENDERER_HANDLER(set_painting_color)
{
    state().paint_color = ColorSpace::color_from_parameters(state().paint_color_space, args);
}

RENDERER_TODO(set_painting_color_extended);

RENDERER_HANDLER(set_stroking_color_and_space_to_gray)
{
    state().stroke_color_space = ColorSpace::Type::DeviceGray;
    state().stroke_color = ColorSpace::color_from_parameters(ColorSpace::Type::DeviceGray, args);
}

RENDERER_HANDLER(set_painting_color_and_space_to_gray)
{
    state().paint_color_space = ColorSpace::Type::DeviceGray;
    state().paint_color = ColorSpace::color_from_parameters(ColorSpace::Type::DeviceGray, args);
}

RENDERER_HANDLER(set_stroking_color_and_space_to_rgb)
{
    state().stroke_color_space = ColorSpace::Type::DeviceRGB;
    state().stroke_color = ColorSpace::color_from_parameters(ColorSpace::Type::DeviceRGB, args);
}

RENDERER_HANDLER(set_painting_color_and_space_to_rgb)
{
    state().paint_color_space = ColorSpace::Type::DeviceRGB;
    state().paint_color = ColorSpace::color_from_parameters(ColorSpace::Type::DeviceRGB, args);
}

RENDERER_HANDLER(set_stroking_color_and_space_to_cmyk)
{
    state().stroke_color_space = ColorSpace::Type::DeviceCMYK;
    state().stroke_color = ColorSpace::color_from_parameters(ColorSpace::Type::DeviceCMYK, args);
}

RENDERER_HANDLER(set_painting_color_and_space_to_cmyk)
{
    state().paint_color_space = ColorSpace::Type::DeviceCMYK;
    state().paint_color = ColorSpace::color_from_parameters(ColorSpace::Type::DeviceCMYK, args);
}

RENDERER_TODO(shade);
RENDERER_TODO(inline_image_begin);
RENDERER_TODO(inline_image_begin_data);
RENDERER_TODO(inline_image_end);
RENDERER_TODO(paint_xobject);
RENDERER_TODO(marked_content_point);
RENDERER_TODO(marked_content_designate);
RENDERER_TODO(marked_content_begin);
RENDERER_TODO(marked_content_begin_with_property_list);
RENDERER_TODO(marked_content_end);
RENDERER_TODO(compatibility_begin);
RENDERER_TODO(compatibility_end);

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

void Renderer::show_text(const String& string, int shift)
{
    auto utf = Utf8View(string);
    auto& font = text_state().font;

    for (auto code_point : utf) {
        // FIXME: Don't calculate this matrix for every character
        auto& text_rendering_matrix = calculate_text_rendering_matrix();

        auto text_position = text_rendering_matrix.map(Gfx::FloatPoint { 0.0f, 0.0f });
        text_position.set_y(static_cast<float>(m_bitmap->height()) - text_position.y());

        // FIXME: For some reason, the space character in LiberationSerif is drawn as an exclamation point
        if (code_point != 0x20)
            m_painter.draw_glyph(text_position.to_type<int>(), code_point, *text_state().font, state().paint_color);

        auto glyph_width = static_cast<float>(font->glyph_width(code_point));
        auto tx = (glyph_width - static_cast<float>(shift) / 1000.0f);
        tx += text_state().character_spacing;

        if (code_point == ' ')
            tx += text_state().word_spacing;

        tx *= text_state().horizontal_scaling;

        m_text_rendering_matrix_is_dirty = true;
        m_text_matrix = Gfx::AffineTransform(1, 0, 0, 1, tx, 0).multiply(m_text_matrix);
    }
}

ColorSpace::Type Renderer::get_color_space(const Value& value)
{
    auto name = object_cast<NameObject>(value.as_object())->name();
    auto color_space_opt = ColorSpace::color_space_from_string(name);
    if (!color_space_opt.has_value()) {
        // The name is probably a key into the resource dictionary
        TODO();
    }

    return color_space_opt.value();
}

const Gfx::AffineTransform& Renderer::calculate_text_rendering_matrix()
{
    if (m_text_rendering_matrix_is_dirty) {
        m_text_rendering_matrix = Gfx::AffineTransform(
            text_state().horizontal_scaling,
            0.0f,
            0.0f,
            1.0f,
            0.0f,
            text_state().rise);
        m_text_rendering_matrix.multiply(m_text_matrix);
        m_text_rendering_matrix.multiply(state().ctm);
        m_text_rendering_matrix_is_dirty = false;
    }
    return m_text_rendering_matrix;
}

}
