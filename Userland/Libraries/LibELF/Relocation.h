/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/VirtualAddress.h>

namespace ELF {

bool perform_relative_relocations(FlatPtr base_address);

}
