/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
    };

    enum class ShowDefaultsButton {
        Yes,
        No,
    };

    static ErrorOr<NonnullRefPtr<SettingsWindow>> create(String title, ShowDefaultsButton = ShowDefaultsButton::No);

    virtual ~SettingsWindow() override;

    template<class T, class... Args>
    ErrorOr<NonnullRefPtr<T>> add_tab(String title, Args&&... args)
    {
        auto tab = TRY(m_tab_widget->try_add_tab<T>(move(title), forward<Args>(args)...));
        TRY(m_tabs.try_append(tab));
        return tab;
    }

private:
    SettingsWindow();

    RefPtr<GUI::TabWidget> m_tab_widget;
    NonnullRefPtrVector<Tab> m_tabs;

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_apply_button;
    RefPtr<GUI::Button> m_reset_button;
};

}
