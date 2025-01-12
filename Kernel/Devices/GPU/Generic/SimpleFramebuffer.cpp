/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericShorthands.h>
#include <Kernel/Boot/BootInfo.h>
#include <Kernel/Devices/GPU/Console/BootFramebufferConsole.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel {
extern Atomic<Graphics::Console*> g_boot_console;
}

namespace Kernel::Graphics {

static constinit Array const compatibles_array = {
    "simple-framebuffer"sv,
};

DEVICETREE_DRIVER(SimpleFramebufferDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/display/simple-framebuffer.yaml
ErrorOr<void> SimpleFramebufferDriver::probe(DeviceTree::Device const& device, StringView) const
{
    // Prefer to use the bootloader-provided framebuffer, if available.
    if (!g_boot_info.boot_framebuffer.paddr.is_null() && g_boot_info.boot_framebuffer.type == BootFramebufferType::BGRx8888)
        return {};

    auto maybe_width = device.node().get_property("width"sv);
    if (!maybe_width.has_value())
        return EINVAL;

    auto maybe_height = device.node().get_property("height"sv);
    if (!maybe_height.has_value())
        return EINVAL;

    auto maybe_stride = device.node().get_property("stride"sv);
    if (!maybe_stride.has_value())
        return EINVAL;

    auto maybe_format = device.node().get_property("format"sv);
    if (!maybe_format.has_value())
        return EINVAL;

    if (!first_is_one_of(maybe_format.value().as_string(), "a8r8g8b8"sv, "x8r8g8b8"sv))
        return ENOTSUP;

    auto framebuffer_resource = TRY(device.get_resource(0));

    g_boot_info.boot_framebuffer = {
        .paddr = framebuffer_resource.paddr,
        .pitch = maybe_stride.release_value().as<u32>(),
        .width = maybe_width.release_value().as<u32>(),
        .height = maybe_height.release_value().as<u32>(),
        .bpp = 32,
        .type = BootFramebufferType::BGRx8888,
    };

    // Devicetree drivers are probed after the initial boot console is set up, so we need to re-initialize the g_boot_console to use the this framebuffer.
    // g_boot_console should currently be a BootDummyConsole, as we ignore the simple-framebuffer node if the bootloader provided a framebuffer.
    auto boot_console = TRY(try_make_lock_ref_counted<Graphics::BootFramebufferConsole>(g_boot_info.boot_framebuffer.paddr, g_boot_info.boot_framebuffer.width, g_boot_info.boot_framebuffer.height, g_boot_info.boot_framebuffer.pitch));
    auto* old_boot_console = g_boot_console.exchange(&boot_console.leak_ref());
    if (old_boot_console != nullptr)
        old_boot_console->unref();

    return {};
}

}
