/*
 * Copyright (c) 2021, The SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailSettingsWindow.h"
#include <Applications/MailSettings/MailSettingsWindowGML.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <unistd.h>

void MailSettingsWindow::reset_default_values()
{
    m_server_inputbox->set_text("");
    m_port_combobox->set_text("993");
    m_tls_checkbox->set_checked(false);
    m_email_inputbox->set_text("");
}

void MailSettingsWindow::write_values()
{
    m_server = m_server_inputbox->get_text();
    m_port = m_port_combobox->text();
    m_tls = m_tls_checkbox->is_checked();
    m_email = m_email_inputbox->get_text();

    m_config->write_entry("Connection", "Server", m_server);
    m_config->write_entry("Connection", "Port", m_port);
    m_config->write_bool_entry("Connection", "TLS", m_tls);
    m_config->write_entry("User", "Username", m_email);
    m_config->sync();
}

MailSettingsWindow::MailSettingsWindow()
{
    m_config = Core::ConfigFile::get_for_app("Mail", Core::ConfigFile::AllowWriting::Yes);
    if (unveil(m_config->filename().characters(), "rwc") < 0) {
        perror("unveil");
        GUI::Application::the()->quit();
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        GUI::Application::the()->quit();
    }

    if (unveil(nullptr, nullptr)) {
        perror("unveil");
        GUI::Application::the()->quit();
    }

    //Common port values for email fetching
    m_common_ports.append("143");
    m_common_ports.append("993");

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins(4);
    main_widget.layout()->set_spacing(6);

    auto& tab_widget = main_widget.add<GUI::TabWidget>();
    auto& mail_widget = tab_widget.add_tab<GUI::Widget>("Mail");
    mail_widget.load_from_gml(mail_settings_window_gml);

    auto& server_settings_image_label = *main_widget.find_descendant_of_type_named<GUI::Label>("server_settings_image_label");
    server_settings_image_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/graphics/mail-server-settings.png"));

    auto& user_settings_image_label = *main_widget.find_descendant_of_type_named<GUI::Label>("user_settings_image_label");
    user_settings_image_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/graphics/mail-user-settings.png"));

    m_server_inputbox = *main_widget.find_descendant_of_type_named<GUI::TextBox>("server_input");
    m_server_inputbox->set_text(m_config->read_entry("Connection", "Server", ""));

    m_port_combobox = *main_widget.find_descendant_of_type_named<GUI::ComboBox>("port_input");
    m_port_combobox->set_text(m_config->read_entry("Connection", "Port", "993"));
    m_port_combobox->set_only_allow_values_from_model(false);
    m_port_combobox->set_model(*GUI::ItemListModel<String>::create(m_common_ports));

    m_tls_checkbox = *main_widget.find_descendant_of_type_named<GUI::CheckBox>("tls_input");
    m_tls_checkbox->set_checked(m_config->read_bool_entry("Connection", "TLS", false));

    m_email_inputbox = *main_widget.find_descendant_of_type_named<GUI::TextBox>("email_input");
    m_email_inputbox->set_text(m_config->read_entry("User", "Username", ""));

    auto& button_container = main_widget.add<GUI::Widget>();
    button_container.set_shrink_to_fit(true);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->set_spacing(6);

    m_reset_button = button_container.add<GUI::Button>("Defaults");
    m_reset_button->set_fixed_width(75);
    m_reset_button->on_click = [this](auto) {
        reset_default_values();
    };

    button_container.layout()->add_spacer();

    m_ok_button = button_container.add<GUI::Button>("OK");
    m_ok_button->set_fixed_width(75);
    m_ok_button->on_click = [&](auto) {
        write_values();
        GUI::Application::the()->quit();
    };

    m_cancel_button = button_container.add<GUI::Button>("Cancel");
    m_cancel_button->set_fixed_width(75);
    m_cancel_button->on_click = [&](auto) {
        GUI::Application::the()->quit();
    };

    m_apply_button = button_container.add<GUI::Button>("Apply");
    m_apply_button->set_fixed_width(75);
    m_apply_button->on_click = [&](auto) {
        write_values();
    };
}
