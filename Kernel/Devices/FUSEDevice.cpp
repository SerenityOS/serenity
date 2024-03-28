/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/FUSEDevice.h>
#include <Kernel/FileSystem/FUSE/Definitions.h>
#include <Kernel/FileSystem/FUSE/FUSEConnection.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<FUSEDevice> FUSEDevice::must_create()
{
    // FIXME: Find a way to propagate errors
    auto fuse_device_or_error = DeviceManagement::try_create_device<FUSEDevice>();
    VERIFY(!fuse_device_or_error.is_error());
    return fuse_device_or_error.release_value();
}

UNMAP_AFTER_INIT FUSEDevice::FUSEDevice()
    : CharacterDevice(10, 229)
{
}

UNMAP_AFTER_INIT FUSEDevice::~FUSEDevice() = default;

FUSEInstance const* FUSEDevice::get_instance_from_fd(OpenFileDescription const& fd) const
{
    for (auto const& instance : m_instances) {
        if (instance.fd == &fd)
            return &instance;
    }
    return nullptr;
}

FUSEInstance* FUSEDevice::get_mutable_instance_from_fd(OpenFileDescription const& fd)
{
    for (auto& instance : m_instances) {
        if (instance.fd == &fd)
            return &instance;
    }
    return nullptr;
}

void FUSEDevice::remove_instance(OpenFileDescription const& fd)
{
    size_t target_index = 0;
    bool found = false;
    for (size_t i = 0; i < m_instances.size(); ++i) {
        if (m_instances[i].fd == &fd) {
            target_index = i;
            found = true;
            break;
        }
    }
    VERIFY(found);
    m_instances.remove(target_index);
}

ErrorOr<void> FUSEDevice::initialize_instance(OpenFileDescription const& fd)
{
    for (auto const& instance : m_instances)
        VERIFY(instance.fd != &fd);

    TRY(m_instances.try_append({
        &fd,
        TRY(KBuffer::try_create_with_size("FUSE: Pending request buffer"sv, 0x21000)),
        TRY(KBuffer::try_create_with_size("FUSE: Response buffer"sv, 0x21000)),
    }));

    return {};
}

bool FUSEDevice::can_read(OpenFileDescription const& fd, u64) const
{
    auto const* instance = get_instance_from_fd(fd);
    if (!instance) {
        VERIFY(m_instances.is_empty());
        return false;
    }
    return instance->buffer_ready || instance->drop_request;
}

bool FUSEDevice::can_write(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> FUSEDevice::read(OpenFileDescription& fd, u64, UserOrKernelBuffer& buffer, size_t size)
{
    auto* instance = get_mutable_instance_from_fd(fd);
    if (!instance)
        return Error::from_errno(ENODEV);

    if (instance->drop_request) {
        instance->drop_request = false;

        remove_instance(fd);
        return Error::from_errno(ENODEV);
    }

    if (size < 0x21000)
        return Error::from_errno(EIO);

    if (!instance->buffer_ready)
        return Error::from_errno(ENOENT);

    TRY(buffer.write(instance->pending_request->bytes()));
    instance->buffer_ready = false;
    return instance->pending_request->size();
}

ErrorOr<size_t> FUSEDevice::write(OpenFileDescription& fd, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    auto* instance = get_mutable_instance_from_fd(fd);
    if (!instance)
        return Error::from_errno(ENODEV);

    if (instance->expecting_header) {
        memset(instance->response->data(), 0, instance->response->size());

        fuse_out_header header;
        TRY(buffer.read(&header, 0, sizeof(fuse_out_header)));

        dbgln_if(FUSE_DEBUG, "header: length: {}, error: {}, unique: {}", header.len, header.error, header.unique);
        memcpy(instance->response->data(), &header, sizeof(fuse_out_header));

        if (header.len > sizeof(fuse_out_header))
            instance->expecting_header = false;
        else
            instance->response_ready = true;
    } else {
        fuse_out_header* existing_header = bit_cast<fuse_out_header*>(instance->response->data());

        instance->expecting_header = true;
        if (existing_header->len > instance->response->size())
            return Error::from_errno(EINVAL);

        instance->response_ready = true;
        u64 length = existing_header->len - sizeof(fuse_out_header);
        dbgln_if(FUSE_DEBUG, "request: response length: {}", length);
        TRY(buffer.read(instance->response->data() + sizeof(fuse_out_header), 0, length));
    }

    return size;
}

ErrorOr<NonnullOwnPtr<KBuffer>> FUSEDevice::send_request_and_wait_for_a_reply(OpenFileDescription const& description, Bytes bytes)
{
    MutexLocker locker(m_lock);
    auto& instance = *get_mutable_instance_from_fd(description);

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
}

void FUSEDevice::shutdown_for_description(OpenFileDescription const& description)
{
    MutexLocker locker(m_lock);
    get_mutable_instance_from_fd(description)->drop_request = true;
    evaluate_block_conditions();
}

}
