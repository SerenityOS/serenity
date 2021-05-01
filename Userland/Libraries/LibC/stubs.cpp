/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

extern "C" {

#define DO_STUB(name) \
    void name();      \
    void name() { }

DO_STUB(__register_frame_info);
DO_STUB(__deregister_frame_info);
DO_STUB(_ITM_registerTMCloneTable);
DO_STUB(_ITM_deregisterTMCloneTable);
}
