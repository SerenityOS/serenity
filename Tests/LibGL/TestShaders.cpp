/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
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

static NonnullOwnPtr<GL::GLContext> create_testing_context(int width, int height)
{
    auto bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, { width, height }));
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

    fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragment_shader, 1, &fragment_shader_source, nullptr);
    glCompileShader(fragment_shader);

    program = glCreateProgram();
    glAttachShader(program, vertex_shader);
    glAttachShader(program, fragment_shader);
    glLinkProgram(program);

    glDeleteShader(vertex_shader);
    glDeleteShader(fragment_shader);
    glDeleteProgram(program);

    EXPECT_EQ(glGetError(), 0u);
}
