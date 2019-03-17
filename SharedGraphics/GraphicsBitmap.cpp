#include "GraphicsBitmap.h"
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <stdio.h>

Retained<GraphicsBitmap> GraphicsBitmap::create(Format format, const Size& size)
{
    return adopt(*new GraphicsBitmap(format, size));
}

GraphicsBitmap::GraphicsBitmap(Format format, const Size& size)
    : m_size(size)
    , m_pitch(size.width() * sizeof(RGBA32))
    , m_format(format)
{
    size_t size_in_bytes = size.area() * sizeof(RGBA32);
    m_data = (RGBA32*)mmap(nullptr, size_in_bytes, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    ASSERT(m_data && m_data != (void*)-1);
    set_mmap_name(m_data, size_in_bytes, String::format("GraphicsBitmap [%dx%d]", width(), height()).characters());
    m_mmaped = true;
}

Retained<GraphicsBitmap> GraphicsBitmap::create_wrapper(Format format, const Size& size, RGBA32* data)
{
    return adopt(*new GraphicsBitmap(format, size, data));
}

RetainPtr<GraphicsBitmap> GraphicsBitmap::load_from_file(Format format, const String& path, const Size& size)
{
    int fd = open(path.characters(), O_RDONLY, 0644);
    if (fd < 0) {
        dbgprintf("open(%s) got fd=%d, failed: %s\n", path.characters(), fd, strerror(errno));
        perror("open");
        return nullptr;
    }

    auto* mapped_data = (RGBA32*)mmap(nullptr, size.area() * 4, PROT_READ, MAP_SHARED, fd, 0);
    if (mapped_data == MAP_FAILED) {
        int rc = close(fd);
        ASSERT(rc == 0);
        return nullptr;
    }

    int rc = close(fd);
    ASSERT(rc == 0);
    auto bitmap = create_wrapper(format, size, mapped_data);
    bitmap->m_mmaped = true;
    return bitmap;
}

GraphicsBitmap::GraphicsBitmap(Format format, const Size& size, RGBA32* data)
    : m_size(size)
    , m_data(data)
    , m_pitch(size.width() * sizeof(RGBA32))
    , m_format(format)
{
}

Retained<GraphicsBitmap> GraphicsBitmap::create_with_shared_buffer(Format format, Retained<SharedBuffer>&& shared_buffer, const Size& size)
{
    return adopt(*new GraphicsBitmap(format, move(shared_buffer), size));
}

GraphicsBitmap::GraphicsBitmap(Format format, Retained<SharedBuffer>&& shared_buffer, const Size& size)
    : m_size(size)
    , m_data((RGBA32*)shared_buffer->data())
    , m_pitch(size.width() * sizeof(RGBA32))
    , m_format(format)
    , m_shared_buffer(move(shared_buffer))
{
}

GraphicsBitmap::~GraphicsBitmap()
{
    if (m_mmaped) {
        int rc = munmap(m_data, m_size.area() * 4);
        ASSERT(rc == 0);
    }
    m_data = nullptr;
}

