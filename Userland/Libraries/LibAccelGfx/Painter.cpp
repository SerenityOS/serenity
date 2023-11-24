/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibAccelGfx/GL.h>
#include <LibAccelGfx/Painter.h>
#include <LibGfx/Color.h>
#include <LibGfx/ImmutableBitmap.h>
#include <LibGfx/Painter.h>

namespace AccelGfx {

struct ColorComponents {
    float red;
    float green;
    float blue;
    float alpha;
};

static ColorComponents gfx_color_to_opengl_color(Gfx::Color color)
{
    ColorComponents components;
    components.red = static_cast<float>(color.red()) / 255.0f;
    components.green = static_cast<float>(color.green()) / 255.0f;
    components.blue = static_cast<float>(color.blue()) / 255.0f;
    components.alpha = static_cast<float>(color.alpha()) / 255.0f;
    return components;
}

Gfx::FloatRect Painter::to_clip_space(Gfx::FloatRect const& screen_rect) const
{
    float x = 2.0f * screen_rect.x() / m_target_canvas->size().width() - 1.0f;
    float y = -1.0f + 2.0f * screen_rect.y() / m_target_canvas->size().height();

    float width = 2.0f * screen_rect.width() / m_target_canvas->size().width();
    float height = 2.0f * screen_rect.height() / m_target_canvas->size().height();

    return { x, y, width, height };
}

char const* vertex_shader_source = R"(
#version 330 core
in vec2 aVertexPosition;
void main() {
    gl_Position = vec4(aVertexPosition, 0.0, 1.0);
}
)";

char const* rect_with_rounded_corners_fragment_shader_source = R"(
#version 330 core
uniform vec2 uRectCenter;
uniform vec2 uRectCorner;
uniform vec2 uTopLeftRadius;
uniform vec2 uTopRightRadius;
uniform vec2 uBottomLeftRadius;
uniform vec2 uBottomRightRadius;
uniform vec4 uColor;
out vec4 fragColor;

bool isPointWithinEllipse(vec2 point, vec2 radius) {
    vec2 normalizedPoint = point / radius;
    return dot(normalizedPoint, normalizedPoint) <= 1.0;
}

void main() {
    vec2 p = gl_FragCoord.xy - uRectCenter;
    vec2 cornerRadius = vec2(0.0, 0.0);
    if (p.x < 0.0 && p.y < 0.0) {
        cornerRadius = uTopLeftRadius;
    } else if (p.x > 0.0 && p.y < 0.0) {
        cornerRadius = uTopRightRadius;
    } else if (p.x < 0.0 && p.y > 0.0) {
        cornerRadius = uBottomLeftRadius;
    } else if (p.x > 0.0 && p.y > 0.0) {
        cornerRadius = uBottomRightRadius;
    }
    vec2 q = abs(p) - (uRectCorner - cornerRadius);
    if (q.x < 0 || q.y < 0 || isPointWithinEllipse(q, cornerRadius)) {
        fragColor = uColor;
    } else {
        discard;
    }
}
)";

char const* solid_color_fragment_shader_source = R"(
#version 330 core
uniform vec4 uColor;
out vec4 fragColor;
void main() {
    fragColor = uColor;
}
)";

char const* blit_vertex_shader_source = R"(
#version 330 core
in vec4 aVertexPosition;
out vec2 vTextureCoord;
void main() {
    gl_Position = vec4(aVertexPosition.xy, 0.0, 1.0);
    vTextureCoord = aVertexPosition.zw;
}
)";

char const* blit_fragment_shader_source = R"(
#version 330 core
uniform vec4 uColor;
in vec2 vTextureCoord;
uniform sampler2D uSampler;
out vec4 fragColor;
void main() {
    fragColor = texture(uSampler, vTextureCoord) * uColor;
}
)";

char const* linear_gradient_vertex_shader_source = R"(
#version 330 core
layout (location = 0) in vec2 aVertexPosition;
layout (location = 1) in vec4 aColor;
out vec4 vColor;
void main() {
    gl_Position = vec4(aVertexPosition, 0.0, 1.0);
    vColor = aColor;
}
)";

