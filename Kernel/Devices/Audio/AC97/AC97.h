/*
 * Copyright (c) 2021-2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/RefPtr.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Audio/Controller.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

// See: https://www-inst.eecs.berkeley.edu/~cs150/Documents/ac97_r23.pdf
// And: https://www.intel.com/content/dam/doc/manual/io-controller-hub-7-hd-audio-ac97-manual.pdf

class AC97 final
    : public AudioController
    , public PCI::Device
    , public IRQHandler {

public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<AudioController>> create(PCI::DeviceIdentifier const&);

    virtual ~AC97() override;

    // ^PCI::Device
    virtual StringView device_name() const override { return "AC97"sv; }

    // ^IRQHandler
    virtual StringView purpose() const override { return "AC97"sv; }

private:
    enum NativeAudioMixerRegister : u8 {
        Reset = 0x00,
        SetMasterOutputVolume = 0x02,
        SetPCMOutputVolume = 0x18,
        ExtendedAudioID = 0x28,
        ExtendedAudioStatusControl = 0x2a,
        PCMFrontDACRate = 0x2c,
        VendorID1 = 0x7c,
        VendorID2 = 0x7e,
        MaxUsedMixerOffset = 0x7f,
    };

    enum ExtendedAudioMask : u16 {
        VariableRatePCMAudio = 1 << 0,
        DoubleRatePCMAudio = 1 << 1,
        Revision = 3 << 10,
    };

    enum ExtendedAudioStatusControlFlag : u16 {
        VariableRateAudio = 1 << 0,
        DoubleRateAudio = 1 << 1,
    };

    enum AC97Revision : u8 {
        Revision21OrEarlier = 0b00,
        Revision22 = 0b01,
        Revision23 = 0b10,
        Reserved = 0b11,
    };

    enum NativeAudioBusChannel : u8 {
        PCMInChannel = 0x00,
        PCMOutChannel = 0x10,
        MicrophoneInChannel = 0x20,
        Microphone2Channel = 0x40,
        PCMIn2Channel = 0x50,
        SPDIFChannel = 0x60,
    };

    enum NativeAudioBusRegister : u8 {
        GlobalControl = 0x2c,
        MaxUsedBusOffset = 0x2f
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

    class AC97Channel {
    public:
        enum Register : u8 {
            BufferDescriptorListBaseAddress = 0x00,
            CurrentIndexValue = 0x04,
            LastValidIndex = 0x05,
            Status = 0x06,
            PositionInCurrentBuffer = 0x08,
            PrefetchedIndexValue = 0x0a,
            Control = 0x0b,
        };

        static ErrorOr<NonnullOwnPtr<AC97Channel>> create_with_parent_pci_device(PCI::Address pci_device_address, StringView name, NonnullOwnPtr<IOWindow> channel_io_base);

        bool dma_running() const
        {
            return m_dma_running.with([](auto value) { return value; });
        }
        void handle_dma_stopped();
        StringView name() const { return m_name; }
        void reset();
        void set_last_valid_index(u32 buffer_address, u8 last_valid_index);
        void start_dma();

        IOWindow& io_window() { return *m_channel_io_window; }

    private:
        AC97Channel(PCI::Address pci_device_address, StringView name, NonnullOwnPtr<IOWindow> channel_io_base)
            : m_channel_io_window(move(channel_io_base))
            , m_device_pci_address(pci_device_address)
            , m_name(name)
        {
        }

        NonnullOwnPtr<IOWindow> m_channel_io_window;
        PCI::Address m_device_pci_address;
        SpinlockProtected<bool, LockRank::None> m_dma_running { false };
        StringView m_name;
    };

    AC97(PCI::DeviceIdentifier const&, NonnullOwnPtr<AC97Channel> pcm_out_channel, NonnullOwnPtr<IOWindow> mixer_io_window, NonnullOwnPtr<IOWindow> bus_io_window);

    // ^IRQHandler
    virtual bool handle_irq() override;

    void set_master_output_volume(u8, u8, Muted);
    u32 read_pcm_output_sample_rate();
    ErrorOr<void> set_pcm_output_sample_rate(u32);
    void set_pcm_output_volume(u8, u8, Muted);
    ErrorOr<void> write_single_buffer(UserOrKernelBuffer const&, size_t, size_t);

    // ^AudioController
    virtual ErrorOr<void> initialize(Badge<AudioManagement>) override;
    virtual RefPtr<AudioChannel> audio_channel(u32 index) const override;
    virtual ErrorOr<size_t> write(size_t channel_index, UserOrKernelBuffer const& data, size_t length) override;
    virtual ErrorOr<void> set_pcm_output_sample_rate(size_t channel_index, u32 samples_per_second_rate) override;
    virtual ErrorOr<u32> get_pcm_output_sample_rate(size_t channel_index) override;

    OwnPtr<Memory::Region> m_buffer_descriptor_list;
    u8 m_buffer_descriptor_list_index { 0 };
    AC97Revision m_codec_revision { AC97Revision::Revision21OrEarlier };
    bool m_double_rate_pcm_enabled { false };
    NonnullOwnPtr<IOWindow> m_mixer_io_window;
    NonnullOwnPtr<IOWindow> m_bus_io_window;
    WaitQueue m_irq_queue;
    OwnPtr<Memory::Region> m_output_buffer;
    u8 m_output_buffer_page_count { 4 };
    u8 m_output_buffer_page_index { 0 };
    NonnullOwnPtr<AC97Channel> m_pcm_out_channel;
    u32 m_sample_rate { 0 };
    bool m_variable_rate_pcm_supported { false };
    RefPtr<AudioChannel> m_audio_channel;
};

}
