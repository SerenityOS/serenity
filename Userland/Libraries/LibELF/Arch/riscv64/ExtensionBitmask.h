/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace ELF {

void __get_riscv_feature_bits(void* feature_bits, void* cpu_model);

}
