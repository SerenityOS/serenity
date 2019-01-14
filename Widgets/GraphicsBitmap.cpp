#include "GraphicsBitmap.h"
#include "EventLoop.h"
#include <AK/kmalloc.h>

#ifdef KERNEL
#include "Process.h"
#include "MemoryManager.h"
#endif

#ifdef KERNEL
RetainPtr<GraphicsBitmap> GraphicsBitmap::create(Process& process, const Size& size)
{
    return adopt(*new GraphicsBitmap(process, size));
}

GraphicsBitmap::GraphicsBitmap(Process& process, const Size& size)
    : m_size(size)
    , m_pitch(size.width() * sizeof(RGBA32))
    , m_client_process(&process)
{
    size_t size_in_bytes = size.width() * size.height() * sizeof(RGBA32);
    auto vmo = VMObject::create_anonymous(size_in_bytes);
    m_client_region = process.allocate_region_with_vmo(LinearAddress(), size_in_bytes, vmo.copyRef(), 0, "GraphicsBitmap (shared)", true, true);
    m_client_region->commit(process);

    {
        auto& server = EventLoop::main().server_process();
        ProcessInspectionHandle composer_handle(server);
        m_server_region = server.allocate_region_with_vmo(LinearAddress(), size_in_bytes, move(vmo), 0, "GraphicsBitmap (shared)", true, true);

        process.dumpRegions();
        server.dumpRegions();
    }
    m_data = (RGBA32*)m_server_region->linearAddress.asPtr();
}
#endif

RetainPtr<GraphicsBitmap> GraphicsBitmap::create_wrapper(const Size& size, RGBA32* data)
{
    return adopt(*new GraphicsBitmap(size, data));
}

GraphicsBitmap::GraphicsBitmap(const Size& size, RGBA32* data)
    : m_size(size)
    , m_data(data)
    , m_pitch(size.width() * sizeof(RGBA32))
{
}

GraphicsBitmap::~GraphicsBitmap()
{
#ifdef KERNEL
    if (m_client_region)
        m_client_process->deallocate_region(*m_client_region);
    if (m_server_region)
        EventLoop::main().server_process().deallocate_region(*m_server_region);
#endif
    m_data = nullptr;
}

RGBA32* GraphicsBitmap::scanline(int y)
{
    return reinterpret_cast<RGBA32*>((((byte*)m_data) + (y * m_pitch)));
}

const RGBA32* GraphicsBitmap::scanline(int y) const
{
    return reinterpret_cast<const RGBA32*>((((const byte*)m_data) + (y * m_pitch)));
}
