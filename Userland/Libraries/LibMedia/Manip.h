#pragma once

#include <AK/String.h>
#include <AK/Types.h>

namespace Media {

constexpr u32 fourcc_to_u32(char const vals[4])
{
    return ((vals[3] << 24) | (vals[2] << 16) | (vals[1] << 8) | vals[0]);
}

AK::String u32_to_fourcc(u32 const val);

u16 bytes_to_u16le(ByteBuffer const& bytes);
u32 bytes_to_u32le(ByteBuffer const& bytes);
u32 bytes_to_u32be(ByteBuffer const& bytes);
u32 bytes_u32_to_fourcc(ByteBuffer const& bytes);

}