/*
 * Copyright (c) 2022-2022, Olivier De Cannière <olivier.decanniere96@gmail.com>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Widget.h"
#include <AK/DateConstants.h>
#include <Applications/CalendarSettings/CalendarSettingsWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/SpinBox.h>

namespace CalendarSettings {

void Widget::apply_settings()
{
    Config::write_string("Calendar"sv, "View"sv, "FirstDayOfWeek"sv, m_first_day_of_week_combobox->text());
    Config::write_string("Calendar"sv, "View"sv, "FirstDayOfWeekend"sv, m_first_day_of_weekend_combobox->text());
    Config::write_i32("Calendar"sv, "View"sv, "WeekendLength"sv, m_weekend_length_spinbox->value());
    Config::write_string("Calendar"sv, "View"sv, "DefaultView"sv, m_default_view_combobox->text());
}

void Widget::reset_default_values()
{
    m_first_day_of_week_combobox->set_text("Sunday");
    m_first_day_of_weekend_combobox->set_text("Saturday");
    m_weekend_length_spinbox->set_value(2);
    m_default_view_combobox->set_text("Month");
}

ErrorOr<NonnullRefPtr<Widget>> Widget::create()
{
    auto widget = TRY(Widget::try_create());

    widget->m_first_day_of_week_combobox = *widget->find_descendant_of_type_named<GUI::ComboBox>("first_day_of_week");
    widget->m_first_day_of_week_combobox->set_text(Config::read_string("Calendar"sv, "View"sv, "FirstDayOfWeek"sv, "Sunday"sv));
    widget->m_first_day_of_week_combobox->set_only_allow_values_from_model(true);
    widget->m_first_day_of_week_combobox->set_model(*TRY((GUI::ItemListModel<StringView, Array<StringView, 7>>::try_create(AK::long_day_names))));
    widget->m_first_day_of_week_combobox->on_change = [&, self = NonnullRefPtr<Widget>(widget)](auto, auto) {
        self->set_modified(true);
    };

    widget->m_first_day_of_weekend_combobox = *widget->find_descendant_of_type_named<GUI::ComboBox>("first_day_of_weekend");
    widget->m_first_day_of_weekend_combobox->set_text(Config::read_string("Calendar"sv, "View"sv, "FirstDayOfWeekend"sv, "Saturday"sv));
    widget->m_first_day_of_weekend_combobox->set_only_allow_values_from_model(true);
    widget->m_first_day_of_weekend_combobox->set_model(*TRY((GUI::ItemListModel<StringView, Array<StringView, 7>>::try_create(AK::long_day_names))));
    widget->m_first_day_of_weekend_combobox->on_change = [&, self = NonnullRefPtr<Widget>(widget)](auto, auto) {
        self->set_modified(true);
    };

    widget->m_weekend_length_spinbox = *widget->find_descendant_of_type_named<GUI::SpinBox>("weekend_length");
    widget->m_weekend_length_spinbox->set_value(Config::read_i32("Calendar"sv, "View"sv, "WeekendLength"sv, 2));
    widget->m_weekend_length_spinbox->on_change = [&, self = NonnullRefPtr<Widget>(widget)](auto) {
        self->set_modified(true);
    };

    widget->m_default_view_combobox = *widget->find_descendant_of_type_named<GUI::ComboBox>("default_view");
    widget->m_default_view_combobox->set_text(Config::read_string("Calendar"sv, "View"sv, "DefaultView"sv, "Month"sv));
    widget->m_default_view_combobox->set_only_allow_values_from_model(true);
    widget->m_default_view_combobox->set_model(*TRY((GUI::ItemListModel<StringView, Array<StringView, 2>>::try_create(m_view_modes))));
    widget->m_default_view_combobox->on_change = [&, self = NonnullRefPtr<Widget>(widget)](auto, auto) {
        self->set_modified(true);
    };

    return widget;
}

}