char const* linear_gradient_fragment_shader_source = R"(
#version 330 core
out vec4 FragColor;
in vec4 vColor;
void main() {
    FragColor = vec4(vColor);
}
)";

HashMap<u32, GL::Texture> s_immutable_bitmap_texture_cache;

OwnPtr<Painter> Painter::create()
{
    auto& context = Context::the();
    return make<Painter>(context);
}

OwnPtr<Painter> Painter::create_with_glyphs_texture_from_painter(Painter const& painter)
{
    auto& context = Context::the();
    auto glyphs_texture_painter = make<Painter>(context);
    glyphs_texture_painter->m_glyphs_texture = painter.m_glyphs_texture;
    glyphs_texture_painter->m_glyphs_texture_size = painter.m_glyphs_texture_size;
    glyphs_texture_painter->m_glyphs_texture_map = painter.m_glyphs_texture_map;
    return glyphs_texture_painter;
}

Painter::Painter(Context& context)
    : m_context(context)
    , m_rectangle_program(Program::create(Program::Name::RectangleProgram, vertex_shader_source, solid_color_fragment_shader_source))
    , m_rounded_rectangle_program(Program::create(Program::Name::RoundedRectangleProgram, vertex_shader_source, rect_with_rounded_corners_fragment_shader_source))
    , m_blit_program(Program::create(Program::Name::BlitProgram, blit_vertex_shader_source, blit_fragment_shader_source))
    , m_linear_gradient_program(Program::create(Program::Name::LinearGradientProgram, linear_gradient_vertex_shader_source, linear_gradient_fragment_shader_source))
    , m_glyphs_texture(GL::create_texture())
{
    m_state_stack.empend(State());
}

Painter::~Painter()
{
}

void Painter::clear(Gfx::Color color)
{
    GL::clear_color(color);
}

void Painter::fill_rect(Gfx::IntRect rect, Gfx::Color color)
{
    fill_rect(rect.to_type<float>(), color);
}

static Array<GLfloat, 8> rect_to_vertices(Gfx::FloatRect const& rect)
{
    return {
        rect.left(),
        rect.top(),
        rect.left(),
        rect.bottom(),
        rect.right(),
        rect.bottom(),
        rect.right(),
        rect.top(),
    };
}

void Painter::fill_rect(Gfx::FloatRect rect, Gfx::Color color)
{
    bind_target_canvas();

    // Draw a filled rect (with `color`) using OpenGL after mapping it through the current transform.

    auto vertices = rect_to_vertices(to_clip_space(transform().map(rect)));

    auto vbo = GL::create_buffer();
    GL::upload_to_buffer(vbo, vertices);

    auto vao = GL::create_vertex_array();
    GL::bind_vertex_array(vao);
    GL::bind_buffer(vbo);

    auto [red, green, blue, alpha] = gfx_color_to_opengl_color(color);

    m_rectangle_program.use();

    auto position_attribute = m_rectangle_program.get_attribute_location("aVertexPosition");
    auto color_uniform = m_rectangle_program.get_uniform_location("uColor");

    GL::set_uniform(color_uniform, red, green, blue, alpha);
    GL::set_vertex_attribute(position_attribute, 0, 2);
    GL::enable_blending();
    GL::draw_arrays(GL::DrawPrimitive::TriangleFan, 4);

    GL::delete_buffer(vbo);
    GL::delete_vertex_array(vao);
}

void Painter::fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color const& color, CornerRadius const& top_left_radius, CornerRadius const& top_right_radius, CornerRadius const& bottom_left_radius, CornerRadius const& bottom_right_radius)
{
    fill_rect_with_rounded_corners(rect.to_type<float>(), color, top_left_radius, top_right_radius, bottom_left_radius, bottom_right_radius);
}

