/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SettingsDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/SpinBox.h>

SettingsDialog::SettingsDialog(GUI::Window* parent, int columns, int rows, int cell_size)
    : GUI::Dialog(parent), m_columns { columns }, m_rows { rows }, m_cell_size { cell_size }
{
    set_rect({ 0, 0, 250, 175 });
    set_title("Settings");
    set_icon(parent->icon());
    set_resizable(false);

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);

    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins(4);

    // Columns
    {
        auto& name_box = main_widget.add<GUI::Widget>();
        auto& input_layout = name_box.set_layout<GUI::HorizontalBoxLayout>();
        input_layout.set_spacing(4);

        auto& name_label = name_box.add<GUI::Label>("Columns:");
        name_label.set_text_alignment(Gfx::TextAlignment::CenterRight);

        auto& columns_spinbox = name_box.add<GUI::SpinBox>();
        columns_spinbox.set_min(3);
        columns_spinbox.set_value(m_columns);
        columns_spinbox.on_change = [this, &columns_spinbox] (auto &&) {
            m_columns = columns_spinbox.value();
        };
    }

    // Rows
    {
        auto& name_box = main_widget.add<GUI::Widget>();
        auto& input_layout = name_box.set_layout<GUI::HorizontalBoxLayout>();
        input_layout.set_spacing(4);

        auto& name_label = name_box.add<GUI::Label>("Rows:   ");
        name_label.set_text_alignment(Gfx::TextAlignment::CenterRight);

        auto& rows_spinbox = name_box.add<GUI::SpinBox>();
        rows_spinbox.set_min(3);
        rows_spinbox.set_value(m_rows);
        rows_spinbox.on_change = [this, &rows_spinbox] (auto &&) {
            m_rows = rows_spinbox.value();
        };
    }


    // Cell size
    {
        auto& name_box = main_widget.add<GUI::Widget>();
        auto& input_layout = name_box.set_layout<GUI::HorizontalBoxLayout>();
        input_layout.set_spacing(4);

        auto& name_label = name_box.add<GUI::Label>("Cell size:   ");
        name_label.set_text_alignment(Gfx::TextAlignment::CenterRight);

        auto& rows_spinbox = name_box.add<GUI::SpinBox>();
        rows_spinbox.set_min(16);
        rows_spinbox.set_value(m_cell_size);
        rows_spinbox.on_change = [this, &rows_spinbox] (auto &&) {
            m_rows = rows_spinbox.value();
        };
    }

    // Ok - Cancel buttons
    {
        auto& button_box = main_widget.add<GUI::Widget>();
        auto& button_layout = button_box.set_layout<GUI::HorizontalBoxLayout>();
        button_layout.set_spacing(10);

        button_box.add<GUI::Button>("Cancel").on_click = [this](auto) {
            done(Dialog::ExecCancel);
        };

        button_box.add<GUI::Button>("OK").on_click = [this](auto) {
            done(Dialog::ExecOK);
        };
    }
}
