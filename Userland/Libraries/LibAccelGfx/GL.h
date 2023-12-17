/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#ifdef AK_OS_MACOS
#    define GL_SILENCE_DEPRECATION
#    include <OpenGL/OpenGL.h>
#    include <OpenGL/gl3.h>
#else
#    include <GL/gl.h>
#endif
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>

namespace AccelGfx::GL {

enum class ShaderType {
    Vertex,
    Fragment,
};

struct Shader {
    GLuint id;
};

struct Program {
    GLuint id;
};

struct VertexAttribute {
    GLint id;
};

struct Uniform {
    GLint id;
};

struct Texture {
    GLuint id;
    Optional<Gfx::IntSize> size;
};

struct Buffer {
    GLuint id;
};

struct VertexArray {
    GLuint id;
};

struct Framebuffer {
    GLuint fbo_id;
    GL::Texture texture;
};

void set_viewport(Gfx::IntRect);

enum class BlendFactor {
    Zero,
    One,
    OneMinusSrcAlpha,
    SrcAlpha,
};
void enable_blending(BlendFactor source, BlendFactor destination, BlendFactor source_alpha, BlendFactor destination_alpha);

void read_pixels(Gfx::IntRect, Gfx::Bitmap&);

Shader create_shader(ShaderType type, char const* source);
Program create_program(Shader const& vertex_shader, Shader const& fragment_shader);
void use_program(Program const&);
VertexAttribute get_attribute_location(Program const&, char const* name);
Uniform get_uniform_location(Program const&, char const* name);
void delete_program(Program const&);

Texture create_texture();
void bind_texture(Texture const&);
void upload_texture_data(Texture& texture, Gfx::Bitmap const& bitmap);
void delete_texture(Texture const&);

void set_uniform(Uniform const& uniform, int);
void set_uniform(Uniform const& uniform, float, float);
void set_uniform(Uniform const& uniform, float, float, float, float);
void set_vertex_attribute(VertexAttribute const& attribute, u32 offset, int number_of_components);

enum class ScalingMode {
    Nearest,
    Linear,
};
void set_texture_scale_mode(ScalingMode);

void clear_color(Gfx::Color const&);

enum class DrawPrimitive {
    Triangles,
    TriangleFan,
};

void draw_arrays(DrawPrimitive, size_t count);

Buffer create_buffer();
void bind_buffer(Buffer const&);
void upload_to_buffer(Buffer const&, Span<float> values);
void delete_buffer(Buffer const&);

VertexArray create_vertex_array();
void bind_vertex_array(VertexArray const&);
void delete_vertex_array(VertexArray const&);

Framebuffer create_framebuffer(Gfx::IntSize);
void bind_framebuffer(Framebuffer const& framebuffer);
void delete_framebuffer(Framebuffer const& framebuffer);

void enable_scissor_test(Gfx::IntRect);
void disable_scissor_test();

}
