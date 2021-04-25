/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Vector.h>

namespace IPC {

struct MessageBuffer {
    Vector<u8, 1024> data;
    Vector<int> fds;
};

class Message {
public:
    virtual ~Message();

    virtual u32 endpoint_magic() const = 0;
    virtual int message_id() const = 0;
    virtual const char* message_name() const = 0;
    virtual MessageBuffer encode() const = 0;

protected:
    Message();
};

}
