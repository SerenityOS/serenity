/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewerWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-pdf-viewer");

    auto window = GUI::Window::construct();
    window->set_title("PDF Viewer");
    window->resize(640, 400);

    auto& pdf_viewer_widget = window->set_main_widget<PDFViewerWidget>();

    auto menubar = GUI::Menubar::construct();
    pdf_viewer_widget.initialize_menubar(menubar);
    window->set_menubar(menubar);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (argc >= 2)
        pdf_viewer_widget.open_file(argv[1]);

    return app->exec();
}
