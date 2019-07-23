#include <Kernel/Devices/AdLib.h>
#include <Kernel/IO.h>

#define ADLIB_DEBUG

#define ADLIB_STATUS_PORT 0x388 // r/w
#define ADLIB_DATA_PORT 0x389   // w/o

// Register addresses
#define ADLIB_TEST_LSI 0x01
#define ADLIB_ENABLE_WAVEFORM_CTRL 0x01
#define ADLIB_TIMER1_DATA 0x02
#define ADLIB_TIMER2_DATA 0x03
#define ADLIB_TIMER_CTRL 0x04
#define ADLIB_SPEECH_SYNTH_MODE 0x08
#define ADLIB_KEYBOARD_SPLIT_NOTE 0x08

#define ADLIB_TIMER2_EXPIRE_MASK 0x20  // Set if TIMER2 has expired
#define ADLIB_TIMER1_EXPIRE_MASK 0x40  // Set if TIMER1 has expired
#define ADLIB_TIMER12_EXPIRE_MASK 0x80 // Set if either TIMER1 or TIMER2 has expired

static AdLib* s_the;

AdLib& AdLib::the()
{
    return *s_the;
}

AdLib::AdLib()
    : CharacterDevice(43, 42)
{
    detected = detect();
    s_the = this;
}

AdLib::~AdLib()
{
}

ssize_t AdLib::read(FileDescription&, u8* buffer, ssize_t)
{
    *buffer = read_status();
    return 1;
}

bool AdLib::can_read(FileDescription&) const
{
    return detected;
}

ssize_t AdLib::write(FileDescription&, const u8* buffer, ssize_t size)
{
    if (!size || size != 2 || !detected)
        return 0;

    write_register(buffer[0], buffer[1]);
    return size;
}

bool AdLib::can_write(FileDescription&) const
{
    return detected;
}

void AdLib::write_status(u8 val)
{
    IO::out16(ADLIB_STATUS_PORT, val);
}

void AdLib::write_data(u8 val)
{
    IO::out16(ADLIB_DATA_PORT, val);
}

u8 AdLib::read_status()
{
    return IO::in16(ADLIB_STATUS_PORT);
}

void AdLib::write_register(u8 reg, u8 data)
{
    // After writing to the register port, you must wait twelve cycles before sending the data;
    // after writing the data, eighty-four cycles must elapse before any other sound card
    // operation may be performed.

    // The AdLib manual gives the wait times in microseconds: three point three (3.3)
    // microseconds for the address, and twenty-three (23) microseconds for the data.

    // Apparently this must be a minimum of 12 cycles (or 3.3us in the AdLib manual)
    write_status(reg);
    for (int i = 0; i < 5; i++)
        IO::delay();

    // Apparently this must be a minimum of 84 cycles (or 23us in the AdLib manual)
    write_data(data);
    for (int i = 0; i < 36; i++)
        IO::delay();
}

bool AdLib::detect()
{
    write_register(ADLIB_TIMER_CTRL, 0x60);
    write_register(ADLIB_TIMER_CTRL, 0x80);
    u8 status1 = read_status();
    write_register(ADLIB_TIMER1_DATA, 0xFF);
    write_register(ADLIB_TIMER_CTRL, 0x21);

    // This is approximately 96us
    for (auto i = 0; i < 64; i++)
        IO::delay();

    u8 status2 = read_status();
    write_register(ADLIB_TIMER_CTRL, 0x60);
    write_register(ADLIB_TIMER_CTRL, 0x80);

    status1 &= 0xE0;
    status2 &= 0xE0;

    if (status1 == 0x00 && status2 == 0xC0) {
        kprintf("AdLib: Found an AdLib card!\n");

        // At this point we know that an AdLib card is installed
        // in the user's PC. Let's reset it completely.
        for (auto i = 1; i <= 0xF5; i++) {
            write_register(i, 0x00);
        }

        // Set BIT5 of register1 (WSEnable) so we can use waves other than a sine wave
        write_register(ADLIB_ENABLE_WAVEFORM_CTRL, 0x20);
        return true;
    }

    kprintf("AdLib: No AdLib card detected!\n");
    return false;
}
