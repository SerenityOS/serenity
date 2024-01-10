/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Sam Cohen <sbcohen2000@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyboardSettingsWidget.h"
#include "KeymapDialog.h"
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <LibConfig/Client.h>
#include <LibCore/Directory.h>
#include <LibFileSystem/FileSystem.h>
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

namespace KeyboardSettings {
class KeymapSelectionDialog final : public GUI::Dialog {
    C_OBJECT(KeymapSelectionDialog)
public:
    virtual ~KeymapSelectionDialog() override = default;

    static ByteString select_keymap(Window* parent_window, Vector<ByteString> const& selected_keymaps)
    {
        auto dialog_or_error = KeymapSelectionDialog::create(parent_window, selected_keymaps);
        if (dialog_or_error.is_error()) {
            GUI::MessageBox::show(parent_window, "Couldn't load \"add keymap\" dialog"sv, "Error while opening \"add keymap\" dialog"sv, GUI::MessageBox::Type::Error);
            return ByteString::empty();
        }

        auto dialog = dialog_or_error.release_value();
        dialog->set_title("Add a keymap");

        if (dialog->exec() == ExecResult::OK) {
            return dialog->selected_keymap();
        }

        return ByteString::empty();
    }

    ByteString selected_keymap() { return m_selected_keymap; }

private:
    static ErrorOr<NonnullRefPtr<KeymapSelectionDialog>> create(Window* parent_window, Vector<ByteString> const& selected_keymaps)
    {
        auto widget = TRY(KeyboardSettings::KeymapDialog::try_create());
        auto dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) KeymapSelectionDialog(parent_window, selected_keymaps, widget)));
        return dialog;
    }

    KeymapSelectionDialog(Window* parent_window, Vector<ByteString> const& selected_keymaps, NonnullRefPtr<KeymapDialog> widget)
        : Dialog(parent_window)
    {
        set_main_widget(widget);

        set_resizable(false);
        resize(190, 54);

        set_icon(parent_window->icon());

        auto iterator_result = Core::Directory::for_each_entry("/res/keymaps/"sv, Core::DirIterator::Flags::SkipDots, [&](auto const& entry, auto&) -> ErrorOr<IterationDecision> {
            auto basename = entry.name.replace(".json"sv, ""sv, ReplaceMode::FirstOnly);
            if (selected_keymaps.find(basename).is_end())
                m_character_map_files.append(basename);
            return IterationDecision::Continue;
        });

        if (iterator_result.is_error()) {
            GUI::MessageBox::show(nullptr, ByteString::formatted("Error on reading mapping file list: {}", iterator_result.error()), "Keyboard settings"sv, GUI::MessageBox::Type::Error);
            GUI::Application::the()->quit(-1);
        }

        quick_sort(m_character_map_files);

        m_selected_keymap = m_character_map_files.first();

        m_keymaps_combobox = *widget->find_descendant_of_type_named<GUI::ComboBox>("keymaps_combobox");
        m_keymaps_combobox->set_only_allow_values_from_model(true);
        m_keymaps_combobox->set_model(*GUI::ItemListModel<ByteString>::create(m_character_map_files));
        m_keymaps_combobox->set_selected_index(0);

        m_keymaps_combobox->on_change = [&](auto& keymap, auto) {
            m_selected_keymap = keymap;
        };

        auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
        ok_button.on_click = [this](auto) {
            done(ExecResult::OK);
        };

        auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
        cancel_button.on_click = [this](auto) {
            done(ExecResult::Cancel);
        };
    }

    RefPtr<GUI::ComboBox> m_keymaps_combobox;
    Vector<ByteString> m_character_map_files;
    ByteString m_selected_keymap;
};

class KeymapModel final : public GUI::Model {
public:
    KeymapModel() {};

    int row_count(GUI::ModelIndex const&) const override { return m_data.size(); }
    int column_count(GUI::ModelIndex const&) const override { return 1; }

    GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        ByteString const& data = m_data.at(index.row());
        if (role == GUI::ModelRole::Font && data == m_active_keymap)
            return Gfx::FontDatabase::default_font().bold_variant();

