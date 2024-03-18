/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2022-2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread recvfd sendfd cpath rpath wpath unix"));
    auto app = TRY(GUI::Application::create(arguments));

    Config::enable_permissive_mode();
    Config::pledge_domain("GMLPlayground");
    app->set_config_domain("GMLPlayground"_string);

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/Applications/GMLPlayground.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    StringView path;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "GML file to edit", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-gml-playground"sv));
    auto window = GUI::Window::construct();
    window->set_title("GML Playground");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->restore_size_and_position("GMLPlayground"sv, "Window"sv, { { 800, 600 } });
    window->save_size_and_position_on_close("GMLPlayground"sv, "Window"sv);

    auto main_widget = TRY(MainWidget::try_create(app_icon));
    window->set_main_widget(main_widget);

    TRY(main_widget->initialize_menubar(window));

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        return main_widget->request_close();
    };

    window->show();

    if (path.is_empty()) {
        main_widget->editor().set_text(R"~~~(@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
    }

    // Now add some widgets!
}
)~~~"sv);
        main_widget->editor().set_cursor(4, 28); // after "...widgets!"
        main_widget->update_title();
    } else {
        auto file = TRY(FileSystemAccessClient::Client::the().request_file_read_only_approved(window, path));
        main_widget->load_file(move(file));
    }

    return app->exec();
}
