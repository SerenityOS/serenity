/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021-2022, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGUI/Button.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>

namespace GUI {

class SettingsWindow : public GUI::Window {
    C_OBJECT(SettingsWindow)
public:
    class Tab : public GUI::Widget {
    public:
        virtual void apply_settings() = 0;
        virtual void cancel_settings() { }
        virtual void reset_default_values() { }

        SettingsWindow& settings_window() { return *m_window; }
        void set_settings_window(SettingsWindow& settings_window) { m_window = settings_window; }

        void set_modified(bool modified)
        {
            if (m_window)
                m_window->set_modified(modified);
        }

    private:
        WeakPtr<SettingsWindow> m_window;
    };

    enum class ShowDefaultsButton {
        Yes,
        No,
    };

    static ErrorOr<NonnullRefPtr<SettingsWindow>> create(ByteString title, ShowDefaultsButton = ShowDefaultsButton::No);

    virtual ~SettingsWindow() override = default;

    template<class T, class... Args>
    ErrorOr<NonnullRefPtr<T>> add_tab(String title, StringView id, Args&&... args)
    {
        auto tab = TRY(T::try_create(forward<Args>(args)...));
        TRY(add_tab(tab, title, id));
        return tab;
    }

    ErrorOr<void> add_tab(NonnullRefPtr<Tab> const& tab, String title, StringView id)
    {
        tab->set_title(move(title));
        TRY(m_tab_widget->try_add_widget(*tab));
        TRY(m_tabs.try_set(id, tab));
        tab->set_settings_window(*this);
        return {};
    }

    Optional<NonnullRefPtr<Tab>> get_tab(StringView id) const;
    void set_active_tab(StringView id);

    void apply_settings();
    void cancel_settings();
    void reset_default_values();

    void set_modified(bool);

private:
    SettingsWindow() = default;

    RefPtr<GUI::TabWidget> m_tab_widget;
    HashMap<StringView, NonnullRefPtr<Tab>> m_tabs;

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_apply_button;
    RefPtr<GUI::Button> m_reset_button;
};

}
