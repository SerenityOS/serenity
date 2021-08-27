/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClipboardHistoryModel.h"
#include <LibConfig/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio recvfd sendfd rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    Config::pledge_domains("ClipboardHistory");

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
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

    auto app_icon = GUI::Icon::default_icon("edit-copy");

    auto main_window = GUI::Window::construct();
    main_window->set_title("Clipboard history");
    main_window->set_rect(670, 65, 325, 500);
    main_window->set_icon(app_icon.bitmap_for_size(16));

    auto& table_view = main_window->set_main_widget<GUI::TableView>();
    auto model = ClipboardHistoryModel::create();
    table_view.set_model(model);

    table_view.on_activation = [&](const GUI::ModelIndex& index) {
        auto& data_and_type = model->item_at(index.row());
        GUI::Clipboard::the().set_data(data_and_type.data, data_and_type.mime_type, data_and_type.metadata);
    };

    auto delete_action = GUI::CommonActions::make_delete_action([&](const GUI::Action&) {
        model->remove_item(table_view.selection().first().row());
    });

    auto entry_context_menu = GUI::Menu::construct();
    entry_context_menu->add_action(delete_action);
    table_view.on_context_menu_request = [&](const GUI::ModelIndex&, const GUI::ContextMenuEvent& event) {
        delete_action->set_enabled(!table_view.selection().is_empty());
        entry_context_menu->popup(event.screen_position());
    };

    auto applet_window = GUI::Window::construct();
    applet_window->set_title("ClipboardHistory");
    applet_window->set_window_type(GUI::WindowType::Applet);
    applet_window->set_has_alpha_channel(true);
    auto& icon = applet_window->set_main_widget<GUI::ImageWidget>();
    icon.set_tooltip("Clipboard History");
    icon.load_from_file("/res/icons/16x16/edit-copy.png");
    icon.on_click = [&main_window = *main_window] {
        main_window.show();
        main_window.move_to_front();
    };
    applet_window->resize(16, 16);
    applet_window->show();

    return app->exec();
}
