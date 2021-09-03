/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/NonnullOwnPtr.h>
#include <YAK/NonnullPtrVector.h>

namespace YAK {

template<typename T, size_t inline_capacity>
class NonnullOwnPtrVector : public NonnullPtrVector<NonnullOwnPtr<T>, inline_capacity> {
};

}

using YAK::NonnullOwnPtrVector;
