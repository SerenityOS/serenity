/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Try.h>
#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::PCI {

void write8_locked(DeviceIdentifier const&, PCI::RegisterOffset field, u8 value);
void write16_locked(DeviceIdentifier const&, PCI::RegisterOffset field, u16 value);
void write32_locked(DeviceIdentifier const&, PCI::RegisterOffset field, u32 value);
u8 read8_locked(DeviceIdentifier const&, PCI::RegisterOffset field);
u16 read16_locked(DeviceIdentifier const&, PCI::RegisterOffset field);
u32 read32_locked(DeviceIdentifier const&, PCI::RegisterOffset field);

HardwareID get_hardware_id(DeviceIdentifier const&);
bool is_io_space_enabled(DeviceIdentifier const&);
ErrorOr<void> enumerate(Function<void(DeviceIdentifier const&)> callback);
void enable_interrupt_line(DeviceIdentifier const&);
void disable_interrupt_line(DeviceIdentifier const&);
void raw_access(DeviceIdentifier const&, u32, size_t, u32);

u32 get_BAR0(DeviceIdentifier const&);
u32 get_BAR1(DeviceIdentifier const&);
u32 get_BAR2(DeviceIdentifier const&);
u32 get_BAR3(DeviceIdentifier const&);
u32 get_BAR4(DeviceIdentifier const&);
u32 get_BAR5(DeviceIdentifier const&);
u32 get_BAR(DeviceIdentifier const&, HeaderType0BaseRegister);
size_t get_BAR_space_size(DeviceIdentifier const&, HeaderType0BaseRegister);
BARSpaceType get_BAR_space_type(u32 pci_bar_value);
size_t get_expansion_rom_space_size(DeviceIdentifier const&);

void enable_bus_mastering(DeviceIdentifier const&);
void disable_bus_mastering(DeviceIdentifier const&);
void enable_io_space(DeviceIdentifier const&);
void disable_io_space(DeviceIdentifier const&);
void enable_memory_space(DeviceIdentifier const&);
void disable_memory_space(DeviceIdentifier const&);

// FIXME: Remove this once we can use PCI::Capability with inline buffer
// so we don't need this method
DeviceIdentifier const& get_device_identifier(Address address);
}
