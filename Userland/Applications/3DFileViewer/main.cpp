/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGUI/ActionGroup.h>
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

class GLContextWidget final : public GUI::Frame {
    C_OBJECT(GLContextWidget);

public:
    bool load(const String& fname);
    void toggle_rotate_x() { m_rotate_x = !m_rotate_x; }
    void toggle_rotate_y() { m_rotate_y = !m_rotate_y; }
    void toggle_rotate_z() { m_rotate_z = !m_rotate_z; }
    void set_rotation_speed(float speed) { m_rotation_speed = speed; }

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
    }

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

private:
    RefPtr<Mesh> m_mesh;
    RefPtr<Gfx::Bitmap> m_bitmap;
    OwnPtr<GL::GLContext> m_context;
    NonnullOwnPtr<WavefrontOBJLoader> m_mesh_loader;
    GLuint m_init_list { 0 };
    bool m_rotate_x = true;
    bool m_rotate_y = false;
    bool m_rotate_z = true;
    float m_rotation_speed = 1.f;
};

void GLContextWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(frame_inner_rect(), *m_bitmap, m_bitmap->rect());
}

void GLContextWidget::timer_event(Core::TimerEvent&)
{
    static float angle = 0.0f;
    glCallList(m_init_list);

    angle -= 0.01f;
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    glTranslatef(0, 0, -8.5);
    glRotatef(m_rotate_x ? angle * m_rotation_speed : 0.f, 1, 0, 0);
    glRotatef(m_rotate_y ? angle * m_rotation_speed : 0.f, 0, 1, 0);
    glRotatef(m_rotate_z ? angle * m_rotation_speed : 0.f, 0, 0, 1);

    if (!m_mesh.is_null())
        m_mesh->draw();

    m_context->present();
    update();
}

bool GLContextWidget::load(const String& filename)
{
    auto file = Core::File::construct(filename);

    if (!file->filename().ends_with(".obj")) {
        GUI::MessageBox::show(window(), String::formatted("Opening \"{}\" failed: invalid file type", filename), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    if (!file->open(Core::OpenMode::ReadOnly) && file->error() != ENOENT) {
        GUI::MessageBox::show(window(), String::formatted("Opening \"{}\" failed: {}", filename, strerror(errno)), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    if (file->is_device()) {
        GUI::MessageBox::show(window(), String::formatted("Opening \"{}\" failed: Can't open device files", filename), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    if (file->is_directory()) {
        GUI::MessageBox::show(window(), String::formatted("Opening \"{}\" failed: Can't open directories", filename), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    auto new_mesh = m_mesh_loader->load(file);
    if (new_mesh.is_null()) {
        GUI::MessageBox::show(window(), String::formatted("Reading \"{}\" failed.", filename), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    // Determine whether or not a texture for this model resides within the same directory
    StringBuilder builder;
    builder.append(filename.split('.').at(0));
    builder.append(".bmp");

    // Attempt to open the texture file from disk
    auto texture_image = Gfx::Bitmap::load_from_file(builder.string_view());

    GLuint tex;
    glGenTextures(1, &tex);
    if (texture_image) {
        // Upload texture data to the GL
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, texture_image->width(), texture_image->height(), 0, GL_RGBA, GL_UNSIGNED_BYTE, texture_image->scanline(0));
    } else {
        dbgln("3DFileViewer: Couldn't load texture for {}", filename);
    }

    m_mesh = new_mesh;
    dbgln("3DFileViewer: mesh has {} triangles.", m_mesh->triangle_count());

    return true;
}

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio thread recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // Construct the main window
    auto window = GUI::Window::construct();
    auto app_icon = GUI::Icon::default_icon("app-3d-file-viewer");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("3D File Viewer");
    window->resize(640 + 4, 480 + 4);
    window->set_resizable(false);
    window->set_double_buffering_enabled(true);
    auto& widget = window->set_main_widget<GLContextWidget>();

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");

    auto load_model = [&](StringView const& filename) {
        if (widget.load(filename)) {
            auto canonical_path = Core::File::real_path_for(filename);
            window->set_title(String::formatted("{} - 3D File Viewer", canonical_path));
        }
    };

    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window);

        if (!open_path.has_value())
            return;

        load_model(open_path.value());
    }));
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& view_menu = menubar->add_menu("&View");
    view_menu.add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto& rotation_axis_menu = view_menu.add_submenu("Rotation &Axis");
    auto rotation_x_action = GUI::Action::create_checkable("&X", [&widget](auto&) {
        widget.toggle_rotate_x();
    });
    auto rotation_y_action = GUI::Action::create_checkable("&Y", [&widget](auto&) {
        widget.toggle_rotate_y();
    });
    auto rotation_z_action = GUI::Action::create_checkable("&Z", [&widget](auto&) {
        widget.toggle_rotate_z();
    });

    rotation_axis_menu.add_action(*rotation_x_action);
    rotation_axis_menu.add_action(*rotation_y_action);
    rotation_axis_menu.add_action(*rotation_z_action);

    rotation_x_action->set_checked(true);
    rotation_z_action->set_checked(true);

    auto& rotation_speed_menu = view_menu.add_submenu("Rotation &Speed");
    GUI::ActionGroup rotation_speed_actions;
    rotation_speed_actions.set_exclusive(true);

    auto no_rotation_action = GUI::Action::create_checkable("N&o Rotation", [&widget](auto&) {
        widget.set_rotation_speed(0.f);
    });
    auto slow_rotation_action = GUI::Action::create_checkable("&Slow", [&widget](auto&) {
        widget.set_rotation_speed(0.5f);
    });
    auto normal_rotation_action = GUI::Action::create_checkable("&Normal", [&widget](auto&) {
        widget.set_rotation_speed(1.f);
    });
    auto fast_rotation_action = GUI::Action::create_checkable("&Fast", [&widget](auto&) {
        widget.set_rotation_speed(1.5f);
    });

    rotation_speed_actions.add_action(*no_rotation_action);
    rotation_speed_actions.add_action(*slow_rotation_action);
    rotation_speed_actions.add_action(*normal_rotation_action);
    rotation_speed_actions.add_action(*fast_rotation_action);

    rotation_speed_menu.add_action(*no_rotation_action);
    rotation_speed_menu.add_action(*slow_rotation_action);
    rotation_speed_menu.add_action(*normal_rotation_action);
    rotation_speed_menu.add_action(*fast_rotation_action);

    normal_rotation_action->set_checked(true);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("3D File Viewer", app_icon, window));

    window->set_menubar(move(menubar));
    window->show();

    auto filename = argc > 1 ? argv[1] : "/home/anon/Documents/3D Models/teapot.obj";
    load_model(filename);

    return app->exec();
}
