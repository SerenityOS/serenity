/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <unistd.h>

namespace IPC {

class AutoCloseFileDescriptor : public RefCounted<AutoCloseFileDescriptor> {
public:
    AutoCloseFileDescriptor(int fd)
        : m_fd(fd)
    {
    }

    ~AutoCloseFileDescriptor()
    {
        if (m_fd != -1)
            close(m_fd);
    }

    int value() const { return m_fd; }

private:
    int m_fd;
};

struct MessageBuffer {
    Vector<u8, 1024> data;
    NonnullRefPtrVector<AutoCloseFileDescriptor, 1> fds;
};

enum class ErrorCode : u32 {
    PeerDisconnected
};

template<typename Value>
using IPCErrorOr = ErrorOr<Value, ErrorCode>;

class Message {
public:
    virtual ~Message() = default;

    virtual u32 endpoint_magic() const = 0;
    virtual int message_id() const = 0;
    virtual char const* message_name() const = 0;
    virtual bool valid() const = 0;
    virtual MessageBuffer encode() const = 0;

protected:
    Message() = default;
};

}
