/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibPDF/Renderer.h>
#include <ctype.h>
#include <math.h>

namespace PDF {

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
    // FIXME: Directly concatenating the bytes would be safer than
    // concatenating the resulting commands.
    // FIXME: Text operators are supposed to only have effects on the current
    // stream object. Do the text operators treat this concatenated stream
    // as one stream or multiple?
    Vector<PDF::GraphicsCommand> commands;

    auto contents = m_page.contents->resolved_to<PDF::ArrayObject>(m_document);

    for (auto& ref : *contents) {
        VERIFY(ref.is_object());
        auto stream = ref.as_object()->resolved_to<PDF::StreamObject>(m_document);
        commands.append(PDF::Parser::parse_graphics_commands(stream->bytes()));
    }

    for (auto& command : commands)
        handle_command(command);
}

void Renderer::handle_command(const GraphicsCommand& command)
{
    switch (command.command()) {
#define V(name, snake_name, symbol) \
    case Command::name:             \
        return handle_##snake_name(command.arguments());
        ENUMERATE_GRAPHICS_COMMANDS(V)
#undef V
    case Command::TextNextLineShowString:
        return handle_text_next_line_show_string(command.arguments());
    }
}

void Renderer::handle_save_state(const Vector<Value>&)
{
    m_graphics_state_stack.append(GraphicsState(state()));
}

void Renderer::handle_restore_state(const Vector<Value>&)
{
    m_graphics_state_stack.take_last();
}

void Renderer::handle_concatenate_matrix(const Vector<Value>& args)
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

void Renderer::handle_set_line_width(const Vector<Value>& args)
{
    state().line_width = args[0].to_float();
}

void Renderer::handle_set_line_cap(const Vector<Value>& args)
{
    state().line_cap_style = static_cast<LineCapStyle>(args[0].as_int());
}

void Renderer::handle_set_line_join(const Vector<Value>& args)
{
    state().line_join_style = static_cast<LineJoinStyle>(args[0].as_int());
}

void Renderer::handle_set_miter_limit(const Vector<Value>& args)
{
    state().miter_limit = args[0].to_float();
}

void Renderer::handle_set_dash_pattern(const Vector<Value>& args)
{
    auto dash_array = args[0].as_object()->resolved_to<ArrayObject>(m_document);
    Vector<int> pattern;
    for (auto& element : *dash_array)
        pattern.append(element.as_int());
    state().line_dash_pattern = LineDashPattern { pattern, args[1].as_int() };
}

void Renderer::handle_path_begin(const Vector<Value>&)
{
    m_path = Gfx::Path();
}

void Renderer::handle_path_end(const Vector<Value>&)
{
}

void Renderer::handle_path_line(const Vector<Value>& args)
{
    m_path.line_to(map(args[0].to_float(), args[1].to_float()));
}

void Renderer::handle_path_close(const Vector<Value>&)
{
    m_path.close();
}

void Renderer::handle_path_append_rect(const Vector<Value>& args)
{
    auto pos = map(args[0].to_float(), args[1].to_float());
    auto size = map(Gfx::FloatSize { args[2].to_float(), args[3].to_float() });

    m_path.move_to(pos);
    m_path.line_to({ pos.x() + size.width(), pos.y() });
    m_path.line_to({ pos.x() + size.width(), pos.y() + size.height() });
    m_path.line_to({ pos.x(), pos.y() + size.height() });
    m_path.close();
}

void Renderer::handle_path_stroke(const Vector<Value>&)
{
    m_painter.stroke_path(m_path, state().stroke_color, state().line_width);
}

void Renderer::handle_path_close_and_stroke(const Vector<Value>& args)
{
    m_path.close();
    handle_path_stroke(args);
}

void Renderer::handle_path_fill_nonzero(const Vector<Value>&)
{
    m_painter.fill_path(m_path, state().paint_color, Gfx::Painter::WindingRule::Nonzero);
}

void Renderer::handle_path_fill_nonzero_deprecated(const Vector<Value>& args)
{
    handle_path_fill_nonzero(args);
}

void Renderer::handle_path_fill_evenodd(const Vector<Value>&)
{
    m_painter.fill_path(m_path, state().paint_color, Gfx::Painter::WindingRule::EvenOdd);
}

void Renderer::handle_path_fill_stroke_nonzero(const Vector<Value>& args)
{
    m_painter.stroke_path(m_path, state().stroke_color, state().line_width);
    handle_path_fill_nonzero(args);
}

void Renderer::handle_path_fill_stroke_evenodd(const Vector<Value>& args)
{
    m_painter.stroke_path(m_path, state().stroke_color, state().line_width);
    handle_path_fill_evenodd(args);
}

void Renderer::handle_path_close_fill_stroke_nonzero(const Vector<Value>& args)
{
    m_path.close();
    handle_path_fill_stroke_nonzero(args);
}

