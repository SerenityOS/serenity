/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/SpinlockProtectedBase.h>

namespace Kernel {

template<typename T, LockRank Rank>
class SpinlockProtected : public SpinlockProtectedBase<T, Spinlock<Rank>> { };

template<typename T, LockRank Rank>
class RecursiveSpinlockProtected : public SpinlockProtectedBase<T, RecursiveSpinlock<Rank>> { };

}
