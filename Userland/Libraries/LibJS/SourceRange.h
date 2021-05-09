/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace JS {

struct Position {
    size_t line { 0 };
    size_t column { 0 };
};

struct SourceRange {
    StringView filename;
    Position start;
    Position end;
};

}
