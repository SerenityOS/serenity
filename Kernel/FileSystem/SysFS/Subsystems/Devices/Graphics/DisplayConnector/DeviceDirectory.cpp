/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/DeviceAttribute.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/DeviceDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

DisplayConnector const& DisplayConnectorSysFSDirectory::device(Badge<DisplayConnectorAttributeSysFSComponent>) const
{
    return *m_device;
}

UNMAP_AFTER_INIT NonnullRefPtr<DisplayConnectorSysFSDirectory> DisplayConnectorSysFSDirectory::create(SysFSDirectory const& parent_directory, DisplayConnector const& device)
{
    // FIXME: Handle allocation failure gracefully
    auto device_name = MUST(KString::formatted("{}", device.minor()));
    auto directory = adopt_ref(*new (nothrow) DisplayConnectorSysFSDirectory(move(device_name), parent_directory, device));
    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(DisplayConnectorAttributeSysFSComponent::must_create(*directory, DisplayConnectorAttributeSysFSComponent::Type::MutableModeSettingCapable));
        list.append(DisplayConnectorAttributeSysFSComponent::must_create(*directory, DisplayConnectorAttributeSysFSComponent::Type::DoubleFrameBufferingCapable));
        list.append(DisplayConnectorAttributeSysFSComponent::must_create(*directory, DisplayConnectorAttributeSysFSComponent::Type::FlushSupport));
        list.append(DisplayConnectorAttributeSysFSComponent::must_create(*directory, DisplayConnectorAttributeSysFSComponent::Type::PartialFlushSupport));
        list.append(DisplayConnectorAttributeSysFSComponent::must_create(*directory, DisplayConnectorAttributeSysFSComponent::Type::RefreshRateSupport));
        list.append(DisplayConnectorAttributeSysFSComponent::must_create(*directory, DisplayConnectorAttributeSysFSComponent::Type::EDID));
        return {};
    }));
    return directory;
}

UNMAP_AFTER_INIT DisplayConnectorSysFSDirectory::DisplayConnectorSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const& parent_directory, DisplayConnector const& device)
    : SysFSDirectory(parent_directory)
    , m_device(device)
    , m_device_directory_name(move(device_directory_name))
{
}

}
