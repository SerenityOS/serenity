/*
 * Copyright (c) 2022, Brandon Pruitt <brapru@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/IPv4Address.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <net/if.h>
#include <net/route.h>
#include <netinet/in.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath inet"));
    TRY(Core::System::unveil("/sys/kernel/net", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView modify_action;
    StringView value_host_address;
    StringView value_network_address;
    StringView value_gateway_address;
    StringView value_netmask_address;
    StringView value_interface;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display kernel routing table");
    args_parser.add_positional_argument(modify_action, "Modify the global routing table { add | del }", "action", Core::ArgsParser::Required::No);
    args_parser.add_option(value_host_address, "Target destination is an IPv4 address", "host", 'h', "host");
    args_parser.add_option(value_network_address, "Target destination is a network address", "net", 'n', "net");
    args_parser.add_option(value_gateway_address, "Route packets via a gateway", "gw", 'g', "gw");
    args_parser.add_option(value_netmask_address, "The netmask to be used when adding a network route", "netmask", 'm', "netmask");
    args_parser.add_option(value_interface, "Force the route to be associated with the specified device interface", "interface", 'i', "interface");
    args_parser.parse(arguments);

    enum class Alignment {
        Left,
        Right
    };

    struct Column {
        ByteString title;
        Alignment alignment { Alignment::Left };
        int width { 0 };
        ByteString buffer;
    };

    Vector<Column> columns;

    int destination_column = -1;
    int gateway_column = -1;
    int genmask_column = -1;
    int flags_column = -1;
    int interface_column = -1;

    auto add_column = [&](auto title, auto alignment, auto width) {
        columns.append({ title, alignment, width, {} });
        return columns.size() - 1;
    };

    destination_column = add_column("Destination", Alignment::Left, 15);
    gateway_column = add_column("Gateway", Alignment::Left, 15);
    genmask_column = add_column("Genmask", Alignment::Left, 15);
    flags_column = add_column("Flags", Alignment::Left, 5);
    interface_column = add_column("Interface", Alignment::Left, 9);

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

    if (modify_action.is_empty()) {
        auto file = TRY(Core::File::open("/sys/kernel/net/route"sv, Core::File::OpenMode::Read));
        auto file_contents = TRY(file->read_until_eof());
        auto json = TRY(JsonValue::from_string(file_contents));

        outln("Kernel IP routing table");

        for (auto& column : columns)
            print_column(column, column.title);
        outln();

        Vector<JsonValue> sorted_regions = json.as_array().values();
        quick_sort(sorted_regions, [](auto& a, auto& b) {
            return a.as_object().get_byte_string("destination"sv).value_or({}) < b.as_object().get_byte_string("destination"sv).value_or({});
        });

        for (auto& value : sorted_regions) {
            auto& if_object = value.as_object();

            auto destination = if_object.get_byte_string("destination"sv).value_or({});
            auto gateway = if_object.get_byte_string("gateway"sv).value_or({});
            auto genmask = if_object.get_byte_string("genmask"sv).value_or({});
            auto interface = if_object.get_byte_string("interface"sv).value_or({});
            auto flags = if_object.get_u32("flags"sv).value_or(0);

            StringBuilder flags_builder;
            if (flags & RTF_UP)
                flags_builder.append('U');
            if (flags & RTF_GATEWAY)
                flags_builder.append('G');
            if (flags & RTF_HOST)
                flags_builder.append('H');

            if (destination_column != -1)
                columns[destination_column].buffer = destination;
            if (gateway_column != -1)
                columns[gateway_column].buffer = gateway;
            if (genmask_column != -1)
                columns[genmask_column].buffer = genmask;
            if (flags_column != -1)
                columns[flags_column].buffer = flags_builder.string_view();
            if (interface_column != -1)
                columns[interface_column].buffer = interface;

            for (auto& column : columns)
                print_column(column, column.buffer);
            outln();
        };
    }

    if (!modify_action.is_empty()) {
        bool const action_add = (modify_action == "add");
        bool const action_del = (modify_action == "del");

        if (!action_add && !action_del) {
            warnln("Invalid modify action: {}", modify_action);
            return 1;
        }

        if (value_host_address.is_empty() && value_network_address.is_empty()) {
            warnln("No target host or network specified");
            return 1;
        }

        Optional<IPv4Address> destination;

        if (!value_host_address.is_empty())
            destination = AK::IPv4Address::from_string(value_host_address);

        StringView address;
        StringView cidr;
        if (!value_network_address.is_empty()) {
            // Check if a CIDR notation was provided and parse accordingly
            if (auto position = value_network_address.find('/'); position.has_value()) {
                address = value_network_address.substring_view(0, position.value());
                cidr = value_network_address.substring_view(position.value() + 1);
            } else {
                address = value_network_address;
            }
            destination = AK::IPv4Address::from_string(address);
        }

        if (!destination.has_value()) {
            warnln("Invalid destination IPv4 address");
            return 1;
        }

        auto gateway = AK::IPv4Address::from_string(value_gateway_address);
        if (action_add && !gateway.has_value()) {
            warnln("Invalid gateway IPv4 address: '{}'", value_gateway_address);
            return 1;
        }

        Optional<IPv4Address> genmask;
        if (auto cidr_int = cidr.to_number<int>(); cidr_int.has_value())
            genmask = AK::IPv4Address::netmask_from_cidr(cidr_int.value());
        else
            genmask = AK::IPv4Address::from_string(value_netmask_address);

        if (!genmask.has_value()) {
            warnln("Invalid genmask IPv4 address: '{}'", value_netmask_address);
            return 1;
        }

        int fd = TRY(Core::System::socket(AF_INET, SOCK_DGRAM, IPPROTO_IP));

        rtentry rt {};
        memset(&rt, 0, sizeof(rt));

        rt.rt_dev = const_cast<char*>(value_interface.characters_without_null_termination());
        rt.rt_gateway.sa_family = AF_INET;
        ((sockaddr_in&)rt.rt_dst).sin_addr.s_addr = destination.value().to_in_addr_t();
        ((sockaddr_in&)rt.rt_gateway).sin_addr.s_addr = gateway.value_or(IPv4Address {}).to_in_addr_t();
        ((sockaddr_in&)rt.rt_genmask).sin_addr.s_addr = genmask.value().to_in_addr_t();
        rt.rt_flags = RTF_UP;

        if (!value_host_address.is_empty())
            rt.rt_flags |= RTF_HOST;

        rt.rt_flags |= RTF_GATEWAY;

        if (action_add)
            TRY(Core::System::ioctl(fd, SIOCADDRT, &rt));

        if (action_del)
            TRY(Core::System::ioctl(fd, SIOCDELRT, &rt));
    }

    return 0;
}
