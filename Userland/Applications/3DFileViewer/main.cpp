/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Conrad Pankoff <deoxxa@fknsrs.biz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibGL/GL/gl.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ThreeDeeModelWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibThreeDee/Mesh.h>
#include <LibThreeDee/WavefrontOBJLoader.h>
#include <unistd.h>

static constexpr u16 RENDER_WIDTH = 640;
static constexpr u16 RENDER_HEIGHT = 480;

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
    window->resize(RENDER_WIDTH + 4, RENDER_HEIGHT + 4);
    window->set_resizable(false);
    window->set_double_buffering_enabled(true);

    auto& widget = window->set_main_widget<GUI::ThreeDeeModelWidget>();
    widget.set_render_width_and_height(RENDER_WIDTH, RENDER_HEIGHT);

    float rotation_speed = 30;
    bool rotate_x = true;
    bool rotate_y = false;
    bool rotate_z = true;
    bool show_frame_rate = true;

    auto& time = widget.add<GUI::Label>();
    time.set_visible(show_frame_rate);
    time.set_foreground_role(ColorRole::HoverHighlight);
    time.set_relative_rect({ 0, 8, 86, 10 });
    time.move_by({ window->width() - time.width(), 0 });

    Gfx::IntPoint last_mouse_position;

    widget.on_mousemove = [&widget, &last_mouse_position](GUI::MouseEvent& event) {
        if (event.buttons() == GUI::MouseButton::Left) {
            int delta_x = last_mouse_position.x() - event.x();
            int delta_y = last_mouse_position.y() - event.y();

            widget.set_rotation_angle_x(widget.rotation_angle_x() - delta_y / 2.0f);
            widget.set_rotation_angle_y(widget.rotation_angle_y() - delta_x / 2.0f);
        }

        last_mouse_position = event.position();
    };

    widget.on_mousewheel = [&widget](GUI::MouseEvent& event) {
        if (event.wheel_delta() > 0)
            widget.set_zoom(widget.zoom() / 1.1f);
        else
            widget.set_zoom(widget.zoom() * 1.1f);
    };

    auto timer = Core::Timer::create_repeating(20, [&]() {
        if (rotate_x && rotation_speed != 0)
            widget.set_rotation_angle_x(widget.rotation_angle_x() + rotation_speed * 0.01f);
        if (rotate_y && rotation_speed != 0)
            widget.set_rotation_angle_y(widget.rotation_angle_y() + rotation_speed * 0.01f);
        if (rotate_z && rotation_speed != 0)
            widget.set_rotation_angle_z(widget.rotation_angle_z() + rotation_speed * 0.01f);

        if (show_frame_rate)
            time.set_text(String::formatted("FPS: {}", widget.frame_rate()));
    });
    timer->start();

    auto& file_menu = window->add_menu("&File");

    auto load_model = [&](StringView const& filename) {
        auto file = Core::File::construct(filename);

        if (!file->filename().ends_with(".obj")) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: invalid file type", filename), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        if (!file->open(Core::OpenMode::ReadOnly) && file->error() != ENOENT) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: {}", filename, strerror(errno)), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        if (file->is_device()) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: Can't open device files", filename), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        if (file->is_directory()) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: Can't open directories", filename), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        ThreeDee::WavefrontOBJLoader loader;

        auto mesh = loader.load(file);
        if (mesh.is_null()) {
            GUI::MessageBox::show(window, String::formatted("Reading \"{}\" failed.", filename), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        dbgln("3DFileViewer: mesh has {} triangles.", mesh->triangle_count());

        // Determine whether or not a texture for this model resides within the same directory
        StringBuilder builder;
        builder.append(filename.split_view('.').at(0));
        builder.append(".bmp");

        // Attempt to open the texture file from disk
        auto texture_image = Gfx::Bitmap::try_load_from_file(builder.string_view());
        if (texture_image) {
            dbgln("3DFileViewer: loaded texture from {}.", builder.string_view());
        }

        if (texture_image)
            widget.set_mesh_and_texture(mesh, texture_image);
        else
            widget.set_mesh(mesh);

        auto canonical_path = Core::File::real_path_for(filename);

        window->set_title(String::formatted("{} - 3D File Viewer", canonical_path));
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

    auto& view_menu = window->add_menu("&View");
    view_menu.add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto& rotation_axis_menu = view_menu.add_submenu("Rotation &Axis");
    auto rotation_x_action = GUI::Action::create_checkable("&X", [&rotate_x](auto&) {
        rotate_x = !rotate_x;
    });
    auto rotation_y_action = GUI::Action::create_checkable("&Y", [&rotate_y](auto&) {
        rotate_y = !rotate_y;
    });
    auto rotation_z_action = GUI::Action::create_checkable("&Z", [&rotate_z](auto&) {
        rotate_z = !rotate_z;
    });

    rotation_axis_menu.add_action(*rotation_x_action);
    rotation_axis_menu.add_action(*rotation_y_action);
    rotation_axis_menu.add_action(*rotation_z_action);

    rotation_x_action->set_checked(rotate_x);
    rotation_y_action->set_checked(rotate_y);
    rotation_z_action->set_checked(rotate_z);

    auto& rotation_speed_menu = view_menu.add_submenu("Rotation &Speed");
    GUI::ActionGroup rotation_speed_actions;
    rotation_speed_actions.set_exclusive(true);

    auto no_rotation_action = GUI::Action::create_checkable("N&o Rotation", [&rotation_speed](auto&) {
        rotation_speed = 0;
    });
    auto slow_rotation_action = GUI::Action::create_checkable("&Slow", [&rotation_speed](auto&) {
        rotation_speed = 30;
    });
    auto normal_rotation_action = GUI::Action::create_checkable("&Normal", [&rotation_speed](auto&) {
        rotation_speed = 60;
    });
    auto fast_rotation_action = GUI::Action::create_checkable("&Fast", [&rotation_speed](auto&) {
        rotation_speed = 90;
    });

    rotation_speed_actions.add_action(*no_rotation_action);
    rotation_speed_actions.add_action(*slow_rotation_action);
    rotation_speed_actions.add_action(*normal_rotation_action);
    rotation_speed_actions.add_action(*fast_rotation_action);

    rotation_speed_menu.add_action(*no_rotation_action);
    rotation_speed_menu.add_action(*slow_rotation_action);
    rotation_speed_menu.add_action(*normal_rotation_action);
    rotation_speed_menu.add_action(*fast_rotation_action);

    no_rotation_action->set_checked(rotation_speed == 0);
    slow_rotation_action->set_checked(rotation_speed == 30);
    normal_rotation_action->set_checked(rotation_speed == 60);
    fast_rotation_action->set_checked(rotation_speed == 90);

    auto show_frame_rate_action = GUI::Action::create_checkable("Show Frame &Rate", [&show_frame_rate, &time](auto&) {
        show_frame_rate = !show_frame_rate;
        time.set_visible(show_frame_rate);
    });

    view_menu.add_action(*show_frame_rate_action);

    show_frame_rate_action->set_checked(show_frame_rate);

    auto& texture_menu = window->add_menu("&Texture");

    auto& wrap_s_menu = texture_menu.add_submenu("Wrap &S");
    GUI::ActionGroup wrap_s_actions;
    wrap_s_actions.set_exclusive(true);

    auto wrap_s_repeat_action = GUI::Action::create_checkable("&Repeat", [&widget](auto&) {
        widget.set_wrap_s_mode(GL_REPEAT);
    });
    auto wrap_s_mirrored_repeat_action = GUI::Action::create_checkable("&Mirrored Repeat", [&widget](auto&) {
        widget.set_wrap_s_mode(GL_MIRRORED_REPEAT);
    });
    auto wrap_s_clamp_action = GUI::Action::create_checkable("&Clamp", [&widget](auto&) {
        widget.set_wrap_s_mode(GL_CLAMP);
    });

    wrap_s_actions.add_action(*wrap_s_repeat_action);
    wrap_s_actions.add_action(*wrap_s_mirrored_repeat_action);
    wrap_s_actions.add_action(*wrap_s_clamp_action);

    wrap_s_menu.add_action(*wrap_s_repeat_action);
    wrap_s_menu.add_action(*wrap_s_mirrored_repeat_action);
    wrap_s_menu.add_action(*wrap_s_clamp_action);

    wrap_s_repeat_action->set_checked(widget.wrap_s_mode() == GL_REPEAT);
    wrap_s_mirrored_repeat_action->set_checked(widget.wrap_s_mode() == GL_MIRRORED_REPEAT);
    wrap_s_clamp_action->set_checked(widget.wrap_s_mode() == GL_CLAMP);

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

    wrap_t_repeat_action->set_checked(widget.wrap_t_mode() == GL_REPEAT);
    wrap_t_mirrored_repeat_action->set_checked(widget.wrap_t_mode() == GL_MIRRORED_REPEAT);
    wrap_t_clamp_action->set_checked(widget.wrap_t_mode() == GL_CLAMP);

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

    texture_scale_025_action->set_checked(widget.texture_scale() == 0.25f);
    texture_scale_05_action->set_checked(widget.texture_scale() == 0.5f);
    texture_scale_1_action->set_checked(widget.texture_scale() == 1.f);
    texture_scale_2_action->set_checked(widget.texture_scale() == 2.f);
    texture_scale_4_action->set_checked(widget.texture_scale() == 4.f);

    auto& mag_filter_menu = texture_menu.add_submenu("Mag Filter");
    GUI::ActionGroup mag_filter_actions;
    mag_filter_actions.set_exclusive(true);

    auto mag_filter_nearest_action = GUI::Action::create_checkable("&Nearest", [&widget](auto&) {
        widget.set_mag_filter(GL_NEAREST);
    });

    auto mag_filter_linear_action = GUI::Action::create_checkable("&Linear", [&widget](auto&) {
        widget.set_mag_filter(GL_LINEAR);
    });

    mag_filter_actions.add_action(*mag_filter_nearest_action);
    mag_filter_actions.add_action(*mag_filter_linear_action);

    mag_filter_menu.add_action(*mag_filter_nearest_action);
    mag_filter_menu.add_action(*mag_filter_linear_action);

    mag_filter_nearest_action->set_checked(widget.mag_filter() == GL_NEAREST);
    mag_filter_linear_action->set_checked(widget.mag_filter() == GL_LINEAR);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("3D File Viewer", app_icon, window));

    window->show();

    auto filename = argc > 1 ? argv[1] : "/home/anon/Documents/3D Models/teapot.obj";
    load_model(filename);

    return app->exec();
}
