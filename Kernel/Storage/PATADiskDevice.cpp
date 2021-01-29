/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/IDEController.h>
#include <Kernel/Storage/PATADiskDevice.h>

namespace Kernel {

NonnullRefPtr<PATADiskDevice> PATADiskDevice::create(const IDEController& controller, IDEChannel& channel, DriveType type, InterfaceType interface_type, u16 cylinders, u16 heads, u16 spt, u16 capabilities, int major, int minor)
{
    return adopt(*new PATADiskDevice(controller, channel, type, interface_type, cylinders, heads, spt, capabilities, major, minor));
}

PATADiskDevice::PATADiskDevice(const IDEController& controller, IDEChannel& channel, DriveType type, InterfaceType interface_type, u16 cylinders, u16 heads, u16 spt, u16 capabilities, int major, int minor)
    : StorageDevice(controller, major, minor, 512, 0)
    , m_cylinders(cylinders)
    , m_heads(heads)
    , m_sectors_per_track(spt)
    , m_capabilities(capabilities)
    , m_channel(channel)
    , m_drive_type(type)
    , m_interface_type(interface_type)
{
}

PATADiskDevice::~PATADiskDevice()
{
}

const char* PATADiskDevice::class_name() const
{
    return "PATADiskDevice";
}

void PATADiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    bool use_dma = !m_channel.m_io_group.bus_master_base().is_null() && m_channel.m_dma_enabled.resource();
    m_channel.start_request(request, use_dma, is_slave(), m_capabilities);
}

String PATADiskDevice::device_name() const
{
    return String::formatted("hd{:c}", 'a' + minor());
}

size_t PATADiskDevice::max_addressable_block() const
{
    return m_cylinders * m_heads * m_sectors_per_track;
}

bool PATADiskDevice::is_slave() const
{
    return m_drive_type == DriveType::Slave;
}

}
