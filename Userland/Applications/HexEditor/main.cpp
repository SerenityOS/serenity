/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, Conor Byrne <conor@cbyrne.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexEditorWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix cpath wpath thread"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("HexEditor");

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-hex-editor"));

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Hex Editor");
    window->resize(640, 400);

    auto hex_editor_widget = TRY(window->try_set_main_widget<HexEditorWidget>());

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (hex_editor_widget->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    hex_editor_widget->initialize_menubar(*window);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (arguments.argc > 1) {
        // FIXME: Using `try_request_file_read_only_approved` doesn't work here since the file stored in the editor is only readable.
        auto response = FileSystemAccessClient::Client::the().try_request_file(window, arguments.strings[1], Core::OpenMode::ReadWrite);
        if (response.is_error())
            return 1;
        hex_editor_widget->open_file(response.value());
    }

    return app->exec();
}
