/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewerWidget.h"
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = GUI::Application::construct(arguments);
    auto app_icon = GUI::Icon::default_icon("app-pdf-viewer");

    auto window = GUI::Window::construct();
    window->set_title("PDF Viewer");
    window->resize(640, 400);

    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto pdf_viewer_widget = TRY(window->try_set_main_widget<PDFViewerWidget>());

    pdf_viewer_widget->initialize_menubar(*window);

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (arguments.argc >= 2) {
        auto response = FileSystemAccessClient::Client::the().try_request_file_read_only_approved(window, arguments.argv[1]);
        if (response.is_error())
            return 1;
        pdf_viewer_widget->open_file(*response.value());
    }

    return app->exec();
}
