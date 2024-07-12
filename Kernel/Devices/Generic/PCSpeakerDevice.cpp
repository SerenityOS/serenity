/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/BeepInstruction.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/PCSpeaker.h>
#endif
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/PCSpeakerDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<PCSpeakerDevice> PCSpeakerDevice::must_create()
{
    auto device = MUST(Device::try_create_device<PCSpeakerDevice>());
    return *device;
}

UNMAP_AFTER_INIT PCSpeakerDevice::PCSpeakerDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Generic, 10)
{
}

UNMAP_AFTER_INIT PCSpeakerDevice::~PCSpeakerDevice() = default;

bool PCSpeakerDevice::can_read(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> PCSpeakerDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<size_t> PCSpeakerDevice::write(OpenFileDescription&, u64, UserOrKernelBuffer const& buffer, size_t buffer_size)
{
    if (!kernel_command_line().is_pc_speaker_enabled())
        return Error::from_errno(ENOTSUP);
    if (buffer_size % sizeof(BeepInstruction) != 0)
        return Error::from_errno(EINVAL);
    BeepInstruction instruction {};
    TRY(buffer.read(&instruction, sizeof(BeepInstruction)));
    if (instruction.tone < 20 || instruction.tone > 20000)
        return Error::from_errno(EINVAL);
    if (instruction.milliseconds_duration == 0)
        return Error::from_errno(EINVAL);
#if ARCH(X86_64)
    PCSpeaker::tone_on(instruction.tone);
    auto result = Thread::current()->sleep(Duration::from_milliseconds(instruction.milliseconds_duration));
    PCSpeaker::tone_off();
    if (result.was_interrupted())
        return Error::from_errno(EINTR);
    return sizeof(BeepInstruction);
#else
    return Error::from_errno(ENOTIMPL);
#endif
}

}
