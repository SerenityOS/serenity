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

#include <Kernel/Devices/AudioDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

class SB16;

class SB16 final : public IRQHandler
    , public AudioDevice {
public:
    SB16();
    virtual ~SB16() override;

    static void detect();
    static void create();
    static SB16& the();

protected:
    virtual bool do_initialize(Stream&) override;
    virtual bool can_support_pcm_configuration(Stream&, const CurrentPCM&) override;
    virtual void trigger_playback(Stream&) override;
    virtual void transferred_to_dma_buffer(Stream&, bool) override;
    virtual u8* playback_current_dma_period(Stream&, bool) override;

    // ^Device
    virtual mode_t required_mode() const override { return 0220; }
    virtual String device_name() const override { return "audio"; }

private:
    // ^IRQHandler
    virtual void handle_irq(const RegisterState&) override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "SB16"; }

    void write_mixer_reg(u8 index, u8 value);
    u8 read_mixer_reg(u8 index) const;

    void initialize();
    void setup_dma(Stream&, u32);
    void dsp_transfer_block(Stream&, u32);
    void set_sample_rate(uint16_t hz);
    void dsp_write(u8 value);
    static u8 dsp_read();
    u8 get_irq_line();
    static u64 calculate_period_time(const CurrentPCM&);
    void set_irq_register(u8 irq_number);
    void set_irq_line(u8 irq_number);
    void set_dma_channels();
    bool allocate_dma_region(Stream&);
    PhysicalAddress playback_current_dma_period_paddr(Stream&, bool);

    struct StreamPrivateData {
        u32 periods_read { 0 };
        u32 periods_written { 0 };
    };
    StreamPrivateData& stream_private(Stream& stream)
    {
        return *(StreamPrivateData*)stream.private_data;
    }

    int m_major_version { 0 };
    Vector<StreamPrivateData, 1> m_stream_private;
    Stream* m_active_stream { nullptr };
    u16 m_port { 0x220 };
    bool m_is16bit { false };
};
}
