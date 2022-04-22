/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/DeviceInformation.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

class SysFSUSBBusDirectory final : public SysFSDirectory {
public:
    static void initialize();
    static SysFSUSBBusDirectory& the();

    virtual StringView name() const override { return "usb"sv; }

    void plug(USB::Device&);
    void unplug(USB::Device&);

    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

private:
    explicit SysFSUSBBusDirectory(SysFSBusDirectory&);

    RefPtr<SysFSUSBDeviceInformation> device_node_for(USB::Device& device);

    IntrusiveList<&SysFSUSBDeviceInformation::m_list_node> m_device_nodes;
    mutable Spinlock m_lock;
};

}
