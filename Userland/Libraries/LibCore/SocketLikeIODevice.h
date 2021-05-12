/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/IODevice.h>

namespace Core {

class SocketLikeIODevice : public virtual IODevice {
    C_OBJECT_ABSTRACT(SocketLikeIODevice)
public:
    virtual bool connect(const String& hostname, int port) = 0;
    virtual bool connect(const SocketAddress&, int port) = 0;
    virtual bool connect(const SocketAddress&) = 0;

    virtual size_t receive(Bytes bytes) { return read(bytes); }
    virtual size_t send(ReadonlyBytes bytes) { return write(bytes); };
    virtual bool is_connected() const = 0;

protected:
    SocketLikeIODevice(Object* parent)
        : IODevice(parent)
    {
    }
};

}
