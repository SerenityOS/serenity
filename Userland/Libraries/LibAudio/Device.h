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
#include <LibCore/IODevice.h>

class JsonObject;

namespace Audio {

class Device : public Core::IODevice {
    C_OBJECT(Device)
public:
    enum class State {
        Closed = 0,
        Open,
        Selected,
        Setup,
        Prepared,
        Running,
        XRun
    };

    class Stream {
        friend class Device;
    public:
        unsigned index() const { return m_index; }
        const String& name() const { return m_name; }
        StreamType type() const { return m_type; }
        const Vector<PCM::SampleFormat>& supported_formats() const { return m_supported_formats; }
        const Vector<PCM::SampleLayout>& supported_layouts() const { return m_supported_layouts; }
        const Vector<unsigned>& supported_rates() const { return m_supported_rates; }
        const Vector<unsigned>& supported_channels() const { return m_supported_channels; }

        bool is_setup() const
        {
            return !m_current_params.is_null();
        }

        PCM::SampleFormat format() const { return m_current_params.format; }
        bool set_format(PCM::SampleFormat);
        PCM::SampleLayout layout() const { return m_current_params.layout; }
        bool set_layout(PCM::SampleLayout);
        unsigned rate() const { return m_current_params.rate; }
        bool set_rate(unsigned);
        unsigned channels() const { return m_current_params.channels; }
        bool set_channels(unsigned);

    private:
        Stream(Device& device)
            : m_device(device)
        {
        }
        void parse(unsigned, const JsonObject&);

        Device& m_device;
        unsigned m_index { 0 };
        String m_name;
        StreamType m_type { StreamType::Unknown };
        Vector<PCM::SampleFormat> m_supported_formats;
        Vector<PCM::SampleLayout> m_supported_layouts;
        Vector<unsigned> m_supported_rates;
        Vector<unsigned> m_supported_channels;

        Audio::IOCtlSetPCMHwParams m_current_params;
    };

    virtual bool open(IODevice::OpenMode) override;
    virtual bool close() override;

    bool select_stream(const Stream&);
    bool select_stream(unsigned);
    bool get_pcm_hw_params();
    bool set_pcm_hw_params();
    bool pcm_prepare();

    template<typename F>
    IterationDecision for_each_stream(F f) const
    {
        for (const auto& stream : m_streams) {
            IterationDecision decision = f(stream);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

private:
    Device(Object* parent = nullptr)
        : IODevice(parent)
    {
    }
    explicit Device(const StringView&, Object* parent = nullptr);

    bool open_impl(IODevice::OpenMode, mode_t);
    int json_ioctl(Audio::IOCtl, const StringView&, String*);
    Stream* find_stream(unsigned);

    String m_filename;
    State m_state { State::Closed };
    unsigned m_selected_stream { 0 };
    Vector<Stream, 1> m_streams;
};

}
