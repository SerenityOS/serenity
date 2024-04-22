/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MachPortServer.h"
#include <AK/Debug.h>
#include <LibCore/Platform/ProcessStatisticsMach.h>

namespace Ladybird {

MachPortServer::MachPortServer()
    : m_thread(Threading::Thread::construct([this]() -> intptr_t { thread_loop(); return 0; }, "MachPortServer"sv))
    , m_server_port_name(ByteString::formatted("org.SerenityOS.Ladybird.helper.{}", getpid()))
{
    if (auto err = allocate_server_port(); err.is_error())
        dbgln("Failed to allocate server port: {}", err.error());
    else
        start();
}

MachPortServer::~MachPortServer()
{
    stop();
}

void MachPortServer::start()
{
    m_thread->start();
}

void MachPortServer::stop()
{
    // FIXME: We should join instead (after storing should_stop = false) when we have a way to interrupt the thread's mach_msg call
    m_thread->detach();
    m_should_stop.store(true, MemoryOrder::memory_order_release);
}

bool MachPortServer::is_initialized()
{
    return MACH_PORT_VALID(m_server_port_recv_right.port()) && MACH_PORT_VALID(m_server_port_send_right.port());
}

ErrorOr<void> MachPortServer::allocate_server_port()
{
    m_server_port_recv_right = TRY(Core::MachPort::create_with_right(Core::MachPort::PortRight::Receive));
    m_server_port_send_right = TRY(m_server_port_recv_right.insert_right(Core::MachPort::MessageRight::MakeSend));
    TRY(m_server_port_recv_right.register_with_bootstrap_server(m_server_port_name));

    dbgln_if(MACH_PORT_DEBUG, "Success! we created and attached mach port {:x} to bootstrap server with name {}", m_server_port_recv_right.port(), m_server_port_name);
    return {};
}

void MachPortServer::thread_loop()
{
    while (!m_should_stop.load(MemoryOrder::memory_order_acquire)) {

        Core::Platform::ParentPortMessage message {};

        // Get the pid of the child from the audit trailer so we can associate the port w/it
        mach_msg_options_t const options = MACH_RCV_MSG | MACH_RCV_TRAILER_TYPE(MACH_RCV_TRAILER_AUDIT) | MACH_RCV_TRAILER_ELEMENTS(MACH_RCV_TRAILER_AUDIT);

        // FIXME: How can we interrupt this call during application shutdown?
        auto const ret = mach_msg(&message.header, options, 0, sizeof(message), m_server_port_recv_right.port(), MACH_MSG_TIMEOUT_NONE, MACH_PORT_NULL);
        if (ret != KERN_SUCCESS) {
            dbgln("mach_msg failed: {}", mach_error_string(ret));
            break;
        }

        if (message.header.msgh_id != Core::Platform::SELF_TASK_PORT_MESSAGE_ID) {
            dbgln("Received message with id {}, ignoring", message.header.msgh_id);
            continue;
        }

        if (MACH_MSGH_BITS_LOCAL(message.header.msgh_bits) != MACH_MSG_TYPE_MOVE_SEND) {
            dbgln("Received message with invalid local port rights {}, ignoring", MACH_MSGH_BITS_LOCAL(message.header.msgh_bits));
            continue;
        }

        auto pid = static_cast<pid_t>(message.trailer.msgh_audit.val[5]);
        auto child_port = Core::MachPort::adopt_right(message.port_descriptor.name, Core::MachPort::PortRight::Send);
        dbgln_if(MACH_PORT_DEBUG, "Received child port {:x} from pid {}", child_port.port(), pid);

        if (on_receive_child_mach_port)
            on_receive_child_mach_port(pid, move(child_port));
    }
}

}
