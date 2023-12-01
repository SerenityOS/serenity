/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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

struct WebContentOptions {
    EnableCallgrindProfiling enable_callgrind_profiling { EnableCallgrindProfiling::No };
    EnableGPUPainting enable_gpu_painting { EnableGPUPainting::No };
    IsLayoutTestMode is_layout_test_mode { IsLayoutTestMode::No };
    UseLagomNetworking use_lagom_networking { UseLagomNetworking::No };
};

}
