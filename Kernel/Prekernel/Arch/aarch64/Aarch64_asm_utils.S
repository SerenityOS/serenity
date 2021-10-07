/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.global get_current_exception_level
get_current_exception_level:
  mrs x0, CurrentEL
  lsr x0, x0, #2
  and x0, x0, #0x3
  ret
