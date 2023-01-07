/*
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

void initialize_exceptions(u32 cpu);
void init_page_tables();

}
