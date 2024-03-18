/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <AK/NonnullOwnPtr.h>
#include <LibURL/URL.h>

namespace Gemini {

class Line {
public:
    Line(ByteString string)
        : m_text(move(string))
    {
    }

    virtual ~Line() = default;

    virtual ByteString render_to_html() const = 0;

protected:
    ByteString m_text;
};

class Document : public RefCounted<Document> {
public:
    ByteString render_to_html() const;

    static NonnullRefPtr<Document> parse(StringView source, const URL::URL&);

    const URL::URL& url() const { return m_url; }

private:
    explicit Document(const URL::URL& url)
        : m_url(url)
    {
    }

    void read_lines(StringView);

    Vector<NonnullOwnPtr<Line>> m_lines;
    URL::URL m_url;
    bool m_inside_preformatted_block { false };
    bool m_inside_unordered_list { false };
};

class Text : public Line {
public:
    Text(ByteString line)
        : Line(move(line))
    {
    }
    virtual ~Text() override = default;
    virtual ByteString render_to_html() const override;
};

class Link : public Line {
public:
    Link(ByteString line, Document const&);
    virtual ~Link() override = default;
    virtual ByteString render_to_html() const override;

private:
    URL::URL m_url;
    ByteString m_name;
};

class Preformatted : public Line {
public:
    Preformatted(ByteString line)
        : Line(move(line))
    {
    }
    virtual ~Preformatted() override = default;
    virtual ByteString render_to_html() const override;
};

class UnorderedList : public Line {
public:
    UnorderedList(ByteString line)
        : Line(move(line))
    {
    }
    virtual ~UnorderedList() override = default;
    virtual ByteString render_to_html() const override;
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
    virtual ByteString render_to_html() const override;

private:
    Kind m_kind;
};

class Heading : public Line {
public:
    Heading(ByteString line, int level)
        : Line(move(line))
        , m_level(level)
    {
    }
    virtual ~Heading() override = default;
    virtual ByteString render_to_html() const override;

private:
    int m_level { 1 };
};

}
