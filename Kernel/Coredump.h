/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <Kernel/Forward.h>

namespace Kernel {

class Coredump {
public:
    static KResultOr<NonnullOwnPtr<Coredump>> try_create(NonnullRefPtr<Process>, StringView output_path);

    ~Coredump() = default;
    [[nodiscard]] KResult write();

private:
    Coredump(NonnullRefPtr<Process>, NonnullRefPtr<FileDescription>);
    static KResultOr<NonnullRefPtr<FileDescription>> try_create_target_file(Process const&, StringView output_path);

    [[nodiscard]] KResult write_elf_header();
    [[nodiscard]] KResult write_program_headers(size_t notes_size);
    [[nodiscard]] KResult write_regions();
    [[nodiscard]] KResult write_notes_segment(ByteBuffer&);

    KResultOr<ByteBuffer> create_notes_segment_data() const;
    KResultOr<ByteBuffer> create_notes_process_data() const;
    KResultOr<ByteBuffer> create_notes_threads_data() const;
    KResultOr<ByteBuffer> create_notes_regions_data() const;
    KResultOr<ByteBuffer> create_notes_metadata_data() const;

    NonnullRefPtr<Process> m_process;
    NonnullRefPtr<FileDescription> m_description;
    const size_t m_num_program_headers;
};

}
