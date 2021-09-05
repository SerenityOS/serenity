/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Kernel/StdLib.h"
#include "stddef.h"
#include <AK/Format.h>
#include <AK/Memory.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/SB16.h>
#include <Kernel/IO.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Sections.h>
#include <Kernel/Thread.h>
#include <LibC/sys/ioctl_numbers.h>
#include <errno_numbers.h>

namespace Kernel {

enum class SampleFormat : u8 {
    Signed = 0x10,
    Stereo = 0x20,
};

constexpr size_t AUDIO_BUFFER_CAPACITY = PAGE_SIZE * 16;

constexpr int SB16_DEFAULT_IRQ = 5;

constexpr u16 DSP_READ = 0x22A;
constexpr u16 DSP_WRITE = 0x22C;
constexpr u16 DSP_STATUS = 0x22E;
constexpr u16 DSP_R_ACK = 0x22F;

constexpr u8 DMA_AUTO_INITIALIZE = 0b1 << 4;
constexpr u8 DMA_TRANSFER_PERIPHERAL_READ = 0b10 << 2;
constexpr u8 DMA_TRANSFER_MODE_SINGLE = 0b01 << 6;
constexpr u8 DMA_TRANSFER_MODE_BLOCK = 0b10 << 6;

/* Write a value to the DSP write register */
void SB16::dsp_write(u8 value)
{
    while (IO::in8(DSP_WRITE) & 0x80)
        ;
    IO::out8(DSP_WRITE, value);
}

/* Reads the value of the DSP read register */
u8 SB16::dsp_read()
{
    while (!(IO::in8(DSP_STATUS) & 0x80))
        ;
    return IO::in8(DSP_READ);
}

/* Changes the sample rate of sound output */
void SB16::set_sample_rate(uint16_t hz)
{
    dbgln("SB16: Changing sample rate to {} Hz", hz);
    m_sample_rate = hz;
    dsp_write(0x41); // output
    dsp_write((u8)(hz >> 8));
    dsp_write((u8)hz);
    dsp_write(0x42); // input
    dsp_write((u8)(hz >> 8));
    dsp_write((u8)hz);
}

UNMAP_AFTER_INIT SB16::SB16()
    : IRQHandler(SB16_DEFAULT_IRQ)
    // FIXME: We can't change version numbers later, i.e. after the sound card is initialized.
    , CharacterDevice(42, 42)
{
    initialize();
}

UNMAP_AFTER_INIT SB16::~SB16()
{
}

UNMAP_AFTER_INIT RefPtr<SB16> SB16::try_detect_and_create()
{
    IO::out8(0x226, 1);
    IO::delay(32);
    IO::out8(0x226, 0);

    auto data = dsp_read();
    if (data != 0xaa)
        return {};
    auto device_or_error = DeviceManagement::try_create_device<SB16>();
    if (device_or_error.is_error())
        return {};
    return device_or_error.release_value();
}

UNMAP_AFTER_INIT void SB16::initialize()
{
    disable_irq();

    IO::out8(0x226, 1);
    IO::delay(32);
    IO::out8(0x226, 0);

    auto data = dsp_read();
    if (data != 0xaa) {
        dbgln("SB16: SoundBlaster not ready");
        return;
    }

    // Get the version info
    dsp_write(0xe1);
    m_major_version = dsp_read();
    auto vmin = dsp_read();

    dmesgln("SB16: Found version {}.{}", m_major_version, vmin);
    set_irq_register(SB16_DEFAULT_IRQ);
    dmesgln("SB16: IRQ {}", get_irq_line());

    set_sample_rate(m_sample_rate);

    // Allocate a transfer audio buffer that is independent of DMA
    m_audio_buffer = make<Memory::RingBuffer>("SB16 transfer audio buffer", AUDIO_BUFFER_CAPACITY);

    // Allocate a DMA region
    auto page = MM.allocate_supervisor_physical_page();
    if (!page) {
        dmesgln("SB16: Could not allocate DMA memory region from supervisor.");
        return;
    }

    auto nonnull_page = page.release_nonnull();
    auto maybe_vmobject = Memory::AnonymousVMObject::try_create_with_physical_pages({ &nonnull_page, 1 });
    if (maybe_vmobject.is_error()) {
        dmesgln("SB16: Could not create VM object from DMA memory region: {}", maybe_vmobject.error());
        return;
    }

    auto maybe_dma_region = MM.allocate_kernel_region_with_vmobject(maybe_vmobject.release_value(), PAGE_SIZE, "SB16 DMA buffer", Memory::Region::Access::Write);
    if (maybe_dma_region.is_error()) {
        dmesgln("SB16: Could not create DMA kernel region from VM object.");
        return;
    }
    m_dma_region = maybe_dma_region.release_value();
    auto dma_region_as_buffer = UserOrKernelBuffer::for_kernel_buffer(m_dma_region->vaddr().as_ptr());
    if (dma_region_as_buffer.memset(0, PAGE_SIZE).is_error())
        dbgln("SB16: Warning: Could not zero the DMA transfer region.");

    // Setup DMA transfer in auto-initialize mode
    dma_start(PAGE_SIZE);

    // Setup DSP to receive sample data from DMA,
    // but only chunks half the DMA region size.
    cli();
    enable_irq();

    u8 mode = (u8)SampleFormat::Signed | (u8)SampleFormat::Stereo;
    // 16-bit auto-initialize output.
    u8 command = 0xb6;
    size_t const dsp_transfer_size = PAGE_SIZE / 4;

    dsp_write(command);
    dsp_write(mode);
    dsp_write((u8)dsp_transfer_size);
    dsp_write((u8)(dsp_transfer_size >> 8));
}

KResult SB16::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case SOUNDCARD_IOCTL_GET_SAMPLE_RATE: {
        auto output = static_ptr_cast<u16*>(arg);
        return copy_to_user(output, &m_sample_rate);
    }
    case SOUNDCARD_IOCTL_SET_SAMPLE_RATE: {
        auto sample_rate_value = static_cast<u16>(arg.ptr());
        if (sample_rate_value == 0)
            return EINVAL;
        if (m_sample_rate != sample_rate_value)
            set_sample_rate(sample_rate_value);
        return KSuccess;
    }
    case SOUNDCARD_IOCTL_GET_FREE_BUFFER: {
        auto output = static_ptr_cast<size_t*>(arg);
        auto free = m_audio_buffer->free_bytes();
        return copy_to_user(output, &free);
    }
    default:
        return EINVAL;
    }
}

