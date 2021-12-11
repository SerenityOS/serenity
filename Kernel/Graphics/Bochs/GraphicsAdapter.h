/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class GraphicsManagement;
struct BochsDisplayMMIORegisters;

class BochsGraphicsAdapter final : public GenericGraphicsAdapter
    , public PCI::Device {
    AK_MAKE_ETERNAL
    friend class GraphicsManagement;

private:
    TYPEDEF_DISTINCT_ORDERED_ID(u16, IndexID);

public:
    static NonnullRefPtr<BochsGraphicsAdapter> initialize(PCI::DeviceIdentifier const&);
    virtual ~BochsGraphicsAdapter() = default;
    virtual bool framebuffer_devices_initialized() const override { return !m_framebuffer_device.is_null(); }

    virtual bool modesetting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }

    virtual bool vga_compatible() const override;

private:
    // ^GenericGraphicsAdapter
    virtual bool try_to_set_resolution(size_t output_port_index, size_t width, size_t height) override;
    virtual bool set_y_offset(size_t output_port_index, size_t y) override;

    virtual void initialize_framebuffer_devices() override;

    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

    explicit BochsGraphicsAdapter(PCI::DeviceIdentifier const&);

    IndexID index_id() const;

    void set_safe_resolution();
    void unblank();

    bool validate_setup_resolution(size_t width, size_t height);
    u32 find_framebuffer_address();
    void set_resolution_registers(size_t width, size_t height);
    void set_resolution_registers_via_io(size_t width, size_t height);
    bool validate_setup_resolution_with_io(size_t width, size_t height);
    void set_y_offset(size_t);

    void set_framebuffer_to_big_endian_format();
    void set_framebuffer_to_little_endian_format();

    PhysicalAddress m_mmio_registers;
    Memory::TypedMapping<BochsDisplayMMIORegisters volatile> m_registers;
    RefPtr<FramebufferDevice> m_framebuffer_device;
    RefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;
    Spinlock m_console_mode_switch_lock;
    bool m_console_enabled { false };
    bool m_io_required { false };
    bool m_is_vga_capable { false };
};
}
