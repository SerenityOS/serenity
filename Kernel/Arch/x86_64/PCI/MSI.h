/*
 * Copyright (c) 2023, Pankaj R <dev@pankajraghav.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Address register
static constexpr u32 msi_address_base = 0xfee00000;
static constexpr u8 msi_destination_shift = 12;
static constexpr u32 msi_redirection_hint = 0x00000008;
static constexpr u32 msi_destination_mode_logical = 0x00000004;

// Data register
static constexpr u8 msi_data_vector_mask = 0xff;
static constexpr u32 msi_trigger_mode_level = 0x00008000;
static constexpr u32 msi_level_assert = 0x00004000;

// Vector control
static constexpr u32 msi_vector_control_mask = 0x1;
static constexpr u32 msi_vector_control_unmask = ~(0x1);
