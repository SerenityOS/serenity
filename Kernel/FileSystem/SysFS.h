/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/Singleton.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/TTY/SlavePTY.h>

namespace Kernel {

class SysFS;
class SysFSInode;
class SysFSDirectoryInode;

class SysFSExposedComponent : public RefCounted<SysFSExposedComponent> {
public:
    virtual KResultOr<size_t> entries_count() const { VERIFY_NOT_REACHED(); };
    virtual String name() const { return m_name; };
    virtual ssize_t read_bytes(off_t, ssize_t, UserOrKernelBuffer&, FileDescription*) const { VERIFY_NOT_REACHED(); }
    virtual KResult traverse_as_directory(const SysFS&, Function<bool(const FS::DirectoryEntryView&)>) const { VERIFY_NOT_REACHED(); };
    virtual RefPtr<SysFSExposedComponent> lookup(StringView) { VERIFY_NOT_REACHED(); };
    virtual ssize_t write_bytes(off_t, ssize_t, const UserOrKernelBuffer&, FileDescription*) { return -EROFS; }
    virtual size_t size() const { return 0; }

    virtual NonnullRefPtr<SysFSInode> to_inode(const SysFS&) const;

    size_t component_index() const { return m_component_index; };

    virtual ~SysFSExposedComponent() = default;

protected:
    UNMAP_AFTER_INIT explicit SysFSExposedComponent(String name);

private:
    String m_name;
    size_t m_component_index;
};

class SysFSExposedFolder : public SysFSExposedComponent {
public:
    virtual KResultOr<size_t> entries_count() const override { return m_components.size(); };
    virtual KResult traverse_as_directory(const SysFS&, Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<SysFSExposedComponent> lookup(StringView name) override;

    virtual NonnullRefPtr<SysFSInode> to_inode(const SysFS&) const override;
    UNMAP_AFTER_INIT void add_component(SysFSExposedComponent&);

protected:
    UNMAP_AFTER_INIT explicit SysFSExposedFolder(String name);
    SysFSExposedFolder(String name, SysFSExposedFolder& parent_folder);
    NonnullRefPtrVector<SysFSExposedComponent> m_components;
    RefPtr<SysFSExposedFolder> m_parent_folder;
};

class SysFSRootFolder final : public SysFSExposedFolder {
    friend class SystemRegistrar;

public:
    UNMAP_AFTER_INIT static NonnullRefPtr<SysFSRootFolder> create();
    virtual KResult traverse_as_directory(const SysFS&, Function<bool(const FS::DirectoryEntryView&)>) const override;

private:
    UNMAP_AFTER_INIT SysFSRootFolder();
};

class SystemRegistrar {
    friend class SysFS;
    friend class SysFSExposedComponent;
    friend class SysFSExposedFolder;
    friend class SysFSRootFolder;

public:
    static SystemRegistrar& the();

    UNMAP_AFTER_INIT static void initialize();

    UNMAP_AFTER_INIT SystemRegistrar();
    UNMAP_AFTER_INIT void register_new_component(SysFSExposedComponent&);

    NonnullRefPtr<SysFSExposedFolder> root_folder() { return m_root_folder; }

private:
    RefPtr<SysFSExposedComponent> get_component_by_index(InodeIndex index) const;
    //static size_t allocate_inode_index();

    Lock m_lock;
    NonnullRefPtr<SysFSRootFolder> m_root_folder;
    NonnullRefPtrVector<SysFSExposedComponent> m_sysfs_components;
};

class SysFS final : public FS {
    friend class SysFSInode;
    friend class SysFSDirectoryInode;

public:
    virtual ~SysFS() override;
    static NonnullRefPtr<SysFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override { return "SysFS"; }

    virtual NonnullRefPtr<Inode> root_inode() const override;

private:
    SysFS();
    RefPtr<Inode> get_inode(InodeIdentifier) const;

    NonnullRefPtr<SysFSInode> m_root_inode;
};

class SysFSInode : public Inode {
    friend class SysFS;
    friend class SysFSDirectoryInode;

public:
    static NonnullRefPtr<SysFSInode> create(const SysFS&, const SysFSExposedComponent&);
    String name() const { return m_associated_component->name(); };

protected:
    SysFSInode(const SysFS&, const SysFSExposedComponent&);
    virtual ssize_t read_bytes(off_t, ssize_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual KResult traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual InodeMetadata metadata() const override;
    virtual ssize_t write_bytes(off_t, ssize_t, const UserOrKernelBuffer& buffer, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(const String& name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(u64) override;

    NonnullRefPtr<SysFSExposedComponent> m_associated_component;
};

class SysFSDirectoryInode : public SysFSInode {
    friend class SysFS;
    friend class SysFSRootDirectoryInode;

public:
    static NonnullRefPtr<SysFSDirectoryInode> create(const SysFS&, const SysFSExposedComponent&);
    virtual ~SysFSDirectoryInode() override;

protected:
    SysFSDirectoryInode(const SysFS&, const SysFSExposedComponent&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;

    SysFS& m_parent_fs;
};

}
