#
# Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#


#ifdef __APPLE__
# Darwin uses _ prefixed global symbols
#define SYMBOL(s) _ ## s
#define ELF_TYPE(name, description)
#else
#define SYMBOL(s) s
#define ELF_TYPE(name, description) .type name,description
#endif

        .globl SYMBOL(fixcw)

        # NOTE WELL!  The _Copy functions are called directly
        # from server-compiler-generated code via CallLeafNoFP,
        # which means that they *must* either not use floating
        # point or use it in the same manner as does the server
        # compiler.

        .globl SYMBOL(_Copy_conjoint_bytes)
        .globl SYMBOL(_Copy_arrayof_conjoint_bytes)
        .globl SYMBOL(_Copy_conjoint_jshorts_atomic)
        .globl SYMBOL(_Copy_arrayof_conjoint_jshorts)
        .globl SYMBOL(_Copy_conjoint_jints_atomic)
        .globl SYMBOL(_Copy_arrayof_conjoint_jints)
        .globl SYMBOL(_Copy_conjoint_jlongs_atomic)
        .globl SYMBOL(_mmx_Copy_arrayof_conjoint_jshorts)

        .globl SYMBOL(_Atomic_cmpxchg_long)
        .globl SYMBOL(_Atomic_move_long)

        .text

# Support for void os::Solaris::init_thread_fpu_state() in os_solaris_i486.cpp
# Set fpu to 53 bit precision.  This happens too early to use a stub.
# ported from solaris_x86_32.s
        .p2align 4,,15
SYMBOL(fixcw):
        pushl    $0x27f
        fldcw    0(%esp)
        popl     %eax
        ret

        .globl  SYMBOL(SpinPause)
        ELF_TYPE(SpinPause,@function)
        .p2align 4,,15
SYMBOL(SpinPause):
        rep
        nop
        movl    $1, %eax
        ret

        # Support for void Copy::conjoint_bytes(void* from,
        #                                       void* to,
        #                                       size_t count)
        .p2align 4,,15
        ELF_TYPE(_Copy_conjoint_bytes,@function)
SYMBOL(_Copy_conjoint_bytes):
        pushl    %esi
        movl     4+12(%esp),%ecx      # count
        pushl    %edi
        movl     8+ 4(%esp),%esi      # from
        movl     8+ 8(%esp),%edi      # to
        cmpl     %esi,%edi
        leal     -1(%esi,%ecx),%eax   # from + count - 1
        jbe      cb_CopyRight
        cmpl     %eax,%edi
        jbe      cb_CopyLeft
        # copy from low to high
cb_CopyRight:
        cmpl     $3,%ecx
        jbe      5f                   # <= 3 bytes
        # align source address at dword address boundary
        movl     %ecx,%eax            # original count
        movl     $4,%ecx
        subl     %esi,%ecx
        andl     $3,%ecx              # prefix byte count
        jz       1f                   # no prefix
        subl     %ecx,%eax            # byte count less prefix
        # copy prefix
        subl     %esi,%edi
0:      movb     (%esi),%dl
        movb     %dl,(%edi,%esi,1)
        addl     $1,%esi
        subl     $1,%ecx
        jnz      0b
        addl     %esi,%edi
1:      movl     %eax,%ecx            # byte count less prefix
        shrl     $2,%ecx              # dword count
        jz       4f                   # no dwords to move
        cmpl     $32,%ecx
        jbe      2f                   # <= 32 dwords
        # copy aligned dwords
        rep;     smovl
        jmp      4f
        # copy aligned dwords
2:      subl     %esi,%edi
        .p2align 4,,15
3:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        addl     $4,%esi
        subl     $1,%ecx
        jnz      3b
        addl     %esi,%edi
4:      movl     %eax,%ecx            # byte count less prefix
5:      andl     $3,%ecx              # suffix byte count
        jz       7f                   # no suffix
        # copy suffix
        xorl     %eax,%eax
6:      movb     (%esi,%eax,1),%dl
        movb     %dl,(%edi,%eax,1)
        addl     $1,%eax
        subl     $1,%ecx
        jnz      6b
7:      popl     %edi
        popl     %esi
        ret
        # copy from high to low
