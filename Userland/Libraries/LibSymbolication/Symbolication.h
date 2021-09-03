/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/String.h>
#include <LibDebug/DebugInfo.h>

namespace Symbolication {

struct Symbol {
    FlatPtr address { 0 };
    String name {};
    u32 offset { 0 };
    Vector<Debug::DebugInfo::SourcePosition> source_positions;
};

Optional<FlatPtr> kernel_base();
Vector<Symbol> symbolicate_thread(pid_t pid, pid_t tid);
Optional<Symbol> symbolicate(String const& path, FlatPtr address);

}
