#include "Font.h"
#include <AK/kmalloc.h>
#include <AK/BufferStream.h>
#include <AK/StdLibExtras.h>

#ifdef KERNEL
#include <Kernel/Process.h>
#include <Kernel/MemoryManager.h>
#include <Kernel/FileDescriptor.h>
#include <Kernel/VirtualFileSystem.h>
#endif

#ifdef USERLAND
#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/fcntl.h>
#include <LibC/errno.h>
#include <LibC/mman.h>
#endif

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
static Font* s_default_bold_font;

struct FontFileHeader {
    char magic[4];
    byte glyph_width;
    byte glyph_height;
    byte type;
    byte unused[7];
    char name[64];
} PACKED;

static inline constexpr size_t font_file_size(unsigned glyph_height)
{
    return sizeof(FontFileHeader) + 256 * sizeof(dword) * glyph_height;
}

void Font::initialize()
{
    s_default_font = nullptr;
    s_default_bold_font = nullptr;
}

Font& Font::default_font()
{
    static const char* default_font_path = "/res/fonts/LizaRegular8x10.font";
    if (!s_default_font) {
#ifdef USERLAND
        s_default_font = Font::load_from_file(default_font_path).leak_ref();
#else
        int error;
        auto descriptor = VFS::the().open(default_font_path, error, 0, 0, *VFS::the().root_inode());
        if (!descriptor) {
            kprintf("Failed to open default font (%s)\n", default_font_path);
            ASSERT_NOT_REACHED();
        }
        auto* region = current->allocate_file_backed_region(LinearAddress(), font_file_size(10), descriptor->inode(), "default_font", /*readable*/true, /*writable*/false);
        ASSERT(region);
        region->page_in();
        s_default_font = Font::load_from_memory(region->laddr().as_ptr()).leak_ref();
#endif
        ASSERT(s_default_font);
    }
    return *s_default_font;
}

Font& Font::default_bold_font()
{
    static const char* default_bold_font_path = "/res/fonts/LizaBold8x10.font";
    if (!s_default_bold_font) {
#ifdef USERLAND
        s_default_bold_font = Font::load_from_file(default_bold_font_path).leak_ref();
#else
        int error;
        auto descriptor = VFS::the().open(default_bold_font_path, error, 0, 0, *VFS::the().root_inode());
        if (!descriptor) {
            kprintf("Failed to open default bold font (%s)\n", default_bold_font_path);
            ASSERT_NOT_REACHED();
        }
        auto* region = current->allocate_file_backed_region(LinearAddress(), font_file_size(10), descriptor->inode(), "default_bold_font", /*readable*/true, /*writable*/false);
        ASSERT(region);
        ASSERT_INTERRUPTS_ENABLED();
        region->page_in();
        ASSERT_INTERRUPTS_ENABLED();
        s_default_bold_font = Font::load_from_memory(region->laddr().as_ptr()).leak_ref();
#endif
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
    return adopt(*new Font(m_name, new_rows, m_glyph_width, m_glyph_height));
}

Font::Font(const String& name, unsigned* rows, byte glyph_width, byte glyph_height)
    : m_name(name)
    , m_rows(rows)
    , m_glyph_width(glyph_width)
    , m_glyph_height(glyph_height)
{
    ASSERT(m_glyph_width == error_glyph_width);
    ASSERT(m_glyph_height == error_glyph_height);
    m_error_bitmap = CharacterBitmap::create_from_ascii(error_glyph, error_glyph_width, error_glyph_height);
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

    auto* rows = (unsigned*)(data + sizeof(FontFileHeader));
    return adopt(*new Font(String(header.name), rows, header.glyph_width, header.glyph_height));
}

#ifdef USERLAND
RetainPtr<Font> Font::load_from_file(const String& path)
{
    int fd = open(path.characters(), O_RDONLY, 0644);
    if (fd < 0) {
        dbgprintf("open(%s) got fd=%d, failed: %s\n", path.characters(), fd, strerror(errno));
        perror("open");
        return nullptr;
    }

    auto* mapped_file = (byte*)mmap(nullptr, 4096 * 3, PROT_READ, MAP_SHARED, fd, 0);
    if (mapped_file == MAP_FAILED) {
        int rc = close(fd);
        ASSERT(rc == 0);
        return nullptr;
    }

    auto font = load_from_memory(mapped_file);

    int rc = close(fd);
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
    stream << ByteBuffer::wrap((byte*)m_rows, (256 * bytes_per_glyph));

    ASSERT(stream.at_end());
    ssize_t nwritten = write(fd, buffer.pointer(), buffer.size());
    ASSERT(nwritten == (ssize_t)buffer.size());
    int rc = close(fd);
    ASSERT(rc == 0);
    return true;
}
#endif