cb_CopyLeft:
        std
        leal     -4(%edi,%ecx),%edi   # to + count - 4
        movl     %eax,%esi            # from + count - 1
        movl     %ecx,%eax
        subl     $3,%esi              # from + count - 4
        cmpl     $3,%ecx
        jbe      5f                   # <= 3 bytes
1:      shrl     $2,%ecx              # dword count
        jz       4f                   # no dwords to move
        cmpl     $32,%ecx
        ja       3f                   # > 32 dwords
        # copy dwords, aligned or not
        subl     %esi,%edi
        .p2align 4,,15
2:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        subl     $4,%esi
        subl     $1,%ecx
        jnz      2b
        addl     %esi,%edi
        jmp      4f
        # copy dwords, aligned or not
3:      rep;     smovl
4:      movl     %eax,%ecx            # byte count
5:      andl     $3,%ecx              # suffix byte count
        jz       7f                   # no suffix
        # copy suffix
        subl     %esi,%edi
        addl     $3,%esi
6:      movb     (%esi),%dl
        movb     %dl,(%edi,%esi,1)
        subl     $1,%esi
        subl     $1,%ecx
        jnz      6b
7:      cld
        popl     %edi
        popl     %esi
        ret

        # Support for void Copy::arrayof_conjoint_bytes(void* from,
        #                                               void* to,
        #                                               size_t count)
        #
        # Same as _Copy_conjoint_bytes, except no source alignment check.
        .p2align 4,,15
        ELF_TYPE(_Copy_arrayof_conjoint_bytes,@function)
SYMBOL(_Copy_arrayof_conjoint_bytes):
        pushl    %esi
        movl     4+12(%esp),%ecx      # count
        pushl    %edi
        movl     8+ 4(%esp),%esi      # from
        movl     8+ 8(%esp),%edi      # to
        cmpl     %esi,%edi
        leal     -1(%esi,%ecx),%eax   # from + count - 1
        jbe      acb_CopyRight
        cmpl     %eax,%edi
        jbe      acb_CopyLeft
        # copy from low to high
acb_CopyRight:
        cmpl     $3,%ecx
        jbe      5f
1:      movl     %ecx,%eax
        shrl     $2,%ecx
        jz       4f
        cmpl     $32,%ecx
        ja       3f
        # copy aligned dwords
        subl     %esi,%edi
        .p2align 4,,15
2:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        addl     $4,%esi
        subl     $1,%ecx
        jnz      2b
        addl     %esi,%edi
        jmp      4f
        # copy aligned dwords
3:      rep;     smovl
4:      movl     %eax,%ecx
5:      andl     $3,%ecx
        jz       7f
        # copy suffix
        xorl     %eax,%eax
6:      movb     (%esi,%eax,1),%dl
        movb     %dl,(%edi,%eax,1)
        addl     $1,%eax
        subl     $1,%ecx
        jnz      6b
7:      popl     %edi
        popl     %esi
        ret
acb_CopyLeft:
        std
        leal     -4(%edi,%ecx),%edi   # to + count - 4
        movl     %eax,%esi            # from + count - 1
        movl     %ecx,%eax
        subl     $3,%esi              # from + count - 4
        cmpl     $3,%ecx
        jbe      5f
1:      shrl     $2,%ecx
        jz       4f
        cmpl     $32,%ecx
        jbe      2f                   # <= 32 dwords
        rep;     smovl
        jmp      4f
        .space 8
2:      subl     %esi,%edi
        .p2align 4,,15
3:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        subl     $4,%esi
        subl     $1,%ecx
        jnz      3b
        addl     %esi,%edi
4:      movl     %eax,%ecx
5:      andl     $3,%ecx
        jz       7f
        subl     %esi,%edi
        addl     $3,%esi
6:      movb     (%esi),%dl
        movb     %dl,(%edi,%esi,1)
        subl     $1,%esi
        subl     $1,%ecx
        jnz      6b
7:      cld
        popl     %edi
        popl     %esi
        ret

        # Support for void Copy::conjoint_jshorts_atomic(void* from,
        #                                                void* to,
        #                                                size_t count)
        .p2align 4,,15
        ELF_TYPE(_Copy_conjoint_jshorts_atomic,@function)
