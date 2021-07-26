/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/VirtualAddress.h>

namespace ELF {

bool perform_relative_relocations(FlatPtr base_address);

}
