#include "Profile.h"
#include "ProfileModel.h"
#include <AK/QuickSort.h>
#include <LibCore/CFile.h>
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

        auto frames_value = sample_object.get("frames");
        auto& frames_array = frames_value.as_array();

        if (frames_array.size() < 2)
            continue;

        sample.in_kernel = frames_array.at(1).as_object().get("address").to_number<u32>() < (8 * MB);

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

GModel& Profile::model()
{
    return *m_model;
}

void Profile::rebuild_tree()
{
    Vector<NonnullRefPtr<ProfileNode>> roots;

    auto find_or_create_root = [&roots](const String& symbol, u32 address, u32 offset, u64 timestamp) -> ProfileNode& {
        for (int i = 0; i < roots.size(); ++i) {
            auto& root = roots[i];
            if (root->symbol() == symbol) {
                return root;
            }
        }
        auto new_root = ProfileNode::create(symbol, address, offset, timestamp);
        roots.append(new_root);
        return new_root;
    };

    for (auto& sample : m_samples) {
        if (has_timestamp_filter_range()) {
            auto timestamp = sample.timestamp;
            if (timestamp < m_timestamp_filter_range_start || timestamp > m_timestamp_filter_range_end)
                continue;
        }

        ProfileNode* node = nullptr;

        auto for_each_frame = [&]<typename Callback>(Callback callback)
        {
            if (!m_inverted) {
                for (int i = 0; i < sample.frames.size(); ++i) {
                    if (callback(sample.frames.at(i)) == IterationDecision::Break)
                        break;
                }
            } else {
                for (int i = sample.frames.size() - 1; i >= 0; --i) {
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

OwnPtr<Profile> Profile::load_from_file(const StringView& path)
{
    auto file = CFile::construct(path);
    if (!file->open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Unable to open %s, error: %s\n", String(path).characters(), file->error_string());
        return nullptr;
    }

    auto json = JsonValue::from_string(file->read_all());
    if (!json.is_array()) {
        fprintf(stderr, "Invalid format (not a JSON array)\n");
        return nullptr;
    }

    auto& samples = json.as_array();
    if (samples.is_empty())
        return nullptr;

    return NonnullOwnPtr<Profile>(NonnullOwnPtr<Profile>::Adopt, *new Profile(move(samples)));
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
