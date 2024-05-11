/*
 * Copyright (c) 2022, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/Storage/NVMe/NVMeDefinitions.h>
#include <Kernel/Devices/Storage/NVMe/NVMePollQueue.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<NVMePollQueue>> NVMePollQueue::try_create(NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalRAMPage> rw_dma_page, u16 qid, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs)
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) NVMePollQueue(move(rw_dma_region), rw_dma_page, qid, q_depth, move(cq_dma_region), move(sq_dma_region), move(db_regs))));
}

UNMAP_AFTER_INIT NVMePollQueue::NVMePollQueue(NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalRAMPage> rw_dma_page, u16 qid, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs)
    : NVMeQueue(move(rw_dma_region), rw_dma_page, qid, q_depth, move(cq_dma_region), move(sq_dma_region), move(db_regs))
{
}

void NVMePollQueue::submit_sqe(NVMeSubmission& sub)
{
    NVMeQueue::submit_sqe(sub);
    SpinlockLocker lock_cq(m_cq_lock);
    while (!process_cq()) {
        microseconds_delay(1);
    }
}

}