void SB16::set_irq_register(u8 irq_number)
{
    u8 bitmask;
    switch (irq_number) {
    case 2:
        bitmask = 0;
        break;
    case 5:
        bitmask = 0b10;
        break;
    case 7:
        bitmask = 0b100;
        break;
    case 10:
        bitmask = 0b1000;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    IO::out8(0x224, 0x80);
    IO::out8(0x225, bitmask);
}

u8 SB16::get_irq_line()
{
    IO::out8(0x224, 0x80);
    u8 bitmask = IO::in8(0x225);
    switch (bitmask) {
    case 0:
        return 2;
    case 0b10:
        return 5;
    case 0b100:
        return 7;
    case 0b1000:
        return 10;
    }
    return bitmask;
}
void SB16::set_irq_line(u8 irq_number)
{
    InterruptDisabler disabler;
    if (irq_number == get_irq_line())
        return;
    set_irq_register(irq_number);
    change_irq_number(irq_number);
}

bool SB16::can_read(OpenFileDescription const&, size_t) const
{
    return false;
}

KResultOr<size_t> SB16::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    return 0;
}

void SB16::dma_start(uint32_t length)
{
    auto const addr = m_dma_region->physical_page(0)->paddr().get();
    u8 const channel = 5; // 16-bit samples use DMA channel 5 (on the master DMA controller)
    // Peripheral reads memory, auto-initialize, block transfer mode
    u8 const mode = DMA_TRANSFER_MODE_BLOCK | DMA_TRANSFER_PERIPHERAL_READ | DMA_AUTO_INITIALIZE;

    // Disable the DMA channel
    IO::out8(0xd4, 4 + (channel % 4));

    // Clear the byte pointer flip-flop
    IO::out8(0xd8, 0);

    // Write the DMA mode for the transfer
    IO::out8(0xd6, (channel % 4) | mode);

    // Write the offset of the buffer
    u16 offset = (addr >> 1) % 65536;
    IO::out8(0xc4, (u8)offset);
    IO::out8(0xc4, (u8)(offset >> 8));

    // Write the transfer length
    IO::out8(0xc6, (u8)(length - 1));
    IO::out8(0xc6, (u8)((length - 1) >> 8));

    // Write the buffer
    auto page_number = addr >> 16;
    VERIFY(page_number <= NumericLimits<u8>::max());
    IO::out8(0x8b, page_number);

    // Enable the DMA channel
    IO::out8(0xd4, (channel % 4));
}

