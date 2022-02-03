/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NVMeQueue.h"
#include "Kernel/StdLib.h"
#include "NVMeQueue.h"
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Storage/NVMe/NVMeController.h>
#include <Kernel/Storage/NVMe/NVMeInterruptQueue.h>
#include <Kernel/Storage/NVMe/NVMePollQueue.h>

namespace Kernel {
ErrorOr<NonnullRefPtr<NVMeQueue>> NVMeQueue::try_create(u16 qid, Optional<u8> irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> cq_dma_page, OwnPtr<Memory::Region> sq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> sq_dma_page, Memory::TypedMapping<volatile DoorbellRegister> db_regs)
{
    // Note: Allocate DMA region for RW operation. For now the requests don't exceed more than 4096 bytes (Storage device takes care of it)
    RefPtr<Memory::PhysicalPage> rw_dma_page;
    auto rw_dma_region = TRY(MM.allocate_dma_buffer_page("NVMe Queue Read/Write DMA"sv, Memory::Region::Access::ReadWrite, rw_dma_page));
    if (!irq.has_value()) {
        auto queue = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) NVMePollQueue(move(rw_dma_region), *rw_dma_page, qid, q_depth, move(cq_dma_region), cq_dma_page, move(sq_dma_region), sq_dma_page, move(db_regs))));
        return queue;
    }
    auto queue = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) NVMeInterruptQueue(move(rw_dma_region), *rw_dma_page, qid, irq.value(), q_depth, move(cq_dma_region), cq_dma_page, move(sq_dma_region), sq_dma_page, move(db_regs))));
    return queue;
}

UNMAP_AFTER_INIT NVMeQueue::NVMeQueue(NonnullOwnPtr<Memory::Region> rw_dma_region, Memory::PhysicalPage const& rw_dma_page, u16 qid, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> cq_dma_page, OwnPtr<Memory::Region> sq_dma_region, NonnullRefPtrVector<Memory::PhysicalPage> sq_dma_page, Memory::TypedMapping<volatile DoorbellRegister> db_regs)
    : m_current_request(nullptr)
    , m_rw_dma_region(move(rw_dma_region))
    , m_qid(qid)
    , m_admin_queue(qid == 0)
    , m_qdepth(q_depth)
    , m_cq_dma_region(move(cq_dma_region))
    , m_cq_dma_page(cq_dma_page)
    , m_sq_dma_region(move(sq_dma_region))
    , m_sq_dma_page(sq_dma_page)
    , m_db_regs(move(db_regs))
    , m_rw_dma_page(rw_dma_page)

{
    m_sqe_array = { reinterpret_cast<NVMeSubmission*>(m_sq_dma_region->vaddr().as_ptr()), m_qdepth };
    m_cqe_array = { reinterpret_cast<NVMeCompletion*>(m_cq_dma_region->vaddr().as_ptr()), m_qdepth };
}

bool NVMeQueue::cqe_available()
{
    return PHASE_TAG(m_cqe_array[m_cq_head].status) == m_cq_valid_phase;
}

void NVMeQueue::update_cqe_head()
{
    // To prevent overflow, use a temp variable
    u32 temp_cq_head = m_cq_head + 1;
    if (temp_cq_head == m_qdepth) {
        m_cq_head = 0;
        m_cq_valid_phase ^= 1;
    } else {
        m_cq_head = temp_cq_head;
    }
}

