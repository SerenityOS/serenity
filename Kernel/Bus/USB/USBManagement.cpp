/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/BusDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel::USB {

static Singleton<USBManagement> s_the;
READONLY_AFTER_INIT bool s_initialized_sys_fs_directory = false;

UNMAP_AFTER_INIT USBManagement::USBManagement()
{
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
    if (!initialized())
        return;
    dbgln_if(USB_DEBUG, "Registering driver {}", driver->name());
    the().m_available_drivers.append(driver);
}

LockRefPtr<Driver> USBManagement::get_driver_by_name(StringView name)
{
    if (!initialized())
        return nullptr;
    auto it = the().m_available_drivers.find_if([name](auto driver) { return driver->name() == name; });
    return it.is_end() ? nullptr : LockRefPtr { *it };
}

void USBManagement::unregister_driver(NonnullLockRefPtr<Driver> driver)
{
    if (!initialized())
        return;
    auto& the_instance = the();
    dbgln_if(USB_DEBUG, "Unregistering driver {}", driver->name());
    auto const& found_driver = the_instance.m_available_drivers.find(driver);
    if (!found_driver.is_end())
        the_instance.m_available_drivers.remove(found_driver.index());
}

USBManagement& USBManagement::the()
{
    return *s_the;
}

}
