/*
 * Copyright (c) 2021, The SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Window.h>

class MailSettingsWindow final : public GUI::Window {
    C_OBJECT(MailSettingsWindow)

private:
    MailSettingsWindow();

    void reset_default_values();
    void write_values();

    String m_server;
    String m_port;
    bool m_tls { false };
    String m_email;
    Vector<String> m_common_ports;

    RefPtr<GUI::TextBox> m_server_inputbox;
    RefPtr<GUI::ComboBox> m_port_combobox;
    RefPtr<GUI::CheckBox> m_tls_checkbox;
    RefPtr<GUI::TextBox> m_email_inputbox;

    RefPtr<GUI::Button> m_reset_button;
    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_apply_button;
};
