/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "AddEventDialog.h"
#include <LibCore/DateTime.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font.h>

static const char* short_month_names[] = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
};

AddEventDialog::AddEventDialog(Core::DateTime date_time, Window* parent_window)
    : Dialog(parent_window)
    , m_date_time(date_time)
{
    resize(158, 100);
    set_title("Add Event");
    set_resizable(false);
    set_icon(parent_window->icon());

    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();

    auto& top_container = widget.add<GUI::Widget>();
    top_container.set_layout<GUI::VerticalBoxLayout>();
    top_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    top_container.set_preferred_size(0, 45);
    top_container.layout()->set_margins({ 4, 4, 4, 4 });

    auto& add_label = top_container.add<GUI::Label>("Add title & date:");
    add_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    add_label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    add_label.set_preferred_size(0, 14);
    add_label.set_font(Gfx::Font::default_bold_font());

    auto& event_title_textbox = top_container.add<GUI::TextBox>();
    event_title_textbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    event_title_textbox.set_preferred_size(0, 20);

    auto& middle_container = widget.add<GUI::Widget>();
    middle_container.set_layout<GUI::HorizontalBoxLayout>();
    middle_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    middle_container.set_preferred_size(0, 25);
    middle_container.layout()->set_margins({ 4, 4, 4, 4 });

    auto& starting_month_combo = middle_container.add<GUI::ComboBox>();
    starting_month_combo.set_only_allow_values_from_model(true);
    starting_month_combo.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    starting_month_combo.set_preferred_size(50, 20);
    starting_month_combo.set_model(MonthListModel::create());
    starting_month_combo.set_selected_index(m_date_time.month() - 1);

    auto& starting_day_combo = middle_container.add<GUI::SpinBox>();
    starting_day_combo.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    starting_day_combo.set_preferred_size(40, 20);
    starting_day_combo.set_value(m_date_time.day());
    starting_day_combo.set_min(1);

    auto& starting_year_combo = middle_container.add<GUI::SpinBox>();
    starting_year_combo.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    starting_year_combo.set_preferred_size(55, 20);
    starting_year_combo.set_range(0, 9999);
    starting_year_combo.set_value(m_date_time.year());

    widget.layout()->add_spacer();

    auto& button_container = widget.add<GUI::Widget>();
    button_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    button_container.set_preferred_size(0, 20);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->add_spacer();
    auto& ok_button = button_container.add<GUI::Button>("OK");
    ok_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    ok_button.set_preferred_size(80, 20);
    ok_button.on_click = [this](auto) {
        dbgln("TODO: Add event icon on specific tile");
        done(Dialog::ExecOK);
    };

    event_title_textbox.set_focus(true);
}

AddEventDialog::~AddEventDialog()
{
}

AddEventDialog::MonthListModel::MonthListModel()
{
}

AddEventDialog::MonthListModel::~MonthListModel()
{
}

void AddEventDialog::MonthListModel::update()
{
}

int AddEventDialog::MonthListModel::row_count(const GUI::ModelIndex&) const
{
    return 12;
}

String AddEventDialog::MonthListModel::column_name(int column) const
{
    switch (column) {
    case Column::Month:
        return "Month";
    default:
        ASSERT_NOT_REACHED();
    }
}

GUI::Variant AddEventDialog::MonthListModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto& month = short_month_names[index.row()];
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Month:
            return month;
        default:
            ASSERT_NOT_REACHED();
        }
    }
    return {};
}
