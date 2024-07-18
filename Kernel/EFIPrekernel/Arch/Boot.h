/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Prekernel/Prekernel.h>

#include <Kernel/EFIPrekernel/Error.h>

namespace Kernel {

void arch_prepare_boot(void* root_page_table, BootInfo& boot_info);
[[noreturn]] void arch_enter_kernel(void* root_page_table, FlatPtr kernel_entry_vaddr, FlatPtr kernel_stack_pointer, FlatPtr boot_info_vaddr);

}
