/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/Memory/InodeVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

static Singleton<SpinlockProtected<HashMap<GlobalFutexKey, NonnullLockRefPtr<FutexQueue>>, LockRank::None>> s_global_futex_queues;

void Process::clear_futex_queues_on_exec()
{
    s_global_futex_queues->with([this](auto& queues) {
        auto const* address_space = this->address_space().with([](auto& space) { return space.ptr(); });
        queues.remove_all_matching([address_space](auto& futex_key, auto& futex_queue) {
            if ((futex_key.raw.offset & futex_key_private_flag) == 0)
                return false;
            if (futex_key.private_.address_space != address_space)
                return false;
            bool did_wake_all;
            futex_queue->wake_all(did_wake_all);
            VERIFY(did_wake_all); // No one should be left behind...
            return true;
        });
    });
}

ErrorOr<GlobalFutexKey> Process::get_futex_key(FlatPtr user_address, bool shared)
{
    if (user_address & 0b11) // user_address points to a u32, so must be 4byte aligned
        return EINVAL;

    auto range = Memory::VirtualRange { VirtualAddress(user_address), sizeof(u32) };

    if (!Kernel::Memory::is_user_range(range))
        return EFAULT;

    if (!shared) { // If this is thread-shared, we can skip searching the matching region
        return GlobalFutexKey {
            .private_ = {
                .address_space = this->address_space().with([](auto& space) { return space.ptr(); }),
                .user_address = user_address | futex_key_private_flag,
            }
        };
    }

    return address_space().with([&](auto& space) -> ErrorOr<GlobalFutexKey> {
        auto* matching_region = space->find_region_containing(range);
        if (!matching_region)
            return EFAULT;

        // The user wants to share this futex, but if the address doesn't point to a shared resource, there's not
        // much sharing to be done, so let's mark this as private
        if (!matching_region->is_shared()) {
            return GlobalFutexKey {
                .private_ = {
                    .address_space = space.ptr(),
                    .user_address = user_address | futex_key_private_flag,
                }
            };
        }

        // This address is backed by a shared VMObject, if it's an AnonymousVMObject, it can be shared between processes
        // via forking, and shared regions that are cloned during a fork retain their original AnonymousVMObject.
        // On the other hand, if it's a SharedInodeVMObject, it can be shared by two processes mapping the same file as
        // MAP_SHARED, but since they are deduplicated based on the inode, in all cases the VMObject pointer should be
        // a unique global identifier.
        // NOTE: This assumes that a program will not unmap the only region keeping the vmobject alive while waiting on it,
        // if it does, it will get stuck waiting forever until interrupted by a signal, but since that use case is defined as
        // a programmer error, we are fine with it.

        auto const& vmobject = matching_region->vmobject();
        if (vmobject.is_inode())
            VERIFY(vmobject.is_shared_inode());

        return GlobalFutexKey {
            .shared = {
                .vmobject = &vmobject,
                .offset = matching_region->offset_in_vmobject_from_vaddr(range.base()) }
        };
    });
}

