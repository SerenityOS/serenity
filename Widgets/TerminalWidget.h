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

    void onReceive(const ByteBuffer&);
    void onReceive(byte);
    
private:
    CharacterWithAttributes& at(unsigned row, unsigned column);

    virtual void onPaint(PaintEvent&) override;
    virtual void onKeyDown(KeyEvent&) override;
    virtual void onKeyUp(KeyEvent&) override;

    unsigned m_columns { 80 };
    unsigned m_rows { 25 };

    unsigned m_cursorRow { 0 };
    unsigned m_cursorColumn { 0 };

    CharacterWithAttributes* m_screen { nullptr };
};
