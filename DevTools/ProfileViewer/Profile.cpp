#include "Profile.h"
#include "ProfileModel.h"
#include <AK/QuickSort.h>
#include <LibCore/CFile.h>
#include <stdio.h>

Profile::Profile(const JsonArray& json, Vector<NonnullRefPtr<ProfileNode>>&& roots, u64 first_timestamp, u64 last_timestamp)
    : m_json(json)
    , m_roots(move(roots))
    , m_first_timestamp(first_timestamp)
    , m_last_timestamp(last_timestamp)
{
    m_model = ProfileModel::create(*this);
}

Profile::~Profile()
{
}

GModel& Profile::model()
{
    return *m_model;
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

    NonnullRefPtrVector<ProfileNode> roots;

    auto find_or_create_root = [&roots](const String& symbol, u32 address, u32 offset, u64 timestamp) -> ProfileNode& {
        for (int i = 0; i < roots.size(); ++i) {
            auto& root = roots[i];
            if (root.symbol() == symbol) {
                return root;
            }
        }
        auto new_root = ProfileNode::create(symbol, address, offset, timestamp);
        roots.append(new_root);
        return new_root;
    };

    u64 first_timestamp = samples.at(0).as_object().get("timestamp").to_number<u64>();
    u64 last_timestamp = samples.at(samples.size() - 1).as_object().get("timestamp").to_number<u64>();

    samples.for_each([&](const JsonValue& sample) {
        auto frames_value = sample.as_object().get("frames");
        auto& frames = frames_value.as_array();
        ProfileNode* node = nullptr;
        for (int i = frames.size() - 1; i >= 0; --i) {
            auto& frame = frames.at(i);

            auto symbol = frame.as_object().get("symbol").as_string_or({});
            auto address = frame.as_object().get("address").as_u32();
            auto offset = frame.as_object().get("offset").as_u32();
            auto timestamp = frame.as_object().get("timestamp").to_number<u64>();

            if (symbol.is_empty())
                break;

            if (!node)
                node = &find_or_create_root(symbol, address, offset, timestamp);
            else
                node = &node->find_or_create_child(symbol, address, offset, timestamp);

            node->increment_sample_count();
        }
    });

    for (auto& root : roots) {
        root.sort_children();
    }

    return NonnullOwnPtr<Profile>(NonnullOwnPtr<Profile>::Adopt, *new Profile(move(samples), move(roots), first_timestamp, last_timestamp));
}

void ProfileNode::sort_children()
{
    quick_sort(m_children.begin(), m_children.end(), [](auto& a, auto& b) {
        return a->sample_count() >= b->sample_count();
    });

    for (auto& child : m_children)
        child->sort_children();
}
