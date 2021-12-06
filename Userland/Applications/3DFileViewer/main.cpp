/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>

#include "Mesh.h"
#include "WavefrontOBJLoader.h"

static constexpr u16 RENDER_WIDTH = 640;
static constexpr u16 RENDER_HEIGHT = 480;

class GLContextWidget final : public GUI::Frame {
    C_OBJECT(GLContextWidget);

public:
    bool load_path(String const& fname);
    bool load_fd_and_close(int fd, String const& fname);
    bool load_file(Core::File& file, String const& fname);
    void toggle_rotate_x() { m_rotate_x = !m_rotate_x; }
    void toggle_rotate_y() { m_rotate_y = !m_rotate_y; }
    void toggle_rotate_z() { m_rotate_z = !m_rotate_z; }
    void set_rotation_speed(float speed) { m_rotation_speed = speed; }
    void set_stat_label(RefPtr<GUI::Label> l) { m_stats = l; };
    void set_wrap_s_mode(GLint mode) { m_wrap_s_mode = mode; }
    void set_wrap_t_mode(GLint mode) { m_wrap_t_mode = mode; }
    void set_texture_scale(float scale) { m_texture_scale = scale; }
    void set_texture_enabled(bool texture_enabled) { m_texture_enabled = texture_enabled; }
    void set_mag_filter(GLint filter) { m_mag_filter = filter; }

    void toggle_show_frame_rate()
    {
        m_show_frame_rate = !m_show_frame_rate;
        m_stats->set_visible(m_show_frame_rate);
    }

private:
    GLContextWidget()
        : m_mesh_loader(adopt_own(*new WavefrontOBJLoader()))
    {
        m_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, { RENDER_WIDTH, RENDER_HEIGHT }).release_value_but_fixme_should_propagate_errors();
        m_context = GL::create_context(*m_bitmap);

        start_timer(20);

        GL::make_context_current(m_context);
        glFrontFace(GL_CCW);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        // Set projection matrix
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        auto const half_aspect_ratio = static_cast<double>(RENDER_WIDTH) / RENDER_HEIGHT / 2;
        glFrustum(-half_aspect_ratio, half_aspect_ratio, -0.5, 0.5, 1, 1500);

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
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;

private:
    RefPtr<Mesh> m_mesh;
    RefPtr<Gfx::Bitmap> m_bitmap;
    OwnPtr<GL::GLContext> m_context;
    NonnullOwnPtr<WavefrontOBJLoader> m_mesh_loader;
    GLuint m_init_list { 0 };
    bool m_rotate_x = true;
    bool m_rotate_y = false;
    bool m_rotate_z = true;
    float m_angle_x = 0.0;
    float m_angle_y = 0.0;
    float m_angle_z = 0.0;
    Gfx::IntPoint m_last_mouse;
    float m_rotation_speed = 60.f;
    bool m_show_frame_rate = false;
    int m_cycles = 0;
    int m_accumulated_time = 0;
    RefPtr<GUI::Label> m_stats;
    GLint m_wrap_s_mode = GL_REPEAT;
    GLint m_wrap_t_mode = GL_REPEAT;
    bool m_texture_enabled { true };
    float m_texture_scale = 1.0f;
    GLint m_mag_filter = GL_NEAREST;
    float m_zoom = 1;
};

void GLContextWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(frame_inner_rect(), *m_bitmap, m_bitmap->rect());
}

void GLContextWidget::resize_event(GUI::ResizeEvent& event)
{
    GUI::Frame::resize_event(event);

    if (m_stats)
        m_stats->set_x(width() - m_stats->width() - 6);
};

void GLContextWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() == GUI::MouseButton::Primary) {
        int delta_x = m_last_mouse.x() - event.x();
        int delta_y = m_last_mouse.y() - event.y();

        m_angle_x -= delta_y / 2.0f;
        m_angle_y -= delta_x / 2.0f;
    }

    m_last_mouse = event.position();
}

void GLContextWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (event.wheel_delta() > 0)
        m_zoom /= 1.1f;
    else
        m_zoom *= 1.1f;
}

