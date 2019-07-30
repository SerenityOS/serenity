#include <AK/BufferStream.h>
#include <LibAudio/ABuffer.h>
#include <LibAudio/AWavLoader.h>
#include <LibCore/CFile.h>
#include <LibCore/CIODeviceStreamReader.h>
#include <limits>

AWavLoader::AWavLoader(const StringView& path)
    : m_file(path)
{
    if (!m_file.open(CIODevice::ReadOnly)) {
        m_error_string = String::format("Can't open file: %s", m_file.error_string());
        return;
    }

    parse_header();
}

RefPtr<ABuffer> AWavLoader::get_more_samples()
{
#ifdef AWAVLOADER_DEBUG
    dbgprintf("Read WAV of format PCM with num_channels %u sample rate %u, bits per sample %u\n", m_num_channels, m_sample_rate, m_bits_per_sample);
#endif

    auto raw_samples = m_file.read(128 * KB);
    if (raw_samples.is_empty())
        return nullptr;

    auto buffer = ABuffer::from_pcm_data(raw_samples, m_num_channels, m_bits_per_sample, m_sample_rate);
    m_loaded_samples += buffer->sample_count();
    return buffer;
}

bool AWavLoader::parse_header()
{
    CIODeviceStreamReader stream(m_file);

#define CHECK_OK(msg)                                                           \
    do {                                                                        \
        ASSERT(ok);                                                             \
        if (stream.handle_read_failure()) {                                     \
            m_error_string = String::format("Premature stream EOF at %s", msg); \
            return {};                                                          \
        }                                                                       \
        if (!ok) {                                                              \
            m_error_string = String::format("Parsing failed: %s", msg);         \
            return {};                                                          \
        } else {                                                                \
            dbgprintf("%s is OK!\n", msg);                                      \
        }                                                                       \
    } while (0);

    bool ok = true;
    u32 riff;
    stream >> riff;
    ok = ok && riff == 0x46464952; // "RIFF"
    CHECK_OK("RIFF header");

    u32 sz;
    stream >> sz;
    ok = ok && sz < 1024 * 1024 * 1024; // arbitrary
    CHECK_OK("File size");
    ASSERT(sz < 1024 * 1024 * 1024);

    u32 wave;
    stream >> wave;
    ok = ok && wave == 0x45564157; // "WAVE"
    CHECK_OK("WAVE header");

    u32 fmt_id;
    stream >> fmt_id;
    ok = ok && fmt_id == 0x20746D66; // "FMT"
    CHECK_OK("FMT header");

    u32 fmt_size;
    stream >> fmt_size;
    ok = ok && fmt_size == 16;
    CHECK_OK("FMT size");
    ASSERT(fmt_size == 16);

    u16 audio_format;
    stream >> audio_format;
    CHECK_OK("Audio format");     // incomplete read check
    ok = ok && audio_format == 1; // WAVE_FORMAT_PCM
    ASSERT(audio_format == 1);
    CHECK_OK("Audio format"); // value check

    stream >> m_num_channels;
    ok = ok && (m_num_channels == 1 || m_num_channels == 2);
    CHECK_OK("Channel count");

    stream >> m_sample_rate;
    CHECK_OK("Sample rate");

    u32 byte_rate;
    stream >> byte_rate;
    CHECK_OK("Byte rate");

    u16 block_align;
    stream >> block_align;
    CHECK_OK("Block align");

    stream >> m_bits_per_sample;
    CHECK_OK("Bits per sample"); // incomplete read check
    ok = ok && (m_bits_per_sample == 8 || m_bits_per_sample == 16 || m_bits_per_sample == 24);
    ASSERT(m_bits_per_sample == 8 || m_bits_per_sample == 16 || m_bits_per_sample == 24);
    CHECK_OK("Bits per sample"); // value check

    // Read chunks until we find DATA
    bool found_data = false;
    u32 data_sz = 0;
    while (true) {
        u32 chunk_id;
        stream >> chunk_id;
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

    ok = ok && data_sz < INT32_MAX;
    CHECK_OK("Data was too large");

    int bytes_per_sample = (m_bits_per_sample / 8) * m_num_channels;
    m_total_samples = data_sz / bytes_per_sample;

    // Just make sure we're good before we read the data...
    ASSERT(!stream.handle_read_failure());

    return true;
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

template<typename SampleReader>
static void read_samples_from_stream(BufferStream& stream, SampleReader read_sample, Vector<ASample>& samples, int num_channels, int source_rate)
{
    AResampleHelper resampler(source_rate, 44100);
    float norm_l = 0;
    float norm_r = 0;
    switch (num_channels) {
    case 1:
        while (!stream.handle_read_failure()) {
            resampler.prepare();
            while (resampler.read_sample()) {
                norm_l = read_sample(stream);
            }
            samples.append(ASample(norm_l));
        }
        break;
    case 2:
        while (!stream.handle_read_failure()) {
            resampler.prepare();
            while (resampler.read_sample()) {
                norm_l = read_sample(stream);
                norm_r = read_sample(stream);
            }
            samples.append(ASample(norm_l, norm_r));
        }
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

static float read_norm_sample_24(BufferStream& stream)
{
    u8 byte = 0;
    stream >> byte;
    u32 sample1 = byte;
    stream >> byte;
    u32 sample2 = byte;
    stream >> byte;
    u32 sample3 = byte;

    i32 value = 0;
    value = sample1 << 8;
    value |= (sample2 << 16);
    value |= (sample3 << 24);
    return float(value) / std::numeric_limits<i32>::max();
}

static float read_norm_sample_16(BufferStream& stream)
{
    i16 sample = 0;
    stream >> sample;
    return float(sample) / std::numeric_limits<i16>::max();
}

static float read_norm_sample_8(BufferStream& stream)
{
    u8 sample = 0;
    stream >> sample;
    return float(sample) / std::numeric_limits<u8>::max();
}

// ### can't const this because BufferStream is non-const
// perhaps we need a reading class separate from the writing one, that can be
// entirely consted.
RefPtr<ABuffer> ABuffer::from_pcm_data(ByteBuffer& data, int num_channels, int bits_per_sample, int source_rate)
{
    BufferStream stream(data);
    Vector<ASample> fdata;
    fdata.ensure_capacity(data.size() * 2);

#ifdef AWAVLOADER_DEBUG
    dbg() << "Reading " << bits_per_sample << " bits and " << num_channels << " channels, total bytes: " << data.size();
#endif

    switch (bits_per_sample) {
    case 8:
        read_samples_from_stream(stream, read_norm_sample_8, fdata, num_channels, source_rate);
        break;
    case 16:
        read_samples_from_stream(stream, read_norm_sample_16, fdata, num_channels, source_rate);
        break;
    case 24:
        read_samples_from_stream(stream, read_norm_sample_24, fdata, num_channels, source_rate);
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    // We should handle this in a better way above, but for now --
    // just make sure we're good. Worst case we just write some 0s where they
    // don't belong.
    ASSERT(!stream.handle_read_failure());

    // HACK: This is a total hack to remove an unnecessary sample at the end of the buffer.
    // FIXME: Don't generate the extra sample... :^)
    for (int i = 0; i < 1; ++i)
        fdata.take_last();

    return ABuffer::create_with_samples(move(fdata));
}