void Painter::fill_rect_with_rounded_corners(Gfx::FloatRect const& rect, Color const& color, CornerRadius const& top_left_radius, CornerRadius const& top_right_radius, CornerRadius const& bottom_left_radius, CornerRadius const& bottom_right_radius)
{
    bind_target_canvas();

    auto transformed_rect = transform().map(rect);
    auto vertices = rect_to_vertices(to_clip_space(transformed_rect));

    auto vbo = GL::create_buffer();
    GL::upload_to_buffer(vbo, vertices);

    auto vao = GL::create_vertex_array();
    GL::bind_vertex_array(vao);
    GL::bind_buffer(vbo);

    auto [red, green, blue, alpha] = gfx_color_to_opengl_color(color);

    m_rounded_rectangle_program.use();

    auto position_attribute = m_rounded_rectangle_program.get_attribute_location("aVertexPosition");
    GL::set_vertex_attribute(position_attribute, 0, 2);

    auto color_uniform = m_rounded_rectangle_program.get_uniform_location("uColor");
    GL::set_uniform(color_uniform, red, green, blue, alpha);

    auto rect_center_uniform = m_rounded_rectangle_program.get_uniform_location("uRectCenter");
    GL::set_uniform(rect_center_uniform, transformed_rect.center().x(), transformed_rect.center().y());
    auto rect_corner_uniform = m_rounded_rectangle_program.get_uniform_location("uRectCorner");
    GL::set_uniform(rect_corner_uniform, rect.width() / 2, rect.height() / 2);
    auto top_left_corner_radius_uniform = m_rounded_rectangle_program.get_uniform_location("uTopLeftRadius");
    GL::set_uniform(top_left_corner_radius_uniform, top_left_radius.horizontal_radius, top_left_radius.vertical_radius);
    auto top_right_corner_radius_uniform = m_rounded_rectangle_program.get_uniform_location("uTopRightRadius");
    GL::set_uniform(top_right_corner_radius_uniform, top_right_radius.horizontal_radius, top_right_radius.vertical_radius);
    auto bottom_left_corner_radius_uniform = m_rounded_rectangle_program.get_uniform_location("uBottomLeftRadius");
    GL::set_uniform(bottom_left_corner_radius_uniform, bottom_left_radius.horizontal_radius, bottom_left_radius.vertical_radius);
    auto bottom_right_corner_radius_uniform = m_rounded_rectangle_program.get_uniform_location("uBottomRightRadius");
    GL::set_uniform(bottom_right_corner_radius_uniform, bottom_right_radius.horizontal_radius, bottom_right_radius.vertical_radius);

    GL::enable_blending();
    GL::draw_arrays(GL::DrawPrimitive::TriangleFan, 4);

    GL::delete_buffer(vbo);
    GL::delete_vertex_array(vao);
}

void Painter::draw_line(Gfx::IntPoint a, Gfx::IntPoint b, float thickness, Gfx::Color color)
{
    draw_line(a.to_type<float>(), b.to_type<float>(), thickness, color);
}

void Painter::draw_line(Gfx::FloatPoint a, Gfx::FloatPoint b, float thickness, Color color)
{
    bind_target_canvas();

    auto midpoint = (a + b) / 2.0f;
    auto length = a.distance_from(b);
    auto angle = AK::atan2(b.y() - a.y(), b.x() - a.x());
    auto offset = Gfx::FloatPoint {
        (length / 2) * AK::cos(angle) - (thickness / 2) * AK::sin(angle),
        (length / 2) * AK::sin(angle) + (thickness / 2) * AK::cos(angle),
    };
    auto rect = Gfx::FloatRect(midpoint - offset, { length, thickness });

    auto vertices = rect_to_vertices(to_clip_space(transform().map(rect)));

    auto vbo = GL::create_buffer();
    GL::upload_to_buffer(vbo, vertices);

    auto vao = GL::create_vertex_array();
    GL::bind_vertex_array(vao);
    GL::bind_buffer(vbo);

    auto [red, green, blue, alpha] = gfx_color_to_opengl_color(color);

    m_rectangle_program.use();

    auto position_attribute = m_rectangle_program.get_attribute_location("aVertexPosition");
    auto color_uniform = m_rectangle_program.get_uniform_location("uColor");

    GL::set_uniform(color_uniform, red, green, blue, alpha);
    GL::set_vertex_attribute(position_attribute, 0, 2);
    GL::enable_blending();
    GL::draw_arrays(GL::DrawPrimitive::TriangleFan, 4);

    GL::delete_buffer(vbo);
    GL::delete_vertex_array(vao);
}

