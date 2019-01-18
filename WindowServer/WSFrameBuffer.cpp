#include "WSFrameBuffer.h"
#include <Widgets/GraphicsBitmap.h>
#include <AK/Assertions.h>

WSFrameBuffer* s_the;

void WSFrameBuffer::initialize()
{
    s_the = nullptr;
}

WSFrameBuffer& WSFrameBuffer::the()
{
    ASSERT(s_the);
    return *s_the;
}

WSFrameBuffer::WSFrameBuffer(RGBA32* data, unsigned width, unsigned height)
    : WSScreen(width, height)
    , m_data(data)
{
    ASSERT(!s_the);
    s_the = this;
}


WSFrameBuffer::~WSFrameBuffer()
{
}

RGBA32* WSFrameBuffer::scanline(int y)
{
    unsigned pitch = sizeof(RGBA32) * width();
    return reinterpret_cast<RGBA32*>(((byte*)m_data) + (y * pitch));
}
