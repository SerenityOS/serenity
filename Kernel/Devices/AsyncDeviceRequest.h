/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class Device;

extern WorkQueue* g_io_work;

class AsyncDeviceRequest : public RefCounted<AsyncDeviceRequest> {
    AK_MAKE_NONCOPYABLE(AsyncDeviceRequest);
    AK_MAKE_NONMOVABLE(AsyncDeviceRequest);

public:
    enum [[nodiscard]] RequestResult {
        Pending = 0,
        Started,
        Success,
        Failure,
        MemoryFault,
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

    void add_sub_request(NonnullRefPtr<AsyncDeviceRequest>);

    [[nodiscard]] RequestWaitResult wait(Time* = nullptr);

    void do_start(SpinlockLocker<Spinlock>&& requests_lock)
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
        if (in_target_context(buffer))
            return buffer.write(forward<Args>(args)...);
        ScopedAddressSpaceSwitcher switcher(m_process);
        return buffer.write(forward<Args>(args)...);
    }

    template<size_t BUFFER_BYTES, typename... Args>
    ErrorOr<size_t> write_to_buffer_buffered(UserOrKernelBuffer& buffer, Args... args)
    {
        if (in_target_context(buffer))
            return buffer.write_buffered<BUFFER_BYTES>(forward<Args>(args)...);
        ScopedAddressSpaceSwitcher switcher(m_process);
        return buffer.write_buffered<BUFFER_BYTES>(forward<Args>(args)...);
    }

    template<typename... Args>
    ErrorOr<void> read_from_buffer(const UserOrKernelBuffer& buffer, Args... args)
    {
        if (in_target_context(buffer))
            return buffer.read(forward<Args>(args)...);
        ScopedAddressSpaceSwitcher switcher(m_process);
        return buffer.read(forward<Args>(args)...);
    }

    template<size_t BUFFER_BYTES, typename... Args>
    ErrorOr<size_t> read_from_buffer_buffered(const UserOrKernelBuffer& buffer, Args... args)
    {
        if (in_target_context(buffer))
            return buffer.read_buffered<BUFFER_BYTES>(forward<Args>(args)...);
        ScopedAddressSpaceSwitcher switcher(m_process);
        return buffer.read_buffered<BUFFER_BYTES>(forward<Args>(args)...);
    }

protected:
    AsyncDeviceRequest(Device&);

    RequestResult get_request_result() const;

private:
    void sub_request_finished(AsyncDeviceRequest&);
    void request_finished();

    [[nodiscard]] bool in_target_context(const UserOrKernelBuffer& buffer) const
    {
        if (buffer.is_kernel_buffer())
            return true;
        return m_process == &Process::current();
    }

    [[nodiscard]] static bool is_completed_result(RequestResult result)
    {
        return result > Started;
    }

    Device& m_device;

    AsyncDeviceRequest* m_parent_request { nullptr };
    RequestResult m_result { Pending };
    IntrusiveListNode<AsyncDeviceRequest, RefPtr<AsyncDeviceRequest>> m_list_node;

    using AsyncDeviceSubRequestList = IntrusiveList<&AsyncDeviceRequest::m_list_node>;

    AsyncDeviceSubRequestList m_sub_requests_pending;
    AsyncDeviceSubRequestList m_sub_requests_complete;
    WaitQueue m_queue;
    NonnullRefPtr<Process> m_process;
    void* m_private { nullptr };
    mutable Spinlock m_lock;
};

}
