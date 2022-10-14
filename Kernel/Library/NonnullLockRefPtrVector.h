/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullPtrVector.h>
#include <Kernel/Library/NonnullLockRefPtr.h>

namespace AK {

template<typename T, size_t inline_capacity>
class NonnullLockRefPtrVector : public NonnullPtrVector<NonnullLockRefPtr<T>, inline_capacity> {
    using NonnullPtrVector<NonnullLockRefPtr<T>, inline_capacity>::NonnullPtrVector;
};

}

using AK::NonnullLockRefPtrVector;
