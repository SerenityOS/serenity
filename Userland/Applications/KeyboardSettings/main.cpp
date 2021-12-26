/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterMapFileListModel.h"
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibKeyboard/CharacterMap.h>
#include <spawn.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath cpath wpath recvfd sendfd unix proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // If there is no command line parameter go for GUI.
    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio rpath cpath wpath recvfd sendfd proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto config = Core::ConfigFile::get_for_app("KeyboardSettings");
    if (unveil(config->filename().characters(), "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/keymap", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/proc/keymap", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr)) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-keyboard-settings");

    auto proc_keymap = Core::File::construct("/proc/keymap");
    if (!proc_keymap->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();

    auto json = JsonValue::from_string(proc_keymap->read_all());
    VERIFY(json.has_value());
    JsonObject keymap_object = json.value().as_object();
    VERIFY(keymap_object.has("keymap"));
    String current_keymap = keymap_object.get("keymap").to_string();
    dbgln("KeyboardSettings thinks the current keymap is: {}", current_keymap);

    Vector<String> character_map_files;
    Core::DirIterator iterator("/res/keymaps/", Core::DirIterator::Flags::SkipDots);
    if (iterator.has_error()) {
        GUI::MessageBox::show(nullptr, String::formatted("Error on reading mapping file list: {}", iterator.error_string()), "Keyboard settings", GUI::MessageBox::Type::Error);
        return -1;
    }

    while (iterator.has_next()) {
        auto name = iterator.next_path();
        name.replace(".json", "");
        character_map_files.append(name);
    }
    quick_sort(character_map_files);

    size_t initial_keymap_index = SIZE_MAX;
    for (size_t i = 0; i < character_map_files.size(); ++i) {
        if (character_map_files[i].equals_ignoring_case(current_keymap))
            initial_keymap_index = i;
    }
    VERIFY(initial_keymap_index < character_map_files.size());

    auto window = GUI::Window::construct();
    window->set_title("Keyboard Settings");
    window->resize(300, 70);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& root_widget = window->set_main_widget<GUI::Widget>();
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.set_fill_with_background_color(true);
    root_widget.layout()->set_spacing(0);
    root_widget.layout()->set_margins({ 4, 4, 4, 4 });

    auto& character_map_file_selection_container = root_widget.add<GUI::Widget>();
    character_map_file_selection_container.set_layout<GUI::HorizontalBoxLayout>();
    character_map_file_selection_container.set_fixed_height(22);

    auto& character_map_file_label = character_map_file_selection_container.add<GUI::Label>();
    character_map_file_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    character_map_file_label.set_fixed_width(130);
    character_map_file_label.set_text("Character Mapping File:");

    auto& character_map_file_combo = character_map_file_selection_container.add<GUI::ComboBox>();
    character_map_file_combo.set_only_allow_values_from_model(true);
    character_map_file_combo.set_model(*CharacterMapFileListModel::create(character_map_files));
    character_map_file_combo.set_selected_index(initial_keymap_index);

    auto& num_lock_checkbox = root_widget.add<GUI::CheckBox>("Enable Num Lock on login");
    num_lock_checkbox.set_checked(config->read_bool_entry("StartupEnable", "NumLock", true));

    root_widget.layout()->add_spacer();

    auto apply_settings = [&](bool quit) {
        String character_map_file = character_map_file_combo.text();
        if (character_map_file.is_empty()) {
            GUI::MessageBox::show(window, "Please select character mapping file.", "Keyboard settings", GUI::MessageBox::Type::Error);
            return;
        }
        pid_t child_pid;
        const char* argv[] = { "/bin/keymap", character_map_file.characters(), nullptr };
        if ((errno = posix_spawn(&child_pid, "/bin/keymap", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
            exit(1);
        }

        config->write_bool_entry("StartupEnable", "NumLock", num_lock_checkbox.is_checked());
        config->sync();

        if (quit)
            app->quit();
    };

    auto& bottom_widget = root_widget.add<GUI::Widget>();
    bottom_widget.set_layout<GUI::HorizontalBoxLayout>();
    bottom_widget.layout()->add_spacer();
    bottom_widget.set_fixed_height(22);

    auto& apply_button = bottom_widget.add<GUI::Button>();
    apply_button.set_text("Apply");
    apply_button.set_fixed_width(60);
    apply_button.on_click = [&](auto) {
        apply_settings(false);
    };

    auto& ok_button = bottom_widget.add<GUI::Button>();
    ok_button.set_text("OK");
    ok_button.set_fixed_width(60);
    ok_button.on_click = [&](auto) {
        apply_settings(true);
    };

    auto& cancel_button = bottom_widget.add<GUI::Button>();
    cancel_button.set_text("Cancel");
    cancel_button.set_fixed_width(60);
    cancel_button.on_click = [&](auto) {
        app->quit();
    };

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        });

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(quit_action);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Keyboard Settings", app_icon, window));

    window->set_menubar(move(menubar));

    window->show();

    return app->exec();
}
