/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <Kernel/Locking/RefCountedContended.h>

namespace Kernel {

template<typename T>
using RefContendedPtr = RefPtr<RefCountedContended<T>>;

}
