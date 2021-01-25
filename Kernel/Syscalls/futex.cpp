/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Singleton.h>
#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/Process.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

static SpinLock<u8> g_global_futex_lock;
static AK::Singleton<HashMap<VMObject*, FutexQueues>> g_global_futex_queues;

FutexQueue::FutexQueue(FlatPtr user_address_or_offset, VMObject* vmobject)
    : m_user_address_or_offset(user_address_or_offset)
    , m_is_global(vmobject != nullptr)
{
    dbgln<FUTEX_DEBUG>("Futex @ {}{}",
        this,
        m_is_global ? " (global)" : " (local)");

    if (m_is_global) {
        // Only register for global futexes
        m_vmobject = vmobject->make_weak_ptr();
        vmobject->register_on_deleted_handler(*this);
    }
}

FutexQueue::~FutexQueue()
{
    if (m_is_global) {
        if (auto vmobject = m_vmobject.strong_ref())
            vmobject->unregister_on_deleted_handler(*this);
    }
    dbgln<FUTEX_DEBUG>("~Futex @ {}{}",
        this,
        m_is_global ? " (global)" : " (local)");
}

void FutexQueue::vmobject_deleted(VMObject& vmobject)
{
    ASSERT(m_is_global); // If we got called we must be a global futex
    // Because we're taking ourselves out of the global queue, we need
    // to make sure we have at last a reference until we're done
    NonnullRefPtr<FutexQueue> own_ref(*this);

    dbgln<FUTEX_DEBUG>("Futex::vmobject_deleted @ {}{}",
        this,
        m_is_global ? " (global)" : " (local)");

    // Because this is called from the VMObject's destructor, getting a
    // strong_ref in this function is unsafe!
    m_vmobject = nullptr; // Just to be safe...

    {
        ScopedSpinLock lock(g_global_futex_lock);
        g_global_futex_queues->remove(&vmobject);
    }

    bool did_wake_all;
    auto wake_count = wake_all(did_wake_all);

    if constexpr (FUTEX_DEBUG) {
        if (wake_count > 0)
            dbgln("Futex @ {} unblocked {} waiters due to vmobject free", this, wake_count);
    }

    ASSERT(did_wake_all); // No one should be left behind...
}

void Process::clear_futex_queues_on_exec()
{
    ScopedSpinLock lock(m_futex_lock);
    for (auto& it : m_futex_queues) {
        bool did_wake_all;
        it.value->wake_all(did_wake_all);
        ASSERT(did_wake_all); // No one should be left behind...
    }
    m_futex_queues.clear();
}

