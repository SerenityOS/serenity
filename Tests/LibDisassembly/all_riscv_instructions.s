/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.section .text
.global _start
_start:

# All instructions in RV64G are tested with the help of this file.
# Based on Chapter 24: RV32/64G Instruction Set Listings (RISC-V Unprivileged ISA V20191213)
# When making adjustments to this assembly code, make sure to also adjust the expected output in TestRISCV64Disassembly.cpp.

# RV32I Base Instruction Set
lui x1, 0x74a05
auipc x2, 0x6a7
jal x3, .-8
jalr x4, 2(x5)
beq x6, x7, .-16
bne x8, x9, .-20
blt x10, x11, .-24
bge x12, x13, .-28
bltu x14, x15, .-32
bgeu x16, x17, .-36
lb x18, 1(x19)
lh x20, 2(x21)
lw x22, 4(x23)
lbu x24, 5(x25)
lhu x26, 6(x27)
sb x28, 1(x29)
sh x30, 2(x31)
sw x1, 4(x2)
addi x3, x4, 5
slti x5, x6, 7
sltiu x7, x8, 9
xori x9, x10, 11
ori x11, x12, 13
andi x13, x14, 15
slli x15, x16, 17
srli x17, x18, 19
srai x19, x20, 21
add x3, x4, x5
sub x22, x23, x24
sll x15, x16, x17
slt x5, x6, x7
sltu x7, x8, x9
xor x9, x10, x11
srl x17, x18, x19
sra x19, x20, x21
or x11, x12, x13
and x13, x14, x15
# NOTE: three tests for FENCE since it's uncommon in other tests.
fence irw, w
fence o, iw
fence.tso
ecall
ebreak

# RV64I Base Instruction Set
lwu x1, 4(x2)
ld x3, 8(x4)
sd x5, 16(x6)
addiw x7, x8, -9
slliw x9, x10, 11
sraiw x11, x12, 13
addw x13, x14, x15
subw x16, x17, x18
sllw x19, x20, x21
srlw x22, x23, x24
sraw x25, x26, x27

# RV32/RV64 Zifencei Standard Extension
fence.i

# RV32/RV64 Zicsr Standard Extension
csrrw x1, 3, x2
csrrs x3, 5, x4
csrrc x5, 7, x6
csrrwi x7, 8, 9
csrrsi x8, 9, 10
csrrci x9, 10, 11

# RV32M Standard Extension
mul x1, x2, x3
mulh x4, x5, x6
mulhsu x7, x8, x9
mulhu x10, x11, x12
div x13, x14, x15
divu x16, x17, x18
rem x19, x20, x21
remu x22, x23, x24

# RV64M Standard Extension
mulw x1, x2, x3
divw x13, x14, x15
divuw x16, x17, x18
remw x19, x20, x21
remuw x22, x23, x24

# RV32A Standard Extension
lr.w.aq x1, (x2)
sc.w.rl x3, x4, (x5)
amoswap.w x6, x7, (x8)
amoadd.w x9, x10, (x11)
amoxor.w x12, x13, (x14)
amoand.w x15, x16, (x17)
amoor.w x18, x19, (x20)
amomin.w x21, x22, (x23)
amomax.w x24, x25, (x26)
amominu.w x27, x28, (x29)
amomaxu.w x30, x31, (x1)

# RV64A Standard Extension
lr.d.rl x1, (x2)
sc.d x3, x4, (x5)
amoswap.d x6, x7, (x8)
amoadd.d x9, x10, (x11)
amoxor.d x12, x13, (x14)
amoand.d x15, x16, (x17)
amoor.d x18, x19, (x20)
amomin.d x21, x22, (x23)
amomax.d x24, x25, (x26)
amominu.d x27, x28, (x29)
amomaxu.d x30, x31, (x1)

# RV32F Standard Extension
flw f1, 4(x2)
fsw f3, 8(x4)
fmadd.s f5, f6, f7, f8, dyn
fmsub.s f9, f10, f11, f12, rne
fnmsub.s f13, f14, f15, f16, rdn
fnmadd.s f17, f18, f19, f20, rup
fadd.s f21, f22, f23, rmm
fsub.s f24, f25, f26
fmul.s f27, f28, f29
fdiv.s f30, f31, f1
fsqrt.s f1, f2
fsgnj.s f3, f4, f5
fsgnjn.s f6, f7, f8
fsgnjx.s f9, f10, f11
fmin.s f12, f13, f14
fmax.s f15, f16, f17
fcvt.w.s x18, f19
fcvt.wu.s x20, f21
fmv.x.w x22, f23
feq.s x23, f24, f25
flt.s x26, f27, f28
fle.s x29, f30, f31
fclass.s x1, f0
fcvt.s.w f1, x2
fcvt.s.wu f3, x4
fmv.w.x f5, x6

# RV64F Standard Extension
fcvt.l.s x1, f2
fcvt.lu.s x3, f4
fcvt.s.l f5, x6
fcvt.s.lu f7, x8

# RV32D Standard Extension
fld f1, 4(x2)
fsd f3, 8(x4)
fmadd.d f5, f6, f7, f8, dyn
fmsub.d f9, f10, f11, f12, rne
fnmsub.d f13, f14, f15, f16, rdn
fnmadd.d f17, f18, f19, f20, rup
fadd.d f21, f22, f23, rmm
fsub.d f24, f25, f26
fmul.d f27, f28, f29
fdiv.d f30, f31, f1
fsqrt.d f1, f2
fsgnj.d f3, f4, f5
fsgnjn.d f6, f7, f8
fsgnjx.d f9, f10, f11
fmin.d f12, f13, f14
fmax.d f15, f16, f17
fcvt.s.d f18, f19
fcvt.d.s f20, f21 #, rne
feq.d x23, f24, f25
flt.d x26, f27, f28
fle.d x29, f30, f31
fclass.d x1, f0
fcvt.w.d x2, f3
fcvt.wu.d x4, f5
fcvt.d.w f6, x7 #, rne
fcvt.d.wu f8, x9 #, rne

# RV64D Standard Extension
fcvt.l.d x10, f11
fcvt.lu.d x11, f12
fmv.x.d x13, f14
fcvt.d.l f15, x16
fcvt.d.lu f17, x18
fmv.d.x f19, x20
