/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MouseSettingsWindow.h"
#include "MouseWidget.h"
#include "ThemeWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Widget.h>

MouseSettingsWindow::MouseSettingsWindow()
{
    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins(4);
    main_widget.layout()->set_spacing(6);

    auto& tab_widget = main_widget.add<GUI::TabWidget>();

    auto& mouse_widget = tab_widget.add_tab<MouseWidget>("Mouse");
    auto& theme_widget = tab_widget.add_tab<ThemeWidget>("Cursor Theme");

    auto& button_container = main_widget.add<GUI::Widget>();
    button_container.set_shrink_to_fit(true);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->set_spacing(6);

    m_reset_button = button_container.add<GUI::Button>("Defaults");
    m_reset_button->on_click = [&](auto) {
        mouse_widget.reset_default_values();
        theme_widget.reset_default_values();
    };

    button_container.layout()->add_spacer();

    m_ok_button = button_container.add<GUI::Button>("OK");
    m_ok_button->set_fixed_width(75);
    m_ok_button->on_click = [&](auto) {
        mouse_widget.update_window_server();
        theme_widget.update_window_server();
        GUI::Application::the()->quit();
    };

    m_cancel_button = button_container.add<GUI::Button>("Cancel");
    m_cancel_button->set_fixed_width(75);
    m_cancel_button->on_click = [&](auto) {
        GUI::Application::the()->quit();
    };

    m_apply_button = button_container.add<GUI::Button>("Apply");
    m_apply_button->set_fixed_width(75);
    m_apply_button->on_click = [&](auto) {
        mouse_widget.update_window_server();
        theme_widget.update_window_server();
    };
}

MouseSettingsWindow::~MouseSettingsWindow()
{
}
