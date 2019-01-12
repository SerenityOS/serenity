#include "GraphicsBitmap.h"
#include <AK/kmalloc.h>

RetainPtr<GraphicsBitmap> GraphicsBitmap::create(const Size& size)
{
    return adopt(*new GraphicsBitmap(size));
}

RetainPtr<GraphicsBitmap> GraphicsBitmap::create_wrapper(const Size& size, RGBA32* data)
{
    return adopt(*new GraphicsBitmap(size, data));
}

GraphicsBitmap::GraphicsBitmap(const Size& size)
    : m_size(size)
{
    m_data = static_cast<RGBA32*>(kmalloc(size.width() * size.height() * sizeof(RGBA32)));
    memset(m_data, 0, size.width() * size.height() * sizeof(RGBA32));
    m_owned = true;
}

GraphicsBitmap::GraphicsBitmap(const Size& size, RGBA32* data)
    : m_size(size)
    , m_data(data)
    , m_owned(false)
{
}

GraphicsBitmap::~GraphicsBitmap()
{
    if (m_owned)
        kfree(m_data);
    m_data = nullptr;
}

RGBA32* GraphicsBitmap::scanline(int y)
{
    unsigned pitch = m_size.width() * sizeof(RGBA32);
    return reinterpret_cast<RGBA32*>((((byte*)m_data) + (y * pitch)));
}

const RGBA32* GraphicsBitmap::scanline(int y) const
{
    unsigned pitch = m_size.width() * sizeof(RGBA32);
    return reinterpret_cast<RGBA32*>((((byte*)m_data) + (y * pitch)));
}
