/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/SymbolicLinkLinkedGraphicsDeviceComponent.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<GraphicsAdapterSysFSDirectory> GraphicsAdapterSysFSDirectory::create(SysFSDirectory const& parent_directory, PCI::Address const& linked_device_address, u32 adapter_index)
{
    // FIXME: Handle allocation failure gracefully
    auto device_name = MUST(KString::formatted("{}", adapter_index));
    auto directory = adopt_ref(*new (nothrow) GraphicsAdapterSysFSDirectory(move(device_name), parent_directory));

    RefPtr<PCIDeviceSysFSDirectory> sysfs_pci_device_directory = PCI::get_sysfs_pci_device_directory(linked_device_address);
    VERIFY(!sysfs_pci_device_directory.is_null());

    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSSymbolicLinkLinkedGraphicsDeviceComponent::try_create(*directory, *sysfs_pci_device_directory).release_value_but_fixme_should_propagate_errors());
        return {};
    }));
    return directory;
}

UNMAP_AFTER_INIT GraphicsAdapterSysFSDirectory::GraphicsAdapterSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
    , m_device_directory_name(move(device_directory_name))
{
}

}
