/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/USB/UHCI/UHCIController.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/BusDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel::USB {

static Singleton<USBManagement> s_the;
READONLY_AFTER_INIT bool s_initialized_sys_fs_directory = false;

UNMAP_AFTER_INIT USBManagement::USBManagement()
{
    enumerate_controllers();
}

UNMAP_AFTER_INIT void USBManagement::enumerate_controllers()
{
    if (kernel_command_line().disable_usb())
        return;

    MUST(PCI::enumerate([this](PCI::DeviceIdentifier const& device_identifier) {
        if (!(device_identifier.class_code().value() == 0xc && device_identifier.subclass_code().value() == 0x3))
            return;
        if (device_identifier.prog_if().value() == 0x0) {
            if (kernel_command_line().disable_uhci_controller())
                return;

            if (auto uhci_controller_or_error = UHCIController::try_to_initialize(device_identifier); !uhci_controller_or_error.is_error())
                m_controllers.append(uhci_controller_or_error.release_value());

            return;
        }

        if (device_identifier.prog_if().value() == 0x10) {
            dmesgln("USBManagement: OHCI controller found at {} is not currently supported.", device_identifier.address());
            return;
        }

        if (device_identifier.prog_if().value() == 0x20) {
            dmesgln("USBManagement: EHCI controller found at {} is not currently supported.", device_identifier.address());
            return;
        }

        if (device_identifier.prog_if().value() == 0x30) {
            dmesgln("USBManagement: xHCI controller found at {} is not currently supported.", device_identifier.address());
            return;
        }

        dmesgln("USBManagement: Unknown/unsupported controller at {} with programming interface 0x{:02x}", device_identifier.address(), device_identifier.prog_if().value());
    }));
}

bool USBManagement::initialized()
{
    return s_the.is_initialized();
}

UNMAP_AFTER_INIT void USBManagement::initialize()
{
    if (!s_initialized_sys_fs_directory) {
        SysFSUSBBusDirectory::initialize();
        s_initialized_sys_fs_directory = true;
    }

    s_the.ensure_instance();
}

void USBManagement::register_driver(NonnullLockRefPtr<Driver> driver)
{
    dbgln_if(USB_DEBUG, "Registering driver {}", driver->name());
    m_available_drivers.append(driver);
}

LockRefPtr<Driver> USBManagement::get_driver_by_name(StringView name)
{
    auto it = m_available_drivers.find_if([name](auto driver) { return driver->name() == name; });
    return it.is_end() ? nullptr : LockRefPtr { *it };
}

void USBManagement::unregister_driver(NonnullLockRefPtr<Driver> driver)
{
    dbgln_if(USB_DEBUG, "Unregistering driver {}", driver->name());
    auto const& found_driver = m_available_drivers.find(driver);
    if (!found_driver.is_end())
        m_available_drivers.remove(found_driver.index());
}

USBManagement& USBManagement::the()
{
    return *s_the;
}

}
