/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Graphics/Console/FramebufferConsole.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/PCI/DeviceController.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {

class GraphicsManagement;
class QXLDevice;
class QXLGraphicsAdapter final : public GraphicsDevice
    , public PCI::DeviceController {
    AK_MAKE_ETERNAL
    friend class GraphicsManagement;

    static constexpr int max_monitors = 4;

public:
    static RefPtr<QXLGraphicsAdapter> initialize(PCI::Address);
    virtual ~QXLGraphicsAdapter() = default;
    virtual bool framebuffer_devices_initialized() const override { return !m_framebuffer_device.is_null(); }

    virtual bool modesetting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }

private:
    bool initialize();

    // ^GraphicsDevice
    virtual bool try_to_set_resolution(size_t output_port_index, size_t width, size_t height) override;
    virtual bool set_y_offset(size_t output_port_index, size_t y) override;

    virtual void initialize_framebuffer_devices() override;
    virtual Type type() const override;

    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

    explicit QXLGraphicsAdapter(PCI::Address);

    void set_safe_resolution();

    bool validate_setup_resolution(size_t width, size_t height);
    u32 find_framebuffer_address();
    void set_resolution_registers(size_t width, size_t height);

    OwnPtr<QXLDevice> m_device;

    RefPtr<FramebufferDevice> m_framebuffer_device;
    RefPtr<Graphics::FramebufferConsole> m_framebuffer_console;
    SpinLock<u8> m_console_mode_switch_lock;
    bool m_console_enabled { false };
};

}
