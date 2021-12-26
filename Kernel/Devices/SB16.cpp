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

#include <AK/Memory.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/Devices/SB16.h>
#include <Kernel/IO.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>

//#define SB16_DEBUG

namespace Kernel {
#define SB16_DEFAULT_IRQ 5

enum class SampleFormat : u8 {
    Signed = 0x10,
    Stereo = 0x20,
};

const u16 DSP_READ = 0x22A;
const u16 DSP_WRITE = 0x22C;
const u16 DSP_STATUS = 0x22E;
const u16 DSP_R_ACK = 0x22F;

/* Write a value to the DSP write register */
void SB16::dsp_write(u8 value)
{
    while (IO::in8(DSP_WRITE) & 0x80) {
        ;
    }
    IO::out8(DSP_WRITE, value);
}

/* Reads the value of the DSP read register */
u8 SB16::dsp_read()
{
    while (!(IO::in8(DSP_STATUS) & 0x80)) {
        ;
    }
    return IO::in8(DSP_READ);
}

/* Changes the sample rate of sound output */
void SB16::set_sample_rate(uint16_t hz)
{
    dsp_write(0x41); // output
    dsp_write((u8)(hz >> 8));
    dsp_write((u8)hz);
    dsp_write(0x42); // input
    dsp_write((u8)(hz >> 8));
    dsp_write((u8)hz);
}

static AK::Singleton<SB16> s_the;

SB16::SB16()
    : IRQHandler(SB16_DEFAULT_IRQ)
    , CharacterDevice(42, 42) // ### ?
{
    initialize();
}

SB16::~SB16()
{
}

void SB16::create()
{
    s_the.ensure_instance();
}

SB16& SB16::the()
{
    return *s_the;
}

void SB16::initialize()
{
    disable_irq();

    IO::out8(0x226, 1);
    IO::delay(32);
    IO::out8(0x226, 0);

    auto data = dsp_read();
    if (data != 0xaa) {
        klog() << "SB16: sb not ready";
        return;
    }

    // Get the version info
    dsp_write(0xe1);
    m_major_version = dsp_read();
    auto vmin = dsp_read();

    klog() << "SB16: found version " << m_major_version << "." << vmin;
    set_irq_register(SB16_DEFAULT_IRQ);
    klog() << "SB16: IRQ " << get_irq_line();
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
        ASSERT_NOT_REACHED();
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

bool SB16::can_read(const FileDescription&, size_t) const
{
    return false;
}

KResultOr<size_t> SB16::read(FileDescription&, size_t, UserOrKernelBuffer&, size_t)
{
    return 0;
}

void SB16::dma_start(uint32_t length)
{
    const auto addr = m_dma_region->physical_page(0)->paddr().get();
    const u8 channel = 5; // 16-bit samples use DMA channel 5 (on the master DMA controller)
    const u8 mode = 0x48;

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

    // Enable the DMA channel
    IO::out8(0xd4, (channel % 4));
}

void SB16::handle_irq(const RegisterState&)
{
    // Stop sound output ready for the next block.
    dsp_write(0xd5);

    IO::in8(DSP_STATUS); // 8 bit interrupt
    if (m_major_version >= 4)
        IO::in8(DSP_R_ACK); // 16 bit interrupt

    m_irq_queue.wake_all();
}

void SB16::wait_for_irq()
{
    Thread::current()->wait_on(m_irq_queue, "SB16");
    disable_irq();
}

KResultOr<size_t> SB16::write(FileDescription&, size_t, const UserOrKernelBuffer& data, size_t length)
{
    if (!m_dma_region) {
        auto page = MM.allocate_supervisor_physical_page();
        if (!page)
            return KResult(-ENOMEM);
        auto vmobject = AnonymousVMObject::create_with_physical_page(*page);
        m_dma_region = MM.allocate_kernel_region_with_vmobject(*vmobject, PAGE_SIZE, "SB16 DMA buffer", Region::Access::Write);
        if (!m_dma_region)
            return KResult(-ENOMEM);
    }

#ifdef SB16_DEBUG
    klog() << "SB16: Writing buffer of " << length << " bytes";
#endif
    ASSERT(length <= PAGE_SIZE);
    const int BLOCK_SIZE = 32 * 1024;
    if (length > BLOCK_SIZE) {
        return KResult(-ENOSPC);
    }

    u8 mode = (u8)SampleFormat::Signed | (u8)SampleFormat::Stereo;

    const int sample_rate = 44100;
    set_sample_rate(sample_rate);
    if (!data.read(m_dma_region->vaddr().as_ptr(), length))
        return KResult(-EFAULT);
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
