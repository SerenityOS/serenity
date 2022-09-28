/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
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
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath unix cpath wpath"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/FontEditor.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    Config::pledge_domain("FontEditor");
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath cpath wpath"));

    char const* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The font file for editing.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-font-editor"sv));

    auto window = TRY(GUI::Window::try_create());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(640, 470);

    auto font_editor = TRY(window->try_set_main_widget<FontEditor::MainWidget>());
    TRY(font_editor->initialize_menubar(*window));

    if (path) {
        TRY(font_editor->open_file(path));
    } else {
        auto mutable_font = TRY(TRY(Gfx::BitmapFont::try_load_from_file("/res/fonts/KaticaRegular10.font"))->unmasked_character_set());
        TRY(font_editor->initialize({}, move(mutable_font)));
    }

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (font_editor->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    return app->exec();
}
