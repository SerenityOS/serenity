/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AarchRegisters.h"

extern "C" uint8_t get_current_exception_level();
extern "C" void wait_cycles(int n);

// CPU initialization functions
extern "C" [[noreturn]] void return_from_el3();
