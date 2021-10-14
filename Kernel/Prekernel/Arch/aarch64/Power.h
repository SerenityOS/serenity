/*
 * Copyright (c) 2021, Undefine <cqundefine@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Prekernel {

class Power {
public:
    static Power& the();

    [[noreturn]] void reset();

private:
    Power() {};
};

}