#include "GraphicsBitmap.h"
#include <AK/kmalloc.h>

RetainPtr<GraphicsBitmap> GraphicsBitmap::create(const Size& size)
{
    return adopt(*new GraphicsBitmap(size));
}

GraphicsBitmap::GraphicsBitmap(const Size& size)
    : m_size(size)
{
    m_data = (byte*)kmalloc(size.width() * size.height() * 4);
}

GraphicsBitmap::~GraphicsBitmap()
{
    kfree(m_data);
}

dword* GraphicsBitmap::scanline(int y)
{
    unsigned pitch = m_size.width() * 4;
    return (dword*)(((byte*)m_data) + (y * pitch));
}

