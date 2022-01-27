/*
 * Copyright (c) 2022, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NVMeInterruptQueue.h"
#include "Kernel/Devices/BlockDevice.h"
#include "NVMeDefinitions.h"
#include <Kernel/WorkQueue.h>

namespace Kernel {

UNMAP_AFTER_INIT NVMeInterruptQueue::NVMeInterruptQueue(NonnullOwnPtr<Memory::Region> rw_dma_region, Memory::PhysicalPage const& rw_dma_page, u16 qid, u8 irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> cq_dma_page, OwnPtr<Memory::Region> sq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> sq_dma_page, Memory::TypedMapping<volatile DoorbellRegister> db_regs)
    : NVMeQueue(move(rw_dma_region), rw_dma_page, qid, q_depth, move(cq_dma_region), cq_dma_page, move(sq_dma_region), sq_dma_page, move(db_regs))
    , IRQHandler(irq)
{
    enable_irq();
}

bool NVMeInterruptQueue::handle_irq(const RegisterState&)
{
    SpinlockLocker lock(m_request_lock);
    return process_cq() ? true : false;
}

void NVMeInterruptQueue::submit_sqe(NVMeSubmission& sub)
{
    NVMeQueue::submit_sqe(sub);
}

void NVMeInterruptQueue::complete_current_request(u16 status)
{
    VERIFY(m_request_lock.is_locked());

    g_io_work->queue([this, status]() {
        SpinlockLocker lock(m_request_lock);
        auto current_request = m_current_request;
        m_current_request.clear();
        if (status) {
            lock.unlock();
            current_request->complete(AsyncBlockDeviceRequest::Failure);
            return;
        }
        if (current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Read) {
            if (auto result = current_request->write_to_buffer(current_request->buffer(), m_rw_dma_region->vaddr().as_ptr(), 512 * current_request->block_count()); result.is_error()) {
                lock.unlock();
                current_request->complete(AsyncDeviceRequest::MemoryFault);
                return;
            }
        }
        lock.unlock();
        current_request->complete(AsyncDeviceRequest::Success);
        return;
    });
}
}
