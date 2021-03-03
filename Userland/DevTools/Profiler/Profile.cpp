/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include "Profile.h"
#include "DisassemblyModel.h"
#include "ProfileModel.h"
#include "SamplesModel.h"
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/QuickSort.h>
#include <AK/RefPtr.h>
#include <LibCore/File.h>
#include <LibELF/Image.h>
#include <sys/stat.h>

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
    m_first_timestamp = m_events.first().timestamp;
    m_last_timestamp = m_events.last().timestamp;

    m_model = ProfileModel::create(*this);
    m_samples_model = SamplesModel::create(*this);

    for (auto& event : m_events) {
        m_deepest_stack_depth = max((u32)event.frames.size(), m_deepest_stack_depth);
    }

    rebuild_tree();
}

Profile::~Profile()
{
}

GUI::Model& Profile::model()
{
    return *m_model;
}

GUI::Model& Profile::samples_model()
{
    return *m_samples_model;
}

void Profile::rebuild_tree()
{
    u32 filtered_event_count = 0;
    Vector<NonnullRefPtr<ProfileNode>> roots;

    auto find_or_create_root = [&roots](FlyString object_name, String symbol, u32 address, u32 offset, u64 timestamp, pid_t pid) -> ProfileNode& {
        for (size_t i = 0; i < roots.size(); ++i) {
            auto& root = roots[i];
            if (root->symbol() == symbol) {
                return root;
            }
        }
        auto new_root = ProfileNode::create(move(object_name), move(symbol), address, offset, timestamp, pid);
        roots.append(new_root);
        return new_root;
    };

    HashTable<FlatPtr> live_allocations;

    for_each_event_in_filter_range([&](auto& event) {
        if (event.type == "malloc")
            live_allocations.set(event.ptr);
        else if (event.type == "free")
            live_allocations.remove(event.ptr);
    });

    Optional<size_t> first_filtered_event_index;

    for (size_t event_index = 0; event_index < m_events.size(); ++event_index) {
        auto& event = m_events.at(event_index);
        if (has_timestamp_filter_range()) {
            auto timestamp = event.timestamp;
            if (timestamp < m_timestamp_filter_range_start || timestamp > m_timestamp_filter_range_end)
                continue;
        }

        if (!first_filtered_event_index.has_value())
            first_filtered_event_index = event_index;

        if (event.type == "malloc" && !live_allocations.contains(event.ptr))
            continue;

        if (event.type == "free")
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
            for_each_frame([&](const Frame& frame, bool is_innermost_frame) {
                auto& object_name = frame.object_name;
                auto& symbol = frame.symbol;
                auto& address = frame.address;
                auto& offset = frame.offset;

                if (symbol.is_empty())
                    return IterationDecision::Break;

                // FIXME: More cheating with intentional mixing of TID/PID here:
                if (!node)
                    node = &find_or_create_root(object_name, symbol, address, offset, event.timestamp, event.tid);
                else
                    node = &node->find_or_create_child(object_name, symbol, address, offset, event.timestamp, event.tid);

                node->increment_event_count();
                if (is_innermost_frame) {
                    node->add_event_address(address);
                    node->increment_self_count();
                }
                return IterationDecision::Continue;
            });
        } else {
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
                        node = &find_or_create_root(object_name, symbol, address, offset, event.timestamp, event.tid);
                        root = node;
                        root->will_track_seen_events(m_events.size());
                    } else {
                        node = &node->find_or_create_child(object_name, symbol, address, offset, event.timestamp, event.tid);
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

        ++filtered_event_count;
    }

    sort_profile_nodes(roots);

    m_filtered_event_count = filtered_event_count;
    m_first_filtered_event_index = first_filtered_event_index.value_or(0);
    m_roots = move(roots);
    m_model->update();
}

