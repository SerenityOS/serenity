/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/IDEController.h>
#include <Kernel/Storage/PATADiskDevice.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<PATADiskDevice> PATADiskDevice::create(const IDEController& controller, IDEChannel& channel, DriveType type, InterfaceType interface_type, u16 capabilities, u64 max_addressable_block)
{
    auto minor_device_number = StorageManagement::minor_number();

    // FIXME: We need a way of formatting strings with KString.
    auto device_name = String::formatted("hd{:c}", 'a' + minor_device_number);
    auto device_name_kstring = KString::must_create(device_name.view());

    auto device_or_error = DeviceManagement::try_create_device<PATADiskDevice>(controller, channel, type, interface_type, capabilities, max_addressable_block, minor_device_number, move(device_name_kstring));
    // FIXME: Find a way to propagate errors
    VERIFY(!device_or_error.is_error());
    return device_or_error.release_value();
}

UNMAP_AFTER_INIT PATADiskDevice::PATADiskDevice(const IDEController& controller, IDEChannel& channel, DriveType type, InterfaceType interface_type, u16 capabilities, u64 max_addressable_block, int minor_device_number, NonnullOwnPtr<KString> device_name)
    : StorageDevice(controller, StorageManagement::major_number(), minor_device_number, 512, max_addressable_block, move(device_name))
    , m_capabilities(capabilities)
    , m_channel(channel)
    , m_drive_type(type)
    , m_interface_type(interface_type)
{
}

UNMAP_AFTER_INIT PATADiskDevice::~PATADiskDevice()
{
}

StringView PATADiskDevice::class_name() const
{
    return "PATADiskDevice"sv;
}

void PATADiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    m_channel->start_request(request, is_slave(), m_capabilities);
}

bool PATADiskDevice::is_slave() const
{
    return m_drive_type == DriveType::Slave;
}

}
