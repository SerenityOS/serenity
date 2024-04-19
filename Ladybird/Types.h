/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Ladybird {

enum class EnableCallgrindProfiling {
    No,
    Yes
};

enum class EnableGPUPainting {
    No,
    Yes
};

enum class IsLayoutTestMode {
    No,
    Yes
};

enum class UseLagomNetworking {
    No,
    Yes
};

enum class WaitForDebugger {
    No,
    Yes
};

enum class LogAllJSExceptions {
    No,
    Yes
};

enum class EnableIDLTracing {
    No,
    Yes
};

struct WebContentOptions {
    String command_line;
    String executable_path;
    EnableCallgrindProfiling enable_callgrind_profiling { EnableCallgrindProfiling::No };
    EnableGPUPainting enable_gpu_painting { EnableGPUPainting::No };
    IsLayoutTestMode is_layout_test_mode { IsLayoutTestMode::No };
    UseLagomNetworking use_lagom_networking { UseLagomNetworking::No };
    WaitForDebugger wait_for_debugger { WaitForDebugger::No };
    LogAllJSExceptions log_all_js_exceptions { LogAllJSExceptions::No };
    EnableIDLTracing enable_idl_tracing { EnableIDLTracing::No };
};

}
