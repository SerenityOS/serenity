#include "PS2MouseDevice.h"
#include "IO.h"

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
    byte data = IO::in8(0x60);
    m_data[m_data_state] = data;
    switch (m_data_state) {
    case 0:
        ASSERT(data & 0x08);
        ++m_data_state;
        break;
    case 1:
        ++m_data_state;
        break;
    case 2:
        m_data_state = 0;
#ifdef PS2MOUSE_DEBUG
        dbgprintf("PS2Mouse: %d, %d %s %s\n",
            m_data[1],
            m_data[2],
            (m_data[0] & 1) ? "Left" : "",
            (m_data[0] & 2) ? "Right" : ""
        );
#endif
        m_queue.enqueue(m_data[0]);
        m_queue.enqueue(m_data[1]);
        m_queue.enqueue(m_data[2]);
        break;
    }
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

    byte status = wait_then_read(0x60) | 2;
    wait_then_write(0x64, 0x60);
    wait_then_write(0x60, status);

    // Set default settings.
    mouse_write(0xf6);
    byte ack1 = mouse_read();
    ASSERT(ack1 == 0xfa);

    // Enable.
    mouse_write(0xf4);
    byte ack2 = mouse_read();
    ASSERT(ack2 == 0xfa);

    enable_irq();
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

bool PS2MouseDevice::can_read(Process&) const
{
    return !m_queue.is_empty();
}

ssize_t PS2MouseDevice::read(Process&, byte* buffer, size_t size)
{
    ssize_t nread = 0;
    while ((size_t)nread < size) {
        if (m_queue.is_empty())
            break;
        buffer[nread++] = m_queue.dequeue();
    }
    return nread;
}

ssize_t PS2MouseDevice::write(Process&, const byte*, size_t)
{
    return 0;
}
