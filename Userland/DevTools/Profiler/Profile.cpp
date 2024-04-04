/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Jakub Berkop <jakub.berkop@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Profile.h"
#include "DisassemblyModel.h"
#include "ProfileModel.h"
#include "SamplesModel.h"
#include "SourceModel.h"
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/RefPtr.h>
#include <AK/Try.h>
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
    , m_file_event_nodes(FileEventNode::create(""))
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
    m_file_event_model = FileEventModel::create(*this);

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
        auto const* process = find_process(pid, serial);
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
    m_file_event_nodes->children().clear();

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
            for_each_frame([&](Frame const& frame, bool is_innermost_frame) {
                auto const& object_name = frame.object_name;
                auto const& symbol = frame.symbol;
                auto const& address = frame.address;
                auto const& offset = frame.offset;

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
        if (event.data.has<Event::FilesystemEventData>()) {
            auto const& filesystem_event = event.data.get<Event::FilesystemEventData>();
            auto const& path = filesystem_event.data.visit(
                [&](Event::OpenEventData const& data) {
                    return data.path;
                },
                [&](Event::CloseEventData const& data) {
                    return data.path;
                },
                [&](Event::ReadvEventData const& data) {
                    return data.path;
                },
                [&](Event::ReadEventData const& data) {
                    return data.path;
                },
                [&](Event::PreadEventData const& data) {
                    return data.path;
                });

            auto& event_node = m_file_event_nodes->find_or_create_node(path);

            event_node.for_each_parent_node([&](FileEventNode& node) {
                auto const duration = filesystem_event.duration;

                filesystem_event.data.visit(
                    [&](Event::OpenEventData const&) {
                        node.open().duration += duration;
                        node.open().count++;
                    },
                    [&](Event::CloseEventData const&) {
                        node.close().duration += duration;
                        node.close().count++;
                    },
                    [&](Event::ReadvEventData const&) {
                        node.readv().duration += duration;
                        node.readv().count++;
                    },
                    [&](Event::ReadEventData const&) {
                        node.read().duration += duration;
                        node.read().count++;
                    },
                    [&](Event::PreadEventData const&) {
                        node.pread().duration += duration;
                        node.pread().count++;
                    });
            });
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
    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));

    auto json = JsonValue::from_string(TRY(file->read_until_eof()));
    if (json.is_error() || !json.value().is_object())
        return Error::from_string_literal("Invalid perfcore format (not a JSON object)");

    auto const& object = json.value().as_object();

    if (!g_kernel_debuginfo_object.has_value()) {
        auto debuginfo_file_or_error = Core::MappedFile::map("/boot/Kernel.debug"sv);
        if (!debuginfo_file_or_error.is_error()) {
            auto debuginfo_file = debuginfo_file_or_error.release_value();
            auto debuginfo_image = ELF::Image(debuginfo_file->bytes());
            g_kernel_debuginfo_object = { { move(debuginfo_file), move(debuginfo_image) } };
        }
    }

    auto strings_value = object.get_array("strings"sv);
    if (!strings_value.has_value())
        return Error::from_string_literal("Malformed profile (strings is not an array)");
    auto const& strings = strings_value.value();

    HashMap<FlatPtr, ByteString> profile_strings;
    for (FlatPtr string_id = 0; string_id < strings.size(); ++string_id) {
        auto const& value = strings.at(string_id);
        profile_strings.set(string_id, value.as_string());
    }

    auto events_value = object.get_array("events"sv);
    if (!events_value.has_value())
        return Error::from_string_literal("Malformed profile (events is not an array)");
    auto const& perf_events = events_value.value();

    Vector<NonnullOwnPtr<Process>> all_processes;
    HashMap<pid_t, Process*> current_processes;
    Vector<Event> events;
    EventSerialNumber next_serial;

    for (auto const& perf_event_value : perf_events.values()) {
        auto const& perf_event = perf_event_value.as_object();

        Event event;

        event.serial = next_serial;
        next_serial.increment();
        event.timestamp = perf_event.get_u64("timestamp"sv).value_or(0);
        event.lost_samples = perf_event.get_u32("lost_samples"sv).value_or(0);
        event.pid = perf_event.get_i32("pid"sv).value_or(0);
        event.tid = perf_event.get_i32("tid"sv).value_or(0);

        auto type_string = perf_event.get_byte_string("type"sv).value_or({});

        if (type_string == "sample"sv) {
            event.data = Event::SampleData {};
        } else if (type_string == "kmalloc"sv) {
            event.data = Event::MallocData {
                .ptr = perf_event.get_addr("ptr"sv).value_or(0),
                .size = perf_event.get_integer<size_t>("size"sv).value_or(0),
            };
        } else if (type_string == "kfree"sv) {
            event.data = Event::FreeData {
                .ptr = perf_event.get_addr("ptr"sv).value_or(0),
            };
        } else if (type_string == "signpost"sv) {
            auto string_id = perf_event.get_addr("arg1"sv).value_or(0);
            event.data = Event::SignpostData {
                .string = profile_strings.get(string_id).value_or(ByteString::formatted("Signpost #{}", string_id)),
                .arg = perf_event.get_addr("arg2"sv).value_or(0),
            };
        } else if (type_string == "mmap"sv) {
            auto ptr = perf_event.get_addr("ptr"sv).value_or(0);
            auto size = perf_event.get_integer<size_t>("size"sv).value_or(0);
            auto name = perf_event.get_byte_string("name"sv).value_or({});

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
                .ptr = perf_event.get_addr("ptr"sv).value_or(0),
                .size = perf_event.get_integer<size_t>("size"sv).value_or(0),
            };
            continue;
        } else if (type_string == "process_create"sv) {
            auto parent_pid = perf_event.get_integer<pid_t>("parent_pid"sv).value_or(0);
            auto executable = perf_event.get_byte_string("executable"sv).value_or({});
            event.data = Event::ProcessCreateData {
                .parent_pid = parent_pid,
                .executable = executable,
            };

            auto sampled_process = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Process {
                .pid = event.pid,
                .executable = executable,
                .basename = LexicalPath::basename(executable),
                .start_valid = event.serial,
                .end_valid = {},
            }));

            current_processes.set(sampled_process->pid, sampled_process);
            all_processes.append(move(sampled_process));
            continue;
        } else if (type_string == "process_exec"sv) {
            auto executable = perf_event.get_byte_string("executable"sv).value_or({});
            event.data = Event::ProcessExecData {
                .executable = executable,
            };

            auto* old_process = current_processes.get(event.pid).value();
            old_process->end_valid = event.serial;

            current_processes.remove(event.pid);

            auto sampled_process = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Process {
                .pid = event.pid,
                .executable = executable,
                .basename = LexicalPath::basename(executable),
                .start_valid = event.serial,
                .end_valid = {},
            }));

            current_processes.set(sampled_process->pid, sampled_process);
            all_processes.append(move(sampled_process));
            continue;
        } else if (type_string == "process_exit"sv) {
            auto* old_process = current_processes.get(event.pid).value();
            old_process->end_valid = event.serial;

            current_processes.remove(event.pid);
            continue;
        } else if (type_string == "thread_create"sv) {
            auto parent_tid = perf_event.get_integer<pid_t>("parent_tid"sv).value_or(0);
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
        } else if (type_string == "filesystem"sv) {
            Event::FilesystemEventData fsdata {
                .duration = Duration::from_nanoseconds(perf_event.get_integer<u64>("durationNs"sv).value_or(0)),
                .data = Event::OpenEventData {},
            };
            auto const filesystem_event_type = perf_event.get("fs_event_type"sv).value_or("").as_string();
            if (filesystem_event_type == "open"sv) {
                auto const string_index = perf_event.get_addr("filename_index"sv).value_or(0);
                auto const filename = profile_strings.get(string_index).value_or("");
                fsdata.data = Event::OpenEventData {
                    .dirfd = perf_event.get_integer<int>("dirfd"sv).value_or(0),
                    .path = filename,
                    .options = perf_event.get_integer<int>("options"sv).value_or(0),
                    .mode = perf_event.get_integer<u64>("mode"sv).value_or(0),
                };
            } else if (filesystem_event_type == "close"sv) {
                auto const string_index = perf_event.get_addr("filename_index"sv).value_or(0);
                auto const filename = profile_strings.get(string_index).value_or("");
                fsdata.data = Event::CloseEventData {
                    .fd = perf_event.get_integer<int>("fd"sv).value_or(0),
                    .path = filename,
                };
            } else if (filesystem_event_type == "readv"sv) {
                auto const string_index = perf_event.get_addr("filename_index"sv).value_or(0);
                auto const filename = profile_strings.get(string_index).value();
                fsdata.data = Event::ReadvEventData {
                    .fd = perf_event.get_integer<int>("fd"sv).value_or(0),
                    .path = filename,
                };
            } else if (filesystem_event_type == "read"sv) {
                auto const string_index = perf_event.get_addr("filename_index"sv).value_or(0);
                auto const filename = profile_strings.get(string_index).value();
                fsdata.data = Event::ReadEventData {
                    .fd = perf_event.get_integer<int>("fd"sv).value_or(0),
                    .path = filename,
                };
            } else if (filesystem_event_type == "pread"sv) {
                auto const string_index = perf_event.get_addr("filename_index"sv).value_or(0);
                auto const filename = profile_strings.get(string_index).value();
                fsdata.data = Event::PreadEventData {
                    .fd = perf_event.get_integer<int>("fd"sv).value_or(0),
                    .path = filename,
                    .buffer_ptr = perf_event.get_integer<FlatPtr>("buffer_ptr"sv).value_or(0),
                    .size = perf_event.get_integer<size_t>("size"sv).value_or(0),
                    .offset = perf_event.get_integer<off_t>("offset"sv).value_or(0),
                };
            }

            event.data = fsdata;
        } else {
            dbgln("Unknown event type '{}'", type_string);
            VERIFY_NOT_REACHED();
        }

        auto maybe_kernel_base = Symbolication::kernel_base();

        auto stack = perf_event.get_array("stack"sv);
        VERIFY(stack.has_value());
        auto const& stack_array = stack.value();
        for (ssize_t i = stack_array.values().size() - 1; i >= 0; --i) {
            auto const& frame = stack_array.at(i);
            auto ptr = frame.as_integer<u64>();
            u32 offset = 0;
            DeprecatedFlyString object_name;
            ByteString symbol;

            if (maybe_kernel_base.has_value() && ptr >= maybe_kernel_base.value()) {
                if (g_kernel_debuginfo_object.has_value()) {
                    symbol = g_kernel_debuginfo_object->elf.symbolicate(ptr - maybe_kernel_base.value(), &offset);
                } else {
                    symbol = ByteString::formatted("?? <{:p}>", ptr);
                }
            } else {
                auto it = current_processes.find(event.pid);
                // FIXME: This logic is kinda gnarly, find a way to clean it up.
                LibraryMetadata* library_metadata {};
                if (it != current_processes.end())
                    library_metadata = &it->value->library_metadata;
                if (auto const* library = library_metadata ? library_metadata->library_containing(ptr) : nullptr) {
                    object_name = library->name;
                    symbol = library->symbolicate(ptr, &offset);
                } else {
                    symbol = ByteString::formatted("?? <{:p}>", ptr);
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
        return Error::from_string_literal("No events captured (targeted process was never on CPU)");

    quick_sort(all_processes, [](auto& a, auto& b) {
        if (a->pid == b->pid)
            return a->start_valid < b->start_valid;

        return a->pid < b->pid;
    });

    Vector<Process> processes;
    for (auto& it : all_processes)
        processes.append(move(*it));

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

    return AK::any_of(m_process_filters,
        [&](auto const& process_filter) { return pid == process_filter.pid && serial >= process_filter.start_valid && serial <= process_filter.end_valid; });
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

void Profile::set_disassembly_index(GUI::ModelIndex const& index)
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

void Profile::set_source_index(GUI::ModelIndex const& index)
{
    if (m_source_index == index)
        return;
    m_source_index = index;
    auto* node = static_cast<ProfileNode*>(index.internal_data());
    if (!node)
        m_source_model = nullptr;
    else
        m_source_model = SourceModel::create(*this, *node);
}

GUI::Model* Profile::source_model()
{
    return m_source_model;
}

GUI::Model* Profile::file_event_model()
{
    return m_file_event_model;
}

ProfileNode::ProfileNode(Process const& process)
    : m_root(true)
    , m_process(process)
{
}

ProfileNode::ProfileNode(Process const& process, DeprecatedFlyString const& object_name, ByteString symbol, FlatPtr address, u32 offset, u64 timestamp, pid_t pid)
    : m_process(process)
    , m_symbol(move(symbol))
    , m_pid(pid)
    , m_address(address)
    , m_offset(offset)
    , m_timestamp(timestamp)
{
    ByteString object;
    if (object_name.ends_with(": .text"sv)) {
        object = object_name.view().substring_view(0, object_name.length() - 7);
    } else {
        object = object_name;
    }
    m_object_name = LexicalPath::basename(object);
}

}
