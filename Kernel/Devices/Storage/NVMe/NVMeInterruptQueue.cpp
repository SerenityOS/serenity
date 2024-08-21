/*
 * Copyright (c) 2022, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/Storage/NVMe/NVMeDefinitions.h>
#include <Kernel/Devices/Storage/NVMe/NVMeInterruptQueue.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<NVMeInterruptQueue>> NVMeInterruptQueue::try_create(PCI::Device& device, NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalRAMPage> rw_dma_page, u16 qid, u8 irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs)
{
    auto queue = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) NVMeInterruptQueue(device, move(rw_dma_region), rw_dma_page, qid, irq, q_depth, move(cq_dma_region), move(sq_dma_region), move(db_regs))));
    queue->initialize_interrupt_queue();
    return queue;
}

UNMAP_AFTER_INIT NVMeInterruptQueue::NVMeInterruptQueue(PCI::Device& device, NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalRAMPage> rw_dma_page, u16 qid, u8 irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs)
    : NVMeQueue(move(rw_dma_region), rw_dma_page, qid, q_depth, move(cq_dma_region), move(sq_dma_region), move(db_regs))
    , PCI::IRQHandler(device, irq)
{
}

void NVMeInterruptQueue::initialize_interrupt_queue()
{
    enable_irq();
}

bool NVMeInterruptQueue::handle_irq()
{
    return process_cq() ? true : false;
}

void NVMeInterruptQueue::submit_sqe(NVMeSubmission& sub)
{
    NVMeQueue::submit_sqe(sub);
}

void NVMeInterruptQueue::complete_current_request(u16 cmdid, u16 status)
{
    auto work_item_creation_result = g_io_work->try_queue([this, cmdid, status]() {
        NVMeQueue::complete_current_request(cmdid, status);
    });

    if (work_item_creation_result.is_error()) {
        m_requests.with([cmdid, status](auto& requests) {
            auto& request_pdu = requests.get(cmdid).release_value();
            auto current_request = request_pdu.request;

            current_request->complete(AsyncDeviceRequest::OutOfMemory);
            if (request_pdu.end_io_handler)
                request_pdu.end_io_handler(status);
            request_pdu.clear();
        });
    }
}
}
