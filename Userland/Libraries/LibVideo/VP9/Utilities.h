/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Video::VP9 {

#define SAFE_CALL(call)             \
    do {                            \
        if (!(call)) [[unlikely]] { \
            dbgln("FAILED " #call); \
            return false;           \
        }                           \
    } while (0)

}
