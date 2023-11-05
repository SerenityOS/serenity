/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define GL_GLEXT_PROTOTYPES

#include "Painter.h"
#include "Canvas.h"
#include <GL/gl.h>
#include <GL/glext.h>
#include <LibGfx/Color.h>

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
attribute vec2 aTextureCoord;
varying vec2 vTextureCoord;
void main() {
    gl_Position = aVertexPosition;
    vTextureCoord = aTextureCoord;
}
)";

char const* blit_fragment_shader_source = R"(
precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uSampler;
void main() {
    gl_FragColor = texture2D(uSampler, vTextureCoord);
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
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap.width(), bitmap.height(), 0, GL_BGRA, GL_UNSIGNED_BYTE, bitmap.scanline(0));

    GLenum scaling_mode_gl = to_gl_scaling_mode(scaling_mode);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, scaling_mode_gl);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, scaling_mode_gl);

    auto vertices = rect_to_vertices(to_clip_space(transform().map(dst_rect)));
    auto texture_coordinates = rect_to_vertices(to_texture_space(src_rect, bitmap.size()));

    GLuint vertex_position_attribute = m_blit_program.get_attribute_location("aVertexPosition");
    glVertexAttribPointer(vertex_position_attribute, 2, GL_FLOAT, GL_FALSE, 2 * sizeof(float), vertices.data());
    glEnableVertexAttribArray(vertex_position_attribute);

    GLuint texture_coord_attribute = m_blit_program.get_attribute_location("aTextureCoord");
    glVertexAttribPointer(texture_coord_attribute, 2, GL_FLOAT, GL_FALSE, 2 * sizeof(float), texture_coordinates.data());
    glEnableVertexAttribArray(texture_coord_attribute);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

    glDeleteTextures(1, &texture);
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
