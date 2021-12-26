#pragma once

#include "Size.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class CharacterBitmap : public Retainable<CharacterBitmap> {
public:
    static RetainPtr<CharacterBitmap> create_from_ascii(const char* asciiData, unsigned width, unsigned height);
    ~CharacterBitmap();

    bool bit_at(unsigned x, unsigned y) const { return m_bits[y * width() + x] == '#'; }
    const char* bits() const { return m_bits; }

    Size size() const { return m_size; }
    unsigned width() const { return m_size.width(); }
    unsigned height() const { return m_size.height(); }

private:
    CharacterBitmap(const char* b, unsigned w, unsigned h);

    const char* m_bits { nullptr };
    Size m_size;
};

