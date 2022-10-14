/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NetworkSettingsWidget.h"

#include <AK/IPv4Address.h>
#include <AK/JsonParser.h>
#include <AK/String.h>
#include <Applications/NetworkSettings/NetworkSettingsGML.h>
#include <LibCore/Command.h>
#include <LibCore/File.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Process.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace NetworkSettings {

static int netmask_to_cidr(IPv4Address const& address)
{
    auto address_in_host_representation = AK::convert_between_host_and_network_endian(address.to_u32());
    return 32 - count_trailing_zeroes_safe(address_in_host_representation);
}

NetworkSettingsWidget::NetworkSettingsWidget()
{
    load_from_gml(network_settings_gml);

    m_adapters_combobox = *find_descendant_of_type_named<GUI::ComboBox>("adapters_combobox");
    m_enabled_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("enabled_checkbox");
    m_enabled_checkbox->on_checked = [&](bool value) {
        m_current_adapter_data->enabled = value;
        on_switch_enabled_or_dhcp();
        set_modified(true);
    };
    m_dhcp_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("dhcp_checkbox");
    m_dhcp_checkbox->on_checked = [&](bool value) {
        m_current_adapter_data->dhcp = value;
        on_switch_enabled_or_dhcp();
        set_modified(true);
    };
    m_ip_address_textbox = *find_descendant_of_type_named<GUI::TextBox>("ip_address_textbox");
    m_ip_address_textbox->on_change = [&]() {
        m_current_adapter_data->ip_address = m_ip_address_textbox->text();
        set_modified(true);
    };
    m_cidr_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("cidr_spinbox");
    m_cidr_spinbox->on_change = [&](int value) {
        m_current_adapter_data->cidr = value;
        set_modified(true);
    };
    m_default_gateway_textbox = *find_descendant_of_type_named<GUI::TextBox>("default_gateway_textbox");
    m_default_gateway_textbox->on_change = [&]() {
        m_current_adapter_data->default_gateway = m_default_gateway_textbox->text();
        set_modified(true);
    };

    auto config_file = Core::ConfigFile::open_for_system("Network").release_value_but_fixme_should_propagate_errors();

    auto proc_net_adapters_file = Core::Stream::File::open("/sys/kernel/net/adapters"sv, Core::Stream::OpenMode::Read).release_value_but_fixme_should_propagate_errors();
    auto data = proc_net_adapters_file->read_all().release_value_but_fixme_should_propagate_errors();
    JsonParser parser(data);
    JsonValue proc_net_adapters_json = parser.parse().release_value_but_fixme_should_propagate_errors();

    // FIXME: This should be done before creating a window.
    if (proc_net_adapters_json.as_array().is_empty()) {
        GUI::MessageBox::show_error(window(), "No network adapters found!"sv);
        ::exit(1);
    }

    size_t selected_adapter_index = 0;
    size_t index = 0;
    proc_net_adapters_json.as_array().for_each([&](auto& value) {
        auto& if_object = value.as_object();
        auto adapter_name = if_object.get("name"sv).to_string();
        if (adapter_name == "loop")
            return;

        bool adapter_exists_in_config = config_file->has_group(adapter_name);

        bool enabled = config_file->read_bool_entry(adapter_name, "Enabled", true);
        if (enabled)
            selected_adapter_index = index;

        NetworkAdapterData adapter_data;
        adapter_data.enabled = enabled;
        adapter_data.dhcp = config_file->read_bool_entry(adapter_name, "DHCP", !adapter_exists_in_config);
        adapter_data.ip_address = config_file->read_entry(adapter_name, "IPv4Address");
        auto netmask = IPv4Address::from_string(config_file->read_entry(adapter_name, "IPv4Netmask"));
        adapter_data.cidr = netmask.has_value() ? netmask_to_cidr(*netmask) : 32;
        adapter_data.default_gateway = config_file->read_entry(adapter_name, "IPv4Gateway");
        m_network_adapters.set(adapter_name, move(adapter_data));
        m_adapter_names.append(adapter_name);
        index++;
    });

    m_adapters_combobox->set_model(GUI::ItemListModel<String>::create(m_adapter_names));
    m_adapters_combobox->on_change = [this](String const& text, GUI::ModelIndex const&) {
        on_switch_adapter(text);
    };
    auto const& selected_adapter = selected_adapter_index;
    dbgln("{} in {}", selected_adapter, m_adapter_names);
    m_adapters_combobox->set_selected_index(selected_adapter);
    on_switch_adapter(m_adapter_names[selected_adapter_index]);
}

void NetworkSettingsWidget::on_switch_adapter(String const& adapter)
{
    auto& adapter_data = m_network_adapters.get(adapter).value();
    m_current_adapter_data = &adapter_data;
    on_switch_enabled_or_dhcp();

    m_enabled_checkbox->set_checked(adapter_data.enabled, GUI::AllowCallback::No);
    m_dhcp_checkbox->set_checked(adapter_data.dhcp, GUI::AllowCallback::No);
    m_ip_address_textbox->set_text(adapter_data.ip_address, GUI::AllowCallback::No);
    m_cidr_spinbox->set_value(adapter_data.cidr, GUI::AllowCallback::No);
    m_default_gateway_textbox->set_text(adapter_data.default_gateway, GUI::AllowCallback::No);

    VERIFY(m_current_adapter_data);
}

void NetworkSettingsWidget::on_switch_enabled_or_dhcp()
{
    m_dhcp_checkbox->set_enabled(m_current_adapter_data->enabled);
    m_ip_address_textbox->set_enabled(m_current_adapter_data->enabled && !m_current_adapter_data->dhcp);
    m_cidr_spinbox->set_enabled(m_current_adapter_data->enabled && !m_current_adapter_data->dhcp);
    m_default_gateway_textbox->set_enabled(m_current_adapter_data->enabled && !m_current_adapter_data->dhcp);
}

void NetworkSettingsWidget::apply_settings()
{
    auto config_file = Core::ConfigFile::open_for_system("Network", Core::ConfigFile::AllowWriting::Yes).release_value_but_fixme_should_propagate_errors();
    for (auto const& adapter_data : m_network_adapters) {
        auto netmask = IPv4Address::netmask_from_cidr(adapter_data.value.cidr).to_string();
        config_file->write_bool_entry(adapter_data.key, "Enabled", adapter_data.value.enabled);
        config_file->write_bool_entry(adapter_data.key, "DHCP", adapter_data.value.dhcp);
        if (adapter_data.value.enabled && !adapter_data.value.dhcp) {
            if (!IPv4Address::from_string(adapter_data.value.ip_address).has_value()) {
                GUI::MessageBox::show_error(window(), String::formatted("Invalid IPv4 address for adapter {}", adapter_data.key));
                return;
            }
            if (!IPv4Address::from_string(adapter_data.value.default_gateway).has_value()) {
                GUI::MessageBox::show_error(window(), String::formatted("Invalid IPv4 gateway for adapter {}", adapter_data.key));
                return;
            }
        }
        config_file->write_entry(adapter_data.key, "IPv4Address", adapter_data.value.ip_address);
        config_file->write_entry(adapter_data.key, "IPv4Netmask", netmask);
        config_file->write_entry(adapter_data.key, "IPv4Gateway", adapter_data.value.default_gateway);
    }

    GUI::Process::spawn_or_show_error(window(), "/bin/NetworkServer"sv);
}

}
