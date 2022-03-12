/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGA/ISAAdapter.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<ISAVGAAdapter> ISAVGAAdapter::must_create_with_preset_resolution(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    auto adapter = adopt_ref_if_nonnull(new (nothrow) ISAVGAAdapter()).release_nonnull();
    MUST(adapter->initialize_adapter_with_preset_resolution(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch));
    return adapter;
}
UNMAP_AFTER_INIT NonnullRefPtr<ISAVGAAdapter> ISAVGAAdapter::must_create()
{
    auto adapter = adopt_ref_if_nonnull(new (nothrow) ISAVGAAdapter()).release_nonnull();
    MUST(adapter->initialize_adapter());
    return adapter;
}

UNMAP_AFTER_INIT ISAVGAAdapter::ISAVGAAdapter()
{
}

}
