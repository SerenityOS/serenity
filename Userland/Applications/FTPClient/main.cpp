/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FTPClient.h"
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    FTPClient client;

    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();
    window->set_title("FTP Client");
    window->resize(450, 600);
    window->center_on_screen();

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();

    auto& v_splitter = widget.add<GUI::VerticalSplitter>();

    auto& splitter = v_splitter.add<GUI::HorizontalSplitter>();
    auto& left_side = splitter.add<GUI::Widget>();
    left_side.add<GUI::Label>("Left hello");

    auto& right_side = splitter.add<GUI::Widget>();
    right_side.add<GUI::Label>("Right hello");

    //window->show();

    client.run();

    //return GUI::Application::the()->exec();
}
