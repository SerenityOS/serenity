/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VMWare/Definitions.h>
#include <Kernel/IOWindow.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class GraphicsManagement;

class VMWareDisplayConnector;
class VMWareGraphicsAdapter final
    : public GenericGraphicsAdapter
    , public PCI::Device {
    friend class GraphicsManagement;

public:
    static LockRefPtr<VMWareGraphicsAdapter> try_initialize(PCI::DeviceIdentifier const&);
    virtual ~VMWareGraphicsAdapter() = default;

    ErrorOr<void> modeset_primary_screen_resolution(Badge<VMWareDisplayConnector>, size_t width, size_t height);
    size_t primary_screen_width(Badge<VMWareDisplayConnector>) const;
    size_t primary_screen_height(Badge<VMWareDisplayConnector>) const;
    size_t primary_screen_pitch(Badge<VMWareDisplayConnector>) const;
    void primary_screen_flush(Badge<VMWareDisplayConnector>, size_t current_width, size_t current_height);

private:
    ErrorOr<void> initialize_adapter();
    ErrorOr<void> initialize_fifo_registers();
    ErrorOr<void> negotiate_device_version();

    u32 read_io_register(VMWareDisplayRegistersOffset) const;
    void write_io_register(VMWareDisplayRegistersOffset, u32 value);

    void print_svga_capabilities() const;
    void modeset_primary_screen_resolution(size_t width, size_t height);

    VMWareGraphicsAdapter(PCI::DeviceIdentifier const&, NonnullOwnPtr<IOWindow> registers_io_window);

    Memory::TypedMapping<volatile VMWareDisplayFIFORegisters> m_fifo_registers;
    LockRefPtr<VMWareDisplayConnector> m_display_connector;
    mutable NonnullOwnPtr<IOWindow> m_registers_io_window;
    mutable Spinlock m_io_access_lock { LockRank::None };
    mutable RecursiveSpinlock m_operation_lock { LockRank::None };
};

}
