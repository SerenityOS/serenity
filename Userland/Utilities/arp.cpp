/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/IPv4Address.h>
#include <AK/JsonObject.h>
#include <AK/MACAddress.h>
#include <AK/QuickSort.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <arpa/inet.h>
#include <net/if_arp.h>
#include <net/route.h>
#include <netdb.h>
#include <netinet/in.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty inet unix"));

    bool flag_set = false;
    bool flag_delete = false;
    bool flag_numeric = false;
    StringView value_ipv4_address;
    StringView value_hw_address;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display or modify the system ARP cache");
    args_parser.add_option(flag_set, "Set an ARP table entry", "set", 's');
    args_parser.add_option(flag_delete, "Delete an ARP table entry", "delete", 'd');
    args_parser.add_option(flag_numeric, "Display numerical addresses. Don't resolve hostnames", "numeric", 'n');
    args_parser.add_positional_argument(value_ipv4_address, "IPv4 protocol address", "address", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(value_hw_address, "Hardware address", "hwaddress", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    TRY(Core::System::unveil("/sys/kernel/net/arp", "r"));
    if (!flag_numeric)
        TRY(Core::System::unveil("/tmp/portal/lookup", "rw"));

    TRY(Core::System::unveil(nullptr, nullptr));

    enum class Alignment {
        Left,
        Right
    };

    struct Column {
        StringView title;
        Alignment alignment { Alignment::Left };
        int width { 0 };
        StringView buffer;
    };

    Vector<Column> columns;

    int proto_address_column = -1;
    int hw_address_column = -1;

    auto add_column = [&](auto title, auto alignment, auto width) {
        columns.append({ title, alignment, width, {} });
        return columns.size() - 1;
    };

    proto_address_column = add_column("Address"sv, Alignment::Left, 15);
    hw_address_column = add_column("HWaddress"sv, Alignment::Left, 15);

    auto print_column = [](auto& column, auto& string) {
        if (!column.width) {
            out("{}", string);
            return;
        }
        if (column.alignment == Alignment::Right) {
            out("{:>{1}}  "sv, string, column.width);
        } else {
            out("{:<{1}}  "sv, string, column.width);
        }
    };

    for (auto& column : columns)
        print_column(column, column.title);
    outln();

    if (!flag_set && !flag_delete) {
        auto file = TRY(Core::File::open("/sys/kernel/net/arp"sv, Core::File::OpenMode::Read));
        auto file_contents = TRY(file->read_until_eof());
        auto json = TRY(JsonValue::from_string(file_contents));

        Vector<JsonValue> sorted_regions = json.as_array().values();
        quick_sort(sorted_regions, [](auto& a, auto& b) {
            return a.as_object().get_byte_string("ip_address"sv).value_or({}) < b.as_object().get_byte_string("ip_address"sv).value_or({});
        });

        for (auto& value : sorted_regions) {
            auto& if_object = value.as_object();

            auto ip_address = if_object.get_byte_string("ip_address"sv).value_or({});

            if (!flag_numeric) {
                auto from_string = IPv4Address::from_string(ip_address);
                auto addr = from_string.value().to_in_addr_t();
                auto* hostent = gethostbyaddr(&addr, sizeof(in_addr), AF_INET);
                if (hostent != nullptr) {
                    StringView host_name { hostent->h_name, strlen(hostent->h_name) };
                    if (!host_name.is_empty())
                        ip_address = host_name;
                }
            }

            auto mac_address = if_object.get_byte_string("mac_address"sv).value_or({});

            if (proto_address_column != -1)
                columns[proto_address_column].buffer = ip_address;
            if (hw_address_column != -1)
                columns[hw_address_column].buffer = mac_address;

            for (auto& column : columns)
                print_column(column, column.buffer);
            outln();
        };
    }

    if (flag_set || flag_delete) {
        if (value_ipv4_address.is_empty() || value_hw_address.is_empty()) {
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

        int fd = TRY(Core::System::socket(AF_INET, SOCK_DGRAM, IPPROTO_IP));

        struct arpreq arp_req;
        memset(&arp_req, 0, sizeof(arp_req));

        arp_req.arp_pa.sa_family = AF_INET;
        ((sockaddr_in&)arp_req.arp_pa).sin_addr.s_addr = address.value().to_in_addr_t();

        *(MACAddress*)&arp_req.arp_ha.sa_data[0] = hw_address.value();

        if (flag_set)
            TRY(Core::System::ioctl(fd, SIOCSARP, &arp_req));
        if (flag_delete)
            TRY(Core::System::ioctl(fd, SIOCDARP, &arp_req));
    }

    return 0;
}
