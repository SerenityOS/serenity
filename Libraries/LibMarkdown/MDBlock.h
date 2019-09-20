#pragma once

#include <AK/String.h>

class MDBlock {
public:
    virtual ~MDBlock() {}

    virtual String render_to_html() const = 0;
    virtual String render_for_terminal() const = 0;
    virtual bool parse(Vector<StringView>::ConstIterator& lines) = 0;
};
