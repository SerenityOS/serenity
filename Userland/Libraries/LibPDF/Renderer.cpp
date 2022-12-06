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
    PDFErrorOr<void> Renderer::handle_##name([[maybe_unused]] Vector<Value> const& args, [[maybe_unused]] Optional<NonnullRefPtr<DictObject>> extra_resources)

#define RENDERER_TODO(name)                                         \
    RENDERER_HANDLER(name)                                          \
    {                                                               \
        dbgln("[PDF::Renderer] Unsupported draw operation " #name); \
        TODO();                                                     \
    }

namespace PDF {

PDFErrorOr<void> Renderer::render(Document& document, Page const& page, RefPtr<Gfx::Bitmap> bitmap, RenderingPreferences rendering_preferences)
{
    return Renderer(document, page, bitmap, rendering_preferences).render();
}

static void rect_path(Gfx::Path& path, float x, float y, float width, float height)
{
    path.move_to({ x, y });
    path.line_to({ x + width, y });
    path.line_to({ x + width, y + height });
    path.line_to({ x, y + height });
    path.close();
}

static Gfx::Path rect_path(float x, float y, float width, float height)
{
    Gfx::Path path;
    rect_path(path, x, y, width, height);
    return path;
}

template<typename T>
static void rect_path(Gfx::Path& path, Gfx::Rect<T> rect)
{
    return rect_path(path, rect.x(), rect.y(), rect.width(), rect.height());
}

template<typename T>
static Gfx::Path rect_path(Gfx::Rect<T> rect)
{
    Gfx::Path path;
    rect_path(path, rect);
    return path;
}

Renderer::Renderer(RefPtr<Document> document, Page const& page, RefPtr<Gfx::Bitmap> bitmap, RenderingPreferences rendering_preferences)
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

    static Gfx::AffineTransform horizontal_reflection_matrix = { 1, 0, 0, -1, 0, 0 };

    userspace_matrix.multiply(horizontal_reflection_matrix);
    userspace_matrix.translate(0.0f, -height);

    auto initial_clipping_path = rect_path(0, 0, width, height);
    m_graphics_state_stack.append(GraphicsState { userspace_matrix, { initial_clipping_path, initial_clipping_path } });

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

PDFErrorOr<void> Renderer::handle_operator(Operator const& op, Optional<NonnullRefPtr<DictObject>> extra_resources)
{
    switch (op.type()) {
#define V(name, snake_name, symbol)                                 \
    case OperatorType::name:                                        \
        MUST(handle_##snake_name(op.arguments(), extra_resources)); \
        break;
        ENUMERATE_OPERATORS(V)
#undef V
    case OperatorType::TextNextLineShowString:
        MUST(handle_text_next_line_show_string(op.arguments()));
        break;
    case OperatorType::TextNextLineShowStringSetSpacing:
        MUST(handle_text_next_line_show_string_set_spacing(op.arguments()));
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
        pattern.append(element.to_int());
    state().line_dash_pattern = LineDashPattern { pattern, args[1].get<int>() };
    return {};
}

RENDERER_TODO(set_color_rendering_intent)
RENDERER_TODO(set_flatness_tolerance)

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
    VERIFY(!m_current_path.segments().is_empty());
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
    VERIFY(!m_current_path.segments().is_empty());
    auto current_point = m_current_path.segments().rbegin()->point();
    m_current_path.cubic_bezier_curve_to(
        current_point,
        map(args[0].to_float(), args[1].to_float()),
        map(args[2].to_float(), args[3].to_float()));
    return {};
}

RENDERER_HANDLER(path_cubic_bezier_curve_no_second_control)
{
    VERIFY(args.size() == 4);
    VERIFY(!m_current_path.segments().is_empty());
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
    rect_path(m_current_path, map(rect));
    return {};
}

///
// Path painting operations
///

void Renderer::begin_path_paint()
{
    auto bounding_box = map(state().clipping_paths.current.bounding_box());
    m_painter.clear_clip_rect();
    if (m_rendering_preferences.show_clipping_paths) {
        m_painter.stroke_path(rect_path(bounding_box), Color::Black, 1);
    }
    m_painter.add_clip_rect(bounding_box.to_type<int>());
}

void Renderer::end_path_paint()
{
    m_current_path.clear();
    m_painter.clear_clip_rect();
    state().clipping_paths.current = state().clipping_paths.next;
}

RENDERER_HANDLER(path_stroke)
{
    begin_path_paint();
    m_anti_aliasing_painter.stroke_path(m_current_path, state().stroke_color, state().line_width);
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
    m_anti_aliasing_painter.fill_path(m_current_path, state().paint_color, Gfx::Painter::WindingRule::Nonzero);
    end_path_paint();
    return {};
}

RENDERER_HANDLER(path_fill_nonzero_deprecated)
{
    TRY(handle_path_fill_nonzero(args));
    return {};
}

RENDERER_HANDLER(path_fill_evenodd)
{
    begin_path_paint();
    m_anti_aliasing_painter.fill_path(m_current_path, state().paint_color, Gfx::Painter::WindingRule::EvenOdd);
    end_path_paint();
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
    auto resources = extra_resources.value_or(m_page.resources);
    auto target_font_name = MUST(m_document->resolve_to<NameObject>(args[0]))->name();
    auto fonts_dictionary = MUST(resources->get_dict(m_document, CommonNames::Font));
    auto font_dictionary = MUST(fonts_dictionary->get_dict(m_document, target_font_name));

    text_state().font_size = args[1].to_float();

    auto& text_rendering_matrix = calculate_text_rendering_matrix();
    auto font_size = text_rendering_matrix.x_scale() * text_state().font_size;
    auto font = TRY(PDFFont::create(m_document, font_dictionary, font_size));
    text_state().font = font;

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
            auto shift = next_shift / 1000.0f;
            m_text_matrix.translate(-shift * text_state().font_size * text_state().horizontal_scaling, 0.0f);
            auto str = element.get<NonnullRefPtr<Object>>()->cast<StringObject>()->string();
            show_text(str);
        }
    }

    return {};
}

RENDERER_TODO(type3_font_set_glyph_width)
RENDERER_TODO(type3_font_set_glyph_width_and_bbox)

RENDERER_HANDLER(set_stroking_space)
{
    state().stroke_color_space = TRY(get_color_space(args[0], extra_resources.value_or(m_page.resources)));
    VERIFY(state().stroke_color_space);
    return {};
}

RENDERER_HANDLER(set_painting_space)
{
    state().paint_color_space = TRY(get_color_space(args[0], extra_resources.value_or(m_page.resources)));
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
RENDERER_HANDLER(paint_xobject)
{
    VERIFY(args.size() > 0);
    auto resources = extra_resources.value_or(m_page.resources);
    auto xobject_name = args[0].get<NonnullRefPtr<Object>>()->cast<NameObject>()->name();
    auto xobjects_dict = MUST(resources->get_dict(m_document, CommonNames::XObject));
    auto xobject = MUST(xobjects_dict->get_stream(m_document, xobject_name));

    auto subtype = MUST(xobject->dict()->get_name(m_document, CommonNames::Subtype))->name();
    if (subtype == CommonNames::Image) {
        dbgln("Skipping image");
        return {};
    }

    MUST(handle_save_state({}));
    Vector<Value> matrix;
    if (xobject->dict()->contains(CommonNames::Matrix)) {
        matrix = xobject->dict()->get_array(m_document, CommonNames::Matrix).value()->elements();
    } else {
        matrix = Vector { Value { 1 }, Value { 0 }, Value { 0 }, Value { 1 }, Value { 0 }, Value { 0 } };
    }
    MUST(handle_concatenate_matrix(matrix));
    Optional<NonnullRefPtr<DictObject>> xobject_resources {};
    if (xobject->dict()->contains(CommonNames::Resources)) {
        xobject_resources = xobject->dict()->get_dict(m_document, CommonNames::Resources).value();
    }
    auto operators = TRY(Parser::parse_operators(m_document, xobject->bytes()));
    for (auto& op : operators)
        TRY(handle_operator(op, xobject_resources));
    MUST(handle_restore_state({}));
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

RENDERER_TODO(compatibility_begin)
RENDERER_TODO(compatibility_end)

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

void Renderer::show_text(DeprecatedString const& string)
{
    auto& text_rendering_matrix = calculate_text_rendering_matrix();

    auto font_size = text_rendering_matrix.x_scale() * text_state().font_size;

    auto glyph_position = text_rendering_matrix.map(Gfx::FloatPoint { 0.0f, 0.0f });

    auto original_position = glyph_position;

    for (auto char_code : string.bytes()) {
        auto code_point = text_state().font->char_code_to_code_point(char_code);
        auto char_width = text_state().font->get_char_width(char_code);
        auto glyph_width = char_width * font_size;

        if (code_point != 0x20)
            text_state().font->draw_glyph(m_painter, glyph_position.to_type<int>(), glyph_width, char_code, state().paint_color);

        auto tx = glyph_width;
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

PDFErrorOr<NonnullRefPtr<ColorSpace>> Renderer::get_color_space(Value const& value, NonnullRefPtr<DictObject> resources)
{
    auto name = value.get<NonnullRefPtr<Object>>()->cast<NameObject>()->name();
    return TRY(ColorSpace::create(m_document, name, resources));
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
