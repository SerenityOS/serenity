/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexEditorWidget.h"
#include "FindDialog.h"
#include "GoToOffsetDialog.h"
#include "SearchResultsModel.h"
#include "ValueInspectorModel.h"
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <Applications/HexEditor/HexEditorWindowGML.h>
#include <LibConfig/Client.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibTextCodec/Decoder.h>
#include <string.h>

REGISTER_WIDGET(HexEditor, HexEditor);

HexEditorWidget::HexEditorWidget()
{
    load_from_gml(hex_editor_window_gml).release_value_but_fixme_should_propagate_errors();

    m_toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    m_toolbar_container = *find_descendant_of_type_named<GUI::ToolbarContainer>("toolbar_container");
    m_editor = *find_descendant_of_type_named<HexEditor>("editor");
    m_statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    m_search_results = *find_descendant_of_type_named<GUI::TableView>("search_results");
    m_search_results_container = *find_descendant_of_type_named<GUI::Widget>("search_results_container");
    m_side_panel_container = *find_descendant_of_type_named<GUI::Widget>("side_panel_container");
    m_value_inspector_container = *find_descendant_of_type_named<GUI::Widget>("value_inspector_container");
    m_value_inspector = *find_descendant_of_type_named<GUI::TableView>("value_inspector");

    m_value_inspector->on_activation = [this](GUI::ModelIndex const& index) {
        if (!index.is_valid())
            return;
        m_selecting_from_inspector = true;
        m_editor->set_selection(m_editor->selection_start_offset(), index.data(GUI::ModelRole::Custom).to_integer<size_t>());
        m_editor->update();
    };

    m_editor->on_status_change = [this](int position, HexEditor::EditMode edit_mode, int selection_start, int selection_end) {
        m_statusbar->set_text(0, String::formatted("Offset: {:#08X}", position).release_value_but_fixme_should_propagate_errors());
        m_statusbar->set_text(1, String::formatted("Edit Mode: {}", edit_mode == HexEditor::EditMode::Hex ? "Hex" : "Text").release_value_but_fixme_should_propagate_errors());
        m_statusbar->set_text(2, String::formatted("Selection Start: {}", selection_start).release_value_but_fixme_should_propagate_errors());
        m_statusbar->set_text(3, String::formatted("Selection End: {}", selection_end).release_value_but_fixme_should_propagate_errors());
        m_statusbar->set_text(4, String::formatted("Selected Bytes: {}", m_editor->selection_size()).release_value_but_fixme_should_propagate_errors());

        bool has_selection = m_editor->has_selection();
        m_copy_hex_action->set_enabled(has_selection);
        m_copy_text_action->set_enabled(has_selection);
        m_copy_as_c_code_action->set_enabled(has_selection);
        m_fill_selection_action->set_enabled(has_selection);

        if (m_value_inspector_container->is_visible() && !m_selecting_from_inspector) {
            update_inspector_values(selection_start);
        }
        m_selecting_from_inspector = false;
    };

    m_editor->on_change = [this](bool is_document_dirty) {
        window()->set_modified(is_document_dirty);
    };

    m_editor->undo_stack().on_state_change = [this] {
        m_undo_action->set_enabled(m_editor->undo_stack().can_undo());
        m_redo_action->set_enabled(m_editor->undo_stack().can_redo());
    };

    m_search_results->set_activates_on_selection(true);
    m_search_results->on_activation = [this](const GUI::ModelIndex& index) {
        if (!index.is_valid())
            return;
        auto offset = index.data(GUI::ModelRole::Custom).to_i32();
        m_last_found_index = offset;
        m_editor->set_position(offset);
        m_editor->update();
    };

    m_new_action = GUI::Action::create("New...", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"sv).release_value_but_fixme_should_propagate_errors(), [this](const GUI::Action&) {
        String value;
        if (request_close() && GUI::InputBox::show(window(), value, "Enter a size:"sv, "New File"sv, GUI::InputType::NonemptyText) == GUI::InputBox::ExecResult::OK) {
            auto file_size = AK::StringUtils::convert_to_uint(value);
            if (!file_size.has_value()) {
                GUI::MessageBox::show(window(), "Invalid file size entered."sv, "Error"sv, GUI::MessageBox::Type::Error);
                return;
            }

            if (auto error = m_editor->open_new_file(file_size.value()); error.is_error()) {
                GUI::MessageBox::show(window(), DeprecatedString::formatted("Unable to open new file: {}"sv, error.error()), "Error"sv, GUI::MessageBox::Type::Error);
                return;
            }

            set_path({});
            window()->set_modified(false);
        }
    });

    m_open_action = GUI::CommonActions::make_open_action([this](auto&) {
        if (!request_close())
            return;

        auto response = FileSystemAccessClient::Client::the().open_file(window(), { .requested_access = Core::File::OpenMode::ReadWrite });
        if (response.is_error())
            return;

        open_file(response.value().filename(), response.value().release_stream());
    });

    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (m_path.is_empty())
            return m_save_as_action->activate();

        if (auto result = m_editor->save(); result.is_error()) {
            GUI::MessageBox::show(window(), DeprecatedString::formatted("Unable to save file: {}\n"sv, result.error()), "Error"sv, GUI::MessageBox::Type::Error);
        } else {
            window()->set_modified(false);
            m_editor->update();
        }
        return;
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().save_file(window(), m_name, m_extension, Core::File::OpenMode::ReadWrite | Core::File::OpenMode::Truncate);
        if (response.is_error())
            return;
        auto file = response.release_value();
        if (auto result = m_editor->save_as(file.release_stream()); result.is_error()) {
            GUI::MessageBox::show(window(), DeprecatedString::formatted("Unable to save file: {}\n"sv, result.error()), "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }

        window()->set_modified(false);
        set_path(file.filename());
        dbgln("Wrote document to {}", file.filename());
    });

    m_undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        m_editor->undo();
    });
    m_undo_action->set_enabled(false);

    m_redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        m_editor->redo();
    });
    m_redo_action->set_enabled(false);

    m_find_action = GUI::Action::create("&Find...", { Mod_Ctrl, Key_F }, Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv).release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        auto old_buffer = m_search_buffer;
        bool find_all = false;
        if (FindDialog::show(window(), m_search_text, m_search_buffer, find_all) == GUI::InputBox::ExecResult::OK) {
            if (find_all) {
                auto matches = m_editor->find_all(m_search_buffer, 0);
                m_search_results->set_model(*new SearchResultsModel(move(matches)));
                m_search_results->update();

                if (matches.is_empty()) {
                    GUI::MessageBox::show(window(), DeprecatedString::formatted("Pattern \"{}\" not found in this file", m_search_text), "Not Found"sv, GUI::MessageBox::Type::Warning);
                    return;
                }

                GUI::MessageBox::show(window(), DeprecatedString::formatted("Found {} matches for \"{}\" in this file", matches.size(), m_search_text), DeprecatedString::formatted("{} Matches", matches.size()), GUI::MessageBox::Type::Warning);
                set_search_results_visible(true);
            } else {
                bool same_buffers = false;
                if (old_buffer.size() == m_search_buffer.size()) {
                    if (memcmp(old_buffer.data(), m_search_buffer.data(), old_buffer.size()) == 0)
                        same_buffers = true;
                }

                auto result = m_editor->find_and_highlight(m_search_buffer, same_buffers ? last_found_index() : 0);

                if (!result.has_value()) {
                    GUI::MessageBox::show(window(), DeprecatedString::formatted("Pattern \"{}\" not found in this file", m_search_text), "Not Found"sv, GUI::MessageBox::Type::Warning);
                    return;
                }

                m_last_found_index = result.value();
            }

            m_editor->update();
        }
    });

    m_goto_offset_action = GUI::Action::create("&Go to Offset...", { Mod_Ctrl, Key_G }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-to.png"sv).release_value_but_fixme_should_propagate_errors(), [this](const GUI::Action&) {
        int new_offset;
        auto result = GoToOffsetDialog::show(
            window(),
            m_goto_history,
            new_offset,
            m_editor->selection_start_offset(),
            m_editor->buffer_size());
        if (result == GUI::InputBox::ExecResult::OK) {
            m_editor->highlight(new_offset, new_offset);
            m_editor->update();
        }
    });

    m_layout_toolbar_action = GUI::Action::create_checkable("&Toolbar", [&](auto& action) {
        m_toolbar_container->set_visible(action.is_checked());
        Config::write_bool("HexEditor"sv, "Layout"sv, "ShowToolbar"sv, action.is_checked());
    });

    m_layout_search_results_action = GUI::Action::create_checkable("&Search Results", [&](auto& action) {
        set_search_results_visible(action.is_checked());
        Config::write_bool("HexEditor"sv, "Layout"sv, "ShowSearchResults"sv, action.is_checked());
    });

    m_copy_hex_action = GUI::Action::create("Copy &Hex", { Mod_Ctrl, Key_C }, Gfx::Bitmap::load_from_file("/res/icons/16x16/hex.png"sv).release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        m_editor->copy_selected_hex_to_clipboard();
    });
    m_copy_hex_action->set_enabled(false);

    m_copy_text_action = GUI::Action::create("Copy &Text", { Mod_Ctrl | Mod_Shift, Key_C }, Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"sv).release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        m_editor->copy_selected_text_to_clipboard();
    });
    m_copy_text_action->set_enabled(false);

    m_copy_as_c_code_action = GUI::Action::create("Copy as &C Code", { Mod_Alt | Mod_Shift, Key_C }, Gfx::Bitmap::load_from_file("/res/icons/16x16/c.png"sv).release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        m_editor->copy_selected_hex_to_clipboard_as_c_code();
    });
    m_copy_as_c_code_action->set_enabled(false);

    m_fill_selection_action = GUI::Action::create("Fill &Selection...", { Mod_Ctrl, Key_B }, [&](const GUI::Action&) {
        String value;
        if (GUI::InputBox::show(window(), value, "Fill byte (hex):"sv, "Fill Selection"sv, GUI::InputType::NonemptyText) == GUI::InputBox::ExecResult::OK) {
            auto fill_byte = strtol(value.bytes_as_string_view().characters_without_null_termination(), nullptr, 16);
            auto result = m_editor->fill_selection(fill_byte);
            if (result.is_error())
                GUI::MessageBox::show_error(window(), DeprecatedString::formatted("{}", result.error()));
        }
    });
    m_fill_selection_action->set_enabled(false);

    m_layout_value_inspector_action = GUI::Action::create_checkable("&Value Inspector", [&](auto& action) {
        set_value_inspector_visible(action.is_checked());
        Config::write_bool("HexEditor"sv, "Layout"sv, "ShowValueInspector"sv, action.is_checked());
    });

    m_toolbar->add_action(*m_new_action);
    m_toolbar->add_action(*m_open_action);
    m_toolbar->add_action(*m_save_action);
    m_toolbar->add_separator();
    m_toolbar->add_action(*m_undo_action);
    m_toolbar->add_action(*m_redo_action);
    m_toolbar->add_separator();
    m_toolbar->add_action(*m_find_action);
    m_toolbar->add_action(*m_goto_offset_action);

    m_statusbar->segment(0).set_clickable(true);
    m_statusbar->segment(0).set_action(*m_goto_offset_action);

    m_editor->set_focus(true);

    GUI::Application::the()->on_action_enter = [this](GUI::Action& action) {
        m_statusbar->set_override_text(action.status_tip());
    };
    GUI::Application::the()->on_action_leave = [this](GUI::Action&) {
        m_statusbar->set_override_text({});
    };
}

