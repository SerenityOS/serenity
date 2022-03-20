/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/TypedTransfer.h>
#include <LibAudio/Buffer.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>

class VisualizationWidget : public GUI::Frame {
    C_OBJECT(VisualizationWidget)

public:
    virtual void render(GUI::PaintEvent&, FixedArray<double> const& samples) = 0;

    void set_buffer(RefPtr<Audio::Buffer> buffer)
    {
        if (buffer.is_null())
            return;
        if (buffer->id() == m_last_buffer_id)
            return;
        m_last_buffer_id = buffer->id();

        if (m_sample_buffer.size() != static_cast<size_t>(buffer->sample_count()))
            m_sample_buffer.resize(buffer->sample_count());

        for (size_t i = 0; i < static_cast<size_t>(buffer->sample_count()); i++)
            m_sample_buffer.data()[i] = (buffer->samples()[i].left + buffer->samples()[i].right) / 2.;

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

        AK::TypedTransfer<double>::copy(m_render_buffer.data(), m_sample_buffer.span().slice(buffer_position).data(), m_render_buffer.size());

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
        auto new_buffer = TRY(FixedArray<double>::try_create(count));
        m_render_buffer.swap(new_buffer);
        return {};
    }
    virtual void start_new_file(StringView) { }

protected:
    int m_samplerate;
    int m_last_buffer_id;
    size_t m_frame_count;
    Vector<double> m_sample_buffer;
    FixedArray<double> m_render_buffer;

    static constexpr size_t REFRESH_TIME_MILLISECONDS = 30;

    VisualizationWidget()
        : m_samplerate(-1)
        , m_last_buffer_id(-1)
        , m_frame_count(0)
    {
        start_timer(REFRESH_TIME_MILLISECONDS);
    }

    virtual ~VisualizationWidget() = default;
};
