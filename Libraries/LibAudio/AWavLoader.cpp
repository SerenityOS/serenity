#include <LibCore/CFile.h>
#include <AK/BufferStream.h>
#include <limits>

#include "AWavLoader.h"
#include "AWavFile.h"

RefPtr<AWavFile> AWavLoader::load_wav(const StringView& path)
{
    m_error_string = {};

    CFile wav(path);
    if (!wav.open(CIODevice::ReadOnly)) {
        m_error_string = String::format("Can't open file: %s", wav.error_string());
        return nullptr;
    }

    auto contents = wav.read_all();
    return parse_wav(contents);
}

// TODO: A streaming parser might be better than forcing a ByteBuffer
RefPtr<AWavFile> AWavLoader::parse_wav(ByteBuffer& buffer)
{
    BufferStream stream(buffer);

#define CHECK_OK(msg)                      \
    do {                                   \
        ASSERT(ok);                        \
        if (stream.handle_read_failure()) { \
            m_error_string = String::format("Premature stream EOF at %s", msg); \
            return {}; \
        } \
        if (!ok) {                         \
            m_error_string = String::format("Parsing failed: %s", msg); \
            return {};                     \
        } else {                           \
            dbgprintf("%s is OK!\n", msg); \
        }                                  \
    } while (0);

    bool ok = true;
    u32 riff; stream >> riff;
    ok = ok && riff == 0x46464952; // "RIFF"
    CHECK_OK("RIFF header");

    u32 sz; stream >> sz;
    ok = ok && sz < 1024 * 1024 * 42; // arbitrary
    CHECK_OK("File size");
    ASSERT(sz < 1024 * 1024 * 42);

    u32 wave; stream >> wave;
    ok = ok && wave == 0x45564157; // "WAVE"
    CHECK_OK("WAVE header");

    u32 fmt_id; stream >> fmt_id;
    ok = ok && fmt_id == 0x20746D66; // "FMT"
    CHECK_OK("FMT header");

    u32 fmt_size; stream >> fmt_size;
    ok = ok && fmt_size == 16;
    CHECK_OK("FMT size");
    ASSERT(fmt_size == 16);

    auto ret = adopt(*new AWavFile);
    u16 audio_format; stream >> audio_format;
    CHECK_OK("Audio format"); // incomplete read check
    ok = ok && audio_format == 1; // WAVE_FORMAT_PCM
    ASSERT(audio_format == 1);
    CHECK_OK("Audio format"); // value check
    ret->m_format = AWavFile::Format::PCM;

    u16 num_channels; stream >> num_channels;
    CHECK_OK("Channel count");
    ret->m_channel_count = num_channels;

    u32 sample_rate; stream >> sample_rate;
    CHECK_OK("Sample rate");
    ret->m_sample_rate = sample_rate;

    u32 byte_rate; stream >> byte_rate;
    CHECK_OK("Byte rate");
    ret->m_byte_rate = byte_rate;

    u16 block_align; stream >> block_align;
    CHECK_OK("Block align");
    ret->m_block_align = block_align;

    u16 bits_per_sample; stream >> bits_per_sample;
    CHECK_OK("Bits per sample"); // incomplete read check
    ok = ok && (bits_per_sample == 8 || bits_per_sample == 16);
    ASSERT(bits_per_sample == 8 || bits_per_sample == 16);
    CHECK_OK("Bits per sample"); // value check
    ret->m_bits_per_sample = bits_per_sample;

    // Read chunks until we find DATA
    bool found_data = false;
    u32 data_sz = 0;
    while (true) {
        u32 chunk_id; stream >> chunk_id;
        CHECK_OK("Reading chunk ID searching for data");
        stream >> data_sz;
        CHECK_OK("Reading chunk size searching for data");
        if (chunk_id == 0x61746164) { // DATA
            found_data = true;
            break;
        }
    }

    ok = ok && found_data;
    CHECK_OK("Found no data chunk");
    ASSERT(found_data);

    ok = ok && data_sz < std::numeric_limits<int>::max();
    CHECK_OK("Data was too large");

    // ### consider using BufferStream to do this for us
    ok = ok && int(data_sz) <= (buffer.size() - stream.offset());
    CHECK_OK("Bad DATA (truncated)");

    ret->m_sample_data = buffer.slice(stream.offset(), data_sz);

    // At this point there should be no read failures!
    ASSERT(!stream.handle_read_failure());
    return ret;
}

