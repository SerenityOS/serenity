/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <AK/Vector.h>

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

    static NonnullRefPtr<Document> parse(const StringView& source, const URL&);

    const URL& url() const { return m_url; };

private:
    explicit Document(const URL& url)
        : m_url(url)
    {
    }

    void read_lines(const StringView&);

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
