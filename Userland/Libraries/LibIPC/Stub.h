/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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
struct MessageBuffer;

class Stub {
public:
    virtual ~Stub() = default;

    virtual u32 magic() const = 0;
    virtual String name() const = 0;
    virtual OwnPtr<MessageBuffer> handle(Message const&) = 0;

protected:
    Stub() = default;

private:
    String m_name;
};

}
