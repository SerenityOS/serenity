/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Prekernel {

void drop_to_exception_level_1();
void init_prekernel_page_tables();

[[noreturn]] void panic(const char* msg);

[[noreturn]] void halt();

}
