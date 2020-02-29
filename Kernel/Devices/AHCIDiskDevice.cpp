/*
* Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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
#include <Kernel/Devices/AHCIController.h>
#include <Kernel/Devices/AHCIDiskDevice.h>
#include <Kernel/Devices/AHCIPort.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

AHCIDiskDevice::AHCIDiskDevice(AHCIPort& port, unsigned major, unsigned minor)
    : StorageDevice(major, minor, 512)
    , m_port(port)
{
}

bool AHCIDiskDevice::read_blocks(unsigned index, u16 count, u8* out)
{
    if (AHCIController::the().has_fatal_error()) {
        return false;
    }

    auto& slot = m_port.find_free_slot();
    bool success = slot.issue_read(m_port,
        index, count, out);

    return success;
}

bool AHCIDiskDevice::write_blocks(unsigned index, u16 count, const u8*)
{
    dbg() << "FIXME: Write " << count << " sectors to " << index;
    return false;
}

AHCIDiskDevice::~AHCIDiskDevice()
{
}

}