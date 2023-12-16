/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <LibCore/Command.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
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

    auto proc_net_adapters_file = TRY(Core::File::open("/sys/kernel/net/adapters"sv, Core::File::OpenMode::Read));
    auto data = TRY(proc_net_adapters_file->read_until_eof());
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
        ByteString ipv4_address = "0.0.0.0"sv;
        ByteString ipv4_netmask = "0.0.0.0"sv;
        ByteString ipv4_gateway = "0.0.0.0"sv;
    };

    Vector<ByteString> interfaces_with_dhcp_enabled;
    proc_net_adapters_json.as_array().for_each([&](auto& value) {
        auto& if_object = value.as_object();
        auto ifname = if_object.get_byte_string("name"sv).value_or({});

        if (ifname == "loop"sv)
            return;

        InterfaceConfig config;
        if (!groups.contains_slow(ifname)) {
            dbgln("Config for interface {} doesn't exist, enabling DHCP for it", ifname);
            interfaces_with_dhcp_enabled.append(ifname);
        } else {
            config.enabled = config_file->read_bool_entry(ifname, "Enabled"sv, true);
            config.dhcp_enabled = config_file->read_bool_entry(ifname, "DHCP"sv, false);
            if (!config.dhcp_enabled) {
                config.ipv4_address = config_file->read_entry(ifname, "IPv4Address"sv, "0.0.0.0"sv);
                config.ipv4_netmask = config_file->read_entry(ifname, "IPv4Netmask"sv, "0.0.0.0"sv);
                config.ipv4_gateway = config_file->read_entry(ifname, "IPv4Gateway"sv, "0.0.0.0"sv);
            }
        }
        if (config.enabled) {
            if (config.dhcp_enabled)
                interfaces_with_dhcp_enabled.append(ifname);
            else {
                // FIXME: Propagate errors
                // FIXME: Do this asynchronously
                dbgln("Setting up interface {} statically ({}/{})", ifname, config.ipv4_address, config.ipv4_netmask);
                MUST(Core::command("ifconfig"sv, { "-a", ifname.characters(), "-i", config.ipv4_address.characters(), "-m", config.ipv4_netmask.characters() }, {}));
                if (config.ipv4_gateway != "0.0.0.0") {
                    MUST(Core::command("route"sv, { "del", "-n", "0.0.0.0", "-m", "0.0.0.0", "-i", ifname }, {}));
                    MUST(Core::command("route"sv, { "add", "-n", "0.0.0.0", "-m", "0.0.0.0", "-g", config.ipv4_gateway, "-i", ifname }, {}));
                }
            }
        } else {
            // FIXME: Propagate errors
            dbgln("Disabling interface {}", ifname);
            MUST(Core::command("route"sv, { "del", "-n", "0.0.0.0", "-m", "0.0.0.0", "-i", ifname }, {}));
            MUST(Core::command("ifconfig"sv, { "-a", ifname.characters(), "-i", "0.0.0.0", "-m", "0.0.0.0" }, {}));
        }
    });

    if (!interfaces_with_dhcp_enabled.is_empty()) {
        dbgln("Running DHCPClient for interfaces: {}", interfaces_with_dhcp_enabled);
        Vector<char*> args;
        char dhcp_client_arg[] = "DHCPClient";
        args.append(dhcp_client_arg);
        for (auto& iface : interfaces_with_dhcp_enabled)
            args.append(const_cast<char*>(iface.characters()));
        args.append(nullptr);

        auto dhcp_client_pid = TRY(Core::System::posix_spawnp("DHCPClient"sv, nullptr, nullptr, args.data(), environ));
        TRY(Core::System::disown(dhcp_client_pid));
    }
    return 0;
}
