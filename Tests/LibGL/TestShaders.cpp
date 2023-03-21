/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/QOIWriter.h>
#include <LibTest/TestCase.h>

static NonnullOwnPtr<GL::GLContext> create_testing_context(int width, int height)
{
    auto bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { width, height }));
    auto context = MUST(GL::create_context(*bitmap));
    GL::make_context_current(context);

    return context;
}

TEST_CASE(0001_program_creation)
{
    auto context = create_testing_context(64, 64);

    GLuint vertex_shader;
    GLuint fragment_shader;
    GLuint program;

    auto vertex_shader_source = "#version 330\n"
                                "void main() {\n"
                                "    gl_Position = vec4(0, 0, 0, 0);\n"
                                "}\n";

    auto fragment_shader_source = "#version 330\n"
                                  "out vec4 color;\n"
                                  "void main() {\n"
                                  "    color = vec4(1, 1, 1, 1);\n"
                                  "}\n";

    vertex_shader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertex_shader, 1, &vertex_shader_source, nullptr);
    glCompileShader(vertex_shader);
    GLint vertex_shader_compile_status;
    glGetShaderiv(vertex_shader, GL_COMPILE_STATUS, &vertex_shader_compile_status);
    EXPECT_EQ(vertex_shader_compile_status, GL_TRUE);

    fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragment_shader, 1, &fragment_shader_source, nullptr);
    glCompileShader(fragment_shader);
    GLint fragment_shader_compile_status;
    glGetShaderiv(fragment_shader, GL_COMPILE_STATUS, &fragment_shader_compile_status);
    EXPECT_EQ(fragment_shader_compile_status, GL_TRUE);

    program = glCreateProgram();
    glAttachShader(program, vertex_shader);
    glAttachShader(program, fragment_shader);
    glLinkProgram(program);
    glUseProgram(program);

    glBegin(GL_TRIANGLES);
    glColor3f(1, 0, 0);
    glVertex2i(-1, -1);
    glColor3f(0, 1, 0);
    glVertex2i(1, -1);
    glColor3f(0, 0, 1);
    glVertex2i(1, 1);
    glEnd();

    context->present();

    glDeleteShader(vertex_shader);
    glDeleteShader(fragment_shader);
    glDeleteProgram(program);

    EXPECT_EQ(glGetError(), 0u);
}
