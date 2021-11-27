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

    auto& root_widget = window->set_main_widget<GUI::Widget>();
    root_widget.set_fill_with_background_color(true);
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.layout()->set_spacing(2);

    auto& toolbar_container = root_widget.add<GUI::ToolbarContainer>();
    auto& main_toolbar = toolbar_container.add<GUI::Toolbar>();

    auto& widget = root_widget.add<ViewWidget>();
    if (path) {
        widget.set_path(path);
    }
    widget.on_scale_change = [&](int scale) {
        if (!widget.bitmap()) {
            window->set_title("Image Viewer");
            return;
        }

        window->set_title(String::formatted("{} {} {}% - Image Viewer", widget.path(), widget.bitmap()->size().to_string(), scale));

        if (scale == 100 && !widget.scaled_for_first_image()) {
            widget.set_scaled_for_first_image(true);
            widget.resize_window();
        }
    };
    widget.on_drop = [&](auto& event) {
        if (!event.mime_data().has_urls())
            return;

        auto urls = event.mime_data().urls();

        if (urls.is_empty())
            return;

        window->move_to_front();

        auto path = urls.first().path();
        widget.set_path(path);
        widget.load_from_file(path);

        for (size_t i = 1; i < urls.size(); ++i) {
            Desktop::Launcher::open(URL::create_with_file_protocol(urls[i].path().characters()), "/bin/ImageViewer");
        }
    };
    widget.on_doubleclick = [&] {
        window->set_fullscreen(!window->is_fullscreen());
        toolbar_container.set_visible(!window->is_fullscreen());
        widget.set_frame_thickness(window->is_fullscreen() ? 0 : 2);
    };

    // Actions
    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            auto path = GUI::FilePicker::get_open_filepath(window, "Open Image");
            if (path.has_value()) {
                widget.set_path(path.value());
                widget.load_from_file(path.value());
            }
        });

    auto delete_action = GUI::CommonActions::make_delete_action(
        [&](auto&) {
            auto path = widget.path();
            if (path.is_empty())
                return;

            auto msgbox_result = GUI::MessageBox::show(window,
                String::formatted("Are you sure you want to delete {}?", path),
                "Confirm deletion",
                GUI::MessageBox::Type::Warning,
                GUI::MessageBox::InputType::OKCancel);

            if (msgbox_result == GUI::MessageBox::ExecCancel)
                return;

            if (unlink(widget.path().characters()) < 0) {
                int saved_errno = errno;
                GUI::MessageBox::show(window,
                    String::formatted("unlink({}) failed: {}", path, strerror(saved_errno)),
                    "Delete failed",
                    GUI::MessageBox::Type::Error);

                return;
            }

            widget.clear();
        });

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        });

    auto rotate_left_action = GUI::Action::create("Rotate &Left", { Mod_None, Key_L },
        [&](auto&) {
            widget.rotate(Gfx::RotationDirection::CounterClockwise);
        });

    auto rotate_right_action = GUI::Action::create("Rotate &Right", { Mod_None, Key_R },
        [&](auto&) {
            widget.rotate(Gfx::RotationDirection::Clockwise);
        });

    auto vertical_flip_action = GUI::Action::create("Flip &Vertically", { Mod_None, Key_V },
        [&](auto&) {
            widget.flip(Gfx::Orientation::Vertical);
        });

    auto horizontal_flip_action = GUI::Action::create("Flip &Horizontally", { Mod_None, Key_H },
        [&](auto&) {
            widget.flip(Gfx::Orientation::Horizontal);
        });

    auto desktop_wallpaper_action = GUI::Action::create("Set as Desktop &Wallpaper",
        [&](auto&) {
            GUI::Desktop::the().set_wallpaper(widget.path());
        });

    auto go_first_action = GUI::Action::create("&Go to First", { Mod_None, Key_Home }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-first.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            widget.navigate(ViewWidget::Directions::First);
        });

    auto go_back_action = GUI::Action::create("Go &Back", { Mod_None, Key_Left }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            widget.navigate(ViewWidget::Directions::Back);
        });

    auto go_forward_action = GUI::Action::create("Go &Forward", { Mod_None, Key_Right }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            widget.navigate(ViewWidget::Directions::Forward);
        });

    auto go_last_action = GUI::Action::create("Go to &Last", { Mod_None, Key_End }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-last.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            widget.navigate(ViewWidget::Directions::Last);
        });

    auto full_screen_action = GUI::CommonActions::make_fullscreen_action(
        [&](auto&) {
            widget.on_doubleclick();
        });

    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            widget.set_scale(widget.scale() + 10);
        },
        window);

    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            widget.set_scale(100);
        },
        window);

    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            widget.set_scale(widget.scale() - 10);
        },
        window);

    auto hide_show_toolbar_action = GUI::Action::create("Hide/Show &Toolbar", { Mod_Ctrl, Key_T },
        [&](auto&) {
            toolbar_container.set_visible(!toolbar_container.is_visible());
        });

    auto copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        if (widget.bitmap())
            GUI::Clipboard::the().set_bitmap(*widget.bitmap());
    });
    widget.on_image_change = [&](const Gfx::Bitmap* bitmap) {
        bool should_enable_image_actions = (bitmap != nullptr);
        bool should_enable_forward_actions = (widget.is_next_available() && should_enable_image_actions);
        bool should_enable_backward_actions = (widget.is_previous_available() && should_enable_image_actions);
        delete_action->set_enabled(should_enable_image_actions);
        rotate_left_action->set_enabled(should_enable_image_actions);
        rotate_right_action->set_enabled(should_enable_image_actions);
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

    main_toolbar.add_action(open_action);
    main_toolbar.add_action(delete_action);
    main_toolbar.add_separator();
    main_toolbar.add_action(go_first_action);
    main_toolbar.add_action(go_back_action);
    main_toolbar.add_action(go_forward_action);
    main_toolbar.add_action(go_last_action);
    main_toolbar.add_separator();
    main_toolbar.add_action(zoom_in_action);
    main_toolbar.add_action(reset_zoom_action);
    main_toolbar.add_action(zoom_out_action);

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(open_action);
    file_menu.add_action(delete_action);
    file_menu.add_separator();
    file_menu.add_action(quit_action);

    auto& image_menu = window->add_menu("&Image");
    image_menu.add_action(rotate_left_action);
    image_menu.add_action(rotate_right_action);
    image_menu.add_action(vertical_flip_action);
    image_menu.add_action(horizontal_flip_action);
    image_menu.add_separator();
    image_menu.add_action(desktop_wallpaper_action);

    auto& navigate_menu = window->add_menu("&Navigate");
    navigate_menu.add_action(go_first_action);
    navigate_menu.add_action(go_back_action);
    navigate_menu.add_action(go_forward_action);
    navigate_menu.add_action(go_last_action);

    auto& view_menu = window->add_menu("&View");
    view_menu.add_action(full_screen_action);
    view_menu.add_separator();
    view_menu.add_action(zoom_in_action);
    view_menu.add_action(reset_zoom_action);
    view_menu.add_action(zoom_out_action);
    view_menu.add_separator();
    view_menu.add_action(hide_show_toolbar_action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/ImageViewer.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Image Viewer", app_icon, window));

    if (path != nullptr) {
        widget.load_from_file(path);
    } else {
        widget.clear();
    }

    window->show();

    return app->exec();
}