void Painter::draw_scaled_bitmap(Gfx::IntRect const& dest_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, ScalingMode scaling_mode)
{
    draw_scaled_bitmap(dest_rect.to_type<float>(), bitmap, src_rect.to_type<float>(), scaling_mode);
}

void Painter::draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const& immutable_bitmap, Gfx::IntRect const& src_rect, ScalingMode scaling_mode)
{
    draw_scaled_immutable_bitmap(dst_rect.to_type<float>(), immutable_bitmap, src_rect.to_type<float>(), scaling_mode);
}

void Painter::draw_scaled_immutable_bitmap(Gfx::FloatRect const& dst_rect, Gfx::ImmutableBitmap const& immutable_bitmap, Gfx::FloatRect const& src_rect, ScalingMode scaling_mode)
{
    auto texture = s_immutable_bitmap_texture_cache.get(immutable_bitmap.id());
    VERIFY(texture.has_value());
    blit_scaled_texture(dst_rect, texture.value(), src_rect, scaling_mode);
}

static Gfx::FloatRect to_texture_space(Gfx::FloatRect rect, Gfx::IntSize image_size)
{
    auto x = rect.x() / image_size.width();
    auto y = rect.y() / image_size.height();
    auto width = rect.width() / image_size.width();
    auto height = rect.height() / image_size.height();

    return { x, y, width, height };
}

static GL::ScalingMode to_gl_scaling_mode(Painter::ScalingMode scaling_mode)
{
    switch (scaling_mode) {
    case Painter::ScalingMode::NearestNeighbor:
        return GL::ScalingMode::Nearest;
    case Painter::ScalingMode::Bilinear:
        return GL::ScalingMode::Linear;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Painter::draw_scaled_bitmap(Gfx::FloatRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::FloatRect const& src_rect, ScalingMode scaling_mode)
{
    // FIXME: We should reuse textures across repaints if possible.
    auto texture = GL::create_texture();
    GL::upload_texture_data(texture, bitmap);
    blit_scaled_texture(dst_rect, texture, src_rect, scaling_mode);
    GL::delete_texture(texture);
}

void Painter::prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs)
{
    HashMap<GlyphsTextureKey, NonnullRefPtr<Gfx::Bitmap>> glyph_bitmaps;
    for (auto const& [font, code_points] : unique_glyphs) {
        for (auto const& code_point : code_points) {
            auto glyph = font->glyph(code_point);
            if (glyph.bitmap()) {
                auto atlas_key = GlyphsTextureKey { font, code_point };
                glyph_bitmaps.set(atlas_key, *glyph.bitmap());
            }
        }
    }

    if (glyph_bitmaps.is_empty())
        return;

    Vector<GlyphsTextureKey> glyphs_sorted_by_height;
    glyphs_sorted_by_height.ensure_capacity(glyph_bitmaps.size());
    for (auto const& [atlas_key, bitmap] : glyph_bitmaps) {
        glyphs_sorted_by_height.append(atlas_key);
    }
    quick_sort(glyphs_sorted_by_height, [&](auto const& a, auto const& b) {
        auto const& bitmap_a = *glyph_bitmaps.get(a);
        auto const& bitmap_b = *glyph_bitmaps.get(b);
        return bitmap_a->height() > bitmap_b->height();
    });

    int current_x = 0;
    int current_y = 0;
    int row_height = 0;
    int texture_width = 512;
    int padding = 1;
    for (auto const& glyphs_texture_key : glyphs_sorted_by_height) {
        auto const& bitmap = *glyph_bitmaps.get(glyphs_texture_key);
        if (current_x + bitmap->width() > texture_width) {
            current_x = 0;
            current_y += row_height + padding;
            row_height = 0;
        }
        m_glyphs_texture_map.set(glyphs_texture_key, { current_x, current_y, bitmap->width(), bitmap->height() });
        current_x += bitmap->width() + padding;
        row_height = max(row_height, bitmap->height());
    }

    auto glyphs_texture_bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { texture_width, current_y + row_height }));
    auto glyphs_texure_painter = Gfx::Painter(*glyphs_texture_bitmap);
    for (auto const& [glyphs_texture_key, glyph_bitmap] : glyph_bitmaps) {
        auto rect = m_glyphs_texture_map.get(glyphs_texture_key).value();
        glyphs_texure_painter.blit({ rect.x(), rect.y() }, glyph_bitmap, glyph_bitmap->rect());
    }

    m_glyphs_texture_size = glyphs_texture_bitmap->size();

    GL::upload_texture_data(m_glyphs_texture, *glyphs_texture_bitmap);
}

