/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <Kernel/Devices/Audio/IntelHDA/Codec.h>
#include <Kernel/Devices/Audio/IntelHDA/Format.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel::Audio::IntelHDA {

class Stream {
public:
    static constexpr u32 cyclic_buffer_size_in_ms = 40;

    virtual ~Stream();

    u8 stream_number() const { return m_stream_number; }
    bool running() const { return m_running; }
    u32 sample_rate() const { return m_format_parameters.sample_rate; }

    void start();
    ErrorOr<void> stop();
    virtual ErrorOr<void> handle_interrupt(Badge<Controller>) = 0;

    ErrorOr<void> set_format(FormatParameters);

protected:
    // We always need 2 filled buffers, plus an additional one to prevent buffer underrun
    static constexpr u8 minimum_number_of_buffers = 3;

    // 3.3: High Definition Audio Controller Register Set - streams
    enum StreamRegisterOffset : u8 {
        Control = 0x00,
        Status = 0x03,
        LinkPosition = 0x04,
        CyclicBufferLength = 0x08,
        LastValidIndex = 0x0c,
        Format = 0x12,
        BDLLowerBaseAddress = 0x18,
        BDLUpperBaseAddress = 0x1c,
    };

    // 3.3.35: Input/Output/Bidirectional Stream Descriptor Control
    enum StreamControlFlag : u32 {
        StreamReset = 1u << 0,
        StreamRun = 1u << 1,
        InterruptOnCompletionEnable = 1u << 2,
    };

    // 3.3.36 : Input/Output/Bidirectional Stream Descriptor Status
    enum StreamStatusFlag : u8 {
        BufferCompletionInterruptStatus = 1u << 2,
    };

    // 3.6.3: Buffer Descriptor List Entry
    enum BufferDescriptorEntryFlag : u32 {
        InterruptOnCompletion = 1u << 0,
    };

    // 3.6.3: Buffer Descriptor List Entry
    struct BufferDescriptorEntry {
        u64 address;
        u32 size;
        BufferDescriptorEntryFlag flags;
    };

    Stream(NonnullOwnPtr<IOWindow> stream_io_window, u8 stream_number)
        : m_stream_io_window(move(stream_io_window))
        , m_stream_number(stream_number)
    {
    }

    u32 read_control();
    void write_control(u32);

    ErrorOr<void> initialize_buffer();
    ErrorOr<void> reset();

    NonnullOwnPtr<IOWindow> m_stream_io_window;
    u8 m_stream_number;
    OwnPtr<Memory::Region> m_buffer_descriptor_list;
    SpinlockProtected<OwnPtr<Memory::Region>, LockRank::None> m_buffers;
    size_t m_buffer_position { 0 };
    WaitQueue m_irq_queue;
    bool m_running { false };
    FormatParameters m_format_parameters;
};

class OutputStream : public Stream {
public:
    static constexpr u8 fixed_channel = 0;

    static ErrorOr<NonnullOwnPtr<OutputStream>> create(NonnullOwnPtr<IOWindow> stream_io_window, u8 stream_number)
    {
        return adopt_nonnull_own_or_enomem(new (nothrow) OutputStream(move(stream_io_window), stream_number));
    }

    ~OutputStream() = default;

    // ^Stream
    ErrorOr<void> handle_interrupt(Badge<Controller>) override;

    ErrorOr<size_t> write(UserOrKernelBuffer const&, size_t);

private:
    OutputStream(NonnullOwnPtr<IOWindow> stream_io_window, u8 stream_number)
        : Stream(move(stream_io_window), stream_number)
    {
        // 3.3.35: Input/Output/Bidirectional Stream Descriptor Control
        //         "Although the controller hardware is capable of transmitting any stream number,
        //          by convention stream 0 is reserved as unused by software, so that converters
        //          whose stream numbers have been reset to 0 do not unintentionally decode data
        //          not intended for them."
        VERIFY(stream_number >= 1);
    }

    u32 m_last_link_position { 0 };
};

// FIXME: implement InputStream and BidirectionalStream

}
