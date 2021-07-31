/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/IPv4Address.h>
#include <AK/JsonObject.h>
#include <AK/MACAddress.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <net/route.h>
#include <netinet/in.h>
#include <sys/ioctl.h>
#include <sys/socket.h>

int main(int argc, char** argv)
{
    static bool flag_set;
    static bool flag_delete;
    const char* value_ipv4_address = nullptr;
    const char* value_hw_address = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display or modify the system ARP cache");
    args_parser.add_option(flag_set, "Set an ARP table entry", "set", 's');
    args_parser.add_option(flag_delete, "Delete an ARP table entry", "delete", 'd');
    args_parser.add_positional_argument(value_ipv4_address, "IPv4 protocol address", "address", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(value_hw_address, "Hardware address", "hwaddress", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto file = Core::File::construct("/proc/net/arp");
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", file->name(), file->error_string());
        return 1;
    }

    if (!flag_set && !flag_delete) {
        outln("Address          HWaddress");
        auto file_contents = file->read_all();
        auto json = JsonValue::from_string(file_contents);
        VERIFY(json.has_value());
        json.value().as_array().for_each([](auto& value) {
            auto& if_object = value.as_object();

            auto ip_address = if_object.get("ip_address").to_string();
            auto mac_address = if_object.get("mac_address").to_string();

            outln("{:15}  {:17}", ip_address, mac_address);
        });
    }

    if (flag_set || flag_delete) {
        if (!value_ipv4_address || !value_hw_address) {
            warnln("No protocol address or hardware address specified.");
            return 1;
        }

        auto address = IPv4Address::from_string(value_ipv4_address);
        if (!address.has_value()) {
            warnln("Invalid IPv4 protocol address: '{}'", value_ipv4_address);
            return 1;
        }

        auto hw_address = MACAddress::from_string(value_hw_address);
        if (!hw_address.has_value()) {
            warnln("Invalid MACAddress: '{}'", value_hw_address);
            return 1;
        }

        int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
        if (fd < 0) {
            perror("socket");
            return 1;
        }

        struct arpreq arp_req;
        memset(&arp_req, 0, sizeof(arp_req));

        arp_req.arp_pa.sa_family = AF_INET;
        ((sockaddr_in&)arp_req.arp_pa).sin_addr.s_addr = address.value().to_in_addr_t();

        *(MACAddress*)&arp_req.arp_ha.sa_data[0] = hw_address.value();

        int rc;
        if (flag_set)
            rc = ioctl(fd, SIOCSARP, &arp_req);
        if (flag_delete)
            rc = ioctl(fd, SIOCDARP, &arp_req);

        if (rc < 0) {
            perror("ioctl");
            return 1;
        }
    }

    return 0;
}
