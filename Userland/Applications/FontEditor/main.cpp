/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontEditor.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/FontDatabase.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd thread rpath unix cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd thread rpath unix cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!Desktop::Launcher::add_allowed_handler_with_only_specific_urls(
            "/bin/Help",
            { URL::create_with_file_protocol("/usr/share/man/man1/FontEditor.md") })
        || !Desktop::Launcher::seal_allowlist()) {
        warnln("Failed to set up allowed launch URLs");
        return 1;
    }

    if (pledge("stdio recvfd sendfd thread rpath cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The font file for editing.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    RefPtr<Gfx::BitmapFont> edited_font;
    if (path == nullptr) {
        path = "Untitled.font";
        edited_font = static_ptr_cast<Gfx::BitmapFont>(Gfx::FontDatabase::default_font().clone());
    } else {
        auto bitmap_font = Gfx::BitmapFont::load_from_file(path);
        if (!bitmap_font) {
            String message = String::formatted("Couldn't load font: {}\n", path);
            GUI::MessageBox::show(nullptr, message, "Font Editor", GUI::MessageBox::Type::Error);
            return 1;
        }
        edited_font = static_ptr_cast<Gfx::BitmapFont>(bitmap_font->clone());
        if (!edited_font) {
            String message = String::formatted("Couldn't load font: {}\n", path);
            GUI::MessageBox::show(nullptr, message, "Font Editor", GUI::MessageBox::Type::Error);
            return 1;
        }
    }

    auto app_icon = GUI::Icon::default_icon("app-font-editor");

    auto window = GUI::Window::construct();
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(440, 470);

    auto& font_editor = window->set_main_widget<FontEditorWidget>(path, move(edited_font));
    font_editor.update_title();

    auto menubar = GUI::Menubar::construct();
    font_editor.initialize_menubar(menubar);
    window->set_menubar(move(menubar));

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (font_editor.request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    return app->exec();
}
