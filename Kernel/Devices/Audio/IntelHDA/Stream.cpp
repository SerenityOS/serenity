/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Stream.h"

#include <AK/Optional.h>
#include <Kernel/Devices/Audio/IntelHDA/Controller.h>
#include <Kernel/Devices/Audio/IntelHDA/Format.h>
#include <Kernel/Devices/Audio/IntelHDA/Timing.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::Audio::IntelHDA {

Stream::~Stream()
{
    if (m_running)
        MUST(stop());
}

u32 Stream::read_control()
{
    // 3.3.35: Input/Output/Bidirectional Stream Descriptor Control
    u32 control_and_status = m_stream_io_window->read32(StreamRegisterOffset::Control);
    return control_and_status & 0xffffffu;
}

void Stream::write_control(u32 control)
{
    // 3.3.35: Input/Output/Bidirectional Stream Descriptor Control
    auto status = m_stream_io_window->read8(StreamRegisterOffset::Status);
    u32 control_and_status = (status << 24)
        | ((m_stream_number & 0xf) << 20)
        | (control & 0xfffffu);
    m_stream_io_window->write32(StreamRegisterOffset::Control, control_and_status);
}

static constexpr u8 container_size_in_bytes(u8 bit_size)
{
    // 4.5.1: Stream Data In Memory
    if (bit_size > 16)
        return 4;
    else if (bit_size > 8)
        return 2;
    return 1;
}

ErrorOr<void> Stream::initialize_buffer()
{
    VERIFY(m_format_parameters.sample_rate > 0);
    VERIFY(m_format_parameters.pcm_bits > 0);
    VERIFY(m_format_parameters.number_of_channels > 0);

    // 4.5.1: Stream Data In Memory
    // NOTE: we ignore the number of blocks per packet since we are only required to have an integer number
    //       of samples per buffer, and we always have at least one packet per buffer.
    size_t block_size_in_bytes = container_size_in_bytes(m_format_parameters.pcm_bits) * m_format_parameters.number_of_channels;
    size_t number_of_blocks_in_buffer = PAGE_SIZE / block_size_in_bytes;
    VERIFY(number_of_blocks_in_buffer > 0);

    size_t number_of_blocks_required_for_cyclic_buffer_size = ceil_div(cyclic_buffer_size_in_ms * m_format_parameters.sample_rate, 1'000);
    size_t number_of_buffers_required_for_cyclic_buffer_size = AK::max(ceil_div(number_of_blocks_required_for_cyclic_buffer_size, number_of_blocks_in_buffer), minimum_number_of_buffers);
    VERIFY(number_of_buffers_required_for_cyclic_buffer_size > 0 && number_of_buffers_required_for_cyclic_buffer_size <= 256);

    size_t cyclic_buffer_size_in_bytes = number_of_buffers_required_for_cyclic_buffer_size * PAGE_SIZE;

    TRY(m_buffers.with([&](auto& buffers) -> ErrorOr<void> {
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        buffers = TRY(MM.allocate_dma_buffer_pages(cyclic_buffer_size_in_bytes, "IntelHDA Stream Buffers"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));

        // 3.3.38 Input/Output/Bidirectional Stream Descriptor Cyclic Buffer Length
        m_stream_io_window->write32(StreamRegisterOffset::CyclicBufferLength, buffers->size());

        // 3.3.39: Input/Output/Bidirectional Stream Descriptor Last Valid Index
        m_stream_io_window->write16(StreamRegisterOffset::LastValidIndex, number_of_buffers_required_for_cyclic_buffer_size - 1);

        // 3.6.2: Buffer Descriptor List
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        m_buffer_descriptor_list = TRY(MM.allocate_dma_buffer_page("IntelHDA Stream BDL"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
        auto bdl_physical_address = m_buffer_descriptor_list->physical_page(0)->paddr().get();
        m_stream_io_window->write32(StreamRegisterOffset::BDLLowerBaseAddress, bdl_physical_address & 0xffffffffu);
        m_stream_io_window->write32(StreamRegisterOffset::BDLUpperBaseAddress, bdl_physical_address >> 32);

        // 3.6.3: Buffer Descriptor List Entry
        auto* buffer_descriptors = bit_cast<BufferDescriptorEntry*>(m_buffer_descriptor_list->vaddr().as_ptr());
        for (size_t buffer_index = 0; buffer_index < buffers->page_count(); ++buffer_index) {
            auto* entry = &buffer_descriptors[buffer_index];
            entry->address = buffers->physical_page(buffer_index)->paddr().get();
            entry->size = PAGE_SIZE;
            entry->flags = BufferDescriptorEntryFlag::InterruptOnCompletion;
        }
        return {};
    }));
    return {};
}

ErrorOr<void> Stream::reset()
{
    // 3.3.35: Input/Output/Bidirectional Stream Descriptor Control
    if (m_running)
        TRY(stop());

    // Writing a 1 causes the corresponding stream to be reset. The Stream Descriptor registers
    // (except the SRST bit itself), FIFO's, and cadence generator for the corresponding stream
    // are reset.
    auto control = read_control();
    control |= StreamControlFlag::StreamReset;
    write_control(control);

    // After the stream hardware has completed sequencing into the reset state, it will report a
    // 1 in this bit. Software must read a 1 from this bit to verify that the stream is in reset.
    TRY(wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() {
        control = read_control();
        return (control & StreamControlFlag::StreamReset) > 0;
    }));

    // Writing a 0 causes the corresponding stream to exit reset.
    control &= ~StreamControlFlag::StreamReset;
    write_control(control);

    // When the stream hardware is ready to begin operation, it will report a 0 in this bit.
    // Software must read a 0 from this bit before accessing any of the stream registers
    return wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() {
        control = read_control();
        return (control & StreamControlFlag::StreamReset) == 0;
    });
}

void Stream::start()
{
    // 3.3.35: Input/Output/Bidirectional Stream Descriptor Control
    VERIFY(!m_running);
    dbgln_if(INTEL_HDA_DEBUG, "IntelHDA: Starting stream");

    auto control = read_control();
    control |= StreamControlFlag::StreamRun;
    control |= StreamControlFlag::InterruptOnCompletionEnable;
    write_control(control);
    m_running = true;
}

ErrorOr<void> Stream::stop()
{
    // 3.3.35: Input/Output/Bidirectional Stream Descriptor Control
    VERIFY(m_running);
    dbgln_if(INTEL_HDA_DEBUG, "IntelHDA: Stopping stream");

    auto control = read_control();
    control &= ~StreamControlFlag::StreamRun;
    write_control(control);

    // 4.5.4: Stopping Streams
    // Wait until RUN bit is 0
    TRY(wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() {
        control = read_control();
        return (control & StreamControlFlag::StreamRun) == 0;
    }));

    m_running = false;
    m_buffer_position = 0;
    return {};
}

