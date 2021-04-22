/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/VM/MappedROM.h>

namespace Kernel {

MappedROM map_bios();
MappedROM map_ebda();

}
