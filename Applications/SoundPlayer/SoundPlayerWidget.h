#pragma once

#include "PlaybackManager.h"
#include "SampleWidget.h"
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GSlider.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class SoundPlayerWidget final : public GWidget {
    C_OBJECT(SoundPlayerWidget)
public:
    virtual ~SoundPlayerWidget() override;
    void open_file(String path);
    void hide_scope(bool);
    PlaybackManager& manager() { return m_manager; }

private:
    explicit SoundPlayerWidget(GWindow&, NonnullRefPtr<AClientConnection>);

    void update_position(const int position);
    void update_ui();
    int normalize_rate(int) const;
    int denormalize_rate(int) const;

    class Slider final : public GSlider {
        C_OBJECT(Slider)
    public:
        virtual ~Slider() override;
        Function<void(int)> on_knob_released;
        void set_value(int value)
        {
            if (!knob_dragging())
                GSlider::set_value(value);
        }

    protected:
        Slider(Orientation orientation, GWidget* parent)
            : GSlider(orientation, parent)
        {
        }

        virtual void mouseup_event(GMouseEvent& event) override
        {
            if (on_knob_released && is_enabled())
                on_knob_released(value());

            GSlider::mouseup_event(event);
        }
    };

    GWindow& m_window;
    NonnullRefPtr<AClientConnection> m_connection;
    PlaybackManager m_manager;
    float m_sample_ratio;
    RefPtr<GLabel> m_status;
    RefPtr<GLabel> m_elapsed;
    RefPtr<GLabel> m_remaining;
    RefPtr<Slider> m_slider;
    RefPtr<SampleWidget> m_sample_widget;
    RefPtr<GraphicsBitmap> m_play_icon { GraphicsBitmap::load_from_file("/res/icons/16x16/play.png") };
    RefPtr<GraphicsBitmap> m_pause_icon { GraphicsBitmap::load_from_file("/res/icons/16x16/pause.png") };
    RefPtr<GButton> m_play;
    RefPtr<GButton> m_stop;
};
