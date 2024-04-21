/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if !defined(AK_OS_MACH)
#    error "This file is only available on Mach platforms"
#endif

#include <LibCore/Platform/ProcessStatistics.h>
#include <mach/mach.h>

namespace Core::Platform {

struct ChildPortMessage {
    mach_msg_header_t header;
    mach_msg_body_t body;
    mach_msg_port_descriptor_t port_descriptor;
};

struct ParentPortMessage {
    mach_msg_header_t header;
    mach_msg_body_t body;
    mach_msg_port_descriptor_t port_descriptor;
    mach_msg_audit_trailer_t trailer; // for the child's pid
};

static constexpr mach_msg_id_t SELF_TASK_PORT_MESSAGE_ID = 0x1234CAFE;

void register_with_mach_server(ByteString const& server_name);

}
