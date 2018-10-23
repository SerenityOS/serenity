#pragma once

#include "types.h"
#include "DataBuffer.h"
#include "RefPtr.h"
#include <AK/StdLib.h>

/* IPC message types. There will be moar. */
#define MSG_INTERRUPT 0x00000001
#define MSG_KILL      0x00000002
#define MSG_NOTIFY    0x00000003

#define DEV_READ      0x00000004

#define FS_OPEN       0x00000100
#define FS_CLOSE      0x00000101
#define FS_READ       0x00000102

#define SYS_KILL      0x00000666

namespace IPC {

class Handle {
public:
    // If Handle::Any is passed as the `src' parameter of receive(),
    // any process can send us a message.
    enum AnyHandle { Any };
    Handle(AnyHandle) : m_data(0xffffffff) { }

    enum KernelTask {
        DiskTask = 4002,
        FileSystemTask = 4003,
        MotdTask = 4004,
        UserTask = 4005,
        InitTask = 4006,
    };
    Handle(KernelTask task) : m_data((DWORD)task) { }

    Handle() { }
    explicit Handle(DWORD data) : m_data(data) { }

    DWORD data() const { return m_data; }
    bool operator==(const Handle& o) const { return m_data == o.m_data; }
    bool operator!=(const Handle& o) const { return m_data != o.m_data; }

private:
    DWORD m_data { 0 };
};

class Message {
public:
    Message() { }
    explicit Message(DWORD type) : m_type(type), m_isValid(true) { }
    Message(DWORD type, RefPtr<DataBuffer>&& d) : m_data(move(d)), m_type(type), m_isValid(true) { }
    Message(Message&&);
    Message& operator=(Message&&);

    size_t length() const { return m_data ? m_data->length() : 0; }
    const BYTE* data() const { return m_data ? m_data->data() : nullptr; }
    BYTE* data() { return m_data ? m_data->data() : nullptr; }

    bool isValid() const { return m_isValid; }

    DWORD type() const { return m_type; }
    Handle sender() const { return m_sender; }

    void setType(DWORD t) { m_type = t; }
    void setSender(Handle s) { m_sender = s; }

private:
    RefPtr<DataBuffer> m_data;
    DWORD m_type { 0 };
    Handle m_sender;
    bool m_isValid { false };
};

Message receive(Handle);
void send(Handle, Message&&);
void notify(Handle);

}

