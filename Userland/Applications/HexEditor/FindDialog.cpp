/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FindDialog.h"
#include <AK/Array.h>
#include <AK/Hex.h>
#include <AK/StringView.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

struct Option {
    StringView title;
    OptionId opt;
    bool enabled;
    bool default_action;
};

static constexpr Array<Option, 2> options = {
    {
        { "ASCII String"sv, OptionId::AsciiString, true, true },
        { "Hex value"sv, OptionId::HexValue, true, false },
    }
};

GUI::Dialog::ExecResult FindDialog::show(GUI::Window* parent_window, String& out_text, ByteBuffer& out_buffer, bool& find_all)
{
    auto dialog_or_error = FindDialog::try_create();
    if (dialog_or_error.is_error()) {
        GUI::MessageBox::show(parent_window, "Couldn't load find dialog"sv, "Error while opening find dialog"sv, GUI::MessageBox::Type::Error);
        return ExecResult::Aborted;
    }

    auto dialog = dialog_or_error.release_value();

    if (parent_window)
        dialog->set_icon(parent_window->icon());

    if (!out_text.is_empty())
        dialog->m_text_editor->set_text(out_text);

    dialog->m_find_button->set_enabled(!dialog->m_text_editor->text().is_empty());
    dialog->m_find_all_button->set_enabled(!dialog->m_text_editor->text().is_empty());

    auto result = dialog->exec();

    if (result != ExecResult::OK)
        return result;

    auto selected_option = dialog->selected_option();
    auto processed = dialog->process_input(dialog->text_value(), selected_option);

    out_text = dialog->text_value();

    if (processed.is_error()) {
        GUI::MessageBox::show_error(parent_window, MUST(String::formatted("Input is invalid: {}", processed.release_error())));
        result = ExecResult::Aborted;
    } else {
        out_buffer = move(processed.value());
    }

    find_all = dialog->find_all();

    dbgln("Find: value={} option={} find_all={}", out_text, (int)selected_option, find_all);
    return result;
}

ErrorOr<ByteBuffer> FindDialog::process_input(StringView text_value, OptionId opt)
{
    dbgln("process_input opt={}", (int)opt);
    VERIFY(!text_value.is_empty());

    switch (opt) {
    case OptionId::AsciiString:
        return ByteBuffer::copy(text_value.bytes());
    case OptionId::HexValue: {
        auto text_no_spaces = text_value.replace(" "sv, ""sv, ReplaceMode::All);
        return decode_hex(text_no_spaces);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<NonnullRefPtr<FindDialog>> FindDialog::try_create()
{
    auto find_widget = TRY(HexEditor::FindWidget::try_create());
    auto find_dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow)
            FindDialog(move(find_widget))));
    return find_dialog;
}

FindDialog::FindDialog(NonnullRefPtr<HexEditor::FindWidget> find_widget)
    : Dialog(nullptr)
{
    resize(280, 146);
    center_on_screen();
    set_resizable(false);
    set_title("Find");

    set_main_widget(find_widget);

    m_text_editor = *find_widget->find_descendant_of_type_named<GUI::TextBox>("text_editor");
    m_find_button = *find_widget->find_descendant_of_type_named<GUI::Button>("find_button");
    m_find_all_button = *find_widget->find_descendant_of_type_named<GUI::Button>("find_all_button");
    m_cancel_button = *find_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");

    auto& radio_container = *find_widget->find_descendant_of_type_named<GUI::Widget>("radio_container");
    for (size_t i = 0; i < options.size(); i++) {
        auto action = options[i];
        auto& radio = radio_container.add<GUI::RadioButton>();
        radio.set_enabled(action.enabled);
        radio.set_text(String::from_utf8(action.title).release_value_but_fixme_should_propagate_errors());

        radio.on_checked = [this, i](auto) {
            m_selected_option = options[i].opt;
        };

        if (action.default_action) {
            radio.set_checked(true);
            m_selected_option = options[i].opt;
        }
    }

    m_text_editor->on_change = [this]() {
        m_find_button->set_enabled(!m_text_editor->text().is_empty());
        m_find_all_button->set_enabled(!m_text_editor->text().is_empty());
    };

    m_find_button->on_click = [this](auto) {
        auto text = String::from_byte_string(m_text_editor->text()).release_value_but_fixme_should_propagate_errors();
        if (!text.is_empty()) {
            m_text_value = text;
            done(ExecResult::OK);
        }
    };
    m_find_button->set_default(true);

    m_find_all_button->on_click = [this](auto) {
        m_find_all = true;
        m_find_button->click();
    };

    m_cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}
