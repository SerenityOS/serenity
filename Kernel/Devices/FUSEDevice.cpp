/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/FUSEDevice.h>
#include <Kernel/FileSystem/FUSE/Definitions.h>
#include <Kernel/FileSystem/FUSE/FUSEConnection.h>
#include <Kernel/Time/TimerQueue.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<FUSEDevice> FUSEDevice::must_create()
{
    return MUST(Device::try_create_device<FUSEDevice>());
}

UNMAP_AFTER_INIT FUSEDevice::FUSEDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Generic, 229)
{
}

UNMAP_AFTER_INIT FUSEDevice::~FUSEDevice() = default;

ErrorOr<void> FUSEDevice::initialize_instance(OpenFileDescription const& description)
{
    return m_instances.with([&](auto& instances) -> ErrorOr<void> {
        VERIFY(!instances.active_instances.contains(&description));
        TRY(instances.active_instances.try_set(&description, {}));
        return {};
    });
}

bool FUSEDevice::can_read(OpenFileDescription const& description, u64) const
{
    return m_instances.with([&](auto& instances) {
        auto iterator = instances.closing_instances.find(&description);
        if (iterator != instances.closing_instances.end())
            return true;

        auto instance_iterator = instances.active_instances.find(&description);
        if (instance_iterator == instances.active_instances.end()) {
            VERIFY(instances.active_instances.is_empty());
            return false;
        }

        auto const& requests_for_instance = (*instance_iterator).value;
        for (auto const& request : requests_for_instance.in_reverse()) {
            if (request.buffer_ready)
                return true;
        }
        return false;
    });
}

