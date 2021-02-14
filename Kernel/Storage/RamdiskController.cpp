/*
 * Copyright (c) 2021, the SerenityOS developers
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
#include <Kernel/Storage/RamdiskController.h>

namespace Kernel {

NonnullRefPtr<RamdiskController> RamdiskController::initialize()
{
    return adopt(*new RamdiskController());
}

bool RamdiskController::reset()
{
    TODO();
}

bool RamdiskController::shutdown()
{
    TODO();
}

size_t RamdiskController::devices_count() const
{
    return m_devices.size();
}

void RamdiskController::start_request(const StorageDevice&, AsyncBlockDeviceRequest&)
{
    ASSERT_NOT_REACHED();
}

void RamdiskController::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    ASSERT_NOT_REACHED();
}

RamdiskController::RamdiskController()
    : StorageController()
{
    // Populate ramdisk controllers from Multiboot boot modules, if any.
    size_t count = 0;
    for (auto used_memory_range : MemoryManager::the().used_memory_ranges()) {
        if (used_memory_range.type == UsedMemoryRangeType::BootModule) {
            size_t length = page_round_up(used_memory_range.end.get()) - used_memory_range.start.get();
            auto region = MemoryManager::the().allocate_kernel_region(used_memory_range.start, length, "Ramdisk", Region::Access::Read | Region::Access::Write);
            m_devices.append(RamdiskDevice::create(*this, move(region), 6, count));
            count++;
        }
    }
}

RamdiskController::~RamdiskController()
{
}

RefPtr<StorageDevice> RamdiskController::device(u32 index) const
{
    if (index >= m_devices.size())
        return nullptr;
    return m_devices[index];
}

}
