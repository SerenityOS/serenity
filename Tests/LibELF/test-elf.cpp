/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibFileSystem/FileSystem.h>
#include <LibTest/TestCase.h>
#include <elf.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

TEST_CASE(test_interp_header_tiny_p_filesz)
{
    char buffer[0x2000];

    auto& header = *(Elf32_Ehdr*)buffer;
    header.e_ident[EI_MAG0] = ELFMAG0;
    header.e_ident[EI_MAG1] = ELFMAG1;
    header.e_ident[EI_MAG2] = ELFMAG2;
    header.e_ident[EI_MAG3] = ELFMAG3;
    header.e_ident[EI_CLASS] = ELFCLASS32;
    header.e_ident[EI_DATA] = ELFDATA2LSB;
    header.e_ident[EI_VERSION] = EV_CURRENT;
    header.e_ident[EI_OSABI] = ELFOSABI_SYSV;
    header.e_ident[EI_ABIVERSION] = 0;
    header.e_type = ET_REL;
    header.e_version = EV_CURRENT;
    header.e_ehsize = sizeof(Elf32_Ehdr);
    header.e_machine = EM_386;
    header.e_shentsize = sizeof(Elf32_Shdr);
    header.e_phnum = 1;
    header.e_phoff = 52;                     // inaccurate
    header.e_phentsize = sizeof(Elf32_Phdr); // inaccurate
    header.e_shnum = 3;                      // inaccurate
    header.e_shoff = 1024;                   // inaccurate
    header.e_shstrndx = 2;                   // inaccurate
    header.e_entry = 1024;                   // inaccurate

    auto* ph = (Elf32_Phdr*)(&buffer[header.e_phoff]);
    ph[0].p_flags = PF_R | PF_X;
    ph[0].p_vaddr = 0x00d4;
    ph[0].p_align = PAGE_SIZE;
    ph[0].p_type = PT_INTERP;
    ph[0].p_memsz = 0xffff0000;
    ph[0].p_offset = 0x100;

    // p_filesz (1 or less) to trigger crash
    ph[0].p_filesz = 1;

    char path[] = "/tmp/test-elf.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT_NE(fd, -1);
    EXPECT_EQ(fchmod(fd, 0700), 0);

    int nwritten = write(fd, buffer, sizeof(buffer));
    EXPECT(nwritten);

    auto elf_path = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
    EXPECT(elf_path.characters());

    int rc = execl(elf_path.characters(), "test-elf", nullptr);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, 8);

    EXPECT_EQ(unlink(path), 0);
}

TEST_CASE(test_interp_header_p_filesz_larger_than_p_memsz)
{
    char buffer[0x2000];

    auto& header = *(Elf32_Ehdr*)buffer;
    header.e_ident[EI_MAG0] = ELFMAG0;
    header.e_ident[EI_MAG1] = ELFMAG1;
    header.e_ident[EI_MAG2] = ELFMAG2;
    header.e_ident[EI_MAG3] = ELFMAG3;
    header.e_ident[EI_CLASS] = ELFCLASS32;
    header.e_ident[EI_DATA] = ELFDATA2LSB;
    header.e_ident[EI_VERSION] = EV_CURRENT;
    header.e_ident[EI_OSABI] = ELFOSABI_SYSV;
    header.e_ident[EI_ABIVERSION] = 0;
    header.e_type = ET_REL;
    header.e_version = EV_CURRENT;
    header.e_ehsize = sizeof(Elf32_Ehdr);
    header.e_machine = EM_386;
    header.e_shentsize = sizeof(Elf32_Shdr);
    header.e_phnum = 1;
    header.e_phoff = 52;                     // inaccurate
    header.e_phentsize = sizeof(Elf32_Phdr); // inaccurate
    header.e_shnum = 3;                      // inaccurate
    header.e_shoff = 1024;                   // inaccurate
    header.e_shstrndx = 2;                   // inaccurate
    header.e_entry = 1024;                   // inaccurate

    auto* ph = (Elf32_Phdr*)(&buffer[header.e_phoff]);
    ph[0].p_flags = PF_R | PF_X;
    ph[0].p_vaddr = 0x00d4;
    ph[0].p_align = PAGE_SIZE;
    ph[0].p_type = PT_INTERP;
    ph[0].p_memsz = 0xffff0000;
    ph[0].p_offset = 0x1000;
    ph[0].p_filesz = 0x1000;

    char path[] = "/tmp/test-elf.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT_NE(fd, -1);
    EXPECT_EQ(fchmod(fd, 0700), 0);

    int nwritten = write(fd, buffer, sizeof(buffer));
    EXPECT(nwritten);

    auto elf_path = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
    EXPECT(elf_path.characters());

    int rc = execl(elf_path.characters(), "test-elf", nullptr);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, 8);

    EXPECT_EQ(unlink(path), 0);
}

