/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/WaitQueue.h>
#include <LibBareMetal/Memory/PhysicalAddress.h>

namespace Kernel {

class SB16;

class SB16 final : public IRQHandler
    , public CharacterDevice {
public:
    SB16();
    virtual ~SB16() override;

    static SB16& the();

    // ^CharacterDevice
    virtual bool can_read(const FileDescription&) const override;
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_write(const FileDescription&) const override { return true; }

    virtual const char* purpose() const override { return class_name(); }

private:
    // ^IRQHandler
    virtual void handle_irq(RegisterState&) override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "SB16"; }

    void initialize();
    void wait_for_irq();
    void dma_start(uint32_t length);
    void set_sample_rate(uint16_t hz);
    void dsp_write(u8 value);
    u8 dsp_read();
    u8 get_irq_line();
    void set_irq_register(u8 irq_number);
    void set_irq_line(u8 irq_number);

    OwnPtr<Region> m_dma_region;
    int m_major_version { 0 };

    WaitQueue m_irq_queue;
};
}
