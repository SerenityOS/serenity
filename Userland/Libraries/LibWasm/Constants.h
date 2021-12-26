/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Wasm::Constants {

// Value
static constexpr auto i32_tag = 0x7f;
static constexpr auto i64_tag = 0x7e;
static constexpr auto f32_tag = 0x7d;
static constexpr auto f64_tag = 0x7c;
static constexpr auto function_reference_tag = 0x70;
static constexpr auto extern_reference_tag = 0x6f;

// Function
static constexpr auto function_signature_tag = 0x60;

// Global
static constexpr auto const_tag = 0x00;
static constexpr auto var_tag = 0x01;

// Block
static constexpr auto empty_block_tag = 0x40;

// Import section
static constexpr auto extern_function_tag = 0x00;
static constexpr auto extern_table_tag = 0x01;
static constexpr auto extern_memory_tag = 0x02;
static constexpr auto extern_global_tag = 0x03;

static constexpr auto page_size = 64 * KiB;

// Limits
static constexpr auto max_allowed_call_stack_depth = 1000;
static constexpr auto max_allowed_executed_instructions_per_call = 64 * 1024 * 1024;

}
