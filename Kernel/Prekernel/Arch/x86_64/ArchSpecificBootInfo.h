/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Memory/PhysicalAddress.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

struct ArchSpecificBootInfo {
    PhysicalAddress boot_pd0 { 0 };
};

}
