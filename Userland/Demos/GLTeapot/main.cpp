/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
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
    bool load(const String& fname);

private:
    GLContextWidget()
        : m_mesh_loader(adopt_own(*new WavefrontOBJLoader()))
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

        m_init_list = glGenLists(1);
        glNewList(m_init_list, GL_COMPILE);
        {
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClearDepth(1.0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }
        glEndList();

        // Load the teapot
        m_mesh = m_mesh_loader->load("/res/gl/teapot.obj");

        dbgln("GLTeapot: teapot mesh has {} triangles.", m_mesh->triangle_count());
    }

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

private:
    RefPtr<Mesh> m_mesh;
    RefPtr<Gfx::Bitmap> m_bitmap;
    OwnPtr<GL::GLContext> m_context;
    NonnullOwnPtr<WavefrontOBJLoader> m_mesh_loader;
    GLuint m_init_list { 0 };
};

void GLContextWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(rect(), *m_bitmap, m_bitmap->rect());
}

void GLContextWidget::timer_event(Core::TimerEvent&)
{
    static float angle = 0.0f;
    glCallList(m_init_list);

    angle -= 0.01f;
    auto matrix = Gfx::translation_matrix(FloatVector3(0, 0, -8.5))
        * Gfx::rotation_matrix(FloatVector3(1, 0, 0), angle)
        * Gfx::rotation_matrix(FloatVector3(0, 1, 0), 0.0f)
        * Gfx::rotation_matrix(FloatVector3(0, 0, 1), angle);

    // We need to transpose here because OpenGL expects matrices in column major order
    // but our matrix class stores elements in row major order.
    matrix = matrix.transpose();

    glMatrixMode(GL_MODELVIEW);
    glLoadMatrixf((float*)matrix.elements());

    if (!m_mesh.is_null())
        m_mesh->draw();

    m_context->present();
    update();
}

bool GLContextWidget::load(const String& fname)
{
    m_mesh = m_mesh_loader->load(fname);
    if (!m_mesh.is_null()) {
        dbgln("GLTeapot: mesh has {} triangles.", m_mesh->triangle_count());
        return true;
    }

    return false;
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

    if (unveil("/home", "r") < 0) {
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
    auto& widget = window->set_main_widget<GLContextWidget>();

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");

    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window);

        if (!open_path.has_value())
            return;

        auto file = Core::File::construct(open_path.value());

        if (!file->filename().ends_with(".obj")) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: invalid file type", open_path.value()), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        if (!file->open(Core::OpenMode::ReadOnly) && file->error() != ENOENT) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: {}", open_path.value(), strerror(errno)), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        if (file->is_device()) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: Can't open device files", open_path.value()), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        if (file->is_directory()) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: Can't open directories", open_path.value()), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        if (!widget.load(file->filename()))
            GUI::MessageBox::show(window, String::formatted("Reading \"{}\" failed.", open_path.value()), "Error", GUI::MessageBox::Type::Error);
    }));

    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("GLTeapot", app_icon, window));

    window->set_menubar(move(menubar));
    window->show();

    return app->exec();
}
