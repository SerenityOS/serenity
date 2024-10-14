/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GoToOffsetDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

GUI::Dialog::ExecResult GoToOffsetDialog::show(GUI::Window* parent_window, int& history_offset, int& out_offset, int selection_offset, int buffer_size)
{
    auto dialog_or_error = GoToOffsetDialog::try_create();
    if (dialog_or_error.is_error()) {
        GUI::MessageBox::show(parent_window, "Couldn't load \"go to offset\" dialog"sv, "Error while opening \"go to offset\" dialog"sv, GUI::MessageBox::Type::Error);
        return ExecResult::Aborted;
    }

    auto dialog = dialog_or_error.release_value();

    dialog->m_selection_offset = selection_offset;
    dialog->m_buffer_size = buffer_size;

    if (parent_window)
        dialog->set_icon(parent_window->icon());

    if (history_offset)
        dialog->m_text_editor->set_text(String::number(history_offset));

    auto result = dialog->exec();

    if (result != ExecResult::OK)
        return result;

    auto input_offset = dialog->process_input();
    history_offset = move(input_offset);

    auto new_offset = dialog->calculate_new_offset(input_offset);
    dbgln("Go to offset: value={}", new_offset);
    out_offset = move(new_offset);

    return ExecResult::OK;
}

int GoToOffsetDialog::process_input()
{
    auto input_offset = String::from_byte_string(m_text_editor->text().trim_whitespace()).release_value_but_fixme_should_propagate_errors();
    int offset;
    auto type = m_offset_type_box->text().trim_whitespace();
    if (type == "Decimal") {
        offset = input_offset.to_number<int>().value_or(0);
    } else if (type == "Hexadecimal") {
        // FIXME: Find a better way of parsing hex to a number that doesn't require a zero terminated string
        offset = strtol(input_offset.to_byte_string().characters(), nullptr, 16);
    } else {
        VERIFY_NOT_REACHED();
    }
    return offset;
}

int GoToOffsetDialog::calculate_new_offset(int input_offset)
{
    int new_offset;
    auto from = m_offset_from_box->text().trim_whitespace();
    if (from == "Start") {
        new_offset = input_offset;
    } else if (from == "End") {
        new_offset = m_buffer_size - input_offset;
    } else if (from == "Here") {
        new_offset = input_offset + m_selection_offset;
    } else {
        VERIFY_NOT_REACHED();
    }

    if (new_offset > m_buffer_size)
        new_offset = m_buffer_size;
    if (new_offset < 0)
        new_offset = 0;

    return new_offset;
}

void GoToOffsetDialog::update_statusbar()
{
    auto new_offset = calculate_new_offset(process_input());
    m_statusbar->set_text(0, String::formatted("HEX: {:#08X}", new_offset).release_value_but_fixme_should_propagate_errors());
    m_statusbar->set_text(1, String::formatted("DEC: {}", new_offset).release_value_but_fixme_should_propagate_errors());
}

ErrorOr<NonnullRefPtr<GoToOffsetDialog>> GoToOffsetDialog::try_create()
{
    auto offset_widget = TRY(HexEditor::GoToOffsetWidget::try_create());
    auto offset_dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow)
            GoToOffsetDialog(move(offset_widget))));
    return offset_dialog;
}

GoToOffsetDialog::GoToOffsetDialog(NonnullRefPtr<HexEditor::GoToOffsetWidget> goto_offset_widget)
    : Dialog(nullptr)
{
    resize(300, 80);
    center_on_screen();
    set_resizable(false);
    set_title("Go to Offset");

    set_main_widget(goto_offset_widget);

    m_text_editor = *goto_offset_widget->find_descendant_of_type_named<GUI::TextBox>("text_editor");
    m_go_button = *goto_offset_widget->find_descendant_of_type_named<GUI::Button>("go_button");
    m_offset_type_box = *goto_offset_widget->find_descendant_of_type_named<GUI::ComboBox>("offset_type");
    m_offset_from_box = *goto_offset_widget->find_descendant_of_type_named<GUI::ComboBox>("offset_from");
    m_statusbar = *goto_offset_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    m_offset_type.append("Decimal"sv);
    m_offset_type.append("Hexadecimal"sv);
    m_offset_type_box->set_model(GUI::ItemListModel<StringView>::create(m_offset_type));
    m_offset_type_box->set_selected_index(0);
    m_offset_type_box->set_only_allow_values_from_model(true);

    m_offset_from.append("Start"sv);
    m_offset_from.append("Here"sv);
    m_offset_from.append("End"sv);
    m_offset_from_box->set_model(GUI::ItemListModel<StringView>::create(m_offset_from));
    m_offset_from_box->set_selected_index(0);
    m_offset_from_box->set_only_allow_values_from_model(true);

    m_go_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };
    m_go_button->set_default(true);

    m_text_editor->on_change = [this]() {
        auto text = m_text_editor->text();
        if (text.starts_with("0x"sv)) {
            m_offset_type_box->set_selected_index(1);
            m_text_editor->set_text(text.replace("0x"sv, ""sv, ReplaceMode::FirstOnly));
        }
        update_statusbar();
    };

    m_offset_type_box->on_change = [this](auto&, auto&) {
        update_statusbar();
    };

    m_offset_from_box->on_change = [this](auto&, auto&) {
        update_statusbar();
    };

    update_statusbar();
}
