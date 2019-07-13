#pragma once

#include "Music.h"
#include <LibGUI/GWidget.h>

class GPainter;

class PianoWidget final : public GWidget {
public:
    PianoWidget();
    virtual ~PianoWidget() override;

    virtual void paint_event(GPaintEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void keyup_event(GKeyEvent&) override;

    void fill_audio_buffer(uint8_t* stream, int len);

private:
    double w_sine(size_t);
    double w_saw(size_t);
    double w_square(size_t);

    void render_piano_key(GPainter&, int index, PianoKey, const StringView&);
    void render_piano(GPainter&);
    void render_knobs(GPainter&);
    void render_knob(GPainter&, const Rect&, bool state, const StringView&);

    void note(Music::PianoKey offset_n, bool is_down);
    void update_keys();
    int octave_base() const;

    RefPtr<GraphicsBitmap> m_bitmap;

#define note_count sizeof(note_frequency) / sizeof(double)

    bool m_note_on[note_count];
    double m_power[note_count];
    double m_sin_pos[note_count];
    double m_square_pos[note_count];
    double m_saw_pos[note_count];

    int m_octave_min { 1 };
    int m_octave_max { 6 };
    int m_octave { 4 };

    int m_width { 512 };
    int m_height { 512 };

    int m_wave_type { 0 };
    bool m_delay_enabled { false };
    bool m_release_enabled { false };

    bool keys[256];
};
