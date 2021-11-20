/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/Widget.h>

namespace GUI {

SettingsWindow::SettingsWindow(StringView title, ShowDefaultsButton show_defaults_button)
{
    set_title(title);
    resize(400, 480);
    set_resizable(false);
    set_minimizable(false);

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins(4);
    main_widget.layout()->set_spacing(6);

    m_tab_widget = main_widget.add<GUI::TabWidget>();

    auto& button_container = main_widget.add<GUI::Widget>();
    button_container.set_shrink_to_fit(true);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->set_spacing(6);

    if (show_defaults_button == ShowDefaultsButton::Yes) {
        m_reset_button = button_container.add<GUI::Button>("Defaults");
        m_reset_button->on_click = [&](auto) {
            for (auto& tab : m_tabs) {
                tab.reset_default_values();
                tab.apply_settings();
            }
        };
    }

    button_container.layout()->add_spacer();

    m_ok_button = button_container.add<GUI::Button>("OK");
    m_ok_button->set_fixed_width(75);
    m_ok_button->on_click = [&](auto) {
        for (auto& tab : m_tabs)
            tab.apply_settings();
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
        for (auto& tab : m_tabs)
            tab.apply_settings();
    };
}

SettingsWindow::~SettingsWindow()
{
}

}
