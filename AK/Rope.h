/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace AK {

class RopeNode : public RefCounted<RopeNode> {
public:
    template<typename... Args>
    static NonnullRefPtr<RopeNode> construct(Args... args) { return adopt(*new RopeNode(move(args)...)); }

    RopeNode()
        : RopeNode("")
    {
    }

    RopeNode(const StringView& text)
        : m_type(Type::String)
        , m_string(text)
    {
    }

    RopeNode(RefPtr<RopeNode> left, RefPtr<RopeNode> right)
        : m_type(Type::Append)
        , m_left(move(left))
        , m_right(move(right))
    {
        update_length();
    }

    ~RopeNode();

    void remove(size_t start, size_t length);
    void insert(const StringView&, size_t offset);

    NonnullRefPtr<RopeNode> slice(size_t start, size_t length);
    NonnullRefPtr<RopeNode> slice(size_t start);

    NonnullRefPtr<RopeNode> leaf(size_t offset) const;
    char at(size_t) const;
    auto operator[](size_t i) const { return at(i); }

    const RefPtr<RopeNode>& left() const { return m_left; }
    const RefPtr<RopeNode>& right() const { return m_right; }
    const String& string() const { return m_string; }
    String& string() { return m_string; }

    String to_string() const;

    size_t length() const { return is_append() ? m_length : m_string.length(); }

    bool is_append() const { return m_type == Type::Append; }

private:
    RefPtr<RopeNode>& left() { return m_left; }
    RefPtr<RopeNode>& right() { return m_right; }

    size_t level() const { return m_level; }

    void rebalance_if_needed();
    void rebalance();
    void update_length();
    void sanity_check() const
    {
        if (is_append()) {
            ASSERT(m_left && m_right);
        } else {
            ASSERT(!m_left && !m_right);
        }
    }

    enum class Type {
        Append,
        String,
    } m_type { Type::String };

    RefPtr<RopeNode> m_left;
    RefPtr<RopeNode> m_right;
    size_t m_length { 0 };
    size_t m_level { 1 };

    String m_string;
};

class Rope {
public:
    Rope() { }

    Rope(const Rope& other);
    Rope(Rope&& other);
    Rope(const StringView& string);

    Rope& operator=(const Rope& other)
    {
        m_root = other.m_root;
        return *this;
    }

    Rope& operator=(Rope&& other)
    {
        m_root = move(other.m_root);
        return *this;
    }

    size_t length() const { return m_root ? m_root->length() : 0; }

    void remove(size_t start, size_t length)
    {
        ASSERT(m_root);
        m_root->remove(start, length);
    }

    void insert(const StringView& string, size_t offset)
    {
        if (m_root)
            m_root->insert(string, offset);
        else if (offset == 0) {
            m_root = RopeNode::construct(string);
        } else {
            ASSERT_NOT_REACHED();
        }
    }

    Rope slice(size_t start, size_t length) const
    {
        ASSERT(m_root);
        return { m_root->slice(start, length) };
    }

    Rope slice(size_t start) const
    {
        ASSERT(m_root);
        return { m_root->slice(start) };
    }

    char at(size_t i) const
    {
        ASSERT(m_root);
        return m_root->at(i);
    }
    auto operator[](size_t i) const { return at(i); }

    String to_string() const;

private:
    Rope(NonnullRefPtr<RopeNode> root)
        : m_root(move(root))
    {
    }

    mutable RefPtr<RopeNode> m_root;
};

}

using AK::Rope;
