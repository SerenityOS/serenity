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
#include <AK/ScopeGuard.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/SB16.h>
#include <Kernel/IO.h>
#include <Kernel/Thread.h>
#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>

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
}

static AK::Singleton<SB16> s_the;
static const Audio::PCM::SampleFormat s_supported_formats[] = { Audio::PCM::SampleFormat::S16_LE, Audio::PCM::SampleFormat::U16_LE, Audio::PCM::SampleFormat::Unknown };
static const Audio::PCM::SampleLayout s_supported_layouts[] = { Audio::PCM::SampleLayout::Interleaved, Audio::PCM::SampleLayout::Unknown };
static const unsigned s_supported_rates[] = { 44100, 48000, 0 };
static const unsigned s_supported_channels[] = { 1, 2 };

UNMAP_AFTER_INIT SB16::SB16()
    : IRQHandler(SB16_DEFAULT_IRQ)
    , AudioDevice(42, 42) // ### ?
{
    m_stream_private.resize(1);
    m_streams.append(Stream {
        .name = "speaker",
        .private_data = (void*)&m_stream_private[0],
        .type = Audio::StreamType::Playback,
        .supported = {
            .formats = s_supported_formats,
            .layouts = s_supported_layouts,
            .rates = s_supported_rates,
            .channels = s_supported_channels,
            .periods_min = 2,
            .periods_max = 8,
        },
        .current = {
            .format = Audio::PCM::SampleFormat::S16_LE,
            .layout = Audio::PCM::SampleLayout::Interleaved,
            .rate = 44100,
            .channels = 2,
            .periods = 3,
            .periods_trigger = 2,
        },
        .dma_periods = 2,
    });

    m_streams[0].current.period_ns = calculate_period_time(m_streams[0].current);

    initialize();
}

UNMAP_AFTER_INIT SB16::~SB16()
{
}

UNMAP_AFTER_INIT void SB16::detect()
{
    IO::out8(0x226, 1);
    IO::delay(32);
    IO::out8(0x226, 0);

    auto data = dsp_read();
    if (data != 0xaa) {
        return;
    }
    SB16::create();
}

u64 SB16::calculate_period_time(const CurrentPCM& config)
{
    // We set up a DMA transfer for 2 periods. The size of that is limited.
    // calculate how much time one of these periods equals to
    constexpr size_t maximum_bytes = 65536;
    size_t maximum_frames = maximum_bytes / Audio::PCM::bytes_per_frame(config.format, config.channels);
    maximum_frames /= 2;
    return Audio::PCM::frames_to_time(maximum_frames, config.rate);
}

UNMAP_AFTER_INIT void SB16::create()
{
    s_the.ensure_instance();
}

SB16& SB16::the()
{
    return *s_the;
}

void SB16::write_mixer_reg(u8 index, u8 value)
{
    IO::out8(m_port + 4, index);
    IO::out8(m_port + 5, value);
}

u8 SB16::read_mixer_reg(u8 index) const
{
    IO::out8(m_port + 4, index);
    return IO::in8(m_port + 5);
}

UNMAP_AFTER_INIT void SB16::initialize()
{
    disable_irq();

    IO::out8(m_port + 6, 1);
    IO::delay(32);
    IO::out8(m_port + 6, 0);

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
    set_irq_line(SB16_DEFAULT_IRQ);
    set_dma_channels();
    dmesgln("SB16: IRQ {}", get_irq_line());
}

