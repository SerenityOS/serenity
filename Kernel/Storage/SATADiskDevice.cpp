/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Storage/AHCIController.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/SATADiskDevice.h>

namespace Kernel {

NonnullRefPtr<SATADiskDevice> SATADiskDevice::create(const AHCIController& controller, const AHCIPort& port, size_t sector_size, u64 max_addressable_block)
{
    auto device = adopt_ref(*new SATADiskDevice(controller, port, sector_size, max_addressable_block));
    device->after_inserting();
    return device;
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
