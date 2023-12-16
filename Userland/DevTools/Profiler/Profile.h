/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Jakub Berkop <jakub.berkop@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DisassemblyModel.h"
#include "FilesystemEventModel.h"
#include "Process.h"
#include "Profile.h"
#include "ProfileModel.h"
#include "SamplesModel.h"
#include "SignpostsModel.h"
#include "SourceModel.h"
#include <AK/Bitmap.h>
#include <AK/DeprecatedFlyString.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/OwnPtr.h>
#include <AK/Time.h>
#include <AK/Variant.h>
#include <LibCore/MappedFile.h>
#include <LibELF/Image.h>
#include <LibGUI/Forward.h>
#include <LibGUI/ModelIndex.h>

namespace Profiler {

extern Optional<MappedObject> g_kernel_debuginfo_object;
extern OwnPtr<Debug::DebugInfo> g_kernel_debug_info;

class ProfileNode : public RefCounted<ProfileNode> {
public:
    static NonnullRefPtr<ProfileNode> create(Process const& process, DeprecatedFlyString const& object_name, ByteString symbol, FlatPtr address, u32 offset, u64 timestamp, pid_t pid)
    {
        return adopt_ref(*new ProfileNode(process, object_name, move(symbol), address, offset, timestamp, pid));
    }

    static NonnullRefPtr<ProfileNode> create_process_node(Process const& process)
    {
        return adopt_ref(*new ProfileNode(process));
    }

    // These functions are only relevant for root nodes
    void will_track_seen_events(size_t profile_event_count)
    {
        if (m_seen_events.size() != profile_event_count)
            m_seen_events = Bitmap::create(profile_event_count, false).release_value_but_fixme_should_propagate_errors();
    }
    bool has_seen_event(size_t event_index) const { return m_seen_events.get(event_index); }
    void did_see_event(size_t event_index) { m_seen_events.set(event_index, true); }

    DeprecatedFlyString const& object_name() const { return m_object_name; }
    ByteString const& symbol() const { return m_symbol; }
    FlatPtr address() const { return m_address; }
    u32 offset() const { return m_offset; }
    u64 timestamp() const { return m_timestamp; }

    u32 event_count() const { return m_event_count; }
    u32 self_count() const { return m_self_count; }

    int child_count() const { return m_children.size(); }
    Vector<NonnullRefPtr<ProfileNode>> const& children() const { return m_children; }

    void add_child(ProfileNode& child)
    {
        if (child.m_parent == this)
            return;
        VERIFY(!child.m_parent);
        child.m_parent = this;
        m_children.append(child);
    }

    ProfileNode& find_or_create_child(DeprecatedFlyString const& object_name, ByteString symbol, FlatPtr address, u32 offset, u64 timestamp, pid_t pid)
    {
        for (size_t i = 0; i < m_children.size(); ++i) {
            auto& child = m_children[i];
            if (child->symbol() == symbol) {
                return child;
            }
        }
        auto new_child = ProfileNode::create(m_process, object_name, move(symbol), address, offset, timestamp, pid);
        add_child(new_child);
        return new_child;
    }

    ProfileNode* parent() { return m_parent; }
    ProfileNode const* parent() const { return m_parent; }

    void increment_event_count() { ++m_event_count; }
    void increment_self_count() { ++m_self_count; }

    void sort_children();

    HashMap<FlatPtr, size_t> const& events_per_address() const { return m_events_per_address; }
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
    explicit ProfileNode(Process const&, DeprecatedFlyString const& object_name, ByteString symbol, FlatPtr address, u32 offset, u64 timestamp, pid_t);

    bool m_root { false };
    Process const& m_process;
    ProfileNode* m_parent { nullptr };
    DeprecatedFlyString m_object_name;
    ByteString m_symbol;
    pid_t m_pid { 0 };
    FlatPtr m_address { 0 };
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
    static ErrorOr<NonnullOwnPtr<Profile>> load_from_perfcore_file(StringView path);

    GUI::Model& model();
    GUI::Model& samples_model();
    GUI::Model& signposts_model();
    GUI::Model* disassembly_model();
    GUI::Model* source_model();
    GUI::Model* file_event_model();

    Process const* find_process(pid_t pid, EventSerialNumber serial) const
    {
        auto it = m_processes.find_if([&pid, &serial](auto& entry) {
            return entry.pid == pid && entry.valid_at(serial);
        });
        return it.is_end() ? nullptr : &(*it);
    }

