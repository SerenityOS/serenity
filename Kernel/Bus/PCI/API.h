/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::PCI {

void write8(Address address, u32 field, u8 value);
void write16(Address address, u32 field, u16 value);
void write32(Address address, u32 field, u32 value);
u8 read8(Address address, u32 field);
u16 read16(Address address, u32 field);
u32 read32(Address address, u32 field);

ID get_id(PCI::Address);
bool is_io_space_enabled(Address);
void enumerate(Function<void(Address, PhysicalID const&)> callback);
void enable_interrupt_line(Address);
void disable_interrupt_line(Address);
u8 get_interrupt_line(Address);
void raw_access(Address, u32, size_t, u32);
u32 get_BAR0(Address);
u32 get_BAR1(Address);
u32 get_BAR2(Address);
u32 get_BAR3(Address);
u32 get_BAR4(Address);
u32 get_BAR5(Address);
u32 get_BAR(Address address, u8 bar);
u8 get_revision_id(Address);
u8 get_programming_interface(Address);
u8 get_subclass(Address);
u8 get_class(Address);
u16 get_subsystem_id(Address);
u16 get_subsystem_vendor_id(Address);
size_t get_BAR_space_size(Address, u8);
void enable_bus_mastering(Address);
void disable_bus_mastering(Address);
void enable_io_space(Address);
void disable_io_space(Address);
void enable_memory_space(Address);
void disable_memory_space(Address);
PhysicalID get_physical_id(Address address);

}
