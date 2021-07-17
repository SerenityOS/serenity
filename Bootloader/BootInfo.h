/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <Kernel/Multiboot.h>

namespace Bootloader {

struct [[gnu::packed]] BootInfo {
    u32 kernel_cmdline_ptr;
};
}
