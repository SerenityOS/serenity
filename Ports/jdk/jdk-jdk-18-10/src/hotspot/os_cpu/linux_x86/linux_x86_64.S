# 
# Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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


        # NOTE WELL!  The _Copy functions are called directly
	# from server-compiler-generated code via CallLeafNoFP,
	# which means that they *must* either not use floating
	# point or use it in the same manner as does the server
	# compiler.
	
        .globl _Copy_arrayof_conjoint_bytes
	.globl _Copy_arrayof_conjoint_jshorts
        .globl _Copy_conjoint_jshorts_atomic
        .globl _Copy_arrayof_conjoint_jints
        .globl _Copy_conjoint_jints_atomic
        .globl _Copy_arrayof_conjoint_jlongs
        .globl _Copy_conjoint_jlongs_atomic

	.text

        .globl SpinPause
        .align 16
        .type  SpinPause,@function
SpinPause:
        rep
        nop
        movq   $1, %rax
        ret

        # Support for void Copy::arrayof_conjoint_bytes(void* from,
        #                                               void* to,
        #                                               size_t count)
        # rdi - from
        # rsi - to
        # rdx - count, treated as ssize_t
        #
        .p2align 4,,15
	.type    _Copy_arrayof_conjoint_bytes,@function
_Copy_arrayof_conjoint_bytes:
        movq     %rdx,%r8             # byte count
        shrq     $3,%rdx              # qword count
        cmpq     %rdi,%rsi
        leaq     -1(%rdi,%r8,1),%rax  # from + bcount*1 - 1
        jbe      acb_CopyRight
        cmpq     %rax,%rsi
        jbe      acb_CopyLeft 
acb_CopyRight:
        leaq     -8(%rdi,%rdx,8),%rax # from + qcount*8 - 8
        leaq     -8(%rsi,%rdx,8),%rcx # to + qcount*8 - 8
        negq     %rdx
        jmp      7f
        .p2align 4,,15
1:      movq     8(%rax,%rdx,8),%rsi
        movq     %rsi,8(%rcx,%rdx,8)
        addq     $1,%rdx
        jnz      1b
2:      testq    $4,%r8               # check for trailing dword
        jz       3f
        movl     8(%rax),%esi         # copy trailing dword
        movl     %esi,8(%rcx)
        addq     $4,%rax
        addq     $4,%rcx              # original %rsi is trashed, so we
                                      #  can't use it as a base register
3:      testq    $2,%r8               # check for trailing word
        jz       4f
        movw     8(%rax),%si          # copy trailing word
        movw     %si,8(%rcx)
        addq     $2,%rcx
4:      testq    $1,%r8               # check for trailing byte
        jz       5f
        movb     -1(%rdi,%r8,1),%al   # copy trailing byte
        movb     %al,8(%rcx)
5:      ret
        .p2align 4,,15
6:      movq     -24(%rax,%rdx,8),%rsi
        movq     %rsi,-24(%rcx,%rdx,8)
        movq     -16(%rax,%rdx,8),%rsi
        movq     %rsi,-16(%rcx,%rdx,8)
        movq     -8(%rax,%rdx,8),%rsi
        movq     %rsi,-8(%rcx,%rdx,8)
        movq     (%rax,%rdx,8),%rsi
        movq     %rsi,(%rcx,%rdx,8)
7:      addq     $4,%rdx
        jle      6b
        subq     $4,%rdx
        jl       1b
        jmp      2b
acb_CopyLeft:
        testq    $1,%r8               # check for trailing byte
        jz       1f
        movb     -1(%rdi,%r8,1),%cl   # copy trailing byte
        movb     %cl,-1(%rsi,%r8,1)
        subq     $1,%r8               # adjust for possible trailing word
1:      testq    $2,%r8               # check for trailing word
        jz       2f
        movw     -2(%rdi,%r8,1),%cx   # copy trailing word
        movw     %cx,-2(%rsi,%r8,1)
2:      testq    $4,%r8               # check for trailing dword
        jz       5f
        movl     (%rdi,%rdx,8),%ecx   # copy trailing dword
        movl     %ecx,(%rsi,%rdx,8)
        jmp      5f
        .p2align 4,,15
3:      movq     -8(%rdi,%rdx,8),%rcx
        movq     %rcx,-8(%rsi,%rdx,8)
        subq     $1,%rdx
        jnz      3b
        ret
        .p2align 4,,15
4:      movq     24(%rdi,%rdx,8),%rcx
        movq     %rcx,24(%rsi,%rdx,8)
        movq     16(%rdi,%rdx,8),%rcx
        movq     %rcx,16(%rsi,%rdx,8)
        movq     8(%rdi,%rdx,8),%rcx
        movq     %rcx,8(%rsi,%rdx,8)
        movq     (%rdi,%rdx,8),%rcx
        movq     %rcx,(%rsi,%rdx,8)
