/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontEditor.h"
#include <AK/URL.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/FontDatabase.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath unix cpath wpath"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man1/FontEditor.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    Config::pledge_domains("FontEditor");
    Config::monitor_domain("FontEditor");
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath cpath wpath"));

    char const* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The font file for editing.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app_icon = GUI::Icon::default_icon("app-font-editor");

    auto window = TRY(GUI::Window::try_create());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(440, 470);

    auto font_editor = TRY(window->try_set_main_widget<FontEditorWidget>());
    font_editor->initialize_menubar(*window);

    if (path) {
        auto success = font_editor->open_file(path);
        if (!success)
            return 1;
    } else {
        auto mutable_font = static_ptr_cast<Gfx::BitmapFont>(Gfx::FontDatabase::default_font().clone())->unmasked_character_set();
        font_editor->initialize({}, move(mutable_font));
    }

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (font_editor->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    return app->exec();
}
