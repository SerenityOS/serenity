#include "SoundPlayerWidget.h"
#include <AK/StringBuilder.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibM/math.h>

SoundPlayerWidget::SoundPlayerWidget(NonnullRefPtr<AClientConnection> connection, AWavLoader& loader)
    : m_manager(PlaybackManager(connection, loader))
{
    set_fill_with_background_color(true);
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 2, 2, 2, 2 });

    m_sample_ratio = PLAYBACK_MANAGER_RATE / static_cast<float>(loader.sample_rate());

    auto status_widget = GWidget::construct(this);
    status_widget->set_fill_with_background_color(true);
    status_widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));

    m_elapsed = GLabel::construct(status_widget);
    m_elapsed->set_frame_shape(FrameShape::Container);
    m_elapsed->set_frame_shadow(FrameShadow::Sunken);
    m_elapsed->set_frame_thickness(2);
    m_elapsed->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    m_elapsed->set_preferred_size(80, 0);

    m_sample_widget = SampleWidget::construct(status_widget);

    m_remaining = GLabel::construct(status_widget);
    m_remaining->set_frame_shape(FrameShape::Container);
    m_remaining->set_frame_shadow(FrameShadow::Sunken);
    m_remaining->set_frame_thickness(2);
    m_remaining->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    m_remaining->set_preferred_size(80, 0);

    m_slider = Slider::construct(Orientation::Horizontal, this);
    m_slider->set_min(0);
    m_slider->set_max(normalize_rate(static_cast<int>(loader.total_samples())));
    m_slider->on_knob_released = [&](int value) { m_manager.seek(denormalize_rate(value)); };

    auto control_widget = GWidget::construct(this);
    control_widget->set_fill_with_background_color(true);
    control_widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    control_widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    control_widget->set_preferred_size(0, 30);
    control_widget->layout()->set_margins({ 10, 2, 10, 2 });
    control_widget->layout()->set_spacing(10);

    m_play = GButton::construct(control_widget);
    m_play->set_icon(*m_pause_icon);
    m_play->on_click = [this](GButton& button) {
        button.set_icon(m_manager.toggle_pause() ? *m_play_icon : *m_pause_icon);
    };

    auto stop = GButton::construct(control_widget);
    stop->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/stop.png"));
    stop->on_click = [&](GButton&) { m_manager.stop(); };

    m_status = GLabel::construct(this);
    m_status->set_frame_shape(FrameShape::Box);
    m_status->set_frame_shadow(FrameShadow::Raised);
    m_status->set_frame_thickness(4);
    m_status->set_text_alignment(TextAlignment::CenterLeft);
    m_status->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_status->set_preferred_size(0, 18);

    m_status->set_text(String::format(
        "Sample rate %uHz, %u channels, %u bits per sample",
        loader.sample_rate(),
        loader.num_channels(),
        loader.bits_per_sample()));

    update_position(0);

    m_manager.on_update = [&]() { update_ui(); };
    m_manager.play();
}

SoundPlayerWidget::~SoundPlayerWidget()
{
}

SoundPlayerWidget::Slider::~Slider()
{
}

int SoundPlayerWidget::normalize_rate(int rate) const
{
    return static_cast<int>(rate * m_sample_ratio);
}

int SoundPlayerWidget::denormalize_rate(int rate) const
{
    return static_cast<int>(rate / m_sample_ratio);
}

void SoundPlayerWidget::update_ui()
{
    m_sample_widget->set_buffer(m_manager.current_buffer());
    m_play->set_icon(m_manager.is_paused() ? *m_play_icon : *m_pause_icon);
    update_position(m_manager.connection()->get_played_samples());
}

void SoundPlayerWidget::update_position(const int position)
{
    int total_norm_samples = position + normalize_rate(m_manager.last_seek());
    float seconds = (total_norm_samples / static_cast<float>(PLAYBACK_MANAGER_RATE));
    float remaining_seconds = m_manager.total_length() - seconds;

    m_elapsed->set_text(String::format(
        "Position:\n%u:%02u.%02u",
        static_cast<int>(seconds / 60),
        static_cast<int>(seconds) % 60,
        static_cast<int>(seconds * 100) % 100));

    m_remaining->set_text(String::format(
        "Remaining:\n%u:%02u.%02u",
        static_cast<int>(remaining_seconds / 60),
        static_cast<int>(remaining_seconds) % 60,
        static_cast<int>(remaining_seconds * 100) % 100));

    m_slider->set_value(total_norm_samples);
}