void GLContextWidget::timer_event(Core::TimerEvent&)
{
    auto timer = Core::ElapsedTimer::start_new();

    glCallList(m_init_list);

    if (m_rotate_x)
        m_angle_x -= m_rotation_speed * 0.01f;
    if (m_rotate_y)
        m_angle_y -= m_rotation_speed * 0.01f;
    if (m_rotate_z)
        m_angle_z -= m_rotation_speed * 0.01f;
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    glTranslatef(0, 0, -8.5);
    glRotatef(m_angle_x, 1, 0, 0);
    glRotatef(m_angle_y, 0, 1, 0);
    glRotatef(m_angle_z, 0, 0, 1);

    if (m_texture_enabled) {
        glEnable(GL_TEXTURE_2D);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, m_wrap_s_mode);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, m_wrap_t_mode);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, m_mag_filter);
    } else {
        glDisable(GL_TEXTURE_2D);
    }
    glScalef(m_zoom, m_zoom, m_zoom);

    if (!m_mesh.is_null())
        m_mesh->draw(m_texture_scale);

    m_context->present();

    if ((m_cycles % 30) == 0) {
        auto render_time = m_accumulated_time / 30.0;
        auto frame_rate = render_time > 0 ? 1000 / render_time : 0;
        m_stats->set_text(String::formatted("{:.0f} fps, {:.1f} ms", frame_rate, render_time));
        m_accumulated_time = 0;
    }

    update();

    m_accumulated_time += timer.elapsed();
    m_cycles++;
}

