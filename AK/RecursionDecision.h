/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace AK {

enum class RecursionDecision {
    Recurse,
    Continue,
    Break,
};

}

using AK::RecursionDecision;
