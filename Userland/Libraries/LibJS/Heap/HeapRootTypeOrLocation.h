/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SourceLocation.h>

namespace JS {

enum class HeapRootType {
    Handle,
    MarkedVector,
    RegisterPointer,
    StackPointer,
    VM,
};

using HeapRootTypeOrLocation = Variant<HeapRootType, SourceLocation*>;

}
