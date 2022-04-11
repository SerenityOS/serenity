/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

extern "C" void wait_cycles(int n);
extern "C" void el1_vector_table_install(void* vector_table);

// CPU initialization functions
extern "C" [[noreturn]] void return_from_el2();
extern "C" [[noreturn]] void return_from_el3();
