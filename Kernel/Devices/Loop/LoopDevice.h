/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/IntrusiveList.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/ListedRefCounted.h>

namespace Kernel {

class LoopDevice final : public BlockDevice {
    friend class Device;

public:
    virtual bool unref() const override;
    virtual ~LoopDevice() = default;

    void remove(Badge<DeviceControlDevice>);
    static ErrorOr<NonnullRefPtr<LoopDevice>> create_with_file_description(OpenFileDescription&);

    virtual StringView class_name() const override { return "LoopDevice"sv; }

    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;

    virtual bool is_loop_device() const override { return true; }

    unsigned index() const { return m_index; }

    Inode const& inode() const { return m_backing_custody->inode(); }
    Inode& inode() { return m_backing_custody->inode(); }

    Custody const& custody() const { return *m_backing_custody; }
    Custody& custody() { return *m_backing_custody; }

private:
    virtual void start_request(AsyncBlockDeviceRequest&) override;

    LoopDevice(NonnullRefPtr<Custody>, unsigned index);

    NonnullRefPtr<Custody> const m_backing_custody;
    unsigned const m_index { 0 };

    mutable IntrusiveListNode<LoopDevice, NonnullRefPtr<LoopDevice>> m_list_node;

public:
    using List = IntrusiveList<&LoopDevice::m_list_node>;

    static SpinlockProtected<LoopDevice::List, LockRank::None>& all_instances();
};

}
