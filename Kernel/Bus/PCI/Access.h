/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Try.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

class Access {
public:
    static Access& the();
    static void initialize();
    static bool is_initialized();
    static bool is_disabled();
    static bool is_hardware_disabled();

    ErrorOr<void> add_host_controller_and_scan_for_devices(NonnullRefPtr<HostController>);

    void register_driver(NonnullRefPtr<Driver> driver);
    void register_device(NonnullRefPtr<Device> device);
    RefPtr<Driver> get_driver_by_name(StringView name);
    void unregister_driver(NonnullRefPtr<Driver> driver);

private:
    Access();

    SpinlockProtected<HashMap<u32, NonnullRefPtr<PCI::HostController>>, LockRank::None> m_host_controllers;
    SpinlockProtected<IntrusiveList<&Driver::m_list_node>, LockRank::None> m_all_drivers;
};
}
