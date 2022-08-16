/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClipboardHistoryModel.h"
#include <AK/LexicalPath.h>
#include <LibConfig/Client.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

static void try_load_clipboard_content(String clipboard_file_path, RefPtr<ClipboardHistoryModel> model)
{
    dbgln("Try loading content from persistent clipboard");

    auto clipboard_file_or_error = Core::File::open(clipboard_file_path, Core::OpenMode::ReadOnly);
    if (clipboard_file_or_error.is_error()) {
        dbgln("Failed to open persistent clipboard file {} as read-only", clipboard_file_path);
        return;
    }
    auto file_content = clipboard_file_or_error.release_value()->read_all();
    auto json_or_error = JsonValue::from_string(file_content);

    if (json_or_error.is_error()) {
        dbgln("Failed to parse persistent clipboard file {}. Abort loading.", clipboard_file_path);
        return;
    }

    auto json = json_or_error.release_value();
    json.as_array().for_each([&model = *model](JsonValue const& entry) {
        if (!entry.as_object().has("Data"sv) || !entry.as_object().has("Type"sv)) {
            return IterationDecision::Continue;
        }

        HashMap<String, String> metadata;
        auto mime_type = entry.as_object().get("Type"sv).to_string();
        auto data_bytes = entry.as_object().get("Data"sv).to_string().bytes();
        auto data = ByteBuffer::copy(data_bytes.data(), data_bytes.size());

        if (data.is_error()) {
            return IterationDecision::Continue;
        }

        auto state = ClipboardHistoryModel::ClipboardEntryState::Persistent;
        GUI::Clipboard::DataAndType item = { data.release_value(), mime_type, metadata };
        model.add_item(item, state);

        return IterationDecision::Continue;
    });
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix cpath wpath"));
    auto app = TRY(GUI::Application::try_create(arguments));
    auto clipboard_config = TRY(Core::ConfigFile::open_for_app("KeyboardSettings"));
    bool persistent_clipboard = clipboard_config->read_bool_entry("Clipboard", "PersistentClipboard", false);
    auto clipboard_file_path = clipboard_config->read_entry("Clipboard", "ClipboardFilePath");

    Config::pledge_domain("ClipboardHistory");
    Config::monitor_domain("ClipboardHistory");
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(Core::StandardPaths::config_directory(), "r"sv));
    if (!clipboard_file_path.is_empty())
        TRY(Core::System::unveil(clipboard_file_path, "cwr"sv));
    TRY(Core::System::unveil(nullptr, nullptr));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("edit-copy"sv));

    auto main_window = TRY(GUI::Window::try_create());
    main_window->set_title("Clipboard history");
    main_window->set_rect(670, 65, 325, 500);
    main_window->set_icon(app_icon.bitmap_for_size(16));

    auto table_view = TRY(main_window->try_set_main_widget<GUI::TableView>());
    auto model = ClipboardHistoryModel::create();
    table_view->set_model(model);

    if (persistent_clipboard && !clipboard_file_path.is_empty())
        try_load_clipboard_content(clipboard_file_path, model);

    table_view->on_activation = [&](GUI::ModelIndex const& index) {
        auto& data_and_type = model->item_at(index.row()).data_and_type;
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

    auto persistent_toggle_action = GUI::Action::create("Toggle persistence", [&](const GUI::Action&) {
        table_view->selection().for_each_index([&](GUI::ModelIndex& index) {
            auto clipboard_config = Core::ConfigFile::open_for_app("KeyboardSettings");
            if (clipboard_config.is_error()) {
                dbgln("Cannot read config for KeyboardSettings application from Clipboard manager");
                return;
            }
            auto clipboard_file_path = clipboard_config.release_value()->read_entry("Clipboard", "ClipboardFilePath");
            if (clipboard_file_path.is_empty()) {
                dbgln("Persistent clipboard path is not defined, cannot write to it");
                return;
            }

            auto data_string = String::copy(model->item_at(index.row()).data_and_type.data);
            auto mime_type = model->item_at(index.row()).data_and_type.mime_type;

            auto clipboard_entry_index = index.row();
            bool currently_persistent = model->is_persistent(clipboard_entry_index);

            JsonValue clipboard_json_content;

            auto clipboard_file_or_error = Core::File::open(clipboard_file_path, Core::OpenMode::ReadOnly);
            if (clipboard_file_or_error.is_error()) {
                // Setting default value as the clipboard file could not exist yet
                clipboard_json_content = JsonValue::from_string("[]"sv).release_value();
            } else {
                auto clipboard_content = clipboard_file_or_error.release_value()->read_all();
                auto json_or_error = JsonValue::from_string(clipboard_content);
                if (json_or_error.is_error()) {
                    dbgln("Failed to parse persistent clipboard file {}", clipboard_file_path);
                    return;
                }
                clipboard_json_content = json_or_error.release_value();
            }

            JsonArray new_entries;

            if (!currently_persistent) {
                if (mime_type == "text/plain" && data_string.length() < 128) {
                    dbgln("Setting clipboard entry persistent: {}", data_string);
                }

                clipboard_json_content.as_array().for_each([&](auto& entry) {
                    new_entries.append(entry);
                });

                JsonObject t;
                t.set("Data", data_string);
                t.set("Type", mime_type);
                JsonValue new_entry_object = JsonValue::from_string(t.to_string()).value();

                new_entries.append(new_entry_object);
            } else {
                if (mime_type == "text/plain" && data_string.length() < 128) {
                    dbgln("Removing clipboard entry from persistent clipboard: {}", data_string);
                }

                clipboard_json_content.as_array().for_each([&](auto& entry) {
                    auto mime_type = entry.as_object().get("Type"sv).to_string();
                    auto data = entry.as_object().get("Data"sv).to_string();

                    if (!((mime_type == mime_type) && (data == data_string))) {
                        new_entries.append(entry);
                    }
                });
            }

            auto clipboard_file_write_or_error = Core::File::open(clipboard_file_path, Core::OpenMode::WriteOnly | Core::OpenMode::Truncate);
            if (clipboard_file_write_or_error.is_error()) {
                dbgln("Failed to open persistent clipboard file {} as write only", clipboard_file_path);
                return;
            }

            bool clipboard_updated = clipboard_file_write_or_error.release_value()->write(new_entries.to_string());
            if (clipboard_updated) {
                auto state = ClipboardHistoryModel::ClipboardEntryState::Persistent;
                if (currently_persistent) {
                    state = ClipboardHistoryModel::ClipboardEntryState::Volatile;
                }
                model->set_state(clipboard_entry_index, state);
            }
        });
    });

    auto entry_context_menu = TRY(GUI::Menu::try_create());
    TRY(entry_context_menu->try_add_action(delete_action));
    TRY(entry_context_menu->try_add_action(debug_dump_action));
    TRY(entry_context_menu->try_add_action(persistent_toggle_action));

    table_view->on_context_menu_request = [&](GUI::ModelIndex const&, GUI::ContextMenuEvent const& event) {
        delete_action->set_enabled(!table_view->selection().is_empty());
        debug_dump_action->set_enabled(!table_view->selection().is_empty());
        persistent_toggle_action->set_enabled(
            (!table_view->selection().is_empty()) && (persistent_clipboard));
        entry_context_menu->popup(event.screen_position());
    };

    // Saved x and y positions since their values will be 0 if we hide the window
    int saved_x = main_window->x();
    int saved_y = main_window->y();

    auto applet_window = TRY(GUI::Window::try_create());
    applet_window->set_title("ClipboardHistory");
    applet_window->set_window_type(GUI::WindowType::Applet);
    applet_window->set_has_alpha_channel(true);
    auto icon_widget = TRY(applet_window->try_set_main_widget<GUI::ImageWidget>());
    icon_widget->set_tooltip("Clipboard History");
    icon_widget->load_from_file("/res/icons/16x16/edit-copy.png"sv);
    icon_widget->on_click = [&main_window = *main_window, &saved_x, &saved_y] {
        if (main_window.is_visible()) {
            if (main_window.is_active()) {
                saved_x = main_window.x();
                saved_y = main_window.y();
                main_window.hide();
            } else {
                main_window.move_to_front();
            }
        } else {
            main_window.set_rect(saved_x, saved_y, main_window.width(), main_window.height());
            main_window.show();
            main_window.move_to_front();
        }
    };
    applet_window->resize(16, 16);
    applet_window->show();

    return app->exec();
}
