/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace SSH {

enum class BehaviorControl : u8 {
    ContinueExecution,
    WaitForMoreData,
    Disconnect,
};

}
