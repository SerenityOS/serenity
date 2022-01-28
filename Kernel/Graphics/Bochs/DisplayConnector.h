/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

struct BochsDisplayMMIORegisters;
class BochsDisplayConnector
    : public DisplayConnector {
    friend class BochsGraphicsAdapter;

public:
    TYPEDEF_DISTINCT_ORDERED_ID(u16, IndexID);

    static NonnullOwnPtr<BochsDisplayConnector> must_create(PhysicalAddress framebuffer_address, NonnullOwnPtr<Memory::Region> registers_region, size_t registers_region_offset);

    virtual IndexID index_id() const;

protected:
    ErrorOr<void> create_attached_framebuffer_console();

    explicit BochsDisplayConnector(PhysicalAddress framebuffer_address);

private:
    BochsDisplayConnector(PhysicalAddress framebuffer_address, NonnullOwnPtr<Memory::Region> registers_region, size_t registers_region_offset);

    void enable_console();
    void disable_console();

    virtual bool modesetting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }
    virtual ErrorOr<ByteBuffer> get_edid() const override;
    virtual ErrorOr<void> set_resolution(Resolution const&) override;
    virtual ErrorOr<void> set_safe_resolution() override;
    virtual ErrorOr<Resolution> get_resolution() override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> unblank() override;

    void set_framebuffer_to_big_endian_format();
    void set_framebuffer_to_little_endian_format();

protected:
    Mutex m_modeset_lock;

private:
    const PhysicalAddress m_framebuffer_address;
    Memory::TypedMapping<BochsDisplayMMIORegisters volatile> m_registers;
    RefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;
};
}
