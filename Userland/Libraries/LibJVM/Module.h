#pragma once
#include <AK/FixedArray.h>

namespace JVM {

struct Requirement {
    short requires_index;
    short requires_flags;
    short requires_version_index;
};

struct Export {
    short exports_index;
    short exports_flags;
    AK::FixedArray<short> exports_to_index;
};

struct Open {
    short opens_index;
    short opens_flags;
    AK::FixedArray<short> opens_to_index;
};

struct Provide {
    short provides_index;
    AK::FixedArray<short> provides_with_index;
};

struct Module {
    short module_name_index;
    short module_flags;
    short module_version_index;

    AK::FixedArray<Requirement> requires;
    AK::FixedArray<Export> exports;
    AK::FixedArray<Open> opens;
    AK::FixedArray<short> uses_index;
    AK::FixedArray<Provide> provides;
};

}
