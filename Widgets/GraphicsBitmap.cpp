#include "GraphicsBitmap.h"
#include <AK/kmalloc.h>

RetainPtr<GraphicsBitmap> GraphicsBitmap::create(const Size& size)
{
    return adopt(*new GraphicsBitmap(size));
}

RetainPtr<GraphicsBitmap> GraphicsBitmap::create_wrapper(const Size& size, byte* data)
{
    return adopt(*new GraphicsBitmap(size, data));
}

GraphicsBitmap::GraphicsBitmap(const Size& size)
    : m_size(size)
{
    m_data = (byte*)kmalloc(size.width() * size.height() * 4);
    m_owned = true;
}

GraphicsBitmap::GraphicsBitmap(const Size& size, byte* data)
    : m_size(size)
{
    m_data = data;
    m_owned = false;
}

GraphicsBitmap::~GraphicsBitmap()
{
    if (m_owned)
        kfree(m_data);
    m_data = nullptr;
}

dword* GraphicsBitmap::scanline(int y)
{
    unsigned pitch = m_size.width() * 4;
    return (dword*)(((byte*)m_data) + (y * pitch));
}

