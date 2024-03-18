/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath unix cpath wpath"));

    auto app = TRY(GUI::Application::create(arguments));
    app->set_config_domain("FontEditor"_string);

    FontEditor::g_resources = FontEditor::Resources::create();

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/Applications/FontEditor.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    Config::pledge_domain("FontEditor");

    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView path;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The font file for editing.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-font-editor"sv));

    auto window = GUI::Window::construct();
    window->set_icon(app_icon.bitmap_for_size(16));
    window->restore_size_and_position("FontEditor"sv, "Window"sv, { { 640, 470 } });
    window->save_size_and_position_on_close("FontEditor"sv, "Window"sv);

    auto font_editor = TRY(FontEditor::MainWidget::try_create());
    window->set_main_widget(font_editor);
    TRY(font_editor->initialize_menubar(*window));
    font_editor->reset();

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (font_editor->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    auto default_path = TRY(String::from_byte_string(Config::read_string("FontEditor"sv, "Defaults"sv, "Font"sv, {})));
    auto path_to_load = path.is_empty() ? default_path : path;
    if (!path_to_load.is_empty()) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, path_to_load);
        if (!response.is_error()) {
            if (auto result = font_editor->open_file(path, response.value().release_stream()); result.is_error())
                font_editor->show_error(result.release_error(), "Opening"sv, path_to_load);
        }
    }

    return app->exec();
}
