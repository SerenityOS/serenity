/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/Utf8View.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/PDFFont.h>
#include <LibPDF/Interpolation.h>
#include <LibPDF/Renderer.h>
#include <LibPDF/Shading.h>

#define RENDERER_HANDLER(name) \
    PDFErrorOr<void> Renderer::handle_##name([[maybe_unused]] ReadonlySpan<Value> args, [[maybe_unused]] Optional<NonnullRefPtr<DictObject>> extra_resources)

namespace PDF {

// Use a RAII object to restore the graphics state, to make sure it gets restored even if
// a TRY(handle_operator()) causes us to exit the operators loop early.
// Explicitly resize stack size at the end so that if the recursive document contains
// `q q unsupportedop Q Q`, we undo the stack pushes from the inner `q q` even if
// `unsupportedop` terminates processing the inner instruction stream before `Q Q`
// would normally pop state.
class Renderer::ScopedState {
public:
    ScopedState(Renderer& renderer)
        : m_renderer(renderer)
        , m_starting_stack_depth(m_renderer.m_graphics_state_stack.size())
    {
        MUST(m_renderer.handle_save_state({}));
    }
    ~ScopedState()
    {
        m_renderer.m_graphics_state_stack.shrink(m_starting_stack_depth);
    }

private:
    Renderer& m_renderer;
    size_t m_starting_stack_depth;
};

PDFErrorsOr<void> Renderer::render(Document& document, Page const& page, RefPtr<Gfx::Bitmap> bitmap, Color background_color, RenderingPreferences rendering_preferences)
{
    return Renderer(document, page, bitmap, background_color, rendering_preferences).render();
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Renderer::apply_page_rotation(NonnullRefPtr<Gfx::Bitmap> bitmap, Page const& page, int extra_degrees)
{
    int rotation_count = ((page.rotate + extra_degrees) / 90) % 4;
    if (rotation_count == 1)
        bitmap = TRY(bitmap->rotated(Gfx::RotationDirection::Clockwise));
    else if (rotation_count == 2)
        bitmap = TRY(bitmap->rotated(Gfx::RotationDirection::Flip));
    else if (rotation_count == 3)
        bitmap = TRY(bitmap->rotated(Gfx::RotationDirection::CounterClockwise));
    return bitmap;
}

static void rect_path(Gfx::Path& path, float x, float y, float width, float height)
{
    path.move_to({ x, y });
    path.line_to({ x + width, y });
    path.line_to({ x + width, y + height });
    path.line_to({ x, y + height });
    path.close();
}

template<typename T>
static void rect_path(Gfx::Path& path, Gfx::Rect<T> rect)
{
    return rect_path(path, rect.x(), rect.y(), rect.width(), rect.height());
}

template<typename T>
static Gfx::Path rect_path(Gfx::Rect<T> const& rect)
{
    Gfx::Path path;
    rect_path(path, rect);
    return path;
}

Renderer::Renderer(RefPtr<Document> document, Page const& page, RefPtr<Gfx::Bitmap> bitmap, Color background_color, RenderingPreferences rendering_preferences)
    : m_document(document)
    , m_bitmap(bitmap)
    , m_page(page)
    , m_painter(*bitmap)
    , m_anti_aliasing_painter(m_painter)
    , m_rendering_preferences(rendering_preferences)
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

    static Gfx::AffineTransform vertical_reflection_matrix = { 1, 0, 0, -1, 0, 0 };

    userspace_matrix.multiply(vertical_reflection_matrix);
    userspace_matrix.translate(0.0f, -height);

    auto initial_clipping_path = rect_path(userspace_matrix.map(Gfx::FloatRect(0, 0, width, height)));
    m_graphics_state_stack.append(GraphicsState { userspace_matrix, { initial_clipping_path, initial_clipping_path } });

    m_bitmap->fill(background_color);
}

PDFErrorsOr<void> Renderer::render()
{
    auto operators = TRY(Parser::parse_operators(m_document, TRY(m_page.page_contents(*m_document))));

    Errors errors;
    for (auto& op : operators) {
        auto maybe_error = handle_operator(op);
        if (maybe_error.is_error()) {
            errors.add_error(maybe_error.release_error());
        }
    }
    if (!errors.errors().is_empty())
        return errors;
    return {};
}

PDFErrorOr<void> Renderer::handle_operator(Operator const& op, Optional<NonnullRefPtr<DictObject>> extra_resources)
{
    switch (op.type()) {
#define V(name, snake_name, symbol)                                \
    case OperatorType::name:                                       \
        TRY(handle_##snake_name(op.arguments(), extra_resources)); \
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
    Vector<float> pattern;
    for (auto& element : *dash_array)
        pattern.append(element.to_float());
    state().line_dash_pattern = LineDashPattern { pattern, args[1].to_float() };
    return {};
}

RENDERER_HANDLER(set_color_rendering_intent)
{
    state().color_rendering_intent = MUST(m_document->resolve_to<NameObject>(args[0]))->name();
    return {};
}

RENDERER_HANDLER(set_flatness_tolerance)
{
    state().flatness_tolerance = args[0].to_float();
    return {};
}

RENDERER_HANDLER(set_graphics_state_from_dict)
{
    auto resources = extra_resources.value_or(m_page.resources);
    auto dict_name = MUST(m_document->resolve_to<NameObject>(args[0]))->name();
    auto ext_gstate_dict = MUST(resources->get_dict(m_document, CommonNames::ExtGState));
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
    VERIFY(!m_current_path.is_empty());
    m_current_path.line_to(map(args[0].to_float(), args[1].to_float()));
    return {};
}

RENDERER_HANDLER(path_cubic_bezier_curve)
{
    VERIFY(args.size() == 6);
    m_current_path.cubic_bezier_curve_to(
        map(args[0].to_float(), args[1].to_float()),
        map(args[2].to_float(), args[3].to_float()),
        map(args[4].to_float(), args[5].to_float()));
    return {};
}

RENDERER_HANDLER(path_cubic_bezier_curve_no_first_control)
{
    VERIFY(args.size() == 4);
    VERIFY(!m_current_path.is_empty());
    auto current_point = m_current_path.last_point();
    m_current_path.cubic_bezier_curve_to(
        current_point,
        map(args[0].to_float(), args[1].to_float()),
        map(args[2].to_float(), args[3].to_float()));
    return {};
}

RENDERER_HANDLER(path_cubic_bezier_curve_no_second_control)
{
    VERIFY(args.size() == 4);
    VERIFY(!m_current_path.is_empty());
    auto first_control_point = map(args[0].to_float(), args[1].to_float());
    auto second_control_point = map(args[2].to_float(), args[3].to_float());
    m_current_path.cubic_bezier_curve_to(
        first_control_point,
        second_control_point,
        second_control_point);
    return {};
}

RENDERER_HANDLER(path_close)
{
    m_current_path.close();
    return {};
}

RENDERER_HANDLER(path_append_rect)
{
    auto rect = Gfx::FloatRect(args[0].to_float(), args[1].to_float(), args[2].to_float(), args[3].to_float());
    // Note: The path of the rectangle is mapped (rather than the rectangle).
    // This is because negative width/heights are possible, and result in different
    // winding orders, but this is lost by Gfx::AffineTransform::map().
    m_current_path.append_path(map(rect_path(rect)));
    return {};
}

void Renderer::activate_clip()
{
    auto bounding_box = state().clipping_paths.current.bounding_box().to_type<int>();
    m_painter.clear_clip_rect();
    if (m_rendering_preferences.show_clipping_paths) {
        m_painter.draw_rect(bounding_box, Color::Black);
    }
    m_painter.add_clip_rect(bounding_box);
}

void Renderer::deactivate_clip()
{
    m_painter.clear_clip_rect();
    state().clipping_paths.current = state().clipping_paths.next;
}

///
// Path painting operations
///

void Renderer::begin_path_paint()
{
    if (m_rendering_preferences.clip_paths)
        activate_clip();
}

void Renderer::end_path_paint()
{
    m_current_path.clear();
    if (m_rendering_preferences.clip_paths)
        deactivate_clip();
}

RENDERER_HANDLER(path_stroke)
{
    begin_path_paint();
    if (state().stroke_style.has<NonnullRefPtr<Gfx::PaintStyle>>()) {
        m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_style.get<NonnullRefPtr<Gfx::PaintStyle>>(), stroke_style());
    } else {
        m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_style.get<Color>(), stroke_style());
    }
    end_path_paint();
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
    begin_path_paint();
    m_current_path.close_all_subpaths();
    if (state().paint_style.has<NonnullRefPtr<Gfx::PaintStyle>>()) {
        m_anti_aliasing_painter.fill_path(m_current_path, state().paint_style.get<NonnullRefPtr<Gfx::PaintStyle>>(), 1.0, Gfx::WindingRule::Nonzero);
    } else {
        m_anti_aliasing_painter.fill_path(m_current_path, state().paint_style.get<Color>(), Gfx::WindingRule::Nonzero);
    }
    end_path_paint();
    return {};
}

RENDERER_HANDLER(path_fill_nonzero_deprecated)
{
    return handle_path_fill_nonzero(args);
}

RENDERER_HANDLER(path_fill_evenodd)
{
    begin_path_paint();
    m_current_path.close_all_subpaths();
    if (state().paint_style.has<NonnullRefPtr<Gfx::PaintStyle>>()) {
        m_anti_aliasing_painter.fill_path(m_current_path, state().paint_style.get<NonnullRefPtr<Gfx::PaintStyle>>(), 1.0, Gfx::WindingRule::EvenOdd);
    } else {
        m_anti_aliasing_painter.fill_path(m_current_path, state().paint_style.get<Color>(), Gfx::WindingRule::EvenOdd);
    }
    end_path_paint();
    return {};
}

RENDERER_HANDLER(path_fill_stroke_nonzero)
{
    begin_path_paint();
    if (state().stroke_style.has<NonnullRefPtr<Gfx::PaintStyle>>()) {
        m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_style.get<NonnullRefPtr<Gfx::PaintStyle>>(), stroke_style());
    } else {
        m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_style.get<Color>(), stroke_style());
    }
    m_current_path.close_all_subpaths();
    if (state().paint_style.has<NonnullRefPtr<Gfx::PaintStyle>>()) {
        m_anti_aliasing_painter.fill_path(m_current_path, state().paint_style.get<NonnullRefPtr<Gfx::PaintStyle>>(), 1.0, Gfx::WindingRule::Nonzero);
    } else {
        m_anti_aliasing_painter.fill_path(m_current_path, state().paint_style.get<Color>(), Gfx::WindingRule::Nonzero);
    }
    end_path_paint();
    return {};
}

