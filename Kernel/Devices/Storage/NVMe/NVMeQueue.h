/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Storage/NVMe/NVMeDefinitions.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

struct DoorbellRegister {
    u32 sq_tail;
    u32 cq_head;
};

struct Doorbell {
    Memory::TypedMapping<DoorbellRegister volatile> mmio_reg;
    Memory::TypedMapping<DoorbellRegister> dbbuf_shadow;
    Memory::TypedMapping<DoorbellRegister> dbbuf_eventidx;
};

enum class QueueType {
    Polled,
    IRQ
};

class AsyncBlockDeviceRequest;

struct NVMeIO {
    void clear()
    {
        request = nullptr;
        end_io_handler = nullptr;
    }
    RefPtr<AsyncBlockDeviceRequest> request;
    Function<void(u16 status)> end_io_handler;
};

class NVMeController;
class NVMeQueue : public AtomicRefCounted<NVMeQueue> {
public:
    static ErrorOr<NonnullLockRefPtr<NVMeQueue>> try_create(NVMeController& device, u16 qid, Optional<u8> irq, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs, QueueType queue_type);
    bool is_admin_queue() { return m_admin_queue; }
    u16 submit_sync_sqe(NVMeSubmission&);
    void read(AsyncBlockDeviceRequest& request, u16 nsid, u64 index, u32 count);
    void write(AsyncBlockDeviceRequest& request, u16 nsid, u64 index, u32 count);
    virtual void submit_sqe(NVMeSubmission&);
    virtual ~NVMeQueue();

protected:
    u32 process_cq();

    // Updates the shadow buffer and returns if mmio is needed
    bool update_shadow_buf(u16 new_value, u32* dbbuf, u32* ei)
    {
        u32 const old = *dbbuf;

        *dbbuf = new_value;
        AK::full_memory_barrier();

        bool need_mmio = static_cast<u16>(new_value - *ei - 1) < static_cast<u16>(new_value - old);
        return need_mmio;
    }

    void update_sq_doorbell()
    {
        full_memory_barrier();
        if (m_db_regs.dbbuf_shadow.paddr.is_null()
            || update_shadow_buf(m_sq_tail, &m_db_regs.dbbuf_shadow->sq_tail, &m_db_regs.dbbuf_eventidx->sq_tail))
            m_db_regs.mmio_reg->sq_tail = m_sq_tail;
    }

    NVMeQueue(NonnullOwnPtr<Memory::Region> rw_dma_region, Memory::PhysicalRAMPage const& rw_dma_page, u16 qid, u32 q_depth, OwnPtr<Memory::Region> cq_dma_region, OwnPtr<Memory::Region> sq_dma_region, Doorbell db_regs);

    [[nodiscard]] u32 get_request_cid()
    {
        u32 expected_tag = m_tag.load(AK::memory_order_acquire);

        for (;;) {
            u32 cid = expected_tag + 1;
            if (cid == m_qdepth)
                cid = 0;
            if (m_tag.compare_exchange_strong(expected_tag, cid, AK::memory_order_acquire))
                return cid;
        }
    }

    virtual void complete_current_request(u16 cmdid, u16 status);

private:
    bool cqe_available();
    void update_cqe_head();
    void update_cq_doorbell()
    {
        full_memory_barrier();
        if (m_db_regs.dbbuf_shadow.paddr.is_null()
            || update_shadow_buf(m_cq_head, &m_db_regs.dbbuf_shadow->cq_head, &m_db_regs.dbbuf_eventidx->cq_head))
            m_db_regs.mmio_reg->cq_head = m_cq_head;
    }

protected:
    SpinlockProtected<HashMap<u16, NVMeIO>, LockRank::None> m_requests;
    NonnullOwnPtr<Memory::Region> m_rw_dma_region;

private:
    u16 m_qid {};
    u8 m_cq_valid_phase { 1 };
    u16 m_sq_tail {};
    u16 m_cq_head {};
    bool m_admin_queue { false };
    u32 m_qdepth {};
    Atomic<u32> m_tag { 0 }; // used for the cid in a submission queue entry
    Spinlock<LockRank::Interrupts> m_sq_lock {};
    OwnPtr<Memory::Region> m_cq_dma_region;
    Span<NVMeSubmission> m_sqe_array;
    OwnPtr<Memory::Region> m_sq_dma_region;
    Span<NVMeCompletion> m_cqe_array;
    WaitQueue m_sync_wait_queue;
    Doorbell m_db_regs;
    NonnullRefPtr<Memory::PhysicalRAMPage const> const m_rw_dma_page;
};
}
