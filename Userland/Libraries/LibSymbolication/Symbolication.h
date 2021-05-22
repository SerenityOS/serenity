/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Symbolication {

struct Symbol {
    FlatPtr address { 0 };
    String name {};
    u32 offset { 0 };
    String filename {};
    u32 line_number { 0 };
};

Vector<Symbol> symbolicate_thread(pid_t pid, pid_t tid);
Optional<Symbol> symbolicate(String const& path, FlatPtr address);

}