TEST_CASE(test_interp_header_p_filesz_plus_p_offset_overflow_p_memsz)
{
    char buffer[0x2000];

    auto& header = *(Elf32_Ehdr*)buffer;
    header.e_ident[EI_MAG0] = ELFMAG0;
    header.e_ident[EI_MAG1] = ELFMAG1;
    header.e_ident[EI_MAG2] = ELFMAG2;
    header.e_ident[EI_MAG3] = ELFMAG3;
    header.e_ident[EI_CLASS] = ELFCLASS32;
    header.e_ident[EI_DATA] = ELFDATA2LSB;
    header.e_ident[EI_VERSION] = EV_CURRENT;
    header.e_ident[EI_OSABI] = ELFOSABI_SYSV;
    header.e_ident[EI_ABIVERSION] = 0;
    header.e_type = ET_REL;
    header.e_version = EV_CURRENT;
    header.e_ehsize = sizeof(Elf32_Ehdr);
    header.e_machine = EM_386;
    header.e_shentsize = sizeof(Elf32_Shdr);
    header.e_phoff = 52;                     // inaccurate
    header.e_phentsize = sizeof(Elf32_Phdr); // inaccurate
    header.e_shnum = 3;                      // inaccurate
    header.e_shoff = 1024;                   // inaccurate
    header.e_shstrndx = 2;                   // inaccurate
    header.e_entry = 1024;                   // inaccurate

    auto* ph = (Elf32_Phdr*)(&buffer[header.e_phoff]);
    ph[0].p_flags = PF_R | PF_X;
    ph[0].p_vaddr = 0x00d4;
    ph[0].p_align = PAGE_SIZE;
    ph[0].p_type = PT_INTERP;

    // p_memsz must be of sufficient size to hold maxint - 0x1000
    ph[0].p_memsz = 0xfffff000;

    // p_offset + p_filesz must not exceed buffer size in order to pass buffer size check in ELF::validate_program_headers().
    // p_memsz + p_offset must be sufficiently large to overflow maxint.
    ph[0].p_offset = 0x1234;
    ph[0].p_filesz = -0x1000;

    char path[] = "/tmp/test-elf.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT_NE(fd, -1);
    EXPECT_EQ(fchmod(fd, 0700), 0);

    int nwritten = write(fd, buffer, sizeof(buffer));
    EXPECT(nwritten);

    auto elf_path = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
    EXPECT(elf_path.characters());

    int rc = execl(elf_path.characters(), "test-elf", nullptr);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, 8);

    EXPECT_EQ(unlink(path), 0);
}

