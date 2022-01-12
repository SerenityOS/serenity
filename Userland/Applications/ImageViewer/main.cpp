/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mohsan Ali <mohsan0073@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ViewWidget.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <LibMain/Main.h>
#include <serenity.h>
#include <string.h>

using namespace ImageViewer;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath unix thread"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Desktop::Launcher::add_allowed_handler_with_any_url("/bin/ImageViewer"));
    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man1/ImageViewer.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    auto app_icon = GUI::Icon::default_icon("filetype-image");

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The image file to be displayed.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(true);
    window->resize(300, 200);
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Image Viewer");

    auto root_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    root_widget->set_fill_with_background_color(true);
    root_widget->set_layout<GUI::VerticalBoxLayout>();
    root_widget->layout()->set_spacing(2);

    auto toolbar_container = TRY(root_widget->try_add<GUI::ToolbarContainer>());
    auto main_toolbar = TRY(toolbar_container->try_add<GUI::Toolbar>());

    auto widget = TRY(root_widget->try_add<ViewWidget>());
    if (path) {
        widget->set_path(path);
    }
    widget->on_scale_change = [&](float scale) {
        if (!widget->bitmap()) {
            window->set_title("Image Viewer");
            return;
        }

        window->set_title(String::formatted("{} {} {}% - Image Viewer", widget->path(), widget->bitmap()->size().to_string(), (int)(scale * 100)));

        if (scale == 100 && !widget->scaled_for_first_image()) {
            widget->set_scaled_for_first_image(true);
            widget->resize_window();
        }
    };
    widget->on_drop = [&](auto& event) {
        if (!event.mime_data().has_urls())
            return;

        auto urls = event.mime_data().urls();

        if (urls.is_empty())
            return;

        window->move_to_front();

        auto path = urls.first().path();
        widget->set_path(path);
        widget->load_from_file(path);

        for (size_t i = 1; i < urls.size(); ++i) {
            Desktop::Launcher::open(URL::create_with_file_protocol(urls[i].path().characters()), "/bin/ImageViewer");
        }
    };
    widget->on_doubleclick = [&] {
        window->set_fullscreen(!window->is_fullscreen());
        toolbar_container->set_visible(!window->is_fullscreen());
        widget->set_frame_thickness(window->is_fullscreen() ? 0 : 2);
    };

    // Actions
    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            auto path = GUI::FilePicker::get_open_filepath(window, "Open Image");
            if (path.has_value()) {
                widget->set_path(path.value());
                widget->load_from_file(path.value());
            }
        });

    auto delete_action = GUI::CommonActions::make_delete_action(
        [&](auto&) {
            auto path = widget->path();
            if (path.is_empty())
                return;

            auto msgbox_result = GUI::MessageBox::show(window,
                String::formatted("Are you sure you want to delete {}?", path),
                "Confirm deletion",
                GUI::MessageBox::Type::Warning,
                GUI::MessageBox::InputType::OKCancel);

            if (msgbox_result == GUI::MessageBox::ExecCancel)
                return;

            auto unlinked_or_error = Core::System::unlink(widget->path());
            if (unlinked_or_error.is_error()) {
                GUI::MessageBox::show(window,
                    String::formatted("unlink({}) failed: {}", path, unlinked_or_error.error()),
                    "Delete failed",
                    GUI::MessageBox::Type::Error);

                return;
            }

            widget->clear();
        });

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        });

    auto rotate_counterclockwise_action = GUI::CommonActions::make_rotate_counterclockwise_action([&](auto&) {
        widget->rotate(Gfx::RotationDirection::CounterClockwise);
    });

    auto rotate_clockwise_action = GUI::CommonActions::make_rotate_clockwise_action([&](auto&) {
        widget->rotate(Gfx::RotationDirection::Clockwise);
    });

    auto vertical_flip_action = GUI::Action::create("Flip &Vertically", { Mod_None, Key_V }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-flip-vertical.png")),
        [&](auto&) {
            widget->flip(Gfx::Orientation::Vertical);
        });

    auto horizontal_flip_action = GUI::Action::create("Flip &Horizontally", { Mod_None, Key_H }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-flip-horizontal.png")),
        [&](auto&) {
            widget->flip(Gfx::Orientation::Horizontal);
        });

    auto desktop_wallpaper_action = GUI::Action::create("Set as Desktop &Wallpaper", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-display-settings.png")),
        [&](auto&) {
            auto could_set_wallpaper = GUI::Desktop::the().set_wallpaper(widget->path());
            if (!could_set_wallpaper) {
                GUI::MessageBox::show(window,
                    String::formatted("set_wallpaper({}) failed", widget->path()),
                    "Could not set wallpaper",
                    GUI::MessageBox::Type::Error);
            }
        });

    auto go_first_action = GUI::Action::create("&Go to First", { Mod_None, Key_Home }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-first.png")),
        [&](auto&) {
            widget->navigate(ViewWidget::Directions::First);
        });

    auto go_back_action = GUI::Action::create("Go &Back", { Mod_None, Key_Left }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png")),
        [&](auto&) {
            widget->navigate(ViewWidget::Directions::Back);
        });

    auto go_forward_action = GUI::Action::create("Go &Forward", { Mod_None, Key_Right }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png")),
        [&](auto&) {
            widget->navigate(ViewWidget::Directions::Forward);
        });

    auto go_last_action = GUI::Action::create("Go to &Last", { Mod_None, Key_End }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-last.png")),
        [&](auto&) {
            widget->navigate(ViewWidget::Directions::Last);
        });

    auto full_screen_action = GUI::CommonActions::make_fullscreen_action(
        [&](auto&) {
            widget->on_doubleclick();
        });

    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            widget->set_scale(widget->scale() * 1.44f);
        },
        window);

    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            widget->set_scale(1.f);
        },
        window);

    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            widget->set_scale(widget->scale() / 1.44f);
        },
        window);

    auto hide_show_toolbar_action = GUI::Action::create("Hide/Show &Toolbar", { Mod_Ctrl, Key_T },
        [&](auto&) {
            toolbar_container->set_visible(!toolbar_container->is_visible());
        });

    auto copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        if (widget->bitmap())
            GUI::Clipboard::the().set_bitmap(*widget->bitmap());
    });

    auto nearest_neighbor_action = GUI::Action::create_checkable("&Nearest Neighbor", [&](auto&) {
        widget->set_scaling_mode(Gfx::Painter::ScalingMode::NearestNeighbor);
    });
    nearest_neighbor_action->set_checked(true);

    auto bilinear_action = GUI::Action::create_checkable("&Bilinear", [&](auto&) {
        widget->set_scaling_mode(Gfx::Painter::ScalingMode::BilinearBlend);
    });

    widget->on_image_change = [&](const Gfx::Bitmap* bitmap) {
        bool should_enable_image_actions = (bitmap != nullptr);
        bool should_enable_forward_actions = (widget->is_next_available() && should_enable_image_actions);
        bool should_enable_backward_actions = (widget->is_previous_available() && should_enable_image_actions);
        delete_action->set_enabled(should_enable_image_actions);
        rotate_counterclockwise_action->set_enabled(should_enable_image_actions);
        rotate_clockwise_action->set_enabled(should_enable_image_actions);
        vertical_flip_action->set_enabled(should_enable_image_actions);
        horizontal_flip_action->set_enabled(should_enable_image_actions);
        desktop_wallpaper_action->set_enabled(should_enable_image_actions);

        go_first_action->set_enabled(should_enable_backward_actions);
        go_back_action->set_enabled(should_enable_backward_actions);
        go_forward_action->set_enabled(should_enable_forward_actions);
        go_last_action->set_enabled(should_enable_forward_actions);

        zoom_in_action->set_enabled(should_enable_image_actions);
        reset_zoom_action->set_enabled(should_enable_image_actions);
        zoom_out_action->set_enabled(should_enable_image_actions);
        if (!should_enable_image_actions) {
            window->set_title("Image Viewer");
        }
    };

    (void)TRY(main_toolbar->try_add_action(open_action));
    (void)TRY(main_toolbar->try_add_action(delete_action));
    (void)TRY(main_toolbar->try_add_separator());
    (void)TRY(main_toolbar->try_add_action(go_first_action));
    (void)TRY(main_toolbar->try_add_action(go_back_action));
    (void)TRY(main_toolbar->try_add_action(go_forward_action));
    (void)TRY(main_toolbar->try_add_action(go_last_action));
    (void)TRY(main_toolbar->try_add_separator());
    (void)TRY(main_toolbar->try_add_action(zoom_in_action));
    (void)TRY(main_toolbar->try_add_action(reset_zoom_action));
    (void)TRY(main_toolbar->try_add_action(zoom_out_action));

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(open_action));
    TRY(file_menu->try_add_action(delete_action));
    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(quit_action));

    auto image_menu = TRY(window->try_add_menu("&Image"));
    TRY(image_menu->try_add_action(rotate_counterclockwise_action));
    TRY(image_menu->try_add_action(rotate_clockwise_action));
    TRY(image_menu->try_add_action(vertical_flip_action));
    TRY(image_menu->try_add_action(horizontal_flip_action));
    TRY(image_menu->try_add_separator());
    TRY(image_menu->try_add_action(desktop_wallpaper_action));

    auto navigate_menu = TRY(window->try_add_menu("&Navigate"));
    TRY(navigate_menu->try_add_action(go_first_action));
    TRY(navigate_menu->try_add_action(go_back_action));
    TRY(navigate_menu->try_add_action(go_forward_action));
    TRY(navigate_menu->try_add_action(go_last_action));

    auto view_menu = TRY(window->try_add_menu("&View"));
    TRY(view_menu->try_add_action(full_screen_action));
    TRY(view_menu->try_add_separator());
    TRY(view_menu->try_add_action(zoom_in_action));
    TRY(view_menu->try_add_action(reset_zoom_action));
    TRY(view_menu->try_add_action(zoom_out_action));
    TRY(view_menu->try_add_separator());

    auto scaling_mode_menu = TRY(view_menu->try_add_submenu("&Scaling Mode"));

    auto scaling_mode_group = make<GUI::ActionGroup>();
    scaling_mode_group->set_exclusive(true);
    scaling_mode_group->add_action(*nearest_neighbor_action);
    scaling_mode_group->add_action(*bilinear_action);

    TRY(scaling_mode_menu->try_add_action(nearest_neighbor_action));
    TRY(scaling_mode_menu->try_add_action(bilinear_action));

    TRY(view_menu->try_add_separator());
    TRY(view_menu->try_add_action(hide_show_toolbar_action));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/ImageViewer.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Image Viewer", app_icon, window)));

    if (path != nullptr) {
        widget->load_from_file(path);
    } else {
        widget->clear();
    }

    window->show();

    return app->exec();
}
