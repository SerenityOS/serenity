#include "CMOS.h"
#include "IO.h"

namespace CMOS {

BYTE read(BYTE index)
{
    IO::out8(0x70, index);
    return IO::in8(0x71);
}

void write(BYTE index, BYTE data)
{
    IO::out8(0x70, index);
    IO::out8(0x71, data);
}

}
