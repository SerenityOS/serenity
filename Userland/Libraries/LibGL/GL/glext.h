/*
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGL/GL/glplatform.h>

#define GL_GLEXT_VERSION 20211115

typedef void(APIENTRYP PFNGLLOCKARRAYSEXTPROC)(GLint first, GLsizei count);
typedef void(APIENTRYP PFNGLUNLOCKARRAYSEXTPROC)(void);
