#include <AK/MappedFile.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/PNGLoader.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>

NonnullRefPtr<GraphicsBitmap> GraphicsBitmap::create(Format format, const Size& size)
{
    return adopt(*new GraphicsBitmap(format, size));
}

GraphicsBitmap::GraphicsBitmap(Format format, const Size& size)
    : m_size(size)
    , m_pitch(round_up_to_power_of_two(size.width() * sizeof(RGBA32), 16))
    , m_format(format)
{
    if (format == Format::Indexed8)
        m_palette = new RGBA32[256];
    m_data = (RGBA32*)mmap_with_name(nullptr, size_in_bytes(), PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, String::format("GraphicsBitmap [%dx%d]", width(), height()).characters());
    ASSERT(m_data && m_data != (void*)-1);
    m_needs_munmap = true;
}

NonnullRefPtr<GraphicsBitmap> GraphicsBitmap::create_wrapper(Format format, const Size& size, RGBA32* data)
{
    return adopt(*new GraphicsBitmap(format, size, data));
}

RefPtr<GraphicsBitmap> GraphicsBitmap::load_from_file(const StringView& path)
{
    return load_png(path);
}

RefPtr<GraphicsBitmap> GraphicsBitmap::load_from_file(Format format, const StringView& path, const Size& size)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;
    return adopt(*new GraphicsBitmap(format, size, move(mapped_file)));
}

GraphicsBitmap::GraphicsBitmap(Format format, const Size& size, RGBA32* data)
    : m_size(size)
    , m_data(data)
    , m_pitch(round_up_to_power_of_two(size.width() * sizeof(RGBA32), 16))
    , m_format(format)
{
    ASSERT(format != Format::Indexed8);
}

GraphicsBitmap::GraphicsBitmap(Format format, const Size& size, MappedFile&& mapped_file)
    : m_size(size)
    , m_data((RGBA32*)mapped_file.pointer())
    , m_pitch(round_up_to_power_of_two(size.width() * sizeof(RGBA32), 16))
    , m_format(format)
    , m_mapped_file(move(mapped_file))
{
    ASSERT(format != Format::Indexed8);
}

NonnullRefPtr<GraphicsBitmap> GraphicsBitmap::create_with_shared_buffer(Format format, NonnullRefPtr<SharedBuffer>&& shared_buffer, const Size& size)
{
    return adopt(*new GraphicsBitmap(format, move(shared_buffer), size));
}

GraphicsBitmap::GraphicsBitmap(Format format, NonnullRefPtr<SharedBuffer>&& shared_buffer, const Size& size)
    : m_size(size)
    , m_data((RGBA32*)shared_buffer->data())
    , m_pitch(round_up_to_power_of_two(size.width() * sizeof(RGBA32), 16))
    , m_format(format)
    , m_shared_buffer(move(shared_buffer))
{
    ASSERT(format != Format::Indexed8);
}

GraphicsBitmap::~GraphicsBitmap()
{
    if (m_needs_munmap) {
        int rc = munmap(m_data, size_in_bytes());
        ASSERT(rc == 0);
    }
    m_data = nullptr;
    delete[] m_palette;
}

void GraphicsBitmap::set_mmap_name(const StringView& name)
{
    ASSERT(m_needs_munmap);
    ::set_mmap_name(m_data, size_in_bytes(), String(name).characters());
}

void GraphicsBitmap::fill(Color color)
{
    ASSERT(m_format == GraphicsBitmap::Format::RGB32 || m_format == GraphicsBitmap::Format::RGBA32);
    for (int y = 0; y < height(); ++y) {
        auto* scanline = this->scanline(y);
        fast_u32_fill(scanline, color.value(), width());
    }
}
