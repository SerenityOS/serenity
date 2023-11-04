/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define GL_GLEXT_PROTOTYPES

#include "Painter.h"
#include "Canvas.h"
#include <AK/QuickSort.h>
#include <GL/gl.h>
#include <GL/glext.h>
#include <LibGfx/Color.h>
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
    float x = 2.0f * screen_rect.x() / m_canvas->width() - 1.0f;
    float y = -1.0f + 2.0f * screen_rect.y() / m_canvas->height();

    float width = 2.0f * screen_rect.width() / m_canvas->width();
    float height = 2.0f * screen_rect.height() / m_canvas->height();

    return { x, y, width, height };
}

char const* vertex_shader_source = R"(
attribute vec2 aVertexPosition;
void main() {
    gl_Position = vec4(aVertexPosition, 0.0, 1.0);
}
)";

char const* solid_color_fragment_shader_source = R"(
precision mediump float;
uniform vec4 uColor;
void main() {
    gl_FragColor = uColor;
}
)";

char const* blit_vertex_shader_source = R"(
attribute vec4 aVertexPosition;
varying vec2 vTextureCoord;
void main() {
    gl_Position = vec4(aVertexPosition.xy, 0.0, 1.0);
    vTextureCoord = aVertexPosition.zw;
}
)";

char const* blit_fragment_shader_source = R"(
precision mediump float;
uniform vec4 uColor;
varying vec2 vTextureCoord;
uniform sampler2D uSampler;
void main() {
    gl_FragColor = texture2D(uSampler, vTextureCoord) * uColor;
}
)";

OwnPtr<Painter> Painter::create()
{
    auto& context = Context::the();
    return make<Painter>(context);
}

Painter::Painter(Context& context)
    : m_context(context)
    , m_rectangle_program(Program::create(vertex_shader_source, solid_color_fragment_shader_source))
    , m_blit_program(Program::create(blit_vertex_shader_source, blit_fragment_shader_source))
{
    m_state_stack.empend(State());

    glGenTextures(1, &m_glyphs_texture);
}

Painter::~Painter()
{
    flush();
}

void Painter::clear(Gfx::Color color)
{
    auto [red, green, blue, alpha] = gfx_color_to_opengl_color(color);
    glClearColor(red, green, blue, alpha);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
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
    // Draw a filled rect (with `color`) using OpenGL after mapping it through the current transform.

    auto vertices = rect_to_vertices(to_clip_space(transform().map(rect)));

    auto [red, green, blue, alpha] = gfx_color_to_opengl_color(color);

    m_rectangle_program.use();

    GLuint position_attribute = m_rectangle_program.get_attribute_location("aVertexPosition");
    GLuint color_uniform = m_rectangle_program.get_uniform_location("uColor");

    glUniform4f(color_uniform, red, green, blue, alpha);

    glVertexAttribPointer(position_attribute, 2, GL_FLOAT, GL_FALSE, 2 * sizeof(float), vertices.data());
    glEnableVertexAttribArray(position_attribute);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
}

void Painter::draw_scaled_bitmap(Gfx::IntRect const& dest_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, ScalingMode scaling_mode)
{
    draw_scaled_bitmap(dest_rect.to_type<float>(), bitmap, src_rect.to_type<float>(), scaling_mode);
}

static Gfx::FloatRect to_texture_space(Gfx::FloatRect rect, Gfx::IntSize image_size)
{
    auto x = rect.x() / image_size.width();
    auto y = rect.y() / image_size.height();
    auto width = rect.width() / image_size.width();
    auto height = rect.height() / image_size.height();

    return { x, y, width, height };
}

static GLenum to_gl_scaling_mode(Painter::ScalingMode scaling_mode)
{
    switch (scaling_mode) {
    case Painter::ScalingMode::NearestNeighbor:
        return GL_NEAREST;
    case Painter::ScalingMode::Bilinear:
        return GL_LINEAR;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Painter::draw_scaled_bitmap(Gfx::FloatRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::FloatRect const& src_rect, ScalingMode scaling_mode)
{
    m_blit_program.use();

    GLuint texture;

    // FIXME: We should reuse textures across repaints if possible.
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, bitmap.width(), bitmap.height(), 0, GL_BGRA, GL_UNSIGNED_BYTE, bitmap.scanline(0));

    GLenum scaling_mode_gl = to_gl_scaling_mode(scaling_mode);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, scaling_mode_gl);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, scaling_mode_gl);

    auto dst_rect_in_clip_space = to_clip_space(transform().map(dst_rect));
    auto src_rect_in_texture_space = to_texture_space(src_rect, bitmap.size());

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

    GLuint vertex_position_attribute = m_blit_program.get_attribute_location("aVertexPosition");
    glVertexAttribPointer(vertex_position_attribute, 4, GL_FLOAT, GL_FALSE, 4 * sizeof(float), vertices.data());
    glEnableVertexAttribArray(vertex_position_attribute);

    GLuint color_uniform = m_blit_program.get_uniform_location("uColor");
    glUniform4f(color_uniform, 1, 1, 1, 1);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

    glDeleteTextures(1, &texture);
}

