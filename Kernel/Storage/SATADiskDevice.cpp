/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Storage/AHCIController.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/SATADiskDevice.h>

namespace Kernel {

NonnullRefPtr<SATADiskDevice> SATADiskDevice::create(const AHCIController& controller, const AHCIPort& port, size_t sector_size, u64 max_addressable_block)
{
    auto device_or_error = DeviceManagement::try_create_device<SATADiskDevice>(controller, port, sector_size, max_addressable_block);
    // FIXME: Find a way to propagate errors
    VERIFY(!device_or_error.is_error());
    return device_or_error.release_value();
}

SATADiskDevice::SATADiskDevice(const AHCIController& controller, const AHCIPort& port, size_t sector_size, u64 max_addressable_block)
    : StorageDevice(controller, sector_size, max_addressable_block)
    , m_port(port)
{
}

SATADiskDevice::~SATADiskDevice()
{
}

StringView SATADiskDevice::class_name() const
{
    return "SATADiskDevice";
}

void SATADiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    m_port.strong_ref()->start_request(request);
}

String SATADiskDevice::storage_name() const
{
    return String::formatted("hd{:c}", 'a' + minor());
}

}
