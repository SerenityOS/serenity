/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Devices/Storage/NVMe/NVMeController.h>
#include <Kernel/Devices/Storage/NVMe/NVMeInterruptQueue.h>
#include <Kernel/Devices/Storage/NVMe/NVMePollQueue.h>
#include <Kernel/Devices/Storage/NVMe/NVMeQueue.h>
#include <Kernel/Library/StdLib.h>

namespace Kernel {
ErrorOr<NonnullLockRefPtr<NVMeQueue>> NVMeQueue::try_create(NVMeController& device, u16 qid, Optional<u8> irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs, QueueType queue_type)
{
    // Note: Allocate DMA region for RW operation. For now the requests don't exceed more than 4096 bytes (Storage device takes care of it)
    RefPtr<Memory::PhysicalRAMPage> rw_dma_page;
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    auto rw_dma_region = TRY(MM.allocate_dma_buffer_page("NVMe Queue Read/Write DMA"sv, Memory::Region::Access::ReadWrite, rw_dma_page, Memory::MemoryType::IO));

    if (rw_dma_page.is_null())
        return ENOMEM;

    if (queue_type == QueueType::Polled) {
        auto queue = NVMePollQueue::try_create(move(rw_dma_region), rw_dma_page.release_nonnull(), qid, q_depth, move(cq_dma_region), move(sq_dma_region), move(db_regs));
        return queue;
    }

    auto queue = NVMeInterruptQueue::try_create(device, move(rw_dma_region), rw_dma_page.release_nonnull(), qid, irq.release_value(), q_depth, move(cq_dma_region), move(sq_dma_region), move(db_regs));
    return queue;
}

UNMAP_AFTER_INIT NVMeQueue::NVMeQueue(NonnullOwnPtr<Memory::Region> rw_dma_region, Memory::PhysicalRAMPage const& rw_dma_page, u16 qid, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs)
    : m_rw_dma_region(move(rw_dma_region))
    , m_qid(qid)
    , m_admin_queue(qid == 0)
    , m_qdepth(q_depth)
    , m_cq_dma_region(move(cq_dma_region))
    , m_sq_dma_region(move(sq_dma_region))
    , m_db_regs(move(db_regs))
    , m_rw_dma_page(rw_dma_page)

{
    m_requests.with([q_depth](auto& requests) {
        requests.try_ensure_capacity(q_depth).release_value_but_fixme_should_propagate_errors();
    });
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
    m_requests.with([this, &nr_of_processed_cqes](auto& requests) {
        while (cqe_available()) {
            u16 status;
            u16 cmdid;
            ++nr_of_processed_cqes;
            status = CQ_STATUS_FIELD(m_cqe_array[m_cq_head].status);
            cmdid = m_cqe_array[m_cq_head].command_id;
            dbgln_if(NVME_DEBUG, "NVMe: Completion with status {:x} and command identifier {}. CQ_HEAD: {}", status, cmdid, m_cq_head);

            if (!requests.contains(cmdid)) {
                dmesgln("Bogus cmd id: {}", cmdid);
                VERIFY_NOT_REACHED();
            }
            complete_current_request(cmdid, status);
            update_cqe_head();
        }
    });
    if (nr_of_processed_cqes) {
        update_cq_doorbell();
    }
    return nr_of_processed_cqes;
}

void NVMeQueue::submit_sqe(NVMeSubmission& sub)
{
    SpinlockLocker lock(m_sq_lock);

    memcpy(&m_sqe_array[m_sq_tail], &sub, sizeof(NVMeSubmission));
    {
        u32 temp_sq_tail = m_sq_tail + 1;
        if (temp_sq_tail == m_qdepth)
            m_sq_tail = 0;
        else
            m_sq_tail = temp_sq_tail;
    }

    dbgln_if(NVME_DEBUG, "NVMe: Submission with command identifier {}. SQ_TAIL: {}", sub.cmdid, m_sq_tail);
    update_sq_doorbell();
}

void NVMeQueue::complete_current_request(u16 cmdid, u16 status)
{
    m_requests.with([this, cmdid, status](auto& requests) {
        auto& request_pdu = requests.get(cmdid).release_value();
        auto current_request = request_pdu.request;
        AsyncDeviceRequest::RequestResult req_result = AsyncDeviceRequest::Success;

        ScopeGuard guard = [&req_result, status, &request_pdu] {
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
    });
}

u16 NVMeQueue::submit_sync_sqe(NVMeSubmission& sub)
{
    // For now let's use sq tail as a unique command id.
    u16 cmd_status;
    u16 cid = get_request_cid();
    sub.cmdid = cid;

    m_requests.with([this, &sub, &cmd_status](auto& requests) {
        requests.set(sub.cmdid, { nullptr, [this, &cmd_status](u16 status) mutable { cmd_status = status; m_sync_wait_queue.wake_all(); } });
    });
    submit_sqe(sub);

    // FIXME: Only sync submissions (usually used for admin commands) use a WaitQueue based IO. Eventually we need to
    //  move this logic into the block layer instead of sprinkling them in the driver code.
    m_sync_wait_queue.wait_forever("NVMe sync submit"sv);
    return cmd_status;
}

void NVMeQueue::read(AsyncBlockDeviceRequest& request, u16 nsid, u64 index, u32 count)
{
    NVMeSubmission sub {};
    sub.op = OP_NVME_READ;
    sub.rw.nsid = nsid;
    sub.rw.slba = AK::convert_between_host_and_little_endian(index);
    // No. of lbas is 0 based
    sub.rw.length = AK::convert_between_host_and_little_endian((count - 1) & 0xFFFF);
    sub.rw.data_ptr.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(m_rw_dma_page->paddr().as_ptr()));
    sub.cmdid = get_request_cid();

    m_requests.with([&sub, &request](auto& requests) {
        requests.set(sub.cmdid, { request, nullptr });
    });

    full_memory_barrier();
    submit_sqe(sub);
}

void NVMeQueue::write(AsyncBlockDeviceRequest& request, u16 nsid, u64 index, u32 count)
{
    NVMeSubmission sub {};

    sub.op = OP_NVME_WRITE;
    sub.rw.nsid = nsid;
    sub.rw.slba = AK::convert_between_host_and_little_endian(index);
    // No. of lbas is 0 based
    sub.rw.length = AK::convert_between_host_and_little_endian((count - 1) & 0xFFFF);
    sub.rw.data_ptr.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(m_rw_dma_page->paddr().as_ptr()));
    sub.cmdid = get_request_cid();

    m_requests.with([&sub, &request](auto& requests) {
        requests.set(sub.cmdid, { request, nullptr });
    });

    if (auto result = request.read_from_buffer(request.buffer(), m_rw_dma_region->vaddr().as_ptr(), request.buffer_size()); result.is_error()) {
        complete_current_request(sub.cmdid, AsyncDeviceRequest::MemoryFault);
        return;
    }

    full_memory_barrier();
    submit_sqe(sub);
}

UNMAP_AFTER_INIT NVMeQueue::~NVMeQueue() = default;
}
