/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, Conor Byrne <conor@cbyrne.dev>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexEditorWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
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

    auto app = TRY(GUI::Application::create(arguments));

    StringView filename;
    StringView annotations_filename;

    Core::ArgsParser args_parser;
    args_parser.add_option(annotations_filename, "Annotations file to load", "annotations", 'a', "path");
    args_parser.add_positional_argument(filename, "File to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/Applications/HexEditor.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    Config::pledge_domain("HexEditor");
    app->set_config_domain("HexEditor"_string);

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-hex-editor"sv));

    auto window = GUI::Window::construct();
    window->set_title("Hex Editor");
    window->restore_size_and_position("HexEditor"sv, "Window"sv, { { 640, 400 } });
    window->save_size_and_position_on_close("HexEditor"sv, "Window"sv);

    auto hex_editor_widget = TRY(HexEditor::HexEditorWidget::create());
    window->set_main_widget(hex_editor_widget);

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (hex_editor_widget->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    TRY(hex_editor_widget->initialize_menubar(*window));
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (!filename.is_empty()) {
        // FIXME: Using `try_request_file_read_only_approved` doesn't work here since the file stored in the editor is only readable.
        auto response = FileSystemAccessClient::Client::the().request_file(window, filename, Core::File::OpenMode::ReadWrite);
        if (!response.is_error())
            hex_editor_widget->open_file(response.value().filename(), response.value().release_stream());
    }

    if (!annotations_filename.is_empty())
        hex_editor_widget->open_annotations_file(annotations_filename);

    return app->exec();
}
