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

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Storage/IDEController.h>
#include <Kernel/Storage/PATADiskDevice.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<IDEController> IDEController::initialize(PCI::Address address, bool force_pio)
{
    return adopt(*new IDEController(address, force_pio));
}

bool IDEController::reset()
{
    TODO();
}

bool IDEController::shutdown()
{
    TODO();
}

size_t IDEController::devices_count() const
{
    size_t count = 0;
    for (u32 index = 0; index < 4; index++) {
        if (!device(index).is_null())
            count++;
    }
    return count;
}

void IDEController::start_request(const StorageDevice&, AsyncBlockDeviceRequest&)
{
    VERIFY_NOT_REACHED();
}

void IDEController::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT IDEController::IDEController(PCI::Address address, bool force_pio)
    : StorageController()
    , PCI::DeviceController(address)
{
    initialize(force_pio);
}

UNMAP_AFTER_INIT IDEController::~IDEController()
{
}

UNMAP_AFTER_INIT void IDEController::initialize(bool force_pio)
{
    auto bus_master_base = IOAddress(PCI::get_BAR4(pci_address()) & (~1));

    auto bar0 = PCI::get_BAR0(pci_address());
    auto base_io = (bar0 == 0x1 || bar0 == 0) ? IOAddress(0x1F0) : IOAddress(bar0);
    auto bar1 = PCI::get_BAR1(pci_address());
    auto control_io = (bar1 == 0x1 || bar1 == 0) ? IOAddress(0x3F6) : IOAddress(bar1);

    m_channels.append(IDEChannel::create(*this, { base_io, control_io, bus_master_base }, IDEChannel::ChannelType::Primary, force_pio));

    auto bar2 = PCI::get_BAR2(pci_address());
    base_io = (bar2 == 0x1 || bar2 == 0) ? IOAddress(0x170) : IOAddress(bar2);
    auto bar3 = PCI::get_BAR3(pci_address());
    control_io = (bar3 == 0x1 || bar3 == 0) ? IOAddress(0x376) : IOAddress(bar3);
    m_channels.append(IDEChannel::create(*this, { base_io, control_io, bus_master_base.offset(8) }, IDEChannel::ChannelType::Secondary, force_pio));
}

RefPtr<StorageDevice> IDEController::device_by_channel_and_position(u32 index) const
{
    switch (index) {
    case 0:
        return m_channels[0].master_device();
    case 1:
        return m_channels[0].slave_device();
    case 2:
        return m_channels[1].master_device();
    case 3:
        return m_channels[1].slave_device();
    }
    VERIFY_NOT_REACHED();
}

RefPtr<StorageDevice> IDEController::device(u32 index) const
{
    NonnullRefPtrVector<StorageDevice> connected_devices;
    for (size_t index = 0; index < 4; index++) {
        auto checked_device = device_by_channel_and_position(index);
        if (checked_device.is_null())
            continue;
        connected_devices.append(checked_device.release_nonnull());
    }
    if (index >= connected_devices.size())
        return nullptr;
    return connected_devices[index];
}

}
