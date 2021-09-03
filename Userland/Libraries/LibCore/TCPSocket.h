/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Badge.h>
#include <LibCore/Socket.h>

namespace Core {

class TCPSocket final : public Socket {
    C_OBJECT(TCPSocket)
public:
    virtual ~TCPSocket() override;

private:
    TCPSocket(int fd, Object* parent = nullptr);
    explicit TCPSocket(Object* parent = nullptr);
};

}
