/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/TCPSocket.h>

#pragma once

enum class TransportType {
    RAW,
    TLS,
    SSL
};

class TransportBase {
public:
    virtual ~TransportBase() = default;

    virtual void init(ReadonlyBytes input, RefPtr<Core::Socket> connection) = 0;
    virtual ByteBuffer receive(int max_size, RefPtr<Core::Socket> connection) = 0;
    virtual bool send(ReadonlyBytes, RefPtr<Core::Socket> connection) = 0;
    virtual bool is_init() = 0;
};

class RawTransport : public TransportBase {
public:
    void init(ReadonlyBytes input, RefPtr<Core::Socket> connection);
    ByteBuffer receive(int max_size, RefPtr<Core::Socket> connection);
    bool send(ReadonlyBytes, RefPtr<Core::Socket> connection);

    bool is_init() { return true; }
};
