/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Graphics/Bochs/GraphicsAdapter.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/Intel/NativeGraphicsAdapter.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<GraphicsManagement> s_the;

GraphicsManagement& GraphicsManagement::the()
{
    return *s_the;
}

bool GraphicsManagement::is_initialized()
{
    return s_the.is_initialized();
}

UNMAP_AFTER_INIT GraphicsManagement::GraphicsManagement()
{
}

bool GraphicsManagement::framebuffer_devices_allowed() const
{
    return kernel_command_line().are_framebuffer_devices_enabled();
}

void GraphicsManagement::deactivate_graphical_mode()
{
    for (auto& graphics_device : m_graphics_devices) {
        graphics_device.enable_consoles();
    }
}
void GraphicsManagement::activate_graphical_mode()
{
    for (auto& graphics_device : m_graphics_devices) {
        graphics_device.disable_consoles();
    }
}

static inline bool is_vga_compatible_pci_device(PCI::DeviceIdentifier const& device_identifier)
{
    // Note: Check for Display Controller, VGA Compatible Controller or
    // Unclassified, VGA-Compatible Unclassified Device
    auto is_display_controller_vga_compatible = device_identifier.class_code().value() == 0x3 && device_identifier.subclass_code().value() == 0x0;
    auto is_general_pci_vga_compatible = device_identifier.class_code().value() == 0x0 && device_identifier.subclass_code().value() == 0x1;
    return is_display_controller_vga_compatible || is_general_pci_vga_compatible;
}

static inline bool is_display_controller_pci_device(PCI::DeviceIdentifier const& device_identifier)
{
    return device_identifier.class_code().value() == 0x3;
}

UNMAP_AFTER_INIT bool GraphicsManagement::determine_and_initialize_graphics_device(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(is_vga_compatible_pci_device(device_identifier) || is_display_controller_pci_device(device_identifier));
    auto add_and_configure_adapter = [&](GenericGraphicsAdapter& graphics_device) {
        m_graphics_devices.append(graphics_device);
        if (!framebuffer_devices_allowed()) {
            graphics_device.enable_consoles();
            return;
        }
        graphics_device.initialize_framebuffer_devices();
    };

    RefPtr<GenericGraphicsAdapter> adapter;
    switch (device_identifier.hardware_id().vendor_id) {
    case PCI::VendorID::QEMUOld:
        if (device_identifier.hardware_id().device_id == 0x1111)
            adapter = BochsGraphicsAdapter::initialize(device_identifier);
        break;
    case PCI::VendorID::VirtualBox:
        if (device_identifier.hardware_id().device_id == 0xbeef)
            adapter = BochsGraphicsAdapter::initialize(device_identifier);
        break;
    case PCI::VendorID::Intel:
        adapter = IntelNativeGraphicsAdapter::initialize(device_identifier);
        break;
    case PCI::VendorID::VirtIO:
        dmesgln("Graphics: Using VirtIO console");
        adapter = Graphics::VirtIOGPU::GraphicsAdapter::initialize(device_identifier);
        break;
    default:
        if (!is_vga_compatible_pci_device(device_identifier))
            break;
        // Note: Although technically possible that a system has a
        // non-compatible VGA graphics device that was initialized by the
        // Multiboot bootloader to provide a framebuffer, in practice we
        // probably want to support these devices natively instead of
        // initializing them as some sort of a generic GenericGraphicsAdapter. For now,
        // the only known example of this sort of device is qxl in QEMU. For VGA
        // compatible devices we don't have a special driver for (e.g. ati-vga,
        // qxl-vga, cirrus-vga, vmware-svga in QEMU), it's much more likely that
        // these devices will be supported by the Multiboot loader that will
        // utilize VESA BIOS extensions (that we don't currently) of these cards
        // support, so we want to utilize the provided framebuffer of these
        // devices, if possible.
        if (!m_vga_adapter && PCI::is_io_space_enabled(device_identifier.address())) {
            if (multiboot_framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_RGB) {
                dmesgln("Graphics: Using a preset resolution from the bootloader");
                adapter = VGACompatibleAdapter::initialize_with_preset_resolution(device_identifier,
                    multiboot_framebuffer_addr,
                    multiboot_framebuffer_width,
                    multiboot_framebuffer_height,
                    multiboot_framebuffer_pitch);
            }
        } else {
            dmesgln("Graphics: Using a VGA compatible generic adapter");
            adapter = VGACompatibleAdapter::initialize(device_identifier);
        }
        break;
    }
    if (!adapter)
        return false;
    add_and_configure_adapter(*adapter);

    // Note: If IO space is enabled, this VGA adapter is operating in VGA mode.
    // Note: If no other VGA adapter is attached as m_vga_adapter, we should attach it then.
    if (!m_vga_adapter && PCI::is_io_space_enabled(device_identifier.address()) && adapter->vga_compatible()) {
        dbgln("Graphics adapter @ {} is operating in VGA mode", device_identifier.address());
        m_vga_adapter = static_ptr_cast<VGACompatibleAdapter>(adapter);
    }
    return true;
}

