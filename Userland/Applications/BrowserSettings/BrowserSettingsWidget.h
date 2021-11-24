/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/CheckBox.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextBox.h>

class BrowserSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(BrowserSettingsWidget)
public:
    virtual ~BrowserSettingsWidget() override;

    virtual void apply_settings() override;

private:
    BrowserSettingsWidget();

    RefPtr<GUI::TextBox> m_homepage_url_textbox;
    RefPtr<GUI::CheckBox> m_show_bookmarks_bar_checkbox;
    RefPtr<GUI::CheckBox> m_auto_close_download_windows_checkbox;
};
