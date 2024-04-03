/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>
#include <LibWebView/Platform/ProcessInfo.h>

namespace WebView {

struct ProcessStatistics {
    u64 total_time_scheduled = 0;
    Vector<ProcessInfo> processes;
};

ErrorOr<void> update_process_statistics(ProcessStatistics&);

}
