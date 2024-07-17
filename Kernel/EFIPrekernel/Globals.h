/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Firmware/EFI/SystemTable.h>

namespace Kernel {

extern EFI::Handle g_efi_image_handle;
extern EFI::SystemTable* g_efi_system_table;

}
