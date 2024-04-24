/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

#define _FUTEX_OP_SHIFT_OP 28
#define _FUTEX_OP_MASK_OP 0xf
#define _FUTEX_OP_SHIFT_CMP 24
#define _FUTEX_OP_MASK_CMP 0xf
#define _FUTEX_OP_SHIFT_OP_ARG 12
#define _FUTEX_OP_MASK_OP_ARG 0xfff
#define _FUTEX_OP_SHIFT_CMP_ARG 0
#define _FUTEX_OP_MASK_CMP_ARG 0xfff

#define FUTEX_OP(op, op_arg, cmp, cmp_arg) \
    ((((op) & _FUTEX_OP_MASK_OP) << _FUTEX_OP_SHIFT_OP) | (((cmp) & _FUTEX_OP_MASK_CMP) << _FUTEX_OP_SHIFT_CMP) | (((op_arg) & _FUTEX_OP_MASK_OP_ARG) << _FUTEX_OP_SHIFT_OP_ARG) | (((cmp_arg) & _FUTEX_OP_MASK_CMP_ARG) << _FUTEX_OP_SHIFT_CMP_ARG))

#define _FUTEX_OP(val3) (((val3) >> _FUTEX_OP_SHIFT_OP) & _FUTEX_OP_MASK_OP)
#define _FUTEX_CMP(val3) (((val3) >> _FUTEX_OP_SHIFT_CMP) & _FUTEX_OP_MASK_CMP)
#define _FUTEX_OP_ARG(val3) (((val3) >> _FUTEX_OP_SHIFT_OP_ARG) & _FUTEX_OP_MASK_OP_ARG)
#define _FUTEX_CMP_ARG(val3) (((val3) >> _FUTEX_OP_SHIFT_CMP_ARG) & _FUTEX_OP_MASK_CMP_ARG)

#define FUTEX_OP_SET 0
#define FUTEX_OP_ADD 1
#define FUTEX_OP_OR 2
#define FUTEX_OP_ANDN 3
#define FUTEX_OP_XOR 4
#define FUTEX_OP_ARG_SHIFT 8

#define FUTEX_OP_CMP_EQ 0
#define FUTEX_OP_CMP_NE 1
#define FUTEX_OP_CMP_LT 2
#define FUTEX_OP_CMP_LE 3
#define FUTEX_OP_CMP_GT 4
#define FUTEX_OP_CMP_GE 5

#define FUTEX_WAIT 1
#define FUTEX_WAKE 2

#define FUTEX_REQUEUE 3
#define FUTEX_CMP_REQUEUE 4
#define FUTEX_WAKE_OP 5
#define FUTEX_WAIT_BITSET 9
#define FUTEX_WAKE_BITSET 10

#define FUTEX_CLOCK_REALTIME (1 << 8)
#define FUTEX_PRIVATE_FLAG (1 << 9)
#define FUTEX_CMD_MASK ~(FUTEX_CLOCK_REALTIME | FUTEX_PRIVATE_FLAG)

#define FUTEX_BITSET_MATCH_ANY 0xffffffff

#ifdef __cplusplus
}
#endif