RENDERER_HANDLER(path_fill_stroke_evenodd)
{
    begin_path_paint();
    if (state().stroke_style.has<NonnullRefPtr<Gfx::PaintStyle>>()) {
        m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_style.get<NonnullRefPtr<Gfx::PaintStyle>>(), stroke_style());
    } else {
        m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_style.get<Color>(), stroke_style());
    }
    m_current_path.close_all_subpaths();
    if (state().paint_style.has<NonnullRefPtr<Gfx::PaintStyle>>()) {
        m_anti_aliasing_painter.fill_path(m_current_path, state().paint_style.get<NonnullRefPtr<Gfx::PaintStyle>>(), 1.0, Gfx::WindingRule::EvenOdd);
    } else {
        m_anti_aliasing_painter.fill_path(m_current_path, state().paint_style.get<Color>(), Gfx::WindingRule::EvenOdd);
    }
    end_path_paint();
    return {};
}

RENDERER_HANDLER(path_close_fill_stroke_nonzero)
{
    m_current_path.close();
    return handle_path_fill_stroke_nonzero(args);
}

RENDERER_HANDLER(path_close_fill_stroke_evenodd)
{
    m_current_path.close();
    return handle_path_fill_stroke_evenodd(args);
}

RENDERER_HANDLER(path_end)
{
    begin_path_paint();
    end_path_paint();
    return {};
}

RENDERER_HANDLER(path_intersect_clip_nonzero)
{
    // FIXME: Support arbitrary path clipping in Path and utilize that here
    auto next_clipping_bbox = state().clipping_paths.next.bounding_box();
    next_clipping_bbox.intersect(m_current_path.bounding_box());
    state().clipping_paths.next = rect_path(next_clipping_bbox);
    return {};
}

RENDERER_HANDLER(path_intersect_clip_evenodd)
{
    // FIXME: Should have different behavior than path_intersect_clip_nonzero
    return handle_path_intersect_clip_nonzero(args);
}

