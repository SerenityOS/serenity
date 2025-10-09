/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Prekernel/Prekernel.h>

namespace Kernel {

void init_gop_and_populate_framebuffer_boot_info(BootInfo&);

}
