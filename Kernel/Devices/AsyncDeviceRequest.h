/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/VM/ProcessPagingScope.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class Device;

extern WorkQueue* g_io_work;

class AsyncDeviceRequest : public RefCounted<AsyncDeviceRequest> {
    AK_MAKE_NONCOPYABLE(AsyncDeviceRequest);
    AK_MAKE_NONMOVABLE(AsyncDeviceRequest);

public:
    enum RequestResult {
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

    virtual const char* name() const = 0;
    virtual void start() = 0;

    void add_sub_request(NonnullRefPtr<AsyncDeviceRequest>);

    [[nodiscard]] RequestWaitResult wait(Time* = nullptr);

    void do_start(ScopedSpinLock<SpinLock<u8>>&& requests_lock)
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
    [[nodiscard]] bool write_to_buffer(UserOrKernelBuffer& buffer, Args... args)
    {
        if (in_target_context(buffer))
            return buffer.write(forward<Args>(args)...);
        ProcessPagingScope paging_scope(m_process);
        return buffer.write(forward<Args>(args)...);
    }

    template<size_t BUFFER_BYTES, typename... Args>
    [[nodiscard]] bool write_to_buffer_buffered(UserOrKernelBuffer& buffer, Args... args)
    {
        if (in_target_context(buffer))
            return buffer.write_buffered<BUFFER_BYTES>(forward<Args>(args)...);
        ProcessPagingScope paging_scope(m_process);
        return buffer.write_buffered<BUFFER_BYTES>(forward<Args>(args)...);
    }

    template<typename... Args>
    [[nodiscard]] bool read_from_buffer(const UserOrKernelBuffer& buffer, Args... args)
    {
        if (in_target_context(buffer))
            return buffer.read(forward<Args>(args)...);
        ProcessPagingScope paging_scope(m_process);
        return buffer.read(forward<Args>(args)...);
    }

    template<size_t BUFFER_BYTES, typename... Args>
    [[nodiscard]] bool read_from_buffer_buffered(const UserOrKernelBuffer& buffer, Args... args)
    {
        if (in_target_context(buffer))
            return buffer.read_buffered<BUFFER_BYTES>(forward<Args>(args)...);
        ProcessPagingScope paging_scope(m_process);
        return buffer.read_buffered<BUFFER_BYTES>(forward<Args>(args)...);
    }

protected:
    AsyncDeviceRequest(Device&);

    RequestResult get_request_result() const;

private:
    void sub_request_finished(AsyncDeviceRequest&);
    void request_finished();

    bool in_target_context(const UserOrKernelBuffer& buffer) const
    {
        if (buffer.is_kernel_buffer())
            return true;
        return m_process == Process::current();
    }

    static bool is_completed_result(RequestResult result)
    {
        return result > Started;
    }

    Device& m_device;

    AsyncDeviceRequest* m_parent_request { nullptr };
    RequestResult m_result { Pending };
    NonnullRefPtrVector<AsyncDeviceRequest> m_sub_requests_pending;
    NonnullRefPtrVector<AsyncDeviceRequest> m_sub_requests_complete;
    WaitQueue m_queue;
    NonnullRefPtr<Process> m_process;
    void* m_private { nullptr };
    mutable SpinLock<u8> m_lock;
};

}
