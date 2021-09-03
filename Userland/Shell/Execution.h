/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"
#include <YAK/Forward.h>
#include <YAK/NonnullRefPtrVector.h>
#include <YAK/String.h>
#include <YAK/Vector.h>
#include <LibCore/ElapsedTimer.h>

namespace Shell {

class FileDescriptionCollector {
public:
    FileDescriptionCollector() { }
    ~FileDescriptionCollector();

    void collect();
    void add(int fd);

private:
    Vector<int, 32> m_fds;
};

class SavedFileDescriptors {
public:
    SavedFileDescriptors(const NonnullRefPtrVector<AST::Rewiring>&);
    ~SavedFileDescriptors();

private:
    struct SavedFileDescriptor {
        int original { -1 };
        int saved { -1 };
    };

    Vector<SavedFileDescriptor> m_saves;
    FileDescriptionCollector m_collector;
};

}
