/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

REGISTER_WIDGET(GUI, ImageWidget)

namespace GUI {

ImageWidget::ImageWidget(StringView)
    : m_timer(Core::Timer::try_create().release_value_but_fixme_should_propagate_errors())

{
    set_frame_style(Gfx::FrameStyle::NoFrame);
    set_auto_resize(true);

    REGISTER_BOOL_PROPERTY("auto_resize", auto_resize, set_auto_resize);
    REGISTER_BOOL_PROPERTY("should_stretch", should_stretch, set_should_stretch);
    REGISTER_WRITE_ONLY_STRING_PROPERTY("bitmap", load_from_file);
}

void ImageWidget::set_bitmap(Gfx::Bitmap const* bitmap)
{
    if (m_bitmap == bitmap)
        return;

    m_bitmap = bitmap;
    if (m_bitmap && m_auto_resize)
        set_fixed_size(m_bitmap->size());

    update();
}

void ImageWidget::set_auto_resize(bool value)
{
    if (m_auto_resize == value)
        return;
    m_auto_resize = value;
    if (m_bitmap && m_auto_resize)
        set_fixed_size(m_bitmap->size());
}

// Same as ImageViewer::ViewWidget::animate(), you probably want to keep any changes in sync
void ImageWidget::animate()
{
    auto first_animated_frame_index = m_image_decoder->first_animated_frame_index();
    auto total_animated_frames = m_image_decoder->frame_count() - first_animated_frame_index;
    m_current_frame_index = (m_current_frame_index + 1) % total_animated_frames;

    auto current_frame = m_image_decoder->frame(first_animated_frame_index + m_current_frame_index).release_value_but_fixme_should_propagate_errors();
    set_bitmap(current_frame.image);

    if (current_frame.duration != m_timer->interval()) {
        m_timer->restart(current_frame.duration);
    }

    if (m_current_frame_index == total_animated_frames - 1) {
        ++m_loops_completed;
        if (m_image_decoder->loop_count() > 0 && m_loops_completed == m_image_decoder->loop_count()) {
            m_timer->stop();
        }
    }
}

void ImageWidget::load_from_file(StringView path)
{
    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error())
        return;

    auto& mapped_file = *file_or_error.value();
    auto mime_type = Core::guess_mime_type_based_on_filename(path);
    m_image_decoder = MUST(Gfx::ImageDecoder::try_create_for_raw_bytes(mapped_file.bytes(), mime_type));
    VERIFY(m_image_decoder);

    auto frame = m_image_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
    auto bitmap = frame.image;
    VERIFY(bitmap);

    set_bitmap(bitmap);

    if (m_image_decoder->is_animated() && m_image_decoder->frame_count() > 1) {
        auto first_frame = m_image_decoder->frame(0).release_value_but_fixme_should_propagate_errors();
        m_timer->set_interval(first_frame.duration);
        m_timer->on_timeout = [this] { animate(); };
        m_timer->start();
    }
}

void ImageWidget::mousedown_event(GUI::MouseEvent&)
{
    if (on_click)
        on_click();
}

void ImageWidget::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    if (!m_bitmap) {
        return;
    }

    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_should_stretch) {
        painter.draw_scaled_bitmap(frame_inner_rect(), *m_bitmap, m_bitmap->rect(), (float)opacity_percent() / 100.0f);
    } else {
        auto location = frame_inner_rect().center().translated(-(m_bitmap->width() / 2), -(m_bitmap->height() / 2));
        painter.blit(location, *m_bitmap, m_bitmap->rect(), (float)opacity_percent() / 100.0f);
    }
}

void ImageWidget::set_opacity_percent(int percent)
{
    if (m_opacity_percent == percent)
        return;
    m_opacity_percent = percent;
    update();
}

}
