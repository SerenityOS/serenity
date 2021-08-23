#include <LibMedia/Manip.h>
#include <iostream>

namespace Media {

AK::String u32_to_fourcc(u32 const val)
{
    // Little-endian
    char const fcc[5] = { static_cast<char>(val),
        static_cast<char>(val >> 8),
        static_cast<char>(val >> 16),
        static_cast<char>(val >> 24),
        '\0' };
    return AK::String(fcc);
}

u16 bytes_to_u16le(ByteBuffer const& bytes)
{
    VERIFY(bytes.size() >= 2);
    return (bytes[1] << 8) | bytes[0];
}

u32 bytes_to_u32le(ByteBuffer const& bytes)
{
    VERIFY(bytes.size() >= 4);
    return (bytes[3] << 24) | (bytes[2] << 16) | (bytes[1] << 8) | bytes[0];
}

u32 bytes_u32_to_fourcc(ByteBuffer const& bytes)
{
    return bytes_to_u32le(bytes);
}

}
 