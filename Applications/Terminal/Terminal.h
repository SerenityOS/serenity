#pragma once

#include <AK/AKString.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Types.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CTimer.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/Rect.h>
#include <LibGUI/GFrame.h>

class Font;

class BufferPosition {
public:
    BufferPosition() {}
    BufferPosition(int row, int column)
        : m_row(row)
        , m_column(column)
    {
    }

    bool is_valid() const { return m_row >= 0 && m_column >= 0; }
    int row() const { return m_row; }
    int column() const { return m_column; }

    bool operator<(const BufferPosition& other) const
    {
        return m_row < other.m_row || (m_row == other.m_row && m_column < other.m_column);
    }

    bool operator<=(const BufferPosition& other) const
    {
        return *this < other || *this == other;
    }

    bool operator>=(const BufferPosition& other) const
    {
        return !(*this < other);
    }

    bool operator==(const BufferPosition& other) const
    {
        return m_row == other.m_row && m_column == other.m_column;
    }

    bool operator!=(const BufferPosition& other) const
    {
        return !(*this == other);
    }

private:
    int m_row { -1 };
    int m_column { -1 };
};

class Terminal final : public GFrame {
    C_OBJECT(Terminal)
public:
    explicit Terminal(int ptm_fd, RefPtr<CConfigFile> config);
    virtual ~Terminal() override;

    void create_window();
    void on_char(u8);

    void flush_dirty_lines();
    void force_repaint();

    void apply_size_increments_to_window(GWindow&);

    void set_opacity(u8);
    float opacity() { return m_opacity; };
    bool should_beep() { return m_should_beep; }
    void set_should_beep(bool sb) { m_should_beep = sb; };

    RefPtr<CConfigFile> config() const { return m_config; }

    bool has_selection() const;
    bool selection_contains(const BufferPosition&) const;
    String selected_text() const;
    BufferPosition buffer_position_at(const Point&) const;
    BufferPosition normalized_selection_start() const;
    BufferPosition normalized_selection_end() const;

private:
    typedef Vector<unsigned, 4> ParamVector;

    virtual void event(CEvent&) override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;

    void scroll_up();
    void scroll_down();
    void newline();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, u8 ch);
    void invalidate_cursor();
    void set_window_title(const String&);

    void inject_string(const String&);
    void unimplemented_escape();
    void unimplemented_xterm_escape();

    void escape$A(const ParamVector&);
    void escape$B(const ParamVector&);
    void escape$C(const ParamVector&);
    void escape$D(const ParamVector&);
    void escape$H(const ParamVector&);
    void escape$J(const ParamVector&);
    void escape$K(const ParamVector&);
    void escape$M(const ParamVector&);
    void escape$P(const ParamVector&);
    void escape$G(const ParamVector&);
    void escape$X(const ParamVector&);
    void escape$b(const ParamVector&);
    void escape$d(const ParamVector&);
    void escape$m(const ParamVector&);
    void escape$s(const ParamVector&);
    void escape$u(const ParamVector&);
    void escape$t(const ParamVector&);
    void escape$r(const ParamVector&);
    void escape$S(const ParamVector&);
    void escape$T(const ParamVector&);
    void escape$L(const ParamVector&);
    void escape$h_l(bool, bool, const ParamVector&);

    void clear();

    void set_size(u16 columns, u16 rows);
    u16 columns() const { return m_columns; }
    u16 rows() const { return m_rows; }
    Rect glyph_rect(u16 row, u16 column);
    Rect row_rect(u16 row);
    void update_cursor();

    struct Attribute {
        Attribute() { reset(); }

        static u8 default_foreground_color;
        static u8 default_background_color;

        void reset()
        {
            foreground_color = default_foreground_color;
            background_color = default_background_color;
            flags = Flags::NoAttributes;
        }
        u8 foreground_color;
        u8 background_color;

        enum Flags {
            NoAttributes = 0x00,
            Bold = 0x01,
            Italic = 0x02,
            Underline = 0x04,
            Negative = 0x08,
            Blink = 0x10,
            Touched = 0x20,
        };

        bool is_untouched() const { return !(flags & Touched); }

        // TODO: it would be really nice if we had a helper for enums that
        // exposed bit ops for class enums...
        int flags = Flags::NoAttributes;

        bool operator==(const Attribute& other) const
        {
            return foreground_color == other.foreground_color && background_color == other.background_color && flags == other.flags;
        }
        bool operator!=(const Attribute& other) const
        {
            return !(*this == other);
        }
    };

    struct Line {
        explicit Line(u16 columns);
        ~Line();
        void clear(Attribute);
        bool has_only_one_background_color() const;
        void set_length(u16);
        u8* characters { nullptr };
        Attribute* attributes { nullptr };
        bool dirty { false };
        u16 m_length { 0 };
    };
    Line& line(size_t index)
    {
        ASSERT(index < m_rows);
        return m_lines[index];
    }
    const Line& line(size_t index) const
    {
        ASSERT(index < m_rows);
        return m_lines[index];
    }

    NonnullOwnPtrVector<Line> m_lines;

    BufferPosition m_selection_start;
    BufferPosition m_selection_end;

    int m_scroll_region_top { 0 };
    int m_scroll_region_bottom { 0 };

    u16 m_columns { 0 };
    u16 m_rows { 0 };

    u16 m_cursor_row { 0 };
    u16 m_cursor_column { 0 };
    u16 m_saved_cursor_row { 0 };
    u16 m_saved_cursor_column { 0 };
    bool m_stomp { false };

    bool m_should_beep { false };

    Attribute m_current_attribute;

    void execute_escape_sequence(u8 final);
    void execute_xterm_command();

    enum EscapeState {
        Normal,
        ExpectBracket,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,

        ExpectXtermParameter1,
        ExpectXtermParameter2,
        ExpectXtermFinal,
    };
    EscapeState m_escape_state { Normal };
    Vector<u8> m_parameters;
    Vector<u8> m_intermediates;
    Vector<u8> m_xterm_param1;
    Vector<u8> m_xterm_param2;
    Vector<bool> m_horizontal_tabs;
    u8 m_final { 0 };
    bool m_belling { false };

    int m_pixel_width { 0 };
    int m_pixel_height { 0 };

    int m_inset { 2 };
    int m_line_spacing { 4 };
    int m_line_height { 0 };

    int m_ptm_fd { -1 };

    bool m_swallow_current { false };

    bool m_in_active_window { false };
    bool m_need_full_flush { false };

    CNotifier m_notifier;

    u8 m_opacity { 255 };
    bool m_needs_background_fill { true };
    bool m_cursor_blink_state { true };

    int m_glyph_width { 0 };

    CTimer m_cursor_blink_timer;
    CTimer m_visual_beep_timer;
    RefPtr<CConfigFile> m_config;

    u8 m_last_char { 0 };
};
