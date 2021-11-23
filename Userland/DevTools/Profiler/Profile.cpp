/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Profile.h"
#include "DisassemblyModel.h"
#include "ProfileModel.h"
#include "SamplesModel.h"
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/QuickSort.h>
#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibELF/Image.h>
#include <LibSymbolication/Symbolication.h>
#include <sys/stat.h>

namespace Profiler {

static void sort_profile_nodes(Vector<NonnullRefPtr<ProfileNode>>& nodes)
{
    quick_sort(nodes.begin(), nodes.end(), [](auto& a, auto& b) {
        return a->event_count() >= b->event_count();
    });

    for (auto& child : nodes)
        child->sort_children();
}

Profile::Profile(Vector<Process> processes, Vector<Event> events)
    : m_processes(move(processes))
    , m_events(move(events))
{
    for (size_t i = 0; i < m_events.size(); ++i) {
        if (m_events[i].data.has<Event::SignpostData>())
            m_signpost_indices.append(i);
    }

    m_first_timestamp = m_events.first().timestamp;
    m_last_timestamp = m_events.last().timestamp;

    m_model = ProfileModel::create(*this);
    m_samples_model = SamplesModel::create(*this);
    m_signposts_model = SignpostsModel::create(*this);

    rebuild_tree();
}

GUI::Model& Profile::model()
{
    return *m_model;
}

GUI::Model& Profile::samples_model()
{
    return *m_samples_model;
}

GUI::Model& Profile::signposts_model()
{
    return *m_signposts_model;
}

void Profile::rebuild_tree()
{
    Vector<NonnullRefPtr<ProfileNode>> roots;

    auto find_or_create_process_node = [this, &roots](pid_t pid, EventSerialNumber serial) -> ProfileNode& {
        auto* process = find_process(pid, serial);
        if (!process) {
            dbgln("Profile contains event for unknown process with pid={}, serial={}", pid, serial.to_number());
            VERIFY_NOT_REACHED();
        }
        for (auto root : roots) {
            if (&root->process() == process)
                return root;
        }
        auto new_root = ProfileNode::create_process_node(*process);
        roots.append(new_root);
        return new_root;
    };

    HashTable<FlatPtr> live_allocations;

    for_each_event_in_filter_range([&](Event const& event) {
        event.data.visit(
            [&](Event::MallocData const& data) {
                live_allocations.set(data.ptr);
            },
            [&](Event::FreeData const& data) {
                live_allocations.remove(data.ptr);
            },
            [&](auto&) {});
    });

    m_filtered_event_indices.clear();
    m_filtered_signpost_indices.clear();

    for (size_t event_index = 0; event_index < m_events.size(); ++event_index) {
        auto& event = m_events.at(event_index);

        if (has_timestamp_filter_range()) {
            auto timestamp = event.timestamp;
            if (timestamp < m_timestamp_filter_range_start || timestamp > m_timestamp_filter_range_end)
                continue;
        }

        if (!process_filter_contains(event.pid, event.serial))
            continue;

        if (event.data.has<Event::SignpostData>()) {
            m_filtered_signpost_indices.append(event_index);
            continue;
        }

        m_filtered_event_indices.append(event_index);

        if (auto* malloc_data = event.data.get_pointer<Event::MallocData>(); malloc_data && !live_allocations.contains(malloc_data->ptr))
            continue;

        if (event.data.has<Event::FreeData>())
            continue;

        auto for_each_frame = [&]<typename Callback>(Callback callback) {
            if (!m_inverted) {
                for (size_t i = 0; i < event.frames.size(); ++i) {
                    if (callback(event.frames.at(i), i == event.frames.size() - 1) == IterationDecision::Break)
                        break;
                }
            } else {
                for (ssize_t i = event.frames.size() - 1; i >= 0; --i) {
                    if (callback(event.frames.at(i), static_cast<size_t>(i) == event.frames.size() - 1) == IterationDecision::Break)
                        break;
                }
            }
        };

        if (!m_show_top_functions) {
            ProfileNode* node = nullptr;
            auto& process_node = find_or_create_process_node(event.pid, event.serial);
            process_node.increment_event_count();
            for_each_frame([&](const Frame& frame, bool is_innermost_frame) {
                auto& object_name = frame.object_name;
                auto& symbol = frame.symbol;
                auto& address = frame.address;
                auto& offset = frame.offset;

                if (symbol.is_empty())
                    return IterationDecision::Break;

                // FIXME: More cheating with intentional mixing of TID/PID here:
                if (!node)
                    node = &process_node;
                node = &node->find_or_create_child(object_name, symbol, address, offset, event.timestamp, event.pid);

                node->increment_event_count();
                if (is_innermost_frame) {
                    node->add_event_address(address);
                    node->increment_self_count();
                }
                return IterationDecision::Continue;
            });
        } else {
            auto& process_node = find_or_create_process_node(event.pid, event.serial);
            process_node.increment_event_count();
            for (size_t i = 0; i < event.frames.size(); ++i) {
                ProfileNode* node = nullptr;
                ProfileNode* root = nullptr;
                for (size_t j = i; j < event.frames.size(); ++j) {
                    auto& frame = event.frames.at(j);
                    auto& object_name = frame.object_name;
                    auto& symbol = frame.symbol;
                    auto& address = frame.address;
                    auto& offset = frame.offset;
                    if (symbol.is_empty())
                        break;

                    // FIXME: More PID/TID mixing cheats here:
                    if (!node) {
                        node = &find_or_create_process_node(event.pid, event.serial);
                        node = &node->find_or_create_child(object_name, symbol, address, offset, event.timestamp, event.pid);
                        root = node;
                        root->will_track_seen_events(m_events.size());
                    } else {
                        node = &node->find_or_create_child(object_name, symbol, address, offset, event.timestamp, event.pid);
                    }

                    if (!root->has_seen_event(event_index)) {
                        root->did_see_event(event_index);
                        root->increment_event_count();
                    } else if (node != root) {
                        node->increment_event_count();
                    }

                    if (j == event.frames.size() - 1) {
                        node->add_event_address(address);
                        node->increment_self_count();
                    }
                }
            }
        }
    }

    sort_profile_nodes(roots);

    m_roots = move(roots);
    m_model->invalidate();
}

Optional<MappedObject> g_kernel_debuginfo_object;
OwnPtr<Debug::DebugInfo> g_kernel_debug_info;

ErrorOr<NonnullOwnPtr<Profile>> Profile::load_from_perfcore_file(StringView path)
{
    auto file = TRY(Core::File::open(path, Core::OpenMode::ReadOnly));

    auto json = JsonValue::from_string(file->read_all());
    if (json.is_error() || !json.value().is_object())
        return Error::from_string_literal("Invalid perfcore format (not a JSON object)"sv);

    auto& object = json.value().as_object();

    if (!g_kernel_debuginfo_object.has_value()) {
        auto debuginfo_file_or_error = Core::MappedFile::map("/boot/Kernel.debug");
        if (!debuginfo_file_or_error.is_error()) {
            auto debuginfo_file = debuginfo_file_or_error.release_value();
            auto debuginfo_image = ELF::Image(debuginfo_file->bytes());
            g_kernel_debuginfo_object = { { debuginfo_file, move(debuginfo_image) } };
        }
    }

    auto strings_value = object.get_ptr("strings"sv);
    if (!strings_value || !strings_value->is_array())
        return Error::from_string_literal("Malformed profile (strings is not an array)"sv);

    HashMap<FlatPtr, String> profile_strings;
    for (FlatPtr string_id = 0; string_id < strings_value->as_array().size(); ++string_id) {
        auto& value = strings_value->as_array().at(string_id);
        profile_strings.set(string_id, value.to_string());
    }

    auto events_value = object.get_ptr("events");
    if (!events_value || !events_value->is_array())
        return Error::from_string_literal("Malformed profile (events is not an array)"sv);

    auto& perf_events = events_value->as_array();

    NonnullOwnPtrVector<Process> all_processes;
    HashMap<pid_t, Process*> current_processes;
    Vector<Event> events;
    EventSerialNumber next_serial;

    for (auto& perf_event_value : perf_events.values()) {
        auto& perf_event = perf_event_value.as_object();

        Event event;

        event.serial = next_serial;
        next_serial.increment();
        event.timestamp = perf_event.get("timestamp").to_number<u64>();
        event.lost_samples = perf_event.get("lost_samples").to_number<u32>();
        event.pid = perf_event.get("pid").to_i32();
        event.tid = perf_event.get("tid").to_i32();

        auto type_string = perf_event.get("type").to_string();

        if (type_string == "sample"sv) {
            event.data = Event::SampleData {};
        } else if (type_string == "malloc"sv) {
            event.data = Event::MallocData {
                .ptr = perf_event.get("ptr"sv).to_number<FlatPtr>(),
                .size = perf_event.get("size"sv).to_number<size_t>(),
            };
        } else if (type_string == "free"sv) {
            event.data = Event::FreeData {
                .ptr = perf_event.get("ptr"sv).to_number<FlatPtr>(),
            };
        } else if (type_string == "signpost"sv) {
            auto string_id = perf_event.get("arg1"sv).to_number<FlatPtr>();
            event.data = Event::SignpostData {
                .string = profile_strings.get(string_id).value_or(String::formatted("Signpost #{}", string_id)),
                .arg = perf_event.get("arg2"sv).to_number<FlatPtr>(),
            };
        } else if (type_string == "mmap"sv) {
            auto ptr = perf_event.get("ptr"sv).to_number<FlatPtr>();
            auto size = perf_event.get("size"sv).to_number<size_t>();
            auto name = perf_event.get("name"sv).to_string();

            event.data = Event::MmapData {
                .ptr = ptr,
                .size = size,
                .name = name,
            };

            auto it = current_processes.find(event.pid);
            if (it != current_processes.end())
                it->value->library_metadata.handle_mmap(ptr, size, name);
            continue;
        } else if (type_string == "munmap"sv) {
            event.data = Event::MunmapData {
                .ptr = perf_event.get("ptr"sv).to_number<FlatPtr>(),
                .size = perf_event.get("size"sv).to_number<size_t>(),
            };
            continue;
        } else if (type_string == "process_create"sv) {
            auto parent_pid = perf_event.get("parent_pid"sv).to_number<pid_t>();
            auto executable = perf_event.get("executable"sv).to_string();
            event.data = Event::ProcessCreateData {
                .parent_pid = parent_pid,
                .executable = executable,
            };

            auto sampled_process = adopt_own(*new Process {
                .pid = event.pid,
                .executable = executable,
                .basename = LexicalPath::basename(executable),
                .start_valid = event.serial,
                .end_valid = {},
            });

            current_processes.set(sampled_process->pid, sampled_process);
            all_processes.append(move(sampled_process));
            continue;
        } else if (type_string == "process_exec"sv) {
            auto executable = perf_event.get("executable"sv).to_string();
            event.data = Event::ProcessExecData {
                .executable = executable,
            };

            auto old_process = current_processes.get(event.pid).value();
            old_process->end_valid = event.serial;

            current_processes.remove(event.pid);

            auto sampled_process = adopt_own(*new Process {
                .pid = event.pid,
                .executable = executable,
                .basename = LexicalPath::basename(executable),
                .start_valid = event.serial,
                .end_valid = {},
            });

            current_processes.set(sampled_process->pid, sampled_process);
            all_processes.append(move(sampled_process));
            continue;
        } else if (type_string == "process_exit"sv) {
            auto old_process = current_processes.get(event.pid).value();
            old_process->end_valid = event.serial;

            current_processes.remove(event.pid);
            continue;
        } else if (type_string == "thread_create"sv) {
            auto parent_tid = perf_event.get("parent_tid"sv).to_number<pid_t>();
            event.data = Event::ThreadCreateData {
                .parent_tid = parent_tid,
            };
            auto it = current_processes.find(event.pid);
            if (it != current_processes.end())
                it->value->handle_thread_create(event.tid, event.serial);
            continue;
        } else if (type_string == "thread_exit"sv) {
            auto it = current_processes.find(event.pid);
            if (it != current_processes.end())
                it->value->handle_thread_exit(event.tid, event.serial);
            continue;
        } else {
            dbgln("Unknown event type '{}'", type_string);
            VERIFY_NOT_REACHED();
        }

        auto maybe_kernel_base = Symbolication::kernel_base();

        auto* stack = perf_event.get_ptr("stack");
        VERIFY(stack);
        auto& stack_array = stack->as_array();
        for (ssize_t i = stack_array.values().size() - 1; i >= 0; --i) {
            auto& frame = stack_array.at(i);
            auto ptr = frame.to_number<u64>();
            u32 offset = 0;
            FlyString object_name;
            String symbol;

            if (maybe_kernel_base.has_value() && ptr >= maybe_kernel_base.value()) {
                if (g_kernel_debuginfo_object.has_value()) {
                    symbol = g_kernel_debuginfo_object->elf.symbolicate(ptr - maybe_kernel_base.value(), &offset);
                } else {
                    symbol = String::formatted("?? <{:p}>", ptr);
                }
            } else {
                auto it = current_processes.find(event.pid);
                // FIXME: This logic is kinda gnarly, find a way to clean it up.
                LibraryMetadata* library_metadata {};
                if (it != current_processes.end())
                    library_metadata = &it->value->library_metadata;
                if (auto* library = library_metadata ? library_metadata->library_containing(ptr) : nullptr) {
                    object_name = library->name;
                    symbol = library->symbolicate(ptr, &offset);
                } else {
                    symbol = String::formatted("?? <{:p}>", ptr);
                }
            }

            event.frames.append({ object_name, symbol, (FlatPtr)ptr, offset });
        }

        if (event.frames.size() < 2)
            continue;

        FlatPtr innermost_frame_address = event.frames.at(1).address;
        event.in_kernel = maybe_kernel_base.has_value() && innermost_frame_address >= maybe_kernel_base.value();

        events.append(move(event));
    }

    if (events.is_empty())
        return Error::from_string_literal("No events captured (targeted process was never on CPU)"sv);

    quick_sort(all_processes, [](auto& a, auto& b) {
        if (a.pid == b.pid)
            return a.start_valid < b.start_valid;
        else
            return a.pid < b.pid;
    });

    Vector<Process> processes;
    for (auto& it : all_processes)
        processes.append(move(it));

    return adopt_nonnull_own_or_enomem(new (nothrow) Profile(move(processes), move(events)));
}

void ProfileNode::sort_children()
{
    sort_profile_nodes(m_children);
}

void Profile::set_timestamp_filter_range(u64 start, u64 end)
{
    if (m_has_timestamp_filter_range && m_timestamp_filter_range_start == start && m_timestamp_filter_range_end == end)
        return;
    m_has_timestamp_filter_range = true;

    m_timestamp_filter_range_start = min(start, end);
    m_timestamp_filter_range_end = max(start, end);

    rebuild_tree();
    m_samples_model->invalidate();
    m_signposts_model->invalidate();
}

void Profile::clear_timestamp_filter_range()
{
    if (!m_has_timestamp_filter_range)
        return;
    m_has_timestamp_filter_range = false;
    rebuild_tree();
    m_samples_model->invalidate();
    m_signposts_model->invalidate();
}

void Profile::add_process_filter(pid_t pid, EventSerialNumber start_valid, EventSerialNumber end_valid)
{
    auto filter = ProcessFilter { pid, start_valid, end_valid };
    if (m_process_filters.contains_slow(filter))
        return;
    m_process_filters.append(move(filter));

    rebuild_tree();
    if (m_disassembly_model)
        m_disassembly_model->invalidate();
    m_samples_model->invalidate();
    m_signposts_model->invalidate();
}

void Profile::remove_process_filter(pid_t pid, EventSerialNumber start_valid, EventSerialNumber end_valid)
{
    auto filter = ProcessFilter { pid, start_valid, end_valid };
    if (!m_process_filters.contains_slow(filter))
        return;
    m_process_filters.remove_first_matching([&filter](ProcessFilter const& other_filter) {
        return other_filter == filter;
    });

    rebuild_tree();
    if (m_disassembly_model)
        m_disassembly_model->invalidate();
    m_samples_model->invalidate();
    m_signposts_model->invalidate();
}

void Profile::clear_process_filter()
{
    if (m_process_filters.is_empty())
        return;
    m_process_filters.clear();
    rebuild_tree();
    if (m_disassembly_model)
        m_disassembly_model->invalidate();
    m_samples_model->invalidate();
    m_signposts_model->invalidate();
}

bool Profile::process_filter_contains(pid_t pid, EventSerialNumber serial)
{
    if (!has_process_filter())
        return true;

    for (auto const& process_filter : m_process_filters)
        if (pid == process_filter.pid && serial >= process_filter.start_valid && serial <= process_filter.end_valid)
            return true;

    return false;
}

void Profile::set_inverted(bool inverted)
{
    if (m_inverted == inverted)
        return;
    m_inverted = inverted;
    rebuild_tree();
}

void Profile::set_show_top_functions(bool show)
{
    if (m_show_top_functions == show)
        return;
    m_show_top_functions = show;
    rebuild_tree();
}

void Profile::set_show_percentages(bool show_percentages)
{
    if (m_show_percentages == show_percentages)
        return;
    m_show_percentages = show_percentages;
}

void Profile::set_disassembly_index(const GUI::ModelIndex& index)
{
    if (m_disassembly_index == index)
        return;
    m_disassembly_index = index;
    auto* node = static_cast<ProfileNode*>(index.internal_data());
    if (!node)
        m_disassembly_model = nullptr;
    else
        m_disassembly_model = DisassemblyModel::create(*this, *node);
}

GUI::Model* Profile::disassembly_model()
{
    return m_disassembly_model;
}

ProfileNode::ProfileNode(Process const& process)
    : m_root(true)
    , m_process(process)
{
}

ProfileNode::ProfileNode(Process const& process, const String& object_name, String symbol, FlatPtr address, u32 offset, u64 timestamp, pid_t pid)
    : m_process(process)
    , m_symbol(move(symbol))
    , m_pid(pid)
    , m_address(address)
    , m_offset(offset)
    , m_timestamp(timestamp)
{
    String object;
    if (object_name.ends_with(": .text"sv)) {
        object = object_name.view().substring_view(0, object_name.length() - 7);
    } else {
        object = object_name;
    }
    m_object_name = LexicalPath::basename(object);
}

}
