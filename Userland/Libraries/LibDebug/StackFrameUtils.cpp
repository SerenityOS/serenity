/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "StackFrameUtils.h"

namespace Debug::StackFrameUtils {

Optional<StackFrameInfo> get_info(ProcessInspector const& inspector, FlatPtr current_ebp)
{
    auto return_address = inspector.peek(current_ebp + sizeof(FlatPtr));
    auto next_ebp = inspector.peek(current_ebp);
    if (!return_address.has_value() || !next_ebp.has_value())
        return {};

    StackFrameInfo info = { return_address.value(), next_ebp.value() };
    return info;
}

}
