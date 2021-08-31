/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Conrad Pankoff <deoxxa@fknsrs.biz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibThreeDee/Mesh.h>

namespace GUI {

class ThreeDeeModelWidget final : public Frame {
    C_OBJECT(ThreeDeeModelWidget);

public:
    void set_mesh(RefPtr<ThreeDee::Mesh>);
    void set_mesh_and_texture(RefPtr<ThreeDee::Mesh>, RefPtr<Gfx::Bitmap>);
    void set_render_width_and_height(int render_width, int render_height);
    int render_width() const { return m_render_width; }
    void set_render_width(int render_width) { set_render_width_and_height(render_width, m_render_height); }
    int render_height() const { return m_render_height; }
    void set_render_height(int render_height) { set_render_width_and_height(m_render_width, render_height); }
    void set_rotation_angle_x(float rotation_angle_x) { m_rotation_angle_x = rotation_angle_x; }
    float rotation_angle_x() const { return m_rotation_angle_x; }
    void set_rotation_angle_y(float rotation_angle_y) { m_rotation_angle_y = rotation_angle_y; }
    float rotation_angle_y() const { return m_rotation_angle_y; }
    void set_rotation_angle_z(float rotation_angle_z) { m_rotation_angle_z = rotation_angle_z; }
    float rotation_angle_z() const { return m_rotation_angle_z; }
    void set_texture_scale(float texture_scale) { m_texture_scale = texture_scale; }
    float texture_scale() const { return m_texture_scale; }
    void set_zoom(float zoom) { m_zoom = zoom; }
    float zoom() const { return m_zoom; }

    void set_wrap_s_mode(GLint mode) { m_wrap_s_mode = mode; }
    GLint wrap_s_mode() const { return m_wrap_s_mode; }
    void set_wrap_t_mode(GLint mode) { m_wrap_t_mode = mode; }
    GLint wrap_t_mode() const { return m_wrap_t_mode; }
    void set_mag_filter(GLint filter) { m_mag_filter = filter; }
    GLint mag_filter() const { return m_mag_filter; }

    int cycles() const { return m_cycles; }
    int accumulated_time() const { return m_accumulated_time; }
    int frame_rate() const { return m_frame_rate; }

    Function<void(GUI::MouseEvent&)> on_mousemove;
    Function<void(GUI::MouseEvent&)> on_mousewheel;

private:
    ThreeDeeModelWidget();

    virtual void paint_event(PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mousewheel_event(GUI::MouseEvent& event) override;

    void reset_context();

private:
    RefPtr<ThreeDee::Mesh> m_mesh;
    RefPtr<Gfx::Bitmap> m_bitmap;
    OwnPtr<GL::GLContext> m_context;
    GLuint m_init_list { 0 };

    int m_render_width = 640;
    int m_render_height = 480;
    float m_rotation_angle_x = 0;
    float m_rotation_angle_y = 0;
    float m_rotation_angle_z = 0;
    float m_texture_scale = 1.;
    float m_zoom = 1;

    GLint m_wrap_s_mode = GL_REPEAT;
    GLint m_wrap_t_mode = GL_REPEAT;
    GLint m_mag_filter = GL_NEAREST;

    int m_cycles = 0;
    int m_accumulated_time = 0;
    int m_frame_rate = 0;
};

}
