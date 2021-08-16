/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package sun.jvm.hotspot.debugger.win32.coff;

/** Enumerates the types of COFF object file relocations for all
    currently-supported processors. (Some of the descriptions are
    taken directly from Microsoft's documentation and are copyrighted
    by Microsoft.) */

public interface TypeIndicators {
  //
  // I386 processors
  //

  /** This relocation is ignored. */
  public static final short IMAGE_REL_I386_ABSOLUTE = 0x0000;
  /** Not supported. */
  public static final short IMAGE_REL_I386_DIR16 = (short) 0x0001;
  /** Not supported. */
  public static final short IMAGE_REL_I386_REL16 = (short) 0x0002;
  /** The target?s 32-bit virtual address. */
  public static final short IMAGE_REL_I386_DIR32 = (short) 0x0006;
  /** The target?s 32-bit relative virtual address. */
  public static final short IMAGE_REL_I386_DIR32NB = (short) 0x0007;
  /** Not supported. */
  public static final short IMAGE_REL_I386_SEG12 = (short) 0x0009;
  /** The 16-bit-section index of the section containing the
      target. This is used to support debugging information. */
  public static final short IMAGE_REL_I386_SECTION = (short) 0x000A;
  /** The 32-bit offset of the target from the beginning of its
      section. This is used to support debugging information as well
      as static thread local storage. */
  public static final short IMAGE_REL_I386_SECREL = (short) 0x000B;
  /** The 32-bit relative displacement to the target. This supports
      the x86 relative branch and call instructions. */
  public static final short IMAGE_REL_I386_REL32 = (short) 0x0014;

  //
  // MIPS processors
  //

  /** This relocation is ignored. */
  public static final short IMAGE_REL_MIPS_ABSOLUTE = (short) 0x0000;
  /** The high 16 bits of the target's 32-bit virtual address. */
  public static final short IMAGE_REL_MIPS_REFHALF = (short) 0x0001;
  /** The target's 32-bit virtual address. */
  public static final short IMAGE_REL_MIPS_REFWORD = (short) 0x0002;
  /** The low 26 bits of the target's virtual address. This
      supports the MIPS J and JAL instructions. */
  public static final short IMAGE_REL_MIPS_JMPADDR = (short) 0x0003;
  /** The high 16 bits of the target's 32-bit virtual address. Used
      for the first instruction in a two-instruction sequence that
      loads a full address. This relocation must be immediately
      followed by a PAIR relocations whose SymbolTableIndex contains a
      signed 16-bit displacement which is added to the upper 16 bits
      taken from the location being relocated. */
  public static final short IMAGE_REL_MIPS_REFHI = (short) 0x0004;
  /** The low 16 bits of the target's virtual address. */
  public static final short IMAGE_REL_MIPS_REFLO = (short) 0x0005;
  /** 16-bit signed displacement of the target relative to the Global
      Pointer (GP) register. */
  public static final short IMAGE_REL_MIPS_GPREL = (short) 0x0006;
  /** Same as IMAGE_REL_MIPS_GPREL. */
  public static final short IMAGE_REL_MIPS_LITERAL = (short) 0x0007;
  /** The 16-bit section index of the section containing the target.
      This is used to support debugging information. */
  public static final short IMAGE_REL_MIPS_SECTION = (short) 0x000A;
  /** The 32-bit offset of the target from the beginning of its
      section. This is used to support debugging information as well
      as static thread local storage. */
  public static final short IMAGE_REL_MIPS_SECREL = (short) 0x000B;
  /** The low 16 bits of the 32-bit offset of the target from the
      beginning of its section. */
  public static final short IMAGE_REL_MIPS_SECRELLO = (short) 0x000C;
  /** The high 16 bits of the 32-bit offset of the target from the
      beginning of its section. A PAIR relocation must immediately
      follow this on. The SymbolTableIndex of the PAIR relocation
      contains a signed 16-bit displacement, which is added to the
      upper 16 bits taken from the location being relocated. */
  public static final short IMAGE_REL_MIPS_SECRELHI = (short) 0x000D;
  /** The low 26 bits of the target's virtual address. This supports
      the MIPS16 JAL instruction. */
  public static final short IMAGE_REL_MIPS_JMPADDR16 = (short) 0x0010;
  /** The target's 32-bit relative virtual address. */
  public static final short IMAGE_REL_MIPS_REFWORDNB = (short) 0x0022;
  /** This relocation is only valid when it immediately follows a
      REFHI or SECRELHI relocation. Its SymbolTableIndex contains a
      displacement and not an index into the symbol table. */
  public static final short IMAGE_REL_MIPS_PAIR = (short) 0x0025;

