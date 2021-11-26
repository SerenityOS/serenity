/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>

namespace Kernel::Graphics {
class VGAConsole : public Console {
public:
    // Note: these are the modes we will support and only these
    enum class Mode {
        TextMode = 1, // Text Mode
        Colored256,   // 320x200 256 color mode
        Colored16,    // 640x480 16 color mode
    };

public:
    static NonnullRefPtr<VGAConsole> initialize(const VGACompatibleAdapter&, Mode, size_t width, size_t height);

    virtual bool is_hardware_paged_capable() const override { return false; }
    virtual bool has_hardware_cursor() const override { return false; }
    virtual void flush(size_t, size_t, size_t, size_t) override { }

    virtual ~VGAConsole() = default;

protected:
    VGAConsole(const VGACompatibleAdapter&, Mode, size_t width, size_t height);

    NonnullOwnPtr<Memory::Region> m_vga_region;
    NonnullRefPtr<VGACompatibleAdapter> m_adapter;
    const Mode m_mode;
};
}
