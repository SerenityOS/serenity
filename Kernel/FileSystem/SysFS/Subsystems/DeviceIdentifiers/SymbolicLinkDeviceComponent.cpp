/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/SymbolicLinkDeviceComponent.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<SysFSSymbolicLinkDeviceComponent>> SysFSSymbolicLinkDeviceComponent::try_create(SysFSCharacterDevicesDirectory const& parent_directory, Device const& device, SysFSComponent const& pointed_component)
{
    auto device_name = TRY(KString::formatted("{}:{}", device.major(), device.minor()));
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSSymbolicLinkDeviceComponent(parent_directory, move(device_name), device, pointed_component));
}

ErrorOr<NonnullRefPtr<SysFSSymbolicLinkDeviceComponent>> SysFSSymbolicLinkDeviceComponent::try_create(SysFSBlockDevicesDirectory const& parent_directory, Device const& device, SysFSComponent const& pointed_component)
{
    auto device_name = TRY(KString::formatted("{}:{}", device.major(), device.minor()));
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSSymbolicLinkDeviceComponent(parent_directory, move(device_name), device, pointed_component));
}

SysFSSymbolicLinkDeviceComponent::SysFSSymbolicLinkDeviceComponent(SysFSCharacterDevicesDirectory const& parent_directory, NonnullOwnPtr<KString> major_minor_formatted_device_name, Device const& device, SysFSComponent const& pointed_component)
    : SysFSSymbolicLink(parent_directory, pointed_component)
    , m_block_device(device.is_block_device())
    , m_major_minor_formatted_device_name(move(major_minor_formatted_device_name))
{
    VERIFY(device.is_character_device());
}

SysFSSymbolicLinkDeviceComponent::SysFSSymbolicLinkDeviceComponent(SysFSBlockDevicesDirectory const& parent_directory, NonnullOwnPtr<KString> major_minor_formatted_device_name, Device const& device, SysFSComponent const& pointed_component)
    : SysFSSymbolicLink(parent_directory, pointed_component)
    , m_block_device(device.is_block_device())
    , m_major_minor_formatted_device_name(move(major_minor_formatted_device_name))
{
    VERIFY(device.is_block_device());
}

}
