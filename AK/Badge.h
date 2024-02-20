/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

namespace AK {

template<typename T>
class Badge {
public:
    using Type = T;

    Badge(Badge&&) = default;
    Badge& operator=(Badge&&) = default;

private:
    friend T;
    constexpr Badge() = default;

    Badge(Badge const&) = delete;
    Badge& operator=(Badge const&) = delete;
};

}

#if USING_AK_GLOBALLY
using AK::Badge;
#endif
