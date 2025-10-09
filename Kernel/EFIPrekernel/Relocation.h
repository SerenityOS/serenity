/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibELF/Image.h>

namespace Kernel {

void perform_kernel_relocations(ELF::Image const& kernel_elf_image, Bytes kernel_elf_image_data, FlatPtr base_address);

}