ErrorOr<void> Stream::set_format(FormatParameters format)
{
    // Reset the stream so we can set a new buffer
    TRY(reset());

    // Write the sample rate payload
    auto format_payload = TRY(encode_format(format));
    m_stream_io_window->write16(StreamRegisterOffset::Format, format_payload);
    m_format_parameters = format;

    // Re-initialize the bufer
    TRY(initialize_buffer());
    return {};
}

ErrorOr<void> OutputStream::handle_interrupt(Badge<Controller>)
{
    auto interrupt_status = m_stream_io_window->read8(StreamRegisterOffset::Status);

    if ((interrupt_status & StreamStatusFlag::BufferCompletionInterruptStatus) > 0) {
        // 3.3.36: BCIS remains active until software clears it by writing a 1 to this bit position.
        m_stream_io_window->write8(StreamRegisterOffset::Status, interrupt_status);

        // Wake up thread waiting for new buffers to write to
        m_irq_queue.wake_all();

        // If the read head is past our last written buffer, stop the stream. There are three possible
        // condition combinations of last & new link, and current buffer position for our circular buffer.
        auto new_link_position = m_stream_io_window->read32(StreamRegisterOffset::LinkPosition);
        if ((m_last_link_position < m_buffer_position && m_buffer_position < new_link_position)
            || (new_link_position < m_last_link_position && m_last_link_position < m_buffer_position)
            || (m_buffer_position < new_link_position && new_link_position < m_last_link_position)) {
            dbgln_if(INTEL_HDA_DEBUG, "OutputStream::{}: Stopping because of stream underrun (link position: {} → {}, buffer position: {})",
                __FUNCTION__, m_last_link_position, new_link_position, m_buffer_position);
            TRY(stop());
        }
        m_last_link_position = new_link_position;
    }

    return {};
}

ErrorOr<size_t> OutputStream::write(UserOrKernelBuffer const& data, size_t length)
{
    auto wait_until_buffer_index_can_be_written = [&](u8 buffer_index) {
        while (m_running) {
            m_last_link_position = m_stream_io_window->read32(StreamRegisterOffset::LinkPosition);
            auto read_buffer_index = m_last_link_position / PAGE_SIZE;
            if (read_buffer_index != buffer_index)
                return;

            dbgln_if(INTEL_HDA_DEBUG, "IntelHDA: Waiting until buffer {} becomes writeable", buffer_index);

            m_irq_queue.wait_forever("IntelHDA"sv);
        }
    };

    auto write_into_single_buffer = [&](UserOrKernelBuffer const& data, size_t data_offset, size_t length, size_t offset_within_buffer) -> ErrorOr<u8> {
        u8 buffer_index = m_buffer_position / PAGE_SIZE;
        VERIFY(length <= PAGE_SIZE - offset_within_buffer);

        wait_until_buffer_index_can_be_written(buffer_index);

        TRY(m_buffers.with([&](auto& buffers) -> ErrorOr<void> {
            // NOTE: if the buffers were reinitialized, we might point to an out-of-bounds page
            if (buffer_index >= buffers->page_count())
                return EAGAIN;

            auto* buffer = buffers->vaddr_from_page_index(buffer_index).as_ptr() + offset_within_buffer;
            TRY(data.read(buffer, data_offset, length));

            // Cycle back to position 0 when we reach the end
            m_buffer_position += length;
            VERIFY(m_buffer_position <= buffers->size());
            if (m_buffer_position == buffers->size())
                m_buffer_position = 0;
            return {};
        }));
        return buffer_index;
    };

    // FIXME: support PCM bit sizes other than 16
    VERIFY(m_format_parameters.pcm_bits == 16);

    // Split up input data into separate buffer writes
    size_t length_remaining = length;
    size_t data_offset = 0;
    u8 last_buffer_index = 0;
    while (length_remaining > 0) {
        size_t offset_within_current_buffer = m_buffer_position % PAGE_SIZE;
        size_t length_to_write = AK::min(length_remaining, PAGE_SIZE - offset_within_current_buffer);

        last_buffer_index = TRY(write_into_single_buffer(data, data_offset, length_to_write, offset_within_current_buffer));

        data_offset += length_to_write;
        length_remaining -= length_to_write;
    }

    // Start this stream if not already running
    // 3.3.39: LVI must be at least 1; i.e., there must be at least two valid entries in
    // the buffer descriptor list before DMA operations can begin.
    if (!m_running && last_buffer_index >= 2)
        start();

    return length;
}

}
