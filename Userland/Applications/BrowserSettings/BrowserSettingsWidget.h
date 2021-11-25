/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextBox.h>

class BrowserSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(BrowserSettingsWidget)
public:
    virtual ~BrowserSettingsWidget() override;

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    BrowserSettingsWidget();

    RefPtr<GUI::TextBox> m_homepage_url_textbox;
    void set_color_scheme(StringView);
    RefPtr<GUI::ComboBox> m_color_scheme_combobox;
    RefPtr<GUI::CheckBox> m_show_bookmarks_bar_checkbox;
    RefPtr<GUI::CheckBox> m_auto_close_download_windows_checkbox;

    void set_search_engine_url(StringView);
    bool m_is_custom_search_engine { false };
    RefPtr<GUI::CheckBox> m_enable_search_engine_checkbox;
    RefPtr<GUI::Widget> m_search_engine_combobox_group;
    RefPtr<GUI::ComboBox> m_search_engine_combobox;
    RefPtr<GUI::Widget> m_custom_search_engine_group;
    RefPtr<GUI::TextBox> m_custom_search_engine_textbox;
};