SYMBOL(_Copy_conjoint_jshorts_atomic):
        pushl    %esi
        movl     4+12(%esp),%ecx      # count
        pushl    %edi
        movl     8+ 4(%esp),%esi      # from
        movl     8+ 8(%esp),%edi      # to
        cmpl     %esi,%edi
        leal     -2(%esi,%ecx,2),%eax # from + count*2 - 2
        jbe      cs_CopyRight
        cmpl     %eax,%edi
        jbe      cs_CopyLeft
        # copy from low to high
cs_CopyRight:
        # align source address at dword address boundary
        movl     %esi,%eax            # original from
        andl     $3,%eax              # either 0 or 2
        jz       1f                   # no prefix
        # copy prefix
        subl     $1,%ecx
        jl       5f                   # zero count
        movw     (%esi),%dx
        movw     %dx,(%edi)
        addl     %eax,%esi            # %eax == 2
        addl     %eax,%edi
1:      movl     %ecx,%eax            # word count less prefix
        sarl     %ecx                 # dword count
        jz       4f                   # no dwords to move
        cmpl     $32,%ecx
        jbe      2f                   # <= 32 dwords
        # copy aligned dwords
        rep;     smovl
        jmp      4f
        # copy aligned dwords
2:      subl     %esi,%edi
        .p2align 4,,15
3:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        addl     $4,%esi
        subl     $1,%ecx
        jnz      3b
        addl     %esi,%edi
4:      andl     $1,%eax              # suffix count
        jz       5f                   # no suffix
        # copy suffix
        movw     (%esi),%dx
        movw     %dx,(%edi)
5:      popl     %edi
        popl     %esi
        ret
        # copy from high to low
cs_CopyLeft:
        std
        leal     -4(%edi,%ecx,2),%edi # to + count*2 - 4
        movl     %eax,%esi            # from + count*2 - 2
        movl     %ecx,%eax
        subl     $2,%esi              # from + count*2 - 4
1:      sarl     %ecx                 # dword count
        jz       4f                   # no dwords to move
        cmpl     $32,%ecx
        ja       3f                   # > 32 dwords
        subl     %esi,%edi
        .p2align 4,,15
2:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        subl     $4,%esi
        subl     $1,%ecx
        jnz      2b
        addl     %esi,%edi
        jmp      4f
3:      rep;     smovl
4:      andl     $1,%eax              # suffix count
        jz       5f                   # no suffix
        # copy suffix
        addl     $2,%esi
        addl     $2,%edi
        movw     (%esi),%dx
        movw     %dx,(%edi)
5:      cld
        popl     %edi
        popl     %esi
        ret

        # Support for void Copy::arrayof_conjoint_jshorts(void* from,
        #                                                 void* to,
        #                                                 size_t count)
        .p2align 4,,15
        ELF_TYPE(_Copy_arrayof_conjoint_jshorts,@function)
SYMBOL(_Copy_arrayof_conjoint_jshorts):
        pushl    %esi
        movl     4+12(%esp),%ecx      # count
        pushl    %edi
        movl     8+ 4(%esp),%esi      # from
        movl     8+ 8(%esp),%edi      # to
        cmpl     %esi,%edi
        leal     -2(%esi,%ecx,2),%eax # from + count*2 - 2
        jbe      acs_CopyRight
        cmpl     %eax,%edi
        jbe      acs_CopyLeft
acs_CopyRight:
        movl     %ecx,%eax            # word count
        sarl     %ecx                 # dword count
        jz       4f                   # no dwords to move
        cmpl     $32,%ecx
        jbe      2f                   # <= 32 dwords
        # copy aligned dwords
        rep;     smovl
        jmp      4f
        # copy aligned dwords
        .space 5
2:      subl     %esi,%edi
        .p2align 4,,15
3:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        addl     $4,%esi
        subl     $1,%ecx
        jnz      3b
        addl     %esi,%edi
4:      andl     $1,%eax              # suffix count
        jz       5f                   # no suffix
        # copy suffix
        movw     (%esi),%dx
        movw     %dx,(%edi)
5:      popl     %edi
        popl     %esi
        ret
acs_CopyLeft:
        std
        leal     -4(%edi,%ecx,2),%edi # to + count*2 - 4
        movl     %eax,%esi            # from + count*2 - 2
        movl     %ecx,%eax
        subl     $2,%esi              # from + count*2 - 4
        sarl     %ecx                 # dword count
        jz       4f                   # no dwords to move
        cmpl     $32,%ecx
        ja       3f                   # > 32 dwords
        subl     %esi,%edi
        .p2align 4,,15
2:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        subl     $4,%esi
        subl     $1,%ecx
        jnz      2b
        addl     %esi,%edi
        jmp      4f
3:      rep;     smovl
4:      andl     $1,%eax              # suffix count
        jz       5f                   # no suffix
        # copy suffix
        addl     $2,%esi
        addl     $2,%edi
        movw     (%esi),%dx
        movw     %dx,(%edi)
5:      cld
        popl     %edi
        popl     %esi
        ret

        # Support for void Copy::conjoint_jints_atomic(void* from,
        #                                              void* to,
        #                                              size_t count)
        # Equivalent to
        #   arrayof_conjoint_jints
        .p2align 4,,15
        ELF_TYPE(_Copy_conjoint_jints_atomic,@function)
        ELF_TYPE(_Copy_arrayof_conjoint_jints,@function)
SYMBOL(_Copy_conjoint_jints_atomic):
SYMBOL(_Copy_arrayof_conjoint_jints):
        pushl    %esi
        movl     4+12(%esp),%ecx      # count
        pushl    %edi
        movl     8+ 4(%esp),%esi      # from
        movl     8+ 8(%esp),%edi      # to
        cmpl     %esi,%edi
        leal     -4(%esi,%ecx,4),%eax # from + count*4 - 4
        jbe      ci_CopyRight
        cmpl     %eax,%edi
        jbe      ci_CopyLeft
ci_CopyRight:
        cmpl     $32,%ecx
        jbe      2f                   # <= 32 dwords
        rep;     smovl
        popl     %edi
        popl     %esi
        ret
        .space 10
2:      subl     %esi,%edi
        jmp      4f
        .p2align 4,,15
3:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        addl     $4,%esi
4:      subl     $1,%ecx
        jge      3b
        popl     %edi
        popl     %esi
        ret
ci_CopyLeft:
        std
        leal     -4(%edi,%ecx,4),%edi # to + count*4 - 4
        cmpl     $32,%ecx
        ja       4f                   # > 32 dwords
        subl     %eax,%edi            # eax == from + count*4 - 4
        jmp      3f
        .p2align 4,,15
2:      movl     (%eax),%edx
        movl     %edx,(%edi,%eax,1)
        subl     $4,%eax
3:      subl     $1,%ecx
        jge      2b
        cld
        popl     %edi
        popl     %esi
        ret
4:      movl     %eax,%esi            # from + count*4 - 4
        rep;     smovl
        cld
        popl     %edi
        popl     %esi
        ret

        # Support for void Copy::conjoint_jlongs_atomic(jlong* from,
        #                                               jlong* to,
        #                                               size_t count)
        #
        # 32-bit
        #
        # count treated as signed
        #
        # // if (from > to) {
        #   while (--count >= 0) {
        #     *to++ = *from++;
        #   }
        # } else {
        #   while (--count >= 0) {
        #     to[count] = from[count];
        #   }
        # }
        .p2align 4,,15
        ELF_TYPE(_Copy_conjoint_jlongs_atomic,@function)
SYMBOL(_Copy_conjoint_jlongs_atomic):
        movl     4+8(%esp),%ecx       # count
        movl     4+0(%esp),%eax       # from
        movl     4+4(%esp),%edx       # to
        cmpl     %eax,%edx
        jae      cla_CopyLeft
cla_CopyRight:
        subl     %eax,%edx
        jmp      2f
        .p2align 4,,15
1:      fildll   (%eax)
        fistpll  (%edx,%eax,1)
        addl     $8,%eax
2:      subl     $1,%ecx
        jge      1b
        ret
        .p2align 4,,15
3:      fildll   (%eax,%ecx,8)
        fistpll  (%edx,%ecx,8)
cla_CopyLeft:
        subl     $1,%ecx
        jge      3b
        ret

        # Support for void Copy::arrayof_conjoint_jshorts(void* from,
        #                                                 void* to,
        #                                                 size_t count)
        .p2align 4,,15
        ELF_TYPE(_mmx_Copy_arrayof_conjoint_jshorts,@function)
