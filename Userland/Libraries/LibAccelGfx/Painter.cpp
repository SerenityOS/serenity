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

OwnPtr<Painter> Painter::create()
{
    auto& context = Context::the();
    return make<Painter>(context);
}

Painter::Painter(Context& context)
    : m_context(context)
    , m_rectangle_program(Program::create(vertex_shader_source, solid_color_fragment_shader_source))
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
