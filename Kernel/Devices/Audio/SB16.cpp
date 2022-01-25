/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/Audio/SB16.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Sections.h>
#include <Kernel/Thread.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

enum class SampleFormat : u8 {
    Signed = 0x10,
    Stereo = 0x20,
};

constexpr int SB16_DEFAULT_IRQ = 5;

constexpr u16 DSP_READ = 0x22A;
constexpr u16 DSP_WRITE = 0x22C;
constexpr u16 DSP_STATUS = 0x22E;
constexpr u16 DSP_R_ACK = 0x22F;

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
    auto device = device_or_error.release_value();
    DeviceManagement::the().attach_audio_device(device);
    return device;
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
}

ErrorOr<void> SB16::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case SOUNDCARD_IOCTL_GET_SAMPLE_RATE: {
        auto output = static_ptr_cast<u16*>(arg);
        return copy_to_user(output, &m_sample_rate);
    }
    case SOUNDCARD_IOCTL_SET_SAMPLE_RATE: {
        auto sample_rate_input = static_cast<u32>(arg.ptr());
        if (sample_rate_input == 0 || sample_rate_input > 44100)
            return ENOTSUP;
        auto sample_rate_value = static_cast<u16>(sample_rate_input);
        if (m_sample_rate != sample_rate_value)
            set_sample_rate(sample_rate_value);
        return {};
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

bool SB16::can_read(OpenFileDescription const&, u64) const
{
    return false;
}

ErrorOr<size_t> SB16::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    return 0;
}

void SB16::dma_start(uint32_t length)
{
    auto const addr = m_dma_region->physical_page(0)->paddr().get();
    u8 const channel = 5; // 16-bit samples use DMA channel 5 (on the master DMA controller)
    u8 const mode = 0x48;

    // Disable the DMA channel
    IO::out8(0xd4, 4 + (channel % 4));

    // Clear the byte pointer flip-flop
    IO::out8(0xd8, 0);

    // Write the DMA mode for the transfer
    IO::out8(0xd6, (channel % 4) | mode);

    // Write the offset of the buffer
    u16 offset = (addr / 2) % 65536;
    IO::out8(0xc4, (u8)offset);
    IO::out8(0xc4, (u8)(offset >> 8));

    // Write the transfer length
    IO::out8(0xc6, (u8)(length - 1));
    IO::out8(0xc6, (u8)((length - 1) >> 8));

    // Write the buffer
    IO::out8(0x8b, addr >> 16);
    auto page_number = addr >> 16;
    VERIFY(page_number <= NumericLimits<u8>::max());
    IO::out8(0x8b, page_number);

    // Enable the DMA channel
    IO::out8(0xd4, (channel % 4));
}

bool SB16::handle_irq(RegisterState const&)
{
    // FIXME: Check if the interrupt was actually for us or not... (shared IRQs)

    // Stop sound output ready for the next block.
    dsp_write(0xd5);

    IO::in8(DSP_STATUS); // 8 bit interrupt
    if (m_major_version >= 4)
        IO::in8(DSP_R_ACK); // 16 bit interrupt

    m_irq_queue.wake_all();
    return true;
}

void SB16::wait_for_irq()
{
    m_irq_queue.wait_forever("SB16");
    disable_irq();
}

ErrorOr<size_t> SB16::write(OpenFileDescription&, u64, UserOrKernelBuffer const& data, size_t length)
{
    if (!m_dma_region) {
        m_dma_region = TRY(MM.allocate_dma_buffer_page("SB16 DMA buffer", Memory::Region::Access::Write));
    }

    dbgln_if(SB16_DEBUG, "SB16: Writing buffer of {} bytes", length);

    VERIFY(length <= PAGE_SIZE);
    int const BLOCK_SIZE = 32 * 1024;
    if (length > BLOCK_SIZE) {
        return ENOSPC;
    }

    u8 mode = (u8)SampleFormat::Signed | (u8)SampleFormat::Stereo;

    TRY(data.read(m_dma_region->vaddr().as_ptr(), length));
    dma_start(length);

    // 16-bit single-cycle output.
    // FIXME: Implement auto-initialized output.
    u8 command = 0xb0;

    u16 sample_count = length / sizeof(i16);
    if (mode & (u8)SampleFormat::Stereo)
        sample_count /= 2;

    sample_count -= 1;

    cli();
    enable_irq();

    dsp_write(command);
    dsp_write(mode);
    dsp_write((u8)sample_count);
    dsp_write((u8)(sample_count >> 8));

    wait_for_irq();
    return length;
}

}
