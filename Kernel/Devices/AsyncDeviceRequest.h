/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

class Device;

extern WorkQueue* g_io_work;

class AsyncDeviceRequest : public AtomicRefCounted<AsyncDeviceRequest> {
    AK_MAKE_NONCOPYABLE(AsyncDeviceRequest);
    AK_MAKE_NONMOVABLE(AsyncDeviceRequest);

public:
    enum [[nodiscard]] RequestResult {
        Pending = 0,
        Started,
        Success,
        Failure,
        MemoryFault,
        OutOfMemory,
        Cancelled
    };

    class RequestWaitResult {
        friend class AsyncDeviceRequest;

    public:
        RequestResult request_result() const { return m_request_result; }
        Thread::BlockResult wait_result() const { return m_wait_result; }

    private:
        RequestWaitResult(RequestResult request_result, Thread::BlockResult wait_result)
            : m_request_result(request_result)
            , m_wait_result(wait_result)
        {
        }

        RequestResult m_request_result;
        Thread::BlockResult m_wait_result;
    };

    virtual ~AsyncDeviceRequest();

    virtual StringView name() const = 0;
    virtual void start() = 0;

    void add_sub_request(NonnullLockRefPtr<AsyncDeviceRequest>);

    [[nodiscard]] RequestWaitResult wait(Duration* = nullptr);

    void do_start(SpinlockLocker<Spinlock<LockRank::None>>&& requests_lock)
    {
        if (is_completed_result(m_result))
            return;
        m_result = Started;
        requests_lock.unlock();

        start();
    }

    void complete(RequestResult result);

    void set_private(void* priv)
    {
        VERIFY(!m_private || !priv);
        m_private = priv;
    }
    void* get_private() const { return m_private; }

    template<typename... Args>
    ErrorOr<void> write_to_buffer(UserOrKernelBuffer& buffer, Args... args)
    {
        auto process = m_process.strong_ref();
        if (!process)
            return Error::from_errno(ESRCH);
        if (in_target_context(*process, buffer))
            return buffer.write(forward<Args>(args)...);
        ScopedAddressSpaceSwitcher switcher(*process);
        return buffer.write(forward<Args>(args)...);
    }

    template<size_t BUFFER_BYTES, typename... Args>
    ErrorOr<size_t> write_to_buffer_buffered(UserOrKernelBuffer& buffer, Args... args)
    {
        auto process = m_process.strong_ref();
        if (!process)
            return Error::from_errno(ESRCH);
        if (in_target_context(*process, buffer))
            return buffer.write_buffered<BUFFER_BYTES>(forward<Args>(args)...);
        ScopedAddressSpaceSwitcher switcher(*process);
        return buffer.write_buffered<BUFFER_BYTES>(forward<Args>(args)...);
    }

    template<typename... Args>
    ErrorOr<void> read_from_buffer(UserOrKernelBuffer const& buffer, Args... args)
    {
        auto process = m_process.strong_ref();
        if (!process)
            return Error::from_errno(ESRCH);
        if (in_target_context(*process, buffer))
            return buffer.read(forward<Args>(args)...);
        ScopedAddressSpaceSwitcher switcher(*process);
        return buffer.read(forward<Args>(args)...);
    }

    template<size_t BUFFER_BYTES, typename... Args>
    ErrorOr<size_t> read_from_buffer_buffered(UserOrKernelBuffer const& buffer, Args... args)
    {
        auto process = m_process.strong_ref();
        if (!process)
            return Error::from_errno(ESRCH);
        if (in_target_context(*process, buffer))
            return buffer.read_buffered<BUFFER_BYTES>(forward<Args>(args)...);
        ScopedAddressSpaceSwitcher switcher(*process);
        return buffer.read_buffered<BUFFER_BYTES>(forward<Args>(args)...);
    }

protected:
    AsyncDeviceRequest(Device&);

    RequestResult get_request_result() const;

private:
    void sub_request_finished(AsyncDeviceRequest&);
    void request_finished();

    [[nodiscard]] bool in_target_context(Process& process, UserOrKernelBuffer const& buffer) const
    {
        if (buffer.is_kernel_buffer())
            return true;
        return &process == &Process::current();
    }

    [[nodiscard]] static bool is_completed_result(RequestResult result)
    {
        return result > Started;
    }

    Device& m_device;

    AsyncDeviceRequest* m_parent_request { nullptr };
    RequestResult m_result { Pending };
    IntrusiveListNode<AsyncDeviceRequest, LockRefPtr<AsyncDeviceRequest>> m_list_node;

    using AsyncDeviceSubRequestList = IntrusiveList<&AsyncDeviceRequest::m_list_node>;

    AsyncDeviceSubRequestList m_sub_requests_pending;
    AsyncDeviceSubRequestList m_sub_requests_complete;
    WaitQueue m_queue;
    LockWeakPtr<Process> const m_process;
    void* m_private { nullptr };
    mutable Spinlock<LockRank::None> m_lock {};
};

}
