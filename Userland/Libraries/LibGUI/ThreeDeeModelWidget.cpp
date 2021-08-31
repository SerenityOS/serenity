/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Conrad Pankoff <deoxxa@fknsrs.biz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ThreeDeeModelWidget.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibThreeDee/Mesh.h>

REGISTER_WIDGET(GUI, ThreeDeeModelWidget)

namespace GUI {

ThreeDeeModelWidget::ThreeDeeModelWidget()
{
    REGISTER_INT_PROPERTY("render_width", render_width, set_render_width);
    REGISTER_INT_PROPERTY("render_height", render_height, set_render_height);
    REGISTER_FLOAT_PROPERTY("rotation_angle_x", rotation_angle_x, set_rotation_angle_x);
    REGISTER_FLOAT_PROPERTY("rotation_angle_y", rotation_angle_y, set_rotation_angle_y);
    REGISTER_FLOAT_PROPERTY("rotation_angle_z", rotation_angle_z, set_rotation_angle_z);
    REGISTER_FLOAT_PROPERTY("texture_scale", texture_scale, set_texture_scale);
    REGISTER_FLOAT_PROPERTY("zoom", zoom, set_zoom);
    REGISTER_READONLY_INT_PROPERTY("cycles", cycles);
    REGISTER_READONLY_INT_PROPERTY("accumulated_time", accumulated_time);
    REGISTER_READONLY_INT_PROPERTY("frame_rate", frame_rate);

    reset_context();
}

void ThreeDeeModelWidget::set_mesh(RefPtr<ThreeDee::Mesh> mesh)
{
    set_mesh_and_texture(mesh, nullptr);
}

void ThreeDeeModelWidget::set_mesh_and_texture(RefPtr<ThreeDee::Mesh> mesh, RefPtr<Gfx::Bitmap> texture)
{
    // Upload texture data to the GL
    if (texture) {
        GLuint tex;
        glGenTextures(1, &tex);
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, texture->width(), texture->height(), 0, GL_BGRA, GL_UNSIGNED_BYTE, texture->scanline(0));
    }

    m_mesh = mesh;
}

void ThreeDeeModelWidget::set_render_width_and_height(int render_width, int render_height)
{
    m_render_width = render_width;
    m_render_height = render_height;

    reset_context();
}

void ThreeDeeModelWidget::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    if (m_bitmap)
        painter.draw_scaled_bitmap(frame_inner_rect(), *m_bitmap, m_bitmap->rect());
}

void ThreeDeeModelWidget::timer_event(Core::TimerEvent&)
{
    Core::ElapsedTimer timer;
    timer.start();

    glCallList(m_init_list);

    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    glTranslatef(0, 0, -8.5);
    glRotatef(m_rotation_angle_x, 1, 0, 0);
    glRotatef(m_rotation_angle_y, 0, 1, 0);
    glRotatef(m_rotation_angle_z, 0, 0, 1);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, m_wrap_s_mode);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, m_wrap_t_mode);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, m_mag_filter);
    glScalef(m_zoom, m_zoom, m_zoom);

    if (!m_mesh.is_null())
        m_mesh->draw(m_texture_scale);

    m_context->present();

    if ((m_cycles % 30) == 0) {
        int render_time = m_accumulated_time / 30;
        m_frame_rate = render_time > 0 ? 1000 / render_time : 0;
        m_accumulated_time = 0;
    }

    update();

    m_accumulated_time += timer.elapsed();
    m_cycles++;
}

void ThreeDeeModelWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (on_mousemove)
        on_mousemove(event);
}

void ThreeDeeModelWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (on_mousewheel)
        on_mousewheel(event);
}

void ThreeDeeModelWidget::reset_context()
{
    m_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, { m_render_width, m_render_height });
    m_context = GL::create_context(*m_bitmap);

    GL::make_context_current(m_context);
    glFrontFace(GL_CCW);
    glEnable(GL_CULL_FACE);
    glEnable(GL_DEPTH_TEST);

    // Set projection matrix
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glFrustum(-0.5, 0.5, -0.5, 0.5, 1, 1500);

    m_init_list = glGenLists(1);
    glNewList(m_init_list, GL_COMPILE);
    {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClearDepth(1.0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    glEndList();

    if (!has_timer())
        start_timer(20);
}

}