5:      subq     $4,%rdx
        jge      4b
        addq     $4,%rdx
        jg       3b
        ret

        # Support for void Copy::arrayof_conjoint_jshorts(void* from,
        #                                                 void* to,
        #                                                 size_t count)
        # Equivalent to
        #   conjoint_jshorts_atomic
        #
        # If 'from' and/or 'to' are aligned on 4- or 2-byte boundaries, we
        # let the hardware handle it.  The tow or four words within dwords
        # or qwords that span cache line boundaries will still be loaded
        # and stored atomically.
        #
        # rdi - from
        # rsi - to
        # rdx - count, treated as ssize_t
        #
        .p2align 4,,15
	.type    _Copy_arrayof_conjoint_jshorts,@function
	.type    _Copy_conjoint_jshorts_atomic,@function
_Copy_arrayof_conjoint_jshorts:
_Copy_conjoint_jshorts_atomic:
        movq     %rdx,%r8             # word count
        shrq     $2,%rdx              # qword count
        cmpq     %rdi,%rsi
        leaq     -2(%rdi,%r8,2),%rax  # from + wcount*2 - 2
        jbe      acs_CopyRight
        cmpq     %rax,%rsi
        jbe      acs_CopyLeft 
acs_CopyRight:
        leaq     -8(%rdi,%rdx,8),%rax # from + qcount*8 - 8
        leaq     -8(%rsi,%rdx,8),%rcx # to + qcount*8 - 8
        negq     %rdx
        jmp      6f
1:      movq     8(%rax,%rdx,8),%rsi
        movq     %rsi,8(%rcx,%rdx,8)
        addq     $1,%rdx
        jnz      1b
2:      testq    $2,%r8               # check for trailing dword
        jz       3f
        movl     8(%rax),%esi         # copy trailing dword
        movl     %esi,8(%rcx)
        addq     $4,%rcx              # original %rsi is trashed, so we
                                      #  can't use it as a base register
3:      testq    $1,%r8               # check for trailing word
        jz       4f
        movw     -2(%rdi,%r8,2),%si   # copy trailing word
        movw     %si,8(%rcx)
4:      ret
        .p2align 4,,15
5:      movq     -24(%rax,%rdx,8),%rsi
        movq     %rsi,-24(%rcx,%rdx,8)
        movq     -16(%rax,%rdx,8),%rsi
        movq     %rsi,-16(%rcx,%rdx,8)
        movq     -8(%rax,%rdx,8),%rsi
        movq     %rsi,-8(%rcx,%rdx,8)
        movq     (%rax,%rdx,8),%rsi
        movq     %rsi,(%rcx,%rdx,8)
6:      addq     $4,%rdx
        jle      5b
        subq     $4,%rdx
        jl       1b
        jmp      2b
acs_CopyLeft:
        testq    $1,%r8               # check for trailing word
        jz       1f
        movw     -2(%rdi,%r8,2),%cx   # copy trailing word
        movw     %cx,-2(%rsi,%r8,2)
1:      testq    $2,%r8               # check for trailing dword
        jz       4f
        movl     (%rdi,%rdx,8),%ecx   # copy trailing dword
        movl     %ecx,(%rsi,%rdx,8)
        jmp      4f
2:      movq     -8(%rdi,%rdx,8),%rcx
        movq     %rcx,-8(%rsi,%rdx,8)
        subq     $1,%rdx
        jnz      2b
        ret
        .p2align 4,,15
3:      movq     24(%rdi,%rdx,8),%rcx
        movq     %rcx,24(%rsi,%rdx,8)
        movq     16(%rdi,%rdx,8),%rcx
        movq     %rcx,16(%rsi,%rdx,8)
        movq     8(%rdi,%rdx,8),%rcx
        movq     %rcx,8(%rsi,%rdx,8)
        movq     (%rdi,%rdx,8),%rcx
        movq     %rcx,(%rsi,%rdx,8)
4:      subq     $4,%rdx
        jge      3b
        addq     $4,%rdx
        jg       2b
        ret

        # Support for void Copy::arrayof_conjoint_jints(jint* from,
        #                                               jint* to,
        #                                               size_t count)
        # Equivalent to
        #   conjoint_jints_atomic
        #
        # If 'from' and/or 'to' are aligned on 4-byte boundaries, we let
        # the hardware handle it.  The two dwords within qwords that span
        # cache line boundaries will still be loaded and stored atomically.
        #
        # rdi - from
        # rsi - to
        # rdx - count, treated as ssize_t
        #
        .p2align 4,,15
	.type    _Copy_arrayof_conjoint_jints,@function
	.type    _Copy_conjoint_jints_atomic,@function
_Copy_arrayof_conjoint_jints:
_Copy_conjoint_jints_atomic:
        movq     %rdx,%r8             # dword count
        shrq     %rdx                 # qword count
        cmpq     %rdi,%rsi
        leaq     -4(%rdi,%r8,4),%rax  # from + dcount*4 - 4
        jbe      aci_CopyRight
        cmpq     %rax,%rsi
        jbe      aci_CopyLeft 
