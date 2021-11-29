/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Debug.h>
#include <AK/Format.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/FontDatabase.h>
#include <fcntl.h>
#include <unistd.h>

#define RENDER_WIDTH 16
#define RENDER_HEIGHT 16

TEST_CASE(simple_triangle)
{
    auto bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, { RENDER_WIDTH, RENDER_HEIGHT }));
    auto context = GL::create_context(*bitmap);

    GL::make_context_current(context);

    glFrontFace(GL_CCW);
    glCullFace(GL_BACK);
    glEnable(GL_CULL_FACE);
    glEnable(GL_DEPTH_TEST);

    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClearDepth(1.0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glBegin(GL_TRIANGLES);
    glColor4f(1, 1, 1, 1);
    glVertex2f(0, 1);
    glVertex2f(-1, -1);
    glVertex2f(1, -1);
    glEnd();

    context->present();

    EXPECT_EQ(glGetError(), 0u);
    // FIXME: Verify that the image is indeed correct

    if constexpr (GL_DEBUG) {
        // output the image to manually verify that the output is correct
        Gfx::BMPWriter writer {};
        auto buffer = writer.dump(bitmap);
        int fd = open("./picture.bmp", O_CREAT | O_WRONLY, 0755);
        EXPECT(fd > 0);
        ssize_t nwritten = write(fd, buffer.data(), buffer.size());
        EXPECT_EQ((size_t)nwritten, buffer.size());
        close(fd);
    }
}