  //
  // Alpha processors
  //

  /** This relocation is ignored. */
  public static final short IMAGE_REL_ALPHA_ABSOLUTE = (short) 0x0000;
  /** The target's 32-bit virtual address. This fixup is illegal in a
      PE32+ image unless the image has been sandboxed by clearing the
      IMAGE_FILE_LARGE_ADDRESS_AWARE bit in the File Header. */
  public static final short IMAGE_REL_ALPHA_REFLONG = (short) 0x0001;
  /** The target's 64-bit virtual address. */
  public static final short IMAGE_REL_ALPHA_REFQUAD = (short) 0x0002;
  /** 32-bit signed displacement of the target relative to the Global
      Pointer (GP) register. */
  public static final short IMAGE_REL_ALPHA_GPREL32 = (short) 0x0003;
  /** 16-bit signed displacement of the target relative to the Global
      Pointer (GP) register. */
  public static final short IMAGE_REL_ALPHA_LITERAL = (short) 0x0004;
  /** Reserved for future use. */
  public static final short IMAGE_REL_ALPHA_LITUSE = (short) 0x0005;
  /** Reserved for future use. */
  public static final short IMAGE_REL_ALPHA_GPDISP = (short) 0x0006;
  /** The 21-bit relative displacement to the target. This supports
      the Alpha relative branch instructions. */
  public static final short IMAGE_REL_ALPHA_BRADDR = (short) 0x0007;
  /** 14-bit hints to the processor for the target of an Alpha jump
      instruction. */
  public static final short IMAGE_REL_ALPHA_HINT = (short) 0x0008;
  /** The target's 32-bit virtual address split into high and low
      16-bit parts. Either an ABSOLUTE or MATCH relocation must
      immediately follow this relocation. The high 16 bits of the
      target address are stored in the location identified by the
      INLINE_REFLONG relocation. The low 16 bits are stored four bytes
      later if the following relocation is of type ABSOLUTE or at a
      signed displacement given in the SymbolTableIndex if the
      following relocation is of type MATCH. */
  public static final short IMAGE_REL_ALPHA_INLINE_REFLONG = (short) 0x0009;
  /** The high 16 bits of the target's 32-bit virtual address. Used
      for the first instruction in a two-instruction sequence that
      loads a full address. This relocation must be immediately
      followed by a PAIR relocations whose SymbolTableIndex contains a
      signed 16-bit displacement which is added to the upper 16 bits
      taken from the location being relocated. */
  public static final short IMAGE_REL_ALPHA_REFHI = (short) 0x000A;
  /** The low 16 bits of the target's virtual address. */
  public static final short IMAGE_REL_ALPHA_REFLO = (short) 0x000B;
  /** This relocation is only valid when it immediately follows a
      REFHI , REFQ3, REFQ2, or SECRELHI relocation. Its
      SymbolTableIndex contains a displacement and not an index into
      the symbol table. */
  public static final short IMAGE_REL_ALPHA_PAIR = (short) 0x000C;
  /** This relocation is only valid when it immediately follows
      INLINE_REFLONG relocation. Its SymbolTableIndex contains the
      displacement in bytes of the location for the matching low
      address and not an index into the symbol table. */
  public static final short IMAGE_REL_ALPHA_MATCH = (short) 0x000D;
  /** The 16-bit section index of the section containing the target.
      This is used to support debugging information. */
  public static final short IMAGE_REL_ALPHA_SECTION = (short) 0x000E;
  /** The 32-bit offset of the target from the beginning of its
      section. This is used to support debugging information as well
      as static thread local storage. */
  public static final short IMAGE_REL_ALPHA_SECREL = (short) 0x000F;
  /** The target's 32-bit relative virtual address. */
  public static final short IMAGE_REL_ALPHA_REFLONGNB = (short) 0x0010;
  /** The low 16 bits of the 32-bit offset of the target from the
      beginning of its section. */
  public static final short IMAGE_REL_ALPHA_SECRELLO = (short) 0x0011;
  /** The high 16 bits of the 32-bit offset of the target from the
      beginning of its section. A PAIR relocation must immediately
      follow this on. The SymbolTableIndex of the PAIR relocation
      contains a signed 16-bit displacement which is added to the
      upper 16 bits taken from the location being relocated. */
  public static final short IMAGE_REL_ALPHA_SECRELHI = (short) 0x0012;
  /** The low 16 bits of the high 32 bits of the target's 64-bit
      virtual address. This relocation must be immediately followed by
      a PAIR relocations whose SymbolTableIndex contains a signed
      32-bit displacement which is added to the 16 bits taken from the
      location being relocated. The 16 bits in the relocated location
      are shifted left by 32 before this addition. */
  public static final short IMAGE_REL_ALPHA_REFQ3 = (short) 0x0013;
  /** The high 16 bits of the low 32 bits of the target's 64-bit
      virtual address. This relocation must be immediately followed by
      a PAIR relocations whose SymbolTableIndex contains a signed
      16-bit displacement which is added to the upper 16 bits taken
      from the location being relocated. */
  public static final short IMAGE_REL_ALPHA_REFQ2 = (short) 0x0014;
  /** The low 16 bits of the target's 64-bit virtual address. */
  public static final short IMAGE_REL_ALPHA_REFQ1 = (short) 0x0015;
  /** The low 16 bits of the 32-bit signed displacement of the target
      relative to the Global Pointer (GP) register. */
  public static final short IMAGE_REL_ALPHA_GPRELLO = (short) 0x0016;
  /** The high 16 bits of the 32-bit signed displacement of the target
      relative to the Global Pointer (GP) register. */
  public static final short IMAGE_REL_ALPHA_GPRELHI = (short) 0x0017;

