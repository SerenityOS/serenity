#include <LibCore/CFile.h>
#include <AK/BufferStream.h>
#include <limits>

#include "AWavLoader.h"
#include "ABuffer.h"

RefPtr<ABuffer> AWavLoader::load_wav(const StringView& path)
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
RefPtr<ABuffer> AWavLoader::parse_wav(ByteBuffer& buffer)
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

    u16 audio_format; stream >> audio_format;
    CHECK_OK("Audio format"); // incomplete read check
    ok = ok && audio_format == 1; // WAVE_FORMAT_PCM
    ASSERT(audio_format == 1);
    CHECK_OK("Audio format"); // value check

    u16 num_channels; stream >> num_channels;
    ok = ok && (num_channels == 1 || num_channels == 2);
    CHECK_OK("Channel count");

    u32 sample_rate; stream >> sample_rate;
    CHECK_OK("Sample rate");

    u32 byte_rate; stream >> byte_rate;
    CHECK_OK("Byte rate");

    u16 block_align; stream >> block_align;
    CHECK_OK("Block align");

    u16 bits_per_sample; stream >> bits_per_sample;
    CHECK_OK("Bits per sample"); // incomplete read check
    ok = ok && (bits_per_sample == 8 || bits_per_sample == 16);
    ASSERT(bits_per_sample == 8 || bits_per_sample == 16);
    CHECK_OK("Bits per sample"); // value check

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

    // Just make sure we're good before we read the data...
    ASSERT(!stream.handle_read_failure());

    auto sample_data = buffer.slice(stream.offset(), data_sz);

    dbgprintf("Read WAV of format PCM with num_channels %d sample rate %d, bits per sample %d\n", num_channels, sample_rate, bits_per_sample);

    return ABuffer::from_pcm_data(sample_data, num_channels, bits_per_sample, sample_rate);
}

// Small helper to resample from one playback rate to another
// This isn't really "smart", in that we just insert (or drop) samples.
// Should do better...
class AResampleHelper {
public:
    AResampleHelper(float source, float target);
    bool read_sample();
    void prepare();
private:
    const float m_ratio;
    float m_current_ratio { 0 };
};

AResampleHelper::AResampleHelper(float source, float target)
    : m_ratio(source / target)
{
}

void AResampleHelper::prepare()
{
    m_current_ratio += m_ratio;
}

bool AResampleHelper::read_sample()
{
    if (m_current_ratio > 1) {
        m_current_ratio--;
        return true;
    }

    return false;
}

template <typename T>
static void read_samples_from_stream(BufferStream& stream, Vector<ASample>& samples, int num_channels, int source_rate)
{
    AResampleHelper resampler(source_rate, 44100);
    T sample = 0;
    float norm_l = 0;
    float norm_r = 0;
    switch (num_channels) {
    case 1:
        while (!stream.handle_read_failure()) {
            resampler.prepare();
            while (resampler.read_sample()) {
                stream >> sample;
                norm_l = float(sample) / std::numeric_limits<T>::max();
            }
            samples.append(ASample(norm_l));
        }
        break;
    case 2:
        while (!stream.handle_read_failure()) {
            resampler.prepare();
            while (resampler.read_sample()) {
                stream >> sample;
                norm_l = float(sample) / std::numeric_limits<T>::max();
                stream >> sample;
                norm_r = float(sample) / std::numeric_limits<T>::max();
            }
            samples.append(ASample(norm_l, norm_r));
        }
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

// ### can't const this because BufferStream is non-const
// perhaps we need a reading class separate from the writing one, that can be
// entirely consted.
RefPtr<ABuffer> ABuffer::from_pcm_data(ByteBuffer& data, int num_channels, int bits_per_sample, int source_rate)
{
    BufferStream stream(data);
    Vector<ASample> fdata;
    fdata.ensure_capacity(data.size() * 2);

    dbg() << "Reading " << bits_per_sample << " bits and " << num_channels << " channels, total bytes: " << data.size();

    switch (bits_per_sample) {
    case 8:
        read_samples_from_stream<u8>(stream, fdata, num_channels, source_rate);
        break;
    case 16:
        read_samples_from_stream<i16>(stream, fdata, num_channels, source_rate);
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    // We should handle this in a better way above, but for now --
    // just make sure we're good. Worst case we just write some 0s where they
    // don't belong.
    ASSERT(!stream.handle_read_failure());

    return adopt(*new ABuffer(fdata));
}
