/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"

namespace regex {

class Optimizer {
public:
    static void append_alternation(ByteCode& target, ByteCode& left, ByteCode& right);
};

}
