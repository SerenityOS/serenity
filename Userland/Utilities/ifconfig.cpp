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
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <net/if.h>
#include <net/route.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>

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
        auto file = TRY(Core::File::open("/sys/kernel/net/adapters", Core::OpenMode::ReadOnly));
        auto json = TRY(JsonValue::from_string(file->read_all()));

        json.as_array().for_each([](auto& value) {
            auto& if_object = value.as_object();

            auto name = if_object.get("name"sv).to_string();
            auto class_name = if_object.get("class_name"sv).to_string();
            auto mac_address = if_object.get("mac_address"sv).to_string();
            auto ipv4_address = if_object.get("ipv4_address"sv).to_string();
            auto netmask = if_object.get("ipv4_netmask"sv).to_string();
            auto packets_in = if_object.get("packets_in"sv).to_u32();
            auto bytes_in = if_object.get("bytes_in"sv).to_u32();
            auto packets_out = if_object.get("packets_out"sv).to_u32();
            auto bytes_out = if_object.get("bytes_out"sv).to_u32();
            auto mtu = if_object.get("mtu"sv).to_u32();

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

        String ifname = value_adapter;

        if (!value_ipv4.is_empty()) {
            auto address = IPv4Address::from_string(value_ipv4);

            if (!address.has_value()) {
                warnln("Invalid IPv4 address: '{}'", value_ipv4);
                return 1;
            }

            int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
            if (fd < 0) {
                perror("socket");
                return 1;
            }

            struct ifreq ifr;
            memset(&ifr, 0, sizeof(ifr));

            bool fits = ifname.copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
            if (!fits) {
                warnln("Interface name '{}' is too long", ifname);
                return 1;
            }
            ifr.ifr_addr.sa_family = AF_INET;
            ((sockaddr_in&)ifr.ifr_addr).sin_addr.s_addr = address.value().to_in_addr_t();

            int rc = ioctl(fd, SIOCSIFADDR, &ifr);
            if (rc < 0) {
                perror("ioctl(SIOCSIFADDR)");
                return 1;
            }
        }

        if (!value_mask.is_empty()) {
            auto address = IPv4Address::from_string(value_mask);

            if (!address.has_value()) {
                warnln("Invalid IPv4 mask: '{}'", value_mask);
                return 1;
            }

            int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
            if (fd < 0) {
                perror("socket");
                return 1;
            }

            struct ifreq ifr;
            memset(&ifr, 0, sizeof(ifr));

            bool fits = ifname.copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
            if (!fits) {
                warnln("Interface name '{}' is too long", ifname);
                return 1;
            }
            ifr.ifr_netmask.sa_family = AF_INET;
            ((sockaddr_in&)ifr.ifr_netmask).sin_addr.s_addr = address.value().to_in_addr_t();

            int rc = ioctl(fd, SIOCSIFNETMASK, &ifr);
            if (rc < 0) {
                perror("ioctl(SIOCSIFNETMASK)");
                return 1;
            }
        }
    }
    return 0;
}
