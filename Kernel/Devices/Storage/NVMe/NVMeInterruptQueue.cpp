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

ErrorOr<NonnullLockRefPtr<NVMeInterruptQueue>> NVMeInterruptQueue::try_create(PCI::Device& device, NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalPage> rw_dma_page, u16 qid, u8 irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs)
{
    auto queue = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) NVMeInterruptQueue(device, move(rw_dma_region), rw_dma_page, qid, irq, q_depth, move(cq_dma_region), move(sq_dma_region), move(db_regs))));
    queue->initialize_interrupt_queue();
    return queue;
}

UNMAP_AFTER_INIT NVMeInterruptQueue::NVMeInterruptQueue(PCI::Device& device, NonnullOwnPtr<Memory::Region> rw_dma_region, NonnullRefPtr<Memory::PhysicalPage> rw_dma_page, u16 qid, u8 irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs)
    : NVMeQueue(move(rw_dma_region), rw_dma_page, qid, q_depth, move(cq_dma_region), move(sq_dma_region), move(db_regs))
    , PCIIRQHandler(device, irq)
{
}

void NVMeInterruptQueue::initialize_interrupt_queue()
{
    enable_irq();
}

bool NVMeInterruptQueue::handle_irq(RegisterState const&)
{
    SpinlockLocker lock(m_request_lock);
    return process_cq() ? true : false;
}

void NVMeInterruptQueue::submit_sqe(NVMeSubmission& sub)
{
    NVMeQueue::submit_sqe(sub);
}

void NVMeInterruptQueue::complete_current_request(u16 cmdid, u16 status)
{
    auto work_item_creation_result = g_io_work->try_queue([this, cmdid, status]() {
        SpinlockLocker lock(m_request_lock);
        auto& request_pdu = m_requests.get(cmdid).release_value();
        auto current_request = request_pdu.request;
        AsyncDeviceRequest::RequestResult req_result = AsyncDeviceRequest::Success;

        ScopeGuard guard = [req_result, status, &request_pdu, &lock] {
            // FIXME: We should unlock at the end of this function to make sure no new requests is inserted
            //  before we complete the request and calling end_io_handler but that results in a deadlock
            //  For now this is avoided by asserting the `used` field while inserting.
            lock.unlock();
            if (request_pdu.request)
                request_pdu.request->complete(req_result);
            if (request_pdu.end_io_handler)
                request_pdu.end_io_handler(status);
            request_pdu.clear();
        };

        // There can be submission without any request associated with it such as with
        // admin queue commands during init. If there is no request, we are done
        if (!current_request)
            return;

        if (status) {
            req_result = AsyncBlockDeviceRequest::Failure;
            return;
        }

        if (current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Read) {
            if (auto result = current_request->write_to_buffer(current_request->buffer(), m_rw_dma_region->vaddr().as_ptr(), current_request->buffer_size()); result.is_error()) {
                req_result = AsyncBlockDeviceRequest::MemoryFault;
                return;
            }
        }
        return;
    });

    if (work_item_creation_result.is_error()) {
        SpinlockLocker lock(m_request_lock);
        auto& request_pdu = m_requests.get(cmdid).release_value();
        auto current_request = request_pdu.request;

        current_request->complete(AsyncDeviceRequest::OutOfMemory);
        if (request_pdu.end_io_handler)
            request_pdu.end_io_handler(status);
        request_pdu.clear();
    }
}
}
