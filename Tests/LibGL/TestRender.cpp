/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 * Copyright (c) 2022-2024, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <LibCore/File.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/QOIWriter.h>
#include <LibTest/TestCase.h>

#ifdef AK_OS_SERENITY
#    define REFERENCE_IMAGE_DIR "/usr/Tests/LibGL/reference-images"
#else
#    define REFERENCE_IMAGE_DIR "reference-images"
#endif
#define SAVE_OUTPUT false

static NonnullOwnPtr<GL::GLContext> create_testing_context(int width, int height, Gfx::BitmapFormat format = Gfx::BitmapFormat::BGRx8888)
{
    auto bitmap = MUST(Gfx::Bitmap::create(format, { width, height }));
    auto context = MUST(GL::create_context(*bitmap));
    GL::make_context_current(context);
    return context;
}

static void expect_bitmap_equals_reference(Gfx::Bitmap const& bitmap, StringView test_name)
{
    auto reference_filename = ByteString::formatted("{}.qoi", test_name);

    if constexpr (SAVE_OUTPUT) {
        auto target_path = LexicalPath("/home/anon").append(reference_filename);
        auto qoi_buffer = MUST(Gfx::QOIWriter::encode(bitmap));
        auto qoi_output_stream = MUST(Core::File::open(target_path.string(), Core::File::OpenMode::Write));
        MUST(qoi_output_stream->write_until_depleted(qoi_buffer));
    }

    auto reference_image_path = ByteString::formatted(REFERENCE_IMAGE_DIR "/{}", reference_filename);
    auto reference_bitmap = MUST(Gfx::Bitmap::load_from_file(reference_image_path));
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

TEST_CASE(0008_test_pop_matrix_regression)
{
    auto context = create_testing_context(64, 64);

    // Load identity matrix after popping
    glMatrixMode(GL_MODELVIEW);
    glTranslatef(10.f, 10.f, 10.f);
    glPushMatrix();
    glPopMatrix();
    glLoadIdentity();

    glBegin(GL_TRIANGLES);
    glColor3f(0.f, 1.f, 0.f);
    glVertex2f(.5f, -.5f);
    glVertex2f(.0f, .5f);
    glVertex2f(-.5f, -.5f);
    glEnd();

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0008_test_pop_matrix_regression"sv);
}

TEST_CASE(0009_test_draw_elements_in_display_list)
{
    auto context = create_testing_context(64, 64);

    glColor3f(0.f, 0.f, 1.f);
    glEnableClientState(GL_VERTEX_ARRAY);

    auto const list_index = glGenLists(1);
    glNewList(list_index, GL_COMPILE);
    float vertices[] = { 0.f, .5f, -.5f, -.5f, .5f, -.5f };
    glVertexPointer(2, GL_FLOAT, 0, &vertices);
    u8 indices[] = { 0, 1, 2 };
    glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_BYTE, &indices);
    glEndList();

    // Modifying an index here should not have an effect
    indices[0] = 2;

    glCallList(list_index);

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0009_test_draw_elements_in_display_list"sv);
}

TEST_CASE(0010_test_store_data_in_buffer)
{
    auto context = create_testing_context(64, 64);

    glColor3f(1.f, 0.f, 0.f);
    glEnableClientState(GL_VERTEX_ARRAY);

    float vertices[] = { 0.f, .5f, -.5f, -.5f, .5f, -.5f };
    u8 indices[] = { 0, 1, 2 };

    GLuint buffers[2];
    glGenBuffers(2, buffers);

    glBindBuffer(GL_ARRAY_BUFFER, buffers[0]);
    glBufferData(GL_ARRAY_BUFFER, 6 * sizeof(float), vertices, GL_STATIC_DRAW);

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[1]);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, 3, indices, GL_STATIC_DRAW);

    glVertexPointer(2, GL_FLOAT, 0, 0);
    glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_BYTE, 0);

    glDeleteBuffers(2, buffers);

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0010_test_store_data_in_buffer"sv);
}

TEST_CASE(0011_tex_env_combine_with_constant_color)
{
    auto context = create_testing_context(64, 64);

    glEnable(GL_TEXTURE_2D);
    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
    glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_RGB, GL_CONSTANT);
    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_REPLACE);

    float color[4] = { .3f, .5f, .7f, 1.f };
    glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, color);

    glRecti(-1, -1, 1, 1);

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0011_tex_env_combine_with_constant_color"sv);
}

TEST_CASE(0012_blend_equations)
{
    auto context = create_testing_context(64, 64, Gfx::BitmapFormat::BGRA8888);

    // Assert initial state
    int actual_mode;
    glGetIntegerv(GL_BLEND_EQUATION_RGB, &actual_mode);
    EXPECT_EQ(actual_mode, GL_FUNC_ADD);
    glGetIntegerv(GL_BLEND_EQUATION_ALPHA, &actual_mode);
    EXPECT_EQ(actual_mode, GL_FUNC_ADD);

    // Clear with alpha 0 so we get a transparent color buffer
    glClearColor(0, 0, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT);

    glColor4f(.8f, .2f, .3f, .7f);
    glRecti(-1, -1, 1, 1);

    glColor4f(.3f, .1f, .8f, .5f);
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE);

    static constexpr Array<GLenum, 5> blend_modes = {
        GL_FUNC_ADD,
        GL_FUNC_SUBTRACT,
        GL_FUNC_REVERSE_SUBTRACT,
        GL_MIN,
        GL_MAX,
    };
    auto constexpr grid_size = blend_modes.size();
    auto constexpr cell_size = 2.f / grid_size;
    for (size_t x = 0; x < grid_size; ++x) {
        for (size_t y = 0; y < grid_size; ++y) {
            auto rgb_mode = blend_modes[x];
            auto alpha_mode = blend_modes[y];

            glBlendEquationSeparate(rgb_mode, alpha_mode);

            glGetIntegerv(GL_BLEND_EQUATION_RGB, &actual_mode);
            EXPECT_EQ(static_cast<GLenum>(actual_mode), rgb_mode);

            glGetIntegerv(GL_BLEND_EQUATION_ALPHA, &actual_mode);
            EXPECT_EQ(static_cast<GLenum>(actual_mode), alpha_mode);

            glRectf(
                -1.f + cell_size * static_cast<float>(x),
                1.f - cell_size * static_cast<float>(y),
                -1.f + cell_size * static_cast<float>(x + 1),
                1.f - cell_size * static_cast<float>(y + 1));
        }
    }

    EXPECT_EQ(glGetError(), 0u);

    context->present();
    expect_bitmap_equals_reference(context->frontbuffer(), "0012_blend_equations"sv);
}
