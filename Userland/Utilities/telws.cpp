/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Format.h>
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>
#include <LibLine/Editor.h>
#include <LibProtocol/WebSocket.h>
#include <LibProtocol/WebSocketClient.h>

int main(int argc, char** argv)
{
    if (pledge("stdio unix inet accept rpath wpath cpath fattr tty sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::ArgsParser args_parser;

    String origin;
    String url_string;

    args_parser.add_positional_argument(url_string, "URL to connect to", "url", Core::ArgsParser::Required::Yes);
    args_parser.add_option(origin, "URL to use as origin", "origin", 'o', "origin");

    args_parser.parse(argc, argv);

    URL url(url_string);

    if (!url.is_valid()) {
        warnln("The given URL is not valid");
        return 1;
    }

    Core::EventLoop loop;
    RefPtr<Line::Editor> editor = Line::Editor::construct();
    bool should_quit = false;
    auto websocket_client = Protocol::WebSocketClient::construct();
    auto socket = websocket_client->connect(url, origin);
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
        outln("[Received utf8 text] {}", String(ReadonlyBytes(message.data)));
    };
    socket->on_close = [&](auto code, auto message, bool was_clean) {
        outln("[Server {} closed connection : '{}' (code {})]",
            was_clean ? "cleanly" : "dirtily",
            message,
            code);
        should_quit = true;
        Core::EventLoop::current().quit(0);
    };

    if (pledge("stdio unix inet accept rpath wpath tty sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

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

        if (line.starts_with(".")) {
            if (line.starts_with(".text ")) {
                editor->add_to_history(line);
                if (socket->ready_state() != Protocol::WebSocket::ReadyState::Open) {
                    outln("Could not send message : socket is not open.");
                    continue;
                }
                socket->send(line.substring(6));
                continue;
            }
            if (line.starts_with(".base64 ")) {
                editor->add_to_history(line);
                if (socket->ready_state() != Protocol::WebSocket::ReadyState::Open) {
                    outln("Could not send message : socket is not open.");
                    continue;
                }
                auto base64_data = line.substring(8);
                auto buffer = decode_base64(base64_data);
                if (buffer.has_value()) {
                    socket->send(buffer.value(), false);
                } else {
                    outln("Could not send message : Base64 string contains an invalid character.");
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
