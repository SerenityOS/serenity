/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/KCOVInstance.h>

namespace Kernel {
class KCOVDevice final : public BlockDevice {
    friend class DeviceManagement;

public:
    static HashMap<ProcessID, KCOVInstance*>* proc_instance;
    static HashMap<ThreadID, KCOVInstance*>* thread_instance;

    static NonnullRefPtr<KCOVDevice> must_create();
    static void free_thread();
    static void free_process();

    // ^File
    ErrorOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;
    ErrorOr<NonnullRefPtr<OpenFileDescription>> open(int options) override;

protected:
    KCOVDevice();

    virtual StringView class_name() const override { return "KCOVDevice"sv; }

    virtual bool can_read(const OpenFileDescription&, u64) const override final { return true; }
    virtual bool can_write(const OpenFileDescription&, u64) const override final { return true; }
    virtual void start_request(AsyncBlockDeviceRequest& request) override final { request.complete(AsyncDeviceRequest::Failure); }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
};

}
