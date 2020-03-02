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

#include "Profile.h"
#include "ProfileModel.h"
#include <AK/HashTable.h>
#include <AK/MappedFile.h>
#include <AK/QuickSort.h>
#include <LibCore/File.h>
#include <LibELF/ELFLoader.h>
#include <stdio.h>

static void sort_profile_nodes(Vector<NonnullRefPtr<ProfileNode>>& nodes)
{
    quick_sort(nodes.begin(), nodes.end(), [](auto& a, auto& b) {
        return a->event_count() >= b->event_count();
    });

    for (auto& child : nodes)
        child->sort_children();
}

Profile::Profile(Vector<Event> events)
    : m_events(move(events))
{
    m_first_timestamp = m_events.first().timestamp;
    m_last_timestamp = m_events.last().timestamp;

    m_model = ProfileModel::create(*this);

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

void Profile::rebuild_tree()
{
    Vector<NonnullRefPtr<ProfileNode>> roots;

    auto find_or_create_root = [&roots](const String& symbol, u32 address, u32 offset, u64 timestamp) -> ProfileNode& {
        for (size_t i = 0; i < roots.size(); ++i) {
            auto& root = roots[i];
            if (root->symbol() == symbol) {
                return root;
            }
        }
        auto new_root = ProfileNode::create(symbol, address, offset, timestamp);
        roots.append(new_root);
        return new_root;
    };

    HashTable<uintptr_t> live_allocations;

    for (auto& event : m_events) {
        if (has_timestamp_filter_range()) {
            auto timestamp = event.timestamp;
            if (timestamp < m_timestamp_filter_range_start || timestamp > m_timestamp_filter_range_end)
                continue;
        }

        if (event.type == "malloc")
            live_allocations.set(event.ptr);
        else if (event.type == "free")
            live_allocations.remove(event.ptr);
    }

    for (auto& event : m_events) {
        if (has_timestamp_filter_range()) {
            auto timestamp = event.timestamp;
            if (timestamp < m_timestamp_filter_range_start || timestamp > m_timestamp_filter_range_end)
                continue;
        }

        if (event.type == "malloc" && !live_allocations.contains(event.ptr))
            continue;

        if (event.type == "free")
            continue;

        ProfileNode* node = nullptr;

        auto for_each_frame = [&]<typename Callback>(Callback callback)
        {
            if (!m_inverted) {
                for (size_t i = 0; i < event.frames.size(); ++i) {
                    if (callback(event.frames.at(i)) == IterationDecision::Break)
                        break;
                }
            } else {
                for (ssize_t i = event.frames.size() - 1; i >= 0; --i) {
                    if (callback(event.frames.at(i)) == IterationDecision::Break)
                        break;
                }
            }
        };

        for_each_frame([&](const Frame& frame) {
            auto& symbol = frame.symbol;
            auto& address = frame.address;
            auto& offset = frame.offset;

            if (symbol.is_empty())
                return IterationDecision::Break;

            if (!node)
                node = &find_or_create_root(symbol, address, offset, event.timestamp);
            else
                node = &node->find_or_create_child(symbol, address, offset, event.timestamp);

            node->increment_event_count();
            return IterationDecision::Continue;
        });
    }

    sort_profile_nodes(roots);

    m_roots = move(roots);
    m_model->update();
}

OwnPtr<Profile> Profile::load_from_perfcore_file(const StringView& path)
{
    auto file = Core::File::construct(path);
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Unable to open %s, error: %s\n", String(path).characters(), file->error_string());
        return nullptr;
    }

    auto json = JsonValue::from_string(file->read_all());
    if (!json.is_object()) {
        fprintf(stderr, "Invalid perfcore format (not a JSON object)\n");
        return nullptr;
    }

    auto& object = json.as_object();
    auto executable_path = object.get("executable").to_string();

    MappedFile elf_file(executable_path);
    if (!elf_file.is_valid()) {
        fprintf(stderr, "Unable to open executable '%s' for symbolication.\n", executable_path.characters());
        return nullptr;
    }

    auto elf_loader = make<ELFLoader>(static_cast<const u8*>(elf_file.data()), elf_file.size());

    MappedFile kernel_elf_file("/boot/kernel");
    OwnPtr<ELFLoader> kernel_elf_loader;
    if (kernel_elf_file.is_valid())
        kernel_elf_loader = make<ELFLoader>(static_cast<const u8*>(kernel_elf_file.data()), kernel_elf_file.size());

    auto events_value = object.get("events");
    if (!events_value.is_array())
        return nullptr;

    auto& perf_events = events_value.as_array();
    if (perf_events.is_empty())
        return nullptr;

    Vector<Event> events;

    for (auto& perf_event_value : perf_events.values()) {
        auto& perf_event = perf_event_value.as_object();

        Event event;

        event.timestamp = perf_event.get("timestamp").to_number<u64>();
        event.type = perf_event.get("type").to_string();

        if (event.type == "malloc") {
            event.ptr = perf_event.get("ptr").to_number<uintptr_t>();
            event.size = perf_event.get("size").to_number<size_t>();
        } else if (event.type == "free") {
            event.ptr = perf_event.get("ptr").to_number<uintptr_t>();
        }

        auto stack_array = perf_event.get("stack").as_array();
        for (ssize_t i = stack_array.values().size() - 1; i >= 1; --i) {
            auto& frame = stack_array.at(i);
            auto ptr = frame.to_number<u32>();
            u32 offset = 0;
            String symbol;

            if (ptr >= 0xc0000000) {
                if (kernel_elf_loader) {
                    symbol = kernel_elf_loader->symbolicate(ptr, &offset);
                } else {
                    symbol = "??";
                }
            } else {
                symbol = elf_loader->symbolicate(ptr, &offset);
            }

            if (symbol == "??")
                symbol = String::format("??", ptr);

            event.frames.append({ symbol, ptr, offset });
        }

        if (event.frames.size() < 2)
            continue;

        uintptr_t innermost_frame_address = event.frames.at(1).address;
        event.in_kernel = innermost_frame_address >= 0xc0000000;

        events.append(move(event));
    }

    return NonnullOwnPtr<Profile>(NonnullOwnPtr<Profile>::Adopt, *new Profile(move(events)));
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
}

void Profile::clear_timestamp_filter_range()
{
    if (!m_has_timestamp_filter_range)
        return;
    m_has_timestamp_filter_range = false;
    rebuild_tree();
}

void Profile::set_inverted(bool inverted)
{
    if (m_inverted == inverted)
        return;
    m_inverted = inverted;
    rebuild_tree();
}
