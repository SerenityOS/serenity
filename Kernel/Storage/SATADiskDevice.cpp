/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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
#include <Kernel/Storage/AHCIController.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/SATADiskDevice.h>

namespace Kernel {

NonnullRefPtr<SATADiskDevice> SATADiskDevice::create(const AHCIController& controller, const AHCIPort& port, size_t sector_size, u64 max_addressable_block)
{
    return adopt(*new SATADiskDevice(controller, port, sector_size, max_addressable_block));
}

SATADiskDevice::SATADiskDevice(const AHCIController& controller, const AHCIPort& port, size_t sector_size, u64 max_addressable_block)
    : StorageDevice(controller, sector_size, max_addressable_block)
    , m_port(port)
{
}

SATADiskDevice::~SATADiskDevice()
{
}

const char* SATADiskDevice::class_name() const
{
    return "SATADiskDevice";
}

void SATADiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    m_port->start_request(request);
}

String SATADiskDevice::device_name() const
{
    return String::formatted("hd{:c}", 'a' + minor());
}

}