u32 NVMeQueue::process_cq()
{
    u32 nr_of_processed_cqes = 0;
    while (cqe_available()) {
        u16 status;
        u16 cmdid;
        ++nr_of_processed_cqes;
        status = CQ_STATUS_FIELD(m_cqe_array[m_cq_head].status);
        cmdid = m_cqe_array[m_cq_head].command_id;
        dbgln_if(NVME_DEBUG, "NVMe: Completion with status {:x} and command identifier {}. CQ_HEAD: {}", status, cmdid, m_cq_head);
        // TODO: We don't use AsyncBlockDevice requests for admin queue as it is only applicable for a block device (NVMe namespace)
        //  But admin commands precedes namespace creation. Unify requests to avoid special conditions
        if (m_admin_queue == false) {
            // As the block layer calls are now sync (as we wait on each requests),
            // everything is operated on a single request similar to BMIDE driver.
            // TODO: Remove this constraint eventually.
            VERIFY(cmdid == m_prev_sq_tail);
            if (m_current_request) {
                complete_current_request(status);
            }
        }
        update_cqe_head();
    }
    if (nr_of_processed_cqes) {
        update_cq_doorbell();
    }
    return nr_of_processed_cqes;
}

void NVMeQueue::submit_sqe(NVMeSubmission& sub)
{
    SpinlockLocker lock(m_sq_lock);
    // For now let's use sq tail as a unique command id.
    sub.cmdid = m_sq_tail;
    m_prev_sq_tail = m_sq_tail;

    memcpy(&m_sqe_array[m_sq_tail], &sub, sizeof(NVMeSubmission));
    {
        u32 temp_sq_tail = m_sq_tail + 1;
        if (temp_sq_tail == m_qdepth)
            m_sq_tail = 0;
        else
            m_sq_tail = temp_sq_tail;
    }

    dbgln_if(NVME_DEBUG, "NVMe: Submission with command identifier {}. SQ_TAIL: {}", sub.cmdid, m_sq_tail);
    full_memory_barrier();
    update_sq_doorbell();
}

u16 NVMeQueue::submit_sync_sqe(NVMeSubmission& sub)
{
    // For now let's use sq tail as a unique command id.
    u16 cqe_cid;
    u16 cid = m_sq_tail;

    submit_sqe(sub);
    do {
        int index;
        {
            SpinlockLocker lock(m_cq_lock);
            index = m_cq_head - 1;
            if (index < 0)
                index = m_qdepth - 1;
        }
        cqe_cid = m_cqe_array[index].command_id;
        IO::delay(1);
    } while (cid != cqe_cid);

    auto status = CQ_STATUS_FIELD(m_cqe_array[m_cq_head].status);
    return status;
}

void NVMeQueue::read(AsyncBlockDeviceRequest& request, u16 nsid, u64 index, u32 count)
{
    NVMeSubmission sub {};
    SpinlockLocker m_lock(m_request_lock);
    m_current_request = request;

    sub.op = OP_NVME_READ;
    sub.rw.nsid = nsid;
    sub.rw.slba = AK::convert_between_host_and_little_endian(index);
    // No. of lbas is 0 based
    sub.rw.length = AK::convert_between_host_and_little_endian((count - 1) & 0xFFFF);
    sub.rw.data_ptr.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(m_rw_dma_page->paddr().as_ptr()));

    full_memory_barrier();
    submit_sqe(sub);
}

void NVMeQueue::write(AsyncBlockDeviceRequest& request, u16 nsid, u64 index, u32 count)
{
    NVMeSubmission sub {};
    SpinlockLocker m_lock(m_request_lock);
    m_current_request = request;

    if (auto result = m_current_request->read_from_buffer(m_current_request->buffer(), m_rw_dma_region->vaddr().as_ptr(), 512 * m_current_request->block_count()); result.is_error()) {
        complete_current_request(AsyncDeviceRequest::MemoryFault);
        return;
    }
    sub.op = OP_NVME_WRITE;
    sub.rw.nsid = nsid;
    sub.rw.slba = AK::convert_between_host_and_little_endian(index);
    // No. of lbas is 0 based
    sub.rw.length = AK::convert_between_host_and_little_endian((count - 1) & 0xFFFF);
    sub.rw.data_ptr.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(m_rw_dma_page->paddr().as_ptr()));

    full_memory_barrier();
    submit_sqe(sub);
}

UNMAP_AFTER_INIT NVMeQueue::~NVMeQueue()
{
}
}
