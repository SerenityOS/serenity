#include <Kernel/Devices/SB16.h>
#include <Kernel/IO.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>

//#define SB16_DEBUG

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

static SB16* s_the;

SB16::SB16()
    : IRQHandler(5)
    , CharacterDevice(42, 42) // ### ?
{
    s_the = this;
    initialize();
}

SB16::~SB16()
{
}

SB16& SB16::the()
{
    return *s_the;
}

void SB16::initialize()
{
    IO::out8(0x226, 1);
    IO::delay();
    IO::out8(0x226, 0);

    auto data = dsp_read();
    if (data != 0xaa) {
        kprintf("SB16: sb not ready");
        return;
    }

    // Get the version info
    dsp_write(0xe1);
    m_major_version = dsp_read();
    auto vmin = dsp_read();

    kprintf("SB16: found version %d.%d\n", m_major_version, vmin);
    enable_irq();
}

bool SB16::can_read(const FileDescription&) const
{
    return false;
}

ssize_t SB16::read(FileDescription&, u8*, ssize_t)
{
    return 0;
}

void SB16::dma_start(uint32_t length)
{
    const auto addr = m_dma_buffer_page->paddr().get();
    const u8 channel = 5; // 16-bit samples use DMA channel 5 (on the master DMA controller)
    const u8 mode = 0;

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

void SB16::handle_irq()
{
    // Stop sound output ready for the next block.
    dsp_write(0xd5);

    IO::in8(DSP_STATUS); // 8 bit interrupt
    if (m_major_version >= 4)
        IO::in8(DSP_R_ACK); // 16 bit interrupt

    m_interrupted = true;
}

void SB16::wait_for_irq()
{
    // Well, we have no way of knowing how much got written. So just hope all of
    // it did, even if we're interrupted.
    (void)current->block_until("Interrupting", [this] {
        return m_interrupted;
    });
}

ssize_t SB16::write(FileDescription&, const u8* data, ssize_t length)
{
    if (!m_dma_buffer_page)
        m_dma_buffer_page = MM.allocate_supervisor_physical_page();

#ifdef SB16_DEBUG
    kprintf("SB16: Writing buffer of %d bytes\n", length);
#endif
    ASSERT(length <= PAGE_SIZE);
    const int BLOCK_SIZE = 32 * 1024;
    if (length > BLOCK_SIZE) {
        return -ENOSPC;
    }

    u8 mode = (u8)SampleFormat::Signed | (u8)SampleFormat::Stereo;

    disable_irq();
    const int sample_rate = 44100;
    set_sample_rate(sample_rate);
    memcpy(m_dma_buffer_page->paddr().as_ptr(), data, length);
    dma_start(length);

    // 16-bit single-cycle output.
    // FIXME: Implement auto-initialized output.
    u8 command = 0xb0;

    u16 sample_count = length / sizeof(i16);
    if (mode & (u8)SampleFormat::Stereo)
        sample_count /= 2;

    sample_count -= 1;

    dsp_write(command);
    dsp_write(mode);
    dsp_write((u8)sample_count);
    dsp_write((u8)(sample_count >> 8));

    m_interrupted = false;
    enable_irq();
    wait_for_irq();
    return length;
}
