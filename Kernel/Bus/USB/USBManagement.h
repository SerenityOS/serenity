/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/Drivers/USBDriver.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Library/NonnullLockRefPtr.h>

namespace Kernel::USB {

class USBManagement {

public:
    USBManagement();
    static bool initialized();
    static void initialize();
    static USBManagement& the();

    static void register_driver(NonnullLockRefPtr<Driver> driver);
    static LockRefPtr<Driver> get_driver_by_name(StringView name);
    static void unregister_driver(NonnullLockRefPtr<Driver> driver);

    static void add_recipe(DeviceTree::DeviceRecipe<NonnullLockRefPtr<USBController>>);

    static Vector<NonnullLockRefPtr<Driver>>& available_drivers();

private:
    void enumerate_controllers();

    USBController::List m_controllers;
};

}
