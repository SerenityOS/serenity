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

ErrorOr<NonnullRefPtr<SettingsWindow>> SettingsWindow::create(String title, ShowDefaultsButton show_defaults_button)
{
    auto window = TRY(SettingsWindow::try_create());

    window->set_title(move(title));
    window->resize(400, 480);
    window->set_resizable(false);
    window->set_minimizable(false);

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->set_fill_with_background_color(true);
    (void)TRY(main_widget->try_set_layout<GUI::VerticalBoxLayout>());
    main_widget->layout()->set_margins(4);
    main_widget->layout()->set_spacing(6);

    window->m_tab_widget = TRY(main_widget->try_add<GUI::TabWidget>());

    auto button_container = TRY(main_widget->try_add<GUI::Widget>());
    button_container->set_shrink_to_fit(true);
    (void)TRY(button_container->try_set_layout<GUI::HorizontalBoxLayout>());
    button_container->layout()->set_spacing(6);

    if (show_defaults_button == ShowDefaultsButton::Yes) {
        window->m_reset_button = TRY(button_container->try_add<GUI::Button>("Defaults"));
        window->m_reset_button->on_click = [window = window->make_weak_ptr<SettingsWindow>()](auto) mutable {
            for (auto& tab : window->m_tabs) {
                tab.reset_default_values();
                tab.apply_settings();
            }
        };
    }

    TRY(button_container->layout()->try_add_spacer());

    window->m_ok_button = TRY(button_container->try_add<GUI::Button>("OK"));
    window->m_ok_button->set_fixed_width(75);
    window->m_ok_button->on_click = [window = window->make_weak_ptr<SettingsWindow>()](auto) mutable {
        for (auto& tab : window->m_tabs)
            tab.apply_settings();
        GUI::Application::the()->quit();
    };

    window->m_cancel_button = TRY(button_container->try_add<GUI::Button>("Cancel"));
    window->m_cancel_button->set_fixed_width(75);
    window->m_cancel_button->on_click = [window = window->make_weak_ptr<SettingsWindow>()](auto) mutable {
        for (auto& tab : window->m_tabs)
            tab.cancel_settings();
        GUI::Application::the()->quit();
    };

    window->m_apply_button = TRY(button_container->try_add<GUI::Button>("Apply"));
    window->m_apply_button->set_fixed_width(75);
    window->m_apply_button->on_click = [window = window->make_weak_ptr<SettingsWindow>()](auto) mutable {
        for (auto& tab : window->m_tabs)
            tab.apply_settings();
    };

    return window;
}

SettingsWindow::SettingsWindow()
{
}

SettingsWindow::~SettingsWindow()
{
}

}
