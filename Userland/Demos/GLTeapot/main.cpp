/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <unistd.h>

#include "Mesh.h"
#include "MeshLoader.h"
#include "WavefrontOBJLoader.h"

static constexpr u16 RENDER_WIDTH = 640;
static constexpr u16 RENDER_HEIGHT = 480;

class GLContextWidget final : public GUI::Widget {
    C_OBJECT(GLContextWidget)
public:
private:
    GLContextWidget()
    {
        m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { RENDER_WIDTH, RENDER_HEIGHT });
        m_context = GL::create_context(*m_bitmap);

        start_timer(20);

        GL::make_context_current(m_context);
        glFrontFace(GL_CW);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        // Set projection matrix
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glFrustum(-0.5, 0.5, -0.5, 0.5, 1, 1500);

        // Load the teapot
        auto mesh_loader = adopt_own(*new WavefrontOBJLoader());
        m_teapot = mesh_loader->load("/res/gl/teapot.obj");

        dbgln("GLTeapot: teapot mesh has {} triangles.", m_teapot->triangle_count());
    }

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

private:
    RefPtr<Mesh> m_teapot;
    RefPtr<Gfx::Bitmap> m_bitmap;
    OwnPtr<GL::GLContext> m_context;
};

void GLContextWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    /* Blit it! */
    painter.draw_scaled_bitmap(event.rect(), *m_bitmap, m_bitmap->rect());
}

void GLContextWidget::timer_event(Core::TimerEvent&)
{
    static float angle = 0.0f;

    angle -= 0.01f;

    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClearDepth(1.0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    auto matrix = FloatMatrix4x4::translate(FloatVector3(0, 0, -8.5))
        * FloatMatrix4x4::rotate(FloatVector3(1, 0, 0), angle)
        * FloatMatrix4x4::rotate(FloatVector3(0, 1, 0), 0.0f)
        * FloatMatrix4x4::rotate(FloatVector3(0, 0, 1), angle);

    glMatrixMode(GL_MODELVIEW);
    glLoadMatrixf((float*)matrix.elements());

    m_teapot->draw();

    m_context->present();
    update();
}

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    // Construct the main window
    auto window = GUI::Window::construct();
    auto app_icon = GUI::Icon::default_icon("app-teapot");

    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("GLTeapot");
    window->resize(640, 480);
    window->set_resizable(false);
    window->set_double_buffering_enabled(true);
    window->set_main_widget<GLContextWidget>();

    window->show();

    return app->exec();
}
