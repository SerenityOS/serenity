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

#include <AK/Debug.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <LibAudio/Device.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/select.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <unistd.h>

namespace Audio {

void Device::Stream::parse(unsigned stream_index, const JsonObject& stream_obj)
{
    m_index = stream_index;

    m_name = stream_obj.get("name").to_string();
    m_type = (StreamType)stream_obj.get("type").to_u32();

    auto& supported_obj = stream_obj.get_ptr("supported")->as_object();
    auto& supported_formats = supported_obj.get_ptr("formats")->as_array();
    m_supported_formats.clear_with_capacity();
    m_supported_formats.ensure_capacity(supported_formats.size());
    supported_formats.for_each([&](auto& value) {
        m_supported_formats.append((PCM::SampleFormat)value.to_u32());
    });
    auto& supported_layouts = supported_obj.get_ptr("layouts")->as_array();
    m_supported_layouts.clear_with_capacity();
    m_supported_layouts.ensure_capacity(supported_layouts.size());
    supported_layouts.for_each([&](auto& value) {
        m_supported_layouts.append((PCM::SampleLayout)value.to_u32());
    });
    auto& supported_rates = supported_obj.get_ptr("rates")->as_array();
    m_supported_rates.clear_with_capacity();
    m_supported_rates.ensure_capacity(supported_rates.size());
    supported_rates.for_each([&](auto& value) {
        m_supported_rates.append(value.to_u32());
    });
    auto& supported_channels = supported_obj.get_ptr("channels")->as_array();
    m_supported_channels.clear_with_capacity();
    m_supported_channels.ensure_capacity(supported_channels.size());
    supported_channels.for_each([&](auto& value) {
        m_supported_channels.append(value.to_u32());
    });
    if (auto* current_value = stream_obj.get_ptr("current")) {
        auto& current_obj = current_value->as_object();
        m_current_params.format = (PCM::SampleFormat)current_obj.get("format").to_u32();
        m_current_params.layout = (PCM::SampleLayout)current_obj.get("layout").to_u32();
        m_current_params.rate = current_obj.get("rate").to_u32();
        m_current_params.channels = current_obj.get("channels").to_u32();
        m_current_params.periods = current_obj.get("periods").to_u32();
        m_current_params.periods_trigger = current_obj.get("periods_triggr").to_u32();
        m_current_params.period_ns = current_obj.get("period_ns").to_number<u64>();
    } else {
        m_current_params = {};
    }
}

bool Device::Stream::set_format(PCM::SampleFormat format)
{
    m_current_params.format = format;
    return m_device.set_pcm_hw_params();
}

bool Device::Stream::set_layout(PCM::SampleLayout layout)
{
    m_current_params.layout = layout;
    return m_device.set_pcm_hw_params();
}

bool Device::Stream::set_rate(unsigned rate)
{
    m_current_params.rate = rate;
    return m_device.set_pcm_hw_params();
}

bool Device::Stream::set_channels(unsigned channels)
{
    m_current_params.channels = channels;
    return m_device.set_pcm_hw_params();
}

Device::Device(const StringView& filename, Object* parent)
    : IODevice(parent)
    , m_filename(filename)
{
}

auto Device::find_stream(unsigned stream_index) -> Stream*
{
    for (auto& stream : m_streams) {
        if (stream.index() == stream_index)
            return &stream;
    }
    return nullptr;
}

bool Device::open(IODevice::OpenMode mode)
{
    return open_impl(mode, 0666);
}

bool Device::close()
{
    auto result = IODevice::close();
    m_state = State::Closed;
    m_selected_stream = 0;
    return result;
}

int Device::json_ioctl(Audio::IOCtl request, const StringView& in, String* out)
{
    Audio::IOCtlJsonParams params { };
    Vector<char, 1024> buffer;
    if (out)
        buffer.resize(1024);

    int result = 0;
    int attempts = 0;
    for (;;) {
        if (!in.is_null()) {
            params.in_buffer = in.characters_without_null_termination();
            params.in_buffer_size = in.length();
        }
        if (out) {
            params.out_buffer = &buffer[0];
            params.out_buffer_size = buffer.size();
        }

        result = audio_ioctl(fd(), request, params);
        if (!out || result >= 0 || result != -EINVAL)
            break;
        if (++attempts >= 2) {
            dbgln("Giving up on sending audio ioctl: {}", (unsigned)request);
            return -EINVAL;
        }
        // See if we should grow the buffer and retry
        if (params.out_buffer_size > buffer.size())
            buffer.resize(params.out_buffer_size);
    }
    if (out && result >= 0) {
        if (params.out_buffer_size > 0) {
            *out = String((const char*)params.out_buffer, params.out_buffer_size);
        } else {
            *out = String::empty();
        }
    }
    return result;
}

bool Device::open_impl(IODevice::OpenMode mode, mode_t permissions)
{
    if (m_state >= Device::State::Open && !close())
        return false;

    VERIFY(!m_filename.is_null());
    int flags = 0;
    if ((mode & IODevice::ReadWrite) == IODevice::ReadWrite) {
        flags |= O_RDWR | O_CREAT | O_APPEND;
    } else if (mode & IODevice::ReadOnly) {
        flags |= O_RDONLY;
    } else if (mode & IODevice::WriteOnly) {
        flags |= O_WRONLY | O_CREAT | O_APPEND;
    }
    if (mode & IODevice::Truncate)
        flags |= O_TRUNC;
    int fd = ::open(m_filename.characters(), flags, permissions);
    if (fd < 0) {
        set_error(errno);
        return false;
    }

    set_fd(fd);
    set_mode(mode);

    m_state = Device::State::Open;
    if (!get_pcm_hw_params()) {
        close();
        return false;
    }
    return true;
}

bool Device::get_pcm_hw_params()
{
    if (m_state < Device::State::Open)
        return false;

    String hw_params;
    if (json_ioctl(Audio::IOCtl::GET_PCM_HW_PARAMS, nullptr, &hw_params) < 0) {
        dbgln("GET_PCM_HW_PARAMS failed");
        return false;
    }
    dbgln("GET_PCM_HW_PARAMS returned: '{}'", hw_params);
    auto json = JsonValue::from_string(hw_params);
    VERIFY(json.has_value());
    const JsonArray& streams_array = json.value().as_array();

    // TODO: remove no longer existing streams
    streams_array.for_each([&](auto& value) {
        const auto& stream_obj = value.as_object();
        unsigned stream_index = stream_obj.get("index").to_u32();
        if (auto* existing_stream = find_stream(stream_index)) {
            existing_stream->parse(stream_index, stream_obj);
        } else {
            Stream new_stream(*this);
            new_stream.parse(stream_index, stream_obj);
            m_streams.append(move(new_stream));
        }
    });
    return true;
}

bool Device::set_pcm_hw_params()
{
    if (m_state < Device::State::Selected)
        return false;

    auto* stream = find_stream(m_selected_stream);
    if (!stream)
        return false;

    if (audio_ioctl(fd(), Audio::IOCtl::SET_PCM_HW_PARAMS, stream->m_current_params) < 0) {
        dbgln("Failed to set hw params");
        // NOTE: We don't want to call get_pcm_hw_params here because that
        // would wipe out all current params. In this case the caller should
        // either revert to the setting used before, or pick another one.
        m_state = Device::State::Selected;
        return false;
    }

    // Now get the latest values
    bool result = get_pcm_hw_params();
    stream = find_stream(m_selected_stream);
    if (stream) {
        // the stream is still there
        m_state = stream->is_setup() ? Device::State::Setup : Device::State::Selected;
    } else {
        // The stream is gone now...
        m_state = Device::State::Open;
        m_selected_stream = 0;
    }
    return result;
}

bool Device::pcm_prepare()
{
    if (m_state != Device::State::Setup)
        return false;
    if (audio_ioctl(fd(), Audio::IOCtl::PCM_PREPARE, 0) < 0) {
        dbgln("PCM_PREPARE failed");
        return false;
    }
    m_state = Device::State::Prepared;
    return true;
}

bool Device::select_stream(const Stream& stream)
{
    if (m_state != Device::State::Open)
        return false;

    if (audio_ioctl(fd(), Audio::IOCtl::SELECT_STREAM, stream.index()) < 0) {
        dbgln("SELECT_STREAM failed");
        return false;
    }
    m_state = stream.is_setup() ? Device::State::Setup : Device::State::Selected;
    m_selected_stream = stream.index();
    return true;
}

bool Device::select_stream(unsigned stream_index)
{
    auto* stream = find_stream(stream_index);
    if (!stream)
        return false;
    return select_stream(*stream);
}

}
