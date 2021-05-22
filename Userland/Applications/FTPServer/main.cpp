/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FTPServer.h"
#include "FTPServerTransferModel.h"
#include <AK/JsonParser.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    String config_path = "/home/anon/.config/FTPServerConfig.json";
    bool nogui = false;
    int port = 21;

    if (geteuid() != 0) {
        port = 2121;
        config_path = String::formatted("{}/{}", Core::StandardPaths::config_directory(), "FTPServerConfig.json");
    }

    Core::ArgsParser args_parser;
    args_parser.add_option(port, "Sets the port to use", "port", 'p', "port");
    args_parser.add_option(nogui, "Runs in terminal-only mode", "nogui", 'n');
    args_parser.add_option(config_path, "The file used to load server configurations", "config_path", 'c', "config_path");
    args_parser.parse(argc, argv);

    outln("FTPServer: Loading config file from: {}", config_path);

    auto file = Core::File::construct(config_path);
    if (!file->open(Core::OpenMode::ReadWrite)) {
        outln("FTPServer: Unable to open config file: {}", config_path);
        return 1;
    }

    auto file_contents = file->read_all();
    auto maybe_json = JsonValue::from_string(file_contents);

    if (!maybe_json.has_value()) {
        outln("FTPServer: Failed to parse config file: {}", config_path);
        return 1;
    }

    if (pledge("stdio inet accept unix thread rpath sendfd recvfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop event_loop;
    FTPServer server(port, move(maybe_json.value().as_object()));

    if (!nogui) {
        auto app = GUI::Application::construct(argc, argv);

        auto window = GUI::Window::construct();
        window->set_title("FTP Server");
        window->resize(450, 600);
        window->center_on_screen();

        auto app_icon = GUI::Icon::default_icon("app-ftp-server");
        window->set_icon(app_icon.bitmap_for_size(16));

        auto& widget = window->set_main_widget<GUI::Widget>();
        widget.set_fill_with_background_color(true);
        widget.set_layout<GUI::VerticalBoxLayout>();

        auto& splitter = widget.add<GUI::VerticalSplitter>();
        server.m_log_view = splitter.add<GUI::TextEditor>(GUI::TextEditor::Type::MultiLine);
        server.m_log_view->set_mode(GUI::TextEditor::Mode::ReadOnly);

        server.m_transfer_table = splitter.add<GUI::TableView>();
        server.m_transfer_table->set_column_headers_visible(true);
        server.m_transfer_table->set_model(FTPServerTransferModel::create(server));
        server.m_transfer_table->model()->update();

        server.start();
        window->show();

        return GUI::Application::the()->exec();
    }

    server.start();

    return event_loop.exec();
}
