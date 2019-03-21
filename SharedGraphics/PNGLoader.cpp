#include <SharedGraphics/PNGLoader.h>
#include <Kernel/NetworkOrdered.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <string.h>
#include <SharedGraphics/puff.c>
#include <serenity.h>

struct PNG_IHDR {
    NetworkOrdered<dword> width;
    NetworkOrdered<dword> height;
    byte bit_depth { 0 };
    byte color_type { 0 };
    byte compression_method { 0 };
    byte filter_method { 0 };
    byte interlace_method { 0 };
};

static_assert(sizeof(PNG_IHDR) == 13);

struct Scanline {
    byte filter { 0 };
    ByteBuffer data;
};

struct PNGLoadingContext {
    int width { -1 };
    int height { -1 };
    byte bit_depth { 0 };
    byte color_type { 0 };
    byte compression_method { 0 };
    byte filter_method { 0 };
    byte interlace_method { 0 };
    byte bytes_per_pixel { 0 };
    bool has_seen_zlib_header { false };
    bool has_alpha() const { return color_type & 4; }
    Vector<Scanline> scanlines;
    RetainPtr<GraphicsBitmap> bitmap;
    byte* decompression_buffer { nullptr };
    int decompression_buffer_size { 0 };
    Vector<byte> compressed_data;
};

class Streamer {
public:
    Streamer(const byte* data, int size)
        : m_original_data(data)
        , m_original_size(size)
        , m_data_ptr(data)
        , m_size_remaining(size)
    {
    }

    template<typename T>
    bool read(T& value)
    {
        if (m_size_remaining < sizeof(T))
            return false;
        value = *((NetworkOrdered<T>*)m_data_ptr);
        m_data_ptr += sizeof(T);
        m_size_remaining -= sizeof(T);
        return true;
    }

    bool read_bytes(byte* buffer, int count)
    {
        if (m_size_remaining < count)
            return false;
        memcpy(buffer, m_data_ptr, count);
        m_data_ptr += count;
        m_size_remaining -= count;
        return true;
    }

    bool wrap_bytes(ByteBuffer& buffer, int count)
    {
        if (m_size_remaining < count)
            return false;
        buffer = ByteBuffer::wrap((void*)m_data_ptr, count);
        m_data_ptr += count;
        m_size_remaining -= count;
        return true;
    }

    bool at_end() const { return !m_size_remaining; }

private:
    const byte* m_original_data;
    int m_original_size;
    const byte* m_data_ptr;
    int m_size_remaining;
};

static RetainPtr<GraphicsBitmap> load_png_impl(const byte*, int);
static bool process_chunk(Streamer&, PNGLoadingContext& context);

RetainPtr<GraphicsBitmap> load_png(const String& path)
{
    int fd = open(path.characters(), O_RDONLY);
    if (fd < 0) {
        perror("open");
        return nullptr;
    }

    struct stat st;
    if (fstat(fd, &st) < 0) {
        perror("fstat");
        if (close(fd) < 0)
            perror("close");
        return nullptr;
    }

    if (st.st_size < 8) {
        if (close(fd) < 0)
            perror("close");
        return nullptr;
    }

    auto* mapped_file = (byte*)mmap(nullptr, st.st_size, PROT_READ, MAP_SHARED, fd, 0);
    if (mapped_file == MAP_FAILED) {
        if (close(fd) < 0)
            perror("close");
        return nullptr;
    }

    auto bitmap = load_png_impl(mapped_file, st.st_size);

    if (munmap(mapped_file, st.st_size) < 0)
        perror("munmap");

    if (close(fd) < 0)
        perror("close");

    return bitmap;
}

static byte paeth_predictor(int a, int b, int c)
{
    int p = a + b - c;
    int pa = abs(p - a);
    int pb = abs(p - b);
    int pc = abs(p - c);
    if (pa <= pb && pa <= pc)
        return a;
    if (pb <= pc)
        return b;
    return c;
}