bool FUSEDevice::can_write(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> FUSEDevice::read(OpenFileDescription& description, u64, UserOrKernelBuffer& buffer, size_t size)
{
    return m_instances.with([&](auto& instances) -> ErrorOr<size_t> {
        bool removed = instances.closing_instances.remove_first_matching([&](auto const* closing_description) { return closing_description == &description; });
        if (removed)
            return Error::from_errno(ENODEV);

        if (size < 0x21000)
            return Error::from_errno(EIO);

        auto instance_iterator = instances.active_instances.find(&description);
        if (instance_iterator == instances.active_instances.end())
            return Error::from_errno(ENODEV);

        auto& requests_for_instance = (*instance_iterator).value;

        for (auto& request : requests_for_instance.in_reverse()) {
            if (!request.buffer_ready)
                continue;

            TRY(buffer.write(request.pending_request->bytes()));
            request.buffer_ready = false;
            return request.pending_request->size();
        }

        return Error::from_errno(ENOENT);
    });
}

ErrorOr<size_t> FUSEDevice::write(OpenFileDescription& description, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    return m_instances.with([&](auto& instances) -> ErrorOr<size_t> {
        auto instance_iterator = instances.active_instances.find(&description);

        if (instance_iterator == instances.active_instances.end())
            return Error::from_errno(ENODEV);

        auto& requests_for_instance = (*instance_iterator).value;
        auto& instance = requests_for_instance.last();

        if (instance.expecting_header) {
            memset(instance.response->data(), 0, instance.response->size());

            fuse_out_header header;
            TRY(buffer.read(&header, 0, sizeof(fuse_out_header)));

            dbgln_if(FUSE_DEBUG, "header: length: {}, error: {}, unique: {}", header.len, header.error, header.unique);
            memcpy(instance.response->data(), &header, sizeof(fuse_out_header));

            if (header.len > sizeof(fuse_out_header)) {
                instance.expecting_header = false;
            } else {
                instance.response_ready = true;
                instance_queue.notify_all();
            }
        } else {
            fuse_out_header* existing_header = bit_cast<fuse_out_header*>(instance.response->data());

            instance.expecting_header = true;
            if (existing_header->len > instance.response->size())
                return Error::from_errno(EINVAL);

            instance.response_ready = true;
            instance_queue.notify_all();
            u64 length = existing_header->len - sizeof(fuse_out_header);
            dbgln_if(FUSE_DEBUG, "request: response length: {}", length);
            TRY(buffer.read(instance.response->data() + sizeof(fuse_out_header), 0, length));
        }

        return size;
    });
}

ErrorOr<void> FUSEDevice::queue_request(OpenFileDescription const& description, Bytes bytes, InstanceTracker& instances)
{
    VERIFY(bytes.size() >= sizeof(fuse_in_header) && bytes.size() <= 0x21000);

    auto instance_iterator = instances.active_instances.find(&description);
    VERIFY(instance_iterator != instances.active_instances.end());
    auto& requests_for_instance = (*instance_iterator).value;

    TRY(requests_for_instance.try_append({
        &description,
        TRY(KBuffer::try_create_with_size("FUSE: Pending request buffer"sv, 0x21000)),
        TRY(KBuffer::try_create_with_size("FUSE: Response buffer"sv, 0x21000)),
    }));

    auto& instance = requests_for_instance.last();

    memset(instance.pending_request->data(), 0, instance.pending_request->size());
    memcpy(instance.pending_request->data(), bytes.data(), bytes.size());
    instance.buffer_ready = true;

    return {};
}

ErrorOr<NonnullOwnPtr<KBuffer>> FUSEDevice::send_request_and_wait_for_a_reply(OpenFileDescription const& description, Bytes bytes)
{
    TRY(m_instances.with([&](auto& instances) -> ErrorOr<void> {
        return queue_request(description, bytes, instances);
    }));

    evaluate_block_conditions();

    u64 unique = bit_cast<fuse_in_header*>(bytes.data())->unique;

    ErrorOr<OwnPtr<KBuffer>> reply_or_error = nullptr;
    auto receive_reply = [&](auto& instances) -> bool {
        auto instance_iterator = instances.active_instances.find(&description);
        VERIFY(instance_iterator != instances.active_instances.end());
        auto& requests_for_instance = (*instance_iterator).value;

        for (size_t i = 0; i < requests_for_instance.size(); ++i) {
            if (bit_cast<fuse_in_header*>(requests_for_instance[i].pending_request->data())->unique != unique)
                continue;
            if (requests_for_instance[i].timed_out) {
                requests_for_instance.remove(i);
                reply_or_error = ETIMEDOUT;
                return true;
            }
            if (!requests_for_instance[i].response_ready)
                continue;
            reply_or_error = KBuffer::try_create_with_bytes("FUSEDevice: Response"sv, requests_for_instance[i].response->bytes());
            requests_for_instance.remove(i);
            return true;
        }

        return false;
    };

    auto set_timed_out = [&]() {
        m_instances.with([&](auto& instances) {
            auto instance_iterator = instances.active_instances.find(&description);
            VERIFY(instance_iterator != instances.active_instances.end());
            auto& requests_for_instance = (*instance_iterator).value;

            for (auto& request : requests_for_instance) {
                if (bit_cast<fuse_in_header*>(request.pending_request->data())->unique != unique)
                    continue;
                request.timed_out = true;
                break;
            }
        });
        instance_queue.notify_all();
    };

    auto timer = TRY(try_make_ref_counted<Timer>());

    auto deadline = TimeManagement::the().current_time(CLOCK_MONOTONIC_COARSE) + Duration::from_seconds(1);
    auto timer_was_added = TimerQueue::the().add_timer_without_id(*timer, CLOCK_MONOTONIC_COARSE, deadline, [&] {
        set_timed_out();
    });

    if (!timer_was_added)
        // Somehow, the deadline elapsed before the timer was queued.
        // Deal with that by just running the handler manually.
        set_timed_out();

    auto wait_status = instance_queue.wait_until(m_instances, receive_reply);
    TimerQueue::the().cancel_timer(*timer);

    if (wait_status.is_error()) {
        VERIFY(wait_status.error().code() == EINTR);
        return wait_status.release_error();
    }

    if (reply_or_error.is_error())
        return reply_or_error.release_error();

    auto reply = reply_or_error.release_value();
    return reply.release_nonnull();
}

void FUSEDevice::shutdown_for_description(OpenFileDescription const& description)
{
    m_instances.with([&](auto& instances) {
        VERIFY(instances.active_instances.remove(&description));
        instances.closing_instances.append(&description);
    });

    evaluate_block_conditions();
}

}
