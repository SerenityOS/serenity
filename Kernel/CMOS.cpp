#include <Kernel/CMOS.h>
#include <Kernel/IO.h>

namespace CMOS {

byte read(byte index)
{
    IO::out8(0x70, index);
    return IO::in8(0x71);
}

void write(byte index, byte data)
{
    IO::out8(0x70, index);
    IO::out8(0x71, data);
}

}
