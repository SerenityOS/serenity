/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Try.h>
#include <Kernel/Devices/GPU/Bochs/Definitions.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

struct BochsDisplayMMIORegisters;
class QEMUDisplayConnector final
    : public DisplayConnector {
    friend class BochsGraphicsAdapter;
    friend class Device;

public:
    AK_TYPEDEF_DISTINCT_ORDERED_ID(u16, IndexID);

    static ErrorOr<NonnullRefPtr<QEMUDisplayConnector>> create(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, Memory::TypedMapping<BochsDisplayMMIORegisters volatile>);

private:
    IndexID index_id() const;

    ErrorOr<void> fetch_and_initialize_edid();
    ErrorOr<void> create_attached_framebuffer_console();
    QEMUDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, Memory::TypedMapping<BochsDisplayMMIORegisters volatile>);

    virtual bool mutable_mode_setting_capable() const override final { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> set_safe_mode_setting() override final;
    virtual ErrorOr<void> unblank() override;
    virtual bool partial_flush_support() const override final { return false; }
    virtual bool flush_support() const override final { return false; }
    // Note: Paravirtualized hardware doesn't require a defined refresh rate for modesetting.
    virtual bool refresh_rate_support() const override final { return false; }
    virtual ErrorOr<void> flush_first_surface() override final;

    void set_framebuffer_to_big_endian_format();
    void set_framebuffer_to_little_endian_format();

    virtual void enable_console() override final;
    virtual void disable_console() override final;

    LockRefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;

    Memory::TypedMapping<BochsDisplayMMIORegisters volatile> m_registers;
};
}
