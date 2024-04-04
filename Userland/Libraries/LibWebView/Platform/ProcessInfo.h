/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#if defined(AK_OS_MACH)
#    include <LibCore/MachPort.h>
#endif

namespace WebView {

enum class ProcessType {
    Chrome,
    WebContent,
    WebWorker,
    SQLServer,
    RequestServer,
    ImageDecoder,
};

struct ProcessInfo {
    ProcessInfo(ProcessType type, pid_t pid)
        : type(type)
        , pid(pid)
    {
    }

    ProcessType type = ProcessType::WebContent;
    pid_t pid;
    u64 memory_usage_bytes = 0;
    float cpu_percent = 0.0f;

    u64 time_spent_in_process = 0;

#if defined(AK_OS_MACH)
    Core::MachPort child_task_port;

    ProcessInfo(pid_t pid, Core::MachPort&& port)
        : pid(pid)
        , child_task_port(move(port))
    {
    }
#endif
};

}
