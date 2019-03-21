#include <SharedGraphics/PNGLoader.h>
#include <Kernel/NetworkOrdered.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <string.h>
#include <SharedGraphics/puff.c>

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
    Vector<Scanline> scanlines;
    RetainPtr<GraphicsBitmap> bitmap;
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
    const byte* data_ptr = data;
    int data_remaining = data_size;

    const byte png_header[8] = { 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };
    if (memcmp(data, png_header, sizeof(png_header))) {
        dbgprintf("Invalid PNG header\n");
        return nullptr;
    }

    dbgprintf("Okay, PNG loaded\n");

    PNGLoadingContext context;

    data_ptr += sizeof(png_header);
    data_remaining -= sizeof(png_header);

    Streamer streamer(data_ptr, data_remaining);
    while (!streamer.at_end()) {
        if (!process_chunk(streamer, context)) {
            return nullptr;
        }
    }

    context.bitmap = GraphicsBitmap::create(GraphicsBitmap::Format::RGBA32, { context.width, context.height });

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

    printf("unfiltering %d scanlines\n", context.height);
    for (int y = 0; y < context.height; ++y) {
        auto filter = context.scanlines[y].filter;
        printf("[%d] filter=%u\n", y, context.scanlines[y].filter);
        memcpy(context.bitmap->scanline(y), context.scanlines[y].data.pointer(), context.scanlines[y].data.size());
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
                x.a += a.a;
            }

            if (filter == 2) {
                x.r += b.r;
                x.g += b.g;
                x.b += b.b;
                x.a += b.a;
            }

            if (filter == 3) {
                x.r = x.r + ((a.r + b.r) / 2);
                x.g = x.g + ((a.g + b.g) / 2);
                x.b = x.b + ((a.b + b.b) / 2);
                x.a = x.a + ((a.a + b.a) / 2);
            }

            if (filter == 4) {
                x.r += paeth_predictor(a.r, b.r, c.r);
                x.g += paeth_predictor(a.g, b.g, c.g);
                x.b += paeth_predictor(a.b, b.b, c.b);
                x.a += paeth_predictor(a.a, b.a, c.a);
            }
        }
    }

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
    return true;
}

static bool process_IDAT(const ByteBuffer& data, PNGLoadingContext& context)
{
    for (int i = 0; i < data.size(); ++i) {
        printf("%c", data[i]);
    }
    printf("\n");

    printf("first byte into puff: %c (%b)\n", data[0], data[0]);

    struct [[gnu::packed]] ZlibHeader {
        byte compression_method : 4;
        byte compression_info : 4;
        byte flags;
    };
    static_assert(sizeof(ZlibHeader) == 2);

    auto& zhdr = *(ZlibHeader*)data.pointer();
    printf("compression_method: %u\n", zhdr.compression_method);
    printf("compression_info: %u\n", zhdr.compression_info);
    printf("flags: %b\n", zhdr.flags);

    unsigned long destlen = (context.width * context.height * sizeof(RGBA32)) + context.height;
    unsigned long srclen = data.size() - 2;
    auto decompression_buffer = ByteBuffer::create_uninitialized(destlen + context.height);
    int ret = puff(decompression_buffer.pointer(), &destlen, data.pointer() + 2, &srclen);
    if (ret != 0)
        return false;
    Streamer streamer(decompression_buffer.pointer(), decompression_buffer.size());
    for (int y = 0; y < context.height; ++y) {
        byte filter;
        if (!streamer.read(filter))
            return false;
        context.scanlines.append({ filter, ByteBuffer::create_uninitialized(context.width * sizeof(RGBA32)) });
        auto& scanline_buffer = context.scanlines.last().data;
        if (!streamer.read_bytes(scanline_buffer.pointer(), scanline_buffer.size()))
            return false;
    }
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
    auto chunk_data = ByteBuffer::create_uninitialized(chunk_size);
    if (!streamer.read_bytes(chunk_data.pointer(), chunk_size)) {
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
