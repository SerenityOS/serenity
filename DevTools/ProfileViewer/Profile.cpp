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
        return a->sample_count() >= b->sample_count();
    });

    for (auto& child : nodes)
        child->sort_children();
}

Profile::Profile(const JsonArray& json)
    : m_json(json)
{
    m_first_timestamp = m_json.at(0).as_object().get("timestamp").to_number<u64>();
    m_last_timestamp = m_json.at(m_json.size() - 1).as_object().get("timestamp").to_number<u64>();

    m_model = ProfileModel::create(*this);

    m_samples.ensure_capacity(m_json.size());
    for (auto& sample_value : m_json.values()) {

        auto& sample_object = sample_value.as_object();

        Sample sample;
        sample.timestamp = sample_object.get("timestamp").to_number<u64>();
        sample.type = sample_object.get("type").to_string();

        if (sample.type == "malloc") {
            sample.ptr = sample_object.get("ptr").to_number<u32>();
            sample.size = sample_object.get("size").to_number<u32>();
        } else if (sample.type == "free") {
            sample.ptr = sample_object.get("ptr").to_number<u32>();
        }

        auto frames_value = sample_object.get("frames");
        auto& frames_array = frames_value.as_array();

        if (frames_array.size() < 2)
            continue;

        u32 innermost_frame_address = frames_array.at(1).as_object().get("address").to_number<u32>();
        sample.in_kernel = innermost_frame_address >= 0xc0000000;

        for (int i = frames_array.size() - 1; i >= 1; --i) {
            auto& frame_value = frames_array.at(i);
            auto& frame_object = frame_value.as_object();
            Frame frame;
            frame.symbol = frame_object.get("symbol").as_string_or({});
            frame.address = frame_object.get("address").as_u32();
            frame.offset = frame_object.get("offset").as_u32();
            sample.frames.append(move(frame));
        };

        m_deepest_stack_depth = max((u32)frames_array.size(), m_deepest_stack_depth);

        m_samples.append(move(sample));
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

    for (auto& sample : m_samples) {
        if (has_timestamp_filter_range()) {
            auto timestamp = sample.timestamp;
            if (timestamp < m_timestamp_filter_range_start || timestamp > m_timestamp_filter_range_end)
                continue;
        }

        if (sample.type == "malloc")
            live_allocations.set(sample.ptr);
        else if (sample.type == "free")
            live_allocations.remove(sample.ptr);
    }

    for (auto& sample : m_samples) {
        if (has_timestamp_filter_range()) {
            auto timestamp = sample.timestamp;
            if (timestamp < m_timestamp_filter_range_start || timestamp > m_timestamp_filter_range_end)
                continue;
        }

        if (sample.type == "malloc" && !live_allocations.contains(sample.ptr))
            continue;

        if (sample.type == "free")
            continue;

        ProfileNode* node = nullptr;

        auto for_each_frame = [&]<typename Callback>(Callback callback)
        {
            if (!m_inverted) {
                for (size_t i = 0; i < sample.frames.size(); ++i) {
                    if (callback(sample.frames.at(i)) == IterationDecision::Break)
                        break;
                }
            } else {
                for (size_t i = sample.frames.size() - 1; i >= 0; --i) {
                    if (callback(sample.frames.at(i)) == IterationDecision::Break)
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
                node = &find_or_create_root(symbol, address, offset, sample.timestamp);
            else
                node = &node->find_or_create_child(symbol, address, offset, sample.timestamp);

            node->increment_sample_count();
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

    JsonArray profile_events;

    for (auto& perf_event_value : perf_events.values()) {
        auto& perf_event = perf_event_value.as_object();

        JsonObject object;
        object.set("timestamp", perf_event.get("timestamp"));
        object.set("type", perf_event.get("type"));
        object.set("ptr", perf_event.get("ptr"));
        object.set("size", perf_event.get("size"));

        JsonArray frames_array;
        auto stack_array = perf_event.get("stack").as_array();

        for (auto& frame : stack_array.values()) {
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

            JsonObject frame_object;
            frame_object.set("address", ptr);
            frame_object.set("symbol", symbol);
            frame_object.set("offset", offset);
            frames_array.append(move(frame_object));
        }

        object.set("frames", move(frames_array));
        profile_events.append(move(object));
    }

    return NonnullOwnPtr<Profile>(NonnullOwnPtr<Profile>::Adopt, *new Profile(move(profile_events)));
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
