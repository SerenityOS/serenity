/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel::USB {

class SysFSUSBDeviceInformation : public SysFSComponent {
    friend class SysFSUSBBusDirectory;

public:
    virtual ~SysFSUSBDeviceInformation() override;

    static NonnullRefPtr<SysFSUSBDeviceInformation> create(USB::Device&);
    virtual StringView name() const override { return m_device_name->view(); }

    RefPtr<USB::Device> device() const { return m_device; }

protected:
    SysFSUSBDeviceInformation(NonnullOwnPtr<KString> device_name, USB::Device& device);

    virtual ErrorOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;

    IntrusiveListNode<SysFSUSBDeviceInformation, RefPtr<SysFSUSBDeviceInformation>> m_list_node;

    NonnullRefPtr<USB::Device> m_device;

private:
    ErrorOr<void> try_generate(KBufferBuilder&);
    virtual ErrorOr<void> refresh_data(OpenFileDescription& description) const override;
    mutable Mutex m_lock { "SysFSUSBDeviceInformation" };
    NonnullOwnPtr<KString> m_device_name;
};

class SysFSUSBBusDirectory final : public SysFSDirectory {
public:
    static void initialize();
    static SysFSUSBBusDirectory& the();

    virtual StringView name() const override { return "usb"sv; }

    void plug(USB::Device&);
    void unplug(USB::Device&);

    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

private:
    explicit SysFSUSBBusDirectory(SysFSBusDirectory&);

    RefPtr<SysFSUSBDeviceInformation> device_node_for(USB::Device& device);

    IntrusiveList<&SysFSUSBDeviceInformation::m_list_node> m_device_nodes;
    mutable Spinlock m_lock;
};

}