ErrorOr<FlatPtr> Process::sys$futex(Userspace<Syscall::SC_futex_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    auto params = TRY(copy_typed_from_user(user_params));

    Thread::BlockTimeout timeout;
    u32 cmd = params.futex_op & FUTEX_CMD_MASK;

    bool use_realtime_clock = (params.futex_op & FUTEX_CLOCK_REALTIME) != 0;
    if (use_realtime_clock && cmd != FUTEX_WAIT && cmd != FUTEX_WAIT_BITSET) {
        return ENOSYS;
    }

    bool shared = (params.futex_op & FUTEX_PRIVATE_FLAG) == 0;

    switch (cmd) {
    case FUTEX_WAIT:
    case FUTEX_WAIT_BITSET:
    case FUTEX_REQUEUE:
    case FUTEX_CMP_REQUEUE: {
        if (params.timeout) {
            auto timeout_time = TRY(copy_time_from_user(params.timeout));
            bool is_absolute = cmd != FUTEX_WAIT;
            clockid_t clock_id = use_realtime_clock ? CLOCK_REALTIME_COARSE : CLOCK_MONOTONIC_COARSE;
            timeout = Thread::BlockTimeout(is_absolute, &timeout_time, nullptr, clock_id);
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

    auto find_futex_queue = [&](GlobalFutexKey futex_key, bool create_if_not_found, bool* did_create = nullptr) -> ErrorOr<LockRefPtr<FutexQueue>> {
        VERIFY(!create_if_not_found || did_create != nullptr);
        return s_global_futex_queues->with([&](auto& queues) -> ErrorOr<LockRefPtr<FutexQueue>> {
            auto it = queues.find(futex_key);
            if (it != queues.end())
                return it->value;
            if (!create_if_not_found)
                return nullptr;
            *did_create = true;
            auto futex_queue = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) FutexQueue));
            auto result = TRY(queues.try_set(futex_key, futex_queue));
            VERIFY(result == AK::HashSetResult::InsertedNewEntry);
            return futex_queue;
        });
    };

    auto remove_futex_queue = [&](GlobalFutexKey futex_key) {
        return s_global_futex_queues->with([&](auto& queues) {
            auto it = queues.find(futex_key);
            if (it == queues.end())
                return;
            if (it->value->try_remove())
                queues.remove(it);
        });
    };

    auto do_wake = [&](FlatPtr user_address, u32 count, Optional<u32> const& bitmask) -> ErrorOr<int> {
        if (count == 0)
            return 0;
        auto futex_key = TRY(get_futex_key(user_address, shared));
        auto futex_queue = TRY(find_futex_queue(futex_key, false));
        if (!futex_queue)
            return 0;
        bool is_empty;
        u32 woke_count = futex_queue->wake_n(count, bitmask, is_empty);
        if (is_empty) {
            // If there are no more waiters, we want to get rid of the futex!
            remove_futex_queue(futex_key);
        }
        return (int)woke_count;
    };

    auto user_address = FlatPtr(params.userspace_address);
    auto user_address2 = FlatPtr(params.userspace_address2);

    auto do_wait = [&](u32 bitset) -> ErrorOr<FlatPtr> {
        bool did_create;
        LockRefPtr<FutexQueue> futex_queue;
        auto futex_key = TRY(get_futex_key(user_address, shared));
        do {
            auto user_value = user_atomic_load_relaxed(params.userspace_address);
            if (!user_value.has_value())
                return EFAULT;
            if (user_value.value() != params.val) {
                dbgln_if(FUTEX_DEBUG, "futex wait: EAGAIN. user value: {:p} @ {:p} != val: {}", user_value.value(), params.userspace_address, params.val);
                return EAGAIN;
            }
            atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);

            did_create = false;
            futex_queue = TRY(find_futex_queue(futex_key, true, &did_create));
            VERIFY(futex_queue);
            // We need to try again if we didn't create this queue and the existing queue
            // was removed before we were able to queue an imminent wait.
        } while (!did_create && !futex_queue->queue_imminent_wait());

        // We must not hold the lock before blocking. But we have a reference
        // to the FutexQueue so that we can keep it alive.

        Thread::BlockResult block_result = futex_queue->wait_on(timeout, bitset);

        if (futex_queue->is_empty_and_no_imminent_waits()) {
            // If there are no more waiters, we want to get rid of the futex!
            remove_futex_queue(futex_key);
        }
        if (block_result == Thread::BlockResult::InterruptedByTimeout) {
            return ETIMEDOUT;
        }
        return 0;
    };

    auto do_requeue = [&](Optional<u32> val3) -> ErrorOr<FlatPtr> {
        auto user_value = user_atomic_load_relaxed(params.userspace_address);
        if (!user_value.has_value())
            return EFAULT;
        if (val3.has_value() && val3.value() != user_value.value())
            return EAGAIN;
        atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);

        auto futex_key = TRY(get_futex_key(user_address, shared));
        auto futex_queue = TRY(find_futex_queue(futex_key, false));
        if (!futex_queue)
            return 0;

        LockRefPtr<FutexQueue> target_futex_queue;
        bool is_empty = false;
        bool is_target_empty = false;
        auto futex_key2 = TRY(get_futex_key(user_address2, shared));
        auto woken_or_requeued = TRY(futex_queue->wake_n_requeue(
            params.val, [&]() -> ErrorOr<FutexQueue*> {
                // NOTE: futex_queue's lock is being held while this callback is called
                // The reason we're doing this in a callback is that we don't want to always
                // create a target queue, only if we actually have anything to move to it!
                target_futex_queue = TRY(find_futex_queue(futex_key2, true));
                return target_futex_queue.ptr();
            },
            params.val2, is_empty, is_target_empty));
        if (is_empty)
            remove_futex_queue(futex_key);
        if (is_target_empty && target_futex_queue)
            remove_futex_queue(futex_key2);
        return woken_or_requeued;
    };

    switch (cmd) {
    case FUTEX_WAIT:
        return do_wait(0);

    case FUTEX_WAKE:
        return TRY(do_wake(user_address, params.val, {}));

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
            return EINVAL;
        }
        if (!oldval.has_value())
            return EFAULT;
        atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);
        auto result = TRY(do_wake(user_address, params.val, {}));
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
                return EINVAL;
            }
            if (compare_result)
                result += TRY(do_wake(user_address2, params.val2, {}));
        }
        return result;
    }

    case FUTEX_REQUEUE:
        return do_requeue({});

    case FUTEX_CMP_REQUEUE:
        return do_requeue(params.val3);

    case FUTEX_WAIT_BITSET:
        VERIFY(params.val3 != FUTEX_BITSET_MATCH_ANY); // we should have turned it into FUTEX_WAIT
        if (params.val3 == 0)
            return EINVAL;
        return do_wait(params.val3);

    case FUTEX_WAKE_BITSET:
        VERIFY(params.val3 != FUTEX_BITSET_MATCH_ANY); // we should have turned it into FUTEX_WAKE
        if (params.val3 == 0)
            return EINVAL;
        return TRY(do_wake(user_address, params.val, params.val3));
    }
    return ENOSYS;
}

}
