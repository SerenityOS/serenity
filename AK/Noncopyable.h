/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_MAKE_NONCOPYABLE(c) \
private:                       \
    c(c const&) = delete;      \
    c& operator=(c const&) = delete

#define AK_MAKE_NONMOVABLE(c) \
private:                      \
    c(c&&) = delete;          \
    c& operator=(c&&) = delete
