/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2021, David Isaksson <davidisaksson93@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibAudio/ConnectionToManagerServer.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>

static constexpr bool audio_applet_show_percent_default = false;

class AudioWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(AudioWidget)

private:
    struct VolumeBitmapPair {
        int volume_threshold { 0 };
        NonnullRefPtr<Gfx::Bitmap> bitmap;
    };

public:
    static ErrorOr<NonnullRefPtr<AudioWidget>> try_create()
    {
        Array<VolumeBitmapPair, 5> volume_level_bitmaps = {
            { { 66, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-high.png"sv)) },
                { 33, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-medium.png"sv)) },
                { 1, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-low.png"sv)) },
                { 0, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-zero.png"sv)) },
                { 0, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-muted.png"sv)) } }
        };
        auto audio_client = TRY(Audio::ConnectionToManagerServer::try_create());
        NonnullRefPtr<AudioWidget> audio_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) AudioWidget(move(audio_client), move(volume_level_bitmaps))));
        TRY(audio_widget->try_initialize_graphical_elements());
        return audio_widget;
    }

private:
    AudioWidget(NonnullRefPtr<Audio::ConnectionToManagerServer> audio_client, Array<VolumeBitmapPair, 5> volume_level_bitmaps)
        : m_audio_client(move(audio_client))
        , m_volume_level_bitmaps(move(volume_level_bitmaps))
    {
        m_audio_volume = static_cast<int>(m_audio_client->get_main_mix_volume() * 100);
        m_audio_muted = m_audio_client->is_main_mix_muted();

        m_audio_client->on_main_mix_muted_state_change = [this](bool muted) {
            if (m_audio_muted == muted)
                return;
            m_mute_box->set_checked(!m_audio_muted);
            m_slider->set_enabled(!muted);
            m_audio_muted = muted;
            update();
        };

        m_audio_client->on_main_mix_volume_change = [this](double volume) {
            m_audio_volume = static_cast<int>(round(volume * 100));
            m_slider->set_value(m_slider->max() - m_audio_volume, GUI::AllowCallback::No);
            if (!m_audio_muted)
                update();
        };
    }

    ErrorOr<void> try_initialize_graphical_elements()
    {
        m_slider_window = add<GUI::Window>(window());
        m_slider_window->set_window_type(GUI::WindowType::Popup);

        m_root_container = m_slider_window->set_main_widget<GUI::Frame>();
        m_root_container->set_fill_with_background_color(true);
        m_root_container->set_layout<GUI::VerticalBoxLayout>(4, 0);
        m_root_container->set_frame_style(Gfx::FrameStyle::Window);

        m_percent_box = m_root_container->add<GUI::CheckBox>("\xE2\x84\xB9"_string);
        m_percent_box->set_tooltip(show_percent() ? "Hide percent"_string : "Show percent"_string);
        m_percent_box->set_checked(show_percent());
        m_percent_box->on_checked = [&](bool show_percent) {
            set_show_percent(show_percent);
            GUI::Application::the()->hide_tooltip();

            Config::write_bool("AudioApplet"sv, "Applet"sv, "ShowPercent"sv, show_percent);
        };

        m_slider = m_root_container->add<GUI::VerticalSlider>();
        m_slider->set_max(100);
        m_slider->set_page_step(5);
        m_slider->set_step(5);
        m_slider->set_value(m_slider->max() - m_audio_volume);
        m_slider->set_knob_size_mode(GUI::Slider::KnobSizeMode::Proportional);
        m_slider->on_change = [&](int value) {
            m_audio_volume = m_slider->max() - value;
            double volume = clamp(static_cast<double>(m_audio_volume) / m_slider->max(), 0.0, 1.0);
            m_audio_client->set_main_mix_volume(volume);
            update();
        };

        m_mute_box = m_root_container->add<GUI::CheckBox>("\xE2\x9D\x8C"_string);
        m_mute_box->set_checked(m_audio_muted);
        m_mute_box->set_tooltip(m_audio_muted ? "Unmute"_string : "Mute"_string);
        m_mute_box->on_checked = [&](bool is_muted) {
            m_mute_box->set_tooltip(is_muted ? "Unmute"_string : "Mute"_string);
            m_audio_client->set_main_mix_muted(is_muted);
            GUI::Application::the()->hide_tooltip();
        };

        return {};
    }

public:
    virtual ~AudioWidget() override = default;

    bool show_percent() const { return m_show_percent; }
    void set_show_percent(bool show_percent)
    {
        m_show_percent = show_percent;
        m_percent_box->set_checked(show_percent);
        m_percent_box->set_tooltip(show_percent ? "Hide percent"_string : "Show percent"_string);
        if (show_percent)
            window()->resize(44, 16);
        else
            window()->resize(16, 16);
    }

private:
    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Primary) {
            if (!m_slider_window->is_visible())
                open();
            else
                close();
            return;
        }
        if (event.button() == GUI::MouseButton::Secondary) {
            m_audio_client->set_main_mix_muted(!m_audio_muted);
            update();
        }
    }

    virtual void mousewheel_event(GUI::MouseEvent& event) override
    {
        if (m_audio_muted)
            return;
        m_slider->dispatch_event(event);
        update();
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.clear_rect(event.rect(), Color::from_argb(0));

        auto& audio_bitmap = choose_bitmap_from_volume();
        painter.blit({}, audio_bitmap, audio_bitmap.rect());

        if (show_percent()) {
            auto volume_text = m_audio_muted ? "mute" : ByteString::formatted("{}%", m_audio_volume);
            painter.draw_text(Gfx::IntRect { 16, 3, 24, 16 }, volume_text, Gfx::FontDatabase::default_fixed_width_font(), Gfx::TextAlignment::TopLeft, palette().window_text());
        }
    }

    virtual void applet_area_rect_change_event(GUI::AppletAreaRectChangeEvent&) override
    {
        reposition_slider_window();
    }

    void open()
    {
        reposition_slider_window();
        m_slider_window->show();
    }

    void close()
    {
        m_slider_window->hide();
    }

    Gfx::Bitmap& choose_bitmap_from_volume()
    {
        if (m_audio_muted)
            return *m_volume_level_bitmaps.last().bitmap;

        for (auto& pair : m_volume_level_bitmaps) {
            if (m_audio_volume >= pair.volume_threshold)
                return *pair.bitmap;
        }
        VERIFY_NOT_REACHED();
    }

    void reposition_slider_window()
    {
        constexpr auto width { 50 };
        constexpr auto height { 125 };
        constexpr auto tray_and_taskbar_padding { 6 };
        constexpr auto icon_offset { (width - 16) / 2 };
        auto applet_rect = window()->applet_rect_on_screen();
        m_slider_window->set_rect(
            applet_rect.x() - icon_offset,
            applet_rect.y() - height - tray_and_taskbar_padding,
            width,
            height);
    }

    NonnullRefPtr<Audio::ConnectionToManagerServer> m_audio_client;
    Array<VolumeBitmapPair, 5> m_volume_level_bitmaps;
    bool m_show_percent { false };
    bool m_audio_muted { false };
    int m_audio_volume { 100 };

    RefPtr<GUI::Slider> m_slider;
    RefPtr<GUI::Window> m_slider_window;
    RefPtr<GUI::CheckBox> m_mute_box;
    RefPtr<GUI::CheckBox> m_percent_box;
    RefPtr<GUI::Frame> m_root_container;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath unix thread"));

    auto app = TRY(GUI::Application::create(arguments));
    Config::pledge_domain("AudioApplet");
    TRY(Core::System::unveil("/tmp/session/%sid/portal/audiomanager", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = GUI::Window::construct();
    window->set_has_alpha_channel(true);
    window->set_title("Audio");
    window->set_window_type(GUI::WindowType::Applet);

    auto audio_widget = TRY(AudioWidget::try_create());
    window->set_main_widget(audio_widget);
    window->show();

    // This affects the positioning, which depends on the window actually existing.
    bool should_show_percent = Config::read_bool("AudioApplet"sv, "Applet"sv, "ShowPercent"sv, audio_applet_show_percent_default);
    audio_widget->set_show_percent(should_show_percent);

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));

    return app->exec();
}