void HexEditorWidget::update_inspector_values(size_t position)
{
    // build out primitive types like u8, i8, u16, etc
    size_t byte_read_count = 0;
    u64 unsigned_64_bit_int = 0;
    for (int i = 0; i < 8; ++i) {
        Optional<u8> read_result = m_editor->get_byte(position + i);
        u8 current_byte = 0;
        if (!read_result.has_value())
            break;

        current_byte = read_result.release_value();
        if (m_value_inspector_little_endian)
            unsigned_64_bit_int = ((u64)current_byte << (8 * byte_read_count)) + unsigned_64_bit_int;
        else
            unsigned_64_bit_int = (unsigned_64_bit_int << 8) + current_byte;

        ++byte_read_count;
    }

    if (!m_value_inspector_little_endian) {
        // if we didn't read far enough, lets finish shifting the bytes so the code below works
        size_t bytes_left_to_read = 8 - byte_read_count;
        unsigned_64_bit_int = (unsigned_64_bit_int << (8 * bytes_left_to_read));
    }

    // Populate the model
    NonnullRefPtr<ValueInspectorModel> value_inspector_model = make_ref_counted<ValueInspectorModel>(m_value_inspector_little_endian);
    if (byte_read_count >= 1) {
        u8 unsigned_byte_value = 0;
        if (m_value_inspector_little_endian)
            unsigned_byte_value = (unsigned_64_bit_int & 0xFF);
        else
            unsigned_byte_value = (unsigned_64_bit_int >> (64 - 8)) & 0xFF;

        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::SignedByte, String::number(static_cast<i8>(unsigned_byte_value)));
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UnsignedByte, String::number(unsigned_byte_value));
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::ASCII, String::formatted("{:c}", static_cast<char>(unsigned_byte_value)));
    } else {
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::SignedByte, ""_string);
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UnsignedByte, ""_string);
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::ASCII, ""_string);
    }

    if (byte_read_count >= 2) {
        u16 unsigned_short_value = 0;
        if (m_value_inspector_little_endian)
            unsigned_short_value = (unsigned_64_bit_int & 0xFFFF);
        else
            unsigned_short_value = (unsigned_64_bit_int >> (64 - 16)) & 0xFFFF;

        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::SignedShort, String::number(static_cast<i16>(unsigned_short_value)));
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UnsignedShort, String::number(unsigned_short_value));
    } else {
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::SignedShort, ""_string);
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UnsignedShort, ""_string);
    }

    if (byte_read_count >= 4) {
        u32 unsigned_int_value = 0;
        if (m_value_inspector_little_endian)
            unsigned_int_value = (unsigned_64_bit_int & 0xFFFFFFFF);
        else
            unsigned_int_value = (unsigned_64_bit_int >> 32) & 0xFFFFFFFF;

        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::SignedInt, String::number(static_cast<i32>(unsigned_int_value)));
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UnsignedInt, String::number(unsigned_int_value));
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::Float, String::number(bit_cast<float>(unsigned_int_value)));
    } else {
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::SignedInt, ""_string);
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UnsignedInt, ""_string);
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::Float, ""_string);
    }

    if (byte_read_count >= 8) {
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::SignedLong, String::number(static_cast<i64>(unsigned_64_bit_int)));
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UnsignedLong, String::number(unsigned_64_bit_int));
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::Double, String::number(bit_cast<double>(unsigned_64_bit_int)));
    } else {
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::SignedLong, ""_string);
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UnsignedLong, ""_string);
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::Double, ""_string);
    }

    // FIXME: This probably doesn't honour endianness correctly.
    Utf8View utf8_view { ReadonlyBytes { reinterpret_cast<u8 const*>(&unsigned_64_bit_int), 4 } };
    size_t valid_bytes;
    utf8_view.validate(valid_bytes);
    if (valid_bytes == 0)
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UTF8, ""_string);
    else {
        auto utf8 = String::from_utf8(utf8_view.unicode_substring_view(0, 1).as_string());
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UTF8, move(utf8));
    }

    if (byte_read_count % 2 == 0) {
        Utf16View utf16_view { ReadonlySpan<u16> { reinterpret_cast<u16 const*>(&unsigned_64_bit_int), 4 } };
        size_t valid_code_units;
        utf16_view.validate(valid_code_units);
        if (valid_code_units == 0)
            value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UTF16, ""_string);
        else
            value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UTF16, utf16_view.unicode_substring_view(0, 1).to_utf8().release_value_but_fixme_should_propagate_errors());
    } else {
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UTF16, ""_string);
    }

    auto selected_bytes = m_editor->get_selected_bytes();

    auto ascii_string = String::from_utf8(ReadonlyBytes { selected_bytes });
    value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::ASCIIString, move(ascii_string));

    Utf8View utf8_string_view { ReadonlyBytes { selected_bytes } };
    utf8_string_view.validate(valid_bytes);
    if (valid_bytes == 0)
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UTF8String, ""_string);
    else
        // FIXME: replace control chars with something else - we don't want line breaks here ;)
        value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UTF8String, String::from_utf8(utf8_string_view.as_string()));

    // FIXME: Parse as other values like Timestamp etc

    auto decoder = TextCodec::decoder_for(m_value_inspector_little_endian ? "utf-16le"sv : "utf-16be"sv);
    ErrorOr<String> utf16_string = decoder->to_utf8(StringView(selected_bytes.span()));

    value_inspector_model->set_parsed_value(ValueInspectorModel::ValueType::UTF16String, move(utf16_string));

    m_value_inspector->set_model(value_inspector_model);
    m_value_inspector->update();
}

