/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HelpWindow.h"
#include "LibFileSystemAccessClient/Client.h"
#include "Spreadsheet.h"
#include "SpreadsheetWidget.h"
#include <AK/ScopeGuard.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio recvfd sendfd rpath fattr unix cpath wpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    const char* filename = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "File to read from", "file", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    if (filename) {
        if (!Core::File::exists(filename) || Core::File::is_directory(filename)) {
            warnln("File does not exist or is a directory: {}", filename);
            return 1;
        }
    }

    if (unveil("/tmp/portal/webcontent", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    // For writing temporary files when exporting.
    if (unveil("/tmp", "crw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(Core::StandardPaths::home_directory().characters(), "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-spreadsheet");
    auto window = GUI::Window::construct();
    window->set_title("Spreadsheet");
    window->resize(640, 480);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& spreadsheet_widget = window->set_main_widget<Spreadsheet::SpreadsheetWidget>(*window, NonnullRefPtrVector<Spreadsheet::Sheet> {}, filename == nullptr);

    spreadsheet_widget.initialize_menubar(*window);

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (spreadsheet_widget.request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    if (filename) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window->window_id(), filename);

        if (response.error != 0) {
            if (response.error != -1)
                GUI::MessageBox::show_error(window, String::formatted("Opening \"{}\" failed: {}", *response.chosen_file, strerror(response.error)));
            return 1;
        }
        spreadsheet_widget.load_file(*response.fd, filename);
    }

    return app->exec();
}
