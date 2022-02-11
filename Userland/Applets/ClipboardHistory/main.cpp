/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClipboardHistoryModel.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));
    auto app = GUI::Application::construct(arguments);

    Config::pledge_domain("ClipboardHistory");
    Config::monitor_domain("ClipboardHistory");

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("edit-copy"));

    auto main_window = TRY(GUI::Window::try_create());
    main_window->set_title("Clipboard history");
    main_window->set_rect(670, 65, 325, 500);
    main_window->set_icon(app_icon.bitmap_for_size(16));

    auto table_view = TRY(main_window->try_set_main_widget<GUI::TableView>());
    auto model = ClipboardHistoryModel::create();
    table_view->set_model(model);

    table_view->on_activation = [&](GUI::ModelIndex const& index) {
        auto& data_and_type = model->item_at(index.row());
        GUI::Clipboard::the().set_data(data_and_type.data, data_and_type.mime_type, data_and_type.metadata);
    };

    auto delete_action = GUI::CommonActions::make_delete_action([&](const GUI::Action&) {
        model->remove_item(table_view->selection().first().row());
    });

    auto debug_dump_action = GUI::Action::create("Dump to debug console", [&](const GUI::Action&) {
        table_view->selection().for_each_index([&](GUI::ModelIndex& index) {
            dbgln("{}", model->data(index, GUI::ModelRole::Display).as_string());
        });
    });

    auto entry_context_menu = TRY(GUI::Menu::try_create());
    TRY(entry_context_menu->try_add_action(delete_action));
    TRY(entry_context_menu->try_add_action(debug_dump_action));
    table_view->on_context_menu_request = [&](GUI::ModelIndex const&, GUI::ContextMenuEvent const& event) {
        delete_action->set_enabled(!table_view->selection().is_empty());
        debug_dump_action->set_enabled(!table_view->selection().is_empty());
        entry_context_menu->popup(event.screen_position());
    };

    auto applet_window = TRY(GUI::Window::try_create());
    applet_window->set_title("ClipboardHistory");
    applet_window->set_window_type(GUI::WindowType::Applet);
    applet_window->set_has_alpha_channel(true);
    auto icon_widget = TRY(applet_window->try_set_main_widget<GUI::ImageWidget>());
    icon_widget->set_tooltip("Clipboard History");
    icon_widget->load_from_file("/res/icons/16x16/edit-copy.png");
    icon_widget->on_click = [&main_window = *main_window] {
        main_window.show();
        main_window.move_to_front();
    };
    applet_window->resize(16, 16);
    applet_window->show();

    return app->exec();
}
