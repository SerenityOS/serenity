/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/String.h>

namespace AK {
class BufferStream;
}

namespace IPC {

class Message;
class MessageBuffer;

class Endpoint {
public:
    virtual ~Endpoint();

    virtual u32 magic() const = 0;
    virtual String name() const = 0;
    virtual OwnPtr<MessageBuffer> handle(const Message&) = 0;

protected:
    Endpoint();

private:
    String m_name;
};

}
