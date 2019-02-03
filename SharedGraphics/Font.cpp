#include "Font.h"
#include "Peanut8x10.h"
#include "Liza8x10.h"
#include <AK/kmalloc.h>
#include <AK/BufferStream.h>
#include <AK/StdLibExtras.h>

#ifdef USERLAND
#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/fcntl.h>
#include <LibC/errno.h>
#include <LibC/mman.h>
#endif

#define DEFAULT_FONT_NAME Liza8x10

static const byte error_glyph_width = 8;
static const byte error_glyph_height = 10;
static constexpr const char* error_glyph {
    "  ####  "
    " #    # "
    " #    # "
    " # ## # "
    " # ## # "
    "  ####  "
    "   ##   "
    " ###### "
    "   ##   "
    "   ##   ",
};

static Font* s_default_font;

void Font::initialize()
{
    s_default_font = nullptr;
}

Font& Font::default_font()
{
    if (!s_default_font) {
#ifdef USERLAND
        s_default_font = Font::load_from_file("/res/fonts/Liza8x10.font").leak_ref();
        ASSERT(s_default_font);
#else

        s_default_font = adopt(*new Font(DEFAULT_FONT_NAME::name, DEFAULT_FONT_NAME::glyphs, DEFAULT_FONT_NAME::glyph_width, DEFAULT_FONT_NAME::glyph_height, DEFAULT_FONT_NAME::first_glyph, DEFAULT_FONT_NAME::last_glyph)).leak_ref();
#endif
    }
    return *s_default_font;
}

RetainPtr<Font> Font::clone() const
{
    size_t bytes_per_glyph = glyph_width() * glyph_height();
    // FIXME: This is leaked!
    char** new_glyphs = static_cast<char**>(kmalloc(sizeof(char*) * 256));
    for (unsigned i = 0; i < 256; ++i) {
        new_glyphs[i] = static_cast<char*>(kmalloc(bytes_per_glyph));
        if (i >= m_first_glyph && i <= m_last_glyph) {
            memcpy(new_glyphs[i], m_glyphs[i - m_first_glyph], bytes_per_glyph);
        } else {
            memset(new_glyphs[i], ' ', bytes_per_glyph);
        }
    }
    return adopt(*new Font(m_name, new_glyphs, m_glyph_width, m_glyph_height, 0, 255));
}

Font::Font(const String& name, const char* const* glyphs, byte glyph_width, byte glyph_height, byte first_glyph, byte last_glyph)
    : m_name(name)
    , m_glyphs(glyphs)
    , m_glyph_width(glyph_width)
    , m_glyph_height(glyph_height)
    , m_first_glyph(first_glyph)
    , m_last_glyph(last_glyph)
{
    ASSERT(m_glyph_width == error_glyph_width);
    ASSERT(m_glyph_height == error_glyph_height);
    m_error_bitmap = CharacterBitmap::create_from_ascii(error_glyph, error_glyph_width, error_glyph_height);
    for (unsigned ch = 0; ch < 256; ++ch) {
        if (ch < m_first_glyph || ch > m_last_glyph) {
            m_bitmaps[ch] = m_error_bitmap.copy_ref();
            continue;
        }
        const char* data = m_glyphs[(unsigned)ch - m_first_glyph];
        m_bitmaps[ch] = CharacterBitmap::create_from_ascii(data, m_glyph_width, m_glyph_height);
    }
}

Font::~Font()
{
}

#ifdef USERLAND
struct FontFileHeader {
    char magic[4];
    byte glyph_width;
    byte glyph_height;
    byte type;
    byte unused[7];
    char name[64];
} PACKED;

RetainPtr<Font> Font::load_from_memory(const byte* data)
{
    auto& header = *reinterpret_cast<const FontFileHeader*>(data);
    if (memcmp(header.magic, "!Fnt", 4)) {
        dbgprintf("header.magic != '!Fnt', instead it's '%c%c%c%c'\n", header.magic[0], header.magic[1], header.magic[2], header.magic[3]);
        return nullptr;
    }
    if (header.name[63] != '\0') {
        dbgprintf("Font name not fully null-terminated\n");
        return nullptr;
    }

    auto* glyphs_ptr = reinterpret_cast<const unsigned*>(data + sizeof(FontFileHeader));

    char** new_glyphs = static_cast<char**>(kmalloc(sizeof(char*) * 256));
    for (unsigned glyph_index = 0; glyph_index < 256; ++glyph_index) {
        new_glyphs[glyph_index] = static_cast<char*>(kmalloc(header.glyph_width * header.glyph_height));
        char* bitptr = new_glyphs[glyph_index];
        for (unsigned y = 0; y < header.glyph_height; ++y) {
            unsigned pattern = *(glyphs_ptr++);
            for (unsigned x = 0; x < header.glyph_width; ++x) {
                if (pattern & (1u << x)) {
                    *(bitptr++) = '#';
                } else {
                    *(bitptr++) = ' ';
                }
            }
        }
    }

    return adopt(*new Font(String(header.name), new_glyphs, header.glyph_width, header.glyph_height, 0, 255));
}

RetainPtr<Font> Font::load_from_file(const String& path)
{
    int fd = open(path.characters(), O_RDONLY, 0644);
    if (fd < 0) {
        dbgprintf("open(%s) got fd=%d, failed: %s\n", path.characters(), fd, strerror(errno));
        perror("open");
        return nullptr;
    }

    auto* mapped_file = (byte*)mmap(nullptr, 4096 * 3, PROT_READ, MAP_SHARED, fd, 0);
    if (mapped_file == MAP_FAILED)
        return nullptr;

    auto font = load_from_memory(mapped_file);
    int rc = munmap(mapped_file, 4096 * 3);
    ASSERT(rc == 0);
    return font;
}

bool Font::write_to_file(const String& path)
{
    int fd = open(path.characters(), O_WRONLY | O_CREAT, 0644);
    if (fd < 0) {
        perror("open");
        return false;
    }

    FontFileHeader header;
    memset(&header, 0, sizeof(FontFileHeader));
    memcpy(header.magic, "!Fnt", 4);
    header.glyph_width = m_glyph_width;
    header.glyph_height = m_glyph_height;
    header.type = 0;
    memcpy(header.name, m_name.characters(), min(m_name.length(), 63u));

    size_t bytes_per_glyph = sizeof(unsigned) * m_glyph_height;

    auto buffer = ByteBuffer::create_uninitialized(sizeof(FontFileHeader) + (256 * bytes_per_glyph));
    BufferStream stream(buffer);

    stream << ByteBuffer::wrap((byte*)&header, sizeof(FontFileHeader));

    for (unsigned glyph_index = 0; glyph_index < 256; ++glyph_index) {
        auto* glyph_bits = (byte*)m_glyphs[glyph_index];
        for (unsigned y = 0; y < m_glyph_height; ++y) {
            unsigned pattern = 0;
            for (unsigned x = 0; x < m_glyph_width; ++x) {
                if (glyph_bits[y * m_glyph_width + x] == '#') {
                    pattern |= 1 << x;
                }
            }
            stream << pattern;
        }
    }

    ASSERT(stream.at_end());
    ssize_t nwritten = write(fd, buffer.pointer(), buffer.size());
    ASSERT(nwritten == (ssize_t)buffer.size());
    int rc = close(fd);
    ASSERT(rc == 0);
    return true;
}
#endif