bool GLContextWidget::load_path(String const& filename)
{
    auto file = Core::File::construct(filename);

    if (!file->open(Core::OpenMode::ReadOnly) && file->error() != ENOENT) {
        GUI::MessageBox::show(window(), String::formatted("Opening \"{}\" failed: {}", filename, strerror(errno)), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    return load_file(file, filename);
}

bool GLContextWidget::load_fd_and_close(int fd, String const& filename)
{
    auto file = Core::File::construct();

    if (!file->open(fd, Core::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescriptor::Yes) && file->error() != ENOENT) {
        GUI::MessageBox::show(window(), String::formatted("Opening \"{}\" failed: {}", filename, strerror(errno)), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    return load_file(file, filename);
}

bool GLContextWidget::load_file(Core::File& file, String const& filename)
{
    if (!filename.ends_with(".obj")) {
        GUI::MessageBox::show(window(), String::formatted("Opening \"{}\" failed: invalid file type", filename), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    if (file.is_device()) {
        GUI::MessageBox::show(window(), String::formatted("Opening \"{}\" failed: Can't open device files", filename), "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    if (file.is_directory()) {
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

    String texture_path = Core::File::absolute_path(builder.string_view());

    // Attempt to open the texture file from disk
    RefPtr<Gfx::Bitmap> texture_image;
    if (Core::File::exists(texture_path)) {
        auto bitmap_or_error = Gfx::Bitmap::try_load_from_file(texture_path);
        if (!bitmap_or_error.is_error())
            texture_image = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    } else {
        auto result = FileSystemAccessClient::Client::the().request_file(window()->window_id(), builder.string_view(), Core::OpenMode::ReadOnly);

        if (result.error != 0) {
            return false;
        }

        auto bitmap_or_error = Gfx::Bitmap::try_load_from_fd_and_close(*result.fd, *result.chosen_file);
        if (!bitmap_or_error.is_error())
            texture_image = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    }

    GLuint tex;
    glGenTextures(1, &tex);
    if (texture_image) {
        // Upload texture data to the GL
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, texture_image->width(), texture_image->height(), 0, GL_BGRA, GL_UNSIGNED_BYTE, texture_image->scanline(0));
    } else {
        dbgln("3DFileViewer: Couldn't load texture for {}", filename);
    }

    m_mesh = new_mesh;
    dbgln("3DFileViewer: mesh has {} triangles.", m_mesh->triangle_count());

    return true;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = GUI::Application::construct(arguments);

    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath unix"));

    TRY(Core::System::unveil("/tmp/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/home/anon/Documents/3D Models/teapot.obj", "r"));
    TRY(Core::System::unveil("/home/anon/Documents/3D Models/teapot.bmp", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    // Construct the main window
    auto window = GUI::Window::construct();
    auto app_icon = GUI::Icon::default_icon("app-3d-file-viewer");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("3D File Viewer");
    window->resize(640 + 4, 480 + 4);
    window->set_resizable(false);
    window->set_double_buffering_enabled(true);
    auto& widget = window->set_main_widget<GLContextWidget>();

    auto& time = widget.add<GUI::Label>();
    time.set_visible(false);
    time.set_foreground_role(ColorRole::HoverHighlight);
    time.set_relative_rect({ 0, 8, 100, 10 });
    time.set_text_alignment(Gfx::TextAlignment::CenterRight);
    time.set_x(widget.width() - time.width() - 6);
    widget.set_stat_label(time);

    auto& file_menu = window->add_menu("&File");

    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        auto result = FileSystemAccessClient::Client::the().open_file(window->window_id());

        if (result.error != 0)
            return;

        if (widget.load_fd_and_close(*result.fd, *result.chosen_file)) {
            auto canonical_path = Core::File::absolute_path(*result.chosen_file);
            window->set_title(String::formatted("{} - 3D File Viewer", canonical_path));
        }
    }));
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& view_menu = window->add_menu("&View");
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
        widget.set_rotation_speed(30.f);
    });
    auto normal_rotation_action = GUI::Action::create_checkable("&Normal", [&widget](auto&) {
        widget.set_rotation_speed(60.f);
    });
    auto fast_rotation_action = GUI::Action::create_checkable("&Fast", [&widget](auto&) {
        widget.set_rotation_speed(90.f);
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

    auto show_frame_rate_action = GUI::Action::create_checkable("Show Frame &Rate", [&widget](auto&) {
        widget.toggle_show_frame_rate();
    });

    view_menu.add_action(*show_frame_rate_action);

    auto& texture_menu = window->add_menu("&Texture");

    auto texture_enabled_action = GUI::Action::create_checkable("&Enable Texture", [&widget](auto& action) {
        widget.set_texture_enabled(action.is_checked());
    });
    texture_enabled_action->set_checked(true);
    texture_menu.add_action(texture_enabled_action);

    auto& wrap_u_menu = texture_menu.add_submenu("Wrap &S");
    GUI::ActionGroup wrap_s_actions;
    wrap_s_actions.set_exclusive(true);

    auto wrap_u_repeat_action = GUI::Action::create_checkable("&Repeat", [&widget](auto&) {
        widget.set_wrap_s_mode(GL_REPEAT);
    });
    auto wrap_u_mirrored_repeat_action = GUI::Action::create_checkable("&Mirrored Repeat", [&widget](auto&) {
        widget.set_wrap_s_mode(GL_MIRRORED_REPEAT);
    });
    auto wrap_u_clamp_action = GUI::Action::create_checkable("&Clamp", [&widget](auto&) {
        widget.set_wrap_s_mode(GL_CLAMP);
    });

    wrap_s_actions.add_action(*wrap_u_repeat_action);
    wrap_s_actions.add_action(*wrap_u_mirrored_repeat_action);
    wrap_s_actions.add_action(*wrap_u_clamp_action);

    wrap_u_menu.add_action(*wrap_u_repeat_action);
    wrap_u_menu.add_action(*wrap_u_mirrored_repeat_action);
    wrap_u_menu.add_action(*wrap_u_clamp_action);

    wrap_u_repeat_action->set_checked(true);

    auto& wrap_t_menu = texture_menu.add_submenu("Wrap &T");
    GUI::ActionGroup wrap_t_actions;
    wrap_t_actions.set_exclusive(true);

    auto wrap_t_repeat_action = GUI::Action::create_checkable("&Repeat", [&widget](auto&) {
        widget.set_wrap_t_mode(GL_REPEAT);
    });
    auto wrap_t_mirrored_repeat_action = GUI::Action::create_checkable("&Mirrored Repeat", [&widget](auto&) {
        widget.set_wrap_t_mode(GL_MIRRORED_REPEAT);
    });
    auto wrap_t_clamp_action = GUI::Action::create_checkable("&Clamp", [&widget](auto&) {
        widget.set_wrap_t_mode(GL_CLAMP);
    });

    wrap_t_actions.add_action(*wrap_t_repeat_action);
    wrap_t_actions.add_action(*wrap_t_mirrored_repeat_action);
    wrap_t_actions.add_action(*wrap_t_clamp_action);

    wrap_t_menu.add_action(*wrap_t_repeat_action);
    wrap_t_menu.add_action(*wrap_t_mirrored_repeat_action);
    wrap_t_menu.add_action(*wrap_t_clamp_action);

    wrap_t_repeat_action->set_checked(true);

    auto& texture_scale_menu = texture_menu.add_submenu("S&cale");
    GUI::ActionGroup texture_scale_actions;
    texture_scale_actions.set_exclusive(true);

    auto texture_scale_025_action = GUI::Action::create_checkable("0.25x", [&widget](auto&) {
        widget.set_texture_scale(0.25f);
    });

    auto texture_scale_05_action = GUI::Action::create_checkable("0.5x", [&widget](auto&) {
        widget.set_texture_scale(0.5f);
    });

    auto texture_scale_1_action = GUI::Action::create_checkable("1x", [&widget](auto&) {
        widget.set_texture_scale(1);
    });

    auto texture_scale_2_action = GUI::Action::create_checkable("2x", [&widget](auto&) {
        widget.set_texture_scale(2);
    });

    auto texture_scale_4_action = GUI::Action::create_checkable("4x", [&widget](auto&) {
        widget.set_texture_scale(4);
    });

    texture_scale_actions.add_action(*texture_scale_025_action);
    texture_scale_actions.add_action(*texture_scale_05_action);
    texture_scale_actions.add_action(*texture_scale_1_action);
    texture_scale_actions.add_action(*texture_scale_2_action);
    texture_scale_actions.add_action(*texture_scale_4_action);

    texture_scale_menu.add_action(*texture_scale_025_action);
    texture_scale_menu.add_action(*texture_scale_05_action);
    texture_scale_menu.add_action(*texture_scale_1_action);
    texture_scale_menu.add_action(*texture_scale_2_action);
    texture_scale_menu.add_action(*texture_scale_4_action);

    texture_scale_1_action->set_checked(true);

    auto& texture_mag_filter_menu = texture_menu.add_submenu("Mag Filter");
    GUI::ActionGroup texture_mag_filter_actions;
    texture_mag_filter_actions.set_exclusive(true);

    auto texture_mag_filter_nearest_action = GUI::Action::create_checkable("&Nearest", [&widget](auto&) {
        widget.set_mag_filter(GL_NEAREST);
    });

    auto texture_mag_filter_linear_action = GUI::Action::create_checkable("&Linear", [&widget](auto&) {
        widget.set_mag_filter(GL_LINEAR);
    });

    texture_mag_filter_actions.add_action(*texture_mag_filter_nearest_action);
    texture_mag_filter_actions.add_action(*texture_mag_filter_linear_action);

    texture_mag_filter_menu.add_action(*texture_mag_filter_nearest_action);
    texture_mag_filter_menu.add_action(*texture_mag_filter_linear_action);

    texture_mag_filter_nearest_action->set_checked(true);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("3D File Viewer", app_icon, window));

    window->show();

    auto filename = arguments.argc > 1 ? arguments.argv[1] : "/home/anon/Documents/3D Models/teapot.obj";
    if (widget.load_path(filename)) {
        auto canonical_path = Core::File::absolute_path(filename);
        window->set_title(String::formatted("{} - 3D File Viewer", canonical_path));
    }

    return app->exec();
}
