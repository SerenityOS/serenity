/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/FUSEDevice.h>
#include <Kernel/FileSystem/FUSE/Definitions.h>
#include <Kernel/FileSystem/FUSE/FUSEConnection.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<FUSEDevice> FUSEDevice::must_create()
{
    return MUST(DeviceManagement::try_create_device<FUSEDevice>());
}

UNMAP_AFTER_INIT FUSEDevice::FUSEDevice()
    : CharacterDevice(10, 229)
{
}

UNMAP_AFTER_INIT FUSEDevice::~FUSEDevice() = default;

ErrorOr<void> FUSEDevice::initialize_instance(OpenFileDescription const& fd)
{
    return m_instances.with([&](auto& instances) -> ErrorOr<void> {
        for (auto const& instance : instances)
            VERIFY(instance.fd != &fd);

        TRY(instances.try_append({
            &fd,
            TRY(KBuffer::try_create_with_size("FUSE: Pending request buffer"sv, 0x21000)),
            TRY(KBuffer::try_create_with_size("FUSE: Response buffer"sv, 0x21000)),
        }));

        return {};
    });
}

bool FUSEDevice::can_read(OpenFileDescription const& fd, u64) const
{
    return m_instances.with([&](auto& instances) {
        Optional<size_t> instance_index = {};
        for (size_t i = 0; i < instances.size(); ++i) {
            if (instances[i].fd == &fd) {
                instance_index = i;
                break;
            }
        }
        if (!instance_index.has_value()) {
            VERIFY(instances.is_empty());
            return false;
        }

        auto& instance = instances[instance_index.value()];
        return instance.buffer_ready || instance.drop_request;
    });
}

bool FUSEDevice::can_write(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> FUSEDevice::read(OpenFileDescription& fd, u64, UserOrKernelBuffer& buffer, size_t size)
{
    return m_instances.with([&](auto& instances) -> ErrorOr<size_t> {
        Optional<size_t> instance_index = {};
        for (size_t i = 0; i < instances.size(); ++i) {
            if (instances[i].fd == &fd) {
                instance_index = i;
                break;
            }
        }

        if (!instance_index.has_value())
            return Error::from_errno(ENODEV);

        auto& instance = instances[instance_index.value()];

        if (instance.drop_request) {
            instance.drop_request = false;

            instances.remove(instance_index.value());
            return Error::from_errno(ENODEV);
        }

        if (size < 0x21000)
            return Error::from_errno(EIO);

        if (!instance.buffer_ready)
            return Error::from_errno(ENOENT);

        TRY(buffer.write(instance.pending_request->bytes()));
        instance.buffer_ready = false;
        return instance.pending_request->size();
    });
}

ErrorOr<size_t> FUSEDevice::write(OpenFileDescription& description, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    return m_instances.with([&](auto& instances) -> ErrorOr<size_t> {
        Optional<size_t> instance_index = {};
        for (size_t i = 0; i < instances.size(); ++i) {
            if (instances[i].fd == &description) {
                instance_index = i;
                break;
            }
        }

        if (!instance_index.has_value())
            return Error::from_errno(ENODEV);

        auto& instance = instances[instance_index.value()];

        if (instance.expecting_header) {
            memset(instance.response->data(), 0, instance.response->size());

            fuse_out_header header;
            TRY(buffer.read(&header, 0, sizeof(fuse_out_header)));

            dbgln_if(FUSE_DEBUG, "header: length: {}, error: {}, unique: {}", header.len, header.error, header.unique);
            memcpy(instance.response->data(), &header, sizeof(fuse_out_header));

            if (header.len > sizeof(fuse_out_header))
                instance.expecting_header = false;
            else
                instance.response_ready = true;
        } else {
            fuse_out_header* existing_header = bit_cast<fuse_out_header*>(instance.response->data());

            instance.expecting_header = true;
            if (existing_header->len > instance.response->size())
                return Error::from_errno(EINVAL);

            instance.response_ready = true;
            u64 length = existing_header->len - sizeof(fuse_out_header);
            dbgln_if(FUSE_DEBUG, "request: response length: {}", length);
            TRY(buffer.read(instance.response->data() + sizeof(fuse_out_header), 0, length));
        }

        return size;
    });
}

ErrorOr<NonnullOwnPtr<KBuffer>> FUSEDevice::send_request_and_wait_for_a_reply(OpenFileDescription const& description, Bytes bytes)
{
    return m_instances.with([&](auto& instances) -> ErrorOr<NonnullOwnPtr<KBuffer>> {
        Optional<size_t> instance_index = {};
        for (size_t i = 0; i < instances.size(); ++i) {
            if (instances[i].fd == &description) {
                instance_index = i;
                break;
            }
        }

        VERIFY(instance_index.has_value());
        auto& instance = instances[instance_index.value()];

        VERIFY(!instance.drop_request);
        VERIFY(bytes.size() <= 0x21000);

        memset(instance.pending_request->data(), 0, instance.pending_request->size());
        memcpy(instance.pending_request->data(), bytes.data(), bytes.size());
        instance.buffer_ready = true;
        evaluate_block_conditions();

        while (!instance.response_ready)
            (void)Thread::current()->sleep(Duration::from_microseconds(100));

        auto result = KBuffer::try_create_with_bytes("FUSEDevice: Response"sv, instance.response->bytes());
        instance.response_ready = false;

        return result;
    });
}

void FUSEDevice::shutdown_for_description(OpenFileDescription const& description)
{
    m_instances.with([&](auto& instances) {
        Optional<size_t> instance_index = {};
        for (size_t i = 0; i < instances.size(); ++i) {
            if (instances[i].fd == &description) {
                instance_index = i;
                break;
            }
        }
        VERIFY(instance_index.has_value());
        instances[instance_index.value()].drop_request = true;
    });

    evaluate_block_conditions();
}

}
