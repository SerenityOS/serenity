/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>

#include "LibDebug/DebugSession.h"

namespace Debug::StackFrameUtils {

struct StackFrameInfo {
    FlatPtr return_address;
    FlatPtr next_ebp;
};

Optional<StackFrameInfo> get_info(ProcessInspector const&, FlatPtr current_ebp);

}
