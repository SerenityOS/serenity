/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>

namespace Kernel {

class FileBackedFileSystem : public FileSystem {
public:
    virtual ~FileBackedFileSystem() override;

    File& file() { return m_file_description->file(); }
    OpenFileDescription& file_description() { return *m_file_description; }
    const File& file() const { return m_file_description->file(); }
    OpenFileDescription& file_description() const { return *m_file_description; }

protected:
    explicit FileBackedFileSystem(OpenFileDescription&);

private:
    virtual bool is_file_backed() const override { return true; }

    mutable NonnullRefPtr<OpenFileDescription> m_file_description;
};

}
