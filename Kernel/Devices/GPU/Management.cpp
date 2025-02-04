/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/Delay.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Hypervisor/BochsDisplayConnector.h>
#endif
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Boot/Multiboot.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/GPU/3dfx/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/Bochs/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/Console/BootFramebufferConsole.h>
#include <Kernel/Devices/GPU/Intel/NativeGraphicsAdapter.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Devices/GPU/VMWare/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/VirtIO/GraphicsAdapter.h>
#include <Kernel/Memory/AnonymousVMObject.h>
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
    auto is_display_controller_vga_compatible = device_identifier.class_code() == PCI::ClassID::Display && device_identifier.subclass_code() == PCI::Display::SubclassID::VGA;
    auto is_general_pci_vga_compatible = device_identifier.class_code() == PCI::ClassID::Legacy && device_identifier.subclass_code() == PCI::Legacy::SubclassID::VGACompatible;
    return is_display_controller_vga_compatible || is_general_pci_vga_compatible;
}

static inline bool is_display_controller_pci_device(PCI::DeviceIdentifier const& device_identifier)
{
    return device_identifier.class_code() == PCI::ClassID::Display;
}

struct PCIGraphicsDriverInitializer {
    ErrorOr<bool> (*probe)(PCI::DeviceIdentifier const&) = nullptr;
    ErrorOr<NonnullLockRefPtr<GPUDevice>> (*create)(PCI::DeviceIdentifier const&) = nullptr;
};

static constexpr PCIGraphicsDriverInitializer s_initializers[] = {
    { IntelNativeGraphicsAdapter::probe, IntelNativeGraphicsAdapter::create },
    { BochsGraphicsAdapter::probe, BochsGraphicsAdapter::create },
    { VirtIOGraphicsAdapter::probe, VirtIOGraphicsAdapter::create },
    { VMWareGraphicsAdapter::probe, VMWareGraphicsAdapter::create },
    { VoodooGraphicsAdapter::probe, VoodooGraphicsAdapter::create },
};

UNMAP_AFTER_INIT ErrorOr<void> GraphicsManagement::determine_and_initialize_graphics_device(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(is_vga_compatible_pci_device(device_identifier) || is_display_controller_pci_device(device_identifier));
    for (auto& initializer : s_initializers) {
        auto initializer_probe_found_driver_match_or_error = initializer.probe(device_identifier);
        if (initializer_probe_found_driver_match_or_error.is_error()) {
            dmesgln("Graphics: Failed to probe device {}, due to {}", device_identifier.address(), initializer_probe_found_driver_match_or_error.error());
            continue;
        }
        auto initializer_probe_found_driver_match = initializer_probe_found_driver_match_or_error.release_value();
        if (initializer_probe_found_driver_match) {
            auto adapter = TRY(initializer.create(device_identifier));
            TRY(m_graphics_devices.try_append(*adapter));
            return {};
        }
    }
    return {};
}

UNMAP_AFTER_INIT void GraphicsManagement::initialize_preset_resolution_generic_display_connector()
{
    VERIFY(!g_boot_info.boot_framebuffer.paddr.is_null());
    VERIFY(g_boot_info.boot_framebuffer.type == BootFramebufferType::BGRx8888);
    dmesgln("Graphics: Using a preset resolution from the bootloader, without knowing the PCI device");
    m_preset_resolution_generic_display_connector = MUST(GenericDisplayConnector::create_with_preset_resolution(
        g_boot_info.boot_framebuffer.paddr,
        g_boot_info.boot_framebuffer.width,
        g_boot_info.boot_framebuffer.height,
        g_boot_info.boot_framebuffer.pitch));
}

UNMAP_AFTER_INIT bool GraphicsManagement::initialize()
{

    /* Explanation on the flow here:
     *
     * If the user chose to disable graphics support entirely, then all we can do
     * is exit this function and keep the dummy boot console.
     * Otherwise, we either try to find a device that we natively support so
     * we can initialize it, and in case we don't find any device to initialize,
     * we try to initialize a simple DisplayConnector to support a pre-initialized
     * framebuffer.
     */

    ScopeGuard assign_console_on_initialization_exit([this] {
        if (!m_console) {
            // If no graphics driver was instantiated and we had a bootloader provided
            // framebuffer console we can simply re-use it.
            if (auto* boot_console = g_boot_console.load()) {
                m_console = *boot_console;
                boot_console->unref(); // Drop the leaked reference from Kernel::init()
            }
        }
    });

    auto graphics_subsystem_mode = kernel_command_line().graphics_subsystem_mode();
    if (graphics_subsystem_mode == CommandLine::GraphicsSubsystemMode::Disabled) {
        VERIFY(!m_console);
        return true;
    }

    bool boot_framebuffer_usable = !g_boot_info.boot_framebuffer.paddr.is_null()
        && g_boot_info.boot_framebuffer.type == BootFramebufferType::BGRx8888;

    bool use_boot_framebuffer = graphics_subsystem_mode == CommandLine::GraphicsSubsystemMode::Limited
        && boot_framebuffer_usable;

    if (use_boot_framebuffer) {
        initialize_preset_resolution_generic_display_connector();
        return true;
    }

    if (!PCI::Access::is_disabled()) {
        MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
            // Note: Each graphics controller will try to set its native screen resolution
            // upon creation. Later on, if we don't want to have framebuffer devices, a
            // framebuffer console will take the control instead.
            if (!is_vga_compatible_pci_device(device_identifier) && !is_display_controller_pci_device(device_identifier))
                return;
            if (auto result = determine_and_initialize_graphics_device(device_identifier); result.is_error())
                dbgln("Failed to initialize device {}, due to {}", device_identifier.address(), result.error());
        }));
    }

    // Note: If we failed to find any graphics device to be used natively, but the
    // bootloader prepared a framebuffer for us to use, then just create a DisplayConnector
    // for it so the user can still use the system in graphics mode.
    // Prekernel sets the framebuffer address to 0 if no framebuffer is present,
    // as there is likely never a valid framebuffer at this physical address.
    // Note: We only support RGB framebuffers. Any other format besides RGBX (and RGBA) or BGRX (and BGRA) is obsolete
    // and is not useful for us.
    if (m_graphics_devices.is_empty() && boot_framebuffer_usable) {
        initialize_preset_resolution_generic_display_connector();
        return true;
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
