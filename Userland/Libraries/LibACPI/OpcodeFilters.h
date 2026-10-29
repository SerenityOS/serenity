/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibACPI/Definitions.h>

namespace ACPI::AML {

bool namespace_modifier_object_filter(Opcode opcode);
bool named_object_filter(Opcode opcode);
bool object_filter(Opcode opcode);
bool statement_filter(Opcode opcode);
bool expression_filter(Opcode opcode);
bool const_object_filter(Opcode opcode);
bool computational_data_filter(Opcode opcode);
bool data_object_filter(Opcode opcode);
bool data_ref_object_filter(Opcode opcode);
bool package_element_filter(Opcode opcode);
bool term_filter(Opcode opcode);
bool term_arg_filter(Opcode opcode);
bool arg_obj_filter(Opcode opcode);
bool local_obj_filter(Opcode opcode);

bool is_namestring_start(u8 c);
bool is_lead_name_char(u8 c);
bool is_name_char(u8 c);

}