void SB16::set_irq_register(u8 irq_number)
{
    u8 bitmask;
    switch (irq_number) {
    case 2:
        bitmask = 0b1;
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
    write_mixer_reg(0x80, bitmask);
}

u8 SB16::get_irq_line()
{
    u8 bitmask = read_mixer_reg(0x80);
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

void SB16::set_dma_channels()
{
    write_mixer_reg(0x81, (1 << 5) | (1 << 1)); // 16 bit dma | 8 bit dma
}

bool SB16::can_support_pcm_configuration(Stream&, const CurrentPCM& config)
{
    // TODO: Validate overflows...
    size_t bytes_per_second = Audio::PCM::bytes_per_frame(config.format, config.channels) * config.rate;
    dbgln("SB16 bytes per second: {}", bytes_per_second);
    return true;
}

bool SB16::allocate_dma_region(Stream& stream)
{
    VERIFY(stream.dma_periods > 0);
    VERIFY(stream.bytes_per_period > 0);

    stream.dma_region = nullptr; // free it first
    size_t exact_size = stream.dma_periods * stream.bytes_per_period;
    size_t size = page_round_up(exact_size);
    auto access = stream.type == Audio::StreamType::Playback ? Region::Access::Write : Region::Access::Read;
    if (size > PAGE_SIZE) {
        // Must not cross a 64k boundary and limited to 64k
        stream.dma_region = MM.allocate_contiguous_kernel_region(size, "Audio DMA buffer", access, 64 * KiB);
        if (stream.dma_region) {
            auto paddr_first = stream.dma_region->physical_page(0)->paddr().get();
            VERIFY(!(paddr_first & 0xffff));
            auto paddr_last = stream.dma_region->physical_page(stream.dma_region->size() / PAGE_SIZE - 1)->paddr().get();
            VERIFY((paddr_first & ~0xffff) == (paddr_last & ~0xffff)); // must not cross a 64k boundary
        }
    } else {
        auto page = MM.allocate_supervisor_physical_page();
        if (!page)
            return false;

        auto vmobject = AnonymousVMObject::create_with_physical_page(*page);
        stream.dma_region = MM.allocate_kernel_region_with_vmobject(*vmobject, PAGE_SIZE, "Audio DMA buffer", access);
    }
    dbgln("SB16: Mapped DMA region: {} - {} ({} bytes) split into {} periods with {} bytes each", stream.dma_region->vaddr(), stream.dma_region->vaddr().offset(exact_size), exact_size, stream.dma_periods, stream.bytes_per_period);
    return stream.dma_region;
}

bool SB16::do_initialize(Stream& stream)
{
    if (m_active_stream)
        return false;
    if (!allocate_dma_region(stream))
        return false;

    m_active_stream = &stream;
    m_is16bit = Audio::PCM::bytes_per_sample(stream.current.format) == 2;
    return true;
}

void SB16::trigger_playback(Stream& stream)
{
    dbgln("SB16::trigger_playback");
    VERIFY(m_active_stream);

    u8 mode = 0;
    if (Audio::PCM::is_signed(stream.current.format))
        mode |= (u8)SampleFormat::Signed;
    if (stream.current.channels == 2)
        mode |= (u8)SampleFormat::Stereo;

    // Setup the entire dma buffer
    setup_dma(stream, stream.bytes_per_period * stream.dma_periods);
    set_sample_rate(stream.current.rate);

    // Transfer only one period at a time
    dsp_transfer_block(stream, stream.bytes_per_period);

    enable_irq();
}

void SB16::transferred_to_dma_buffer(Stream& stream, bool is_playing)
{
    auto& priv = stream_private(stream);
    auto& period_counter = is_playing ? priv.periods_read : priv.periods_written;
    period_counter++;
    dbgln("SB16::transferred_to_dma_buffer {}: periods read {} written {}", is_playing ? "playing" : "recording", priv.periods_read, priv.periods_written);
    if (is_playing)
        VERIFY(priv.periods_written - priv.periods_read <= stream.dma_periods);
}

PhysicalAddress SB16::playback_current_dma_period_paddr(Stream& stream, bool is_write)
{
    auto& priv = stream_private(stream);
    size_t offset = ((is_write ? priv.periods_written : priv.periods_read) % stream.dma_periods) * stream.bytes_per_period;
    dbgln("Physical DMA address offset: {} ({})", offset, is_write ? "write" : "read");
    return stream.dma_region->physical_page(0)->paddr().offset(offset);
}

u8* SB16::playback_current_dma_period(Stream& stream, bool is_write)
{
    auto& priv = stream_private(stream);
    size_t offset = ((is_write ? priv.periods_written : priv.periods_read) % stream.dma_periods) * stream.bytes_per_period;
    dbgln("DMA mapped address offset: {} ({}) => {}", offset, is_write ? "write" : "read", stream.dma_region->vaddr().offset(offset));
    return stream.dma_region->vaddr().as_ptr() + offset;
}

void SB16::setup_dma(Stream& stream, u32 length)
{
    const auto addr = playback_current_dma_period_paddr(stream, false).get();
    dbgln("Setup DMA read {} ({} bytes)", VirtualAddress(playback_current_dma_period(stream, false)), length);
    const u8 channel = m_is16bit ? 5 : 1; // 16-bit samples use DMA channel 5 (on the master DMA controller)

    // Disable the DMA channel
    IO::out8(m_is16bit ? 0xd4 : 0x0a, 4 + (channel % 4));

    // Clear the byte pointer flip-flop
    IO::out8(m_is16bit ? 0xd8 : 0x0c, 0);

    // Write the DMA mode for the transfer
    IO::out8(m_is16bit ? 0xd6 : 0x0b, (channel % 4) | 0x58);

    // Write the transfer length
    auto transfer_length = (m_is16bit ? length / 2 : length) - 1;
    IO::out8(m_is16bit ? 0xc6 : 0x03, (u8)transfer_length);
    IO::out8(m_is16bit ? 0xc6 : 0x03, (u8)(transfer_length >> 8));

    // Write the buffer addresss
    auto offset = (m_is16bit ? addr / 2 : addr) % 65536;
    IO::out8(m_is16bit ? 0x8b : 0x83, addr >> 16);
    IO::out8(m_is16bit ? 0xc4 : 0x02, (u8)offset);
    IO::out8(m_is16bit ? 0xc4 : 0x02, (u8)(offset >> 8));

    // Enable the DMA channel
    IO::out8(m_is16bit ? 0xd4 : 0x0a, (channel % 4));
}

void SB16::dsp_transfer_block(Stream& stream, u32 length)
{
    u8 mode = 0;
    if (Audio::PCM::is_signed(stream.current.format))
        mode |= (u8)SampleFormat::Signed;
    if (stream.current.channels == 2)
        mode |= (u8)SampleFormat::Stereo;

    auto sample_count = (m_is16bit ? length / 2 : length) - 1;
    dbgln("SB16: dsp_transfer_block {} samples", sample_count);
    dsp_write(m_is16bit ? 0xb6 : 0xc6);
    dsp_write(mode);
    dsp_write((u8)sample_count);
    dsp_write((u8)(sample_count >> 8));
}

void SB16::handle_irq(const RegisterState&)
{
    u8 irq_status = read_mixer_reg(0x82);
    if (!(irq_status & 3)) {
        dbgln("SB16: ignore irq");
        return;
    }

    ScopedSpinLock lock(m_request_lock);
    dbgln("SB16::handle_irq -->");
    ScopeGuard log_guard([&] {
        dbgln("<--SB16::handle_irq");
    });
    if (m_active_stream) {
        finished_playing_period(*m_active_stream);
        dsp_transfer_block(*m_active_stream, m_active_stream->bytes_per_period);
    } else {
        dsp_write(m_is16bit ? 0xd5 : 0xd0);
    }

    // acknowledge irq
    if (irq_status & 1)
        IO::in8(m_port + 0xe); // ack 8 bit
    if (irq_status & 2)
        IO::in8(m_port + 0xf); // ack 16 bit
}

}
