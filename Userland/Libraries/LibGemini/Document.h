/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/URL.h>

namespace Gemini {

class Line {
public:
    Line(String string)
        : m_text(move(string))
    {
    }

    virtual ~Line();

    virtual String render_to_html() const = 0;

protected:
    String m_text;
};

class Document : public RefCounted<Document> {
public:
    String render_to_html() const;

    static NonnullRefPtr<Document> parse(StringView source, const URL&);

    const URL& url() const { return m_url; };

private:
    explicit Document(const URL& url)
        : m_url(url)
    {
    }

    void read_lines(StringView);

    NonnullOwnPtrVector<Line> m_lines;
    URL m_url;
    bool m_inside_preformatted_block { false };
    bool m_inside_unordered_list { false };
};

class Text : public Line {
public:
    Text(String line)
        : Line(move(line))
    {
    }
    virtual ~Text() override;
    virtual String render_to_html() const override;
};

class Link : public Line {
public:
    Link(String line, const Document&);
    virtual ~Link() override;
    virtual String render_to_html() const override;

private:
    URL m_url;
    String m_name;
};

class Preformatted : public Line {
public:
    Preformatted(String line)
        : Line(move(line))
    {
    }
    virtual ~Preformatted() override;
    virtual String render_to_html() const override;
};

class UnorderedList : public Line {
public:
    UnorderedList(String line)
        : Line(move(line))
    {
    }
    virtual ~UnorderedList() override;
    virtual String render_to_html() const override;
};

class Control : public Line {
public:
    enum Kind {
        UnorderedListStart,
        UnorderedListEnd,
        PreformattedStart,
        PreformattedEnd,
    };
    Control(Kind kind)
        : Line("")
        , m_kind(kind)
    {
    }
    virtual ~Control() override;
    virtual String render_to_html() const override;

private:
    Kind m_kind;
};

class Heading : public Line {
public:
    Heading(String line, int level)
        : Line(move(line))
        , m_level(level)
    {
    }
    virtual ~Heading() override;
    virtual String render_to_html() const override;

private:
    int m_level { 1 };
};

}
