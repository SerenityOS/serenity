/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

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
    ProcessType type;
    pid_t pid;
    u64 memory_usage_bytes = 0;
    float cpu_percent = 0.0f;

    u64 time_spent_in_process = 0;
};

}