UNMAP_AFTER_INIT bool GraphicsManagement::initialize()
{

    /* Explanation on the flow when not requesting to force not creating any 
     * framebuffer devices:
     * If the user wants to use a Console instead of the graphical environment,
     * they doesn't need to request text mode.
     * Graphical mode might not be accessible on bare-metal hardware because
     * the bootloader didn't set a framebuffer and we don't have a native driver
     * to set a framebuffer for it. We don't have VBE modesetting capabilities
     * in the kernel yet, so what will happen is one of the following situations:
     * 1. The bootloader didn't specify settings of a pre-set framebuffer. The
     * kernel has a native driver for a detected display adapter, therefore
     * the kernel can still set a framebuffer.
     * 2. The bootloader specified settings of a pre-set framebuffer, and the 
     * kernel has a native driver for a detected display adapter, therefore
     * the kernel can still set a framebuffer and change the settings of it.
     * In that situation, the kernel will simply ignore the Multiboot pre-set 
     * framebuffer.
     * 2. The bootloader specified settings of a pre-set framebuffer, and the 
     * kernel does not have a native driver for a detected display adapter, 
     * therefore the kernel will use the pre-set framebuffer. Modesetting is not
     * available in this situation.
     * 3. The bootloader didn't specify settings of a pre-set framebuffer, and 
     * the kernel does not have a native driver for a detected display adapter, 
     * therefore the kernel will try to initialize a VGA text mode console.
     * In that situation, the kernel will assume that VGA text mode was already
     * initialized, but will still try to modeset it. No switching to graphical 
     * environment is allowed in this case.
     * 
     * By default, the kernel assumes that no framebuffer was created until it
     * was proven that there's an existing framebuffer or we can modeset the 
     * screen resolution to create a framebuffer.
     * 
     * If the user requests to force no initialization of framebuffer devices
     * the same flow above will happen, except that no framebuffer device will
     * be created, so SystemServer will not try to initialize WindowServer.
     */

    if (!framebuffer_devices_allowed())
        dbgln("Forcing non-initialization of framebuffer devices");

    PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        // Note: Each graphics controller will try to set its native screen resolution
        // upon creation. Later on, if we don't want to have framebuffer devices, a
        // framebuffer console will take the control instead.
        if (!is_vga_compatible_pci_device(device_identifier) && !is_display_controller_pci_device(device_identifier))
            return;
        determine_and_initialize_graphics_device(device_identifier);
    });

    if (m_graphics_devices.is_empty()) {
        dbgln("No graphics adapter was initialized.");
        return false;
    }
    return true;
}

bool GraphicsManagement::framebuffer_devices_exist() const
{
    for (auto& graphics_device : m_graphics_devices) {
        if (graphics_device.framebuffer_devices_initialized())
            return true;
    }
    return false;
}
}
