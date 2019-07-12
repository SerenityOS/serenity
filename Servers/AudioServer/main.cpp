#include <LibCore/CFile.h>

u32 read_u32(const ByteBuffer& buf, u32& off)
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

u16 read_u16(const ByteBuffer& buf, u32& off)
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

ByteBuffer read_wav_data(const StringView& path)
{
    CFile wav(path);
    if (!wav.open(CIODevice::ReadOnly)) {
        dbgprintf("Can't open wav to dump it to audio: %s", wav.error_string());
        return {};
    }

    const auto& contents = wav.read_all();
    u32 off = 0;

    if (contents.size() - off < 12) {
        dbgprintf("WAV is too small (no header, %d bytes)\n", contents.size());
        return {};
    }

    dbgprintf("Trying to parse %d bytes of wav\n", contents.size());

#define CHECK_OK(msg)                      \
    do {                                   \
        ASSERT(ok);                        \
        if (!ok) {                         \
            dbgprintf("%s failed\n", msg); \
            return {};                     \
        } else {                           \
            dbgprintf("%S is OK!\n", msg); \
        }                                  \
    } while (0);

    bool ok = true;
    u32 riff = read_u32(contents, off);
    ok = ok && riff == 0x46464952; // "RIFF"
    CHECK_OK("RIFF header");

    u32 sz = read_u32(contents, off);
    ASSERT(sz < 1024 * 1024 * 42);
    ok = ok && sz < 1024 * 1024 * 42; // arbitrary
    CHECK_OK("File size");

    u32 wave = read_u32(contents, off);
    ok = ok && wave == 0x45564157; // "WAVE"
    CHECK_OK("WAVE header");

    if (contents.size() - off < 8) {
        dbgprintf("WAV is too small (no fmt, %d bytes)\n", contents.size());
        return {};
    }

    u32 fmt_id = read_u32(contents, off);
    ok = ok && fmt_id == 0x20746D66; // "FMT"
    CHECK_OK("FMT header");

    u32 fmt_size = read_u32(contents, off);
    ok = ok && fmt_size == 16;
    ASSERT(fmt_size == 16);
    CHECK_OK("FMT size");

    if (contents.size() - off < 16) {
        dbgprintf("WAV is too small (fmt chunk, %d bytes)\n", contents.size());
        return {};
    }

    u16 audio_format = read_u16(contents, off);
    ok = ok && audio_format == 1; // WAVE_FORMAT_PCM
    ASSERT(audio_format == 1);
    CHECK_OK("Audio format");

    u16 num_channels = read_u16(contents, off);
    ok = ok && num_channels == 1;
    ASSERT(num_channels == 1);
    CHECK_OK("Channel count");

    u32 sample_rate = read_u32(contents, off);
    CHECK_OK("Sample rate");

    off += 4; // bytes per sec: we don't care.
    off += 2; // block align: we don't care.

    u16 bits_per_sample = read_u16(contents, off);
    ok = ok && (bits_per_sample == 8 || bits_per_sample == 16);
    ASSERT(bits_per_sample == 8 || bits_per_sample == 16);
    CHECK_OK("Bits per sample");

    dbgprintf("Read WAV of format %d with num_channels %d sample rate %d, bits per sample %d\n", audio_format, num_channels, sample_rate, bits_per_sample);

    // Read chunks until we find DATA
    if (off >= u32(contents.size()) - 8) {
        ok = ok && false;
        ASSERT_NOT_REACHED();
        CHECK_OK("Premature EOF without DATA");
    }

    bool found_data = false;
    u32 data_sz = 0;
    while (off < u32(contents.size()) - 8) {
        u32 chunk_id = read_u32(contents, off);
        data_sz = read_u32(contents, off);
        if (chunk_id == 0x61746164) { // DATA
            found_data = true;
            break;
        }
        off += data_sz;
    }

    ok = ok && found_data;
    ASSERT(found_data);
    CHECK_OK("Found no data chunk");

    ok = ok && data_sz <= (contents.size() - off);
    CHECK_OK("Bad DATA size");

    return contents.slice(off, data_sz);
}

void read_and_play_wav()
{
    CFile audio("/dev/audio");
    if (!audio.open(CIODevice::WriteOnly)) {
        dbgprintf("Can't open audio device: %s", audio.error_string());
        return;
    }

    const auto& contents = read_wav_data("/home/anon/tmp.wav");
    const int chunk_size = 4096;
    int i = 0;
    while (i < contents.size()) {
        const auto chunk = contents.slice(i, chunk_size);
        audio.write(chunk);
        i += chunk_size;
    }
}

int main(int, char**)
{
    read_and_play_wav();
    return 0;
}
