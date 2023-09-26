/*
 * Copyright (c) 2023, Glenford Williams <hey@glenfordwilliams.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGUI/OpenGLWidget.h>

namespace GUI {

OpenGLWidget::OpenGLWidget()
{
    set_frame_style(Gfx::FrameStyle::NoFrame);
}

void OpenGLWidget::resize_event(ResizeEvent& event)
{
    GUI::Frame::resize_event(event);
    resize_gl(event.size().width(), event.size().height());
}

void OpenGLWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    GUI::Frame::paint_event(event);

    if (!initialized())
        initialize();

    make_current();
    paint_gl();
    context().present();

    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(frame_inner_rect(), *m_bitmap, m_bitmap->rect());
}

void OpenGLWidget::initialize()
{
    if (initialized())
        return;

    m_bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, size()));
    m_context = MUST(GL::create_context(*m_bitmap));

    initialize_gl();
}

void OpenGLWidget::initialize_gl()
{
}

void OpenGLWidget::paint_gl()
{
}

void OpenGLWidget::resize_gl(int w, int h)
{
    glViewport(0, 0, w, h);

    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
}
}
