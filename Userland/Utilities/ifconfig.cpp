/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/IPv4Address.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <net/if.h>
#include <netinet/in.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView value_ipv4 {};
    StringView value_adapter {};
    StringView value_mask {};

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display or modify the configuration of each network interface.");
    args_parser.add_option(value_ipv4, "Set the IP address of the selected network", "ipv4", 'i', "ip");
    args_parser.add_option(value_adapter, "Select a specific network adapter to configure", "adapter", 'a', "adapter");
    args_parser.add_option(value_mask, "Set the network mask of the selected network", "mask", 'm', "mask");
    args_parser.parse(arguments);

    if (value_ipv4.is_empty() && value_adapter.is_empty() && value_mask.is_empty()) {
        auto file = TRY(Core::File::open("/sys/kernel/net/adapters"sv, Core::File::OpenMode::Read));
        auto file_contents = TRY(file->read_until_eof());
        auto json = TRY(JsonValue::from_string(file_contents));

        json.as_array().for_each([](auto& value) {
            auto& if_object = value.as_object();

            auto name = if_object.get_byte_string("name"sv).value_or({});
            auto class_name = if_object.get_byte_string("class_name"sv).value_or({});
            auto mac_address = if_object.get_byte_string("mac_address"sv).value_or({});
            auto ipv4_address = if_object.get_byte_string("ipv4_address"sv).value_or({});
            auto netmask = if_object.get_byte_string("ipv4_netmask"sv).value_or({});
            auto packets_in = if_object.get_u32("packets_in"sv).value_or(0);
            auto bytes_in = if_object.get_u32("bytes_in"sv).value_or(0);
            auto packets_out = if_object.get_u32("packets_out"sv).value_or(0);
            auto bytes_out = if_object.get_u32("bytes_out"sv).value_or(0);
            auto mtu = if_object.get_u32("mtu"sv).value_or(0);

            outln("{}:", name);
            outln("\tmac: {}", mac_address);
            outln("\tipv4: {}", ipv4_address);
            outln("\tnetmask: {}", netmask);
            outln("\tclass: {}", class_name);
            outln("\tRX: {} packets {} bytes ({})", packets_in, bytes_in, human_readable_size(bytes_in));
            outln("\tTX: {} packets {} bytes ({})", packets_out, bytes_out, human_readable_size(bytes_out));
            outln("\tMTU: {}", mtu);
            outln();
        });
    } else {

        if (value_adapter.is_empty()) {
            warnln("No network adapter was specified.");
            return 1;
        }

        ByteString ifname = value_adapter;

        if (!value_ipv4.is_empty()) {
            auto address = IPv4Address::from_string(value_ipv4);

            if (!address.has_value()) {
                warnln("Invalid IPv4 address: '{}'", value_ipv4);
                return 1;
            }

            auto fd = TRY(Core::System::socket(AF_INET, SOCK_DGRAM, IPPROTO_IP));

            struct ifreq ifr;
            memset(&ifr, 0, sizeof(ifr));

            bool fits = ifname.copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
            if (!fits) {
                warnln("Interface name '{}' is too long", ifname);
                return 1;
            }
            ifr.ifr_addr.sa_family = AF_INET;
            ((sockaddr_in&)ifr.ifr_addr).sin_addr.s_addr = address.value().to_in_addr_t();

            TRY(Core::System::ioctl(fd, SIOCSIFADDR, &ifr));
        }

        if (!value_mask.is_empty()) {
            auto address = IPv4Address::from_string(value_mask);

            if (!address.has_value()) {
                warnln("Invalid IPv4 mask: '{}'", value_mask);
                return 1;
            }

            auto fd = TRY(Core::System::socket(AF_INET, SOCK_DGRAM, IPPROTO_IP));

            struct ifreq ifr;
            memset(&ifr, 0, sizeof(ifr));

            bool fits = ifname.copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
            if (!fits) {
                warnln("Interface name '{}' is too long", ifname);
                return 1;
            }
            ifr.ifr_netmask.sa_family = AF_INET;
            ((sockaddr_in&)ifr.ifr_netmask).sin_addr.s_addr = address.value().to_in_addr_t();

            TRY(Core::System::ioctl(fd, SIOCSIFNETMASK, &ifr));
        }
    }
    return 0;
}
