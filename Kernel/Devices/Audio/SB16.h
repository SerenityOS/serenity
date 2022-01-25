/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class SB16;

class SB16 final : public IRQHandler
    , public CharacterDevice {
    friend class DeviceManagement;

public:
    virtual ~SB16() override;

    static RefPtr<SB16> try_detect_and_create();

    // ^CharacterDevice
    virtual bool can_read(const OpenFileDescription&, u64) const override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const OpenFileDescription&, u64) const override { return true; }

    virtual StringView purpose() const override { return class_name(); }

    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned, Userspace<void*>) override;

private:
    SB16();

    // ^IRQHandler
    virtual bool handle_irq(const RegisterState&) override;

    // ^CharacterDevice
    virtual StringView class_name() const override { return "SB16"sv; }

    void initialize();
    void wait_for_irq();
    void dma_start(uint32_t length);
    void set_sample_rate(uint16_t hz);
    void dsp_write(u8 value);
    static u8 dsp_read();
    u8 get_irq_line();
    void set_irq_register(u8 irq_number);
    void set_irq_line(u8 irq_number);

    OwnPtr<Memory::Region> m_dma_region;
    int m_major_version { 0 };
    u16 m_sample_rate { 44100 };

    WaitQueue m_irq_queue;
};
}