RENDERER_HANDLER(text_begin)
{
    m_text_matrix = Gfx::AffineTransform();
    m_text_line_matrix = Gfx::AffineTransform();
    m_text_rendering_matrix_is_dirty = true;
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

PDFErrorOr<NonnullRefPtr<PDFFont>> Renderer::get_font(FontCacheKey const& key)
{
    auto it = m_font_cache.find(key);
    if (it != m_font_cache.end()) {
        // Update the potentially-stale size set in text_set_matrix_and_line_matrix().
        it->value->set_font_size(key.font_size);
        return it->value;
    }

    auto font = TRY(PDFFont::create(m_document, key.font_dictionary, key.font_size));
    m_font_cache.set(key, font);
    return font;
}

RENDERER_HANDLER(text_set_font)
{
    auto target_font_name = MUST(m_document->resolve_to<NameObject>(args[0]))->name();

    text_state().font_size = args[1].to_float();

    auto& text_rendering_matrix = calculate_text_rendering_matrix();
    auto font_size = text_rendering_matrix.x_scale() * text_state().font_size / text_state().horizontal_scaling;

    auto resources = extra_resources.value_or(m_page.resources);
    auto fonts_dictionary = MUST(resources->get_dict(m_document, CommonNames::Font));
    auto font_dictionary = MUST(fonts_dictionary->get_dict(m_document, target_font_name));

    FontCacheKey cache_key { move(font_dictionary), font_size };
    text_state().font = TRY(get_font(cache_key));

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
    m_text_rendering_matrix_is_dirty = true;
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

    // Settings the text/line matrix retroactively affects fonts
    if (text_state().font) {
        auto new_text_rendering_matrix = calculate_text_rendering_matrix();
        text_state().font->set_font_size(text_state().font_size * new_text_rendering_matrix.x_scale() / text_state().horizontal_scaling);
    }

    return {};
}

RENDERER_HANDLER(text_next_line)
{
    TRY(handle_text_next_line_offset(Array<Value, 2> { 0.0f, -text_state().leading }));
    return {};
}

RENDERER_HANDLER(text_show_string)
{
    auto text = MUST(m_document->resolve_to<StringObject>(args[0]))->string();
    TRY(show_text(text));
    return {};
}

RENDERER_HANDLER(text_next_line_show_string)
{
    TRY(handle_text_next_line(args));
    TRY(handle_text_show_string(args));
    return {};
}

RENDERER_HANDLER(text_next_line_show_string_set_spacing)
{
    TRY(handle_text_set_word_space(args.slice(0, 1)));
    TRY(handle_text_set_char_space(args.slice(1, 1)));
    TRY(handle_text_next_line_show_string(args.slice(2)));
    return {};
}

RENDERER_HANDLER(text_show_string_array)
{
    auto elements = MUST(m_document->resolve_to<ArrayObject>(args[0]))->elements();

    for (auto& element : elements) {
        if (element.has_number()) {
            float shift = element.to_float() / 1000.0f;
            if (text_state().font->writing_mode() == WritingMode::Horizontal)
                m_text_matrix.translate(-shift * text_state().font_size * text_state().horizontal_scaling, 0.0f);
            else
                m_text_matrix.translate(0.0f, -shift * text_state().font_size);
            m_text_rendering_matrix_is_dirty = true;
        } else {
            auto str = element.get<NonnullRefPtr<Object>>()->cast<StringObject>()->string();
            TRY(show_text(str));
        }
    }

    return {};
}

RENDERER_HANDLER(type3_font_set_glyph_width)
{
    // FIXME: Do something with this.
    return {};
}

RENDERER_HANDLER(type3_font_set_glyph_width_and_bbox)
{
    // FIXME: Do something with this.
    return {};
}

RENDERER_HANDLER(set_stroking_space)
{
    state().stroke_color_space = TRY(get_color_space_from_resources(args[0], extra_resources.value_or(m_page.resources)));
    VERIFY(state().stroke_color_space);
    return {};
}

RENDERER_HANDLER(set_painting_space)
{
    state().paint_color_space = TRY(get_color_space_from_resources(args[0], extra_resources.value_or(m_page.resources)));
    VERIFY(state().paint_color_space);
    return {};
}

RENDERER_HANDLER(set_stroking_color)
{
    state().stroke_style = TRY(state().stroke_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_stroking_color_extended)
{
    // FIXME: Handle Pattern color spaces
    auto last_arg = args.last();
    if (last_arg.has<NonnullRefPtr<Object>>() && last_arg.get<NonnullRefPtr<Object>>()->is<NameObject>()) {
        dbgln("pattern space {}", last_arg.get<NonnullRefPtr<Object>>()->cast<NameObject>()->name());
        return Error::rendering_unsupported_error("Pattern color spaces not yet implemented");
    }

    state().stroke_style = TRY(state().stroke_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_painting_color)
{
    state().paint_style = TRY(state().paint_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_painting_color_extended)
{
    // FIXME: Handle Pattern color spaces
    auto last_arg = args.last();
    if (last_arg.has<NonnullRefPtr<Object>>() && last_arg.get<NonnullRefPtr<Object>>()->is<NameObject>()) {
        dbgln("pattern space {}", last_arg.get<NonnullRefPtr<Object>>()->cast<NameObject>()->name());
        return Error::rendering_unsupported_error("Pattern color spaces not yet implemented");
    }

    state().paint_style = TRY(state().paint_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_stroking_color_and_space_to_gray)
{
    state().stroke_color_space = DeviceGrayColorSpace::the();
    state().stroke_style = TRY(state().stroke_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_painting_color_and_space_to_gray)
{
    state().paint_color_space = DeviceGrayColorSpace::the();
    state().paint_style = TRY(state().paint_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_stroking_color_and_space_to_rgb)
{
    state().stroke_color_space = DeviceRGBColorSpace::the();
    state().stroke_style = TRY(state().stroke_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_painting_color_and_space_to_rgb)
{
    state().paint_color_space = DeviceRGBColorSpace::the();
    state().paint_style = TRY(state().paint_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_stroking_color_and_space_to_cmyk)
{
    state().stroke_color_space = TRY(DeviceCMYKColorSpace::the());
    state().stroke_style = TRY(state().stroke_color_space->style(args));
    return {};
}

RENDERER_HANDLER(set_painting_color_and_space_to_cmyk)
{
    state().paint_color_space = TRY(DeviceCMYKColorSpace::the());
    state().paint_style = TRY(state().paint_color_space->style(args));
    return {};
}

RENDERER_HANDLER(shade)
{
    auto inverse_ctm = state().ctm.inverse();
    if (!inverse_ctm.has_value())
        return {};

    VERIFY(args.size() == 1);
    auto shading_name = MUST(m_document->resolve_to<NameObject>(args[0]))->name();
    auto resources = extra_resources.value_or(m_page.resources);
    auto shading_resource_dict = TRY(resources->get_dict(m_document, CommonNames::Shading));
    if (!shading_resource_dict->contains(shading_name)) {
        dbgln("missing shade {}", shading_name);
        return Error::malformed_error("Missing entry for shade name");
    }

    auto shading_dict_or_stream = TRY(shading_resource_dict->get_object(m_document, shading_name));
    auto shading = TRY(Shading::create(m_document, shading_dict_or_stream, *this));

    ClipRAII clip_raii { *this };
    return shading->draw(m_painter, inverse_ctm.value());
}

RENDERER_HANDLER(inline_image_begin)
{
    // The parser only calls the inline_image_end handler for inline images.
    VERIFY_NOT_REACHED();
}

RENDERER_HANDLER(inline_image_begin_data)
{
    // The parser only calls the inline_image_end handler for inline images.
    VERIFY_NOT_REACHED();
}

static PDFErrorOr<Value> expand_inline_image_value(Value const& value, HashMap<DeprecatedFlyString, DeprecatedFlyString> const& value_expansions)
{
    if (!value.has<NonnullRefPtr<Object>>())
        return value;

    auto const& object = value.get<NonnullRefPtr<Object>>();
    if (object->is<NameObject>()) {
        auto const& name = object->cast<NameObject>()->name();
        auto expanded_name = value_expansions.get(name);
        if (!expanded_name.has_value())
            return value;
        return Value { make_object<NameObject>(expanded_name.value()) };
    }

    // For the Filters array.
    if (object->is<ArrayObject>()) {
        auto const& array = object->cast<ArrayObject>()->elements();
        Vector<Value> expanded_array;
        for (auto const& element : array) {
            auto expanded_element = TRY(expand_inline_image_value(element, value_expansions));
            expanded_array.append(expanded_element);
        }
        return Value { make_object<ArrayObject>(move(expanded_array)) };
    }

    // For the DecodeParms dict. It might be fine to just `return value` here, I'm not sure if there can really be abbreviations in here.
    if (object->is<DictObject>()) {
        auto const& dict = object->cast<DictObject>()->map();
        HashMap<DeprecatedFlyString, Value> expanded_dict;
        for (auto const& [key, value] : dict) {
            auto expanded_value = TRY(expand_inline_image_value(value, value_expansions));
            expanded_dict.set(key, expanded_value);
        }
        return Value { make_object<DictObject>(move(expanded_dict)) };
    }
    VERIFY_NOT_REACHED();
}

static PDFErrorOr<Value> expand_inline_image_colorspace(Value color_space_value, NonnullRefPtr<DictObject> resources, RefPtr<Document> document)
{
    // PDF 1.7 spec, 4.8.6 Inline Images:
    // "Beginning with PDF 1.2, the value of the ColorSpace entry may also be the name
    //  of a color space in the ColorSpace subdictionary of the current resource dictionary."

    // But PDF 1.7 spec, 4.5.2 Color Space Families:
    // "Outside a content stream, certain objects, such as image XObjects,
    //  specify a color space as an explicit parameter, often associated with
    //  the key ColorSpace. In this case, the color space array or name is
    //  always defined directly as a PDF object, not by an entry in the
    //  ColorSpace resource subdictionary."

    // This converts a named color space of an inline image to an explicit color space object,
    // so that the regular image drawing code tolerates it.

    if (!color_space_value.has<NonnullRefPtr<Object>>())
        return color_space_value;

    auto const& object = color_space_value.get<NonnullRefPtr<Object>>();
    if (!object->is<NameObject>())
        return color_space_value;

    auto const& name = object->cast<NameObject>()->name();
    if (name == "DeviceGray" || name == "DeviceRGB" || name == "DeviceCMYK")
        return color_space_value;

    auto color_space_resource_dict = TRY(resources->get_dict(document, CommonNames::ColorSpace));
    return color_space_resource_dict->get_object(document, name);
}

static PDFErrorOr<NonnullRefPtr<StreamObject>> expand_inline_image_abbreviations(NonnullRefPtr<StreamObject> inline_stream, NonnullRefPtr<DictObject> resources, RefPtr<Document> document)
{
    // TABLE 4.43 Entries in an inline image object
    static HashMap<DeprecatedFlyString, DeprecatedFlyString> key_expansions {
        { "BPC", "BitsPerComponent" },
        { "CS", "ColorSpace" },
        { "D", "Decode" },
        { "DP", "DecodeParms" },
        { "F", "Filter" },
        { "H", "Height" },
        { "IM", "ImageMask" },
        { "I", "Interpolate" },
        { "Intent", "Intent" }, // "No abbreviation"
        { "L", "Length" },      // PDF 2.0; would make more sense to read in Parser.
        { "W", "Width" },
    };

    // TABLE 4.44 Additional abbreviations in an inline image object
    // "Also note that JBIG2Decode and JPXDecode are not listed in Table 4.44
    //  because those filters can be applied only to image XObjects."
    static HashMap<DeprecatedFlyString, DeprecatedFlyString> value_expansions {
        { "G", "DeviceGray" },
        { "RGB", "DeviceRGB" },
        { "CMYK", "DeviceCMYK" },
        { "I", "Indexed" },
        { "AHx", "ASCIIHexDecode" },
        { "A85", "ASCII85Decode" },
        { "LZW", "LZWDecode" },
        { "Fl", "FlateDecode" },
        { "RL", "RunLengthDecode" },
        { "CCF", "CCITTFaxDecode" },
        { "DCT", "DCTDecode" },
    };

    // The values in key_expansions, that is the final expansions, are the valid keys in an inline image dict.
    HashTable<DeprecatedFlyString> valid_keys;
    for (auto const& [key, value] : key_expansions)
        valid_keys.set(value);

    HashMap<DeprecatedFlyString, Value> expanded_dict;
    for (auto const& [key, value] : inline_stream->dict()->map()) {
        DeprecatedFlyString expanded_key = key_expansions.get(key).value_or(key);

        // "Entries other than those listed are ignored"
        if (!valid_keys.contains(expanded_key)) {
            dbgln("PDF: Ignoring invalid inline image key '{}'", expanded_key);
            continue;
        }

        Value expanded_value = TRY(expand_inline_image_value(value, value_expansions));
        if (expanded_key == "ColorSpace")
            expanded_value = TRY(expand_inline_image_colorspace(expanded_value, resources, document));

        expanded_dict.set(expanded_key, expanded_value);
    }

    auto map_object = make_object<DictObject>(move(expanded_dict));
    return make_object<StreamObject>(move(map_object), MUST(ByteBuffer::copy(inline_stream->bytes())));
}

RENDERER_HANDLER(inline_image_end)
{
    VERIFY(args.size() == 1);
    auto inline_stream = args[0].get<NonnullRefPtr<Object>>()->cast<StreamObject>();

    auto resources = extra_resources.value_or(m_page.resources);
    auto expanded_inline_stream = TRY(expand_inline_image_abbreviations(inline_stream, resources, m_document));
    TRY(m_document->unfilter_stream(expanded_inline_stream));

    TRY(show_image(expanded_inline_stream));
    return {};
}

RENDERER_HANDLER(paint_xobject)
{
    VERIFY(args.size() > 0);
    auto resources = extra_resources.value_or(m_page.resources);
    auto xobject_name = args[0].get<NonnullRefPtr<Object>>()->cast<NameObject>()->name();
    auto xobjects_dict = TRY(resources->get_dict(m_document, CommonNames::XObject));
    auto xobject = TRY(xobjects_dict->get_stream(m_document, xobject_name));

    Optional<NonnullRefPtr<DictObject>> xobject_resources {};
    if (xobject->dict()->contains(CommonNames::Resources)) {
        xobject_resources = xobject->dict()->get_dict(m_document, CommonNames::Resources).value();
    }

    auto subtype = MUST(xobject->dict()->get_name(m_document, CommonNames::Subtype))->name();
    if (subtype == CommonNames::Image) {
        TRY(show_image(xobject));
        return {};
    }

    ScopedState scoped_state { *this };

    Vector<Value> matrix;
    if (xobject->dict()->contains(CommonNames::Matrix)) {
        matrix = xobject->dict()->get_array(m_document, CommonNames::Matrix).value()->elements();
    } else {
        matrix = Vector { Value { 1 }, Value { 0 }, Value { 0 }, Value { 1 }, Value { 0 }, Value { 0 } };
    }
    MUST(handle_concatenate_matrix(matrix));
    auto operators = TRY(Parser::parse_operators(m_document, xobject->bytes()));
    for (auto& op : operators)
        TRY(handle_operator(op, xobject_resources));
    return {};
}

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

RENDERER_HANDLER(compatibility_begin)
{
    // We're supposed to ignore unknown operands in compatibility_begin / compatibility_end sections.
    // But we want to know about all operands, so we just ignore this.
    // In practice, it seems like compatibility_begin / compatibility_end were introduced when
    // `sh` was added, and they're used exlusively around `sh`.
    return {};
}

RENDERER_HANDLER(compatibility_end)
{
    // See comment in compatibility_begin.
    return {};
}

template<typename T>
Gfx::Point<T> Renderer::map(T x, T y) const
{
    return state().ctm.map(Gfx::Point<T> { x, y });
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

Gfx::Path Renderer::map(Gfx::Path const& path) const
{
    return path.copy_transformed(state().ctm);
}

float Renderer::line_width() const
{
    // PDF 1.7 spec, 4.3.2 Details of Graphics State Parameters, Line Width:
    // "A line width of 0 denotes the thinnest line that can be rendered at device resolution: 1 device pixel wide."
    if (state().line_width == 0)
        return 1;
    return state().ctm.x_scale() * state().line_width;
}

Gfx::Path::CapStyle Renderer::line_cap_style() const
{
    switch (state().line_cap_style) {
    case LineCapStyle::ButtCap:
        return Gfx::Path::CapStyle::Butt;
    case LineCapStyle::RoundCap:
        return Gfx::Path::CapStyle::Round;
    case LineCapStyle::SquareCap:
        return Gfx::Path::CapStyle::Square;
    }
    VERIFY_NOT_REACHED();
}

Gfx::Path::JoinStyle Renderer::line_join_style() const
{
    switch (state().line_join_style) {
    case LineJoinStyle::Miter:
        return Gfx::Path::JoinStyle::Miter;
    case LineJoinStyle::Round:
        return Gfx::Path::JoinStyle::Round;
    case LineJoinStyle::Bevel:
        return Gfx::Path::JoinStyle::Bevel;
    }
    VERIFY_NOT_REACHED();
}

Gfx::Path::StrokeStyle Renderer::stroke_style() const
{
    auto dash_pattern = state().line_dash_pattern.pattern;

    // The spec doesn't explicitly say how to handle dash patterns with an odd number of elements.
    // <canvas> and <svg> repeat the pattern, so we do the same.
    if (dash_pattern.size() % 2 == 1)
        dash_pattern.extend(dash_pattern);

    for (auto& value : dash_pattern)
        value *= state().ctm.x_scale();
    float dash_phase = state().ctm.x_scale() * state().line_dash_pattern.phase;

    return { line_width(), line_cap_style(), line_join_style(), state().miter_limit, move(dash_pattern), dash_phase };
}

PDFErrorOr<void> Renderer::set_graphics_state_from_dict(NonnullRefPtr<DictObject> dict)
{
    // ISO 32000 (PDF 2.0), 8.4.5 Graphics state parameter dictionaries

    if (dict->contains(CommonNames::LW))
        TRY(handle_set_line_width(Array { dict->get_value(CommonNames::LW) }));

    if (dict->contains(CommonNames::LC))
        TRY(handle_set_line_cap(Array { dict->get_value(CommonNames::LC) }));

    if (dict->contains(CommonNames::LJ))
        TRY(handle_set_line_join(Array { dict->get_value(CommonNames::LJ) }));

    if (dict->contains(CommonNames::ML))
        TRY(handle_set_miter_limit(Array { dict->get_value(CommonNames::ML) }));

    if (dict->contains(CommonNames::D)) {
        auto array = MUST(dict->get_array(m_document, CommonNames::D));
        TRY(handle_set_dash_pattern(array->elements()));
    }

    if (dict->contains(CommonNames::RI))
        TRY(handle_set_color_rendering_intent(Array { dict->get_value(CommonNames::RI) }));

    // FIXME: OP
    // FIXME: op
    // FIXME: OPM
    // FIXME: Font
    // FIXME: BG
    // FIXME: BG2
    // FIXME: UCR
    // FIXME: UCR2
    // FIXME: TR
    // FIXME: TR2
    // FIXME: HT

    if (dict->contains(CommonNames::FL))
        TRY(handle_set_flatness_tolerance(Array { dict->get_value(CommonNames::FL) }));

    // FIXME: SM
    // FIXME: SA
    // FIXME: BM
    // FIXME: SMask
    // FIXME: CA
    // FIXME: ca
    // FIXME: AIS
    // FIXME: TK
    // FIXME: UseBlackPtComp
    // FIXME: HTO

    return {};
}

PDFErrorOr<void> Renderer::show_text(ByteString const& string)
{
    if (!text_state().font)
        return Error::rendering_unsupported_error("Can't draw text because an invalid font was in use");

    OwnPtr<ClipRAII> clip_raii;
    if (m_rendering_preferences.clip_text)
        clip_raii = make<ClipRAII>(*this);

    auto start_position = Gfx::FloatPoint { 0.0f, 0.0f };
    auto end_position = TRY(text_state().font->draw_string(m_painter, start_position, string, *this));

    // Update text matrix.
    auto delta = end_position - start_position;
    m_text_rendering_matrix_is_dirty = true;
    m_text_matrix.translate(delta);
    return {};
}

enum UpsampleMode {
    StoreValuesUnchanged,
    UpsampleTo8Bit,
};
static Vector<u8> upsample_to_8_bit(ReadonlyBytes content, int samples_per_line, int bits_per_component, UpsampleMode mode)
{
    VERIFY(bits_per_component == 1 || bits_per_component == 2 || bits_per_component == 4);
    Vector<u8> upsampled_storage;
    upsampled_storage.ensure_capacity(content.size() * 8 / bits_per_component);
    u8 const mask = (1 << bits_per_component) - 1;

    int x = 0;
    for (auto byte : content) {
        for (int i = 0; i < 8; i += bits_per_component) {
            auto value = (byte >> (8 - bits_per_component - i)) & mask;
            if (mode == UpsampleMode::UpsampleTo8Bit)
                upsampled_storage.append(value * (255 / mask));
            else
                upsampled_storage.append(value);
            ++x;

            // "Byte boundaries are ignored, except that each row of sample data must begin on a byte boundary."
            if (x == samples_per_line) {
                x = 0;
                break;
            }
        }
    }
    return upsampled_storage;
}

PDFErrorOr<Renderer::LoadedImage> Renderer::load_image(NonnullRefPtr<StreamObject> image)
{
    auto image_dict = image->dict();
    auto width = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::Width)));
    auto height = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::Height)));

    auto is_filter = [&](DeprecatedFlyString const& name) -> PDFErrorOr<bool> {
        if (!image_dict->contains(CommonNames::Filter))
            return false;
        auto filter_object = TRY(image_dict->get_object(m_document, CommonNames::Filter));
        if (filter_object->is<NameObject>())
            return filter_object->cast<NameObject>()->name() == name;
        auto filters = filter_object->cast<ArrayObject>();
        if (filters->elements().is_empty())
            return false;
        auto last_filter_index = filters->elements().size() - 1;
        return MUST(filters->get_name_at(m_document, last_filter_index))->name() == name;
    };
    bool is_jpeg2000 = TRY(is_filter(CommonNames::JPXDecode));

    // "SMaskInData specifies whether soft-mask information packaged with the im-
    //  age samples should be used (see “Soft-Mask Images” on page 553); if it is, the
    //  SMask entry is not needed. If SMaskInData is nonzero, there must be only one
    //  opacity channel in the JPEG2000 data and it must apply to all color channels."
    if (is_jpeg2000 && image_dict->contains(CommonNames::SMaskInData)) {
        auto smask_in_data = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::SMaskInData)));
        if (smask_in_data == 0) {
            // "If present, encoded soft-mask image information should be ignored."
            // That's what we currently always do.
        } else if (smask_in_data == 1) {
            // "The image’s data stream includes encoded soft-mask values. An
            //  application can create a soft-mask image from the information to
            //  be used as a source of mask shape or mask opacity in the transpar-
            //  ency imaging model.""
            return Error(Error::Type::RenderingUnsupported, "SMaskInData=1 not yet supported");
        } else if (smask_in_data == 2) {
            // "The image’s data stream includes color channels that have been
            //  preblended with a background; the image data also includes an
            //  opacity channel. An application can create a soft-mask image with
            //  a Matte entry from the opacity channel information to be used as
            //  a source of mask shape or mask opacity in the transparency mod-
            //  el."
            return Error(Error::Type::RenderingUnsupported, "SMaskInData=2 not yet supported");
        } else {
            return Error(Error::Type::MalformedPDF, "Invalid SMaskInData value");
        }
    }

    bool is_image_mask = false;
    if (image_dict->contains(CommonNames::ImageMask)) {
        is_image_mask = TRY(m_document->resolve_to<bool>(image_dict->get_value(CommonNames::ImageMask)));
    }

    if (is_image_mask && is_jpeg2000) {
        // JPEG2000 always returns 8bpp data, but image masks are 1bpp. Need to convert.
        return Error(Error::Type::RenderingUnsupported, "JPEG2000 image masks not yet supported");
    }

    // "(Required for images, except those that use the JPXDecode filter; not allowed for image masks) [...]
    //  it can be any type of color space except Pattern."
    NonnullRefPtr<ColorSpace> color_space = DeviceGrayColorSpace::the();
    if (!is_image_mask) {
        // "If ColorSpace is not present in the image dictionary, the color space informa-
        //  tion in the JPEG2000 data is used."
        // FIXME: When implementing this, check how palettized JPEG2000 images with color space
        //        from the image are handled. I suppose the palette embedded in the image is then used?
        if (!image_dict->contains(CommonNames::ColorSpace) && is_jpeg2000)
            return Error(Error::Type::RenderingUnsupported, "Using color space from jpeg2000 image not yet implemented");
        auto color_space_object = MUST(image_dict->get_object(m_document, CommonNames::ColorSpace));
        color_space = TRY(get_color_space_from_document(color_space_object));
    }

    auto color_rendering_intent = state().color_rendering_intent;
    if (image_dict->contains(CommonNames::Intent))
        color_rendering_intent = TRY(image_dict->get_name(m_document, CommonNames::Intent))->name();
    // FIXME: Do something with color_rendering_intent.

    // "Valid values are 1, 2, 4, 8, and (in PDF 1.5) 16."
    // Per spec, this is required even for /Mask images, but it's required to be 1 there.
    // In practice, it's sometimes missing for /Mask images.
    // "If the image stream uses the JPXDecode filter, this entry is optional and ignored if present."
    auto bits_per_component = 1;
    if (is_jpeg2000)
        bits_per_component = 8;
    else if (!is_image_mask)
        bits_per_component = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::BitsPerComponent)));
    switch (bits_per_component) {
    case 1:
    case 2:
    case 4:
    case 8:
    case 16:
        // Ok!
        break;
    default:
        return Error(Error::Type::MalformedPDF, "Image's /BitsPerComponent invalid");
    }
    auto content = image->bytes();

    int const n_components = color_space->number_of_components();

    // PDF 1.7 spec, 3.3.8 JPXDecode Filter:
    // "Decode is ignored, except in the case where the image is treated as a mask; that is, when ImageMask is true.
    //  In this case, the JPEG2000 data must provide a single color channel with 1-bit samples."
    Vector<float> decode_array;
    if ((!is_jpeg2000 || is_image_mask) && image_dict->contains(CommonNames::Decode)) {
        decode_array = MUST(image_dict->get_array(m_document, CommonNames::Decode))->float_elements();
    } else {
        decode_array = color_space->default_decode();
    }

    if (bits_per_component == 1 && color_space->family() == ColorSpaceFamily::DeviceGray) {
        // Fast path for 1bpp grayscale. Used for masks and scanned pages (CCITT or JBIG2).
        // FIXME: This fast path could work for CalGray and Indexed too,
        //        but IndexedColorSpace::default_decode() currently assumes 8bpp.
        auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { width, height }));

        Color colors[] = {
            TRY(color_space->style({ &decode_array[0], 1 })).get<Color>(),
            TRY(color_space->style({ &decode_array[1], 1 })).get<Color>(),
        };

        auto const bytes_per_line = ceil_div(width, 8);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                auto byte = content[y * bytes_per_line + x / 8];
                auto bit = 7 - (x % 8);
                auto color = colors[(byte >> bit) & 1];
                bitmap->set_pixel(x, y, color);
            }
        }

        return LoadedImage { bitmap, is_image_mask };
    }

    Vector<u8> resampled_storage;
    if (bits_per_component < 8) {
        UpsampleMode mode = color_space->family() == ColorSpaceFamily::Indexed ? UpsampleMode::StoreValuesUnchanged : UpsampleMode::UpsampleTo8Bit;
        resampled_storage = upsample_to_8_bit(content, width * n_components, bits_per_component, mode);
        content = resampled_storage;
        bits_per_component = 8;
    } else if (bits_per_component == 16) {
        if (color_space->family() == ColorSpaceFamily::Indexed)
            return Error(Error::Type::RenderingUnsupported, "16 bpp indexed images not yet supported");

        // PDF 1.7 spec, 4.8.2 Sample Representation:
        // "units of 16 bits are given with the most significant byte first"
        // FIXME: Eventually use all 16 bits instead of throwing away the lower 8 bits.
        resampled_storage.ensure_capacity(content.size() / 2);
        for (size_t i = 0; i < content.size(); i += 2)
            resampled_storage.append(content[i]);

        content = resampled_storage;
        bits_per_component = 8;
    }

    Vector<LinearInterpolation1D> component_value_decoders;
    component_value_decoders.ensure_capacity(decode_array.size());
    for (size_t i = 0; i < decode_array.size(); i += 2) {
        auto dmin = decode_array[i];
        auto dmax = decode_array[i + 1];
        component_value_decoders.empend(0.0f, 255.0f, dmin, dmax);
    }

    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { width, height }));
    int x = 0;
    int y = 0;
    auto const bytes_per_component = bits_per_component / 8;
    Vector<float> component_values;
    component_values.resize(n_components);
    while (!content.is_empty() && y < height) {
        auto sample = content.slice(0, bytes_per_component * n_components);
        content = content.slice(bytes_per_component * n_components);
        for (int i = 0; i < n_components; ++i) {
            auto component = sample.slice(0, bytes_per_component);
            sample = sample.slice(bytes_per_component);
            component_values[i] = component_value_decoders[i].interpolate(component[0]);
        }
        auto color = TRY(color_space->style(component_values)).get<Color>();
        bitmap->set_pixel(x, y, color);
        ++x;
        if (x == width) {
            x = 0;
            ++y;
        }
    }
    return LoadedImage { bitmap, is_image_mask };
}

PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> Renderer::make_mask_bitmap_from_array(NonnullRefPtr<ArrayObject> array, NonnullRefPtr<StreamObject> image)
{
    // PDF 1.7 spec, 4.8.5. Masked Images, Color Key Masking
    // "For color key masking, the value of the Mask entry is an array of 2 × n integers, [min_1 max_1 ... min_n max_n],
    //  where n is the number of color components in the image’s color space. Each integer must be in the range 0 to 2**(BitsPerComponent − 1),
    //  representing color values _before_ decoding with the Decode array.
    //  An image sample is masked [...] if min_i ≤ c_i ≤ max_i for all 1 ≤ i ≤ n."
    // For indexed images, this means the array masks the index, not the color.
    auto image_dict = image->dict();
    auto width = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::Width)));
    auto height = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::Height)));
    auto bits_per_component = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::BitsPerComponent)));
    VERIFY(bits_per_component == 1 || bits_per_component == 2 || bits_per_component == 4 || bits_per_component == 8 || bits_per_component == 16);

    if (array->size() % 2 != 0)
        return Error(Error::Type::MalformedPDF, "Mask array must have an even number of elements");
    auto n_components = array->size() / 2;
    Vector<int, 4> min, max;
    for (size_t i = 0; i < n_components; ++i) {
        min.append(array->at(i * 2).to_int());
        max.append(array->at(i * 2 + 1).to_int());
    }

    auto mask_bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { width, height }));
    auto bit_stream = make<BigEndianInputBitStream>(make<FixedMemoryStream>(image->bytes()));
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            bool is_masked = true;
            for (size_t i = 0; i < n_components; ++i) {
                u16 sample = TRY(bit_stream->read_bits(bits_per_component));
                if (sample < min[i] || sample > max[i]) {
                    is_masked = false;
                    TRY(bit_stream->read_bits((n_components - 1 - i) * bits_per_component));
                    break;
                }
            }
            mask_bitmap->set_pixel(x, y, Color::from_argb(is_masked ? 0x00'00'00'00 : 0xff'ff'ff'ff));
        }
        bit_stream->align_to_byte_boundary();
    }
    return mask_bitmap;
}

