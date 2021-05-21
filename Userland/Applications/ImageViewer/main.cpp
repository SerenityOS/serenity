/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ViewWidget.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MimeData.h>
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
#include <serenity.h>
#include <stdio.h>
#include <string.h>

using namespace ImageViewer;

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath wpath cpath unix thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (!Desktop::Launcher::add_allowed_handler_with_any_url("/bin/ImageViewer")) {
        warnln("Failed to set up allowed launch URLs");
        return 1;
    }

    if (!Desktop::Launcher::add_allowed_handler_with_only_specific_urls(
            "/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man1/ImageViewer.md") })) {
        warnln("Failed to set up allowed launch URLs");
        return 1;
    }

    if (!Desktop::Launcher::seal_allowlist()) {
        warnln("Failed to seal allowed launch URLs");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("filetype-image");

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The image file to be displayed.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto window = GUI::Window::construct();
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
    widget.on_scale_change = [&](int scale, Gfx::IntRect rect) {
        if (!widget.bitmap()) {
            window->set_title("Image Viewer");
            return;
        }

        window->set_title(String::formatted("{} {} {}% - Image Viewer", widget.path(), widget.bitmap()->size().to_string(), scale));

        if (window->is_fullscreen())
            return;

        if (window->is_maximized())
            return;

        auto w = max(window->width(), rect.width() + 4);
        auto h = max(window->height(), rect.height() + widget.toolbar_height() + 6);
        window->resize(w, h);
    };
    widget.on_drop = [&](auto& event) {
        window->move_to_front();

        if (!event.mime_data().has_urls())
            return;

        auto urls = event.mime_data().urls();

        if (urls.is_empty())
            return;

        widget.load_from_file(urls.first().path());

        for (size_t i = 1; i < urls.size(); ++i) {
            Desktop::Launcher::open(URL::create_with_file_protocol(urls[i].path().characters()), "/bin/ImageViewer");
        }
    };
    widget.on_doubleclick = [&] {
        window->set_fullscreen(!window->is_fullscreen());
        toolbar_container.set_visible(!window->is_fullscreen());
    };

    // Actions
    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            auto path = GUI::FilePicker::get_open_filepath(window, "Open Image");
            if (path.has_value()) {
                widget.load_from_file(path.value());
            }
        });

    auto delete_action = GUI::CommonActions::make_delete_action(
        [&](auto&) {
            auto path = widget.path();
            if (path.is_empty())
                return;

            auto msgbox_result = GUI::MessageBox::show(window,
                String::formatted("Really delete {}?", path),
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

    auto go_first_action = GUI::Action::create("&Go to First", { Mod_None, Key_Home }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-first.png"),
        [&](auto&) {
            widget.navigate(ViewWidget::Directions::First);
        });

    auto go_back_action = GUI::Action::create("Go &Back", { Mod_None, Key_Left }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"),
        [&](auto&) {
            widget.navigate(ViewWidget::Directions::Back);
        });

    auto go_forward_action = GUI::Action::create("Go &Forward", { Mod_None, Key_Right }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"),
        [&](auto&) {
            widget.navigate(ViewWidget::Directions::Forward);
        });

    auto go_last_action = GUI::Action::create("Go to &Last", { Mod_None, Key_End }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-last.png"),
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
        delete_action->set_enabled(should_enable_image_actions);
        rotate_left_action->set_enabled(should_enable_image_actions);
        rotate_right_action->set_enabled(should_enable_image_actions);
        vertical_flip_action->set_enabled(should_enable_image_actions);
        horizontal_flip_action->set_enabled(should_enable_image_actions);
        desktop_wallpaper_action->set_enabled(should_enable_image_actions);
        go_first_action->set_enabled(should_enable_image_actions);
        go_back_action->set_enabled(should_enable_image_actions);
        go_forward_action->set_enabled(should_enable_image_actions);
        go_last_action->set_enabled(should_enable_image_actions);
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

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(open_action);
    file_menu.add_action(delete_action);
    file_menu.add_separator();
    file_menu.add_action(quit_action);

    auto& image_menu = menubar->add_menu("&Image");
    image_menu.add_action(rotate_left_action);
    image_menu.add_action(rotate_right_action);
    image_menu.add_action(vertical_flip_action);
    image_menu.add_action(horizontal_flip_action);
    image_menu.add_separator();
    image_menu.add_action(desktop_wallpaper_action);

    auto& navigate_menu = menubar->add_menu("&Navigate");
    navigate_menu.add_action(go_first_action);
    navigate_menu.add_action(go_back_action);
    navigate_menu.add_action(go_forward_action);
    navigate_menu.add_action(go_last_action);

    auto& view_menu = menubar->add_menu("&View");
    view_menu.add_action(full_screen_action);
    view_menu.add_separator();
    view_menu.add_action(zoom_in_action);
    view_menu.add_action(reset_zoom_action);
    view_menu.add_action(zoom_out_action);
    view_menu.add_separator();
    view_menu.add_action(hide_show_toolbar_action);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/ImageViewer.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Image Viewer", app_icon, window));

    window->set_menubar(move(menubar));

    if (path != nullptr) {
        widget.load_from_file(path);
    } else {
        widget.clear();
    }

    window->show();

    return app->exec();
}
