/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Socket.h>

namespace Core {

class UDPSocket final : public Socket {
    C_OBJECT(UDPSocket)
public:
    virtual ~UDPSocket() override;

private:
    explicit UDPSocket(Object* parent = nullptr);
};

}
