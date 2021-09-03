/*
 * Copyright (c) 2021, The SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DateTimeSettingsWindow.h"
#include <AK/Vector.h>
#include <Applications/DateTimeSettings/DateTimeSettingsWindowGML.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Widget.h>

DateTimeSettingsWindow::DateTimeSettingsWindow()
{
    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_spacing(2);
    main_widget.layout()->set_margins(4);

    auto& time_format_widget = main_widget.add<GUI::Widget>();
    time_format_widget.load_from_gml(date_time_settings_window_gml);

    m_time_format_box = *main_widget.find_descendant_of_type_named<GUI::ComboBox>("time_format_box");

    m_time_format_model.append("24-hour");
    m_time_format_model.append("12-hour");
    m_time_format_box->set_model(*GUI::ItemListModel<String>::create(m_time_format_model));

    auto commit_settings = [&](bool quit) {
        // TODO: Remember combo and time format specifier state

        if (quit)
            GUI::Application::the()->quit();
    };

    auto& bottom_widget = main_widget.add<GUI::Widget>();
    bottom_widget.set_shrink_to_fit(true);
    bottom_widget.set_layout<GUI::HorizontalBoxLayout>();
    bottom_widget.layout()->add_spacer();

    m_ok_button = bottom_widget.add<GUI::Button>("OK");
    m_ok_button->set_fixed_width(60);
    m_ok_button->on_click = [&](auto) {
        commit_settings(true);
    };

    m_cancel_button = bottom_widget.add<GUI::Button>("Cancel");
    m_cancel_button->set_fixed_width(60);
    m_cancel_button->on_click = [&](auto) {
        GUI::Application::the()->quit();
    };

    m_apply_button = bottom_widget.add<GUI::Button>("Apply");
    m_apply_button->set_fixed_width(60);
    m_apply_button->on_click = [&](auto) {
        commit_settings(false);
    };
}

DateTimeSettingsWindow::~DateTimeSettingsWindow()
{
}
