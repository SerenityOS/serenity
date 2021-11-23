/*
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>

namespace Kernel {

// See: https://www-inst.eecs.berkeley.edu/~cs150/Documents/ac97_r23.pdf
// And: https://www.intel.com/content/dam/doc/manual/io-controller-hub-7-hd-audio-ac97-manual.pdf

class AC97 final : public PCI::Device
    , public IRQHandler
    , public CharacterDevice {
    friend class DeviceManagement;

public:
    static void detect();

    virtual ~AC97() override;

    // ^IRQHandler
    virtual StringView purpose() const override { return class_name(); }

    // ^CharacterDevice
    virtual bool can_read(const OpenFileDescription&, size_t) const override { return false; }
    virtual bool can_write(const OpenFileDescription&, size_t) const override { return true; }
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned, Userspace<void*>) override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;

private:
    enum NativeAudioMixerRegister : u8 {
        Reset = 0x00,
        SetMasterOutputVolume = 0x02,
        SetPCMOutputVolume = 0x18,
        ExtendedAudioID = 0x28,
        PCMFrontDACRate = 0x2c,
    };

    enum ExtendedAudioMask : u16 {
        VariableRatePCMAudio = 1 << 0,
        Revision = 3 << 10,
    };

    enum AC97Revision : u8 {
        Revision21OrEarlier = 0b00,
        Revision22 = 0b01,
        Revision23 = 0b10,
    };

    enum NativeAudioBusRegister : u8 {
        PCMOutBufferDescriptorListBaseAddress = 0x10,
        PCMOutCurrentIndex = 0x14,
        PCMOutLastValidIndex = 0x15,
        PCMOutStatus = 0x16,
        PCMOutPrefetchedIndex = 0x1a,
        PCMOutControl = 0x1b,
        GlobalControl = 0x2c,
    };

    enum AudioStatusRegisterFlag : u16 {
        DMAControllerHalted = 1 << 0,
        CurrentEqualsLastValid = 1 << 1,
        LastValidBufferCompletionInterrupt = 1 << 2,
        BufferCompletionInterruptStatus = 1 << 3,
        FIFOError = 1 << 4,
    };

    enum AudioControlRegisterFlag : u8 {
        RunPauseBusMaster = 1 << 0,
        ResetRegisters = 1 << 1,
        FIFOErrorInterruptEnable = 1 << 3,
        InterruptOnCompletionEnable = 1 << 4,
    };

    enum GlobalControlFlag : u32 {
        GPIInterruptEnable = 1 << 0,
        AC97ColdReset = 1 << 1,
    };

    enum Muted {
        Yes,
        No,
    };

    struct BufferDescriptorListEntry {
        u32 buffer_pointer;
        u32 control_and_length;
    };

    enum BufferDescriptorListEntryFlags : u32 {
        BufferUnderrunPolicy = 1 << 30,
        InterruptOnCompletion = 1u << 31,
    };

    AC97(PCI::DeviceIdentifier);

    // ^IRQHandler
    virtual bool handle_irq(const RegisterState&) override;

    // ^CharacterDevice
    virtual StringView class_name() const override { return "AC97"sv; }

    void initialize();
    void reset_pcm_out();
    void set_master_output_volume(u8, u8, Muted);
    void set_pcm_output_sample_rate(u16);
    void set_pcm_output_volume(u8, u8, Muted);
    void set_output_buffer_dma_lvi(u8);
    void start_output_buffer_dma();

    OwnPtr<Memory::Region> m_buffer_descriptor_list;
    u8 m_buffer_descriptor_list_index = 0;
    IOAddress m_io_mixer_base;
    IOAddress m_io_bus_base;
    WaitQueue m_irq_queue;
    OwnPtr<Memory::Region> m_output_buffer;
    bool m_output_buffer_dma_running = false;
    u8 m_output_buffer_page_count = 4;
    u8 m_output_buffer_page_index = 0;
    u16 m_sample_rate = 44100;
};

}
