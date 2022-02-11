/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Audio/Controller.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class SB16;

class SB16 final
    : public AudioController
    , public IRQHandler {

public:
    virtual ~SB16() override;

    static ErrorOr<NonnullRefPtr<SB16>> try_detect_and_create();

    virtual StringView purpose() const override { return "SB16"sv; }

private:
    // ^AudioController
    virtual RefPtr<AudioChannel> audio_channel(u32 index) const override;
    virtual ErrorOr<size_t> write(size_t channel_index, UserOrKernelBuffer const& data, size_t length) override;
    virtual void detect_hardware_audio_channels(Badge<AudioManagement>) override;
    virtual ErrorOr<void> set_pcm_output_sample_rate(size_t channel_index, u32 samples_per_second_rate) override;
    virtual ErrorOr<u32> get_pcm_output_sample_rate(size_t channel_index) override;

    SB16();

    // ^IRQHandler
    virtual bool handle_irq(const RegisterState&) override;

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
    RefPtr<AudioChannel> m_audio_channel;
};
}
