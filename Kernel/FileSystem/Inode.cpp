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

#include <AK/NonnullRefPtrVector.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/VM/SharedInodeVMObject.h>

namespace Kernel {

static SpinLock s_all_inodes_lock;
static AK::Singleton<InlineLinkedList<Inode>> s_list;

SpinLock<u32>& Inode::all_inodes_lock()
{
    return s_all_inodes_lock;
}

InlineLinkedList<Inode>& Inode::all_with_lock()
{
    VERIFY(s_all_inodes_lock.is_locked());

    return *s_list;
}

void Inode::sync()
{
    NonnullRefPtrVector<Inode, 32> inodes;
    {
        ScopedSpinLock all_inodes_lock(s_all_inodes_lock);
        for (auto& inode : all_with_lock()) {
            if (inode.is_metadata_dirty())
                inodes.append(inode);
        }
    }

    for (auto& inode : inodes) {
        VERIFY(inode.is_metadata_dirty());
        inode.flush_metadata();
    }
}

KResultOr<NonnullOwnPtr<KBuffer>> Inode::read_entire(FileDescription* description) const
{
    KBufferBuilder builder;

    ssize_t nread;
    u8 buffer[4096];
    off_t offset = 0;
    for (;;) {
        auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);
        nread = read_bytes(offset, sizeof(buffer), buf, description);
        if (nread < 0)
            return KResult((ErrnoCode)-nread);
        VERIFY(nread <= (ssize_t)sizeof(buffer));
        if (nread <= 0)
            break;
        builder.append((const char*)buffer, nread);
        offset += nread;
        if (nread < (ssize_t)sizeof(buffer))
            break;
    }
    if (nread < 0) {
        klog() << "Inode::read_entire: ERROR: " << nread;
        return KResult((ErrnoCode)-nread);
    }

    auto entire_file = builder.build();
    if (!entire_file)
        return ENOMEM;
    return entire_file.release_nonnull();
}

KResultOr<NonnullRefPtr<Custody>> Inode::resolve_as_link(Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level) const
{
    // The default implementation simply treats the stored
    // contents as a path and resolves that. That is, it
    // behaves exactly how you would expect a symlink to work.
    auto contents_or = read_entire();
    if (contents_or.is_error())
        return contents_or.error();

    auto& contents = contents_or.value();
    auto path = StringView(contents->data(), contents->size());
    return VFS::the().resolve_path(path, base, out_parent, options, symlink_recursion_level);
}

Inode::Inode(FS& fs, InodeIndex index)
    : m_fs(fs)
    , m_index(index)
{
    ScopedSpinLock all_inodes_lock(s_all_inodes_lock);
    all_with_lock().append(this);
}

Inode::~Inode()
{
    ScopedSpinLock all_inodes_lock(s_all_inodes_lock);
    all_with_lock().remove(this);
}

void Inode::will_be_destroyed()
{
    LOCKER(m_lock);
    if (m_metadata_dirty)
        flush_metadata();
}

int Inode::set_atime(time_t)
{
    return -ENOTIMPL;
}

int Inode::set_ctime(time_t)
{
    return -ENOTIMPL;
}

int Inode::set_mtime(time_t)
{
    return -ENOTIMPL;
}

KResult Inode::increment_link_count()
{
    return ENOTIMPL;
}

KResult Inode::decrement_link_count()
{
    return ENOTIMPL;
}

void Inode::set_shared_vmobject(SharedInodeVMObject& vmobject)
{
    LOCKER(m_lock);
    m_shared_vmobject = vmobject;
}

bool Inode::bind_socket(LocalSocket& socket)
{
    LOCKER(m_lock);
    if (m_socket)
        return false;
    m_socket = socket;
    return true;
}

bool Inode::unbind_socket()
{
    LOCKER(m_lock);
    if (!m_socket)
        return false;
    m_socket = nullptr;
    return true;
}

void Inode::register_watcher(Badge<InodeWatcher>, InodeWatcher& watcher)
{
    LOCKER(m_lock);
    VERIFY(!m_watchers.contains(&watcher));
    m_watchers.set(&watcher);
}

void Inode::unregister_watcher(Badge<InodeWatcher>, InodeWatcher& watcher)
{
    LOCKER(m_lock);
    VERIFY(m_watchers.contains(&watcher));
    m_watchers.remove(&watcher);
}

NonnullRefPtr<FIFO> Inode::fifo()
{
    LOCKER(m_lock);
    VERIFY(metadata().is_fifo());

    // FIXME: Release m_fifo when it is closed by all readers and writers
    if (!m_fifo)
        m_fifo = FIFO::create(metadata().uid);

    VERIFY(m_fifo);
    return *m_fifo;
}

void Inode::set_metadata_dirty(bool metadata_dirty)
{
    LOCKER(m_lock);

    if (metadata_dirty) {
        // Sanity check.
        VERIFY(!fs().is_readonly());
    }

    if (m_metadata_dirty == metadata_dirty)
        return;

    m_metadata_dirty = metadata_dirty;
    if (m_metadata_dirty) {
        // FIXME: Maybe we should hook into modification events somewhere else, I'm not sure where.
        //        We don't always end up on this particular code path, for instance when writing to an ext2fs file.
        for (auto& watcher : m_watchers) {
            watcher->notify_inode_event({}, InodeWatcherEvent::Type::Modified);
        }
    }
}

void Inode::did_add_child(const InodeIdentifier& child_id)
{
    LOCKER(m_lock);
    for (auto& watcher : m_watchers) {
        watcher->notify_child_added({}, child_id);
    }
}

void Inode::did_remove_child(const InodeIdentifier& child_id)
{
    LOCKER(m_lock);
    for (auto& watcher : m_watchers) {
        watcher->notify_child_removed({}, child_id);
    }
}

KResult Inode::prepare_to_write_data()
{
    // FIXME: It's a poor design that filesystems are expected to call this before writing out data.
    //        We should funnel everything through an interface at the VFS layer so this can happen from a single place.
    LOCKER(m_lock);
    if (fs().is_readonly())
        return EROFS;
    auto metadata = this->metadata();
    if (metadata.is_setuid() || metadata.is_setgid()) {
        dbgln("Inode::prepare_to_write_data(): Stripping SUID/SGID bits from {}", identifier());
        return chmod(metadata.mode & ~(04000 | 02000));
    }
    return KSuccess;
}

RefPtr<SharedInodeVMObject> Inode::shared_vmobject() const
{
    LOCKER(m_lock);
    return m_shared_vmobject.strong_ref();
}

bool Inode::is_shared_vmobject(const SharedInodeVMObject& other) const
{
    LOCKER(m_lock);
    return m_shared_vmobject.unsafe_ptr() == &other;
}

}
