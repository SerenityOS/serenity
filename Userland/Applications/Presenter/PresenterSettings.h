/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/CheckBox.h>
#include <LibGUI/SettingsWindow.h>

class PresenterSettingsFooterWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT(PresenterSettingsFooterWidget)
public:
    virtual void apply_settings() override;
    virtual void cancel_settings() override;

private:
    PresenterSettingsFooterWidget();

    void on_footer_settings_override_change();

    RefPtr<GUI::TextEditor> m_footer_text;
    RefPtr<GUI::CheckBox> m_enable_footer;
    RefPtr<GUI::CheckBox> m_override_footer;
};
