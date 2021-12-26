#include "Font.h"
#include <AK/kmalloc.h>
#include <AK/BufferStream.h>
#include <AK/StdLibExtras.h>
#include <AK/MappedFile.h>
#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/fcntl.h>
#include <LibC/errno.h>
#include <LibC/mman.h>

struct [[gnu::packed]] FontFileHeader {
    char magic[4];
    byte glyph_width;
    byte glyph_height;
    byte type;
    byte is_variable_width;
    byte unused[6];
    char name[64];
};

Font& Font::default_font()
{
    static Font* s_default_font;
    static const char* default_font_path = "/res/fonts/Katica10.font";
    if (!s_default_font) {
        s_default_font = Font::load_from_file(default_font_path).leak_ref();
        ASSERT(s_default_font);
    }
    return *s_default_font;
}

Font& Font::default_fixed_width_font()
{
    static Font* s_default_fixed_width_font;
    static const char* default_fixed_width_font_path = "/res/fonts/CsillaThin7x10.font";
    if (!s_default_fixed_width_font) {
        s_default_fixed_width_font = Font::load_from_file(default_fixed_width_font_path).leak_ref();
        ASSERT(s_default_fixed_width_font);
    }
    return *s_default_fixed_width_font;
}

Font& Font::default_bold_font()
{
    static Font* s_default_bold_font;
    static const char* default_bold_font_path = "/res/fonts/KaticaBold10.font";
    if (!s_default_bold_font) {
        s_default_bold_font = Font::load_from_file(default_bold_font_path).leak_ref();
        ASSERT(s_default_bold_font);
    }
    return *s_default_bold_font;
}

RetainPtr<Font> Font::clone() const
{
    size_t bytes_per_glyph = sizeof(dword) * glyph_height();
    // FIXME: This is leaked!
    auto* new_rows = static_cast<unsigned*>(kmalloc(bytes_per_glyph * 256));
    memcpy(new_rows, m_rows, bytes_per_glyph * 256);
    auto* new_widths = static_cast<byte*>(kmalloc(256));
    if (m_glyph_widths)
        memcpy(new_widths, m_glyph_widths, 256);
    else
        memset(new_widths, m_glyph_width, 256);
    return adopt(*new Font(m_name, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height));
}

Font::Font(const String& name, unsigned* rows, byte* widths, bool is_fixed_width, byte glyph_width, byte glyph_height)
    : m_name(name)
    , m_rows(rows)
    , m_glyph_widths(widths)
    , m_glyph_width(glyph_width)
    , m_glyph_height(glyph_height)
    , m_min_glyph_width(glyph_width)
    , m_max_glyph_width(glyph_width)
    , m_fixed_width(is_fixed_width)
{
    if (!m_fixed_width) {
        byte maximum = 0;
        byte minimum = 255;
        for (int i = 0; i < 256; ++i) {
            minimum = min(minimum, m_glyph_widths[i]);
            maximum = max(maximum, m_glyph_widths[i]);
        }
        m_min_glyph_width = minimum;
        m_max_glyph_width = maximum;
    }
}

Font::~Font()
{
}

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

    size_t bytes_per_glyph = sizeof(unsigned) * header.glyph_height;

    auto* rows = (unsigned*)(data + sizeof(FontFileHeader));
    byte* widths = nullptr;
    if (header.is_variable_width)
        widths = (byte*)(rows) + 256 * bytes_per_glyph;
    return adopt(*new Font(String(header.name), rows, widths, !header.is_variable_width, header.glyph_width, header.glyph_height));
}

RetainPtr<Font> Font::load_from_file(const String& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;

    auto font = load_from_memory((const byte*)mapped_file.pointer());
    font->m_mapped_file = move(mapped_file);
    return font;
}

bool Font::write_to_file(const String& path)
{
    int fd = creat(path.characters(), 0644);
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
    header.is_variable_width = !m_fixed_width;
    memcpy(header.name, m_name.characters(), min(m_name.length(), 63));

    size_t bytes_per_glyph = sizeof(unsigned) * m_glyph_height;

    auto buffer = ByteBuffer::create_uninitialized(sizeof(FontFileHeader) + (256 * bytes_per_glyph) + 256);
    BufferStream stream(buffer);

    stream << ByteBuffer::wrap(&header, sizeof(FontFileHeader));
    stream << ByteBuffer::wrap(m_rows, (256 * bytes_per_glyph));
    stream << ByteBuffer::wrap(m_glyph_widths, 256);

    ASSERT(stream.at_end());
    ssize_t nwritten = write(fd, buffer.pointer(), buffer.size());
    ASSERT(nwritten == (ssize_t)buffer.size());
    int rc = close(fd);
    ASSERT(rc == 0);
    return true;
}

int Font::width(const String& string) const
{
    return width(string.characters(), string.length());
}

int Font::width(const char* characters, int length) const
{
    if (!length)
        return 0;

    if (m_fixed_width)
        return length * m_glyph_width;

    int width = 0;
    for (int i = 0; i < length; ++i)
        width += glyph_width(characters[i]) + 1;

    return width - 1;
}
