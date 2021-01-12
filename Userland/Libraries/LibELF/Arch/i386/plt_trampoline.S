/*-
 * Copyright (c) 1998, 2002 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Christos Zoulas and by Charles M. Hannum.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * This asm method is copied from NetBSD. We changed the internal method that
 * gets called and the name, but it's still essentially the same as
 * _rtld_bind_start from libexec/ld.elf_so/arch/i386/rtld_start.S
 */

.align	4
	.globl	_plt_trampoline
	.hidden	_plt_trampoline
	.type	_plt_trampoline,@function
_plt_trampoline:	# (obj, reloff)
	pushf				# save registers
	pushl	%eax
	pushl	%ecx
	pushl	%edx

	pushl	20(%esp)		 # Copy of reloff
	pushl	20(%esp)		 # Copy of obj
	call	_fixup_plt_entry # Call the binder
	addl	$8,%esp			 # pop binder args
	movl	%eax,20(%esp)	 # Store function to be called in obj

	popl	%edx
	popl	%ecx
	popl	%eax
	popf

	leal	4(%esp),%esp		# Discard reloff, do not change eflags
	ret
