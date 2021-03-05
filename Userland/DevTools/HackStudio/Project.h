/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "ProjectFile.h"
#include <AK/LexicalPath.h>
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <LibGUI/FileSystemModel.h>

namespace HackStudio {

class Project {
    AK_MAKE_NONCOPYABLE(Project);
    AK_MAKE_NONMOVABLE(Project);

public:
    ~Project();

    static OwnPtr<Project> open_with_root_path(const String& root_path);

    GUI::FileSystemModel& model() { return *m_model; }
    const GUI::FileSystemModel& model() const { return *m_model; }
    String name() const { return LexicalPath(m_root_path).basename(); }
    String root_path() const { return m_root_path; }

    NonnullRefPtr<ProjectFile> get_file(const String& path) const;

    void for_each_text_file(Function<void(const ProjectFile&)>) const;

private:
    explicit Project(const String& root_path);

    String to_absolute_path(const String&) const;

    RefPtr<GUI::FileSystemModel> m_model;
    mutable NonnullRefPtrVector<ProjectFile> m_files;

    String m_root_path;
};

}
