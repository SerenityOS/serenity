/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "ClipboardHistoryModel.h"
#include <LibGUI/Application.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <stdio.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
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

    auto main_window = GUI::Window::construct();
    main_window->set_title("Clipboard history");
    main_window->set_rect(670, 65, 325, 500);

    auto& table_view = main_window->set_main_widget<GUI::TableView>();
    auto model = ClipboardHistoryModel::create();
    table_view.set_model(model);

    GUI::Clipboard::the().on_change = [&](const String&) {
        auto item = GUI::Clipboard::the().data_and_type();
        model->add_item(item);
    };

    table_view.on_activation = [&](const GUI::ModelIndex& index) {
        auto& data_and_type = model->item_at(index.row());
        GUI::Clipboard::the().set_data(data_and_type.data, data_and_type.mime_type, data_and_type.metadata);
    };

    auto applet_window = GUI::Window::construct();
    applet_window->set_title("ClipboardHistory");
    applet_window->set_window_type(GUI::WindowType::MenuApplet);
    auto& icon = applet_window->set_main_widget<GUI::ImageWidget>();
    icon.load_from_file("/res/icons/16x16/clipboard.png");
    icon.set_fill_with_background_color(true);
    icon.on_click = [&main_window = *main_window] {
        main_window.show();
    };
    applet_window->resize(16, 16);
    applet_window->show();

    return app->exec();
}
