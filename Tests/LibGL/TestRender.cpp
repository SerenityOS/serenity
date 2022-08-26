/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/FileStream.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/QOIWriter.h>
#include <LibTest/TestCase.h>

#ifdef __serenity__
#    define REFERENCE_IMAGE_DIR "/usr/Tests/LibGL/reference-images"
#else
#    define REFERENCE_IMAGE_DIR "reference-images"
#endif
#define SAVE_OUTPUT false

static NonnullOwnPtr<GL::GLContext> create_testing_context(int width, int height)
{
    auto bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, { width, height }));
    auto context = GL::create_context(*bitmap);
    GL::make_context_current(context);

    // Assume some defaults for our testing contexts
    glFrontFace(GL_CCW);
    glCullFace(GL_BACK);
    glEnable(GL_CULL_FACE);

    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    return context;
}

static void expect_bitmap_equals_reference(Gfx::Bitmap const& bitmap, StringView test_name)
{
    auto reference_filename = String::formatted("{}.qoi", test_name);

    if constexpr (SAVE_OUTPUT) {
        auto target_path = LexicalPath("/home/anon").append(reference_filename);
        auto qoi_buffer = Gfx::QOIWriter::encode(bitmap);
        auto qoi_output_stream = MUST(Core::OutputFileStream::open(target_path.string()));
        auto number_of_bytes_written = qoi_output_stream.write(qoi_buffer);
        qoi_output_stream.close();
        EXPECT_EQ(number_of_bytes_written, qoi_buffer.size());
    }

    auto reference_image_path = String::formatted(REFERENCE_IMAGE_DIR "/{}", reference_filename);
    auto reference_bitmap = MUST(Gfx::Bitmap::try_load_from_file(reference_image_path));
    EXPECT_EQ(reference_bitmap->visually_equals(bitmap), true);
}

TEST_CASE(0001_simple_triangle)
{
    auto context = create_testing_context(64, 64);

    glBegin(GL_TRIANGLES);
    glColor3f(1, 1, 1);
    glVertex2f(0, 1);
    glVertex2f(-1, -1);
    glVertex2f(1, -1);
    glEnd();

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0001_simple_triangle"sv);
}

TEST_CASE(0002_quad_color_interpolation)
{
    auto context = create_testing_context(64, 64);

    glBegin(GL_QUADS);

    glColor3f(1, 0, 0);
    glVertex2i(-1, -1);
    glColor3f(0, 1, 0);
    glVertex2i(1, -1);
    glColor3f(0, 0, 1);
    glVertex2i(1, 1);
    glColor3f(1, 0, 1);
    glVertex2i(-1, 1);
    glEnd();

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0002_quad_color_interpolation"sv);
}

TEST_CASE(0003_rect_w_coordinate_regression)
{
    auto context = create_testing_context(64, 64);

    glEnable(GL_DEPTH_TEST);
    glClear(GL_DEPTH_BUFFER_BIT);

    glColor3f(0, 1, 0);
    glRectf(-0.5f, -0.5f, 0.5f, 0.5f);

    glBegin(GL_TRIANGLES);
    glColor3f(1, 0, 0);
    glVertex2i(-1, -1);
    glVertex2i(1, -1);
    glVertex2i(-1, 1);
    glEnd();

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0003_rect_w_coordinate_regression"sv);
}

TEST_CASE(0004_points)
{
    auto context = create_testing_context(64, 64);

    // Aliased points
    for (size_t i = 0; i < 3; ++i) {
        glPointSize(1.f + i);
        glBegin(GL_POINTS);
        glVertex2f(-.5f + i * .5f, .5f);
        glEnd();
    }

    // Anti-aliased points
    glEnable(GL_POINT_SMOOTH);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    for (size_t i = 0; i < 3; ++i) {
        glPointSize(3.f - i);
        glBegin(GL_POINTS);
        glVertex2f(-.5f + i * .5f, -.5f);
        glEnd();
    }

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0004_points"sv);
}

TEST_CASE(0005_lines_antialiased)
{
    auto context = create_testing_context(64, 64);

    // Draw anti-aliased lines
    glEnable(GL_LINE_SMOOTH);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glBegin(GL_LINES);
    for (size_t i = 0; i < 6; ++i) {
        glVertex2f(-.9f, .25f - i * .1f);
        glVertex2f(.9f, .9f - i * .36f);
    }
    glEnd();

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0005_lines"sv);
}

TEST_CASE(0006_test_rgb565_texture)
{
    auto context = create_testing_context(64, 64);

    GLuint texture_id;
    glGenTextures(1, &texture_id);
    glBindTexture(GL_TEXTURE_2D, texture_id);
    u16 texture_data[] = { 0xF800, 0xC000, 0x8000, 0x07E0, 0x0600, 0x0400, 0x001F, 0x0018, 0x0010 };
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 3, 3, 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, texture_data);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

    glEnable(GL_TEXTURE_2D);
    glBegin(GL_QUADS);
    glTexCoord2i(0, 0);
    glVertex2i(-1, 1);
    glTexCoord2i(0, 1);
    glVertex2i(-1, -1);
    glTexCoord2i(1, 1);
    glVertex2i(1, -1);
    glTexCoord2i(1, 0);
    glVertex2i(1, 1);
    glEnd();

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0006_test_rgb565_texture"sv);
}

TEST_CASE(0007_test_rgba_to_rgb_texture)
{
    auto context = create_testing_context(64, 64);

    GLuint texture_id;
    glGenTextures(1, &texture_id);
    glBindTexture(GL_TEXTURE_2D, texture_id);

    // Write RGBA data with A = 0 to an RGB texture
    u32 texture_data[] = { 0x00FF0000 };
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 1, 1, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8, texture_data);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

    glEnable(GL_TEXTURE_2D);
    glBegin(GL_TRIANGLES);
    glTexCoord2i(0, 0);
    glVertex2i(-1, 1);
    glTexCoord2i(0, 1);
    glVertex2i(-1, -1);
    glTexCoord2i(1, 1);
    glVertex2i(1, -1);
    glEnd();

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0007_test_rgba_to_rgb_texture"sv);
}