ErrorOr<void> HexEditorWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = window.add_menu("&File"_string);
    file_menu->add_action(*m_new_action);
    file_menu->add_action(*m_open_action);
    file_menu->add_action(*m_save_action);
    file_menu->add_action(*m_save_as_action);
    file_menu->add_separator();
    file_menu->add_recent_files_list([&](auto& action) {
        auto path = action.text();
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(&window, path);
        if (response.is_error())
            return;

        auto file = response.release_value();
        open_file(file.filename(), file.release_stream());
    });
    file_menu->add_action(GUI::CommonActions::make_quit_action([this](auto&) {
        if (!request_close())
            return;
        GUI::Application::the()->quit();
    }));

    auto edit_menu = window.add_menu("&Edit"_string);
    edit_menu->add_action(*m_undo_action);
    edit_menu->add_action(*m_redo_action);
    edit_menu->add_separator();
    edit_menu->add_action(GUI::CommonActions::make_select_all_action([this](auto&) {
        m_editor->select_all();
        m_editor->update();
    }));
    edit_menu->add_action(*m_fill_selection_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_copy_hex_action);
    edit_menu->add_action(*m_copy_text_action);
    edit_menu->add_action(*m_copy_as_c_code_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_find_action);
    edit_menu->add_action(GUI::Action::create("Find &Next", { Mod_None, Key_F3 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find-next.png"sv)), [&](const GUI::Action&) {
        if (m_search_text.is_empty() || m_search_buffer.is_empty()) {
            GUI::MessageBox::show(&window, "Nothing to search for"sv, "Not Found"sv, GUI::MessageBox::Type::Warning);
            return;
        }

        auto result = m_editor->find_and_highlight(m_search_buffer, last_found_index());
        if (!result.has_value()) {
            GUI::MessageBox::show(&window, DeprecatedString::formatted("No more matches for \"{}\" found in this file", m_search_text), "Not Found"sv, GUI::MessageBox::Type::Warning);
            return;
        }
        m_editor->update();
        m_last_found_index = result.value();
    }));

    edit_menu->add_action(GUI::Action::create("Find All &Strings", { Mod_Ctrl | Mod_Shift, Key_F }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv)), [&](const GUI::Action&) {
        int min_length = 4;
        auto matches = m_editor->find_all_strings(min_length);
        m_search_results->set_model(*new SearchResultsModel(move(matches)));
        m_search_results->update();

        if (matches.is_empty()) {
            GUI::MessageBox::show(&window, "No strings found in this file"sv, "Not Found"sv, GUI::MessageBox::Type::Warning);
            return;
        }

        set_search_results_visible(true);
        m_editor->update();
    }));
    edit_menu->add_separator();
    edit_menu->add_action(*m_goto_offset_action);

    auto view_menu = window.add_menu("&View"_string);

    auto show_toolbar = Config::read_bool("HexEditor"sv, "Layout"sv, "ShowToolbar"sv, true);
    m_layout_toolbar_action->set_checked(show_toolbar);
    m_toolbar_container->set_visible(show_toolbar);

    auto show_search_results = Config::read_bool("HexEditor"sv, "Layout"sv, "ShowSearchResults"sv, false);
    set_search_results_visible(show_search_results);

    auto show_value_inspector = Config::read_bool("HexEditor"sv, "Layout"sv, "ShowValueInspector"sv, false);
    set_value_inspector_visible(show_value_inspector);

    view_menu->add_action(*m_layout_toolbar_action);
    view_menu->add_action(*m_layout_search_results_action);
    view_menu->add_action(*m_layout_value_inspector_action);
    view_menu->add_separator();

    auto bytes_per_row = Config::read_i32("HexEditor"sv, "Layout"sv, "BytesPerRow"sv, 16);
    m_editor->set_bytes_per_row(bytes_per_row);
    m_editor->update();

    m_bytes_per_row_actions.set_exclusive(true);
    auto bytes_per_row_menu = view_menu->add_submenu("Bytes per &Row"_string);
    for (int i = 8; i <= 32; i += 8) {
        auto action = GUI::Action::create_checkable(DeprecatedString::number(i), [this, i](auto&) {
            m_editor->set_bytes_per_row(i);
            m_editor->update();
            Config::write_i32("HexEditor"sv, "Layout"sv, "BytesPerRow"sv, i);
        });
        m_bytes_per_row_actions.add_action(action);
        bytes_per_row_menu->add_action(action);
        if (i == bytes_per_row)
            action->set_checked(true);
    }

    m_value_inspector_mode_actions.set_exclusive(true);
    auto inspector_mode_menu = view_menu->add_submenu("Value Inspector &Mode"_string);
    auto little_endian_mode = GUI::Action::create_checkable("&Little Endian", [&](auto& action) {
        m_value_inspector_little_endian = action.is_checked();
        update_inspector_values(m_editor->selection_start_offset());

        Config::write_bool("HexEditor"sv, "Layout"sv, "UseLittleEndianInValueInspector"sv, m_value_inspector_little_endian);
    });
    m_value_inspector_mode_actions.add_action(little_endian_mode);
    inspector_mode_menu->add_action(little_endian_mode);

    auto big_endian_mode = GUI::Action::create_checkable("&Big Endian", [this](auto& action) {
        m_value_inspector_little_endian = !action.is_checked();
        update_inspector_values(m_editor->selection_start_offset());

        Config::write_bool("HexEditor"sv, "Layout"sv, "UseLittleEndianInValueInspector"sv, m_value_inspector_little_endian);
    });
    m_value_inspector_mode_actions.add_action(big_endian_mode);
    inspector_mode_menu->add_action(big_endian_mode);

    auto use_little_endian = Config::read_bool("HexEditor"sv, "Layout"sv, "UseLittleEndianInValueInspector"sv, true);
    m_value_inspector_little_endian = use_little_endian;

    little_endian_mode->set_checked(use_little_endian);
    big_endian_mode->set_checked(!use_little_endian);

    auto help_menu = window.add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/HexEditor.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Hex Editor"_string, GUI::Icon::default_icon("app-hex-editor"sv), &window));

    return {};
}

