/*
 * Copyright (c) 2022, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NVMePollQueue.h"
#include "Kernel/Arch/x86/IO.h"
#include "Kernel/Devices/BlockDevice.h"
#include "NVMeDefinitions.h"

namespace Kernel {
UNMAP_AFTER_INIT NVMePollQueue::NVMePollQueue(NonnullOwnPtr<Memory::Region> rw_dma_region, Memory::PhysicalPage const& rw_dma_page, u16 qid, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> cq_dma_page, OwnPtr<Memory::Region> sq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> sq_dma_page, Memory::TypedMapping<volatile DoorbellRegister> db_regs)
    : NVMeQueue(move(rw_dma_region), rw_dma_page, qid, q_depth, move(cq_dma_region), cq_dma_page, move(sq_dma_region), sq_dma_page, move(db_regs))
{
}

void NVMePollQueue::submit_sqe(NVMeSubmission& sub)
{
    NVMeQueue::submit_sqe(sub);
    SpinlockLocker lock_cq(m_cq_lock);
    while (!process_cq()) {
        IO::delay(1);
    }
}

void NVMePollQueue::complete_current_request(u16 status)
{
    auto current_request = m_current_request;
    m_current_request.clear();
    if (status) {
        current_request->complete(AsyncBlockDeviceRequest::Failure);
        return;
    }
    if (current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Read) {
        if (auto result = current_request->write_to_buffer(current_request->buffer(), m_rw_dma_region->vaddr().as_ptr(), 512 * current_request->block_count()); result.is_error()) {
            current_request->complete(AsyncDeviceRequest::MemoryFault);
            return;
        }
    }
    current_request->complete(AsyncDeviceRequest::Success);
    return;
}
}
