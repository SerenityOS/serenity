/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyboardSettingsWidget.h"
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <Applications/KeyboardSettings/KeyboardWidgetGML.h>
#include <Applications/KeyboardSettings/KeymapDialogGML.h>
#include <LibConfig/Client.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/Process.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibKeyboard/CharacterMap.h>
#include <spawn.h>

class KeymapSelectionDialog final : public GUI::Dialog {
    C_OBJECT(KeymapSelectionDialog)
public:
    virtual ~KeymapSelectionDialog() override = default;

    static String select_keymap(Window* parent_window, Vector<String> const& selected_keymaps)
    {
        auto dialog = KeymapSelectionDialog::construct(parent_window, selected_keymaps);
        dialog->set_title("Add a keymap");

        if (dialog->exec() == ExecResult::OK) {
            return dialog->selected_keymap();
        }

        return String::empty();
    }

    String selected_keymap() { return m_selected_keymap; }

private:
    KeymapSelectionDialog(Window* parent_window, Vector<String> const& selected_keymaps)
        : Dialog(parent_window)
    {
        auto& widget = set_main_widget<GUI::Widget>();
        if (!widget.load_from_gml(keymap_dialog_gml))
            VERIFY_NOT_REACHED();

        set_resizable(false);
        resize(190, 54);

        set_icon(parent_window->icon());

        Core::DirIterator iterator("/res/keymaps/", Core::DirIterator::Flags::SkipDots);
        if (iterator.has_error()) {
            GUI::MessageBox::show(nullptr, String::formatted("Error on reading mapping file list: {}", iterator.error_string()), "Keyboard settings"sv, GUI::MessageBox::Type::Error);
            GUI::Application::the()->quit(-1);
        }

        while (iterator.has_next()) {
            auto name = iterator.next_path();
            auto basename = name.replace(".json"sv, ""sv, ReplaceMode::FirstOnly);
            if (!selected_keymaps.find(basename).is_end())
                continue;
            m_character_map_files.append(basename);
        }
        quick_sort(m_character_map_files);

        m_selected_keymap = m_character_map_files.first();

        m_keymaps_combobox = *widget.find_descendant_of_type_named<GUI::ComboBox>("keymaps_combobox");
        m_keymaps_combobox->set_only_allow_values_from_model(true);
        m_keymaps_combobox->set_model(*GUI::ItemListModel<String>::create(m_character_map_files));
        m_keymaps_combobox->set_selected_index(0);

        m_keymaps_combobox->on_change = [&](auto& keymap, auto) {
            m_selected_keymap = keymap;
        };

        auto& ok_button = *widget.find_descendant_of_type_named<GUI::Button>("ok_button");
        ok_button.on_click = [this](auto) {
            done(ExecResult::OK);
        };

        auto& cancel_button = *widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
        cancel_button.on_click = [this](auto) {
            done(ExecResult::Cancel);
        };
    }

    RefPtr<GUI::ComboBox> m_keymaps_combobox;
    Vector<String> m_character_map_files;
    String m_selected_keymap;
};

class KeymapModel final : public GUI::Model {
public:
    KeymapModel() {};

    int row_count(GUI::ModelIndex const&) const override { return m_data.size(); }
    int column_count(GUI::ModelIndex const&) const override { return 1; }

    GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        String const& data = m_data.at(index.row());
        if (role == GUI::ModelRole::Font && data == m_active_keymap)
            return Gfx::FontDatabase::default_font().bold_variant();

        return data;
    }

    void remove_at(size_t index)
    {
        m_data.remove(index);
        invalidate();
    }

    void add_keymap(String const& keymap)
    {
        m_data.append(keymap);
        invalidate();
    }

    void set_active_keymap(String const& keymap)
    {
        m_active_keymap = keymap;
        invalidate();
    }

    String const& active_keymap() { return m_active_keymap; }

    String const& keymap_at(size_t index)
    {
        return m_data[index];
    }

    Vector<String> const& keymaps() const { return m_data; }

private:
    Vector<String> m_data;
    String m_active_keymap;
};

