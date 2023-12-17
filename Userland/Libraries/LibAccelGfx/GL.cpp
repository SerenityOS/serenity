/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define GL_GLEXT_PROTOTYPES

#include <LibAccelGfx/GL.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>

namespace AccelGfx::GL {

static void verify_no_error()
{
    VERIFY(glGetError() == GL_NO_ERROR);
}

void set_viewport(Gfx::IntRect rect)
{
    glViewport(rect.left(), rect.top(), rect.width(), rect.height());
    verify_no_error();
}

static GLenum to_gl_enum(BlendFactor factor)
{
    switch (factor) {
    case BlendFactor::SrcAlpha:
        return GL_SRC_ALPHA;
    case BlendFactor::One:
        return GL_ONE;
    case BlendFactor::Zero:
        return GL_ZERO;
    case BlendFactor::OneMinusSrcAlpha:
        return GL_ONE_MINUS_SRC_ALPHA;
    }
    VERIFY_NOT_REACHED();
}

void enable_blending(BlendFactor source, BlendFactor destination, BlendFactor source_alpha, BlendFactor destination_alpha)
{
    glEnable(GL_BLEND);
    glBlendFuncSeparate(to_gl_enum(source), to_gl_enum(destination), to_gl_enum(source_alpha), to_gl_enum(destination_alpha));
    verify_no_error();
}

void read_pixels(Gfx::IntRect rect, Gfx::Bitmap& bitmap)
{
    VERIFY(bitmap.format() == Gfx::BitmapFormat::BGRA8888);
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadPixels(rect.left(), rect.top(), rect.width(), rect.height(), GL_BGRA, GL_UNSIGNED_BYTE, bitmap.scanline(0));
    verify_no_error();
}

Shader create_shader(ShaderType type, char const* source)
{
    GLuint shader = glCreateShader(type == ShaderType::Vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    int success;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
    if (!success) {
        char buffer[512];
        glGetShaderInfoLog(shader, sizeof(buffer), nullptr, buffer);
        dbgln("GLSL shader compilation failed: {}", buffer);
        VERIFY_NOT_REACHED();
    }

    verify_no_error();

    return { shader };
}

Program create_program(Shader const& vertex_shader, Shader const& fragment_shader)
{
    GLuint program = glCreateProgram();

    glAttachShader(program, vertex_shader.id);
    glAttachShader(program, fragment_shader.id);
    glLinkProgram(program);

    int linked;
    glGetProgramiv(program, GL_LINK_STATUS, &linked);
    if (!linked) {
        char buffer[512];
        glGetProgramInfoLog(program, sizeof(buffer), nullptr, buffer);
        dbgln("GLSL program linking failed: {}", buffer);
        VERIFY_NOT_REACHED();
    }

    glDeleteShader(vertex_shader.id);
    glDeleteShader(fragment_shader.id);

    verify_no_error();

    return { program };
}

void use_program(Program const& program)
{
    glUseProgram(program.id);
    verify_no_error();
}

VertexAttribute get_attribute_location(Program const& program, char const* name)
{
    auto id = glGetAttribLocation(program.id, name);
    verify_no_error();
    return { id };
}

Uniform get_uniform_location(Program const& program, char const* name)
{
    auto id = glGetUniformLocation(program.id, name);
    verify_no_error();
    return { id };
}

void delete_program(Program const& program)
{
    glDeleteProgram(program.id);
    verify_no_error();
}

Texture create_texture()
{
    GLuint texture;
    glGenTextures(1, &texture);
    verify_no_error();
    return { texture, {} };
}

void bind_texture(Texture const& texture)
{
    glBindTexture(GL_TEXTURE_2D, texture.id);
    verify_no_error();
}

void upload_texture_data(Texture& texture, Gfx::Bitmap const& bitmap)
{
    VERIFY(bitmap.format() == Gfx::BitmapFormat::BGRx8888 || bitmap.format() == Gfx::BitmapFormat::BGRA8888);
    bind_texture(texture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap.width(), bitmap.height(), 0, GL_BGRA, GL_UNSIGNED_BYTE, bitmap.scanline(0));
    texture.size = bitmap.size();
    verify_no_error();
}

void delete_texture(Texture const& texture)
{
    glDeleteTextures(1, &texture.id);
    verify_no_error();
}

void set_uniform(Uniform const& uniform, int value)
{
    glUniform1i(uniform.id, value);
    verify_no_error();
}

void set_uniform(Uniform const& uniform, float value1, float value2)
{
    glUniform2f(uniform.id, value1, value2);
    verify_no_error();
}

void set_uniform(Uniform const& uniform, float value1, float value2, float value3, float value4)
{
    glUniform4f(uniform.id, value1, value2, value3, value4);
    verify_no_error();
}

void set_vertex_attribute(VertexAttribute const& attribute, u32 offset, int number_of_components)
{
    glVertexAttribPointer(attribute.id, number_of_components, GL_FLOAT, GL_FALSE, number_of_components * sizeof(float), reinterpret_cast<void*>(offset));
    glEnableVertexAttribArray(attribute.id);
    verify_no_error();
}

void set_texture_scale_mode(ScalingMode scaling_mode)
{
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, scaling_mode == ScalingMode::Nearest ? GL_NEAREST : GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, scaling_mode == ScalingMode::Nearest ? GL_NEAREST : GL_LINEAR);
    verify_no_error();
}

void clear_color(Gfx::Color const& color)
{
    glClearColor(color.red() / 255.0f, color.green() / 255.0f, color.blue() / 255.0f, color.alpha() / 255.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    verify_no_error();
}

void draw_arrays(DrawPrimitive draw_primitive, size_t count)
{
    GLenum mode = GL_TRIANGLES;
    if (draw_primitive == DrawPrimitive::TriangleFan)
        mode = GL_TRIANGLE_FAN;
    glDrawArrays(mode, 0, count);
    verify_no_error();
}

Buffer create_buffer()
{
    GLuint buffer;
    glGenBuffers(1, &buffer);
    verify_no_error();
    return { buffer };
}

void bind_buffer(Buffer const& buffer)
{
    glBindBuffer(GL_ARRAY_BUFFER, buffer.id);
    verify_no_error();
}

void upload_to_buffer(Buffer const& buffer, Span<float> values)
{
    glBindBuffer(GL_ARRAY_BUFFER, buffer.id);
    glBufferData(GL_ARRAY_BUFFER, values.size() * sizeof(float), values.data(), GL_STATIC_DRAW);
    verify_no_error();
}

void delete_buffer(Buffer const& buffer)
{
    glDeleteBuffers(1, &buffer.id);
    verify_no_error();
}

VertexArray create_vertex_array()
{
    GLuint vertex_array;
    glGenVertexArrays(1, &vertex_array);
    verify_no_error();
    return { vertex_array };
}

void bind_vertex_array(VertexArray const& vertex_array)
{
    glBindVertexArray(vertex_array.id);
    verify_no_error();
}

void delete_vertex_array(VertexArray const& vertex_array)
{
    glDeleteVertexArrays(1, &vertex_array.id);
    verify_no_error();
}

Framebuffer create_framebuffer(Gfx::IntSize size)
{
    GLuint texture;
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size.width(), size.height(), 0, GL_BGRA, GL_UNSIGNED_BYTE, NULL);

    GLuint fbo;
    glGenFramebuffers(1, &fbo);

    glBindFramebuffer(GL_FRAMEBUFFER, fbo);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
        VERIFY_NOT_REACHED();
    }

    verify_no_error();

    return { fbo, Texture { texture, size } };
}

void bind_framebuffer(Framebuffer const& framebuffer)
{
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer.fbo_id);
    verify_no_error();
}

void delete_framebuffer(Framebuffer const& framebuffer)
{
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer.fbo_id);
    glDeleteFramebuffers(1, &framebuffer.fbo_id);
    delete_texture(framebuffer.texture);
    verify_no_error();
}

void enable_scissor_test(Gfx::IntRect rect)
{
    glEnable(GL_SCISSOR_TEST);
    glScissor(rect.left(), rect.top(), rect.width(), rect.height());
    verify_no_error();
}

void disable_scissor_test()
{
    glDisable(GL_SCISSOR_TEST);
    verify_no_error();
}

}
