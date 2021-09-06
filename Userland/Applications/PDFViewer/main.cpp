/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewerWidget.h"
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-pdf-viewer");

    auto window = GUI::Window::construct();
    window->set_title("PDF Viewer");
    window->resize(640, 400);

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/filesystemaccess", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto& pdf_viewer_widget = window->set_main_widget<PDFViewerWidget>();

    pdf_viewer_widget.initialize_menubar(*window);

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (argc >= 2) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window->window_id(), argv[1]);

        if (response.error != 0) {
            if (response.error != -1)
                GUI::MessageBox::show_error(window, String::formatted("Opening \"{}\" failed: {}", *response.chosen_file, strerror(response.error)));
            return 1;
        }
        pdf_viewer_widget.open_file(*response.fd, *response.chosen_file);
    }

    return app->exec();
}
