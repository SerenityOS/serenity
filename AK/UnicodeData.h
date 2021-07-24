
#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>

namespace AK {

struct UnicodeData {
    u32 code_point;
    u32 simple_uppercase_mapping;
    u32 simple_lowercase_mapping;
};

Optional<UnicodeData> unicode_data_for_code_point(u32 code_point);

}