void Painter::draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const& color)
{
    bind_target_canvas();

    Vector<GLfloat> vertices;
    vertices.ensure_capacity(glyph_run.size() * 24);

    for (auto& glyph_or_emoji : glyph_run) {
        if (glyph_or_emoji.has<Gfx::DrawGlyph>()) {
            auto& glyph = glyph_or_emoji.get<Gfx::DrawGlyph>();

            auto const* font = glyph.font;
            auto code_point = glyph.code_point;
            auto point = glyph.position;

            auto maybe_texture_rect = m_glyphs_texture_map.get(GlyphsTextureKey { font, code_point });
            if (!maybe_texture_rect.has_value()) {
                continue;
            }

            auto texture_rect = to_texture_space(maybe_texture_rect.value().to_type<float>(), m_glyphs_texture_size);

            auto glyph_position = point + Gfx::FloatPoint(font->glyph_left_bearing(code_point), 0);
            auto glyph_size = maybe_texture_rect->size().to_type<float>();
            auto glyph_rect = transform().map(Gfx::FloatRect { glyph_position, glyph_size });
            auto rect_in_clip_space = to_clip_space(glyph_rect);

            // p0 --- p1
            // | \     |
            // |   \   |
            // |     \ |
            // p2 --- p3

            auto p0 = rect_in_clip_space.top_left();
            auto p1 = rect_in_clip_space.top_right();
            auto p2 = rect_in_clip_space.bottom_left();
            auto p3 = rect_in_clip_space.bottom_right();

            auto s0 = texture_rect.top_left();
            auto s1 = texture_rect.top_right();
            auto s2 = texture_rect.bottom_left();
            auto s3 = texture_rect.bottom_right();

            auto add_triangle = [&](auto& p1, auto& p2, auto& p3, auto& s1, auto& s2, auto& s3) {
                vertices.unchecked_append(p1.x());
                vertices.unchecked_append(p1.y());
                vertices.unchecked_append(s1.x());
                vertices.unchecked_append(s1.y());

                vertices.unchecked_append(p2.x());
                vertices.unchecked_append(p2.y());
                vertices.unchecked_append(s2.x());
                vertices.unchecked_append(s2.y());

                vertices.unchecked_append(p3.x());
                vertices.unchecked_append(p3.y());
                vertices.unchecked_append(s3.x());
                vertices.unchecked_append(s3.y());
            };

            add_triangle(p0, p1, p3, s0, s1, s3);
            add_triangle(p0, p3, p2, s0, s3, s2);
        }
    }

    auto vbo = GL::create_buffer();
    GL::upload_to_buffer(vbo, vertices);

    auto vao = GL::create_vertex_array();
    GL::bind_vertex_array(vao);
    GL::bind_buffer(vbo);

    auto [red, green, blue, alpha] = gfx_color_to_opengl_color(color);

    m_blit_program.use();

    GL::bind_texture(m_glyphs_texture);
    GL::set_texture_scale_mode(GL::ScalingMode::Nearest);

    auto position_attribute = m_blit_program.get_attribute_location("aVertexPosition");
    auto color_uniform = m_blit_program.get_uniform_location("uColor");

    GL::set_uniform(color_uniform, red, green, blue, alpha);
    GL::set_vertex_attribute(position_attribute, 0, 4);
    GL::draw_arrays(GL::DrawPrimitive::Triangles, vertices.size() / 4);

    GL::delete_buffer(vbo);
    GL::delete_vertex_array(vao);
}

