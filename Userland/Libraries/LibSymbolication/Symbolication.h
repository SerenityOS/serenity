/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <LibDebug/DebugInfo.h>

namespace Symbolication {

struct Symbol {
    FlatPtr address { 0 };
    ByteString name {};
    ByteString object {};
    u32 offset { 0 };
    Vector<Debug::DebugInfo::SourcePosition> source_positions;
    bool operator==(Symbol const&) const = default;
};

enum class IncludeSourcePosition {
    Yes,
    No
};

Optional<FlatPtr> kernel_base();
Vector<Symbol> symbolicate_thread(pid_t pid, pid_t tid, IncludeSourcePosition = IncludeSourcePosition::Yes);
Optional<Symbol> symbolicate(ByteString const& path, FlatPtr address, IncludeSourcePosition = IncludeSourcePosition::Yes);

}
