/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/DeviceComponent.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullRefPtr<SysFSDeviceComponent> SysFSDeviceComponent::must_create(Device const& device)
{
    // FIXME: Handle allocation failure gracefully
    auto device_name = MUST(KString::formatted("{}:{}", device.major(), device.minor()));
    return adopt_ref_if_nonnull(new SysFSDeviceComponent(move(device_name), device)).release_nonnull();
}
SysFSDeviceComponent::SysFSDeviceComponent(NonnullOwnPtr<KString> major_minor_formatted_device_name, Device const& device)
    : SysFSComponent()
    , m_block_device(device.is_block_device())
    , m_major_minor_formatted_device_name(move(major_minor_formatted_device_name))
{
    VERIFY(device.is_block_device() || device.is_character_device());
}

}
