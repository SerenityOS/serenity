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
#include <Applications/Calendar/AddEventDialogGML.h>
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
#include <LibGfx/FontDatabase.h>

static const char* short_month_names[] = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
};

Optional<GUI::Calendar::Event> AddEventDialog::show(Core::DateTime date_time, Window* parent_window)
{
    auto dialog = AddEventDialog::construct(date_time, parent_window);
    auto rc = dialog->exec();
    if (rc == ExecResult::ExecOK)
        return dialog->m_event;

    return {};
}

AddEventDialog::AddEventDialog(Core::DateTime date_time, Window* parent_window)
    : Dialog(parent_window)
    , m_event({ {}, date_time })
{
    resize(160, 120);
    set_title("Add Event");
    set_resizable(false);
    set_icon(parent_window->icon());

    auto& root_container = set_main_widget<GUI::Widget>();
    root_container.load_from_gml(add_event_dialog_gml);

    auto& title_box = *find_descendant_of_type_named<GUI::TextBox>("title_box");

    auto& month_combo = *find_descendant_of_type_named<GUI::ComboBox>("month_combo");
    month_combo.set_model(MonthListModel::create());
    month_combo.set_selected_index(m_event.date_time.month() - 1);

    auto& day_combo = *find_descendant_of_type_named<GUI::SpinBox>("day_combo");
    day_combo.set_value(m_event.date_time.day());

    auto& year_combo = *find_descendant_of_type_named<GUI::SpinBox>("year_combo");
    year_combo.set_value(m_event.date_time.year());

    auto& ok_button = *find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&]() {
        m_event.title = title_box.text().is_empty() ? "Unnamed" : title_box.text();
        auto month = month_combo.selected_index() + 1;
        m_event.date_time = Core::DateTime::create(year_combo.value(), month, day_combo.value());
        done(Dialog::ExecOK);
    };
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
        VERIFY_NOT_REACHED();
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
            VERIFY_NOT_REACHED();
        }
    }
    return {};
}