  //
  // PowerPC processors
  //

  /** This relocation is ignored. */
  public static final short IMAGE_REL_PPC_ABSOLUTE = (short) 0x0000;
  /** The target's 64-bit virtual address. */
  public static final short IMAGE_REL_PPC_ADDR64 = (short) 0x0001;
  /** The target's 32-bit virtual address. */
  public static final short IMAGE_REL_PPC_ADDR32 = (short) 0x0002;
  /** The low 24 bits of the target's virtual address. This is only
      valid when the target symbol is absolute and can be sign
      extended to its original value. */
  public static final short IMAGE_REL_PPC_ADDR24 = (short) 0x0003;
  /** The low 16 bits of the target's virtual address. */
  public static final short IMAGE_REL_PPC_ADDR16 = (short) 0x0004;
  /** The low 14 bits of the target's virtual address. This is only
      valid when the target symbol is absolute and can be sign
      extended to its original value. */
  public static final short IMAGE_REL_PPC_ADDR14 = (short) 0x0005;
  /** A 24-bit PC-relative offset to the symbol's location. */
  public static final short IMAGE_REL_PPC_REL24 = (short) 0x0006;
  /** A 14-bit PC-relative offset to the symbol's location. */
  public static final short IMAGE_REL_PPC_REL14 = (short) 0x0007;
  /** The target's 32-bit relative virtual address. */
  public static final short IMAGE_REL_PPC_ADDR32NB = (short) 0x000A;
  /** The 32-bit offset of the target from the beginning of its
      section. This is used to support debugging information as well
      as static thread local storage. */
  public static final short IMAGE_REL_PPC_SECREL = (short) 0x000B;
  /** The 16-bit section index of the section containing the target.
      This is used to support debugging information. */
  public static final short IMAGE_REL_PPC_SECTION = (short) 0x000C;
  /** The 16-bit offset of the target from the beginning of its
      section. This is used to support debugging information as well
      as static thread local storage. */
  public static final short IMAGE_REL_PPC_SECREL16 = (short) 0x000F;
  /** The high 16 bits of the target's 32-bit virtual address. Used
      for the first instruction in a two-instruction sequence that
      loads a full address. This relocation must be immediately
      followed by a PAIR relocations whose SymbolTableIndex contains a
      signed 16-bit displacement which is added to the upper 16 bits
      taken from the location being relocated. */
  public static final short IMAGE_REL_PPC_REFHI = (short) 0x0010;
  /** The low 16 bits of the target's virtual address. */
  public static final short IMAGE_REL_PPC_REFLO = (short) 0x0011;
  /** This relocation is only valid when it immediately follows a
      REFHI or SECRELHI relocation. Its SymbolTableIndex contains a
      displacement and not an index into the symbol table. */
  public static final short IMAGE_REL_PPC_PAIR = (short) 0x0012;
  /** The low 16 bits of the 32-bit offset of the target from the
      beginning of its section. */
  public static final short IMAGE_REL_PPC_SECRELLO = (short) 0x0013;
  /** The high 16 bits of the 32-bit offset of the target from the
      beginning of its section. A PAIR relocation must immediately
      follow this on. The SymbolTableIndex of the PAIR relocation
      contains a signed 16-bit displacement which is added to the
      upper 16 bits taken from the location being relocated. */
  public static final short IMAGE_REL_PPC_SECRELHI = (short) 0x0014;
  /** 16-bit signed displacement of the target relative to the Global
      Pointer (GP) register. */
  public static final short IMAGE_REL_PPC_GPREL = (short) 0x0015;

