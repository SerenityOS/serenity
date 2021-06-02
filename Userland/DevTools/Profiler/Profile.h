/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DisassemblyModel.h"
#include "Process.h"
#include "Profile.h"
#include "ProfileModel.h"
#include "SamplesModel.h"
#include <AK/Bitmap.h>
#include <AK/FlyString.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/MappedFile.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/Result.h>
#include <LibELF/Image.h>
#include <LibGUI/Forward.h>
#include <LibGUI/ModelIndex.h>

namespace Profiler {

class ProfileNode : public RefCounted<ProfileNode> {
public:
    static NonnullRefPtr<ProfileNode> create(Process const& process, FlyString object_name, String symbol, u32 address, u32 offset, u64 timestamp, pid_t pid)
    {
        return adopt_ref(*new ProfileNode(process, move(object_name), move(symbol), address, offset, timestamp, pid));
    }

    static NonnullRefPtr<ProfileNode> create_process_node(Process const& process)
    {
        return adopt_ref(*new ProfileNode(process));
    }

    // These functions are only relevant for root nodes
    void will_track_seen_events(size_t profile_event_count)
    {
        if (m_seen_events.size() != profile_event_count)
            m_seen_events = Bitmap { profile_event_count, false };
    }
    bool has_seen_event(size_t event_index) const { return m_seen_events.get(event_index); }
    void did_see_event(size_t event_index) { m_seen_events.set(event_index, true); }

    const FlyString& object_name() const { return m_object_name; }
    const String& symbol() const { return m_symbol; }
    u32 address() const { return m_address; }
    u32 offset() const { return m_offset; }
    u64 timestamp() const { return m_timestamp; }

    u32 event_count() const { return m_event_count; }
    u32 self_count() const { return m_self_count; }

    int child_count() const { return m_children.size(); }
    const Vector<NonnullRefPtr<ProfileNode>>& children() const { return m_children; }

    void add_child(ProfileNode& child)
    {
        if (child.m_parent == this)
            return;
        VERIFY(!child.m_parent);
        child.m_parent = this;
        m_children.append(child);
    }

    ProfileNode& find_or_create_child(FlyString object_name, String symbol, u32 address, u32 offset, u64 timestamp, pid_t pid)
    {
        for (size_t i = 0; i < m_children.size(); ++i) {
            auto& child = m_children[i];
            if (child->symbol() == symbol) {
                return child;
            }
        }
        auto new_child = ProfileNode::create(m_process, move(object_name), move(symbol), address, offset, timestamp, pid);
        add_child(new_child);
        return new_child;
    };

    ProfileNode* parent() { return m_parent; }
    const ProfileNode* parent() const { return m_parent; }

    void increment_event_count() { ++m_event_count; }
    void increment_self_count() { ++m_self_count; }

    void sort_children();

    const HashMap<FlatPtr, size_t>& events_per_address() const { return m_events_per_address; }
    void add_event_address(FlatPtr address)
    {
        auto it = m_events_per_address.find(address);
        if (it == m_events_per_address.end())
            m_events_per_address.set(address, 1);
        else
            m_events_per_address.set(address, it->value + 1);
    }

    pid_t pid() const { return m_pid; }

    Process const& process() const { return m_process; }
    bool is_root() const { return m_root; }

private:
    explicit ProfileNode(Process const&);
    explicit ProfileNode(Process const&, const String& object_name, String symbol, u32 address, u32 offset, u64 timestamp, pid_t);

    bool m_root { false };
    Process const& m_process;
    ProfileNode* m_parent { nullptr };
    FlyString m_object_name;
    String m_symbol;
    pid_t m_pid { 0 };
    u32 m_address { 0 };
    u32 m_offset { 0 };
    u32 m_event_count { 0 };
    u32 m_self_count { 0 };
    u64 m_timestamp { 0 };
    Vector<NonnullRefPtr<ProfileNode>> m_children;
    HashMap<FlatPtr, size_t> m_events_per_address;
    Bitmap m_seen_events;
};

struct ProcessFilter {
    pid_t pid { 0 };
    EventSerialNumber start_valid;
    EventSerialNumber end_valid;