void Painter::fill_rect_with_linear_gradient(Gfx::IntRect const& rect, ReadonlySpan<Gfx::ColorStop> stops, float angle, Optional<float> repeat_length)
{
    fill_rect_with_linear_gradient(rect.to_type<float>(), stops, angle, repeat_length);
}

void Painter::fill_rect_with_linear_gradient(Gfx::FloatRect const& rect, ReadonlySpan<Gfx::ColorStop> stops, float angle, Optional<float> repeat_length)
{
    bind_target_canvas();

    // FIXME: Implement support for angle and repeat_length
    (void)angle;
    (void)repeat_length;

    Vector<GLfloat> vertices;
    Vector<GLfloat> colors;
    for (size_t stop_index = 0; stop_index < stops.size() - 1; stop_index++) {
        auto const& stop_start = stops[stop_index];
        auto const& stop_end = stops[stop_index + 1];

        // The gradient is divided into segments that represent linear gradients between adjacent pairs of stops.
        auto segment_rect_location = rect.location();
        segment_rect_location.set_x(segment_rect_location.x() + stop_start.position * rect.width());
        auto segment_rect_width = (stop_end.position - stop_start.position) * rect.width();
        auto segment_rect_height = rect.height();
        auto segment_rect = transform().map(Gfx::FloatRect { segment_rect_location.x(), segment_rect_location.y(), segment_rect_width, segment_rect_height });

        auto rect_in_clip_space = to_clip_space(segment_rect);

        // p0 --- p1
        // | \     |
        // |   \   |
        // |     \ |
        // p2 --- p3

        auto p0 = rect_in_clip_space.top_left();
        auto p1 = rect_in_clip_space.top_right();
        auto p2 = rect_in_clip_space.bottom_left();
        auto p3 = rect_in_clip_space.bottom_right();

        auto c0 = gfx_color_to_opengl_color(stop_start.color);
        auto c1 = gfx_color_to_opengl_color(stop_end.color);
        auto c2 = gfx_color_to_opengl_color(stop_start.color);
        auto c3 = gfx_color_to_opengl_color(stop_end.color);

        auto add_triangle = [&](auto& p1, auto& p2, auto& p3, auto& c1, auto& c2, auto& c3) {
            vertices.append(p1.x());
            vertices.append(p1.y());
            colors.append(c1.red);
            colors.append(c1.green);
            colors.append(c1.blue);
            colors.append(c1.alpha);

            vertices.append(p2.x());
            vertices.append(p2.y());
            colors.append(c2.red);
            colors.append(c2.green);
            colors.append(c2.blue);
            colors.append(c2.alpha);

            vertices.append(p3.x());
            vertices.append(p3.y());
            colors.append(c3.red);
            colors.append(c3.green);
            colors.append(c3.blue);
            colors.append(c3.alpha);
        };

        add_triangle(p0, p1, p3, c0, c1, c3);
        add_triangle(p0, p3, p2, c0, c3, c2);
    }

    auto vao = GL::create_vertex_array();
    GL::bind_vertex_array(vao);

    auto vbo_vertices = GL::create_buffer();
    GL::upload_to_buffer(vbo_vertices, vertices);

    auto vbo_colors = GL::create_buffer();
    GL::upload_to_buffer(vbo_colors, colors);

    m_linear_gradient_program.use();
    auto position_attribute = m_linear_gradient_program.get_attribute_location("aVertexPosition");
    auto color_attribute = m_linear_gradient_program.get_attribute_location("aColor");

    GL::bind_buffer(vbo_vertices);
    GL::set_vertex_attribute(position_attribute, 0, 2);

    GL::bind_buffer(vbo_colors);
    GL::set_vertex_attribute(color_attribute, 0, 4);

    GL::draw_arrays(GL::DrawPrimitive::Triangles, vertices.size() / 2);
}

void Painter::save()
{
    m_state_stack.append(state());
}

void Painter::restore()
{
    VERIFY(!m_state_stack.is_empty());
    m_state_stack.take_last();
}

void Painter::set_clip_rect(Gfx::IntRect rect)
{
    GL::enable_scissor_test(transform().map(rect));
}

void Painter::clear_clip_rect()
{
    GL::disable_scissor_test();
}

