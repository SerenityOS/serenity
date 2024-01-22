/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>

#include <AK/HashMap.h>

namespace NetworkSettings {

class NetworkSettingsWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(NetworkSettingsWidget)

public:
    static ErrorOr<NonnullRefPtr<NetworkSettingsWidget>> try_create();
    ErrorOr<void> initialize();

    virtual void apply_settings() override;
    void switch_adapter(ByteString const& adapter);

private:
    NetworkSettingsWidget() = default;

    struct NetworkAdapterData {
        bool enabled = false;
        bool dhcp = false;
        ByteString ip_address;
        int cidr = 0;
        ByteString default_gateway;
    };

    void on_switch_adapter(ByteString const& adapter);
    void on_switch_enabled_or_dhcp();
    ErrorOr<void> apply_settings_impl();
    ErrorOr<Optional<JsonObject>> create_settings_object();

    HashMap<ByteString, NetworkAdapterData> m_network_adapters;
    Vector<ByteString> m_adapter_names;
    NetworkAdapterData* m_current_adapter_data = nullptr;

    RefPtr<GUI::CheckBox> m_enabled_checkbox;
    RefPtr<GUI::CheckBox> m_dhcp_checkbox;
    RefPtr<GUI::ComboBox> m_adapters_combobox;
    RefPtr<GUI::TextBox> m_ip_address_textbox;
    RefPtr<GUI::SpinBox> m_cidr_spinbox;
    RefPtr<GUI::TextBox> m_default_gateway_textbox;
};

}