bool SB16::handle_irq(RegisterState const&)
{
    // FIXME: Check if the interrupt was actually for us or not... (shared IRQs)

    IO::in8(DSP_STATUS); // 8 bit interrupt
    if (m_major_version >= 4)
        IO::in8(DSP_R_ACK); // 16 bit interrupt

    // Copy the next chunk of data from the transfer audio buffer into the DMA buffer half that was just freed
    size_t const new_data_start = m_current_buffer_half * PAGE_SIZE / 2;
    // no buffer overflows today
    VERIFY(new_data_start + PAGE_SIZE / 2 <= PAGE_SIZE);
    auto dma_region_as_buffer = UserOrKernelBuffer::for_kernel_buffer(m_dma_region->vaddr().offset(new_data_start).as_ptr());
    auto copy_result = m_audio_buffer->copy_data_out(PAGE_SIZE / 2 - 1, dma_region_as_buffer);

    if (copy_result.is_error()) {
        dbgln("SB16: Could not copy new audio data to DMA transfer region");
        return true;
    }
    m_audio_buffer->reclaim_space(copy_result.value());

    if (copy_result.value() < PAGE_SIZE / 2) {
        auto start = copy_result.value(), length = PAGE_SIZE / 2 - copy_result.value() - 1;
        auto zero_result = dma_region_as_buffer.memset(0, start, length);
        if (zero_result.is_error()) {
            dbgln("SB16: Could not zero the unused DMA transfer region");
            return true;
        }
    }

    dbgln_if(SB16_DEBUG, "SB16: Copied {} bytes to DMA transfer region at offset {:x}, {} remaining in buffer", copy_result.value(), new_data_start, m_audio_buffer->used_bytes());

    m_current_buffer_half = (m_current_buffer_half + 1) % 2;
    return true;
}

KResultOr<size_t> SB16::write(OpenFileDescription&, u64, UserOrKernelBuffer const& data, size_t length)
{
    dbgln_if(SB16_DEBUG, "SB16: Writing buffer of {} bytes", length);

    VERIFY(length <= PAGE_SIZE);

    size_t bytes_copied = 0;
    PhysicalAddress data_start;
    // When reaching the buffer size limit, we may only write part of the data.
    // The user has to be responsible and only provide a low enough amount of data.
    m_audio_buffer->copy_data_in(data, 0, length, data_start, bytes_copied);
    // dbgln_if(SB16_DEBUG, "SB16: Buffer increased from {} to {} bytes", used_before, used_after);

    if (bytes_copied == 0)
        return KResultOr<size_t>((ErrnoCode)ENOSPC);

    return KResultOr<size_t>((size_t)bytes_copied);
}

}
