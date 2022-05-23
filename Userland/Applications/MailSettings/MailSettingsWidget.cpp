/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailSettingsWidget.h"
#include <Applications/MailSettings/MailSettingsWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>

void MailSettingsWidget::reset_default_values()
{
    m_server_inputbox->set_text("");
    m_port_combobox->set_text("993");
    m_tls_checkbox->set_checked(false);
    m_email_inputbox->set_text("");
}

void MailSettingsWidget::apply_settings()
{
    m_server = m_server_inputbox->get_text();
    m_port = m_port_combobox->text();
    m_tls = m_tls_checkbox->is_checked();
    m_email = m_email_inputbox->get_text();

    Config::write_string("Mail", "Connection", "Server", m_server);
    Config::write_string("Mail", "Connection", "Port", m_port);
    Config::write_bool("Mail", "Connection", "TLS", m_tls);
    Config::write_string("Mail", "User", "Username", m_email);
}

MailSettingsWidget::MailSettingsWidget()
{
    // Common port values for email fetching
    m_common_ports.append("143");
    m_common_ports.append("993");

    load_from_gml(mail_settings_widget_gml);

    m_server_inputbox = *find_descendant_of_type_named<GUI::TextBox>("server_input");
    m_server_inputbox->set_text(Config::read_string("Mail", "Connection", "Server", ""));
    m_server_inputbox->on_change = [&]() {
        set_modified(true);
    };

    m_port_combobox = *find_descendant_of_type_named<GUI::ComboBox>("port_input");
    m_port_combobox->set_text(Config::read_string("Mail", "Connection", "Port", "993"));
    m_port_combobox->set_only_allow_values_from_model(false);
    m_port_combobox->set_model(*GUI::ItemListModel<String>::create(m_common_ports));
    m_port_combobox->on_change = [&](auto, auto) {
        set_modified(true);
    };

    m_tls_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("tls_input");
    m_tls_checkbox->set_checked(Config::read_bool("Mail", "Connection", "TLS", false));
    m_tls_checkbox->on_checked = [&](auto) {
        set_modified(true);
    };

    m_email_inputbox = *find_descendant_of_type_named<GUI::TextBox>("email_input");
    m_email_inputbox->set_text(Config::read_string("Mail", "User", "Username", ""));
    m_email_inputbox->on_change = [&]() {
        set_modified(true);
    };
}