Gfx::AffineTransform Renderer::calculate_image_space_transformation(Gfx::IntSize size)
{
    // Image space maps to a 1x1 unit of user space and starts at the top-left
    auto image_space = state().ctm;
    image_space.multiply(Gfx::AffineTransform(
        1.0f / size.width(),
        0.0f,
        0.0f,
        -1.0f / size.height(),
        0.0f,
        1.0f));
    return image_space;
}

void Renderer::show_empty_image(Gfx::IntSize size)
{
    auto image_space_transformation = calculate_image_space_transformation(size);
    auto image_border = image_space_transformation.map(Gfx::IntRect { {}, size });
    m_painter.stroke_path(rect_path(image_border), Color::Black, 1);
}

static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> apply_alpha_channel(NonnullRefPtr<Gfx::Bitmap> image_bitmap, NonnullRefPtr<Gfx::Bitmap const> mask_bitmap, bool invert_alpha = false)
{
    // Make alpha mask same size as image.
    if (mask_bitmap->size() != image_bitmap->size()) {
        // Some files have 2x2 images for color and huge masks that contain rendered text outlines.
        // So resize to the larger of the two.
        auto new_size = Gfx::IntSize { max(image_bitmap->width(), mask_bitmap->width()), max(image_bitmap->height(), mask_bitmap->height()) };
        if (image_bitmap->size() != new_size)
            image_bitmap = TRY(image_bitmap->scaled_to_size(new_size));
        if (mask_bitmap->size() != new_size)
            mask_bitmap = TRY(mask_bitmap->scaled_to_size(new_size));
    }

    image_bitmap->add_alpha_channel();
    for (int j = 0; j < image_bitmap->height(); ++j) {
        for (int i = 0; i < image_bitmap->width(); ++i) {
            auto image_color = image_bitmap->get_pixel(i, j);
            auto mask_color = mask_bitmap->get_pixel(i, j);
            u8 alpha = mask_color.luminosity();
            if (invert_alpha)
                alpha = 255 - alpha;
            image_color = image_color.with_alpha(alpha);
            image_bitmap->set_pixel(i, j, image_color);
        }
    }
    return image_bitmap;
}

