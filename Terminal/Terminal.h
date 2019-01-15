#pragma once

#include <AK/AKString.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Widgets/GraphicsBitmap.h>


class Terminal {
public:
    Terminal();
    ~Terminal();

    void create_window();
    void paint();
    void on_char(byte);

private:
    void scroll_up();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, byte ch);

    void escape$A(const Vector<unsigned>&);
    void escape$D(const Vector<unsigned>&);
    void escape$H(const Vector<unsigned>&);
    void escape$J(const Vector<unsigned>&);
    void escape$m(const Vector<unsigned>&);
    void escape$s(const Vector<unsigned>&);
    void escape$u(const Vector<unsigned>&);

    void clear();

    void set_size(word columns, word rows);
    word columns() const { return m_columns; }
    word rows() const { return m_rows; }

    struct Attribute {
        Attribute() { reset(); }
        void reset()
        {
            foreground_color = 7;
            background_color = 0;
            bold = false;
        }
        unsigned foreground_color : 4;
        unsigned background_color : 4;
        bool bold : 1;
    };

    byte* m_buffer { nullptr };
    Attribute* m_attributes { nullptr };

    word m_columns { 0 };
    word m_rows { 0 };

    byte m_cursor_row { 0 };
    byte m_cursor_column { 0 };
    byte m_saved_cursor_row { 0 };
    byte m_saved_cursor_column { 0 };

    Attribute m_current_attribute;

    void execute_escape_sequence(byte final);

    enum EscapeState {
        Normal,
        ExpectBracket,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,
    };
    EscapeState m_escape_state { Normal };
    Vector<byte> m_parameters;
    Vector<byte> m_intermediates;
    byte* m_horizontal_tabs { nullptr };
    bool m_belling { false };

    int m_window_id { 0 };
    RetainPtr<GraphicsBitmap> m_backing;

    int m_pixel_width { 0 };
    int m_pixel_height { 0 };

    int m_inset { 2 };
    int m_line_spacing { 4 };
};
