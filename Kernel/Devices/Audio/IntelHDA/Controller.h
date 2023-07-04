/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Audio/Channel.h>
#include <Kernel/Devices/Audio/Controller.h>
#include <Kernel/Devices/Audio/IntelHDA/InterruptHandler.h>
#include <Kernel/Devices/Audio/IntelHDA/OutputPath.h>
#include <Kernel/Devices/Audio/IntelHDA/RingBuffer.h>
#include <Kernel/Library/IOWindow.h>

namespace Kernel::Audio::IntelHDA {

// Specification: https://www.intel.com/content/dam/www/public/us/en/documents/product-specifications/high-definition-audio-specification.pdf

class Codec;

class Controller final
    : public AudioController
    , public PCI::Device {
public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<AudioController>> create(PCI::DeviceIdentifier const&);
    virtual ~Controller() = default;

    // ^PCI::Device
    virtual StringView device_name() const override { return "IntelHDA"sv; }

    ErrorOr<bool> handle_interrupt(Badge<InterruptHandler>);
    ErrorOr<u32> send_command(u8 codec_address, u8 node_id, CodecControlVerb verb, u16 payload);

private:
    static constexpr size_t fixed_audio_channel_index = 0;

    // 3.3: High Definition Audio Controller Register Set
    enum ControllerRegister : u8 {
        GlobalCapabilities = 0x00,
        VersionMinor = 0x02,
        VersionMajor = 0x03,
        GlobalControl = 0x08,
        StateChangeStatus = 0x0e,
        InterruptControl = 0x20,
        InterruptStatus = 0x24,
        CommandOutboundRingBufferOffset = 0x40,
        ResponseInboundRingBufferOffset = 0x50,
        StreamsOffset = 0x80,
    };

    // 3.3.7: GCTL – Global Control
    enum GlobalControlFlag : u32 {
        ControllerReset = 1u << 0,
        AcceptUnsolicitedResponseEnable = 1u << 8,
    };

    // 3.3.14: INTCTL – Interrupt Control
    enum InterruptControlFlag : u32 {
        GlobalInterruptEnable = 1u << 31,
    };

    // 3.3.15: INTSTS – Interrupt Status
    enum InterruptStatusFlag : u32 {
        GlobalInterruptStatus = 1u << 31,
    };

    Controller(PCI::DeviceIdentifier const&, NonnullOwnPtr<IOWindow>);

    ErrorOr<void> initialize_codec(u8 codec_address);
    ErrorOr<void> configure_output_route();
    ErrorOr<void> reset();

    // ^AudioController
    virtual RefPtr<AudioChannel> audio_channel(u32 index) const override;
    virtual ErrorOr<size_t> write(size_t channel_index, UserOrKernelBuffer const& data, size_t length) override;
    virtual ErrorOr<void> initialize(Badge<AudioManagement>) override;
    virtual ErrorOr<void> set_pcm_output_sample_rate(size_t channel_index, u32 samples_per_second_rate) override;
    virtual ErrorOr<u32> get_pcm_output_sample_rate(size_t channel_index) override;

    NonnullOwnPtr<IOWindow> m_controller_io_window;
    u8 m_number_of_output_streams;
    u8 m_number_of_input_streams;
    u8 m_number_of_bidirectional_streams;
    OwnPtr<CommandOutboundRingBuffer> m_command_buffer;
    OwnPtr<ResponseInboundRingBuffer> m_response_buffer;
    RefPtr<InterruptHandler> m_interrupt_handler;
    Vector<NonnullRefPtr<Codec>> m_codecs {};
    OwnPtr<OutputPath> m_output_path;
    RefPtr<AudioChannel> m_audio_channel;
};

}
