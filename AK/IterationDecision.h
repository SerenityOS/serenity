/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace AK {

enum class IterationDecision {
    Continue,
    Break,
};

}

using AK::IterationDecision;
