/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

namespace AK {

enum class RecursionDecision {
    Recurse,
    Continue,
    Break,
};

}

#if USING_AK_GLOBALLY
using AK::RecursionDecision;
#endif
