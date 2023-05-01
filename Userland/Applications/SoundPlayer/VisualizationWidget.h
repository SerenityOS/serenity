/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/Forward.h>
#include <AK/TypedTransfer.h>
#include <LibAudio/Sample.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>

class VisualizationWidget : public GUI::Frame {
    C_OBJECT_ABSTRACT(VisualizationWidget)

public:
    virtual void render(GUI::PaintEvent&, FixedArray<float> const& samples) = 0;

    void set_buffer(FixedArray<Audio::Sample> const& buffer)
    {
        if (buffer.is_empty())
            return;

        if (m_sample_buffer.size() != buffer.size())
            m_sample_buffer.resize(buffer.size());

        for (size_t i = 0; i < buffer.size(); i++)
            m_sample_buffer.data()[i] = (buffer[i].left + buffer[i].right) / 2.f;

        m_frame_count = 0;
    }

    void reset_buffer()
    {
        m_sample_buffer.clear();
        m_render_buffer.fill_with(.0f);
        m_frame_count = 0;
    }

    virtual void set_samplerate(int samplerate)
    {
        m_samplerate = samplerate;
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        if (m_sample_buffer.size() == 0) {
            Frame::paint_event(event);
            GUI::Painter painter(*this);
            painter.add_clip_rect(event.rect());
            painter.fill_rect(frame_inner_rect(), Color::Black);
            return;
        }

        if (m_render_buffer.size() == 0)
            return;

        size_t buffer_position = (m_frame_count * REFRESH_TIME_MILLISECONDS) * m_samplerate / 1000;
        if (buffer_position + m_render_buffer.size() >= m_sample_buffer.size())
            buffer_position = m_sample_buffer.size() - m_render_buffer.size();

        AK::TypedTransfer<float>::copy(m_render_buffer.data(), m_sample_buffer.span().slice(buffer_position).data(), m_render_buffer.size());

        render(event, m_render_buffer);
    }

    virtual void timer_event(Core::TimerEvent&) override
    {
        update();
        m_frame_count++;
    }

    size_t frame_count() const { return m_frame_count; }

    ErrorOr<void> set_render_sample_count(size_t count)
    {
        auto new_buffer = TRY(FixedArray<float>::create(count));
        m_render_buffer.swap(new_buffer);
        return {};
    }
    virtual void start_new_file(StringView) { }

protected:
    int m_samplerate;
    size_t m_frame_count;
    Vector<float> m_sample_buffer;
    FixedArray<float> m_render_buffer;

    static constexpr size_t REFRESH_TIME_MILLISECONDS = 30;

    VisualizationWidget()
        : m_samplerate(-1)
        , m_frame_count(0)
    {
        start_timer(REFRESH_TIME_MILLISECONDS);
    }

    virtual ~VisualizationWidget() = default;
};
