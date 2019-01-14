#include "FrameBuffer.h"
#include "GraphicsBitmap.h"
#include <AK/Assertions.h>

FrameBuffer* s_the;

void FrameBuffer::initialize()
{
    s_the = nullptr;
}

FrameBuffer& FrameBuffer::the()
{
    ASSERT(s_the);
    return *s_the;
}

FrameBuffer::FrameBuffer(unsigned width, unsigned height)
    : AbstractScreen(width, height)
{
    ASSERT(!s_the);
    s_the = this;
}

FrameBuffer::FrameBuffer(RGBA32* data, unsigned width, unsigned height)
    : AbstractScreen(width, height)
    , m_data(data)
{
    ASSERT(!s_the);
    s_the = this;
}


FrameBuffer::~FrameBuffer()
{
}

void FrameBuffer::show()
{
}

RGBA32* FrameBuffer::scanline(int y)
{
    unsigned pitch = sizeof(RGBA32) * width();
    return reinterpret_cast<RGBA32*>(((byte*)m_data) + (y * pitch));
}
