/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

enum class ExceptionLevel : u8 {
    EL0 = 0,
    EL1 = 1,
    EL2 = 2,
    EL3 = 3,
};

extern "C" ExceptionLevel get_current_exception_level();
extern "C" void wait_cycles(int n);

// CPU initialization functions
extern "C" [[noreturn]] void return_from_el2();
extern "C" [[noreturn]] void return_from_el3();
