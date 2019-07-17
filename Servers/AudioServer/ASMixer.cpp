#include <AK/BufferStream.h>
#include <LibCore/CThread.h>

#include <limits>
#include "ASMixer.h"

ASMixer::ASMixer()
    : m_device("/dev/audio")
{
    if (!m_device.open(CIODevice::WriteOnly)) {
        dbgprintf("Can't open audio device: %s\n", m_device.error_string());
        return;
    }

    CThread sound_thread([](void* context) -> int {
        ASMixer* mixer = (ASMixer*)context;
        mixer->mix();
        return 0;
    }, this);
}

void ASMixer::queue(ASClientConnection&, const ABuffer& buffer)
{
    ASSERT(buffer.size_in_bytes());
    CLocker lock(m_lock);
    m_pending_mixing.append(ASMixerBuffer(buffer));
}

void ASMixer::mix()
{
    Vector<ASMixerBuffer> active_mix_buffers;

    for (;;) {
        {
            CLocker lock(m_lock);
            for (const auto& buf : m_pending_mixing)
                active_mix_buffers.append(buf);
            m_pending_mixing.clear();
        }

        // ### use a wakeup of some kind rather than this garbage
        if (active_mix_buffers.size() == 0) {
            // nothing to mix yet
            usleep(10000);
            continue;
        }

        int max_size = 0;

        for (auto& buffer : active_mix_buffers) {
            if (buffer.done)
                continue;
            ASSERT(buffer.buffer->size_in_bytes()); // zero sized buffer? how?
            max_size = max(max_size, buffer.buffer->size_in_bytes() - buffer.pos);
        }

        // ### clear up 'done' buffers more aggressively
        if (max_size == 0) {
            active_mix_buffers.clear();
            continue;
        }

        max_size = min(1023, max_size);

        Vector<ASample> mixed_buffer;
        mixed_buffer.resize(max_size);

        // Mix the buffers together into the output
        for (auto& buffer : active_mix_buffers) {
            if (buffer.done)
                continue;
            auto& samples = buffer.buffer->samples();

            for (int i = 0; i < max_size && buffer.pos < samples.size(); ++buffer.pos, ++i) {
                auto& mixed_sample = mixed_buffer[i];
                mixed_sample += samples[buffer.pos];
            }

            // clear it later
            if (buffer.pos == samples.size())
                buffer.done = true;
        }

        // output the mixed stuff to the device
        // max_size is 0 indexed, so add 1.
        const int output_buffer_byte_size = (max_size + 1) * 2 * 2;
        ASSERT(output_buffer_byte_size == 4096);
        ByteBuffer buffer(ByteBuffer::create_uninitialized(output_buffer_byte_size));
        BufferStream stream(buffer);

        for (int i = 0; i < mixed_buffer.size(); ++i) {
            auto& mixed_sample = mixed_buffer[i];
            mixed_sample.clip();

            i16 out_sample;
            out_sample = mixed_sample.left * std::numeric_limits<i16>::max();
            stream << out_sample;

            ASSERT(!stream.at_end()); // we should have enough space for both channels in one buffer!
            out_sample = mixed_sample.right * std::numeric_limits<i16>::max();
            stream << out_sample;

            ASSERT(!stream.at_end());
        }

        if (stream.offset() != 0) {
            buffer.trim(stream.offset());
            m_device.write(buffer);
            mixed_buffer.resize(0);
        }
    }
}
