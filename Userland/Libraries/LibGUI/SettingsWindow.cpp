/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021-2022, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/Widget.h>

namespace GUI {

void SettingsWindow::set_modified(bool modified)
{
    Window::set_modified(modified);
    if (m_apply_button)
        m_apply_button->set_enabled(modified);
}

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
        window->m_reset_button->set_fixed_width(75);
        window->m_reset_button->on_click = [window = window->make_weak_ptr<SettingsWindow>()](auto) mutable {
            window->reset_default_values();
        };
    }

    TRY(button_container->layout()->try_add_spacer());

    window->m_ok_button = TRY(button_container->try_add<GUI::Button>("OK"));
    window->m_ok_button->set_fixed_width(75);
    window->m_ok_button->on_click = [window = window->make_weak_ptr<SettingsWindow>()](auto) mutable {
        window->apply_settings();
        GUI::Application::the()->quit();
    };

    window->m_cancel_button = TRY(button_container->try_add<GUI::Button>("Cancel"));
    window->m_cancel_button->set_fixed_width(75);
    window->m_cancel_button->on_click = [window = window->make_weak_ptr<SettingsWindow>()](auto) mutable {
        window->cancel_settings();
        GUI::Application::the()->quit();
    };

    window->m_apply_button = TRY(button_container->try_add<GUI::Button>("Apply"));
    window->m_apply_button->set_fixed_width(75);
    window->m_apply_button->on_click = [window = window->make_weak_ptr<SettingsWindow>()](auto) mutable {
        window->apply_settings();
    };

    window->on_close_request = [window = window->make_weak_ptr<SettingsWindow>()]() mutable -> Window::CloseRequestDecision {
        if (!window->is_modified())
            return Window::CloseRequestDecision::Close;

        auto result = MessageBox::show(window, "Apply these settings before closing?", "Unsaved changes", MessageBox::Type::Warning, MessageBox::InputType::YesNoCancel);
        switch (result) {
        case MessageBox::ExecResult::Yes:
            window->apply_settings();
            return Window::CloseRequestDecision::Close;
        case MessageBox::ExecResult::No:
            window->cancel_settings();
            return Window::CloseRequestDecision::Close;
        default:
            return Window::CloseRequestDecision::StayOpen;
        }
    };

    return window;
}

Optional<NonnullRefPtr<SettingsWindow::Tab>> SettingsWindow::get_tab(StringView id) const
{
    auto tab = m_tabs.find(id);
    if (tab == m_tabs.end())
        return {};
    return tab->value;
}

void SettingsWindow::set_active_tab(StringView id)
{
    if (auto tab = get_tab(id); tab.has_value())
        m_tab_widget->set_active_widget(tab.value());
}

void SettingsWindow::apply_settings()
{
    for (auto& [id, tab] : m_tabs)
        tab->apply_settings();
    set_modified(false);
}

void SettingsWindow::cancel_settings()
{
    for (auto& [id, tab] : m_tabs)
        tab->cancel_settings();
}

void SettingsWindow::reset_default_values()
{
    for (auto& [id, tab] : m_tabs) {
        tab->reset_default_values();
        tab->apply_settings();
    }
    set_modified(false);
}

}
