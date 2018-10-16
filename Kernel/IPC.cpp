#include "IPC.h"
#include "Task.h"
#include "i386.h"
#include "StdLib.h"
#include "VGA.h"
#include "system.h"

namespace IPC {

Message receive(Handle src)
{
    for (;;) {
        current->ipc.src = src;
        block(Task::BlockedReceive);
        if (src == Handle::Any && current->ipc.notifies) {
            for (BYTE i = 0; i < 32; ++i) {
                if (current->ipc.notifies & (1 << i)) {
                    // FIXME: Source PID is `i' here. Do something with it?
                    current->ipc.notifies &= ~(1 << i);
                    break;
                }
            }
            return Message(MSG_NOTIFY);
        }

        if (src == Handle::Any || src == current->ipc.msg.sender()) {
            return move(current->ipc.msg);
        }

        // Why are we here?
        ASSERT_NOT_REACHED();
    }
}

void send(Handle dst, Message&& msg)
{
    Task* task;

    // TODO: Block waiting for `dst' to spawn.
    for (;;) {
        task = Task::fromIPCHandle(dst);
        if (task)
            break;
        yield();
    }

    // I'll fill this in myself thankyouverymuch.
    msg.setSender(current->handle());

    // Block until `dst' is ready to receive a message.
    current->ipc.dst = dst;
    block(Task::BlockedSend);

    ASSERT(msg.isValid());
    task->ipc.msg = move(msg);
}

void notify(Handle dst)
{
    Task* task = Task::fromIPCHandle(dst);

    if (!task) {
        // Can't really block here since we might be coming from
        // an interrupt handler and that'd be devastating...
        // XXX: Need to figure that one out.
        kprintf("notify(): no such task %u\n", dst.data());
        return;
    }

    if (current->pid() >= 32) {
        kprintf( "notify(): PID must be < 32\n" );
        return;
    }

    task->ipc.notifies |= 1 << current->pid();
}

Message::Message(Message&& other)
    : m_data(move(other.m_data))
    , m_type(other.m_type)
    , m_sender(other.m_sender)
    , m_isValid(other.m_isValid)
{
    other.m_type = 0;
    other.m_sender = Handle();
    other.m_isValid = false;
}

Message& Message::operator=(Message&& other)
{
    if (this == &other)
        return *this;
    m_data = move(other.m_data);
    m_type = other.m_type;
    m_sender = other.m_sender;
    m_isValid = other.m_isValid;
    other.m_type = 0;
    other.m_sender = Handle();
    other.m_isValid = false;
    return *this;
}


}
