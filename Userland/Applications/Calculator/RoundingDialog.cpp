/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RoundingDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextEditor.h>

RoundingDialog::ExecResult RoundingDialog::show(GUI::Window* parent_window, unsigned& rounding_value)
{
    auto dialog = RoundingDialog::construct(parent_window);

    if (parent_window) {
        dialog->set_icon(parent_window->icon());
        dialog->center_within(*parent_window);
    }

    dialog->m_rounding_spinbox->set_value(rounding_value);

    auto const result = dialog->exec();

    if (result != GUI::Dialog::ExecResult::OK)
        return result;

    rounding_value = dialog->m_rounding_spinbox->value();

    return GUI::Dialog::ExecResult::OK;
}

RoundingDialog::RoundingDialog(GUI::Window* parent_window)
    : Dialog(parent_window)
{
    resize(m_dialog_length, m_dialog_height);
    set_resizable(false);
    set_title("Choose custom rounding");

    auto& main_widget = set_main_widget<GUI::Widget>();

    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    m_rounding_spinbox = GUI::SpinBox::construct();
    m_buttons_container = GUI::Widget::construct();
    m_ok_button = GUI::DialogButton::construct("OK");
    m_cancel_button = GUI::DialogButton::construct("Cancel");

    main_widget.add_child(*m_rounding_spinbox);
    main_widget.add_child(*m_buttons_container);

    m_buttons_container->set_layout<GUI::HorizontalBoxLayout>();
    m_buttons_container->layout()->add_spacer();
    m_buttons_container->add_child(*m_ok_button);
    m_buttons_container->add_child(*m_cancel_button);

    m_rounding_spinbox->on_return_pressed = [this] {
        m_ok_button->click();
    };

    m_ok_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };

    m_cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}