  //
  // SH3 and SH4 processors
  //

  /** This relocation is ignored. */
  public static final short IMAGE_REL_SH3_ABSOLUTE = (short) 0x0000;
  /** Reference to the 16-bit location that contains the virtual
      address of the target symbol. */
  public static final short IMAGE_REL_SH3_DIRECT16 = (short) 0x0001;
  /** The target's 32-bit virtual address. */
  public static final short IMAGE_REL_SH3_DIRECT32 = (short) 0x0002;
  /** Reference to the 8-bit location that contains the virtual
      address of the target symbol. */
  public static final short IMAGE_REL_SH3_DIRECT8 = (short) 0x0003;
  /** Reference to the 8-bit instruction that contains the effective
      16-bit virtual address of the target symbol. */
  public static final short IMAGE_REL_SH3_DIRECT8_WORD = (short) 0x0004;
  /** Reference to the 8-bit instruction that contains the effective
      32-bit virtual address of the target symbol. */
  public static final short IMAGE_REL_SH3_DIRECT8_LONG = (short) 0x0005;
  /** Reference to the 8-bit location whose low 4 bits contain the
      virtual address of the target symbol. */
  public static final short IMAGE_REL_SH3_DIRECT4 = (short) 0x0006;
  /** Reference to the 8-bit instruction whose low 4 bits contain the
      effective 16-bit virtual address of the target symbol. */
  public static final short IMAGE_REL_SH3_DIRECT4_WORD = (short) 0x0007;
  /** Reference to the 8-bit instruction whose low 4 bits contain the
      effective 32-bit virtual address of the target symbol. */
  public static final short IMAGE_REL_SH3_DIRECT4_LONG = (short) 0x0008;
  /** Reference to the 8-bit instruction which contains the effective
      16-bit relative offset of the target symbol. */
  public static final short IMAGE_REL_SH3_PCREL8_WORD = (short) 0x0009;
  /** Reference to the 8-bit instruction which contains the effective
      32-bit relative offset of the target symbol. */
  public static final short IMAGE_REL_SH3_PCREL8_LONG = (short) 0x000A;
  /** Reference to the 16-bit instruction whose low 12 bits contain
      the effective 16-bit relative offset of the target symbol. */
  public static final short IMAGE_REL_SH3_PCREL12_WORD = (short) 0x000B;
  /** Reference to a 32-bit location that is the virtual address of
      the symbol's section. */
  public static final short IMAGE_REL_SH3_STARTOF_SECTION = (short) 0x000C;
  /** Reference to the 32-bit location that is the size of the
      symbol's section. */
  public static final short IMAGE_REL_SH3_SIZEOF_SECTION = (short) 0x000D;
  /** The 16-bit section index of the section containing the target.
      This is used to support debugging information. */
  public static final short IMAGE_REL_SH3_SECTION = (short) 0x000E;
  /** The 32-bit offset of the target from the beginning of its
      section. This is used to support debugging information as well
      as static thread local storage. */
  public static final short IMAGE_REL_SH3_SECREL = (short) 0x000F;
  /** The target's 32-bit relative virtual address. */
  public static final short IMAGE_REL_SH3_DIRECT32_NB = (short) 0x0010;

  //
  // ARM processors
  //

  /** This relocation is ignored. */
  public static final short IMAGE_REL_ARM_ABSOLUTE = (short) 0x0000;
  /** The target's 32-bit virtual address. */
  public static final short IMAGE_REL_ARM_ADDR32 = (short) 0x0001;
  /** The target's 32-bit relative virtual address. */
  public static final short IMAGE_REL_ARM_ADDR32NB = (short) 0x0002;
  /** The 24-bit relative displacement to the target.  */
  public static final short IMAGE_REL_ARM_BRANCH24 = (short) 0x0003;
  /** Reference to a subroutine call, consisting of two 16-bit
      instructions with 11-bit offsets. */
  public static final short IMAGE_REL_ARM_BRANCH11 = (short) 0x0004;
  /** The 16-bit section index of the section containing the target.
      This is used to support debugging information. */
  public static final short IMAGE_REL_ARM_SECTION = (short) 0x000E;
  /** The 32-bit offset of the target from the beginning of its
      section. This is used to support debugging information as well
      as static thread local storage. */
  public static final short IMAGE_REL_ARM_SECREL = (short) 0x000F;
}
