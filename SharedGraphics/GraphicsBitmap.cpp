#include "GraphicsBitmap.h"

#ifdef KERNEL
#include <Kernel/Process.h>
#include <Kernel/MemoryManager.h>
#include <WindowServer/WSMessageLoop.h>
#endif

#ifdef USERLAND
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <stdio.h>
#endif

#ifdef KERNEL
RetainPtr<GraphicsBitmap> GraphicsBitmap::create(Process& process, const Size& size)
{
    return adopt(*new GraphicsBitmap(process, size));
}

GraphicsBitmap::GraphicsBitmap(Process& process, const Size& size)
    : m_size(size)
    , m_pitch(size.width() * sizeof(RGBA32))
    , m_client_process(process.make_weak_ptr())
{
    InterruptDisabler disabler;
    size_t size_in_bytes = size.width() * size.height() * sizeof(RGBA32);
    auto vmo = VMObject::create_anonymous(size_in_bytes);
    m_client_region = process.allocate_region_with_vmo(LinearAddress(), size_in_bytes, vmo.copy_ref(), 0, "GraphicsBitmap (client)", true, true);
    m_client_region->set_shared(true);
    m_client_region->set_is_bitmap(true);
    m_client_region->commit();
    auto& server = WSMessageLoop::the().server_process();
    m_server_region = server.allocate_region_with_vmo(LinearAddress(), size_in_bytes, move(vmo), 0, "GraphicsBitmap (server)", true, false);
    m_server_region->set_shared(true);
    m_server_region->set_is_bitmap(true);

    m_data = (RGBA32*)m_server_region->laddr().as_ptr();
}
#endif

RetainPtr<GraphicsBitmap> GraphicsBitmap::create_wrapper(const Size& size, RGBA32* data)
{
    return adopt(*new GraphicsBitmap(size, data));
}

RetainPtr<GraphicsBitmap> GraphicsBitmap::load_from_file(const String& path, const Size& size)
{
#ifdef USERLAND
    int fd = open(path.characters(), O_RDONLY, 0644);
    if (fd < 0) {
        dbgprintf("open(%s) got fd=%d, failed: %s\n", path.characters(), fd, strerror(errno));
        perror("open");
        return nullptr;
    }

    auto* mapped_file = (RGBA32*)mmap(nullptr, size.area() * 4, PROT_READ, MAP_SHARED, fd, 0);
    if (mapped_file == MAP_FAILED) {
        int rc = close(fd);
        ASSERT(rc == 0);
        return nullptr;
    }
#else
    int error;
    auto descriptor = VFS::the().open(path, error, 0, 0, *VFS::the().root_inode());
    if (!descriptor) {
        kprintf("Failed to load GraphicsBitmap from file (%s)\n", path.characters());
        ASSERT_NOT_REACHED();
    }
    auto* region = WSMessageLoop::the().server_process().allocate_file_backed_region(LinearAddress(), size.area() * 4, descriptor->inode(), ".rgb file", /*readable*/true, /*writable*/false);
    auto* mapped_file = (RGBA32*)region->laddr().get();
#endif


#ifdef USERLAND
    int rc = close(fd);
    ASSERT(rc == 0);
#endif
    auto bitmap = create_wrapper(size, mapped_file);
#ifdef KERNEL
    bitmap->m_server_region = region;
#else
    bitmap->m_mmaped = true;
#endif

    return bitmap;
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
    if (m_client_region && m_client_process)
        m_client_process->deallocate_region(*m_client_region);
    if (m_server_region)
        WSMessageLoop::the().server_process().deallocate_region(*m_server_region);
#else
    if (m_mmaped) {
        int rc = munmap(m_data, m_size.area() * 4);
        ASSERT(rc == 0);
    }
#endif
    m_data = nullptr;
}

