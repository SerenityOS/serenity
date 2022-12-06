/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/String.h>
#include <LibCore/Command.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix exec proc"));
    TRY(Core::System::unveil("/sys/kernel/net", "r"));
    TRY(Core::System::unveil("/bin/DHCPClient", "x"));
    TRY(Core::System::unveil("/etc/Network.ini", "r"));
    TRY(Core::System::unveil("/bin/ifconfig", "x"));
    TRY(Core::System::unveil("/bin/killall", "x"));
    TRY(Core::System::unveil("/bin/route", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto config_file = TRY(Core::ConfigFile::open_for_system("Network"));

    auto proc_net_adapters_file = TRY(Core::Stream::File::open("/sys/kernel/net/adapters"sv, Core::Stream::OpenMode::Read));
    auto data = TRY(proc_net_adapters_file->read_all());
    JsonParser parser(data);
    JsonValue proc_net_adapters_json = TRY(parser.parse());

    // Kill all previously running DHCPServers that may manage to re-assign the IP
    // address before we clear it manually.
    MUST(Core::command("killall"sv, { "DHCPClient" }, {}));

    auto groups = config_file->groups();
    dbgln("Interfaces to configure: {}", groups);

    struct InterfaceConfig {
        bool enabled = false;
        bool dhcp_enabled = false;
        String ipv4_address;
        String ipv4_netmask;
        String ipv4_gateway;
    };

    Vector<String> interfaces_with_dhcp_enabled;
    TRY(proc_net_adapters_json.as_array().try_for_each([&](JsonValue const& value) -> ErrorOr<void> {
        auto& if_object = value.as_object();
        auto ifname = TRY(if_object.get("name"sv).to_string());

        if (ifname == "loop"sv)
            return {};

        InterfaceConfig config;
        if (!groups.contains_slow(ifname.to_deprecated_string())) {
            dbgln("Config for interface {} doesn't exist, enabling DHCP for it", ifname);
            interfaces_with_dhcp_enabled.append(ifname);
        } else {
            config.enabled = config_file->read_bool_entry(ifname.to_deprecated_string(), "Enabled"sv, true);
            config.dhcp_enabled = config_file->read_bool_entry(ifname.to_deprecated_string(), "DHCP"sv, false);
            if (!config.dhcp_enabled) {
                config.ipv4_address = TRY(String::from_deprecated_string(config_file->read_entry(ifname.to_deprecated_string(), "IPv4Address"sv, "0.0.0.0"sv)));
                config.ipv4_netmask = TRY(String::from_deprecated_string(config_file->read_entry(ifname.to_deprecated_string(), "IPv4Netmask"sv, "0.0.0.0"sv)));
                config.ipv4_gateway = TRY(String::from_deprecated_string(config_file->read_entry(ifname.to_deprecated_string(), "IPv4Gateway"sv, "0.0.0.0"sv)));
            }
        }

        if (config.ipv4_address.is_empty()) {
            config.ipv4_address = TRY(String::from_utf8("0.0.0.0"sv));
        }
        if (config.ipv4_netmask.is_empty()) {
            config.ipv4_netmask = TRY(String::from_utf8("0.0.0.0"sv));
        }
        if (config.ipv4_gateway.is_empty()) {
            config.ipv4_gateway = TRY(String::from_utf8("0.0.0.0"sv));
        }

        if (config.enabled) {
            if (config.dhcp_enabled)
                interfaces_with_dhcp_enabled.append(ifname);
            else {
                // FIXME: Do this asynchronously
                dbgln("Setting up interface {} statically ({}/{})", ifname.to_deprecated_string(), config.ipv4_address.to_deprecated_string(), config.ipv4_netmask.to_deprecated_string());
                TRY(Core::command("ifconfig"sv, { "-a", ifname.to_deprecated_string(), "-i", config.ipv4_address.to_deprecated_string(), "-m", config.ipv4_netmask.to_deprecated_string() }, {}));
                if (config.ipv4_gateway != "0.0.0.0") {
                    TRY(Core::command("route"sv, { "del", "-n", "0.0.0.0", "-m", "0.0.0.0", "-i", ifname.to_deprecated_string() }, {}));
                    TRY(Core::command("route"sv, { "add", "-n", "0.0.0.0", "-m", "0.0.0.0", "-g", config.ipv4_gateway.to_deprecated_string(), "-i", ifname.to_deprecated_string() }, {}));
                }
            }
        } else {
            dbgln("Disabling interface {}", ifname);
            TRY(Core::command("route"sv, { "del", "-n", "0.0.0.0", "-m", "0.0.0.0", "-i", ifname.to_deprecated_string() }, {}));
            TRY(Core::command("ifconfig"sv, { "-a", ifname.to_deprecated_string(), "-i", "0.0.0.0", "-m", "0.0.0.0" }, {}));
        }
        return {};
    }));

    if (!interfaces_with_dhcp_enabled.is_empty()) {
        dbgln("Running DHCPClient for interfaces: {}", interfaces_with_dhcp_enabled);
        Vector<char*> args;
        char dhcp_client_arg[] = "DHCPClient";
        args.append(dhcp_client_arg);
        for (auto& iface : interfaces_with_dhcp_enabled)
            args.append(const_cast<char*>(reinterpret_cast<char const*>(iface.bytes().data())));
        args.append(nullptr);

        auto dhcp_client_pid = TRY(Core::System::posix_spawnp("DHCPClient"sv, nullptr, nullptr, args.data(), environ));
        TRY(Core::System::disown(dhcp_client_pid));
    }
    return 0;
}
