#include "Console.h"
#include "VGA.h"
#include "IO.h"
#include "kprintf.h"

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

bool Console::hasDataAvailableForRead() const
{
    return false;
}

ssize_t Console::read(byte*, size_t)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    return 0;
}

ssize_t Console::write(const byte* data, size_t size)
{
    if (!size)
        return 0;
    if (!m_implementation)
        return 0;
    for (size_t i = 0; i < size; ++i)
        putChar(data[i]);
    return 0;
}

void Console::putChar(char ch)
{
#ifdef CONSOLE_OUT_TO_E9
    //if (ch != 27)
    IO::out8(0xe9, ch);
#endif
    if (m_implementation)
        m_implementation->onConsoleReceive(ch);
}

ConsoleImplementation::~ConsoleImplementation()
{
}