void Painter::bind_target_canvas()
{
    m_target_canvas->bind();
    GL::set_viewport({ 0, 0, m_target_canvas->size().width(), m_target_canvas->size().height() });
}

void Painter::set_target_canvas(NonnullRefPtr<Canvas> canvas)
{
    m_target_canvas = canvas;
    canvas->bind();
    GL::set_viewport({ 0, 0, canvas->size().width(), canvas->size().height() });
}

void Painter::flush(Gfx::Bitmap& bitmap)
{
    m_target_canvas->bind();
    GL::read_pixels({ 0, 0, bitmap.width(), bitmap.height() }, bitmap);
}

void Painter::blit_canvas(Gfx::IntRect const& dst_rect, Canvas const& canvas, float opacity)
{
    blit_canvas(dst_rect.to_type<float>(), canvas, opacity);
}

void Painter::blit_canvas(Gfx::FloatRect const& dst_rect, Canvas const& canvas, float opacity)
{
    auto texture = GL::Texture(canvas.framebuffer().texture);
    blit_scaled_texture(dst_rect, texture, { { 0, 0 }, canvas.size() }, Painter::ScalingMode::NearestNeighbor, opacity);
}

void Painter::blit_scaled_texture(Gfx::FloatRect const& dst_rect, GL::Texture const& texture, Gfx::FloatRect const& src_rect, ScalingMode scaling_mode, float opacity)
{
    bind_target_canvas();

    m_blit_program.use();

    auto dst_rect_in_clip_space = to_clip_space(transform().map(dst_rect));
    auto src_rect_in_texture_space = to_texture_space(src_rect, *texture.size);

    Vector<GLfloat> vertices;
    vertices.ensure_capacity(16);

    auto add_vertex = [&](auto const& p, auto const& s) {
        vertices.append(p.x());
        vertices.append(p.y());
        vertices.append(s.x());
        vertices.append(s.y());
    };

    add_vertex(dst_rect_in_clip_space.top_left(), src_rect_in_texture_space.top_left());
    add_vertex(dst_rect_in_clip_space.bottom_left(), src_rect_in_texture_space.bottom_left());
    add_vertex(dst_rect_in_clip_space.bottom_right(), src_rect_in_texture_space.bottom_right());
    add_vertex(dst_rect_in_clip_space.top_right(), src_rect_in_texture_space.top_right());

    auto vbo = GL::create_buffer();
    GL::upload_to_buffer(vbo, vertices);

    auto vao = GL::create_vertex_array();
    GL::bind_vertex_array(vao);
    GL::bind_buffer(vbo);

    auto vertex_position_attribute = m_blit_program.get_attribute_location("aVertexPosition");
    GL::set_vertex_attribute(vertex_position_attribute, 0, 4);

    auto color_uniform = m_blit_program.get_uniform_location("uColor");
    GL::set_uniform(color_uniform, 1, 1, 1, opacity);

    GL::bind_texture(texture);

    auto scaling_mode_gl = to_gl_scaling_mode(scaling_mode);
    GL::set_texture_scale_mode(scaling_mode_gl);

    GL::enable_blending();

    GL::draw_arrays(GL::DrawPrimitive::TriangleFan, 4);

    GL::delete_buffer(vbo);
    GL::delete_vertex_array(vao);
}

void Painter::update_immutable_bitmap_texture_cache(HashMap<u32, Gfx::ImmutableBitmap const*>& immutable_bitmaps)
{
    for (auto immutable_bitmap_id : s_immutable_bitmap_texture_cache.keys()) {
        if (!immutable_bitmaps.contains(immutable_bitmap_id)) {
            auto texture = s_immutable_bitmap_texture_cache.get(immutable_bitmap_id).value();
            GL::delete_texture(texture);
            s_immutable_bitmap_texture_cache.remove(immutable_bitmap_id);
        }
    }

    for (auto const& [id, immutable_bitmap] : immutable_bitmaps) {
        if (s_immutable_bitmap_texture_cache.contains(id))
            continue;
        auto texture = GL::create_texture();
        GL::upload_texture_data(texture, immutable_bitmap->bitmap());
        s_immutable_bitmap_texture_cache.set(id, texture);
    }
}

}
