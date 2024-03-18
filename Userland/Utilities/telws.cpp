/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <LibProtocol/RequestClient.h>
#include <LibProtocol/WebSocket.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio unix inet accept rpath wpath cpath fattr tty sigaction"));

    Core::ArgsParser args_parser;

    ByteString origin;
    ByteString url_string;

    args_parser.add_positional_argument(url_string, "URL to connect to", "url", Core::ArgsParser::Required::Yes);
    args_parser.add_option(origin, "URL to use as origin", "origin", 'o', "origin");

    args_parser.parse(arguments);

    URL::URL url(url_string);

    if (!url.is_valid()) {
        warnln("The given URL is not valid");
        return 1;
    }

    Core::EventLoop loop;

    auto maybe_websocket_client = Protocol::RequestClient::try_create();
    if (maybe_websocket_client.is_error()) {
        warnln("Failed to connect to the websocket server: {}\n", maybe_websocket_client.error());
        return maybe_websocket_client.release_error();
    }
    auto websocket_client = maybe_websocket_client.release_value();

    RefPtr<Line::Editor> editor = Line::Editor::construct();
    bool should_quit = false;
    auto socket = websocket_client->websocket_connect(url, origin);
    if (!socket) {
        warnln("Failed to start socket for '{}'\n", url);
        return 1;
    }
    socket->on_open = [&]() {
        outln("[WebSocket opened]"sv);
    };
    socket->on_error = [&](auto error) {
        outln("[WebSocket Error : {}]", (unsigned)error);
    };
    socket->on_message = [&](auto message) {
        if (!message.is_text) {
            outln("[Received binary data : {} bytes]", message.data.size());
            return;
        }
        outln("[Received utf8 text] {}", ByteString(ReadonlyBytes(message.data)));
    };
    socket->on_close = [&](auto code, auto message, bool was_clean) {
        outln("[Server {} closed connection : '{}' (code {})]",
            was_clean ? "cleanly" : "dirtily",
            message,
            code);
        should_quit = true;
        Core::EventLoop::current().quit(0);
    };

    TRY(Core::System::pledge("stdio unix inet accept rpath wpath tty sigaction"));

    TRY(Core::System::unveil(nullptr, nullptr));

    outln("Started server. Commands :");
    outln("- '<text>' send the text as message");
    outln("- '.text <data>' send the text as message");
    outln("- '.base64 <data>' send the binary data from a base64-encoded string as message");
    outln("- '.exit' Ask to exit the server");
    outln("- '.forceexit' Exit the server");
    while (!should_quit) {
        auto line_or_error = editor->get_line(">");
        if (line_or_error.is_error()) {
            continue;
        }
        auto line = line_or_error.value();
        if (line.is_empty())
            continue;

        if (line.starts_with('.')) {
            if (line.starts_with(".text "sv)) {
                editor->add_to_history(line);
                if (socket->ready_state() != Protocol::WebSocket::ReadyState::Open) {
                    outln("Could not send message : socket is not open.");
                    continue;
                }
                socket->send(line.substring(6));
                continue;
            }
            if (line.starts_with(".base64 "sv)) {
                editor->add_to_history(line);
                if (socket->ready_state() != Protocol::WebSocket::ReadyState::Open) {
                    outln("Could not send message : socket is not open.");
                    continue;
                }
                auto base64_data = line.substring(8);
                auto buffer = decode_base64(base64_data);
                if (buffer.is_error()) {
                    outln("Could not send message : {}", buffer.error().string_literal());
                } else {
                    socket->send(buffer.value(), false);
                }
                continue;
            }
            if (line == ".exit") {
                editor->add_to_history(line);
                if (socket->ready_state() != Protocol::WebSocket::ReadyState::Open) {
                    outln("Socket is not open. Exiting.");
                    should_quit = true;
                    continue;
                }
                socket->close();
                continue;
            }
            if (line == ".forceexit") {
                editor->add_to_history(line);
                if (socket->ready_state() == Protocol::WebSocket::ReadyState::Open)
                    socket->close();
                return 1;
            }
            outln("Unknown command : {}", line);
            continue;
        }
        editor->add_to_history(line);
        if (socket->ready_state() != Protocol::WebSocket::ReadyState::Open) {
            outln("Could not send message : socket is not open.");
            continue;
        }
        socket->send(line);
    }

    return 0;
}
