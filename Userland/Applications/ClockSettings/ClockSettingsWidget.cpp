/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClockSettingsWidget.h"
#include <Applications/ClockSettings/ClockSettingsWidgetGML.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibTimeZone/TimeZone.h>
#include <spawn.h>
#include <unistd.h>

using StringViewListModel = GUI::ItemListModel<StringView, Span<StringView const>>;

ClockSettingsWidget::ClockSettingsWidget()
{
    load_from_gml(clock_settings_widget_gml);

    static auto time_zones = TimeZone::all_time_zones();
    m_time_zone = TimeZone::system_time_zone();

    m_time_zone_combo_box = *find_descendant_of_type_named<GUI::ComboBox>("time_zone_input");
    m_time_zone_combo_box->set_only_allow_values_from_model(true);
    m_time_zone_combo_box->set_model(*StringViewListModel::create(time_zones));
    m_time_zone_combo_box->set_text(m_time_zone);
}

void ClockSettingsWidget::reset_default_values()
{
    m_time_zone = "UTC"sv;
    m_time_zone_combo_box->set_text(m_time_zone);
    set_time_zone();
}

void ClockSettingsWidget::apply_settings()
{
    m_time_zone = m_time_zone_combo_box->text();
    set_time_zone();
}

void ClockSettingsWidget::set_time_zone() const
{
    pid_t child_pid = 0;
    char const* argv[] = { "/bin/timezone", m_time_zone.characters(), nullptr };

    if ((errno = posix_spawn(&child_pid, "/bin/timezone", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        exit(1);
    }
}
