/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Loop/LoopDevice.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

static Singleton<SpinlockProtected<LoopDevice::List, LockRank::None>> s_all_instances;
static Atomic<u64> s_loop_device_id { 0 };

SpinlockProtected<LoopDevice::List, LockRank::None>& LoopDevice::all_instances()
{
    return s_all_instances;
}

void LoopDevice::remove(Badge<DeviceControlDevice>)
{
    LoopDevice::all_instances().with([&](auto&) {
        m_list_node.remove();
    });
}

bool LoopDevice::unref() const
{
    bool did_hit_zero = LoopDevice::all_instances().with([&](auto&) {
        if (deref_base())
            return false;
        const_cast<LoopDevice&>(*this).revoke_weak_ptrs();
        return true;
    });
    if (did_hit_zero) {
        const_cast<LoopDevice&>(*this).will_be_destroyed();
        delete this;
    }
    return did_hit_zero;
}

ErrorOr<NonnullRefPtr<LoopDevice>> LoopDevice::create_with_file_description(OpenFileDescription& description)
{
    auto custody = description.custody();
    if (!custody)
        return Error::from_errno(EINVAL);

    // NOTE: We only support regular inode files, because anything else
    // just doesn't make sense (could be non-seekable files or char devices)
    if (!custody->inode().metadata().is_regular_file())
        return Error::from_errno(ENOTSUP);

    // NOTE: We could technically allow the user to create a loop device from a file
    // on SysFS, ProcFS, etc, but the added value from allowing this is non-existent
    // because there's simply no good reason to ever do this kind of operation.
    //
    // If you need more justification, some filesystems (like ProcFS, SysFS, etc) don't
    // support keeping Inode objects and instead keep re-creating them - this has serious
    // consequences on the integrity of loop devices, as we rely on the backing Inode to
    // be consistent while the LoopDevice is alive.
    if (!custody->inode().fs().supports_backing_loop_devices())
        return Error::from_errno(ENOTSUP);

    return TRY(LoopDevice::all_instances().with([custody](auto& all_instances_list) -> ErrorOr<NonnullRefPtr<LoopDevice>> {
        NonnullRefPtr<LoopDevice> device = TRY(Device::try_create_device<LoopDevice>(*custody, s_loop_device_id.fetch_add(1)));
        all_instances_list.append(*device);
        return device;
    }));
}

void LoopDevice::start_request(AsyncBlockDeviceRequest& request)
{
    auto work_item_creation_result = g_io_work->try_queue([this, &request]() {
        if (request.request_type() == AsyncBlockDeviceRequest::RequestType::Read) {
            auto result = m_backing_custody->inode().read_bytes(request.block_index() * request.block_size(), request.buffer_size(), request.buffer(), nullptr);
            if (result.is_error())
                request.complete(AsyncDeviceRequest::Failure);
            else
                request.complete(AsyncDeviceRequest::Success);
            return;
        } else if (request.request_type() == AsyncBlockDeviceRequest::RequestType::Write) {
            auto result = m_backing_custody->inode().write_bytes(request.block_index() * request.block_size(), request.buffer_size(), request.buffer(), nullptr);
            if (result.is_error())
                request.complete(AsyncDeviceRequest::Failure);
            else
                request.complete(AsyncDeviceRequest::Success);
            return;
        }
        VERIFY_NOT_REACHED();
    });
    if (work_item_creation_result.is_error())
        request.complete(AsyncDeviceRequest::OutOfMemory);
}

bool LoopDevice::can_read(OpenFileDescription const&, u64) const
{
    return true;
}

bool LoopDevice::can_write(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> LoopDevice::read(OpenFileDescription& description, u64 offset, UserOrKernelBuffer& buffer, size_t size)
{
    return m_backing_custody->inode().read_bytes(offset, size, buffer, &description);
}

ErrorOr<size_t> LoopDevice::write(OpenFileDescription& description, u64 offset, UserOrKernelBuffer const& buffer, size_t size)
{
    return m_backing_custody->inode().write_bytes(offset, size, buffer, &description);
}

// FIXME: Allow passing different block sizes to the constructor
LoopDevice::LoopDevice(NonnullRefPtr<Custody> backing_custody, unsigned index)
    : BlockDevice(MajorAllocation::BlockDeviceFamily::Loop, index, 512)
    , m_backing_custody(backing_custody)
    , m_index(index)
{
}

ErrorOr<void> LoopDevice::ioctl(OpenFileDescription&, unsigned, Userspace<void*>)
{
    return EINVAL;
}

}
