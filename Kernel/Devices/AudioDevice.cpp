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
#define AUDIODEVICE_DEBUG 1
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/ScopeGuard.h>
#include <Kernel/Devices/AudioDevice.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/VM/AnonymousVMObject.h>

namespace Kernel {

AsyncAudioDeviceRequest::AsyncAudioDeviceRequest(Device& audio_device, RequestType request_type, unsigned stream, const UserOrKernelBuffer& buffer, size_t buffer_size)
    : AsyncDeviceRequest(audio_device)
    , m_audio_device(static_cast<AudioDevice&>(audio_device))
    , m_request_type(request_type)
    , m_stream(stream)
    , m_buffer(buffer)
    , m_buffer_size(buffer_size)
{
}

void AsyncAudioDeviceRequest::start()
{
    m_audio_device.start_request(*this);
}

void AudioDevice::start_request(AsyncAudioDeviceRequest& request)
{
    if (request.stream() >= m_streams.size()) {
        dbgln("AudioDevice::start_request: No such stream: {}", request.stream());
        request.complete(AsyncDeviceRequest::Failure);
        return;
    }
    auto& stream = stream_for_request(request);
    switch (stream.type) {
    case Audio::StreamType::Playback:
        if (request.request_type() != AsyncAudioDeviceRequest::RequestType::Write) {
            dbgln("AudioDevice::start_request: Can only write to stream {}", request.stream());
            request.complete(AsyncDeviceRequest::Failure);
            return;
        }
        break;
    case Audio::StreamType::Record:
        if (request.request_type() != AsyncAudioDeviceRequest::RequestType::Read) {
            dbgln("AudioDevice::start_request: Can only read from stream {}", request.stream());
            request.complete(AsyncDeviceRequest::Failure);
            return;
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    ScopedSpinLock lock(m_request_lock);
    ScopeGuard log_guard([&] {
        dbgln_if(AUDIODEVICE_DEBUG, "<-- AudioDevice::start_request");
    });
    dbgln_if(AUDIODEVICE_DEBUG, "AudioDevice::start_request for stream {}", request.stream());
    if (stream.state != Stream::State::Prepared && stream.state != Stream::State::Running) {
        dbgln("AudioDevice::start_request: Stream {} not prepared or not in running state", request.stream());
        request.complete(AsyncDeviceRequest::Failure);
        return;
    }
    stream.current_request = &request;
    stream.request_buffer_offset = 0;
    if (stream.type == Audio::StreamType::Playback) {
        dbgln("AudioDevice::start_request processing buffers -->");
        while (do_transfer_request_buffer_playback(stream))
            ;
        dbgln("<-- AudioDevice::start_request processing buffers");
    } else {
        // TODO: Implement
        VERIFY_NOT_REACHED();
    }
}

bool AudioDevice::do_transfer_request_buffer_playback(Stream& stream)
{
    // This function copies up to one period of data until the next period boundary
    VERIFY(stream.buffer_read_offset <= stream.buffer_write_offset);
    VERIFY(stream.buffer_read_offset % stream.bytes_per_period == 0);
    size_t available_space = stream.bytes_all_periods - (stream.buffer_write_offset - stream.buffer_read_offset);
    dbgln("write offset {} read offset {} available {}", stream.buffer_write_offset, stream.buffer_read_offset, available_space);
    if (available_space == 0)
        return false;
    size_t period_offset = stream.buffer_write_offset % stream.bytes_per_period;
    size_t bytes_to_copy = stream.bytes_per_period - period_offset;
    VERIFY(bytes_to_copy <= available_space);
    size_t request_bytes_remaining = stream.current_request->buffer_size() - stream.request_buffer_offset;
    bool write_to_dma = false;
    bool period_full = false;
    if (bytes_to_copy > request_bytes_remaining)
        bytes_to_copy = request_bytes_remaining;
    else
        period_full = true;
    u8* dest_ptr;
    if (stream.dma_periods > 0 && stream.buffer_write_offset - stream.buffer_read_offset < stream.bytes_per_period * stream.dma_periods) {
        // Either we're too close to consuming those periods or we haven't triggered playback,
        // write directly to the dma buffer
        dest_ptr = playback_current_dma_period(stream, true) + period_offset;
        write_to_dma = true;
        dbgln("AudioDevice::do_transfer_request_buffer_playback writing {} bytes to dma at offset {}", bytes_to_copy, period_offset);
    } else {
        dest_ptr = stream.buffer_region->vaddr().as_ptr();
        dest_ptr += stream.buffer_write_offset % stream.bytes_all_periods;
        dbgln("AudioDevice::do_transfer_request_buffer_playback writing {} bytes to buffer at offset {}", bytes_to_copy, period_offset);
    }

    if (!stream.current_request->buffer().read(dest_ptr, bytes_to_copy)) {
        complete_current_request(stream, AsyncDeviceRequest::RequestResult::MemoryFault);
        return false;
    }

    stream.buffer_write_offset += bytes_to_copy;
    stream.request_buffer_offset += bytes_to_copy;

    if (stream.state == Stream::State::Prepared && period_full && (stream.current.periods_trigger == 0 || stream.buffer_write_offset / stream.bytes_per_period == stream.current.periods_trigger)) {
        if (!period_full && stream.current.periods_trigger == 0) {
            dbgln("AudioDevice::do_transfer_request_buffer_playback triggering playback with incomplete period, will cause glitches!");
            __builtin_memset(dest_ptr + bytes_to_copy, 0, stream.bytes_per_period - bytes_to_copy); // TODO: silence samples
            // TODO: we should probably stop playback after we run out of data and trigger an xrun instead
        }
        transferred_to_dma_buffer(stream, false);
        dbgln("Triggering playback");
        stream.state = Stream::State::Running;
        Processor::current().deferred_call_queue([this, &stream] {
            trigger_playback(stream);
        });
    } else if (period_full && write_to_dma) {
        // Tell the driver to advance to the next dma buffer
        transferred_to_dma_buffer(stream, stream.state == Stream::State::Running);
        dbgln("in state {}", (int)stream.state);
    }
    if (stream.request_buffer_offset >= stream.current_request->buffer_size()) {
        complete_current_request(stream, AsyncDeviceRequest::RequestResult::Success);
        return false; // This doesn't indicate an error!
    }
    return true;
}

void AudioDevice::finished_playing_period(Stream& stream)
{
    // NOTE: this may be called from the interrupt handler!
    VERIFY(m_request_lock.is_locked());
    VERIFY(stream.buffer_write_offset >= stream.buffer_read_offset);
    size_t have_bytes = stream.buffer_write_offset - stream.buffer_read_offset;
    if (have_bytes > stream.bytes_per_period)
        have_bytes = stream.bytes_per_period;
    dbgln("finished_playing_period write: {} read: {} have bytes: {} period: {} bytes", stream.buffer_write_offset, stream.buffer_read_offset, have_bytes, stream.bytes_per_period);
    size_t period_offset = stream.buffer_read_offset % stream.bytes_all_periods;
    u8* period_to_read = stream.buffer_region->vaddr().offset(period_offset).as_ptr();
    stream.buffer_read_offset += stream.bytes_per_period;
    u8* dma_write_ptr = playback_current_dma_period(stream, true);
    if (have_bytes > 0)
        __builtin_memcpy(dma_write_ptr, period_to_read, have_bytes);
    if (have_bytes < stream.bytes_per_period) {
        u8* zero_ptr = dma_write_ptr + have_bytes;
        dbgln("Fill {} bytes at {} with silence", stream.bytes_per_period - have_bytes, VirtualAddress(zero_ptr));
        __builtin_memset(zero_ptr, 0, stream.bytes_per_period - have_bytes); // TODO: silence samples

        // TODO: schedule xrun after this is played
        stream.buffer_write_offset = stream.buffer_read_offset;
        transferred_to_dma_buffer(stream, false); // advances periods written
        dbgln("AudioDevice::finished_playing_period writer too slow!");
    } else {
        dbgln("AudioDevice::finished_playing_period");
    }

    transferred_to_dma_buffer(stream, true);
}

void AudioDevice::complete_current_request(Stream& stream, AsyncDeviceRequest::RequestResult result)
{
    // NOTE: this may be called from the interrupt handler!
    VERIFY(stream.current_request);
    VERIFY(m_request_lock.is_locked());

    // Now schedule reading back the buffer as soon as we leave the irq handler.
    // This is important so that we can safely write the buffer back,
    // which could cause page faults.
    m_work_queue.queue([this, result, &stream]() {
        ScopedSpinLock lock(m_request_lock);
        VERIFY(stream.current_request);
        auto& request = *stream.current_request;
        stream.current_request = nullptr;

        dbgln_if(AUDIODEVICE_DEBUG, "AudioDevice::complete_current_request stream: {} result: {}", request.stream(), (int)result);

        request.complete(result);
    });
}

KResultOr<size_t> AudioDevice::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t buffer_size)
{
    unsigned stream = 0; // TODO: get from FileDescription?
    auto read_request = make_request<AsyncAudioDeviceRequest>(AsyncAudioDeviceRequest::Read, stream, buffer, buffer_size);
    auto result = read_request->wait();
    if (result.wait_result().was_interrupted())
        return EINTR;
    switch (result.request_result()) {
    case AsyncDeviceRequest::Failure:
    case AsyncDeviceRequest::Cancelled:
        return EIO;
    case AsyncDeviceRequest::MemoryFault:
        return EFAULT;
    default:
        break;
    }
    return read_request->result_size();
}

KResultOr<size_t> AudioDevice::write(FileDescription&, u64, const UserOrKernelBuffer& buffer, size_t buffer_size)
{
    unsigned stream = 0; // TODO: get from FileDescription?
    auto write_request = make_request<AsyncAudioDeviceRequest>(AsyncAudioDeviceRequest::Write, stream, buffer, buffer_size);
    auto result = write_request->wait();
    if (result.wait_result().was_interrupted())
        return EINTR;
    switch (result.request_result()) {
    case AsyncDeviceRequest::Failure:
    case AsyncDeviceRequest::Cancelled:
        return EIO;
    case AsyncDeviceRequest::MemoryFault:
        return EFAULT;
    default:
        break;
    }
    return write_request->result_size();
}

int AudioDevice::handle_json_ioctl(FileDescription&, unsigned request, Audio::IOCtlJsonParams& params)
{
    String in;
    if (params.in_buffer) {
        in = copy_string_from_user((const char*)params.in_buffer, params.in_buffer_size);
        if (in.is_null())
            return -EFAULT;
    }

    KBufferBuilder builder;

    switch ((Audio::IOCtl)request) {
    case Audio::IOCtl::GET_PCM_HW_PARAMS: {
        JsonArraySerializer<KBufferBuilder> json { builder };
        auto build_params = [&](size_t stream_index, const Stream& stream) {
            if (stream.supported.is_null())
                return;
            auto out_obj = json.add_object();
            out_obj.add("name", stream.name);
            out_obj.add("index", stream_index);
            out_obj.add("type", (unsigned)stream.type);
            auto supported_obj = out_obj.add_object("supported");
            {
                auto array = supported_obj.add_array("formats");
                for (size_t i = 0; stream.supported.formats[i] != Audio::PCM::SampleFormat::Unknown; i++)
                    array.add((unsigned)stream.supported.formats[i]);
            }
            {
                auto array = supported_obj.add_array("layouts");
                for (size_t i = 0; stream.supported.layouts[i] != Audio::PCM::SampleLayout::Unknown; i++)
                    array.add((unsigned)stream.supported.layouts[i]);
            }
            {
                auto array = supported_obj.add_array("rates");
                for (size_t i = 0; stream.supported.rates[i] != 0; i++)
                    array.add((unsigned)stream.supported.rates[i]);
            }
            {
                auto array = supported_obj.add_array("channels");
                for (size_t i = 0; stream.supported.channels[i] != 0; i++)
                    array.add((unsigned)stream.supported.channels[i]);
            }
            supported_obj.add("periods_min", stream.supported.periods_min);
            supported_obj.add("periods_max", stream.supported.periods_max);
            supported_obj.finish();
            if (!stream.current.is_null()) {
                auto current_obj = out_obj.add_object("current");
                current_obj.add("format", (unsigned)stream.current.format);
                current_obj.add("layout", (unsigned)stream.current.layout);
                current_obj.add("rate", (unsigned)stream.current.rate);
                current_obj.add("channels", (unsigned)stream.current.channels);
                current_obj.add("periods", (unsigned)stream.current.periods);
                current_obj.add("periods_trigger", (unsigned)stream.current.periods_trigger);
                current_obj.add("period_ns", stream.current.period_ns);
                current_obj.finish();
            }
        };
        for (size_t i = 0; i < m_streams.size(); i++)
            build_params(i, m_streams[i]);
        json.finish();
        break;
    }
    default:
        return -EINVAL;
    }

    auto generated_data = builder.build();
    if (!generated_data)
        return -ENOMEM;
    auto available_buffer_size = params.out_buffer_size;
    params.out_buffer_size = generated_data->size();
    if (available_buffer_size > generated_data->size()) {
        auto user_buffer = UserOrKernelBuffer::for_user_buffer((u8*)params.out_buffer, params.out_buffer_size);
        if (!user_buffer.has_value())
            return -EFAULT;
        if (!user_buffer.value().write(generated_data->data(), params.out_buffer_size))
            return -EFAULT;
    }
    return 0;
}

bool AudioDevice::setup_pcm_periods_buffers(Stream& stream)
{
    stream.bytes_per_period = (size_t)Audio::PCM::time_to_frames(stream.current.period_ns, stream.current.rate)
        * Audio::PCM::bytes_per_frame(stream.current.format, stream.current.channels);
    stream.bytes_all_periods = stream.bytes_per_period * stream.current.periods;

    dbgln_if(AUDIODEVICE_DEBUG, "AudioDevice::setup_pcm_periods_buffers bytes_per_period: {} periods: {}", stream.bytes_per_period, stream.current.periods);

    size_t total_bytes = stream.bytes_per_period * stream.current.periods;
    stream.buffer_region = MM.allocate_kernel_region(page_round_up(total_bytes), "Audio Device Buffer", Region::Access::Read | Region::Access::Write);
    if (!stream.buffer_region) {
        dbgln_if(AUDIODEVICE_DEBUG, "AudioDevice::setup_pcm_periods_buffers failed to set up period buffers");
        return false;
    }
    stream.buffer_write_offset = 0;
    stream.buffer_read_offset = 0;
    return true;
}

template<typename EntryType, EntryType LastValue>
static bool is_in_list(const EntryType* list, EntryType find_value)
{
    for (size_t i = 0; list[i] != LastValue; i++) {
        if (list[i] == find_value)
            return true;
    }
    return false;
}

bool AudioDevice::is_valid_pcm_configuration(const CurrentPCM& config, const SupportedPCM& supported)
{
    if (!is_in_list<Audio::PCM::SampleFormat, Audio::PCM::SampleFormat::Unknown>(supported.formats, config.format))
        return false;
    if (!is_in_list<Audio::PCM::SampleLayout, Audio::PCM::SampleLayout::Unknown>(supported.layouts, config.layout))
        return false;
    if (!is_in_list<unsigned, 0>(supported.rates, config.rate))
        return false;
    if (!is_in_list<unsigned, 0>(supported.channels, config.channels))
        return false;
    if (config.periods < supported.periods_min || config.periods > supported.periods_max)
        return false;
    if (config.periods_trigger != 0 && (config.periods_trigger < supported.periods_min || config.periods_trigger > supported.periods_max))
        return false;
    return true;
}

bool AudioDevice::set_hw_params(Stream& stream, const Audio::IOCtlSetPCMHwParams& params)
{
    auto new_conf = stream.current;
    if (params.format != Audio::PCM::SampleFormat::Unknown)
        new_conf.format = params.format;
    if (params.layout != Audio::PCM::SampleLayout::Unknown)
        new_conf.layout = params.layout;
    if (params.rate != 0)
        new_conf.rate = params.rate;
    if (params.periods != 0)
        new_conf.periods = params.periods;
    
    new_conf.periods_trigger = params.periods_trigger;
    if (params.period_ns != 0)
        new_conf.period_ns = params.period_ns;
    // Validate that it's within the supported spec
    if (!is_valid_pcm_configuration(new_conf, stream.supported)) {
        dbgln_if(AUDIODEVICE_DEBUG, "AudioDevice::set_hw_params cannot set hw params: unsupported value");
        return false;
    }
    // Check if the device can handle the combination
    if (!can_support_pcm_configuration(stream, new_conf)) {
        dbgln_if(AUDIODEVICE_DEBUG, "AudioDevice::set_hw_params cannot set hw params: unsupported combination");
        return false;
    }

    stream.current = new_conf;
    return true;
}

bool AudioDevice::pcm_prepare(Stream& stream)
{
    if (!setup_pcm_periods_buffers(stream)) {
        dbgln("AudioDevice::pcm_prepare: Failed to setup period buffers");
        return false;
    }
    if (!do_initialize(stream)) {
        dbgln("AudioDevice::pcm_prepare: Failed to initialize stream");
        return false;
    }
    stream.state = Stream::State::Prepared;
    return true;
}

int AudioDevice::ioctl(FileDescription& description, unsigned request, FlatPtr arg)
{
    Stream* stream = &m_streams[0]; // TODO: get from description (may be null if not selected yet)
    dbgln_if(AUDIODEVICE_DEBUG, "AudioDevice::ioctl {}", request);

    switch ((Audio::IOCtl)request) {
    case Audio::IOCtl::GET_PCM_HW_PARAMS: {
        auto* user_params = (Audio::IOCtlJsonParams*)arg;
        Audio::IOCtlJsonParams params;
        if (!copy_from_user(&params, user_params))
            return -EINVAL;
        int result = handle_json_ioctl(description, request, params);
        if (!copy_to_user(user_params, &params))
            return -EFAULT;
        return result;
    }
    case Audio::IOCtl::SELECT_STREAM: {
        unsigned stream_index = (unsigned)arg;
        (void)stream_index; // TODO: save m_streams[stream_index] in description
        stream->state = Stream::State::Setup;
        return 0;
    }
    case Audio::IOCtl::SET_PCM_HW_PARAMS: {
        dbgln("SET_PCM_HW_PARAMS?");
        if (!stream || stream->state != Stream::State::Setup)
            return -EINVAL;
        Audio::IOCtlSetPCMHwParams params;
        if (!copy_from_user(&params, (Audio::IOCtlSetPCMHwParams*)arg))
            return -EINVAL;
        dbgln("SET_PCM_HW_PARAMS...");
        return set_hw_params(*stream, params) ? 0 : -EINVAL;
    }
    case Audio::IOCtl::PCM_PREPARE: {
        dbgln("PCM_PREPARE?");
        if (!stream || stream->state != Stream::State::Setup)
            return -EINVAL;
        dbgln("PCM_PREPARE...");
        return pcm_prepare(*stream) ? 0 : -EINVAL;
    }
    default:
        return -EINVAL;
    }
}

}