void Renderer::handle_path_close_fill_stroke_evenodd(const Vector<Value>& args)
{
    m_path.close();
    handle_path_fill_stroke_evenodd(args);
}

void Renderer::handle_text_set_char_space(const Vector<Value>& args)
{
    text_state().character_spacing = args[0].to_float();
}

void Renderer::handle_text_set_word_space(const Vector<Value>& args)
{
    text_state().word_spacing = args[0].to_float();
}

void Renderer::handle_text_set_horizontal_scale(const Vector<Value>& args)
{
    m_text_rendering_matrix_is_dirty = true;
    text_state().horizontal_scaling = args[0].to_float() / 100.0f;
}

void Renderer::handle_text_set_leading(const Vector<Value>& args)
{
    text_state().leading = args[0].to_float();
}

void Renderer::handle_text_set_font(const Vector<Value>& args)
{
    auto target_font_name = args[0].as_object()->resolved_to<NameObject>(m_document)->name();
    auto fonts_dictionary = m_page.resources->get_dict(m_document, "Font");
    auto font_dictionary = fonts_dictionary->get_dict(m_document, target_font_name);

    // FIXME: We do not yet have the standard 14 fonts, as some of them are not open fonts,
    // so we just use LiberationSerif for everything

    auto font_name = font_dictionary->get_object("BaseFont")->resolved_to<NameObject>(m_document)->name().to_lowercase();
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
    VERIFY(fabsf(state().ctm.x_scale() - state().ctm.y_scale()) < 0.01f);
    specified_font_size *= state().ctm.x_scale();

    text_state().font = Gfx::FontDatabase::the().get("Liberation Serif", font_variant, static_cast<int>(specified_font_size));
    VERIFY(text_state().font);
    m_text_rendering_matrix_is_dirty = true;
}

void Renderer::handle_text_set_rendering_mode(const Vector<Value>& args)
{
    text_state().rendering_mode = static_cast<TextRenderingMode>(args[0].as_int());
}

void Renderer::handle_text_set_rise(const Vector<Value>& args)
{
    m_text_rendering_matrix_is_dirty = true;
    text_state().rise = args[0].to_float();
}

void Renderer::handle_text_begin(const Vector<Value>&)
{
    m_text_matrix = Gfx::AffineTransform();
    m_text_line_matrix = Gfx::AffineTransform();
}

void Renderer::handle_text_end(const Vector<Value>&)
{
    // FIXME: Do we need to do anything here?
}

void Renderer::handle_text_next_line_offset(const Vector<Value>& args)
{
    Gfx::AffineTransform transform(1.0f, 0.0f, 0.0f, 1.0f, args[0].to_float(), args[1].to_float());
    transform.multiply(m_text_line_matrix);
    m_text_matrix = transform;
    m_text_line_matrix = transform;
    m_text_rendering_matrix_is_dirty = true;
}

void Renderer::handle_text_next_line_and_set_leading(const Vector<Value>& args)
{
    text_state().leading = -args[1].to_float();
    handle_text_next_line_offset(args);
}

void Renderer::handle_text_set_matrix_and_line_matrix(const Vector<Value>& args)
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

void Renderer::handle_text_next_line(const Vector<Value>&)
{
    handle_text_next_line_offset({ 0.0f, -text_state().leading });
}

void Renderer::handle_text_show_string(const Vector<Value>& args)
{
    show_text(args[0].as_object()->resolved_to<StringObject>(m_document)->string());
}

void Renderer::handle_text_next_line_show_string(const Vector<Value>& args)
{
    handle_text_next_line(args);
    handle_text_show_string(args);
}

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

void Renderer::show_text(const String& string, int shift)
{
    auto utf = Utf8View(string);
    auto& font = text_state().font;

    for (auto codepoint : utf) {
        // FIXME: Don't calculate this matrix for every character
        auto& text_rendering_matrix = calculate_text_rendering_matrix();

        auto text_position = text_rendering_matrix.map(Gfx::FloatPoint { 0.0f, 0.0f });
        text_position.set_y(static_cast<float>(m_bitmap->height()) - text_position.y());

        // FIXME: For some reason, the space character in LiberationSerif is drawn as an exclamation point
        if (codepoint != 0x20)
            m_painter.draw_glyph(text_position.to_type<int>(), codepoint, *text_state().font, state().paint_color);

        auto glyph_width = static_cast<float>(font->glyph_width(codepoint));
        auto tx = (glyph_width - static_cast<float>(shift) / 1000.0f);
        tx += text_state().character_spacing;

        // FIXME: UTF-aware whitespace detection?
        if (isspace(static_cast<int>(codepoint)))
            tx += text_state().word_spacing;

        tx *= text_state().horizontal_scaling;

        m_text_rendering_matrix_is_dirty = true;
        m_text_matrix = Gfx::AffineTransform(1, 0, 0, 1, tx, 0).multiply(m_text_matrix);
    }
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