        return data;
    }

    void remove_at(size_t index)
    {
        m_data.remove(index);
        invalidate();
    }

    void add_keymap(ByteString const& keymap)
    {
        m_data.append(keymap);
        invalidate();
    }

    void set_active_keymap(ByteString const& keymap)
    {
        m_active_keymap = keymap;
        invalidate();
    }

    ByteString const& active_keymap() { return m_active_keymap; }

    ByteString const& keymap_at(size_t index)
    {
        return m_data[index];
    }

    Vector<ByteString> const& keymaps() const { return m_data; }

private:
    Vector<ByteString> m_data;
    ByteString m_active_keymap;
};

ErrorOr<NonnullRefPtr<KeyboardSettingsWidget>> KeyboardSettingsWidget::create()
{
    auto widget = TRY(try_create());
    TRY(widget->setup());
    return widget;
}

ErrorOr<void> KeyboardSettingsWidget::setup()
{
    auto proc_keymap = TRY(Core::File::open("/sys/kernel/keymap"sv, Core::File::OpenMode::Read));

    auto keymap = TRY(proc_keymap->read_until_eof());
    auto json = TRY(JsonValue::from_string(keymap));
    auto const& keymap_object = json.as_object();
    VERIFY(keymap_object.has("keymap"sv));
    m_initial_active_keymap = keymap_object.get_byte_string("keymap"sv).value();
    dbgln("KeyboardSettings thinks the current keymap is: {}", m_initial_active_keymap);

    auto mapper_config(TRY(Core::ConfigFile::open("/etc/Keyboard.ini")));
    auto keymaps = mapper_config->read_entry("Mapping", "Keymaps", "");

    auto keymaps_vector = keymaps.split(',');

    m_selected_keymaps_listview = find_descendant_of_type_named<GUI::ListView>("selected_keymaps");
    m_selected_keymaps_listview->horizontal_scrollbar().set_visible(false);
    m_selected_keymaps_listview->set_model(TRY(try_make_ref_counted<KeymapModel>()));
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
        if (selection.is_empty()) {
            m_activate_keymap_button->set_enabled(false);
        } else {
            auto& highlighted_keymap = keymaps_list_model.keymap_at(selection.first().row());
            auto& active_keymap = keymaps_list_model.active_keymap();
            m_activate_keymap_button->set_enabled(highlighted_keymap != active_keymap);
        }
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

    m_caps_lock_checkbox = find_descendant_of_type_named<GUI::CheckBox>("caps_lock_remapped_to_ctrl_checkbox");
    auto caps_lock_is_remapped = read_caps_lock_to_ctrl_sys_variable();
    if (caps_lock_is_remapped.is_error()) {
        auto error_message = ByteString::formatted("Could not determine if Caps Lock is remapped to Ctrl: {}", caps_lock_is_remapped.error());
        GUI::MessageBox::show_error(window(), error_message);
    } else {
        m_caps_lock_checkbox->set_checked(caps_lock_is_remapped.value());
    }
    m_caps_lock_checkbox->set_enabled(getuid() == 0);
    m_caps_lock_checkbox->on_checked = [&](auto) {
        set_modified(true);
    };
    return {};
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
    write_caps_lock_to_ctrl_sys_variable(m_caps_lock_checkbox->is_checked());
}

void KeyboardSettingsWidget::set_keymaps(Vector<ByteString> const& keymaps, ByteString const& active_keymap)
{
    auto keymaps_string = ByteString::join(',', keymaps);
    GUI::Process::spawn_or_show_error(window(), "/bin/keymap"sv, Array { "-s", keymaps_string.characters(), "-m", active_keymap.characters() });
}

void KeyboardSettingsWidget::write_caps_lock_to_ctrl_sys_variable(bool caps_lock_to_ctrl)
{
    if (getuid() != 0)
        return;

    auto write_command = ByteString::formatted("caps_lock_to_ctrl={}", caps_lock_to_ctrl ? "1" : "0");
    GUI::Process::spawn_or_show_error(window(), "/bin/sysctl"sv, Array { "-w", write_command.characters() });
}

ErrorOr<bool> KeyboardSettingsWidget::read_caps_lock_to_ctrl_sys_variable()
{
    auto file = TRY(Core::File::open("/sys/kernel/conf/caps_lock_to_ctrl"sv, Core::File::OpenMode::Read));
    auto buffer = TRY(file->read_until_eof());
    StringView contents_string((char const*)buffer.data(), min(1, buffer.size()));
    return contents_string == "1";
}

}