PDFErrorOr<void> Renderer::show_image(NonnullRefPtr<StreamObject> image)
{
    auto image_dict = image->dict();

    OwnPtr<ClipRAII> clip_raii;
    if (m_rendering_preferences.clip_images)
        clip_raii = make<ClipRAII>(*this);

    if (!m_rendering_preferences.show_images) {
        auto width = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::Width)));
        auto height = TRY(m_document->resolve_to<int>(image_dict->get_value(CommonNames::Height)));
        show_empty_image({ width, height });
        return {};
    }
    auto image_bitmap = TRY(load_image(image));
    if (image_bitmap.is_image_mask) {
        // PDF 1.7 spec, 4.8.5 Masked Images, Stencil Masking:
        // "An image mask (an image XObject whose ImageMask entry is true) [...] is treated as a stencil mask [...].
        //  Sample values [...] designate places on the page that should either be marked with the current color or masked out (not marked at all)."
        if (!state().paint_style.has<Gfx::Color>())
            return Error(Error::Type::RenderingUnsupported, "Image masks with pattern fill not yet implemented");

        // Move mask to alpha channel, and put current color in RGB.
        auto current_color = state().paint_style.get<Gfx::Color>();
        for (auto& pixel : *image_bitmap.bitmap) {
            // "a sample value of 0 marks the page with the current color, and a 1 leaves the previous contents unchanged."
            // That's opposite of the normal alpha convention, and we're upsampling masks to 8 bit and use that as normal alpha.
            u8 mask_alpha = 255 - Color::from_argb(pixel).luminosity();
            pixel = current_color.with_alpha(mask_alpha).value();
        }
    } else if (image_dict->contains(CommonNames::SMask)) {
        auto smask_bitmap = TRY(load_image(TRY(image_dict->get_stream(m_document, CommonNames::SMask))));
        image_bitmap.bitmap = TRY(apply_alpha_channel(image_bitmap.bitmap, smask_bitmap.bitmap));
    } else if (image_dict->contains(CommonNames::Mask)) {
        auto mask_object = TRY(image_dict->get_object(m_document, CommonNames::Mask));
        if (mask_object->is<StreamObject>()) {
            auto mask_bitmap = TRY(load_image(mask_object->cast<StreamObject>()));
            bool invert_alpha = mask_bitmap.is_image_mask;
            image_bitmap.bitmap = TRY(apply_alpha_channel(image_bitmap.bitmap, mask_bitmap.bitmap, invert_alpha));
        } else if (mask_object->is<ArrayObject>()) {
            auto mask_bitmap = TRY(make_mask_bitmap_from_array(mask_object->cast<ArrayObject>(), image));
            image_bitmap.bitmap = TRY(apply_alpha_channel(image_bitmap.bitmap, mask_bitmap));
        }
    }

    auto image_space = calculate_image_space_transformation(image_bitmap.bitmap->size());
    auto image_rect = Gfx::FloatRect { image_bitmap.bitmap->rect() };
    m_painter.draw_scaled_bitmap_with_transform(image_bitmap.bitmap->rect(), image_bitmap.bitmap, image_rect, image_space);
    return {};
}

