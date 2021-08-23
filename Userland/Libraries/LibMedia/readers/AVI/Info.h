#pragma once

#include <AK/ByteBuffer.h>

namespace Media::Reader::AVI {
class INFO {
public:
    explicit INFO(ByteBuffer& data);
};

}
