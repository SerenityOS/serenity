/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RoundingDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextEditor.h>

RoundingDialog::ExecResult RoundingDialog::show(GUI::Window* parent_window, unsigned& rounding_value)
{
    auto dialog = RoundingDialog::construct();

    if (parent_window)
        dialog->set_icon(parent_window->icon());

    dialog->m_text_editor->set_text(String::number(rounding_value));
    dialog->m_text_editor->select_all();

    auto result = dialog->exec();

    if (result != GUI::Dialog::ExecResult::OK)
        return result;

    rounding_value = dialog->m_text_editor->get_text().to_uint().value();

    return GUI::Dialog::ExecResult::OK;
}

RoundingDialog::RoundingDialog()
    : Dialog(nullptr)
{
    resize(m_dialog_length, m_dialog_height);
    set_resizable(false);
    set_title("Choose custom rounding");

    auto& main_widget = set_main_widget<GUI::Widget>();

    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    m_error_label = GUI::Label::construct("Error: Please enter a correct value");
    m_text_editor = GUI::TextEditor::construct(GUI::TextEditor::Type::SingleLine);
    m_ok_button = GUI::Button::construct("Ok");

    main_widget.add_child(*m_error_label);
    main_widget.add_child(*m_text_editor);
    main_widget.add_child(*m_ok_button);

    m_text_editor->on_return_pressed = [this] {
        m_ok_button->click();
    };

    m_ok_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };

    m_text_editor->on_change = [this]() {
        bool const valid_data = m_text_editor->get_text().to_uint().has_value();
        m_ok_button->set_enabled(valid_data);
        m_error_label->set_visible(!valid_data);
        if (valid_data)
            resize(m_dialog_length, m_dialog_height);
        else
            resize(m_dialog_length, m_dialog_height_on_error);
    };
}
