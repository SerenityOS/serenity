/*
 * Copyright (c) 2022-2022, Olivier De Cannière <olivier.decanniere96@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalendarSettingsWidget.h"
#include <AK/DateConstants.h>
#include <Applications/CalendarSettings/CalendarSettingsWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/ItemListModel.h>

void CalendarSettingsWidget::apply_settings()
{
    Config::write_string("Calendar"sv, "View"sv, "FirstDayOfWeek"sv, m_first_day_of_week_combobox->text());
    Config::write_string("Calendar"sv, "View"sv, "DefaultView"sv, m_default_view_combobox->text());
}

void CalendarSettingsWidget::reset_default_values()
{
    m_first_day_of_week_combobox->set_text("Sunday");
    m_default_view_combobox->set_text("Month");
}

CalendarSettingsWidget::CalendarSettingsWidget()
{
    load_from_gml(calendar_settings_widget_gml);

    m_first_day_of_week_combobox = *find_descendant_of_type_named<GUI::ComboBox>("first_day_of_week");
    m_first_day_of_week_combobox->set_text(Config::read_string("Calendar"sv, "View"sv, "FirstDayOfWeek"sv, "Sunday"sv));
    m_first_day_of_week_combobox->set_only_allow_values_from_model(true);
    m_first_day_of_week_combobox->set_model(*GUI::ItemListModel<StringView, Array<StringView, 7>>::create(AK::long_day_names));
    m_first_day_of_week_combobox->on_change = [&](auto, auto) {
        set_modified(true);
    };

    m_default_view_combobox = *find_descendant_of_type_named<GUI::ComboBox>("default_view");
    m_default_view_combobox->set_text(Config::read_string("Calendar"sv, "View"sv, "DefaultView"sv, "Month"sv));
    m_default_view_combobox->set_only_allow_values_from_model(true);
    m_default_view_combobox->set_model(*GUI::ItemListModel<StringView, Array<StringView, 2>>::create(m_view_modes));
    m_default_view_combobox->on_change = [&](auto, auto) {
        set_modified(true);
    };
}
