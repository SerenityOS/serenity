/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Storage/RamdiskController.h>

namespace Kernel {

NonnullRefPtr<RamdiskController> RamdiskController::initialize()
{
    return adopt_ref(*new RamdiskController());
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
    VERIFY_NOT_REACHED();
}

void RamdiskController::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    VERIFY_NOT_REACHED();
}

RamdiskController::RamdiskController()
    : StorageController()
{
    // Populate ramdisk controllers from Multiboot boot modules, if any.
    size_t count = 0;
    for (auto& used_memory_range : MemoryManager::the().used_memory_ranges()) {
        if (used_memory_range.type == UsedMemoryRangeType::BootModule) {
            size_t length = page_round_up(used_memory_range.end.get()) - used_memory_range.start.get();
            auto region = MemoryManager::the().allocate_kernel_region(used_memory_range.start, length, "Ramdisk", Region::Access::Read | Region::Access::Write);
            if (!region)
                dmesgln("RamdiskController: Failed to allocate kernel region of size {}", length);
            else
                m_devices.append(RamdiskDevice::create(*this, region.release_nonnull(), 6, count));
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
