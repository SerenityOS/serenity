/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class SB16;

class SB16 final : public IRQHandler
    , public CharacterDevice {
public:
    SB16();
    virtual ~SB16() override;

    static void detect();
    static void create();
    static SB16& the();

    // ^CharacterDevice
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }

    virtual StringView purpose() const override { return class_name(); }

    // ^Device
    virtual mode_t required_mode() const override { return 0220; }
    virtual String device_name() const override { return "audio"; }

private:
    // ^IRQHandler
    virtual bool handle_irq(const RegisterState&) override;

    // ^CharacterDevice
    virtual StringView class_name() const override { return "SB16"; }

    void initialize();
    void wait_for_irq();
    void dma_start(uint32_t length);
    void set_sample_rate(uint16_t hz);
    void dsp_write(u8 value);
    static u8 dsp_read();
    u8 get_irq_line();
    void set_irq_register(u8 irq_number);
    void set_irq_line(u8 irq_number);

    OwnPtr<Region> m_dma_region;
    int m_major_version { 0 };

    WaitQueue m_irq_queue;
};
}
