/*
 * Copyright (c) 2022, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Storage/NVMe/NVMeQueue.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>

namespace Kernel {

class NVMeInterruptQueue : public NVMeQueue
    , public PCI::IRQHandler {
public:
    static ErrorOr<NonnullLockRefPtr<NVMeInterruptQueue>> try_create(PCI::Device& device, NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalRAMPage> rw_dma_page, u16 qid, u8 irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs);
    void submit_sqe(NVMeSubmission& submission) override;
    virtual ~NVMeInterruptQueue() override {};
    virtual StringView purpose() const override { return "NVMe"sv; }
    void initialize_interrupt_queue();

protected:
    NVMeInterruptQueue(PCI::Device& device, NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalRAMPage> rw_dma_page, u16 qid, u8 irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs);

private:
    virtual void complete_current_request(u16 cmdid, u16 status) override;
    bool handle_irq() override;
};
}
