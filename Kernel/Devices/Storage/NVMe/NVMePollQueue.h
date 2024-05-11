/*
 * Copyright (c) 2022, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Storage/NVMe/NVMeQueue.h>

namespace Kernel {

class NVMePollQueue : public NVMeQueue {
public:
    static ErrorOr<NonnullLockRefPtr<NVMePollQueue>> try_create(NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalRAMPage> rw_dma_page, u16 qid, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs);
    void submit_sqe(NVMeSubmission& submission) override;
    virtual ~NVMePollQueue() override {};

protected:
    NVMePollQueue(NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalRAMPage> rw_dma_page, u16 qid, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs);

private:
    Spinlock<LockRank::Interrupts> m_cq_lock {};
};
}
