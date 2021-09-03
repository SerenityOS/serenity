/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/NonnullPtrVector.h>
#include <YAK/NonnullRefPtr.h>

namespace YAK {

template<typename T, size_t inline_capacity>
class NonnullRefPtrVector : public NonnullPtrVector<NonnullRefPtr<T>, inline_capacity> {
    using NonnullPtrVector<NonnullRefPtr<T>, inline_capacity>::NonnullPtrVector;
};

}

using YAK::NonnullRefPtrVector;