    void set_disassembly_index(GUI::ModelIndex const&);
    void set_source_index(GUI::ModelIndex const&);

    Vector<NonnullRefPtr<ProfileNode>> const& roots() const { return m_roots; }

    struct Frame {
        DeprecatedFlyString object_name;
        ByteString symbol;
        FlatPtr address { 0 };
        u32 offset { 0 };
    };

    struct Event {
        u64 timestamp { 0 };
        EventSerialNumber serial;
        pid_t pid { 0 };
        pid_t tid { 0 };
        u32 lost_samples { 0 };
        bool in_kernel { false };

        Vector<Frame> frames;

        struct SampleData {
        };

        struct MallocData {
            FlatPtr ptr {};
            size_t size {};
        };

        struct FreeData {
            FlatPtr ptr {};
        };

        struct SignpostData {
            ByteString string;
            FlatPtr arg {};
        };

        struct MmapData {
            FlatPtr ptr {};
            size_t size {};
            ByteString name;
        };

        struct MunmapData {
            FlatPtr ptr {};
            size_t size {};
        };

        struct ProcessCreateData {
            pid_t parent_pid { 0 };
            ByteString executable;
        };

        struct ProcessExecData {
            ByteString executable;
        };

        struct ThreadCreateData {
            pid_t parent_tid {};
        };

        // Based on Syscall::SC_open_params
        struct OpenEventData {
            int dirfd;
            ByteString path;
            int options;
            u64 mode;
        };

        struct CloseEventData {
            int fd;
            ByteString path;
        };

        struct ReadvEventData {
            int fd;
            ByteString path;
            // struct iovec* iov; // TODO: Implement
            // int iov_count; // TODO: Implement
        };

        struct ReadEventData {
            int fd;
            ByteString path;
        };

        struct PreadEventData {
            int fd;
            ByteString path;
            FlatPtr buffer_ptr;
            size_t size;
            off_t offset;
        };

        struct FilesystemEventData {
            Duration duration;
            Variant<OpenEventData, CloseEventData, ReadvEventData, ReadEventData, PreadEventData> data;
        };

        Variant<nullptr_t, SampleData, MallocData, FreeData, SignpostData, MmapData, MunmapData, ProcessCreateData, ProcessExecData, ThreadCreateData, FilesystemEventData> data { nullptr };
    };

    Vector<Event> const& events() const { return m_events; }
    Vector<size_t> const& filtered_event_indices() const { return m_filtered_event_indices; }
    Vector<size_t> const& filtered_signpost_indices() const { return m_filtered_signpost_indices; }
    NonnullRefPtr<FileEventNode> const& file_event_nodes() { return m_file_event_nodes; }

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

    Vector<Process> const& processes() const { return m_processes; }

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

    template<typename Callback>
    void for_each_signpost(Callback callback) const
    {
        for (auto index : m_signpost_indices) {
            auto const& event = m_events[index];
            if (callback(event) == IterationDecision::Break)
                break;
        }
    }

private:
    Profile(Vector<Process>, Vector<Event>);

    void rebuild_tree();

    RefPtr<ProfileModel> m_model;
    RefPtr<SamplesModel> m_samples_model;
    RefPtr<SignpostsModel> m_signposts_model;
    RefPtr<DisassemblyModel> m_disassembly_model;
    RefPtr<SourceModel> m_source_model;
    RefPtr<FileEventModel> m_file_event_model;

    GUI::ModelIndex m_disassembly_index;
    GUI::ModelIndex m_source_index;

    Vector<NonnullRefPtr<ProfileNode>> m_roots;
    Vector<size_t> m_filtered_event_indices;
    u64 m_first_timestamp { 0 };
    u64 m_last_timestamp { 0 };

    Vector<Process> m_processes;
    Vector<Event> m_events;
    Vector<size_t> m_signpost_indices;
    Vector<size_t> m_filtered_signpost_indices;

    bool m_has_timestamp_filter_range { false };
    u64 m_timestamp_filter_range_start { 0 };
    u64 m_timestamp_filter_range_end { 0 };

    Vector<ProcessFilter> m_process_filters;

    NonnullRefPtr<FileEventNode> m_file_event_nodes;

    bool m_inverted { false };
    bool m_show_top_functions { false };
    bool m_show_percentages { false };
};

}
