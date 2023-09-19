/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewerWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView file_path;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(file_path, "PDF file to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::create(arguments));
    auto app_icon = GUI::Icon::default_icon("app-pdf-viewer"sv);

    Config::pledge_domain("PDFViewer");
    app->set_config_domain("PDFViewer"_string);

    auto window = GUI::Window::construct();
    window->set_title("PDF Viewer");
    window->resize(640, 400);

    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto pdf_viewer_widget = window->set_main_widget<PDFViewerWidget>();

    TRY(pdf_viewer_widget->initialize_menubar(*window));

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (!file_path.is_empty()) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, file_path);
        if (response.is_error())
            return 1;
        pdf_viewer_widget->open_file(response.value().filename(), response.value().release_stream());
    }

    return app->exec();
}
