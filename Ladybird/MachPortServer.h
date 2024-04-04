/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if !defined(AK_OS_MACH)
#    error "This file is only for Mach kernel-based OS's"
#endif

#include <AK/Atomic.h>
#include <AK/String.h>
#include <LibCore/MachPort.h>
#include <LibThreading/Thread.h>

namespace Ladybird {

class MachPortServer {

public:
    MachPortServer();
    ~MachPortServer();

    void start();
    void stop();

    bool is_initialized();

    Function<void(pid_t, Core::MachPort)> on_receive_child_mach_port;

    ByteString const& server_port_name() const { return m_server_port_name; }

private:
    void thread_loop();
    ErrorOr<void> allocate_server_port();

    NonnullRefPtr<Threading::Thread> m_thread;
    ByteString const m_server_port_name;
    Core::MachPort m_server_port_recv_right;
    Core::MachPort m_server_port_send_right;

    Atomic<bool> m_should_stop { false };
};

}
