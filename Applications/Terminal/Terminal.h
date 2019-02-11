#pragma once

#include <AK/AKString.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Rect.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GNotifier.h>

class Font;

class Terminal final : public GWidget {
public:
    explicit Terminal(int ptm_fd);
    virtual ~Terminal() override;

    void create_window();
    void on_char(byte);

    void flush_dirty_lines();

private:
    virtual void event(GEvent&) override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual const char* class_name() const override { return "Terminal"; }

    Font& font() { return *m_font; }
    void scroll_up();
    void newline();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, byte ch);
    void invalidate_cursor();
    void set_window_title(String&&);

    void inject_string(const String&);
    void unimplemented_escape();
    void unimplemented_xterm_escape();

    void escape$A(const Vector<unsigned>&);
    void escape$B(const Vector<unsigned>&);
    void escape$C(const Vector<unsigned>&);
    void escape$D(const Vector<unsigned>&);
    void escape$H(const Vector<unsigned>&);
    void escape$J(const Vector<unsigned>&);
    void escape$K(const Vector<unsigned>&);
    void escape$M(const Vector<unsigned>&);
    void escape$m(const Vector<unsigned>&);
    void escape$s(const Vector<unsigned>&);
    void escape$u(const Vector<unsigned>&);

    void clear();

    void set_size(word columns, word rows);
    word columns() const { return m_columns; }
    word rows() const { return m_rows; }
    Rect glyph_rect(word row, word column);
    Rect row_rect(word row);

    struct Attribute {
        Attribute() { reset(); }
        void reset()
        {
            foreground_color = 7;
            background_color = 0;
            //bold = false;
        }
        byte foreground_color;
        byte background_color;
        //bool bold : 1;
        bool operator==(const Attribute& other) const
        {
            return foreground_color == other.foreground_color && background_color == other.background_color;
        }
        bool operator!=(const Attribute& other) const
        {
            return !(*this == other);
        }
    };

    struct Line {
        explicit Line(word columns);
        ~Line();
        void clear(Attribute);
        bool has_only_one_background_color() const;
        byte* characters { nullptr };
        Attribute* attributes { nullptr };
        bool dirty { false };
        word length { 0 };
    };
    Line& line(size_t index) { ASSERT(index < m_rows); return *m_lines[index]; }

    Line** m_lines { nullptr };

    word m_columns { 0 };
    word m_rows { 0 };

    byte m_cursor_row { 0 };
    byte m_cursor_column { 0 };
    byte m_saved_cursor_row { 0 };
    byte m_saved_cursor_column { 0 };
    bool m_stomp { false };

    Attribute m_current_attribute;

    void execute_escape_sequence(byte final);
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
    Vector<byte> m_parameters;
    Vector<byte> m_intermediates;
    Vector<byte> m_xterm_param1;
    Vector<byte> m_xterm_param2;
    byte m_final { 0 };
    byte* m_horizontal_tabs { nullptr };
    bool m_belling { false };

    int m_pixel_width { 0 };
    int m_pixel_height { 0 };
    int m_rows_to_scroll_backing_store { 0 };

    int m_inset { 2 };
    int m_line_spacing { 4 };
    int m_line_height { 0 };

    int m_ptm_fd { -1 };

    bool m_in_active_window { false };
    bool m_need_full_flush { false };

    RetainPtr<Font> m_font;

    GNotifier m_notifier;
};
