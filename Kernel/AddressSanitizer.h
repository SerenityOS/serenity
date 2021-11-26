/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::AddressSanitizer {

void shadow_va_check_load(unsigned long address, size_t size, void* return_addr);

void shadow_va_check_store(unsigned long address, size_t size, void* return_addr);

}