TEST_CASE(test_load_header_p_memsz_zero)
{
    char buffer[0x2000];

    auto& header = *(Elf32_Ehdr*)buffer;
    header.e_ident[EI_MAG0] = ELFMAG0;
    header.e_ident[EI_MAG1] = ELFMAG1;
    header.e_ident[EI_MAG2] = ELFMAG2;
    header.e_ident[EI_MAG3] = ELFMAG3;
    header.e_ident[EI_CLASS] = ELFCLASS32;
    header.e_ident[EI_DATA] = ELFDATA2LSB;
    header.e_ident[EI_VERSION] = EV_CURRENT;
    header.e_ident[EI_OSABI] = ELFOSABI_SYSV;
    header.e_ident[EI_ABIVERSION] = 0;
    header.e_type = ET_REL;
    header.e_version = EV_CURRENT;
    header.e_ehsize = sizeof(Elf32_Ehdr);
    header.e_machine = EM_386;
    header.e_shentsize = sizeof(Elf32_Shdr);
    header.e_phoff = 52;                     // inaccurate
    header.e_phentsize = sizeof(Elf32_Phdr); // inaccurate
    header.e_shnum = 3;                      // inaccurate
    header.e_shoff = 1024;                   // inaccurate
    header.e_shstrndx = 2;                   // inaccurate
    header.e_entry = 1024;                   // inaccurate

    auto* ph = (Elf32_Phdr*)(&buffer[header.e_phoff]);
    ph[0].p_flags = PF_R | PF_X;
    ph[0].p_vaddr = 0x00d4;
    ph[0].p_align = PAGE_SIZE;
    ph[0].p_type = PT_LOAD;
    ph[0].p_offset = 0;
    ph[0].p_filesz = 0;

    // p_memsz zero to trigger crash
    ph[0].p_memsz = 0;

    char path[] = "/tmp/test-elf.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT_NE(fd, -1);
    EXPECT_EQ(fchmod(fd, 0700), 0);

    int nwritten = write(fd, buffer, sizeof(buffer));
    EXPECT(nwritten);

    auto elf_path = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
    EXPECT(elf_path.characters());

    int rc = execl(elf_path.characters(), "test-elf", nullptr);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, 8);

    EXPECT_EQ(unlink(path), 0);
}

TEST_CASE(test_load_header_p_memsz_not_equal_to_p_align)
{
    char buffer[0x2000];

    auto& header = *(Elf32_Ehdr*)buffer;
    header.e_ident[EI_MAG0] = ELFMAG0;
    header.e_ident[EI_MAG1] = ELFMAG1;
    header.e_ident[EI_MAG2] = ELFMAG2;
    header.e_ident[EI_MAG3] = ELFMAG3;
    header.e_ident[EI_CLASS] = ELFCLASS32;
    header.e_ident[EI_DATA] = ELFDATA2LSB;
    header.e_ident[EI_VERSION] = EV_CURRENT;
    header.e_ident[EI_OSABI] = ELFOSABI_SYSV;
    header.e_ident[EI_ABIVERSION] = 0;
    header.e_type = ET_REL;
    header.e_version = EV_CURRENT;
    header.e_ehsize = sizeof(Elf32_Ehdr);
    header.e_machine = EM_386;
    header.e_shentsize = sizeof(Elf32_Shdr);
    header.e_phoff = 52;                     // inaccurate
    header.e_phentsize = sizeof(Elf32_Phdr); // inaccurate
    header.e_shnum = 3;                      // inaccurate
    header.e_shoff = 1024;                   // inaccurate
    header.e_shstrndx = 2;                   // inaccurate
    header.e_entry = 1024;                   // inaccurate

    auto* ph = (Elf32_Phdr*)(&buffer[header.e_phoff]);
    ph[0].p_flags = PF_R | PF_X;
    ph[0].p_vaddr = 0x00d4;
    ph[0].p_type = PT_LOAD;
    ph[0].p_memsz = 0xffff0000;
    ph[0].p_offset = 0x1000;
    ph[0].p_filesz = 0x1000;

    // p_align not equal to PAGE_SIZE to trigger crash
    ph[0].p_align = PAGE_SIZE / 2;

    char path[] = "/tmp/test-elf.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT_NE(fd, -1);
    EXPECT_EQ(fchmod(fd, 0700), 0);

    int nwritten = write(fd, buffer, sizeof(buffer));
    EXPECT(nwritten);

    auto elf_path = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
    EXPECT(elf_path.characters());

    int rc = execl(elf_path.characters(), "test-elf", nullptr);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, 8);

    EXPECT_EQ(unlink(path), 0);
}
