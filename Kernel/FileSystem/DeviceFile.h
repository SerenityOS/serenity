/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

/* 
 * DeviceFile is the final class of everything that is related to a Device file
 * in the Kernel. It weakly holds a Device object, which means that at any time
 * this object might vanish, resulting in IO errors if that happens when trying
 * to perform any subsequent IO operations.
*/
#include <AK/RefPtr.h>
#include <Kernel/API/KResult.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/VirtualRange.h>

namespace Kernel {

class DeviceFile final : public File {
public:
    virtual ~DeviceFile() override {};

    static KResultOr<NonnullRefPtr<DeviceFile>> try_create(const Device&);

    virtual FileBlockerSet& blocker_set() override;

    virtual void did_seek(OpenFileDescription&, off_t) override;
    virtual String absolute_path(const OpenFileDescription&) const override;
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const OpenFileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const OpenFileDescription&, size_t) const override;
    virtual KResult ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual KResultOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;
    virtual KResultOr<NonnullRefPtr<OpenFileDescription>> open(int options) override;
    virtual StringView class_name() const override;
    virtual KResult close() override;
    virtual bool is_device() const override { return true; }
    virtual bool is_seekable() const;
    virtual bool is_tty() const;
    virtual bool is_block_device() const override;
    virtual bool is_character_device() const override;
    virtual bool is_master_pty() const override;

    virtual const Device* as_device() const override;
    virtual Device* as_device() override;
    virtual const TTY* as_tty() const override;
    virtual TTY* as_tty() override;
    virtual const MasterPTY* as_master_pty() const override;
    virtual MasterPTY* as_master_pty() override;

private:
    virtual void do_evaluate_block_conditions() override
    {
        VERIFY(!Processor::current_in_irq());
        File::do_evaluate_block_conditions();
        auto device = m_device.strong_ref();
        if (!device)
            return;
        device->do_evaluate_device_block_conditions({});
    }

    // Note: We cache all of this metadata to avoid taking a RefPtr each time
    // we want to figure out something about this File.
    const bool m_is_block_device;
    const bool m_is_character_device;
    const bool m_is_master_pty;
    const bool m_is_tty;
    const bool m_seekable;
    const String m_class_name;
    const String m_absolute_path;
    explicit DeviceFile(const Device&);
    WeakPtr<Device> m_device;
};

}
