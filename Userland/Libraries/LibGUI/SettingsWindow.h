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

    virtual ~SettingsWindow() override;

    template<class T, class... Args>
    T& add_tab(StringView const& title, Args&&... args)
    {
        auto& t = m_tab_widget->add_tab<T>(title, forward<Args>(args)...);
        m_tabs.append(t);
        return t;
    }

private:
    SettingsWindow(StringView title, ShowDefaultsButton = ShowDefaultsButton::No);

    RefPtr<GUI::TabWidget> m_tab_widget;
    NonnullRefPtrVector<Tab> m_tabs;

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_apply_button;
    RefPtr<GUI::Button> m_reset_button;
};

}
