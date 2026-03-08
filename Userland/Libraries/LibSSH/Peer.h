/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Socket.h>
#include <LibSSH/KeyExchangeData.h>

namespace SSH {

// This class implements the common part of the SSH Transport Layer Protocol.
// RFC 4253:  https://datatracker.ietf.org/doc/html/rfc4253
class Peer {
public:
    Peer(Core::TCPSocket& tcp_socket)
        : m_tcp_socket(tcp_socket)
    {
    }
    virtual ~Peer() = default;

protected:
    ErrorOr<ByteBuffer> read_packet(ByteBuffer&);
    ErrorOr<void> write_packet(ReadonlyBytes payload);

    ErrorOr<void> handle_new_keys_message(ByteBuffer& data);
    ErrorOr<void> send_new_keys_message();

private:
    Core::TCPSocket& m_tcp_socket;
};

// 4.1.2.  Initial Assignments
// https://datatracker.ietf.org/doc/html/rfc4250#section-4.1.2
enum class MessageID : u8 {
    DISCONNECT = 1,
    IGNORE = 2,
    UNIMPLEMENTED = 3,
    DEBUG = 4,
    SERVICE_REQUEST = 5,
    SERVICE_ACCEPT = 6,
    KEXINIT = 20,
    NEWKEYS = 21,
    USERAUTH_REQUEST = 50,
    USERAUTH_FAILURE = 51,
    USERAUTH_SUCCESS = 52,
    USERAUTH_BANNER = 53,
    GLOBAL_REQUEST = 80,
    REQUEST_SUCCESS = 81,
    REQUEST_FAILURE = 82,
    CHANNEL_OPEN = 90,
    CHANNEL_OPEN_CONFIRMATION = 91,
    CHANNEL_OPEN_FAILURE = 92,
    CHANNEL_WINDOW_ADJUST = 93,
    CHANNEL_DATA = 94,
    CHANNEL_EXTENDED_DATA = 95,
    CHANNEL_EOF = 96,
    CHANNEL_CLOSE = 97,
    CHANNEL_REQUEST = 98,
    CHANNEL_SUCCESS = 99,
    CHANNEL_FAILURE = 100,

    // https://datatracker.ietf.org/doc/html/rfc5656#section-7
    KEX_ECDH_INIT = 30,
    KEX_ECDH_REPLY = 31,
};

} // SSH
