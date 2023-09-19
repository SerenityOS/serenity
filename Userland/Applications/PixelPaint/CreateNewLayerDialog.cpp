/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CreateNewLayerDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace PixelPaint {

CreateNewLayerDialog::CreateNewLayerDialog(Gfx::IntSize suggested_size, GUI::Window* parent_window)
    : Dialog(parent_window)
{
    set_title("Create new layer");
    set_icon(parent_window->icon());
    resize(200, 200);

    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>(4);

    auto& name_label = main_widget->add<GUI::Label>("Name:"_string);
    name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_name_textbox = main_widget->add<GUI::TextBox>();
    m_name_textbox->set_text(default_layer_name);
    m_name_textbox->select_all();
    m_name_textbox->on_change = [this] {
        m_layer_name = m_name_textbox->text();
    };

    auto& width_label = main_widget->add<GUI::Label>("Width:"_string);
    width_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& width_spinbox = main_widget->add<GUI::SpinBox>();

    auto& height_label = main_widget->add<GUI::Label>("Height:"_string);
    height_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& height_spinbox = main_widget->add<GUI::SpinBox>();

    auto& button_container = main_widget->add<GUI::Widget>();
    button_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& ok_button = button_container.add<GUI::Button>("OK"_string);
    ok_button.on_click = [this](auto) {
        done(ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = button_container.add<GUI::Button>("Cancel"_string);
    cancel_button.on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    width_spinbox.on_change = [this](int value) {
        m_layer_size.set_width(value);
    };

    height_spinbox.on_change = [this](int value) {
        m_layer_size.set_height(value);
    };

    width_spinbox.set_range(1, 16384);
    height_spinbox.set_range(1, 16384);

    width_spinbox.set_value(suggested_size.width());
    height_spinbox.set_value(suggested_size.height());
}

}
