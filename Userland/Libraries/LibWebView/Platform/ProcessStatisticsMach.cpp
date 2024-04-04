/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>

#if !defined(AK_OS_MACH)
#    error "This file is only available on Mach platforms"
#endif

#include <AK/ByteString.h>
#include <LibCore/MachPort.h>
#include <LibWebView/Platform/ProcessStatisticsMach.h>

namespace WebView {

ErrorOr<void> update_process_statistics(ProcessStatistics&)
{
    return {};
}

void register_with_mach_server(ByteString const& server_name)
{
    auto server_port_or_error = Core::MachPort::look_up_from_bootstrap_server(server_name);
    if (server_port_or_error.is_error()) {
        dbgln("Failed to lookup server port: {}", server_port_or_error.error());
        return;
    }
    auto server_port = server_port_or_error.release_value();

    // Send our own task port to the server so they can query statistics about us
    ChildPortMessage message {};
    message.header.msgh_bits = MACH_MSGH_BITS(MACH_MSG_TYPE_COPY_SEND, MACH_MSGH_BITS_ZERO) | MACH_MSGH_BITS_COMPLEX;
    message.header.msgh_size = sizeof(message);
    message.header.msgh_remote_port = server_port.port();
    message.header.msgh_local_port = MACH_PORT_NULL;
    message.header.msgh_id = SELF_TASK_PORT_MESSAGE_ID;
    message.body.msgh_descriptor_count = 1;
    message.port_descriptor.name = mach_task_self();
    message.port_descriptor.disposition = MACH_MSG_TYPE_COPY_SEND;
    message.port_descriptor.type = MACH_MSG_PORT_DESCRIPTOR;

    mach_msg_timeout_t const timeout = 100; // milliseconds

    auto const send_result = mach_msg(&message.header, MACH_SEND_MSG | MACH_SEND_TIMEOUT, message.header.msgh_size, 0, MACH_PORT_NULL, timeout, MACH_PORT_NULL);
    if (send_result != KERN_SUCCESS) {
        dbgln("Failed to send message to server: {}", mach_error_string(send_result));
        return;
    }
}

}
