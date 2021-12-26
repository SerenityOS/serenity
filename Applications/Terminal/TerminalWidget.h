#pragma once

#include <AK/String.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CTimer.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/Rect.h>
#include <LibGUI/GFrame.h>
#include <LibVT/Terminal.h>

class GScrollBar;

class TerminalWidget final : public GFrame
    , public VT::TerminalClient {
    C_OBJECT(TerminalWidget)
public:
    explicit TerminalWidget(int ptm_fd, RefPtr<CConfigFile> config);
    virtual ~TerminalWidget() override;

    void create_window();

    void flush_dirty_lines();
    void force_repaint();

    void apply_size_increments_to_window(GWindow&);

    void set_opacity(u8);
    float opacity() { return m_opacity; };
    bool should_beep() { return m_should_beep; }
    void set_should_beep(bool sb) { m_should_beep = sb; };

    RefPtr<CConfigFile> config() const { return m_config; }

    bool has_selection() const;
    bool selection_contains(const VT::Position&) const;
    String selected_text() const;
    VT::Position buffer_position_at(const Point&) const;
    VT::Position normalized_selection_start() const;
    VT::Position normalized_selection_end() const;

    bool is_scrollable() const;

private:
    // ^GWidget
    virtual void event(CEvent&) override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousewheel_event(GMouseEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;

    // ^TerminalClient
    virtual void beep() override;
    virtual void set_window_title(const StringView&) override;
    virtual void terminal_did_resize(u16 columns, u16 rows) override;
    virtual void terminal_history_changed() override;

    Rect glyph_rect(u16 row, u16 column);
    Rect row_rect(u16 row);

    void update_cursor();
    void invalidate_cursor();

    Size compute_base_size() const;
    int first_selection_column_on_row(int row) const;
    int last_selection_column_on_row(int row) const;

    VT::Terminal m_terminal;

    VT::Position m_selection_start;
    VT::Position m_selection_end;

    bool m_should_beep { false };
    bool m_belling { false };

    int m_pixel_width { 0 };
    int m_pixel_height { 0 };

    int m_inset { 2 };
    int m_line_spacing { 4 };
    int m_line_height { 0 };

    int m_ptm_fd { -1 };

    bool m_in_active_window { false };

    CNotifier m_notifier;

    u8 m_opacity { 255 };
    bool m_needs_background_fill { true };
    bool m_cursor_blink_state { true };

    int m_glyph_width { 0 };

    ObjectPtr<CTimer> m_cursor_blink_timer;
    ObjectPtr<CTimer> m_visual_beep_timer;
    RefPtr<CConfigFile> m_config;

    GScrollBar* m_scrollbar { nullptr };
};
