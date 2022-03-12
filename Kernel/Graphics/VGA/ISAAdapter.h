/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/VGA/GenericAdapter.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class ISAVGAAdapter final : public VGAGenericAdapter {
    friend class GraphicsManagement;

public:
    static NonnullRefPtr<ISAVGAAdapter> must_create_with_preset_resolution(PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    static NonnullRefPtr<ISAVGAAdapter> must_create();

private:
    ISAVGAAdapter();
};
}
