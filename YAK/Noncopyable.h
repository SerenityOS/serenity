/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define YAK_MAKE_NONCOPYABLE(c) \
private:                       \
    c(const c&) = delete;      \
    c& operator=(const c&) = delete

#define YAK_MAKE_NONMOVABLE(c) \
private:                      \
    c(c&&) = delete;          \
    c& operator=(c&&) = delete
