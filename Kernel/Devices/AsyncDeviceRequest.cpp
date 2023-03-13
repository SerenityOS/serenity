/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/AsyncDeviceRequest.h>
#include <Kernel/Devices/Device.h>

namespace Kernel {

AsyncDeviceRequest::AsyncDeviceRequest(Device& device)
    : m_device(device)
    , m_process(Process::current())
{
}

AsyncDeviceRequest::~AsyncDeviceRequest()
{
    {
        SpinlockLocker lock(m_lock);
        VERIFY(is_completed_result(m_result));
        VERIFY(m_sub_requests_pending.is_empty());
    }

    // We should not need any locking here anymore. The destructor should
    // only be called until either wait() or cancel() (once implemented) returned.
    // At that point no sub-request should be adding more requests and all
    // sub-requests should be completed (either succeeded, failed, or cancelled).
    // Which means there should be no more pending sub-requests and the
    // entire AsyncDeviceRequest hierarchy should be immutable.
    while (!m_sub_requests_complete.is_empty()) {
        // Note: sub_request is ref-counted, and we use this specific pattern
        // to allow make sure the refcount is dropped properly.
        auto sub_request = m_sub_requests_complete.take_first();
        VERIFY(is_completed_result(sub_request->m_result)); // Shouldn't need any locking anymore
        VERIFY(sub_request->m_parent_request == this);
        sub_request->m_parent_request = nullptr;
    }
}

void AsyncDeviceRequest::request_finished()
{
    if (m_parent_request)
        m_parent_request->sub_request_finished(*this);

    // Trigger processing the next request
    m_device.process_next_queued_request({}, *this);

    // Wake anyone who may be waiting
    m_queue.wake_all();
}

auto AsyncDeviceRequest::wait(Duration* timeout) -> RequestWaitResult
{
    VERIFY(!m_parent_request);
    auto request_result = get_request_result();
    if (is_completed_result(request_result))
        return { request_result, Thread::BlockResult::NotBlocked };
    auto wait_result = m_queue.wait_on(Thread::BlockTimeout(false, timeout), name());
    return { get_request_result(), wait_result };
}

auto AsyncDeviceRequest::get_request_result() const -> RequestResult
{
    SpinlockLocker lock(m_lock);
    return m_result;
}

void AsyncDeviceRequest::add_sub_request(NonnullLockRefPtr<AsyncDeviceRequest> sub_request)
{
    // Sub-requests cannot be for the same device
    VERIFY(&m_device != &sub_request->m_device);
    VERIFY(sub_request->m_parent_request == nullptr);
    sub_request->m_parent_request = this;

    SpinlockLocker lock(m_lock);
    VERIFY(!is_completed_result(m_result));
    m_sub_requests_pending.append(sub_request);
    if (m_result == Started)
        sub_request->do_start(move(lock));
}

void AsyncDeviceRequest::sub_request_finished(AsyncDeviceRequest& sub_request)
{
    bool all_completed;
    {
        SpinlockLocker lock(m_lock);
        VERIFY(m_result == Started);

        if (m_sub_requests_pending.contains(sub_request)) {
            // Note: append handles removing from any previous intrusive list internally.
            m_sub_requests_complete.append(sub_request);
        }

        all_completed = m_sub_requests_pending.is_empty();
        if (all_completed) {
            // Aggregate any errors
            bool any_failures = false;
            bool any_memory_faults = false;
            for (auto& com_sub_request : m_sub_requests_complete) {
                auto sub_result = com_sub_request.get_request_result();
                VERIFY(is_completed_result(sub_result));
                switch (sub_result) {
                case Failure:
                    any_failures = true;
                    break;
                case MemoryFault:
                    any_memory_faults = true;
                    break;
                default:
                    break;
                }
                if (any_failures && any_memory_faults)
                    break; // Stop checking if all error conditions were found
            }
            if (any_failures)
                m_result = Failure;
            else if (any_memory_faults)
                m_result = MemoryFault;
            else
                m_result = Success;
        }
    }
    if (all_completed)
        request_finished();
}

void AsyncDeviceRequest::complete(RequestResult result)
{
    VERIFY(result == Success || result == Failure || result == MemoryFault);
    ScopedCritical critical;
    {
        SpinlockLocker lock(m_lock);
        VERIFY(m_result == Started);
        m_result = result;
    }
    if (Processor::current_in_irq()) {
        ref(); // Make sure we don't get freed
        Processor::deferred_call_queue([this]() {
            request_finished();
            unref();
        });
    } else {
        request_finished();
    }
}

}
