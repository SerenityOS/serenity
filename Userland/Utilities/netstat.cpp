/*
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc/net", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    bool flag_all = false;
    bool flag_list = false;
    bool flag_tcp = false;
    bool flag_udp = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display network connections");
    args_parser.add_option(flag_all, "Display both listening and non-listening sockets", "all", 'a');
    args_parser.add_option(flag_list, "Display only listening sockets", "list", 'l');
    args_parser.add_option(flag_tcp, "Display only TCP network connections", "tcp", 't');
    args_parser.add_option(flag_udp, "Display only UDP network connections", "udp", 'u');
    args_parser.parse(argc, argv);

    bool has_protocol_flag = (flag_tcp || flag_udp);

    enum class Alignment {
        Left,
        Right
    };

    struct Column {
        String title;
        Alignment alignment { Alignment::Left };
        int width { 0 };
        String buffer;
    };

    Vector<Column> columns;

    int protocol_column = -1;
    int bytes_in_column = -1;
    int bytes_out_column = -1;
    int local_address_column = -1;
    int peer_address_column = -1;
    int state_column = -1;

    auto add_column = [&](auto title, auto alignment, auto width) {
        columns.append({ title, alignment, width, {} });
        return columns.size() - 1;
    };

    protocol_column = add_column("Proto", Alignment::Left, 5);
    bytes_in_column = add_column("Bytes-In", Alignment::Right, 9);
    bytes_out_column = add_column("Bytes-Out", Alignment::Right, 9);
    local_address_column = add_column("Local Address", Alignment::Left, 20);
    peer_address_column = add_column("Peer Address", Alignment::Left, 20);
    state_column = add_column("State", Alignment::Left, 9);

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

    if (!has_protocol_flag || flag_tcp || flag_udp) {
        out("Active Internet connections ");

        if (flag_all) {
            outln("(servers and established)");
        } else {
            if (flag_list)
                outln("(only servers)");
            else
                outln("(without servers)");
        }

        for (auto& column : columns)
            print_column(column, column.title);
        outln();
    }

    if (!has_protocol_flag || flag_tcp) {
        auto file = Core::File::construct("/proc/net/tcp");
        if (!file->open(Core::OpenMode::ReadOnly)) {
            warnln("Error: {}", file->error_string());
            return 1;
        }

        auto file_contents = file->read_all();
        auto json = JsonValue::from_string(file_contents);
        VERIFY(json.has_value());

        Vector<JsonValue> sorted_regions = json.value().as_array().values();
        quick_sort(sorted_regions, [](auto& a, auto& b) {
            return a.as_object().get("local_port").to_u32() < b.as_object().get("local_port").to_u32();
        });

        for (auto& value : sorted_regions) {
            auto& if_object = value.as_object();

            auto bytes_in = if_object.get("bytes_in").to_string();
            auto bytes_out = if_object.get("bytes_out").to_string();
            auto peer_address = if_object.get("peer_address").to_string();
            auto peer_port = if_object.get("peer_port").to_string();
            auto formatted_peer_address = String::formatted("{}:{}", peer_address, peer_port);
            auto local_address = if_object.get("local_address").to_string();
            auto local_port = if_object.get("local_port").to_string();
            auto formatted_local_address = String::formatted("{}:{}", local_address, local_port);
            auto state = if_object.get("state").to_string();

            if (!flag_all && ((state == "Listen" && !flag_list) || (state != "Listen" && flag_list)))
                continue;

            if (protocol_column != -1)
                columns[protocol_column].buffer = "tcp";
            if (bytes_in_column != -1)
                columns[bytes_in_column].buffer = bytes_in;
            if (bytes_out_column != -1)
                columns[bytes_out_column].buffer = bytes_out;
            if (local_address_column != -1)
                columns[local_address_column].buffer = formatted_local_address;
            if (peer_address_column != -1)
                columns[peer_address_column].buffer = formatted_peer_address;
            if (state_column != -1)
                columns[state_column].buffer = state;

            for (auto& column : columns)
                print_column(column, column.buffer);
            outln();
        };
    }

    if (!has_protocol_flag || flag_udp) {
        auto file = Core::File::construct("/proc/net/udp");
        if (!file->open(Core::OpenMode::ReadOnly)) {
            warnln("Error: {}", file->error_string());
            return 1;
        }

        auto file_contents = file->read_all();
        auto json = JsonValue::from_string(file_contents);
        VERIFY(json.has_value());

        Vector<JsonValue> sorted_regions = json.value().as_array().values();
        quick_sort(sorted_regions, [](auto& a, auto& b) {
            return a.as_object().get("local_port").to_u32() < b.as_object().get("local_port").to_u32();
        });

        for (auto& value : sorted_regions) {
            auto& if_object = value.as_object();

            auto local_address = if_object.get("local_address").to_string();
            auto local_port = if_object.get("local_port").to_string();
            auto peer_address = if_object.get("peer_address").to_string();
            auto peer_port = if_object.get("peer_port").to_string();

            if (protocol_column != -1)
                columns[protocol_column].buffer = "udp";
            if (bytes_in_column != -1)
                columns[bytes_in_column].buffer = "-";
            if (bytes_out_column != -1)
                columns[bytes_out_column].buffer = "-";
            if (local_address_column != -1)
                columns[local_address_column].buffer = String::formatted("{}:{}", local_address, local_port);
            if (peer_address_column != -1)
                columns[peer_address_column].buffer = String::formatted("{}:{}", peer_address, peer_port);
            if (state_column != -1)
                columns[state_column].buffer = "-";

            for (auto& column : columns)
                print_column(column, column.buffer);
            outln();
        };
    }

    return 0;
}
