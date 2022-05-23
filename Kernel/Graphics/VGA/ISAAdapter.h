/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VGA/VGACompatibleAdapter.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class ISAVGAAdapter final : public VGACompatibleAdapter {
    friend class GraphicsManagement;

public:
    static NonnullRefPtr<ISAVGAAdapter> initialize();

private:
    ISAVGAAdapter();
    RefPtr<Graphics::Console> m_framebuffer_console;
};
}
