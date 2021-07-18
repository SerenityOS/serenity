/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullPtrVector.h>
#include <Kernel/Locking/NonnullRefContendedPtr.h>

namespace Kernel {

template<typename T, size_t inline_capacity = 0>
using NonnullRefContendedPtrVector = AK::NonnullPtrVector<NonnullRefContendedPtr<T>, inline_capacity>;

}
