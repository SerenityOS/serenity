/*
 * Copyright (c) 2022, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Storage/NVMe/NVMeQueue.h>

namespace Kernel {

class NVMeInterruptQueue : public NVMeQueue
    , public IRQHandler {
public:
    NVMeInterruptQueue(NonnullOwnPtr<Memory::Region> rw_dma_region, Memory::PhysicalPage const& rw_dma_page, u16 qid, u8 irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> cq_dma_page, OwnPtr<Memory::Region> sq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> sq_dma_page, Memory::TypedMapping<volatile DoorbellRegister> db_regs);
    void submit_sqe(NVMeSubmission& submission) override;
    virtual ~NVMeInterruptQueue() override {};

private:
    virtual void complete_current_request(u16 status) override;
    bool handle_irq(RegisterState const&) override;
};
}
