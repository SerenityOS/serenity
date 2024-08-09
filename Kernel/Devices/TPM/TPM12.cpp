/*
 * Copyright (c) 2024, Logkos <65683493+logkos@users.noreply.github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/TPM/Definitions.h>
#include <Kernel/Devices/TPM/TPM12.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<TPMDevice> TPMDevice::create()
{
    auto tpm_device_or_error = DeviceManagement::try_create_device<TPMDevice>();
    VERIFY(!tpm_device_or_error.is_error());
    auto tpm_device = tpm_device_or_error.release_value();
    (void)tpm_device->initialize();
    return *tpm_device;
}

// FIXME: We assume TPM is always present at the fixed address, which might not be true for all systems.
//        Ideally, we should discover the TPM using ACPI or platform-specific mechanisms and allow use of other localities
UNMAP_AFTER_INIT TPMDevice::TPMDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::TPM, 10)
    , m_registers(Memory::map_typed<TPM12MMIORegistersLocality0 volatile>(PhysicalAddress(0xFED40000), 0x5000, Memory::Region::Access::ReadWrite).release_value_but_fixme_should_propagate_errors())
{
}

UNMAP_AFTER_INIT TPMDevice::~TPMDevice() = default;

ErrorOr<void> TPMDevice::initialize()
{
    if (!kernel_command_line().is_TPM_enabled())
        return {};

    TPMPower TPM12Startup;
    TPM12Startup.header.tag = 0x00C1; // TPM_TAG_RQU_COMMAND
    TPM12Startup.header.commandSize = sizeof(TPM12Startup);
    TPM12Startup.header.commandCode = 0x00000099; // TPM_ORD_Startup
    TPM12Startup.actionType = 0x0001;             // TPM_ST_CLEAR

    TRY(transmit(reinterpret_cast<u8 const*>(&TPM12Startup)));
    m_initialized = true;
    dbgln("TPM: Initialized TPM 1.2 device.");
    return {};
}

ErrorOr<size_t> TPMDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t buffer_size)
{
    if (!m_initialized)
        return Error::from_errno(ENODEV);

    // FIXME: Burst count is not implemented yet
    Vector<u8> tpm_response;
    while (m_registers->TPM_STS_0.dataAvail == 0b1) {
        for (size_t i = 0; i < buffer_size; ++i) {
            u8 byte = m_registers->TPM_DATA_FIFO_0;
            tpm_response.append(byte);
        }
    }

    size_t bytes_to_copy = min(tpm_response.size(), buffer_size);
    TRY(buffer.write(tpm_response.data(), bytes_to_copy));

    return buffer_size;
}

ErrorOr<size_t> TPMDevice::write(OpenFileDescription&, u64, UserOrKernelBuffer const& buffer, size_t buffer_size)
{
    if (!m_initialized)
        return Error::from_errno(ENODEV);

    Vector<u8> response;
    response.ensure_capacity(buffer_size);

    TRY(buffer.read(response.data(), buffer_size));
    TRY(transmit(response.data()));
    return buffer_size;
}

ErrorOr<void> TPMDevice::ioctl(OpenFileDescription&, unsigned, Userspace<void*>)
{
    return Error::from_errno(ENOTTY);
}

bool TPMDevice::can_read(OpenFileDescription const&, u64) const
{
    return m_registers->TPM_STS_0.dataAvail == 0b1;
}

bool TPMDevice::can_write(OpenFileDescription const&, u64) const
{
    return m_registers->TPM_STS_0.commandReady == 0b1;
}

ErrorOr<void> TPMDevice::transmit(u8 const* buffer)
{
    if (!(m_registers->TPM_STS_0.commandReady == 0b1))
        return Error::from_errno(EBUSY);

    for (size_t i = 0; i < sizeof(buffer); ++i) {
        m_registers->TPM_DATA_FIFO_0 = buffer[i];
    }
    return {};
}

}
