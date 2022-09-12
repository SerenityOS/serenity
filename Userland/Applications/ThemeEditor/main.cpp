/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "PreviewWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath cpath wpath unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    StringView file_to_edit;

    Core::ArgsParser parser;
    parser.add_positional_argument(file_to_edit, "Theme file to edit", "file", Core::ArgsParser::Required::No);
    parser.parse(arguments);

    Optional<String> path = {};

    if (!file_to_edit.is_empty())
        path = Core::File::absolute_path(file_to_edit);

    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath unix"));
    TRY(Core::System::unveil("/tmp/user/%uid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-theme-editor"sv);
    auto window = GUI::Window::construct();

    auto main_widget = TRY(window->try_set_main_widget<ThemeEditor::MainWidget>());

    if (path.has_value()) {
        // Note: This is deferred to ensure that the window has already popped and thus proper window stealing can be performed.
        app->event_loop().deferred_invoke(
            [&window, &path, &main_widget]() {
                auto response = FileSystemAccessClient::Client::the().try_request_file_read_only_approved(window, path.value());
                if (response.is_error())
                    GUI::MessageBox::show_error(window, String::formatted("Opening \"{}\" failed: {}", path.value(), response.error()));
                else
                    main_widget->load_from_file(response.release_value());
            });
    }

    TRY(main_widget->initialize_menubar(window));
    main_widget->update_title();

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        return main_widget->request_close();
    };

    window->resize(820, 520);
    window->set_resizable(false);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));
    return app->exec();
}