void Painter::prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs)
{
    HashMap<GlyphsTextureKey, NonnullRefPtr<Gfx::Bitmap>> glyph_bitmaps;
    for (auto const& [font, code_points] : unique_glyphs) {
        for (auto const& code_point : code_points) {
            auto glyph = font->glyph(code_point);
            auto atlas_key = GlyphsTextureKey { font, code_point };
            glyph_bitmaps.set(atlas_key, *glyph.bitmap());
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
    for (auto const& glyphs_texture_key : glyphs_sorted_by_height) {
        auto const& bitmap = *glyph_bitmaps.get(glyphs_texture_key);
        if (current_x + bitmap->width() > texture_width) {
            current_x = 0;
            current_y += row_height;
            row_height = 0;
        }
        m_glyphs_texture_map.set(glyphs_texture_key, { current_x, current_y, bitmap->width(), bitmap->height() });
        current_x += bitmap->width();
        row_height = max(row_height, bitmap->height());
    }

    auto glyphs_texture_bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { texture_width, current_y + row_height }));
    auto glyphs_texure_painter = Gfx::Painter(*glyphs_texture_bitmap);
    for (auto const& [glyphs_texture_key, glyph_bitmap] : glyph_bitmaps) {
        auto rect = m_glyphs_texture_map.get(glyphs_texture_key).value();
        glyphs_texure_painter.blit({ rect.x(), rect.y() }, glyph_bitmap, glyph_bitmap->rect());
    }

    m_glyphs_texture_size = glyphs_texture_bitmap->size();

    glBindTexture(GL_TEXTURE_2D, m_glyphs_texture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, glyphs_texture_bitmap->width(), glyphs_texture_bitmap->height(), 0, GL_BGRA, GL_UNSIGNED_BYTE, glyphs_texture_bitmap->scanline(0));
}

void Painter::draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const& color)
{
    Vector<GLfloat> vertices;

    for (auto& glyph_or_emoji : glyph_run) {
        if (glyph_or_emoji.has<Gfx::DrawGlyph>()) {
            auto& glyph = glyph_or_emoji.get<Gfx::DrawGlyph>();

            auto const* font = glyph.font;
            auto code_point = glyph.code_point;
            auto point = glyph.position;

            auto maybe_texture_rect = m_glyphs_texture_map.get(GlyphsTextureKey { font, code_point });
            VERIFY(maybe_texture_rect.has_value());

            auto texture_rect = to_texture_space(maybe_texture_rect.value().to_type<float>(), m_glyphs_texture_size);

            auto glyph_position = point + Gfx::FloatPoint(font->glyph_left_bearing(code_point), 0);
            auto glyph_size = maybe_texture_rect->size().to_type<float>();
            auto rect_in_clip_space = to_clip_space({ glyph_position, glyph_size });

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
                vertices.append(p1.x());
                vertices.append(p1.y());
                vertices.append(s1.x());
                vertices.append(s1.y());
                vertices.append(p2.x());
                vertices.append(p2.y());
                vertices.append(s2.x());
                vertices.append(s2.y());
                vertices.append(p3.x());
                vertices.append(p3.y());
                vertices.append(s3.x());
                vertices.append(s3.y());
            };

            add_triangle(p0, p1, p3, s0, s1, s3);
            add_triangle(p0, p3, p2, s0, s3, s2);
        }
    }

    auto [red, green, blue, alpha] = gfx_color_to_opengl_color(color);

    m_blit_program.use();

    glBindTexture(GL_TEXTURE_2D, m_glyphs_texture);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

    GLuint position_attribute = m_blit_program.get_attribute_location("aVertexPosition");
    GLuint color_uniform = m_blit_program.get_uniform_location("uColor");

    glUniform4f(color_uniform, red, green, blue, alpha);

    glVertexAttribPointer(position_attribute, 4, GL_FLOAT, GL_FALSE, 4 * sizeof(float), vertices.data());
    glEnableVertexAttribArray(position_attribute);

    glDrawArrays(GL_TRIANGLES, 0, vertices.size() / 4);
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

void Painter::flush()
{
    m_canvas->flush();
}

}
