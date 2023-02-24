/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HelpWindow.h"
#include "Spreadsheet.h"
#include "SpreadsheetWidget.h"
#include <AK/ScopeGuard.h>
#include <AK/Try.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DeprecatedFile.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath fattr unix cpath wpath thread"));

    auto app = TRY(GUI::Application::try_create(arguments));

    char const* filename = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "File to read from", "file", Core::ArgsParser::Required::No);

    args_parser.parse(arguments);

    if (filename) {
        if (!Core::DeprecatedFile::exists({ filename, strlen(filename) }) || Core::DeprecatedFile::is_directory(filename)) {
            warnln("File does not exist or is a directory: {}", filename);
            return 1;
        }
    }

    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/webcontent", "rw"));
    TRY(Core::System::unveil("/etc", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-spreadsheet"sv);
    auto window = GUI::Window::construct();
    window->resize(640, 480);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto spreadsheet_widget = TRY(window->set_main_widget<Spreadsheet::SpreadsheetWidget>(*window, NonnullRefPtrVector<Spreadsheet::Sheet> {}, filename == nullptr));

    spreadsheet_widget->initialize_menubar(*window);
    spreadsheet_widget->update_window_title();

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (spreadsheet_widget->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    if (filename) {
        auto file = TRY(FileSystemAccessClient::Client::the().request_file_read_only_approved(window, filename));
        spreadsheet_widget->load_file(file.filename(), file.stream());
    }

    return app->exec();
}
