#include <Kernel/Console.h>
#include <Kernel/IO.h>
#include <Kernel/kstdio.h>

// Bytes output to 0xE9 end up on the Bochs console. It's very handy.
#define CONSOLE_OUT_TO_E9

static Console* s_the;

Console& Console::the()
{
    ASSERT(s_the);
    return *s_the;
}

Console::Console()
    : CharacterDevice(5, 1)
{
    s_the = this;
}

Console::~Console()
{
}

bool Console::can_read(FileDescription&) const
{
    return false;
}

ssize_t Console::read(FileDescription&, u8*, ssize_t)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    return 0;
}

ssize_t Console::write(FileDescription&, const u8* data, ssize_t size)
{
    if (!size)
        return 0;
    if (!m_implementation)
        return 0;
    for (ssize_t i = 0; i < size; ++i)
        put_char(data[i]);
    return size;
}

void Console::put_char(char ch)
{
#ifdef CONSOLE_OUT_TO_E9
    //if (ch != 27)
    IO::out8(0xe9, ch);
#endif
    m_logbuffer.enqueue(ch);
    if (m_implementation)
        m_implementation->on_sysconsole_receive(ch);
}

ConsoleImplementation::~ConsoleImplementation()
{
}