aci_CopyRight:
        leaq     -8(%rdi,%rdx,8),%rax # from + qcount*8 - 8
        leaq     -8(%rsi,%rdx,8),%rcx # to + qcount*8 - 8
        negq     %rdx
        jmp      5f
        .p2align 4,,15
1:      movq     8(%rax,%rdx,8),%rsi
        movq     %rsi,8(%rcx,%rdx,8)
        addq     $1,%rdx
        jnz       1b
2:      testq    $1,%r8               # check for trailing dword
        jz       3f
        movl     8(%rax),%esi         # copy trailing dword
        movl     %esi,8(%rcx)
3:      ret
        .p2align 4,,15
4:      movq     -24(%rax,%rdx,8),%rsi
        movq     %rsi,-24(%rcx,%rdx,8)
        movq     -16(%rax,%rdx,8),%rsi
        movq     %rsi,-16(%rcx,%rdx,8)
        movq     -8(%rax,%rdx,8),%rsi
        movq     %rsi,-8(%rcx,%rdx,8)
        movq     (%rax,%rdx,8),%rsi
        movq     %rsi,(%rcx,%rdx,8)
5:      addq     $4,%rdx
        jle      4b
        subq     $4,%rdx
        jl       1b
        jmp      2b
aci_CopyLeft:
        testq    $1,%r8               # check for trailing dword
        jz       3f
        movl     -4(%rdi,%r8,4),%ecx  # copy trailing dword
        movl     %ecx,-4(%rsi,%r8,4)
        jmp      3f
1:      movq     -8(%rdi,%rdx,8),%rcx
        movq     %rcx,-8(%rsi,%rdx,8)
        subq     $1,%rdx
        jnz      1b
        ret
        .p2align 4,,15
2:      movq     24(%rdi,%rdx,8),%rcx
        movq     %rcx,24(%rsi,%rdx,8)
        movq     16(%rdi,%rdx,8),%rcx
        movq     %rcx,16(%rsi,%rdx,8)
        movq     8(%rdi,%rdx,8),%rcx
        movq     %rcx,8(%rsi,%rdx,8)
        movq     (%rdi,%rdx,8),%rcx
        movq     %rcx,(%rsi,%rdx,8)
3:      subq     $4,%rdx
        jge      2b
        addq     $4,%rdx
        jg       1b
        ret

        # Support for void Copy::arrayof_conjoint_jlongs(jlong* from,
        #                                                jlong* to,
        #                                                size_t count)
        # Equivalent to
        #   conjoint_jlongs_atomic
        #   arrayof_conjoint_oops
        #   conjoint_oops_atomic
        #
        # rdi - from
        # rsi - to
        # rdx - count, treated as ssize_t
        #
        .p2align 4,,15
	.type    _Copy_arrayof_conjoint_jlongs,@function
	.type    _Copy_conjoint_jlongs_atomic,@function
_Copy_arrayof_conjoint_jlongs:
_Copy_conjoint_jlongs_atomic:
        cmpq     %rdi,%rsi
        leaq     -8(%rdi,%rdx,8),%rax # from + count*8 - 8
        jbe      acl_CopyRight
        cmpq     %rax,%rsi
        jbe      acl_CopyLeft 
acl_CopyRight:
        leaq     -8(%rsi,%rdx,8),%rcx # to + count*8 - 8
        negq     %rdx
        jmp      3f
1:      movq     8(%rax,%rdx,8),%rsi
        movq     %rsi,8(%rcx,%rdx,8)
        addq     $1,%rdx
        jnz      1b
        ret
        .p2align 4,,15
2:      movq     -24(%rax,%rdx,8),%rsi
        movq     %rsi,-24(%rcx,%rdx,8)
        movq     -16(%rax,%rdx,8),%rsi
        movq     %rsi,-16(%rcx,%rdx,8)
        movq     -8(%rax,%rdx,8),%rsi
        movq     %rsi,-8(%rcx,%rdx,8)
        movq     (%rax,%rdx,8),%rsi
        movq     %rsi,(%rcx,%rdx,8)
3:      addq     $4,%rdx
        jle      2b
        subq     $4,%rdx
        jl       1b
        ret
4:      movq     -8(%rdi,%rdx,8),%rcx
        movq     %rcx,-8(%rsi,%rdx,8)
        subq     $1,%rdx
        jnz      4b
        ret
        .p2align 4,,15
5:      movq     24(%rdi,%rdx,8),%rcx
        movq     %rcx,24(%rsi,%rdx,8)
        movq     16(%rdi,%rdx,8),%rcx
        movq     %rcx,16(%rsi,%rdx,8)
        movq     8(%rdi,%rdx,8),%rcx
        movq     %rcx,8(%rsi,%rdx,8)
        movq     (%rdi,%rdx,8),%rcx
        movq     %rcx,(%rsi,%rdx,8)
acl_CopyLeft:
        subq     $4,%rdx
        jge      5b
        addq     $4,%rdx
        jg       4b
        ret
