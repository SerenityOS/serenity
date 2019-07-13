#include <LibCore/CFile.h>

#include "AWavLoader.h"
#include "AWavFile.h"

static u32 read_u32(const ByteBuffer& buf, u32& off)
{
    ASSERT(buf.size() - off >= 4);
    u32 b0 = buf[off + 0];
    u32 b1 = buf[off + 1];
    u32 b2 = buf[off + 2];
    u32 b3 = buf[off + 3];

    u32 ret = 0;
    ret |= (u8(b3) << 24);
    ret |= (u8(b2) << 16);
    ret |= (u8(b1) << 8);
    ret |= (u8(b0));

    off += 4;
    return ret;
}

static u16 read_u16(const ByteBuffer& buf, u32& off)
{
    ASSERT(buf.size() - off >= 2);
    u16 b0 = buf[off + 0];
    u16 b1 = buf[off + 1];

    u16 ret = 0;
    ret |= (u8(b1) << 8);
    ret |= (u8(b0));

    off += 2;
    return ret;
}

RefPtr<AWavFile> AWavLoader::load_wav(const StringView& path)
{
    m_error_string = {};

    CFile wav(path);
    if (!wav.open(CIODevice::ReadOnly)) {
        m_error_string = String::format("Can't open file: %s", wav.error_string());
        return nullptr;
    }

    const auto& contents = wav.read_all();
    return parse_wav(contents);
}

// TODO: A streaming parser might be better than forcing a ByteBuffer
RefPtr<AWavFile> AWavLoader::parse_wav(const ByteBuffer& buffer)
{
    u32 off = 0;

    if (buffer.size() - off < 12) {
        dbgprintf("WAV is too small (no header, %d bytes)\n", buffer.size());
        return {};
    }

    dbgprintf("Trying to parse %d bytes of wav\n", buffer.size());

#define CHECK_OK(msg)                      \
    do {                                   \
        ASSERT(ok);                        \
        if (!ok) {                         \
            m_error_string = String::format("Parsing failed: %s", msg); \
            return {};                     \
        } else {                           \
            dbgprintf("%s is OK!\n", msg); \
        }                                  \
    } while (0);

    bool ok = true;
    u32 riff = read_u32(buffer, off);
    ok = ok && riff == 0x46464952; // "RIFF"
    CHECK_OK("RIFF header");

    u32 sz = read_u32(buffer, off);
    ASSERT(sz < 1024 * 1024 * 42);
    ok = ok && sz < 1024 * 1024 * 42; // arbitrary
    CHECK_OK("File size");

    u32 wave = read_u32(buffer, off);
    ok = ok && wave == 0x45564157; // "WAVE"
    CHECK_OK("WAVE header");

    if (buffer.size() - off < 8) {
        dbgprintf("WAV is too small (no fmt, %d bytes)\n", buffer.size());
        return {};
    }

    u32 fmt_id = read_u32(buffer, off);
    ok = ok && fmt_id == 0x20746D66; // "FMT"
    CHECK_OK("FMT header");

    u32 fmt_size = read_u32(buffer, off);
    ok = ok && fmt_size == 16;
    ASSERT(fmt_size == 16);
    CHECK_OK("FMT size");

    if (buffer.size() - off < 16) {
        dbgprintf("WAV is too small (fmt chunk, %d bytes)\n", buffer.size());
        return {};
    }

    auto ret = adopt(*new AWavFile);
    u16 audio_format = read_u16(buffer, off);
    ok = ok && audio_format == 1; // WAVE_FORMAT_PCM
    ASSERT(audio_format == 1);
    CHECK_OK("Audio format");
    ret->m_format = AWavFile::Format::PCM;

    u16 num_channels = read_u16(buffer, off);
    CHECK_OK("Channel count");
    ret->m_channel_count = num_channels;

    u32 sample_rate = read_u32(buffer, off);
    CHECK_OK("Sample rate");
    ret->m_sample_rate = sample_rate;

    u32 byte_rate = read_u32(buffer, off);
    CHECK_OK("Byte rate");
    ret->m_byte_rate = byte_rate;

    u16 block_align = read_u16(buffer, off);
    CHECK_OK("Block align");
    ret->m_block_align = block_align;

    u16 bits_per_sample = read_u16(buffer, off);
    ok = ok && (bits_per_sample == 8 || bits_per_sample == 16);
    ASSERT(bits_per_sample == 8 || bits_per_sample == 16);
    CHECK_OK("Bits per sample");
    ret->m_bits_per_sample = bits_per_sample;

    // Read chunks until we find DATA
    if (off >= u32(buffer.size()) - 8) {
        ok = ok && false;
        ASSERT_NOT_REACHED();
        CHECK_OK("Premature EOF without DATA");
    }

    bool found_data = false;
    u32 data_sz = 0;
    while (off < u32(buffer.size()) - 8) {
        u32 chunk_id = read_u32(buffer, off);
        data_sz = read_u32(buffer, off);
        if (chunk_id == 0x61746164) { // DATA
            found_data = true;
            break;
        }
        off += data_sz;
    }

    ok = ok && found_data;
    ASSERT(found_data);
    CHECK_OK("Found no data chunk");

    ok = ok && data_sz <= (buffer.size() - off);
    CHECK_OK("Bad DATA size");

    ret->m_sample_data = buffer.slice(off, data_sz);
    return ret;
}

