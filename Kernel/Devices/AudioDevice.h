/*
 * Copyright (c) 2021, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/API/AudioDevice.h>
#include <Kernel/Devices/AsyncDeviceRequest.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

class AudioDevice;

class AsyncAudioDeviceRequest : public AsyncDeviceRequest {
public:
    enum RequestType {
        Read,
        Write
    };
    AsyncAudioDeviceRequest(Device& audio_device, RequestType request_type, unsigned stream, const UserOrKernelBuffer& buffer, size_t buffer_size);

    RequestType request_type() const { return m_request_type; }
    unsigned stream() const { return m_stream; }
    UserOrKernelBuffer& buffer() { return m_buffer; }
    const UserOrKernelBuffer& buffer() const { return m_buffer; }
    size_t buffer_size() const { return m_buffer_size; }
    size_t result_size() const { return m_result_size; }

    virtual void start() override;
    virtual const char* name() const override
    {
        switch (m_request_type) {
        case Read:
            return "AudioDeviceRequest (read)";
        case Write:
            return "AudioDeviceRequest (write)";
        default:
            VERIFY_NOT_REACHED();
        }
    }

private:
    AudioDevice& m_audio_device;
    const RequestType m_request_type;
    const unsigned m_stream;
    UserOrKernelBuffer m_buffer;
    const size_t m_buffer_size;
    size_t m_result_size { 0 };
};

class AudioDevice : public CharacterDevice {
public:
    // ^CharacterDevice
    virtual bool can_read(const FileDescription&, size_t) const override { return false; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }

    virtual int ioctl(FileDescription&, unsigned, FlatPtr) override;

    void start_request(AsyncAudioDeviceRequest&);

protected:
    AudioDevice(unsigned major, unsigned minor)
        : CharacterDevice(major, minor)
        , m_work_queue("AudioDevice")
    {
    }

    struct SupportedPCM {
        const Audio::PCM::SampleFormat* formats { nullptr };
        const Audio::PCM::SampleLayout* layouts { nullptr };
        const unsigned* rates { nullptr };
        const unsigned* channels { nullptr };
        unsigned periods_min { 0 };
        unsigned periods_max { 0 };

        bool is_null() const
        {
            return !formats || !layouts || !rates || !channels || periods_min == 0 || periods_max == 0;
        }
    };
    struct CurrentPCM {
        Audio::PCM::SampleFormat format { Audio::PCM::SampleFormat::Unknown };
        Audio::PCM::SampleLayout layout { Audio::PCM::SampleLayout::Unknown };
        unsigned rate { 0 };
        unsigned channels { 0 };
        unsigned periods { 0 };
        unsigned periods_trigger { 0 };
        u64 period_ns { 0 };

        bool is_null() const
        {
            return format == Audio::PCM::SampleFormat::Unknown || layout == Audio::PCM::SampleLayout::Unknown
                || rate == 0 || channels == 0 || periods == 0 || periods_trigger == 0 || period_ns == 0;
        }
    };
    struct Stream {
        enum class State {
            Uninitialized = 0,
            Setup,
            Prepared,
            Running,
        };
        const char* name { nullptr };
        void* private_data { nullptr };
        Audio::StreamType type { Audio::StreamType::Unknown };
        SupportedPCM supported;
        CurrentPCM current;
        State state { State::Uninitialized };
        size_t dma_periods { 0 };
        OwnPtr<Region> dma_region { };

        size_t bytes_per_period { 0 };
        size_t bytes_all_periods { 0 };
        OwnPtr<Region> buffer_region { };
        size_t buffer_write_offset { 0 }; // points to where the next write will write to
        size_t buffer_read_offset { 0 }; // points to where the next read will start

        AsyncAudioDeviceRequest* current_request { nullptr };
        size_t request_buffer_offset { 0 };
    };

    Stream& stream_for_request(AsyncAudioDeviceRequest& request)
    {
        return m_streams[request.stream()];
    }

    virtual bool can_support_pcm_configuration(Stream&, const CurrentPCM&) { return false; }

    void complete_current_request(Stream&, AsyncDeviceRequest::RequestResult);
    void finished_playing_period(Stream&);

    virtual bool do_initialize(Stream&) = 0;
    virtual void trigger_playback(Stream&) = 0;
    virtual void transferred_to_dma_buffer(Stream&, bool) = 0;
    virtual u8* playback_current_dma_period(Stream&, bool) = 0;

    SpinLock<u8> m_request_lock;
    Vector<Stream, 1> m_streams;
    OwnPtr<Region> m_periods;
    WorkQueue m_work_queue;

private:
    int handle_json_ioctl(FileDescription&, unsigned, Audio::IOCtlJsonParams&);
    bool set_hw_params(Stream&, const Audio::IOCtlSetPCMHwParams&);
    bool pcm_prepare(Stream&);
    static bool is_valid_pcm_configuration(const CurrentPCM&, const SupportedPCM&);
    bool setup_pcm_periods_buffers(Stream&);
    bool do_transfer_request_buffer_playback(Stream&);
};

}
