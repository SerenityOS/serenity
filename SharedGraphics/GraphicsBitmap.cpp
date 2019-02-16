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
RetainPtr<GraphicsBitmap> GraphicsBitmap::create_kernel_only(const Size& size)
{
    return adopt(*new GraphicsBitmap(size));
}

GraphicsBitmap::GraphicsBitmap(const Size& size)
    : m_size(size)
    , m_pitch(size.width() * sizeof(RGBA32))
{
    InterruptDisabler disabler;
    size_t size_in_bytes = size.width() * size.height() * sizeof(RGBA32);
    auto vmo = VMObject::create_anonymous(size_in_bytes);
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
    RGBA32* mapped_data = nullptr;
#ifdef USERLAND
    int fd = open(path.characters(), O_RDONLY, 0644);
    if (fd < 0) {
        dbgprintf("open(%s) got fd=%d, failed: %s\n", path.characters(), fd, strerror(errno));
        perror("open");
        return nullptr;
    }

    mapped_data = (RGBA32*)mmap(nullptr, size.area() * 4, PROT_READ, MAP_SHARED, fd, 0);
    if (mapped_data == MAP_FAILED) {
        int rc = close(fd);
        ASSERT(rc == 0);
        return nullptr;
    }
#else
    int error;
    auto descriptor = VFS::the().open(path, error, 0, 0, *VFS::the().root_inode());
    if (!descriptor) {
        kprintf("Failed to load GraphicsBitmap from file (%s)\n", path.characters());
        return nullptr;
    }
    auto* region = WSMessageLoop::the().server_process().allocate_file_backed_region(LinearAddress(), size.area() * 4, descriptor->inode(), ".rgb file", /*readable*/true, /*writable*/false);
    mapped_data = (RGBA32*)region->laddr().get();
#endif


#ifdef USERLAND
    int rc = close(fd);
    ASSERT(rc == 0);
#endif
    auto bitmap = create_wrapper(size, mapped_data);
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

RetainPtr<GraphicsBitmap> GraphicsBitmap::create_with_shared_buffer(int shared_buffer_id, const Size& size, RGBA32* data)
{
    if (!data) {
#ifdef KERNEL
        void* shared_buffer = current->sys$get_shared_buffer(shared_buffer_id);
#else
        void* shared_buffer = get_shared_buffer(shared_buffer_id);
#endif
        if (!shared_buffer || shared_buffer == (void*)-1)
            return nullptr;
        data = (RGBA32*)shared_buffer;
    }
    return adopt(*new GraphicsBitmap(shared_buffer_id, size, data));
}

GraphicsBitmap::GraphicsBitmap(int shared_buffer_id, const Size& size, RGBA32* data)
    : m_size(size)
    , m_data(data)
    , m_pitch(size.width() * sizeof(RGBA32))
    , m_shared_buffer_id(shared_buffer_id)
{
}

GraphicsBitmap::~GraphicsBitmap()
{
#ifdef KERNEL
    if (m_server_region)
        WSMessageLoop::the().server_process().deallocate_region(*m_server_region);
#else
    if (m_mmaped) {
        int rc = munmap(m_data, m_size.area() * 4);
        ASSERT(rc == 0);
    }
#endif
    if (m_shared_buffer_id != -1) {
        int rc;
#ifdef KERNEL
        rc = current->sys$release_shared_buffer(m_shared_buffer_id);
#else
        rc = release_shared_buffer(m_shared_buffer_id);
#endif
        ASSERT(rc == 0);
    }
    m_data = nullptr;
}

