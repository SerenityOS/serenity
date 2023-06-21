/*
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Types.h>

namespace Kernel {

void initialize_exceptions();
void panic_without_mmu(StringView);
void dbgln_without_mmu(StringView);

namespace Memory {

void init_page_tables();
void unmap_identity_map();

}

}