PDFErrorOr<NonnullRefPtr<ColorSpace>> Renderer::get_color_space_from_resources(Value const& value, NonnullRefPtr<DictObject> resources)
{
    auto color_space_name = value.get<NonnullRefPtr<Object>>()->cast<NameObject>()->name();
    auto maybe_color_space_family = ColorSpaceFamily::get(color_space_name);
    if (!maybe_color_space_family.is_error()) {
        auto color_space_family = maybe_color_space_family.release_value();
        if (color_space_family.may_be_specified_directly()) {
            return ColorSpace::create(color_space_name, *this);
        }
    }
    auto color_space_resource_dict = TRY(resources->get_dict(m_document, CommonNames::ColorSpace));
    if (!color_space_resource_dict->contains(color_space_name)) {
        dbgln("missing key {}", color_space_name);
        return Error::rendering_unsupported_error("Missing entry for color space name");
    }
    return get_color_space_from_document(TRY(color_space_resource_dict->get_object(m_document, color_space_name)));
}

PDFErrorOr<NonnullRefPtr<ColorSpace>> Renderer::get_color_space_from_document(NonnullRefPtr<Object> color_space_object)
{
    return ColorSpace::create(m_document, color_space_object, *this);
}

Gfx::AffineTransform const& Renderer::calculate_text_rendering_matrix() const
{
    if (m_text_rendering_matrix_is_dirty) {
        // PDF 1.7, 5.3.3. Text Space Details
        Gfx::AffineTransform parameter_matrix {
            text_state().horizontal_scaling,
            0.0f,
            0.0f,
            1.0f,
            0.0f,
            text_state().rise
        };
        m_text_rendering_matrix = state().ctm;
        m_text_rendering_matrix.multiply(m_text_matrix);
        m_text_rendering_matrix.multiply(parameter_matrix);
        m_text_rendering_matrix_is_dirty = false;
    }
    return m_text_rendering_matrix;
}

PDFErrorOr<void> Renderer::render_type3_glyph(Gfx::FloatPoint point, StreamObject const& glyph_data, Gfx::AffineTransform const& font_matrix, Optional<NonnullRefPtr<DictObject>> resources)
{
    ScopedState scoped_state { *this };

    auto text_rendering_matrix = calculate_text_rendering_matrix();
    text_rendering_matrix.set_translation(point);
    state().ctm = text_rendering_matrix;
    state().ctm.scale(text_state().font_size, text_state().font_size);
    state().ctm.multiply(font_matrix);
    m_text_rendering_matrix_is_dirty = true;

    auto operators = TRY(Parser::parse_operators(m_document, glyph_data.bytes()));
    for (auto& op : operators)
        TRY(handle_operator(op, resources));
    return {};
}

}
