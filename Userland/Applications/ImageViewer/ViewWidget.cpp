/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Mohsan Ali <mohsan0073@gmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ViewWidget.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/Directory.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibCore/Timer.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Palette.h>
#include <LibImageDecoderClient/Client.h>

namespace ImageViewer {

void VectorImage::flip(Gfx::Orientation orientation)
{
    if (orientation == Gfx::Orientation::Horizontal)
        apply_transform(Gfx::AffineTransform {}.scale(-1, 1));
    else
        apply_transform(Gfx::AffineTransform {}.scale(1, -1));
}

void VectorImage::rotate(Gfx::RotationDirection rotation_direction)
{
    if (rotation_direction == Gfx::RotationDirection::Clockwise)
        apply_transform(Gfx::AffineTransform {}.rotate_radians(AK::Pi<float> / 2));
    else
        apply_transform(Gfx::AffineTransform {}.rotate_radians(-AK::Pi<float> / 2));
    m_size = { m_size.height(), m_size.width() };
}

void VectorImage::draw_into(Gfx::Painter& painter, Gfx::IntRect const& dest, Gfx::ScalingMode) const
{
    m_vector->draw_into(painter, dest, m_transform);
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> VectorImage::bitmap(Optional<Gfx::IntSize> ideal_size) const
{
    return m_vector->bitmap(ideal_size.value_or(size()), m_transform);
}

void BitmapImage::flip(Gfx::Orientation orientation)
{
    m_bitmap = m_bitmap->flipped(orientation).release_value_but_fixme_should_propagate_errors();
}

void BitmapImage::rotate(Gfx::RotationDirection rotation)
{
    m_bitmap = m_bitmap->rotated(rotation).release_value_but_fixme_should_propagate_errors();
}

void BitmapImage::draw_into(Gfx::Painter& painter, Gfx::IntRect const& dest, Gfx::ScalingMode scaling_mode) const
{
    painter.draw_scaled_bitmap(dest, *m_bitmap, m_bitmap->rect(), 1.0f, scaling_mode);
}

ViewWidget::ViewWidget()
    : m_timer(Core::Timer::try_create().release_value_but_fixme_should_propagate_errors())
{
    set_fill_with_background_color(false);
}

void ViewWidget::clear()
{
    m_timer->stop();
    m_animation.clear();
    m_image = nullptr;
    if (on_image_change)
        on_image_change(m_image);
    set_original_rect({});
    m_path = {};

    reset_view();
    update();
}

void ViewWidget::flip(Gfx::Orientation orientation)
{
    m_image->flip(orientation);
    scale_image_for_window();
}

void ViewWidget::rotate(Gfx::RotationDirection rotation_direction)
{
    m_image->rotate(rotation_direction);
    scale_image_for_window();
}

bool ViewWidget::is_next_available() const
{
    if (m_current_index.has_value())
        return m_current_index.value() + 1 < m_files_in_same_dir.size();
    return false;
}

bool ViewWidget::is_previous_available() const
{
    if (m_current_index.has_value())
        return m_current_index.value() > 0;
    return false;
}

// FIXME: Convert to `String` & use LibFileSystemAccessClient + `Core::System::unveil(nullptr, nullptr)`
//        - Converting to String is not super-trivial due to the LexicalPath usage, while we can do a bunch of
//          String::from_byte_string() and String.to_byte_string(), it is quite ugly to read and
//          probably not the best approach.
//
//        - If we go full-unveil (`Core::System::unveil(nullptr, nullptr)`) this functionality does not work,
//          we can not access the list of contents of a directory through LibFileSystemAccessClient at the moment.
Vector<ByteString> ViewWidget::load_files_from_directory(ByteString const& path) const
{
    Vector<ByteString> files_in_directory;

    auto current_dir = LexicalPath(path).parent().string();
    // FIXME: Propagate errors
    (void)Core::Directory::for_each_entry(current_dir, Core::DirIterator::Flags::SkipDots, [&](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
        auto full_path = LexicalPath::join(directory.path().string(), entry.name).string();
        if (Gfx::Bitmap::is_path_a_supported_image_format(full_path))
            files_in_directory.append(full_path);
        return IterationDecision::Continue;
    });
    return files_in_directory;
}

void ViewWidget::set_path(String const& path)
{
    m_path = path;
    m_files_in_same_dir = load_files_from_directory(path.to_byte_string());
    m_current_index = m_files_in_same_dir.find_first_index(path.to_byte_string());
}

void ViewWidget::navigate(Directions direction)
{
    if (!m_current_index.has_value()) {
        return;
    }

    auto index = m_current_index.value();
    if (direction == Directions::Back) {
        index--;
    } else if (direction == Directions::Forward) {
        index++;
    } else if (direction == Directions::First) {
        index = 0;
    } else if (direction == Directions::Last) {
        index = m_files_in_same_dir.size() - 1;
    }

    auto result = FileSystemAccessClient::Client::the().request_file_read_only_approved(window(), m_files_in_same_dir.at(index));
    if (result.is_error())
        return;

    m_current_index = index;

    auto value = result.release_value();
    open_file(MUST(String::from_byte_string(value.filename())), value.stream());
}

void ViewWidget::doubleclick_event(GUI::MouseEvent&)
{
    on_doubleclick();
}

void ViewWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    Gfx::StylePainter::paint_transparency_grid(painter, frame_inner_rect(), palette());

    if (m_image)
        return m_image->draw_into(painter, content_rect(), m_scaling_mode);
}

void ViewWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary)
        start_panning(event.position());
    GUI::AbstractZoomPanWidget::mousedown_event(event);
}

void ViewWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary)
        stop_panning();
    GUI::AbstractZoomPanWidget::mouseup_event(event);
}

void ViewWidget::open_file(String const& path, Core::File& file)
{
    auto open_result = try_open_file(path, file);
    if (open_result.is_error()) {
        auto error = open_result.release_error();
        auto user_error_message = String::formatted("Failed to open the image: {}.", error).release_value_but_fixme_should_propagate_errors();

        GUI::MessageBox::show_error(nullptr, user_error_message);
    }
}

ErrorOr<void> ViewWidget::try_open_file(String const& path, Core::File& file)
{
    auto file_data = TRY(file.read_until_eof());
    bool is_animated = false;
    size_t loop_count = 0;
    Vector<Animation::Frame> frames;
    // Note: Doing this check only requires reading the header of images
    // (so if the image is not vector graphics it can be still be decoded OOP).
    if (auto decoder = TRY(Gfx::ImageDecoder::try_create_for_raw_bytes(file_data)); decoder && decoder->natural_frame_format() == Gfx::NaturalFrameFormat::Vector) {
        // Use in-process decoding for vector graphics.
        is_animated = decoder->is_animated();
        loop_count = decoder->loop_count();
        frames.ensure_capacity(decoder->frame_count());
        for (u32 i = 0; i < decoder->frame_count(); i++) {
            auto frame_data = TRY(decoder->vector_frame(i));
            frames.unchecked_append({ VectorImage::create(*frame_data.image), frame_data.duration });
        }
    } else {
        // Use out-of-process decoding for raster formats.
        auto client = TRY(ImageDecoderClient::Client::try_create());
        auto mime_type = Core::guess_mime_type_based_on_filename(path);

        // FIXME: Refactor file opening to be more async-aware, and don't await this promise
        auto decoded_image = TRY(client->decode_image(file_data, {}, {}, OptionalNone {}, mime_type)->await());
        is_animated = decoded_image.is_animated;
        loop_count = decoded_image.loop_count;
        frames.ensure_capacity(decoded_image.frames.size());
        for (u32 i = 0; i < decoded_image.frames.size(); i++) {
            auto& frame_data = decoded_image.frames[i];
            frames.unchecked_append({ BitmapImage::create(frame_data.bitmap, decoded_image.scale), int(frame_data.duration) });
        }
    }

    clear();

    m_image = frames[0].image;
    if (is_animated && frames.size() > 1) {
        m_animation = Animation { loop_count, move(frames) };
    }

    set_original_rect(m_image->rect());

    if (m_animation.has_value()) {
        auto const& first_frame = m_animation->frames[0];
        m_timer->set_interval(first_frame.duration);
        m_timer->on_timeout = [this] { animate(); };
        m_timer->start();
    } else {
        m_timer->stop();
    }

    set_path(path);
    GUI::Application::the()->set_most_recently_open_file(path.to_byte_string());

    if (on_image_change)
        on_image_change(m_image);

    if (scaled_for_first_image())
        scale_image_for_window();
    else
        reset_view();

    return {};
}

void ViewWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void ViewWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    if (on_drop)
        on_drop(event);
}

void ViewWidget::resize_event(GUI::ResizeEvent& event)
{
    event.accept();
    scale_image_for_window();
}

void ViewWidget::scale_image_for_window()
{
    if (!m_image)
        return;

    set_original_rect(m_image->rect());
    fit_content_to_view(GUI::AbstractZoomPanWidget::FitType::Both);
}

void ViewWidget::resize_window()
{
    if (window()->is_fullscreen() || window()->is_maximized())
        return;

    auto absolute_bitmap_rect = content_rect();
    absolute_bitmap_rect.translate_by(window()->rect().top_left());

    if (!m_image)
        return;

    auto new_size = content_rect().size();

    if (new_size.width() < 300)
        new_size.set_width(300);
    if (new_size.height() < 200)
        new_size.set_height(200);

    if (new_size.width() > 500)
        new_size = { 500, 500 * absolute_bitmap_rect.height() / absolute_bitmap_rect.width() };
    if (new_size.height() > 500)
        new_size = { 500 * absolute_bitmap_rect.width() / absolute_bitmap_rect.height(), 500 };

    new_size.set_height(new_size.height() + m_toolbar_height);
    window()->resize(new_size);
    scale_image_for_window();
}

void ViewWidget::set_image(Image const* image)
{
    if (m_image == image)
        return;
    m_image = image;
    set_original_rect(m_image->rect());
    update();
}

// Same as ImageWidget::animate(), you probably want to keep any changes in sync
void ViewWidget::animate()
{
    if (!m_animation.has_value())
        return;

    m_current_frame_index = (m_current_frame_index + 1) % m_animation->frames.size();

    auto const& current_frame = m_animation->frames[m_current_frame_index];
    set_image(current_frame.image);

    if ((int)current_frame.duration != m_timer->interval()) {
        m_timer->restart(current_frame.duration);
    }

    if (m_current_frame_index == m_animation->frames.size() - 1) {
        ++m_loops_completed;
        if (m_loops_completed > 0 && m_loops_completed == m_animation->loop_count) {
            m_timer->stop();
        }
    }
}

void ViewWidget::set_scaling_mode(Gfx::ScalingMode scaling_mode)
{
    m_scaling_mode = scaling_mode;
    update();
}

}
