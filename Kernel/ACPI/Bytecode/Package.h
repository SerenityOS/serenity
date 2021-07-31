/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Kernel::ACPI::Package {

struct DecodingResult {
    size_t package_size;
    size_t encoding_length;
};

DecodingResult parse_encoded_name_path_length(u8 first_byte, u8 possible_seg_count_byte);
DecodingResult parse_encoded_package_length(u8 first_byte, Vector<u8> other_bytes);

}
