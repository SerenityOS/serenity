/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NeverDestroyed.h>
#include <AK/Singleton.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/BusDirectory.h>

namespace Kernel::USB {

static NeverDestroyed<Vector<NonnullLockRefPtr<Driver>>> s_available_drivers;
static Singleton<USBManagement> s_the;

UNMAP_AFTER_INIT USBManagement::USBManagement()
{
    SysFSUSBBusDirectory::initialize();
}

void USBManagement::register_driver(NonnullLockRefPtr<Driver> driver)
{
    dbgln_if(USB_DEBUG, "Registering driver {}", driver->name());
    s_available_drivers->append(driver);
}

LockRefPtr<Driver> USBManagement::get_driver_by_name(StringView name)
{
    auto it = s_available_drivers->find_if([name](auto driver) { return driver->name() == name; });
    return it.is_end() ? nullptr : LockRefPtr { *it };
}

void USBManagement::unregister_driver(NonnullLockRefPtr<Driver> driver)
{
    dbgln_if(USB_DEBUG, "Unregistering driver {}", driver->name());
    auto const& found_driver = s_available_drivers->find(driver);
    if (!found_driver.is_end())
        s_available_drivers->remove(found_driver.index());
}

USBManagement& USBManagement::the()
{
    return *s_the;
}

void USBManagement::add_controller(NonnullLockRefPtr<USBController> controller)
{
    m_controllers.append(controller);
}

Vector<NonnullLockRefPtr<Driver>>& USBManagement::available_drivers()
{
    return *s_available_drivers;
}

}