KeyboardSettingsWidget::KeyboardSettingsWidget()
{
    load_from_gml(keyboard_widget_gml);

    auto proc_keymap = Core::File::construct("/sys/kernel/keymap");
    if (!proc_keymap->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();

    auto json = JsonValue::from_string(proc_keymap->read_all()).release_value_but_fixme_should_propagate_errors();
    auto const& keymap_object = json.as_object();
    VERIFY(keymap_object.has("keymap"sv));
    m_initial_active_keymap = keymap_object.get("keymap"sv).to_string();
    dbgln("KeyboardSettings thinks the current keymap is: {}", m_initial_active_keymap);

    auto mapper_config(Core::ConfigFile::open("/etc/Keyboard.ini").release_value_but_fixme_should_propagate_errors());
    auto keymaps = mapper_config->read_entry("Mapping", "Keymaps", "");

    auto keymaps_vector = keymaps.split(',');

    m_selected_keymaps_listview = find_descendant_of_type_named<GUI::ListView>("selected_keymaps");
    m_selected_keymaps_listview->horizontal_scrollbar().set_visible(false);
    m_selected_keymaps_listview->set_model(adopt_ref(*new KeymapModel()));
    auto& keymaps_list_model = static_cast<KeymapModel&>(*m_selected_keymaps_listview->model());

    for (auto& keymap : keymaps_vector) {
        m_initial_keymap_list.append(keymap);
        keymaps_list_model.add_keymap(keymap);
    }

    keymaps_list_model.set_active_keymap(m_initial_active_keymap);

    m_activate_keymap_button = find_descendant_of_type_named<GUI::Button>("activate_keymap_button");

    m_activate_keymap_event = [&]() {
        auto& selection = m_selected_keymaps_listview->selection();
        if (!selection.is_empty()) {
            auto& selected_keymap = keymaps_list_model.keymap_at(selection.first().row());
            keymaps_list_model.set_active_keymap(selected_keymap);
            set_modified(true);
        }
    };

    m_activate_keymap_button->on_click = [&](auto) {
        m_activate_keymap_event();
    };

    m_selected_keymaps_listview->on_activation = [&](auto) {
        m_activate_keymap_event();
    };

    m_add_keymap_button = find_descendant_of_type_named<GUI::Button>("add_keymap_button");

    m_add_keymap_button->on_click = [&](auto) {
        auto keymap = KeymapSelectionDialog::select_keymap(window(), keymaps_list_model.keymaps());
        if (!keymap.is_empty()) {
            keymaps_list_model.add_keymap(keymap);
            set_modified(true);
        }
    };

    m_remove_keymap_button = find_descendant_of_type_named<GUI::Button>("remove_keymap_button");

    m_remove_keymap_button->on_click = [&](auto) {
        auto& selection = m_selected_keymaps_listview->selection();
        bool active_keymap_deleted = false;
        for (auto& index : selection.indices()) {
            if (keymaps_list_model.keymap_at(index.row()) == keymaps_list_model.active_keymap())
                active_keymap_deleted = true;
            keymaps_list_model.remove_at(index.row());
        }
        if (active_keymap_deleted)
            keymaps_list_model.set_active_keymap(keymaps_list_model.keymap_at(0));
        set_modified(true);
    };

    m_selected_keymaps_listview->on_selection_change = [&]() {
        auto& selection = m_selected_keymaps_listview->selection();
        m_remove_keymap_button->set_enabled(!selection.is_empty() && keymaps_list_model.keymaps().size() > 1);
        m_activate_keymap_button->set_enabled(!selection.is_empty());
    };

    m_test_typing_area = *find_descendant_of_type_named<GUI::TextEditor>("test_typing_area");
    m_test_typing_area->on_focusin = [&]() {
        set_keymaps(keymaps_list_model.keymaps(), keymaps_list_model.active_keymap());
    };

    m_test_typing_area->on_focusout = [&]() {
        set_keymaps(m_initial_keymap_list, m_initial_active_keymap);
    };

    m_clear_test_typing_area_button = find_descendant_of_type_named<GUI::Button>("button_clear_test_typing_area");
    m_clear_test_typing_area_button->on_click = [this](auto) {
        m_test_typing_area->clear();
        m_test_typing_area->set_focus(true);
    };

    m_num_lock_checkbox = find_descendant_of_type_named<GUI::CheckBox>("num_lock_checkbox");
    m_num_lock_checkbox->set_checked(Config::read_bool("KeyboardSettings"sv, "StartupEnable"sv, "NumLock"sv, true));
    m_num_lock_checkbox->on_checked = [&](auto) {
        set_modified(true);
    };
}

KeyboardSettingsWidget::~KeyboardSettingsWidget()
{
    set_keymaps(m_initial_keymap_list, m_initial_active_keymap);
}

void KeyboardSettingsWidget::window_activated(bool is_active_window)
{
    if (is_active_window && m_test_typing_area->is_focused()) {
        auto& keymaps_list_model = static_cast<KeymapModel&>(*m_selected_keymaps_listview->model());
        set_keymaps(keymaps_list_model.keymaps(), keymaps_list_model.active_keymap());
    } else {
        set_keymaps(m_initial_keymap_list, m_initial_active_keymap);
    }
}

void KeyboardSettingsWidget::apply_settings()
{
    auto& m_keymaps_list_model = static_cast<KeymapModel&>(*m_selected_keymaps_listview->model());
    set_keymaps(m_keymaps_list_model.keymaps(), m_keymaps_list_model.active_keymap());
    m_initial_keymap_list.clear();
    for (auto& keymap : m_keymaps_list_model.keymaps()) {
        m_initial_keymap_list.append(keymap);
    }
    m_initial_active_keymap = m_keymaps_list_model.active_keymap();
    Config::write_bool("KeyboardSettings"sv, "StartupEnable"sv, "NumLock"sv, m_num_lock_checkbox->is_checked());
}

void KeyboardSettingsWidget::set_keymaps(Vector<String> const& keymaps, String const& active_keymap)
{
    auto keymaps_string = String::join(',', keymaps);
    GUI::Process::spawn_or_show_error(window(), "/bin/keymap"sv, Array { "-s", keymaps_string.characters(), "-m", active_keymap.characters() });
}
