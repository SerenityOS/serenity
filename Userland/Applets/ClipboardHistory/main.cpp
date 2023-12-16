/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClipboardHistoryModel.h"
#include <LibConfig/Client.h>
#include <LibCore/Directory.h>
#include <LibCore/StandardPaths.h>
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
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix cpath wpath"));
    auto app = TRY(GUI::Application::create(arguments));
    auto clipboard_config = TRY(Core::ConfigFile::open_for_app("ClipboardHistory"));

    auto const default_path = ByteString::formatted("{}/{}", Core::StandardPaths::data_directory(), "Clipboard/ClipboardHistory.json"sv);
    auto const clipboard_file_path = clipboard_config->read_entry("Clipboard", "ClipboardFilePath", default_path);
    auto const parent_path = LexicalPath(clipboard_file_path);
    TRY(Core::Directory::create(parent_path.dirname(), Core::Directory::CreateDirectories::Yes));

    Config::pledge_domain("ClipboardHistory");
    Config::monitor_domain("ClipboardHistory");

    TRY(Core::System::pledge("stdio recvfd sendfd rpath cpath wpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(parent_path.dirname(), "rwc"sv));

    TRY(Core::System::unveil(nullptr, nullptr));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("edit-copy"sv));

    auto main_window = GUI::Window::construct();
    main_window->set_title("Clipboard History");
    main_window->set_rect(670, 65, 325, 500);
    main_window->set_icon(app_icon.bitmap_for_size(16));

    auto table_view = main_window->set_main_widget<GUI::TableView>();
    auto model = ClipboardHistoryModel::create();

    TRY(model->read_from_file(clipboard_file_path));

    auto data_and_type = GUI::Clipboard::the().fetch_data_and_type();
    if (!(data_and_type.data.is_empty() && data_and_type.mime_type.is_empty() && data_and_type.metadata.is_empty()))
        model->add_item(data_and_type);

    table_view->set_model(model);

    table_view->on_activation = [&](GUI::ModelIndex const& index) {
        auto& data_and_type = model->item_at(index.row()).data_and_type;
        GUI::Clipboard::the().set_data(data_and_type.data, data_and_type.mime_type, data_and_type.metadata);
    };

    auto delete_action = GUI::CommonActions::make_delete_action([&](const GUI::Action&) {
        if (table_view->selection().is_empty())
            return;

        auto index = table_view->selection().first();
        model->remove_item(index.row());
        if (model->is_empty()) {
            GUI::Clipboard::the().clear();
        } else if (index.row() == 0) {
            auto const& data_and_type = model->item_at(index.row()).data_and_type;
            GUI::Clipboard::the().set_data(data_and_type.data, data_and_type.mime_type, data_and_type.metadata);
        }
    });

    auto debug_dump_action = GUI::Action::create("Dump to Debug Console", [&](const GUI::Action&) {
        table_view->selection().for_each_index([&](GUI::ModelIndex& index) {
            dbgln("{}", model->data(index, GUI::ModelRole::Display).as_string());
        });
    });

    auto clear_action = GUI::Action::create("Clear History", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/trash-can.png"sv)), [&](const GUI::Action&) {
        model->clear();
        GUI::Clipboard::the().clear();
    });

    auto entry_context_menu = GUI::Menu::construct();
    entry_context_menu->add_action(delete_action);
    entry_context_menu->add_action(debug_dump_action);
    entry_context_menu->add_separator();
    entry_context_menu->add_action(clear_action);
    table_view->on_context_menu_request = [&](GUI::ModelIndex const&, GUI::ContextMenuEvent const& event) {
        delete_action->set_enabled(!table_view->selection().is_empty());
        debug_dump_action->set_enabled(!table_view->selection().is_empty());
        clear_action->set_enabled(!model->is_empty());
        entry_context_menu->popup(event.screen_position());
    };

    auto applet_window = GUI::Window::construct();
    applet_window->set_title("ClipboardHistory");
    applet_window->set_window_type(GUI::WindowType::Applet);
    applet_window->set_has_alpha_channel(true);
    auto icon_widget = applet_window->set_main_widget<GUI::ImageWidget>();
    icon_widget->set_tooltip("Clipboard History"_string);
    icon_widget->load_from_file("/res/icons/16x16/edit-copy.png"sv);
    icon_widget->on_click = [&main_window = *main_window] {
        main_window.show();
        main_window.move_to_front();
    };
    applet_window->resize(16, 16);
    applet_window->show();

    return app->exec();
}
