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

#include <AK/ByteBuffer.h>
#include <AK/Rope.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

String RopeNode::to_string() const
{
    constexpr auto checked_return_string = [](auto& node) -> String {
        if (!node)
            return {};

        return node->to_string();
    };

    if (!is_append())
        return m_string;

    if (!m_left)
        return checked_return_string(m_right);

    if (!m_right)
        return checked_return_string(m_left);

    if (length() == 0)
        return "";

    auto left = m_left->to_string();
    auto right = m_right->to_string();

    // FIXME: Less copying.
    auto buffer = ByteBuffer::create_uninitialized(left.length() + right.length());
    buffer.overwrite(0, left.characters(), left.length());
    buffer.overwrite(left.length(), right.characters(), right.length());

    return String::copy(buffer);
}

Rope::Rope(const Rope& other)
    : m_root(other.m_root)
{
}

Rope::Rope(Rope&& other)
    : m_root(move(other.m_root))
{
}

Rope::Rope(const StringView& view)
{
    m_root = RopeNode::construct();
    m_root->string() = view;
}

void RopeNode::remove(size_t start, size_t length)
{
    sanity_check();

    ASSERT(start <= this->length());

    auto end = length + start;
    ASSERT(end < this->length());

    if (is_append()) {
        ssize_t left_length = left()->length();
        auto left_start = min((ssize_t)start, left_length);
        auto left_end = min((ssize_t)end, left_length);
        if (left_start < left_length)
            left()->remove(left_start, left_end - left_start);

        ssize_t right_length = right()->length();
        auto right_start = max(0l, min((ssize_t)start - left_length, right_length));
        auto right_end = max(0l, min((ssize_t)end - left_length, right_length));
        if (right_end > 0)
            right()->remove(right_start, right_end);

        update_length();
    } else {
        m_string = m_string.substring(start, length);
    }

    rebalance_if_needed();
}

void RopeNode::insert(const StringView& text, size_t offset)
{
    sanity_check();
    ASSERT(offset <= length());

    if (is_append()) {
        auto left_length = left()->length();
        if (offset < left_length)
            left()->insert(text, offset);
        else
            m_right->insert(text, offset - left_length);
        update_length();
    } else {
        m_type = Type::Append;
        if (offset == 0) {
            m_left = construct(text);
            m_right = construct(m_string);
        } else {
            m_left = construct(m_string.substring(0, offset));
            if (offset == m_string.length()) {
                m_right = construct(text);
            } else {
                m_right = construct(
                    construct(text),
                    construct(m_string.substring(offset, m_string.length() - offset)));
            }
        }
        update_length();
    }

    rebalance_if_needed();
}

NonnullRefPtr<RopeNode> RopeNode::slice(size_t start, size_t length)
{
    sanity_check();
    ASSERT(start < this->length());
    ASSERT(start + length < this->length());

    ScopeGuard balancer([this] { rebalance_if_needed(); });

    if (!is_append()) {
        if (start == 0 && length == this->length())
            return *this;

        // Slice into this node requested, we have to split the node up.
        auto selected_slice = construct(m_string.substring(start, length));
        m_type = Type::Append;
        left() = construct(
            construct(m_string.substring(0, start)),
            selected_slice);
        right() = construct(m_string.substring(start + length, m_string.length() - start - length));
        return selected_slice;
    }

    if (start < left()->length() && start + length < left()->length())
        return left()->slice(start, length);

    if (start >= left()->length() && start + length >= left()->length() && start + length - left()->length() < right()->length())
        return right()->slice(start, length);

    // The requested slice is split between right() and left()
    // FIXME: Implement this.
    TODO();
}

NonnullRefPtr<RopeNode> RopeNode::slice(size_t start)
{
    ASSERT(start < length());
    return slice(start, length() - start);
}

char RopeNode::at(size_t i) const
{
    return leaf(i)->string()[0];
}

NonnullRefPtr<RopeNode> RopeNode::leaf(size_t offset) const
{
    ASSERT(offset < length());
    if (!is_append())
        return *this;

    if (left()->length() > offset)
        return *left();

    return *right();
}

// AA-tree.
void RopeNode::rebalance()
{
    if (!is_append())
        return;

    constexpr auto skew = [](auto& child) -> RefPtr<RopeNode> {
        if (!child || !child->left())
            return child;

        if (child->left()->level() == child->level()) {
            auto left = child->left();
            child->left() = left->right();
            left->right() = child;
            return left;
        }
        return child;
    };

    constexpr auto split = [](auto& child) -> RefPtr<RopeNode> {
        if (!child)
            return child;

        if (!child->right() || !child->right()->right())
            return child;

        if (child->level() == child->right()->right()->level()) {
            auto right = child->right();
            child->right() = right->left();
            right->left() = child;
            right->m_level++;
            return right;
        }

        return child;
    };

    auto new_left = skew(left());
    auto new_right = skew(right());

    new_left = split(new_left);
    new_right = split(new_right);

    m_left = new_left;
    m_right = new_right;

    update_length();
}

void RopeNode::rebalance_if_needed()
{
    if (!is_append())
        return;

    rebalance();
}

void RopeNode::update_length()
{
    if (is_append()) {
        left()->update_length();
        right()->update_length();
        m_length = left()->length() + right()->length();
    }
}

RopeNode::~RopeNode()
{
}

String Rope::to_string() const
{
    return m_root ? m_root->to_string() : String { "" };
}

}
