#pragma once

#include "Widget.h"
#include <AK/ByteBuffer.h>

struct CharacterWithAttributes {
    byte character;
    byte attribute;
};

class TerminalWidget final : public Widget {
public:
    explicit TerminalWidget(Widget* parent);
    virtual ~TerminalWidget() override;

    unsigned rows() const { return m_rows; }
    unsigned columns() const { return m_columns; }
    
private:
    CharacterWithAttributes& at(unsigned row, unsigned column);

    virtual void onPaint(PaintEvent&) override;
    void onReceive(const ByteBuffer&);
    void onReceive(byte);

    unsigned m_columns { 80 };
    unsigned m_rows { 25 };

    unsigned m_cursorRow { 0 };
    unsigned m_cursorColumn { 0 };

    CharacterWithAttributes* m_screen { nullptr };
};
