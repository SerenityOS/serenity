/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FTPClient.h"
#include <LibCore/FileStream.h>
#include <LibCore/SocketAddress.h>
#include <LibCore/TCPSocket.h>
#include <unistd.h>

#undef FTP_DEBUG
#define FTP_DEBUG 1

void FTPClient::run()
{
    m_socket = Core::TCPSocket::construct();
    m_socket->connect(Core::SocketAddress({}, 21), 21);
    m_socket->set_blocking(true);
    dbgln("Connected, waiting for server accept code");
    drain_socket();

    send("USER stelar7\r\n");
    drain_socket();

    send("PASS buggie\r\n");
    drain_socket();

    send("CWD /res/html\r\n");
    drain_socket();

    send("PASV\r\n");
    String data = drain_socket();

    if (!data.contains("(")) {
        dbgln("Invalid response from server, closing");
        outln("Invalid response from server, closing");
        return;
    }

    auto parts = data.split('(').at(1).split(')').at(0).split(',');
    auto port0 = parts.take_last();
    auto port1 = parts.take_last();
    IPv4Address ip = IPv4Address::from_string(String::join(".", parts)).value();
    u16 port = ((port1.to_int().value() & 0xFF) << 8 | (port0.to_int().value() & 0xFF)) & 0xFFFF;
    auto address = Core::SocketAddress(ip, port);

    send("RETR error.html\r\n");

    dbgln("Connecting to data socket {}:{}", ip.to_string(), port);
    auto data_socket = Core::TCPSocket::construct();
    data_socket->connect(address, port);
    data_socket->set_blocking(true);

    drain_socket();

    auto outstream_or_error = Core::OutputFileStream::open("/home/anon/error_copy.html");
    if (outstream_or_error.is_error()) {
        dbgln(outstream_or_error.error());
        return;
    }

    auto& stream = outstream_or_error.value();

    while (true) {
        auto data = data_socket->read(4 * KiB);
        if (data.size() == 0) {
            break;
        }
        stream.write(data);
    }

    data_socket->close();

    drain_socket();

    m_socket->close();
}

String FTPClient::drain_socket()
{
    dbgln_if(FTP_DEBUG, "Draining socket replies...");

    auto buf = m_socket->read(4 * KiB);
    String data = String(buf, AK::NoChomp);

    // remove \r\n
    if (data.length() > 2) {
        data = data.substring(0, data.length() - 2);
    }

    dbgln_if(FTP_DEBUG, "Received: {}", data);

    return data;
}

void FTPClient::quit()
{
    if (m_socket)
        m_socket->close();
}

void FTPClient::send(String data)
{
    dbgln_if(FTP_DEBUG, "Sending: {}", data);

    if (!m_socket) {
        quit();
        return;
    }

    m_socket->send(data.bytes());
}
