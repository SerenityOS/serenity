/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/OwnPtr.h>

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
    virtual DeprecatedString name() const = 0;
    virtual ErrorOr<OwnPtr<MessageBuffer>> handle(Message const&) = 0;

protected:
    Stub() = default;

private:
    DeprecatedString m_name;
};

}
