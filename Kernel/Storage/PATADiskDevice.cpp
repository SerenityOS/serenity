/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/IDEController.h>
#include <Kernel/Storage/PATADiskDevice.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<PATADiskDevice> PATADiskDevice::create(const IDEController& controller, IDEChannel& channel, DriveType type, InterfaceType interface_type, u16 capabilities, u64 max_addressable_block)
{
    return adopt_ref(*new PATADiskDevice(controller, channel, type, interface_type, capabilities, max_addressable_block));
}

UNMAP_AFTER_INIT PATADiskDevice::PATADiskDevice(const IDEController& controller, IDEChannel& channel, DriveType type, InterfaceType interface_type, u16 capabilities, u64 max_addressable_block)
    : StorageDevice(controller, 512, max_addressable_block)
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
    return "PATADiskDevice";
}

void PATADiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    m_channel->start_request(request, is_slave(), m_capabilities);
}

String PATADiskDevice::device_name() const
{
    return String::formatted("hd{:c}", 'a' + minor());
}

bool PATADiskDevice::is_slave() const
{
    return m_drive_type == DriveType::Slave;
}

}
