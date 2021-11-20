/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>

class MailSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(MailSettingsWidget)

public:
    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    MailSettingsWidget();

    String m_server;
    String m_port;
    bool m_tls { false };
    String m_email;
    Vector<String> m_common_ports;

    RefPtr<GUI::TextBox> m_server_inputbox;
    RefPtr<GUI::ComboBox> m_port_combobox;
    RefPtr<GUI::CheckBox> m_tls_checkbox;
    RefPtr<GUI::TextBox> m_email_inputbox;
};
