#pragma once

#include "Size.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class CBitmap : public Retainable<CBitmap> {
public:
    static RetainPtr<CBitmap> createFromASCII(const char* asciiData, unsigned width, unsigned height);
    ~CBitmap();

    const char* bits() const { return m_bits; }

    Size size() const { return m_size; }
    unsigned width() const { return m_size.width(); }
    unsigned height() const { return m_size.height(); }

private:
    CBitmap(const char* b, unsigned w, unsigned h);

    const char* m_bits { nullptr };
    Size m_size;
};

