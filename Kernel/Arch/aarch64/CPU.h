/*
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define PAGE_MASK (~(FlatPtr)0xfffu)

namespace Kernel {

void drop_to_exception_level_1();
void init_page_tables();

}