int Process::sys$futex(Userspace<const Syscall::SC_futex_params*> user_params)
{
    REQUIRE_PROMISE(thread);

    Syscall::SC_futex_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    Thread::BlockTimeout timeout;
    u32 cmd = params.futex_op & FUTEX_CMD_MASK;
    switch (cmd) {
    case FUTEX_WAIT:
    case FUTEX_WAIT_BITSET:
    case FUTEX_REQUEUE:
    case FUTEX_CMP_REQUEUE: {
        if (params.timeout) {
            timespec ts_stimeout { 0, 0 };
            if (!copy_from_user(&ts_stimeout, params.timeout))
                return -EFAULT;
            clockid_t clock_id = (params.futex_op & FUTEX_CLOCK_REALTIME) ? CLOCK_REALTIME_COARSE : CLOCK_MONOTONIC_COARSE;
            bool is_absolute = cmd != FUTEX_WAIT;
            timeout = Thread::BlockTimeout(is_absolute, &ts_stimeout, nullptr, clock_id);
        }
        if (cmd == FUTEX_WAIT_BITSET && params.val3 == FUTEX_BITSET_MATCH_ANY)
            cmd = FUTEX_WAIT;
        break;
    case FUTEX_WAKE_BITSET:
        if (params.val3 == FUTEX_BITSET_MATCH_ANY)
            cmd = FUTEX_WAKE;
        break;
    }
    }

    bool is_private = (params.futex_op & FUTEX_PRIVATE_FLAG) != 0;
    auto& queue_lock = is_private ? m_futex_lock : g_global_futex_lock;
    auto user_address_or_offset = FlatPtr(params.userspace_address);
    auto user_address_or_offset2 = FlatPtr(params.userspace_address2);

    // If this is a global lock, look up the underlying VMObject *before*
    // acquiring the queue lock
    RefPtr<VMObject> vmobject, vmobject2;
    if (!is_private) {
        if (!Kernel::is_user_range(VirtualAddress(user_address_or_offset), sizeof(u32)))
            return -EFAULT;
        auto region = MM.find_region_from_vaddr(*Process::current(), VirtualAddress(user_address_or_offset));
        if (!region)
            return -EFAULT;
        vmobject = region->vmobject();
        user_address_or_offset = region->offset_in_vmobject_from_vaddr(VirtualAddress(user_address_or_offset));

        switch (cmd) {
        case FUTEX_REQUEUE:
        case FUTEX_CMP_REQUEUE:
        case FUTEX_WAKE_OP: {
            if (!Kernel::is_user_range(VirtualAddress(user_address_or_offset2), sizeof(u32)))
                return -EFAULT;
            auto region2 = MM.find_region_from_vaddr(*Process::current(), VirtualAddress(user_address_or_offset2));
            if (!region2)
                return -EFAULT;
            vmobject2 = region2->vmobject();
            user_address_or_offset2 = region->offset_in_vmobject_from_vaddr(VirtualAddress(user_address_or_offset2));
            break;
        }
        }
    }

    auto find_global_futex_queues = [&](VMObject& vmobject, bool create_if_not_found) -> FutexQueues* {
        auto& global_queues = *g_global_futex_queues;
        auto it = global_queues.find(&vmobject);
        if (it != global_queues.end())
            return &it->value;
        if (create_if_not_found) {
            // TODO: is there a better way than setting and finding it again?
            auto result = global_queues.set(&vmobject, {});
            ASSERT(result == AK::HashSetResult::InsertedNewEntry);
            it = global_queues.find(&vmobject);
            ASSERT(it != global_queues.end());
            return &it->value;
        }
        return nullptr;
    };

    auto find_futex_queue = [&](VMObject* vmobject, FlatPtr user_address_or_offset, bool create_if_not_found) -> RefPtr<FutexQueue> {
        ASSERT(is_private || vmobject);
        auto* queues = is_private ? &m_futex_queues : find_global_futex_queues(*vmobject, create_if_not_found);
        if (!queues)
            return {};
        auto it = queues->find(user_address_or_offset);
        if (it != queues->end())
            return it->value;
        if (create_if_not_found) {
            auto futex_queue = adopt(*new FutexQueue(user_address_or_offset, vmobject));
            auto result = queues->set(user_address_or_offset, futex_queue);
            ASSERT(result == AK::HashSetResult::InsertedNewEntry);
            return futex_queue;
        }
        return {};
    };

    auto remove_futex_queue = [&](VMObject* vmobject, FlatPtr user_address_or_offset) {
        auto* queues = is_private ? &m_futex_queues : find_global_futex_queues(*vmobject, false);
        if (queues) {
            queues->remove(user_address_or_offset);
            if (!is_private && queues->is_empty())
                g_global_futex_queues->remove(vmobject);
        }
    };

    auto do_wake = [&](VMObject* vmobject, FlatPtr user_address_or_offset, u32 count, Optional<u32> bitmask) -> int {
        if (count == 0)
            return 0;
        auto futex_queue = find_futex_queue(vmobject, user_address_or_offset, false);
        if (!futex_queue)
            return 0;
        bool is_empty;
        u32 woke_count = futex_queue->wake_n(count, bitmask, is_empty);
        if (is_empty) {
            // If there are no more waiters, we want to get rid of the futex!
            remove_futex_queue(vmobject, user_address_or_offset);
        }
        return (int)woke_count;
    };

    ScopedSpinLock lock(queue_lock);

    auto do_wait = [&](u32 bitset) -> int {
        auto user_value = user_atomic_load_relaxed(params.userspace_address);
        if (!user_value.has_value())
            return -EFAULT;
        if (user_value.value() != params.val) {
            dbgln("futex wait: EAGAIN. user value: {:p} @ {:p} != val: {}", user_value.value(), params.userspace_address, params.val);
            return -EAGAIN;
        }
        atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);

        auto futex_queue = find_futex_queue(vmobject.ptr(), user_address_or_offset, true);
        ASSERT(futex_queue);

        // We need to release the lock before blocking. But we have a reference
        // to the FutexQueue so that we can keep it alive.
        lock.unlock();

        Thread::BlockResult block_result = futex_queue->wait_on(timeout, bitset);

        lock.lock();
        if (futex_queue->is_empty()) {
            // If there are no more waiters, we want to get rid of the futex!
            remove_futex_queue(vmobject, user_address_or_offset);
        }
        if (block_result == Thread::BlockResult::InterruptedByTimeout) {
            return -ETIMEDOUT;
        }
        return 0;
    };

    auto do_requeue = [&](Optional<u32> val3) -> int {
        auto user_value = user_atomic_load_relaxed(params.userspace_address);
        if (!user_value.has_value())
            return -EFAULT;
        if (val3.has_value() && val3.value() != user_value.value())
            return -EAGAIN;
        atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);

        int woken_or_requeued = 0;
        if (auto futex_queue = find_futex_queue(vmobject.ptr(), user_address_or_offset, false)) {
            RefPtr<FutexQueue> target_futex_queue;
            bool is_empty, is_target_empty;
            woken_or_requeued = futex_queue->wake_n_requeue(
                params.val, [&]() -> FutexQueue* {
                    // NOTE: futex_queue's lock is being held while this callback is called
                    // The reason we're doing this in a callback is that we don't want to always
                    // create a target queue, only if we actually have anything to move to it!
                    target_futex_queue = find_futex_queue(vmobject2.ptr(), user_address_or_offset2, true);
                    return target_futex_queue.ptr();
                },
                params.val2, is_empty, is_target_empty);
            if (is_empty)
                remove_futex_queue(vmobject, user_address_or_offset);
            if (is_target_empty && target_futex_queue)
                remove_futex_queue(vmobject2, user_address_or_offset2);
        }
        return woken_or_requeued;
    };

    switch (cmd) {
    case FUTEX_WAIT:
        return do_wait(0);

    case FUTEX_WAKE:
        return do_wake(vmobject.ptr(), user_address_or_offset, params.val, {});

    case FUTEX_WAKE_OP: {
        Optional<u32> oldval;
        u32 op_arg = _FUTEX_OP_ARG(params.val3);
        auto op = _FUTEX_OP(params.val3);
        if (op & FUTEX_OP_ARG_SHIFT) {
            op_arg = 1 << op_arg;
            op &= FUTEX_OP_ARG_SHIFT;
        }
        atomic_thread_fence(AK::MemoryOrder::memory_order_release);
        switch (op) {
        case FUTEX_OP_SET:
            oldval = user_atomic_exchange_relaxed(params.userspace_address2, op_arg);
            break;
        case FUTEX_OP_ADD:
            oldval = user_atomic_fetch_add_relaxed(params.userspace_address2, op_arg);
            break;
        case FUTEX_OP_OR:
            oldval = user_atomic_fetch_or_relaxed(params.userspace_address2, op_arg);
            break;
        case FUTEX_OP_ANDN:
            oldval = user_atomic_fetch_and_not_relaxed(params.userspace_address2, op_arg);
            break;
        case FUTEX_OP_XOR:
            oldval = user_atomic_fetch_xor_relaxed(params.userspace_address2, op_arg);
            break;
        default:
            return -EINVAL;
        }
        if (!oldval.has_value())
            return -EFAULT;
        atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);
        int result = do_wake(vmobject.ptr(), user_address_or_offset, params.val, {});
        if (params.val2 > 0) {
            bool compare_result;
            switch (_FUTEX_CMP(params.val3)) {
            case FUTEX_OP_CMP_EQ:
                compare_result = (oldval.value() == _FUTEX_CMP_ARG(params.val3));
                break;
            case FUTEX_OP_CMP_NE:
                compare_result = (oldval.value() != _FUTEX_CMP_ARG(params.val3));
                break;
            case FUTEX_OP_CMP_LT:
                compare_result = (oldval.value() < _FUTEX_CMP_ARG(params.val3));
                break;
            case FUTEX_OP_CMP_LE:
                compare_result = (oldval.value() <= _FUTEX_CMP_ARG(params.val3));
                break;
            case FUTEX_OP_CMP_GT:
                compare_result = (oldval.value() > _FUTEX_CMP_ARG(params.val3));
                break;
            case FUTEX_OP_CMP_GE:
                compare_result = (oldval.value() >= _FUTEX_CMP_ARG(params.val3));
                break;
            default:
                return -EINVAL;
            }
            if (compare_result)
                result += do_wake(vmobject2.ptr(), user_address_or_offset2, params.val2, {});
        }
        return result;
    }

    case FUTEX_REQUEUE:
        return do_requeue({});

    case FUTEX_CMP_REQUEUE:
        return do_requeue(params.val3);

    case FUTEX_WAIT_BITSET:
        ASSERT(params.val3 != FUTEX_BITSET_MATCH_ANY); // we should have turned it into FUTEX_WAIT
        if (params.val3 == 0)
            return -EINVAL;
        return do_wait(params.val3);

    case FUTEX_WAKE_BITSET:
        ASSERT(params.val3 != FUTEX_BITSET_MATCH_ANY); // we should have turned it into FUTEX_WAKE
        if (params.val3 == 0)
            return -EINVAL;
        return do_wake(vmobject.ptr(), user_address_or_offset, params.val, params.val3);
    }
    return -ENOSYS;
}

}
