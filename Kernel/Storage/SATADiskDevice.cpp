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
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

NonnullRefPtr<SATADiskDevice> SATADiskDevice::create(const AHCIController& controller, const AHCIPort& port, size_t sector_size, u64 max_addressable_block)
{
    auto minor_device_number = StorageManagement::minor_number();

    // FIXME: We need a way of formatting strings with KString.
    auto device_name = String::formatted("hd{:c}", 'a' + minor_device_number);
    auto device_name_kstring = KString::must_create(device_name.view());

    auto device_or_error = DeviceManagement::try_create_device<SATADiskDevice>(controller, port, sector_size, max_addressable_block, minor_device_number, move(device_name_kstring));
    // FIXME: Find a way to propagate errors
    VERIFY(!device_or_error.is_error());
    return device_or_error.release_value();
}

SATADiskDevice::SATADiskDevice(const AHCIController& controller, const AHCIPort& port, size_t sector_size, u64 max_addressable_block, int minor_number, NonnullOwnPtr<KString> device_name)
    : StorageDevice(controller, StorageManagement::major_number(), minor_number, sector_size, max_addressable_block, move(device_name))
    , m_port(port)
{
}

SATADiskDevice::~SATADiskDevice()
{
}

StringView SATADiskDevice::class_name() const
{
    return "SATADiskDevice"sv;
}

void SATADiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    m_port.strong_ref()->start_request(request);
}

}
