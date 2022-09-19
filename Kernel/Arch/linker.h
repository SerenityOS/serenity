/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

extern u8 start_of_kernel_image[];
extern u8 end_of_kernel_image[];
extern u8 start_of_kernel_text[];
extern u8 start_of_kernel_data[];
extern u8 end_of_kernel_bss[];
extern u8 start_of_ro_after_init[];
extern u8 end_of_ro_after_init[];
extern u8 start_of_unmap_after_init[];
extern u8 end_of_unmap_after_init[];
extern u8 start_of_kernel_ksyms[];
extern u8 end_of_kernel_ksyms[];