static RetainPtr<GraphicsBitmap> load_png_impl(const byte* data, int data_size)
{
    Stopwatch sw("load_png_impl: total");
    const byte* data_ptr = data;
    int data_remaining = data_size;

    const byte png_header[8] = { 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };
    if (memcmp(data, png_header, sizeof(png_header))) {
        dbgprintf("Invalid PNG header\n");
        return nullptr;
    }

    PNGLoadingContext context;

    context.compressed_data.ensure_capacity(data_size);

    data_ptr += sizeof(png_header);
    data_remaining -= sizeof(png_header);

    {
        Stopwatch sw("load_png_impl: read chunks");
        Streamer streamer(data_ptr, data_remaining);
        while (!streamer.at_end()) {
            if (!process_chunk(streamer, context)) {
                return nullptr;
            }
        }
    }

    {
        Stopwatch sw("load_png_impl: uncompress");
        unsigned long srclen = context.compressed_data.size() - 6;
        unsigned long destlen = context.decompression_buffer_size;
        int ret = puff(context.decompression_buffer, &destlen, context.compressed_data.data() + 2, &srclen);
        if (ret < 0)
            return nullptr;
        context.compressed_data.clear();
    }

    {
        Stopwatch sw("load_png_impl: extract scanlines");
        context.scanlines.ensure_capacity(context.height);
        Streamer streamer(context.decompression_buffer, context.decompression_buffer_size);
        for (int y = 0; y < context.height; ++y) {
            byte filter;
            if (!streamer.read(filter))
                return nullptr;

            context.scanlines.append({ filter });
            auto& scanline_buffer = context.scanlines.last().data;
            if (!streamer.wrap_bytes(scanline_buffer, context.width * context.bytes_per_pixel))
                return nullptr;
        }
    }

    {
        Stopwatch sw("load_png_impl: create bitmap");
        context.bitmap = GraphicsBitmap::create(GraphicsBitmap::Format::RGBA32, { context.width, context.height });
    }

    union [[gnu::packed]] Pixel {
        RGBA32 rgba { 0 };
        struct {
            byte r;
            byte g;
            byte b;
            byte a;
        };
    };
    static_assert(sizeof(Pixel) == 4);

    {
    Stopwatch sw("load_png_impl: unfilter");
    for (int y = 0; y < context.height; ++y) {
        auto filter = context.scanlines[y].filter;
        switch (context.color_type) {
        case 2: {
            struct [[gnu::packed]] Triplet { byte r; byte g; byte b; };
            auto* triplets = (Triplet*)context.scanlines[y].data.pointer();
            for (int i = 0; i < context.width; ++i) {
                auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
                pixel.r = triplets[i].r;
                pixel.g = triplets[i].g;
                pixel.b = triplets[i].b;
                pixel.a = 0xff;
            }
            break;
        }
        case 6:
            memcpy(context.bitmap->scanline(y), context.scanlines[y].data.pointer(), context.scanlines[y].data.size());
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
        if (filter == 0)
            continue;
        for (int i = 0; i < context.width; ++i) {
            auto& x = (Pixel&)context.bitmap->scanline(y)[i];
            swap(x.r, x.b);
            Pixel a;
            Pixel b;
            Pixel c;
            if (i != 0) a.rgba = context.bitmap->scanline(y)[i - 1];
            if (y != 0) b.rgba = context.bitmap->scanline(y - 1)[i];
            if (y != 0 && i != 0) c.rgba = context.bitmap->scanline(y - 1)[i - 1];

            if (filter == 1) {
                x.r += a.r;
                x.g += a.g;
                x.b += a.b;
                if (context.has_alpha())
                    x.a += a.a;
            } else if (filter == 2) {
                x.r += b.r;
                x.g += b.g;
                x.b += b.b;
                if (context.has_alpha())
                    x.a += b.a;
            } if (filter == 3) {
                x.r = x.r + ((a.r + b.r) / 2);
                x.g = x.g + ((a.g + b.g) / 2);
                x.b = x.b + ((a.b + b.b) / 2);
                if (context.has_alpha())
                    x.a = x.a + ((a.a + b.a) / 2);
            } if (filter == 4) {
                x.r += paeth_predictor(a.r, b.r, c.r);
                x.g += paeth_predictor(a.g, b.g, c.g);
                x.b += paeth_predictor(a.b, b.b, c.b);
                if (context.has_alpha())
                    x.a += paeth_predictor(a.a, b.a, c.a);
            }
        }
    }
    }

    munmap(context.decompression_buffer, context.decompression_buffer_size);
    context.decompression_buffer = nullptr;
    context.decompression_buffer_size = 0;

    return context.bitmap;
}

static bool process_IHDR(const ByteBuffer& data, PNGLoadingContext& context)
{
    if (data.size() < sizeof(PNG_IHDR))
        return false;
    auto& ihdr = *(const PNG_IHDR*)data.pointer();
    context.width = ihdr.width;
    context.height = ihdr.height;
    context.bit_depth = ihdr.bit_depth;
    context.color_type = ihdr.color_type;
    context.compression_method = ihdr.compression_method;
    context.filter_method = ihdr.filter_method;
    context.interlace_method = ihdr.interlace_method;

    switch (context.color_type) {
    case 2:
        context.bytes_per_pixel = 3;
        break;
    case 6:
        context.bytes_per_pixel = 4;
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    printf("PNG: %dx%d (%d bpp)\n", context.width, context.height, context.bit_depth);
    printf("     Color type: %b\n", context.color_type);
    printf(" Interlace type: %b\n", context.interlace_method);

    context.decompression_buffer_size = (context.width * context.height * context.bytes_per_pixel + context.height);
    context.decompression_buffer = (byte*)mmap(nullptr, context.decompression_buffer_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    return true;
}

static bool process_IDAT(const ByteBuffer& data, PNGLoadingContext& context)
{
    context.compressed_data.append(data.pointer(), data.size());
    return true;
}

static bool process_chunk(Streamer& streamer, PNGLoadingContext& context)
{
    dword chunk_size;
    if (!streamer.read(chunk_size)) {
        printf("Bail at chunk_size\n");
        return false;
    }
    byte chunk_type[5];
    chunk_type[4] = '\0';
    if (!streamer.read_bytes(chunk_type, 4)) {
        printf("Bail at chunk_type\n");
        return false;
    }
    ByteBuffer chunk_data;
    if (!streamer.wrap_bytes(chunk_data, chunk_size)) {
        printf("Bail at chunk_data\n");
        return false;
    }
    dword chunk_crc;
    if (!streamer.read(chunk_crc)) {
        printf("Bail at chunk_crc\n");
        return false;
    }
    printf("Chunk type: '%s', size: %u, crc: %x\n", chunk_type, chunk_size, chunk_crc);

    if (!strcmp((const char*)chunk_type, "IHDR"))
        return process_IHDR(chunk_data, context);
    if (!strcmp((const char*)chunk_type, "IDAT"))
        return process_IDAT(chunk_data, context);
    return true;
}
