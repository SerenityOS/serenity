/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/OwnPtr.h>
#include <YAK/String.h>

namespace YAK {
class BufferStream;
}

namespace IPC {

class Message;
struct MessageBuffer;

class Stub {
public:
    virtual ~Stub();

    virtual u32 magic() const = 0;
    virtual String name() const = 0;
    virtual OwnPtr<MessageBuffer> handle(const Message&) = 0;

protected:
    Stub();

private:
    String m_name;
};

}