SYMBOL(_mmx_Copy_arrayof_conjoint_jshorts):
        pushl    %esi
        movl     4+12(%esp),%ecx
        pushl    %edi
        movl     8+ 4(%esp),%esi
        movl     8+ 8(%esp),%edi
        cmpl     %esi,%edi
        leal     -2(%esi,%ecx,2),%eax
        jbe      mmx_acs_CopyRight
        cmpl     %eax,%edi
        jbe      mmx_acs_CopyLeft
mmx_acs_CopyRight:
        movl     %ecx,%eax
        sarl     %ecx
        je       5f
        cmpl     $33,%ecx
        jae      3f
1:      subl     %esi,%edi
        .p2align 4,,15
2:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        addl     $4,%esi
        subl     $1,%ecx
        jnz      2b
        addl     %esi,%edi
        jmp      5f
3:      smovl # align to 8 bytes, we know we are 4 byte aligned to start
        subl     $1,%ecx
4:      .p2align 4,,15
        movq     0(%esi),%mm0
        addl     $64,%edi
        movq     8(%esi),%mm1
        subl     $16,%ecx
        movq     16(%esi),%mm2
        movq     %mm0,-64(%edi)
        movq     24(%esi),%mm0
        movq     %mm1,-56(%edi)
        movq     32(%esi),%mm1
        movq     %mm2,-48(%edi)
        movq     40(%esi),%mm2
        movq     %mm0,-40(%edi)
        movq     48(%esi),%mm0
        movq     %mm1,-32(%edi)
        movq     56(%esi),%mm1
        movq     %mm2,-24(%edi)
        movq     %mm0,-16(%edi)
        addl     $64,%esi
        movq     %mm1,-8(%edi)
        cmpl     $16,%ecx
        jge      4b
        emms
        testl    %ecx,%ecx
        ja       1b
5:      andl     $1,%eax
        je       7f
6:      movw     (%esi),%dx
        movw     %dx,(%edi)
7:      popl     %edi
        popl     %esi
        ret
mmx_acs_CopyLeft:
        std
        leal     -4(%edi,%ecx,2),%edi
        movl     %eax,%esi
        movl     %ecx,%eax
        subl     $2,%esi
        sarl     %ecx
        je       4f
        cmpl     $32,%ecx
        ja       3f
        subl     %esi,%edi
        .p2align 4,,15
2:      movl     (%esi),%edx
        movl     %edx,(%edi,%esi,1)
        subl     $4,%esi
        subl     $1,%ecx
        jnz      2b
        addl     %esi,%edi
        jmp      4f
3:      rep;     smovl
4:      andl     $1,%eax
        je       6f
        addl     $2,%esi
        addl     $2,%edi
5:      movw     (%esi),%dx
        movw     %dx,(%edi)
6:      cld
        popl     %edi
        popl     %esi
        ret


        # Support for int64_t Atomic::cmpxchg(int64_t compare_value,
        #                                     volatile int64_t* dest,
        #                                     int64_t exchange_value)
        #
        .p2align 4,,15
        ELF_TYPE(_Atomic_cmpxchg_long,@function)
SYMBOL(_Atomic_cmpxchg_long):
                                   #  8(%esp) : return PC
        pushl    %ebx              #  4(%esp) : old %ebx
        pushl    %edi              #  0(%esp) : old %edi
        movl     12(%esp), %ebx    # 12(%esp) : exchange_value (low)
        movl     16(%esp), %ecx    # 16(%esp) : exchange_value (high)
        movl     24(%esp), %eax    # 24(%esp) : compare_value (low)
        movl     28(%esp), %edx    # 28(%esp) : compare_value (high)
        movl     20(%esp), %edi    # 20(%esp) : dest
        lock
        cmpxchg8b (%edi)
        popl     %edi
        popl     %ebx
        ret


        # Support for int64_t Atomic::load and Atomic::store.
        # void _Atomic_move_long(const volatile int64_t* src, volatile int64_t* dst)
        .p2align 4,,15
        ELF_TYPE(_Atomic_move_long,@function)
SYMBOL(_Atomic_move_long):
        movl     4(%esp), %eax   # src
        fildll    (%eax)
        movl     8(%esp), %eax   # dest
        fistpll   (%eax)
        ret
