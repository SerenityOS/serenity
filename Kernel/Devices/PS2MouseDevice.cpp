#include "PS2MouseDevice.h"
#include "IO.h"

#define IRQ_MOUSE                1
#define I8042_BUFFER             0x60
#define I8042_STATUS             0x64
#define I8042_ACK                0xFA
#define I8042_BUFFER_FULL        0x01
#define I8042_WHICH_BUFFER       0x20
#define I8042_MOUSE_BUFFER       0x20
#define I8042_KEYBOARD_BUFFER    0x00

#define PS2MOUSE_GET_DEVICE_ID   0xF2
#define PS2MOUSE_SET_SAMPLE_RATE 0xF3

#define PS2MOUSE_INTELLIMOUSE_ID 0x03

//#define PS2MOUSE_DEBUG

static PS2MouseDevice* s_the;

PS2MouseDevice::PS2MouseDevice()
    : IRQHandler(12)
    , CharacterDevice(10, 1)
{
    s_the = this;
    initialize();
}

PS2MouseDevice::~PS2MouseDevice()
{
}

PS2MouseDevice& PS2MouseDevice::the()
{
    return *s_the;
}

void PS2MouseDevice::handle_irq()
{
    for (;;) {
        byte status = IO::in8(I8042_STATUS);
        if (!(((status & I8042_WHICH_BUFFER) == I8042_MOUSE_BUFFER) && (status & I8042_BUFFER_FULL)))
            return;

        byte data = IO::in8(I8042_BUFFER);
        m_data[m_data_state] = data;

        auto commit_packet = [&] {
            m_data_state = 0;
#ifdef PS2MOUSE_DEBUG
            dbgprintf("PS2Mouse: %d, %d %s %s (buffered: %u)\n",
                m_data[1],
                m_data[2],
                (m_data[0] & 1) ? "Left" : "",
                (m_data[0] & 2) ? "Right" : "",
                m_queue.size()
            );
#endif
            parse_data_packet();
        };

        switch (m_data_state) {
        case 0:
            if (!(data & 0x08)) {
                dbgprintf("PS2Mouse: Stream out of sync.\n");
                break;
            }
            ++m_data_state;
            break;
        case 1:
            ++m_data_state;
            break;
        case 2:
            if (m_has_wheel) {
                ++m_data_state;
                break;
            }
            commit_packet();
            break;
        case 3:
            ASSERT(m_has_wheel);
            commit_packet();
            break;
        }
    }
}

void PS2MouseDevice::parse_data_packet()
{
    int x = m_data[1];
    int y = m_data[2];
    int z = 0;
    if (m_has_wheel)
        z = (char)m_data[3];
    bool x_overflow = m_data[0] & 0x40;
    bool y_overflow = m_data[0] & 0x80;
    bool x_sign = m_data[0] & 0x10;
    bool y_sign = m_data[0] & 0x20;
    if (x && x_sign)
        x -= 0x100;
    if (y && y_sign)
        y -= 0x100;
    if (x_overflow || y_overflow) {
        x = 0;
        y = 0;
    }
    MousePacket packet;
    packet.dx = x;
    packet.dy = y;
    packet.dz = z;
    packet.buttons = m_data[0] & 0x07;

    m_queue.enqueue(packet);
}

void PS2MouseDevice::wait_then_write(byte port, byte data)
{
    prepare_for_output();
    IO::out8(port, data);
}

byte PS2MouseDevice::wait_then_read(byte port)
{
    prepare_for_input();
    return IO::in8(port);
}

void PS2MouseDevice::initialize()
{
    // Enable PS aux port
    wait_then_write(0x64, 0xa8);

    // Enable interrupts
    wait_then_write(0x64, 0x20);

    // Enable the PS/2 mouse IRQ (12).
    // NOTE: The keyboard uses IRQ 1 (and is enabled by bit 0 in this register).
    byte status = wait_then_read(0x60) | 2;
    wait_then_write(0x64, 0x60);
    wait_then_write(0x60, status);

    // Set default settings.
    mouse_write(0xf6);
    expect_ack();

    // Enable.
    mouse_write(0xf4);
    expect_ack();

    mouse_write(PS2MOUSE_GET_DEVICE_ID);
    expect_ack();
    byte device_id = mouse_read();

    if (device_id != PS2MOUSE_INTELLIMOUSE_ID) {
        // Send magical wheel initiation sequence.
        mouse_write(PS2MOUSE_SET_SAMPLE_RATE);
        expect_ack();
        mouse_write(200);
        expect_ack();
        mouse_write(PS2MOUSE_SET_SAMPLE_RATE);
        expect_ack();
        mouse_write(100);
        expect_ack();
        mouse_write(PS2MOUSE_SET_SAMPLE_RATE);
        expect_ack();
        mouse_write(80);
        expect_ack();

        mouse_write(PS2MOUSE_GET_DEVICE_ID);
        expect_ack();
        device_id = mouse_read();
    }

    if (device_id == PS2MOUSE_INTELLIMOUSE_ID) {
        m_has_wheel = true;
        kprintf("PS2MouseDevice: Mouse wheel enabled!\n");
    } else {
        kprintf("PS2MouseDevice: No mouse wheel detected!\n");
    }

    enable_irq();
}

void PS2MouseDevice::expect_ack()
{
    byte data = mouse_read();
    ASSERT(data == I8042_ACK);
}

void PS2MouseDevice::prepare_for_input()
{
    for (;;) {
        if (IO::in8(0x64) & 1)
            return;
    }
}

void PS2MouseDevice::prepare_for_output()
{
    for (;;) {
        if (!(IO::in8(0x64) & 2))
            return;
    }
}

void PS2MouseDevice::mouse_write(byte data)
{
    prepare_for_output();
    IO::out8(0x64, 0xd4);
    prepare_for_output();
    IO::out8(0x60, data);
}

byte PS2MouseDevice::mouse_read()
{
    prepare_for_input();
    return IO::in8(0x60);
}

bool PS2MouseDevice::can_read(FileDescription&) const
{
    return !m_queue.is_empty();
}

ssize_t PS2MouseDevice::read(FileDescription&, byte* buffer, ssize_t size)
{
    ssize_t nread = 0;
    while (nread < size) {
        if (m_queue.is_empty())
            break;
        // Don't return partial data frames.
        if ((size - nread) < (ssize_t)sizeof(MousePacket))
            break;
        auto packet = m_queue.dequeue();
        memcpy(buffer, &packet, sizeof(MousePacket));
        nread += sizeof(MousePacket);
    }
    return nread;
}

ssize_t PS2MouseDevice::write(FileDescription&, const byte*, ssize_t)
{
    return 0;
}