void HexEditorWidget::set_path(StringView path)
{
    if (path.is_empty()) {
        m_path = {};
        m_name = {};
        m_extension = {};
    } else {
        auto lexical_path = LexicalPath(path);
        m_path = lexical_path.string();
        m_name = lexical_path.title();
        m_extension = lexical_path.extension();
    }
    update_title();
}

void HexEditorWidget::update_title()
{
    StringBuilder builder;
    if (m_path.is_empty())
        builder.append("Untitled"sv);
    else
        builder.append(m_path);
    builder.append("[*] - Hex Editor"sv);
    window()->set_title(builder.to_deprecated_string());
}

void HexEditorWidget::open_file(String const& filename, NonnullOwnPtr<Core::File> file)
{
    window()->set_modified(false);
    m_editor->open_file(move(file));
    set_path(filename.to_deprecated_string());
    GUI::Application::the()->set_most_recently_open_file(filename);
}

bool HexEditorWidget::request_close()
{
    if (!window()->is_modified())
        return true;

    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_path);
    if (result == GUI::MessageBox::ExecResult::Yes) {
        m_save_action->activate();
        return !window()->is_modified();
    }
    return result == GUI::MessageBox::ExecResult::No;
}

void HexEditorWidget::set_search_results_visible(bool visible)
{
    m_layout_search_results_action->set_checked(visible);
    m_search_results_container->set_visible(visible);

    // Ensure side panel container is visible if either search result or value inspector are turned on
    m_side_panel_container->set_visible(visible || m_value_inspector_container->is_visible());
}

void HexEditorWidget::set_value_inspector_visible(bool visible)
{
    if (visible)
        update_inspector_values(m_editor->selection_start_offset());

    m_layout_value_inspector_action->set_checked(visible);
    m_value_inspector_container->set_visible(visible);

    // Ensure side panel container is visible if either search result or value inspector are turned on
    m_side_panel_container->set_visible(visible || m_search_results_container->is_visible());
}

void HexEditorWidget::drag_enter_event(GUI::DragEvent& event)
{
    auto const& mime_types = event.mime_types();
    if (mime_types.contains_slow("text/uri-list"sv))
        event.accept();
}

void HexEditorWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        window()->move_to_front();
        if (!request_close())
            return;

        // TODO: A drop event should be considered user consent for opening a file
        auto response = FileSystemAccessClient::Client::the().request_file(window(), urls.first().serialize_path(), Core::File::OpenMode::Read);
        if (response.is_error())
            return;
        open_file(response.value().filename(), response.value().release_stream());
    }
}
