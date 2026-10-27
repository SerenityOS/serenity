/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibTest/TestCase.h>

static NonnullOwnPtr<GL::GLContext> create_testing_context()
{
    auto bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 1, 1 }));
    auto context = MUST(GL::create_context(*bitmap));
    GL::make_context_current(context);
    return context;
}

TEST_CASE(0001_gl_gen_textures_does_not_return_the_same_texture_name_twice_unless_deleted)
{
    // https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glGenTextures.xhtml
    // "Texture names returned by a call to glGenTextures are not returned by subsequent calls, unless they are first deleted with glDeleteTextures."
    auto context = create_testing_context();

    GLuint texture1 = 0;
    GLuint texture2 = 0;

    glGenTextures(1, &texture1);

    // glDeleteTextures previously did not check that the texture name was allocated by glGenTextures before adding it to the free texture name list.
    // This means that if you delete a texture twice in a row, the name will appear twice in the free texture list, making glGenTextures return the
    // same texture name twice in a row.
    glDeleteTextures(1, &texture1);
    glDeleteTextures(1, &texture1);

    texture1 = 0;

    glGenTextures(1, &texture1);
    glGenTextures(1, &texture2);

    EXPECT_NE(texture1, texture2);
}

TEST_CASE(0002_gl_cull_face_does_not_accept_left_and_right)
{
    auto context = create_testing_context();

    // glCullFace only accepts GL_FRONT, GL_BACK and GL_FRONT_AND_BACK. We checked if the mode was valid by performing cull_mode < GL_FRONT || cull_mode > GL_FRONT_AND_BACK.
    // However, this range also contains GL_LEFT and GL_RIGHT, which we would accept when we should return a GL_INVALID_ENUM error.
    glCullFace(GL_LEFT);
    EXPECT_EQ(glGetError(), static_cast<GLenum>(GL_INVALID_ENUM));

    glCullFace(GL_RIGHT);
    EXPECT_EQ(glGetError(), static_cast<GLenum>(GL_INVALID_ENUM));
}

TEST_CASE(0003_gl_bind_buffer_names_must_be_allocated)
{
    auto context = create_testing_context();

    glBindBuffer(GL_ARRAY_BUFFER, 123);
    EXPECT_EQ(glGetError(), static_cast<GLenum>(GL_INVALID_VALUE));
}

TEST_CASE(0004_gl_color_clear_value)
{
    auto context = create_testing_context();

    Array<GLdouble, 4> clear_color;
    glGetDoublev(GL_COLOR_CLEAR_VALUE, clear_color.data());
    EXPECT_EQ(clear_color[0], 0.);
    EXPECT_EQ(clear_color[1], 0.);
    EXPECT_EQ(clear_color[2], 0.);
    EXPECT_EQ(clear_color[3], 0.);

    glClearColor(.1f, .2f, .3f, .4f);

    glGetDoublev(GL_COLOR_CLEAR_VALUE, clear_color.data());
    EXPECT_APPROXIMATE(clear_color[0], .1);
    EXPECT_APPROXIMATE(clear_color[1], .2);
    EXPECT_APPROXIMATE(clear_color[2], .3);
    EXPECT_APPROXIMATE(clear_color[3], .4);
}

TEST_CASE(0005_gl_depth_clear_value)
{
    auto context = create_testing_context();

    GLdouble clear_depth;
    glGetDoublev(GL_DEPTH_CLEAR_VALUE, &clear_depth);
    EXPECT_EQ(clear_depth, 1.);

    glClearDepth(.1f);

    glGetDoublev(GL_DEPTH_CLEAR_VALUE, &clear_depth);
    EXPECT_APPROXIMATE(clear_depth, .1);
}

TEST_CASE(0006_gl_stencil_clear_value)
{
    auto context = create_testing_context();

    GLint clear_stencil;
    glGetIntegerv(GL_STENCIL_CLEAR_VALUE, &clear_stencil);
    EXPECT_EQ(clear_stencil, 0);

    glClearStencil(255);

    glGetIntegerv(GL_STENCIL_CLEAR_VALUE, &clear_stencil);
    EXPECT_EQ(clear_stencil, 255);
}

TEST_CASE(0008_default_texture_size)
{
    auto context = create_testing_context();

    GLint width = -1;
    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, &width);
    EXPECT_EQ(glGetError(), 0u);
    EXPECT_EQ(width, 0);

    GLint height = -1;
    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, &height);
    EXPECT_EQ(glGetError(), 0u);
    EXPECT_EQ(height, 0);
}

TEST_CASE(0007_proxy_texture_2d)
{
    auto context = create_testing_context();

    GLint max_texture_size = -1;
    glGetIntegerv(GL_MAX_TEXTURE_SIZE, &max_texture_size);
    EXPECT_EQ(glGetError(), 0u);
    EXPECT_NE(max_texture_size, -1);

    GLint wanted_size = 4;

    VERIFY(wanted_size <= max_texture_size);

    glTexImage2D(GL_PROXY_TEXTURE_2D, 0, GL_RGBA, wanted_size, wanted_size, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    EXPECT_EQ(glGetError(), 0u);

    GLint width = -1;
    glGetTexLevelParameteriv(GL_PROXY_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, &width);
    EXPECT_EQ(glGetError(), 0u);
    EXPECT_EQ(width, wanted_size);

    GLint height = -1;
    glGetTexLevelParameteriv(GL_PROXY_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, &height);
    EXPECT_EQ(glGetError(), 0u);
    EXPECT_EQ(height, wanted_size);

    glActiveTexture(GL_TEXTURE1);

    height = -1;
    glGetTexLevelParameteriv(GL_PROXY_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, &height);
    EXPECT_EQ(glGetError(), 0u);
    EXPECT_EQ(height, wanted_size);

    // This part of the test only works if there are invalid texture sizes that can be represented by an int.
    VERIFY(max_texture_size < NumericLimits<GLint>::max());

    wanted_size = NumericLimits<GLint>::max();

    glTexImage2D(GL_PROXY_TEXTURE_2D, 0, GL_RGBA, wanted_size, wanted_size, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    EXPECT_EQ(glGetError(), 0u);

    width = -1;
    glGetTexLevelParameteriv(GL_PROXY_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, &width);
    EXPECT_EQ(glGetError(), 0u);
    EXPECT_EQ(width, 0);

    height = -1;
    glGetTexLevelParameteriv(GL_PROXY_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, &height);
    EXPECT_EQ(glGetError(), 0u);
    EXPECT_EQ(height, 0);
}
