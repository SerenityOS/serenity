/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/URL.h>

namespace Gemini {

class Line {
public:
    Line(DeprecatedString string)
        : m_text(move(string))
    {
    }

    virtual ~Line() = default;

    virtual DeprecatedString render_to_html() const = 0;

protected:
    DeprecatedString m_text;
};

class Document : public RefCounted<Document> {
public:
    DeprecatedString render_to_html() const;

    static NonnullRefPtr<Document> parse(StringView source, const URL&);

    const URL& url() const { return m_url; }

private:
    explicit Document(const URL& url)
        : m_url(url)
    {
    }

    void read_lines(StringView);

    Vector<NonnullOwnPtr<Line>> m_lines;
    URL m_url;
    bool m_inside_preformatted_block { false };
    bool m_inside_unordered_list { false };
};

class Text : public Line {
public:
    Text(DeprecatedString line)
        : Line(move(line))
    {
    }
    virtual ~Text() override = default;
    virtual DeprecatedString render_to_html() const override;
};

class Link : public Line {
public:
    Link(DeprecatedString line, Document const&);
    virtual ~Link() override = default;
    virtual DeprecatedString render_to_html() const override;

private:
    URL m_url;
    DeprecatedString m_name;
};

class Preformatted : public Line {
public:
    Preformatted(DeprecatedString line)
        : Line(move(line))
    {
    }
    virtual ~Preformatted() override = default;
    virtual DeprecatedString render_to_html() const override;
};

class UnorderedList : public Line {
public:
    UnorderedList(DeprecatedString line)
        : Line(move(line))
    {
    }
    virtual ~UnorderedList() override = default;
    virtual DeprecatedString render_to_html() const override;
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
    virtual ~Control() override = default;
    virtual DeprecatedString render_to_html() const override;

private:
    Kind m_kind;
};

class Heading : public Line {
public:
    Heading(DeprecatedString line, int level)
        : Line(move(line))
        , m_level(level)
    {
    }
    virtual ~Heading() override = default;
    virtual DeprecatedString render_to_html() const override;

private:
    int m_level { 1 };
};

}
