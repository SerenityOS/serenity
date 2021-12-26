#pragma once

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>

class GModel;
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
    ~Profile();

    GModel& model();

    const Vector<NonnullRefPtr<ProfileNode>>& roots() const { return m_roots; }

    template<typename Callback>
    void for_each_sample(Callback callback)
    {
        m_json.for_each([&](auto& value) {
            callback(value.as_object());
        });
    }

    u64 length_in_ms() const { return m_last_timestamp - m_first_timestamp; }
    u64 first_timestamp() const { return m_first_timestamp; }
    u64 last_timestamp() const { return m_first_timestamp; }

private:
    explicit Profile(const JsonArray&, Vector<NonnullRefPtr<ProfileNode>>&&, u64 first_timestamp, u64 last_timestamp);

    JsonArray m_json;
    RefPtr<ProfileModel> m_model;
    Vector<NonnullRefPtr<ProfileNode>> m_roots;
    u64 m_first_timestamp { 0 };
    u64 m_last_timestamp { 0 };
};
