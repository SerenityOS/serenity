/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/DeviceComponent.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class SysFSComponentRegistry {
    using DevicesList = MutexProtected<IntrusiveList<&SysFSDeviceComponent::m_list_node>>;

public:
    static SysFSComponentRegistry& the();

    static void initialize();

    SysFSComponentRegistry();
    void register_new_component(SysFSComponent&);

    SysFSDirectory& root_directory() { return m_root_directory; }
    Mutex& get_lock() { return m_lock; }

    void register_new_bus_directory(SysFSDirectory&);
    SysFSBusDirectory& buses_directory();

    DevicesList& devices_list();

private:
    Mutex m_lock;
    NonnullRefPtr<SysFSRootDirectory> m_root_directory;
    DevicesList m_devices_list;
};

}
