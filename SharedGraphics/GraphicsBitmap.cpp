#include "GraphicsBitmap.h"
#include <AK/kmalloc.h>

#ifdef KERNEL
#include <Kernel/Process.h>
#include <Kernel/MemoryManager.h>
#include <WindowServer/WSEventLoop.h>
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
    m_client_region = process.allocate_region_with_vmo(LinearAddress(), size_in_bytes, vmo.copyRef(), 0, "GraphicsBitmap (client)", true, true);
    m_client_region->set_shared(true);
    m_client_region->commit();

    {
        auto& server = WSEventLoop::the().server_process();
        InterruptDisabler disabler;
        m_server_region = server.allocate_region_with_vmo(LinearAddress(), size_in_bytes, move(vmo), 0, "GraphicsBitmap (server)", true, false);
        m_server_region->set_shared(true);
    }
    m_data = (RGBA32*)m_server_region->laddr().as_ptr();
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
        WSEventLoop::the().server_process().deallocate_region(*m_server_region);
#endif
    m_data = nullptr;
}

