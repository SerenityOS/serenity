#include "Console.h"
#include "VGA.h"

static Console* s_the;

Console& Console::the()
{
    return *s_the;
}

Console::Console()
{
    s_the = this;
}

Console::~Console()
{
}

ssize_t Console::read(byte* buffer, size_t bufferSize)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    //        A generalized ring buffer would probably be useful.
    return 0;
}

extern int kprintfFromConsole(const char*, ...);

ssize_t Console::write(const byte* data, size_t size)
{
    if (!size)
        return 0;
    
    for (size_t i = 0; i < size; ++i) {
        kprintfFromConsole("%c", data[i]);
    }
    return 0;
}