    bool operator==(ProcessFilter const& rhs) const
    {
        return pid == rhs.pid && start_valid == rhs.start_valid && end_valid == rhs.end_valid;
    }
};

class Profile {
public:
    static Result<NonnullOwnPtr<Profile>, String> load_from_perfcore_file(const StringView& path);

    GUI::Model& model();
    GUI::Model& samples_model();
    GUI::Model* disassembly_model();

    const Process* find_process(pid_t pid, EventSerialNumber serial) const
    {
        auto it = m_processes.find_if([&pid, &serial](auto& entry) {
            return entry.pid == pid && entry.valid_at(serial);
        });
        return it.is_end() ? nullptr : &(*it);
    }

    void set_disassembly_index(const GUI::ModelIndex&);

    const Vector<NonnullRefPtr<ProfileNode>>& roots() const { return m_roots; }

    struct Frame {
        FlyString object_name;
        String symbol;
        u32 address { 0 };
        u32 offset { 0 };
    };

    struct Event {
        EventSerialNumber serial;
        u64 timestamp { 0 };
        String type;
        FlatPtr ptr { 0 };
        size_t size { 0 };
        String name;
        int parent_pid { 0 };
        int parent_tid { 0 };
        String executable;
        int pid { 0 };
        int tid { 0 };
        u32 lost_samples { 0 };
        bool in_kernel { false };
        Vector<Frame> frames;
    };

    const Vector<Event>& events() const { return m_events; }
    const Vector<size_t>& filtered_event_indices() const { return m_filtered_event_indices; }

    u64 length_in_ms() const { return m_last_timestamp - m_first_timestamp; }
    u64 first_timestamp() const { return m_first_timestamp; }
    u64 last_timestamp() const { return m_last_timestamp; }

    void set_timestamp_filter_range(u64 start, u64 end);
    void clear_timestamp_filter_range();
    bool has_timestamp_filter_range() const { return m_has_timestamp_filter_range; }

    void add_process_filter(pid_t pid, EventSerialNumber start_valid, EventSerialNumber end_valid);
    void remove_process_filter(pid_t pid, EventSerialNumber start_valid, EventSerialNumber end_valid);
    void clear_process_filter();
    bool has_process_filter() const { return !m_process_filters.is_empty(); }
    bool process_filter_contains(pid_t pid, EventSerialNumber serial);

    bool is_inverted() const { return m_inverted; }
    void set_inverted(bool);

    void set_show_top_functions(bool);

    bool show_percentages() const { return m_show_percentages; }
    void set_show_percentages(bool);

    const Vector<Process>& processes() const { return m_processes; }

    template<typename Callback>
    void for_each_event_in_filter_range(Callback callback)
    {
        for (auto& event : m_events) {
            if (has_timestamp_filter_range()) {
                auto timestamp = event.timestamp;
                if (timestamp < m_timestamp_filter_range_start || timestamp > m_timestamp_filter_range_end)
                    continue;
            }
            callback(event);
        }
    }

private:
    Profile(Vector<Process>, Vector<Event>);

    void rebuild_tree();

    RefPtr<ProfileModel> m_model;
    RefPtr<SamplesModel> m_samples_model;
    RefPtr<DisassemblyModel> m_disassembly_model;

    GUI::ModelIndex m_disassembly_index;

    Vector<NonnullRefPtr<ProfileNode>> m_roots;
    Vector<size_t> m_filtered_event_indices;
    u64 m_first_timestamp { 0 };
    u64 m_last_timestamp { 0 };

    Vector<Process> m_processes;
    Vector<Event> m_events;

    bool m_has_timestamp_filter_range { false };
    u64 m_timestamp_filter_range_start { 0 };
    u64 m_timestamp_filter_range_end { 0 };

    Vector<ProcessFilter> m_process_filters;

    bool m_inverted { false };
    bool m_show_top_functions { false };
    bool m_show_percentages { false };
};

}
