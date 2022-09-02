/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Graphics/Bochs/GraphicsAdapter.h>
#include <Kernel/Graphics/Console/BootFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/Intel/NativeGraphicsAdapter.h>
#include <Kernel/Graphics/VMWare/GraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<GraphicsManagement> s_the;

extern Atomic<Graphics::Console*> g_boot_console;

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

void GraphicsManagement::disable_vga_emulation_access_permanently()
{
    SpinlockLocker locker(m_main_vga_lock);
    disable_vga_text_mode_console_cursor();
    IO::out8(0x3c4, 1);
    u8 sr1 = IO::in8(0x3c5);
    IO::out8(0x3c5, sr1 | 1 << 5);
    microseconds_delay(1000);
    m_vga_access_is_disabled = true;
}

void GraphicsManagement::enable_vga_text_mode_console_cursor()
{
    SpinlockLocker locker(m_main_vga_lock);
    if (m_vga_access_is_disabled)
        return;
    IO::out8(0x3D4, 0xA);
    IO::out8(0x3D5, 0);
}

void GraphicsManagement::disable_vga_text_mode_console_cursor()
{
    SpinlockLocker locker(m_main_vga_lock);
    if (m_vga_access_is_disabled)
        return;
    IO::out8(0x3D4, 0xA);
    IO::out8(0x3D5, 0x20);
}

void GraphicsManagement::set_vga_text_mode_cursor(size_t console_width, size_t x, size_t y)
{
    SpinlockLocker locker(m_main_vga_lock);
    if (m_vga_access_is_disabled)
        return;
    enable_vga_text_mode_console_cursor();
    u16 value = y * console_width + x;
    IO::out8(0x3d4, 0x0e);
    IO::out8(0x3d5, MSB(value));
    IO::out8(0x3d4, 0x0f);
    IO::out8(0x3d5, LSB(value));
}

void GraphicsManagement::deactivate_graphical_mode()
{
    return m_display_connector_nodes.with([&](auto& display_connectors) {
        for (auto& connector : display_connectors)
            connector.set_display_mode({}, DisplayConnector::DisplayMode::Console);
    });
}
void GraphicsManagement::activate_graphical_mode()
{
    return m_display_connector_nodes.with([&](auto& display_connectors) {
        for (auto& connector : display_connectors)
            connector.set_display_mode({}, DisplayConnector::DisplayMode::Graphical);
    });
}

void GraphicsManagement::attach_new_display_connector(Badge<DisplayConnector>, DisplayConnector& connector)
{
    return m_display_connector_nodes.with([&](auto& display_connectors) {
        display_connectors.append(connector);
    });
}
void GraphicsManagement::detach_display_connector(Badge<DisplayConnector>, DisplayConnector& connector)
{
    return m_display_connector_nodes.with([&](auto& display_connectors) {
        display_connectors.remove(connector);
    });
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
    LockRefPtr<GenericGraphicsAdapter> adapter;

    if (!adapter) {
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
            adapter = VirtIOGraphicsAdapter::initialize(device_identifier);
            break;
        case PCI::VendorID::VMWare:
            adapter = VMWareGraphicsAdapter::try_initialize(device_identifier);
            break;
        default:
            break;
        }
    }

    if (!adapter)
        return false;
    m_graphics_devices.append(*adapter);
    return true;
}

UNMAP_AFTER_INIT void GraphicsManagement::initialize_preset_resolution_generic_display_connector()
{
    VERIFY(!multiboot_framebuffer_addr.is_null());
    VERIFY(multiboot_framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_RGB);
    dmesgln("Graphics: Using a preset resolution from the bootloader, without knowing the PCI device");
    m_preset_resolution_generic_display_connector = GenericDisplayConnector::must_create_with_preset_resolution(
        multiboot_framebuffer_addr,
        multiboot_framebuffer_width,
        multiboot_framebuffer_height,
        multiboot_framebuffer_pitch);
}

UNMAP_AFTER_INIT bool GraphicsManagement::initialize()
{

    /* Explanation on the flow here:
     *
     * If the user chose to disable graphics support entirely, then all we can do
     * is to set up a plain old VGA text console and exit this function.
     * Otherwise, we either try to find a device that we natively support so
     * we can initialize it, and in case we don't find any device to initialize,
     * we try to initialize a simple DisplayConnector to support a pre-initialized
     * framebuffer.
     *
     * Note: If the user disabled PCI access, the kernel behaves like it's running
     * on a pure ISA PC machine and therefore the kernel will try to initialize
     * a variant that is suitable for ISA VGA handling, and not PCI adapters.
     */

    auto graphics_subsystem_mode = kernel_command_line().graphics_subsystem_mode();
    if (graphics_subsystem_mode == CommandLine::GraphicsSubsystemMode::Disabled) {
        VERIFY(!m_console);
        // If no graphics driver was instantiated and we had a bootloader provided
        // framebuffer console we can simply re-use it.
        if (auto* boot_console = g_boot_console.load()) {
            m_console = *boot_console;
            boot_console->unref(); // Drop the leaked reference from Kernel::init()
        }
        return true;
    }

    if (graphics_subsystem_mode == CommandLine::GraphicsSubsystemMode::Limited && !multiboot_framebuffer_addr.is_null() && multiboot_framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_RGB) {
        initialize_preset_resolution_generic_display_connector();
        return true;
    }

    if (PCI::Access::is_disabled()) {
        dmesgln("Graphics: Using an assumed-to-exist ISA VGA compatible generic adapter");
        return true;
    }

    MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        // Note: Each graphics controller will try to set its native screen resolution
        // upon creation. Later on, if we don't want to have framebuffer devices, a
        // framebuffer console will take the control instead.
        if (!is_vga_compatible_pci_device(device_identifier) && !is_display_controller_pci_device(device_identifier))
            return;
        determine_and_initialize_graphics_device(device_identifier);
    }));

    // Note: If we failed to find any graphics device to be used natively, but the
    // bootloader prepared a framebuffer for us to use, then just create a DisplayConnector
    // for it so the user can still use the system in graphics mode.
    // Prekernel sets the framebuffer address to 0 if MULTIBOOT_INFO_FRAMEBUFFER_INFO
    // is not present, as there is likely never a valid framebuffer at this physical address.
    // Note: We only support RGB framebuffers. Any other format besides RGBX (and RGBA) or BGRX (and BGRA) is obsolete
    // and is not useful for us.
    if (m_graphics_devices.is_empty() && !multiboot_framebuffer_addr.is_null() && multiboot_framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_RGB) {
        initialize_preset_resolution_generic_display_connector();
        return true;
    }

    if (!m_console) {
        // If no graphics driver was instantiated and we had a bootloader provided
        // framebuffer console we can simply re-use it.
        if (auto* boot_console = g_boot_console.load()) {
            m_console = *boot_console;
            boot_console->unref(); // Drop the leaked reference from Kernel::init()
        }
    }

    if (m_graphics_devices.is_empty()) {
        dbgln("No graphics adapter was initialized.");
        return false;
    }
    return true;
}

void GraphicsManagement::set_console(Graphics::Console& console)
{
    m_console = console;

    if (auto* boot_console = g_boot_console.exchange(nullptr)) {
        // Disable the initial boot framebuffer console permanently
        boot_console->disable();
        // TODO: Even though we swapped the pointer and disabled the console
        // we technically can't safely destroy it as other CPUs might still
        // try to use it. Once we solve this problem we can drop the reference
        // that we intentionally leaked in Kernel::init().
    }
}

}
