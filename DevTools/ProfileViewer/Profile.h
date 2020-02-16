/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <LibGUI/Forward.h>

class ProfileModel;

class ProfileNode : public RefCounted<ProfileNode> {
public:
    static NonnullRefPtr<ProfileNode> create(const String& symbol, u32 address, u32 offset, u64 timestamp)
    {
        return adopt(*new ProfileNode(symbol, address, offset, timestamp));
    }

    const String& symbol() const { return m_symbol; }
    u32 address() const { return m_address; }
    u32 offset() const { return m_offset; }
    u64 timestamp() const { return m_timestamp; }

    u32 sample_count() const { return m_sample_count; }

    int child_count() const { return m_children.size(); }
    const Vector<NonnullRefPtr<ProfileNode>>& children() const { return m_children; }

    void add_child(ProfileNode& child)
    {
        if (child.m_parent == this)
            return;
        ASSERT(!child.m_parent);
        child.m_parent = this;
        m_children.append(child);
    }

    ProfileNode& find_or_create_child(const String& symbol, u32 address, u32 offset, u64 timestamp)
    {
        for (int i = 0; i < m_children.size(); ++i) {
            auto& child = m_children[i];
            if (child->symbol() == symbol) {
                return child;
            }
        }
        auto new_child = ProfileNode::create(symbol, address, offset, timestamp);
        add_child(new_child);
        return new_child;
    };

    ProfileNode* parent() { return m_parent; }
    const ProfileNode* parent() const { return m_parent; }

    void increment_sample_count() { ++m_sample_count; }

    void sort_children();

private:
    explicit ProfileNode(const String& symbol, u32 address, u32 offset, u64 timestamp)
        : m_symbol(symbol)
        , m_address(address)
        , m_offset(offset)
        , m_timestamp(timestamp)
    {
    }

    ProfileNode* m_parent { nullptr };
    String m_symbol;
    u32 m_address { 0 };
    u32 m_offset { 0 };
    u32 m_sample_count { 0 };
    u64 m_timestamp { 0 };
    Vector<NonnullRefPtr<ProfileNode>> m_children;
};

class Profile {
public:
    static OwnPtr<Profile> load_from_file(const StringView& path);
    static OwnPtr<Profile> load_from_perfcore_file(const StringView& path);
    ~Profile();

    GUI::Model& model();

    const Vector<NonnullRefPtr<ProfileNode>>& roots() const { return m_roots; }

    struct Frame {
        String symbol;
        u32 address { 0 };
        u32 offset { 0 };
    };

    struct Sample {
        u64 timestamp { 0 };
        String type;
        uintptr_t ptr { 0 };
        size_t size { 0 };
        bool in_kernel { false };
        Vector<Frame> frames;
    };

    const Vector<Sample>& samples() const { return m_samples; }

    u64 length_in_ms() const { return m_last_timestamp - m_first_timestamp; }
    u64 first_timestamp() const { return m_first_timestamp; }
    u64 last_timestamp() const { return m_last_timestamp; }
    u32 deepest_stack_depth() const { return m_deepest_stack_depth; }

    void set_timestamp_filter_range(u64 start, u64 end);
    void clear_timestamp_filter_range();
    bool has_timestamp_filter_range() const { return m_has_timestamp_filter_range; }

    bool is_inverted() const { return m_inverted; }
    void set_inverted(bool);

private:
    explicit Profile(const JsonArray&);

    void rebuild_tree();

    JsonArray m_json;
    RefPtr<ProfileModel> m_model;
    Vector<NonnullRefPtr<ProfileNode>> m_roots;
    u64 m_first_timestamp { 0 };
    u64 m_last_timestamp { 0 };

    Vector<Sample> m_samples;

    bool m_has_timestamp_filter_range { false };
    u64 m_timestamp_filter_range_start { 0 };
    u64 m_timestamp_filter_range_end { 0 };

    u32 m_deepest_stack_depth { 0 };
    bool m_inverted { false };
};
