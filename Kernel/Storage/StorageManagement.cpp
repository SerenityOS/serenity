/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/Storage/IDEController.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

static StorageManagement* s_the;

StorageManagement::StorageManagement(String root_device, bool force_pio)
    : m_controllers(enumerate_controllers(force_pio))
    , m_boot_device(determine_boot_device(root_device))
{
}

NonnullRefPtr<StorageDevice> StorageManagement::determine_boot_device(String root_device) const
{
    ASSERT(!m_controllers.is_empty());
    if (!root_device.starts_with("/dev/hd")) {
        klog() << "init_stage2: root filesystem must be on an hard drive";
        Processor::halt();
    }
    auto drive_letter = root_device.substring(strlen("/dev/hd"), root_device.length() - strlen("/dev/hd"))[0];

    if (drive_letter < 'a' || drive_letter > 'z') {
        klog() << "init_stage2: root filesystem must be on an hard drive name";
        Processor::halt();
    }

    size_t drive_index = (u8)drive_letter - (u8)'a';
    auto devices = storage_devices();
    if (drive_index >= devices.size()) {
        klog() << "init_stage2: invalid selection of hard drive.";
        Processor::halt();
    }
    return devices[drive_index];
}

NonnullRefPtrVector<StorageController> StorageManagement::enumerate_controllers(bool force_pio) const
{
    NonnullRefPtrVector<StorageController> controllers;
    PCI::enumerate([&](const PCI::Address& address, PCI::ID) {
        if (PCI::get_class(address) == 0x1 && PCI::get_subclass(address) == 0x1) {
            controllers.append(IDEController::initialize(address, force_pio));
        }
    });
    return controllers;
}

NonnullRefPtrVector<StorageDevice> StorageManagement::storage_devices() const
{
    NonnullRefPtrVector<StorageDevice> devices;
    for (auto& controller : m_controllers) {
        for (size_t index = 0; index < controller.devices_count(); index++) {
            auto device = controller.device(index);
            if (device.is_null())
                continue;
            devices.append(device.release_nonnull());
        }
    }
    return devices;
}

bool StorageManagement::initialized()
{
    return (s_the != nullptr);
}

void StorageManagement::initialize(String root_device, bool force_pio)
{
    ASSERT(!StorageManagement::initialized());
    s_the = new StorageManagement(root_device, force_pio);
}

StorageManagement& StorageManagement::the()
{
    return *s_the;
}

NonnullRefPtr<StorageDevice> StorageManagement::boot_device() const
{
    return m_boot_device;
}
NonnullRefPtrVector<StorageController> StorageManagement::ide_controllers() const
{
    NonnullRefPtrVector<StorageController> ide_controllers;
    for (auto& controller : m_controllers) {
        if (controller.type() == StorageController::Type::IDE)
            ide_controllers.append(controller);
    }
    return ide_controllers;
}

}
