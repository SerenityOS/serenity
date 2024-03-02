/*
 * Copyright (c) 2020-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Error.h>
#include <AK/HashTable.h>
#include <AK/Singleton.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/PCI/Controller/PIIX4HostBridge.h>
#endif
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel::PCI {

static Access* s_access;

void Access::initialize()
{
    VERIFY(!is_initialized());
    new Access;
}

Access& Access::the()
{
    if (s_access == nullptr) {
        VERIFY_NOT_REACHED(); // We failed to initialize the PCI subsystem, so stop here!
    }
    return *s_access;
}

bool Access::is_initialized()
{
    return (s_access != nullptr);
}

bool Access::is_hardware_disabled()
{
    return g_pci_access_io_probe_failed;
}

bool Access::is_disabled()
{
    return g_pci_access_is_disabled_from_commandline || g_pci_access_io_probe_failed;
}

ErrorOr<void> Access::add_host_controller_and_scan_for_devices(NonnullRefPtr<HostController> controller)
{
    TRY(m_host_controllers.with([controller](auto& controllers) -> ErrorOr<void> {
        auto domain_number = controller->domain_number();

        VERIFY(!controllers.contains(domain_number));
        TRY(controllers.try_set(domain_number, move(controller)));
        TRY(controller->enumerate_all_devices({}));
        return {};
    }));
    return {};
}

UNMAP_AFTER_INIT Access::Access()
{
    s_access = this;
}

struct ClassedDriverDeviceLists {
    IntrusiveList<&Driver::m_classed_list_node> driver_list;
    IntrusiveList<&Device::m_classed_list_node> device_list;
};

struct ClassedLists {
    SpinlockProtected<ClassedDriverDeviceLists, LockRank::None> lists[256];
};

static Singleton<ClassedLists> s_classed_lists;

static void attach_device_to_driver(RawPtr<PCI::Driver>& device_driver_ptr, PCI::Driver& driver)
{
    device_driver_ptr = &driver;
}

static bool is_matching_device_based_on_hardware_id(PCI::Device const& device, HardwareIDMatch const& hardware_id_match)
{
    if (device.device_id().hardware_id() == hardware_id_match.hardware_id)
        return true;
    if (device.device_id().subclass_code() != hardware_id_match.subclass_code)
        return false;
    if (hardware_id_match.hardware_id.is_vendor_id_wildcard())
        return true;
    if (device.device_id().hardware_id().vendor_id == hardware_id_match.hardware_id.vendor_id && hardware_id_match.hardware_id.is_device_id_wildcard())
        return true;
    return false;
}

static bool is_matching_driver_based_on_hardware_id_match(PCI::Device const& device, PCI::HardwareIDMatch const* already_chosen_match, HardwareIDMatch const& hardware_id_match)
{
    if (!is_matching_device_based_on_hardware_id(device, hardware_id_match))
        return false;
    if (!already_chosen_match)
        return true;
    // FIXME: Don't choose the "winning driver" based on this order,
    // but account for all HardwareIDMatch members at once.
    if (hardware_id_match.revision_id.has_value()) {
        if (!already_chosen_match->revision_id.has_value() && hardware_id_match.revision_id.value() == device.device_id().revision_id())
            return true;
    }
    if (hardware_id_match.subsystem_id_match.has_value()) {
        if (!already_chosen_match->subsystem_id_match.has_value()) {
            auto& subsystem_id_match = hardware_id_match.subsystem_id_match.value();
            if (subsystem_id_match.subsystem_id == device.device_id().subsystem_id()
                && subsystem_id_match.subsystem_vendor_id == device.device_id().subsystem_vendor_id())
                return true;
        }
    }
    if (hardware_id_match.programming_interface.has_value()) {
        if (!already_chosen_match->programming_interface.has_value()) {
            auto& programming_interface = hardware_id_match.programming_interface.value();
            if (programming_interface == device.device_id().prog_if())
                return true;
        }
    }
    return false;
}

void Access::register_device(NonnullRefPtr<Device> device)
{
    auto result = g_pci_bus_work->try_queue([device]() {
        s_classed_lists->lists[static_cast<u8>(device->device_id().class_code())].with([device](auto& classed_lists) {
            classed_lists.device_list.append(*device);
            RawPtr<PCI::Driver> chosen_driver = nullptr;
            RawPtr<PCI::HardwareIDMatch const> chosen_match = nullptr;
            for (auto& driver : classed_lists.driver_list) {
                for (auto& match : driver.matches()) {
                    if (!is_matching_driver_based_on_hardware_id_match(device, chosen_match, match))
                        continue;
                    chosen_driver = &driver;
                    chosen_match = &match;
                }
            }
            if (!chosen_driver)
                return;
            device->driver({}).with([&](auto& attached_device_driver) {
                // NOTE: If the device is attached to a driver, don't
                // re-attach it to another driver.
                if (attached_device_driver)
                    return;
                auto result = chosen_driver->probe(device);
                if (!result.is_error())
                    attach_device_to_driver(attached_device_driver, *chosen_driver);
                else
                    dbgln("PCI: Failed to attach device {} on driver {} due to {}", device->device_id().address(), chosen_driver->name(), result.release_error());
                return;
            });
        });
    });
    if (result.is_error())
        dbgln("PCI: Failed to queue registering device {} due to {}", device->device_id().address(), result.release_error());
}

void Access::register_driver(NonnullRefPtr<Driver> driver)
{
    m_all_drivers.with([driver](auto& drivers_list) {
        drivers_list.append(*driver);
    });
    dbgln_if(PCI_DEBUG, "PCI: Registering driver {}", driver->name());
    auto result = g_pci_bus_work->try_queue([driver]() {
        s_classed_lists->lists[to_underlying(driver->class_id())].with([driver](auto& classed_lists) {
            classed_lists.driver_list.append(*driver);
            for (auto& device : classed_lists.device_list) {
                device.driver({}).with([&](auto& attached_device_driver) {
                    // NOTE: If the device is attached to a driver, don't
                    // re-attach it to another driver.
                    if (attached_device_driver)
                        return;
                    for (auto& match : driver->matches()) {
                        if (!is_matching_device_based_on_hardware_id(device, match))
                            continue;
                        auto result = driver->probe(device);
                        if (!result.is_error())
                            attach_device_to_driver(attached_device_driver, *driver);
                        else
                            dbgln("PCI: Failed to attach device {} on driver {} due to {}", device.device_id().address(), driver->name(), result.release_error());
                        return;
                    }
                });
            }
        });
    });
    if (result.is_error())
        dbgln("PCI: Failed to queue registering driver {} due to {}", driver->name(), result.release_error());
}

RefPtr<Driver> Access::get_driver_by_name(StringView)
{
    TODO();
}

void Access::unregister_driver(NonnullRefPtr<Driver>)
{
    TODO();
}

}
