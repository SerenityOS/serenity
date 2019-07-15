#pragma once

#include <AK/RefCounted.h>
#include <AK/ByteBuffer.h>
#include <AK/Types.h>
#include <AK/Vector.h>

// A single sample in an audio buffer.
// Values are floating point, and should range from -1.0 to +1.0
struct ASample {
    ASample()
        : left(0)
        , right(0)
    {}

    // For mono
    ASample(float left)
        : left(left)
        , right(left)
    {}

    // For stereo
    ASample(float left, float right)
        : left(left)
        , right(right)
    {}

    void clamp()
    {
        if (left > 1)
            left = 1;
        else if (left < -1)
            left = -1;

        if (right > 1)
            right = 1;
        else if (right < -1)
            right = -1;
    }

    ASample& operator+=(const ASample& other)
    {
        left += other.left;
        right += other.right;
        return *this;
    }

    float left;
    float right;
};

// A buffer of audio samples, normalized to 44100hz.
class ABuffer : public RefCounted<ABuffer> {
public:
    static RefPtr<ABuffer> from_pcm_data(ByteBuffer& data, int num_channels, int bits_per_sample, int source_rate);
    ABuffer(Vector<ASample>& samples)
        : m_samples(samples)
    {}

    const Vector<ASample>& samples() const { return m_samples; }
    Vector<ASample>& samples() { return m_samples; }
    const void* data() const { return m_samples.data(); }
    int size_in_bytes() const { return m_samples.size() * sizeof(ASample); }

private:
    Vector<ASample> m_samples;
};

