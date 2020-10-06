/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "QSWidget.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MimeData.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <string.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept cpath rpath wpath unix cpath fattr proc exec thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept cpath rpath wpath proc exec thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The image file to be displayed.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(true);
    window->resize(300, 200);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-image.png"));
    window->set_title("QuickShow");

    auto& root_widget = window->set_main_widget<GUI::Widget>();
    root_widget.set_fill_with_background_color(true);
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.layout()->set_spacing(2);

    auto& toolbar_container = root_widget.add<GUI::ToolBarContainer>();
    auto& main_toolbar = toolbar_container.add<GUI::ToolBar>();

    auto& widget = root_widget.add<QSWidget>();
    widget.on_scale_change = [&](int scale, Gfx::IntRect rect) {
        if (!widget.bitmap()) {
            window->set_title("QuickShow");
            return;
        }

        window->set_title(String::formatted("{} {} {}% - QuickShow", widget.path(), widget.bitmap()->size().to_string(), scale));

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

        if (event.mime_data().has_urls()) {
            auto urls = event.mime_data().urls();

            if (!urls.is_empty()) {
                auto url = urls.first();
                widget.load_from_file(url.path());
            }

            pid_t child;
            for (size_t i = 1; i < urls.size(); ++i) {
                const char* argv[] = { "/bin/QuickShow", urls[i].path().characters(), nullptr };
                if ((errno = posix_spawn(&child, "/bin/QuickShow", nullptr, nullptr, const_cast<char**>(argv), environ))) {
                    perror("posix_spawn");
                } else {
                    if (disown(child) < 0)
                        perror("disown");
                }
            }
        }
    };
    widget.on_doubleclick = [&] {
        window->set_fullscreen(!window->is_fullscreen());
        toolbar_container.set_visible(!window->is_fullscreen());
    };

    // Actions
    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open image...");
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

            auto unlink_result = unlink(widget.path().characters());
            dbgln("unlink_result::{}", unlink_result);

            if (unlink_result < 0) {
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

    auto rotate_left_action = GUI::Action::create("Rotate Left", { Mod_None, Key_L },
        [&](auto&) {
            widget.rotate(Gfx::RotationDirection::Left);
        });

    auto rotate_right_action = GUI::Action::create("Rotate Right", { Mod_None, Key_R },
        [&](auto&) {
            widget.rotate(Gfx::RotationDirection::Right);
        });

    auto vertical_flip_action = GUI::Action::create("Vertical Flip", { Mod_None, Key_V },
        [&](auto&) {
            widget.flip(Gfx::Orientation::Vertical);
        });

    auto horizontal_flip_action = GUI::Action::create("Horizontal Flip", { Mod_None, Key_H },
        [&](auto&) {
            widget.flip(Gfx::Orientation::Horizontal);
        });

    auto desktop_wallpaper_action = GUI::Action::create("Set as desktop wallpaper",
        [&](auto&) {
            GUI::Desktop::the().set_wallpaper(widget.path());
        });

    auto go_first_action = GUI::Action::create("First", { Mod_None, Key_Home }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-first.png"),
        [&](auto&) {
            widget.navigate(QSWidget::Directions::First);
        });

    auto go_back_action = GUI::Action::create("Back", { Mod_None, Key_Left }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"),
        [&](auto&) {
            widget.navigate(QSWidget::Directions::Back);
        });

    auto go_forward_action = GUI::Action::create("Forward", { Mod_None, Key_Right }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"),
        [&](auto&) {
            widget.navigate(QSWidget::Directions::Forward);
        });

    auto go_last_action = GUI::Action::create("Last", { Mod_None, Key_End }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-last.png"),
        [&](auto&) {
            widget.navigate(QSWidget::Directions::Last);
        });

    auto full_sceen_action = GUI::CommonActions::make_fullscreen_action(
        [&](auto&) {
            widget.on_doubleclick();
        });

    auto zoom_in_action = GUI::Action::create("Zoom In", { Mod_None, Key_Plus }, Gfx::Bitmap::load_from_file("/res/icons/16x16/zoom-in.png"),
        [&](auto&) {
            widget.set_scale(widget.scale() + 10);
        });

    auto zoom_reset_action = GUI::Action::create("Zoom 100%", { Mod_None, Key_0 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/zoom-reset.png"),
        [&](auto&) {
            widget.set_scale(100);
        });

    auto zoom_out_action = GUI::Action::create("Zoom Out", { Mod_None, Key_Minus }, Gfx::Bitmap::load_from_file("/res/icons/16x16/zoom-out.png"),
        [&](auto&) {
            widget.set_scale(widget.scale() - 10);
        });

    auto hide_show_toolbar_action = GUI::Action::create("Hide/Show Toolbar", { Mod_Ctrl, Key_T },
        [&](auto&) {
            toolbar_container.set_visible(!toolbar_container.is_visible());
        });

    auto about_action = GUI::Action::create("About",
        [&](auto&) {
            GUI::AboutDialog::show("QuickShow", Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-image.png"), window);
        });

    auto copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        if (widget.bitmap())
            GUI::Clipboard::the().set_bitmap(*widget.bitmap());
    });

    main_toolbar.add_action(open_action);
    main_toolbar.add_action(delete_action);
    main_toolbar.add_separator();
    main_toolbar.add_action(go_first_action);
    main_toolbar.add_action(go_back_action);
    main_toolbar.add_action(go_forward_action);
    main_toolbar.add_action(go_last_action);
    main_toolbar.add_separator();
    main_toolbar.add_action(zoom_in_action);
    main_toolbar.add_action(zoom_reset_action);
    main_toolbar.add_action(zoom_out_action);

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("QuickShow");
    app_menu.add_action(open_action);
    app_menu.add_action(delete_action);
    app_menu.add_separator();
    app_menu.add_action(quit_action);

    auto& image_menu = menubar->add_menu("Image");
    image_menu.add_action(rotate_left_action);
    image_menu.add_action(rotate_right_action);
    image_menu.add_action(vertical_flip_action);
    image_menu.add_action(horizontal_flip_action);
    image_menu.add_separator();
    image_menu.add_action(desktop_wallpaper_action);

    auto& navigate_menu = menubar->add_menu("Navigate");
    navigate_menu.add_action(go_first_action);
    navigate_menu.add_action(go_back_action);
    navigate_menu.add_action(go_forward_action);
    navigate_menu.add_action(go_last_action);

    auto& view_menu = menubar->add_menu("View");
    view_menu.add_action(full_sceen_action);
    view_menu.add_separator();
    view_menu.add_action(zoom_in_action);
    view_menu.add_action(zoom_reset_action);
    view_menu.add_action(zoom_out_action);
    view_menu.add_separator();
    view_menu.add_action(hide_show_toolbar_action);

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(about_action);

    app->set_menubar(move(menubar));

    if (path != nullptr) {
        widget.load_from_file(path);
    }

    window->show();

    return app->exec();
}