Result<NonnullOwnPtr<Profile>, String> Profile::load_from_perfcore_file(const StringView& path)
{
    auto file = Core::File::construct(path);
    if (!file->open(Core::IODevice::ReadOnly))
        return String::formatted("Unable to open {}, error: {}", path, file->error_string());

    auto json = JsonValue::from_string(file->read_all());
    if (!json.has_value() || !json.value().is_object())
        return String { "Invalid perfcore format (not a JSON object)" };

    auto& object = json.value().as_object();

    auto processes_value = object.get("processes");
    if (processes_value.is_null())
        return String { "Invalid perfcore format (no processes)" };

    if (!processes_value.is_array())
        return String { "Invalid perfcore format (processes is not an array)" };

    Vector<Process> sampled_processes;

    for (auto& process_value : processes_value.as_array().values()) {
        if (!process_value.is_object())
            return String { "Invalid perfcore format (process value is not an object)" };
        auto& process = process_value.as_object();
        auto regions_value = process.get("regions");
        if (!regions_value.is_array())
            return String { "Invalid perfcore format (regions is not an array)" };

        Process sampled_process {
            .pid = (pid_t)process.get("pid").to_i32(),
            .executable = process.get("executable").to_string(),
            .threads = {},
            .regions = {},
            .library_metadata = make<LibraryMetadata>(regions_value.as_array()),
        };

        for (auto& region_value : regions_value.as_array().values()) {
            if (!region_value.is_object())
                return String { "Invalid perfcore format (region is not an object)" };
            auto& region = region_value.as_object();
            sampled_process.regions.append(Process::Region {
                .name = region.get("name").to_string(),
                .base = region.get("base").to_u32(),
                .size = region.get("size").to_u32(),
            });
        }

        sampled_processes.append(move(sampled_process));
    }

    auto file_or_error = MappedFile::map("/boot/Kernel");
    OwnPtr<ELF::Image> kernel_elf;
    if (!file_or_error.is_error())
        kernel_elf = make<ELF::Image>(file_or_error.value()->bytes());

    auto events_value = object.get("events");
    if (!events_value.is_array())
        return String { "Malformed profile (events is not an array)" };

    auto& perf_events = events_value.as_array();
    if (perf_events.is_empty())
        return String { "No events captured (targeted process was never on CPU)" };

    Vector<Event> events;

    for (auto& perf_event_value : perf_events.values()) {
        auto& perf_event = perf_event_value.as_object();

        Event event;

        event.timestamp = perf_event.get("timestamp").to_number<u64>();
        event.type = perf_event.get("type").to_string();
        event.tid = perf_event.get("tid").to_i32();

        if (event.type == "malloc") {
            event.ptr = perf_event.get("ptr").to_number<FlatPtr>();
            event.size = perf_event.get("size").to_number<size_t>();
        } else if (event.type == "free") {
            event.ptr = perf_event.get("ptr").to_number<FlatPtr>();
        }

        auto stack_array = perf_event.get("stack").as_array();
        for (ssize_t i = stack_array.values().size() - 1; i >= 0; --i) {
            auto& frame = stack_array.at(i);
            auto ptr = frame.to_number<u32>();
            u32 offset = 0;
            FlyString object_name;
            String symbol;

            if (ptr >= 0xc0000000) {
                if (kernel_elf) {
                    symbol = kernel_elf->symbolicate(ptr, &offset);
                } else {
                    symbol = "??";
                }
            } else {
                auto it = sampled_processes.find_if([&](auto& entry) {
                    // FIXME: This doesn't support multi-threaded programs!
                    return entry.pid == event.tid;
                });
                // FIXME: This logic is kinda gnarly, find a way to clean it up.
                LibraryMetadata* library_metadata {};
                if (!it.is_end())
                    library_metadata = it->library_metadata.ptr();
                if (auto* library = library_metadata ? library_metadata->library_containing(ptr) : nullptr) {
                    object_name = library->name;
                    if (library->object) {
                        symbol = library->object->elf.symbolicate(ptr - library->base, &offset);
                    } else {
                        symbol = "??";
                    }
                } else {
                    symbol = "??";
                }
            }

            event.frames.append({ object_name, symbol, ptr, offset });
        }

        if (event.frames.size() < 2)
            continue;

        FlatPtr innermost_frame_address = event.frames.at(1).address;
        event.in_kernel = innermost_frame_address >= 0xc0000000;

        events.append(move(event));
    }

    return adopt_own(*new Profile(move(sampled_processes), move(events)));
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
    m_samples_model->update();
}

void Profile::clear_timestamp_filter_range()
{
    if (!m_has_timestamp_filter_range)
        return;
    m_has_timestamp_filter_range = false;
    rebuild_tree();
    m_samples_model->update();
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
    m_disassembly_model = DisassemblyModel::create(*this, *node);
}

GUI::Model* Profile::disassembly_model()
{
    return m_disassembly_model;
}

HashMap<String, OwnPtr<MappedObject>> g_mapped_object_cache;

static MappedObject* get_or_create_mapped_object(const String& path)
{
    if (auto it = g_mapped_object_cache.find(path); it != g_mapped_object_cache.end())
        return it->value.ptr();

    auto file_or_error = MappedFile::map(path);
    if (file_or_error.is_error()) {
        g_mapped_object_cache.set(path, {});
        return nullptr;
    }
    auto elf = ELF::Image(file_or_error.value()->bytes());
    if (!elf.is_valid()) {
        g_mapped_object_cache.set(path, {});
        return nullptr;
    }
    auto new_mapped_object = adopt_own(*new MappedObject {
        .file = file_or_error.release_value(),
        .elf = move(elf),
    });
    auto* ptr = new_mapped_object.ptr();
    g_mapped_object_cache.set(path, move(new_mapped_object));
    return ptr;
}

LibraryMetadata::LibraryMetadata(JsonArray regions)
    : m_regions(move(regions))
{
    for (auto& region_value : m_regions.values()) {
        auto& region = region_value.as_object();
        auto base = region.get("base").as_u32();
        auto size = region.get("size").as_u32();
        auto name = region.get("name").as_string();

        String path;
        if (name.contains("Loader.so"))
            path = "Loader.so";
        else if (!name.contains(":"))
            continue;
        else
            path = name.substring(0, name.view().find_first_of(":").value());

        if (name.contains(".so"))
            path = String::formatted("/usr/lib/{}", path);

        auto* mapped_object = get_or_create_mapped_object(path);
        if (!mapped_object)
            continue;

        m_libraries.set(name, adopt_own(*new Library { base, size, name, mapped_object }));
    }
}

const LibraryMetadata::Library* LibraryMetadata::library_containing(FlatPtr ptr) const
{
    for (auto& it : m_libraries) {
        if (!it.value)
            continue;
        auto& library = *it.value;
        if (ptr >= library.base && ptr < (library.base + library.size))
            return &library;
    }
    return nullptr;
}

ProfileNode::ProfileNode(const String& object_name, String symbol, u32 address, u32 offset, u64 timestamp, pid_t pid)
    : m_symbol(move(symbol))
    , m_pid(pid)
    , m_address(address)
    , m_offset(offset)
    , m_timestamp(timestamp)
{
    String object;
    if (object_name.ends_with(": .text")) {
        object = object_name.view().substring_view(0, object_name.length() - 7);
    } else {
        object = object_name;
    }
    m_object_name = LexicalPath(object).basename();
}

const Process* ProfileNode::process(Profile& profile) const
{
    return profile.find_process(m_pid);
}
