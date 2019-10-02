#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

class MDText final {
public:
    struct Style {
        bool emph { false };
        bool strong { false };
        bool code { false };
        String href;
    };

    struct Span {
        String text;
        Style style;
    };

    const Vector<Span>& spans() const { return m_spans; }

    String render_to_html() const;
    String render_for_terminal() const;

    bool parse(const StringView&);

private:
    Vector<Span> m_spans;
};
