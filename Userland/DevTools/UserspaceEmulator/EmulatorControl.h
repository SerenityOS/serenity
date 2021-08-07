/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <serenity.h>

namespace UserspaceEmulator {

enum class Command {
    MarkROIStart = 5,
    MarkROIEnd = 6,
};

inline void control(Command command)
{
    emuctl(to_underlying(command), 0, 0);
}

}
