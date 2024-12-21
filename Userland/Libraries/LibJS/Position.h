/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace JS {

struct Position {
    size_t line { 0 };
    size_t column { 0 };
    size_t offset { 0 };
};

}
