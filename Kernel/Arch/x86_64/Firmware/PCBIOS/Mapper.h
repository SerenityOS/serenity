/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/MappedROM.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/VirtualAddress.h>

namespace Kernel {

ErrorOr<Memory::MappedROM> map_bios();
ErrorOr<Memory::MappedROM> map_ebda();

}
