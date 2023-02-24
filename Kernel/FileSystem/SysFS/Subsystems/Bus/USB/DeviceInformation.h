/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class SysFSUSBDeviceInformation : public SysFSComponent {
    friend class SysFSUSBBusDirectory;

public:
    virtual ~SysFSUSBDeviceInformation() override;

    static ErrorOr<NonnullRefPtr<SysFSUSBDeviceInformation>> create(USB::Device&);
    virtual StringView name() const override { return m_device_name->view(); }

protected:
    SysFSUSBDeviceInformation(NonnullOwnPtr<KString> device_name, USB::Device& device);

    virtual ErrorOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;

    NonnullRefPtr<USB::Device> m_device;

private:
    ErrorOr<void> try_generate(KBufferBuilder&);
    virtual ErrorOr<void> refresh_data(OpenFileDescription& description) const override;
    mutable Mutex m_lock { "SysFSUSBDeviceInformation"sv };
    NonnullOwnPtr<KString> m_device_name;
};

}
