#include <AK/BufferStream.h>
#include <AudioServer/ASClientConnection.h>
#include <AudioServer/ASMixer.h>
#include <LibAudio/ASAPI.h>
#include <LibCore/CThread.h>
#include <limits>

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

void ASMixer::queue(ASClientConnection& client, const ABuffer& buffer, int buffer_id)
{
    ASSERT(buffer.size_in_bytes());
    CLocker lock(m_lock);
    m_pending_mixing.append(ASMixerBuffer(buffer, client, buffer_id));
}

void ASMixer::mix()
{
    Vector<ASMixerBuffer> active_mix_buffers;

    for (;;) {
        {
            CLocker lock(m_lock);
            active_mix_buffers.append(move(m_pending_mixing));
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
            if (buffer.pos == samples.size()) {
                if (buffer.m_buffer_id && buffer.m_client) {
                    ASAPI_ServerMessage reply;
                    reply.type = ASAPI_ServerMessage::Type::FinishedPlayingBuffer;
                    reply.playing_buffer.buffer_id = buffer.m_buffer_id;
                    buffer.m_client->post_message(reply);
                }
                buffer.done = true;
            }
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

ASMixer::ASMixerBuffer::ASMixerBuffer(const NonnullRefPtr<ABuffer>& buf, ASClientConnection& client, int buffer_id)
    : buffer(buf)
    , m_client(client.make_weak_ptr())
    , m_buffer_id(buffer_id)
{
}
