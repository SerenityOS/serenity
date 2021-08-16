/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */

package jdk.internal.foreign;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.internal.loader.NativeLibraries;
import jdk.internal.loader.NativeLibrary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static jdk.incubator.foreign.CLinker.C_POINTER;

public class SystemLookup implements SymbolLookup {

    private SystemLookup() { }

    final static SystemLookup INSTANCE = new SystemLookup();

    /*
     * On POSIX systems, dlsym will allow us to lookup symbol in library dependencies; the same trick doesn't work
     * on Windows. For this reason, on Windows we do not generate any side-library, and load msvcrt.dll directly instead.
     */
    private static final SymbolLookup syslookup = switch (CABI.current()) {
        case SysV, LinuxAArch64, MacOsAArch64 -> libLookup(libs -> libs.loadLibrary("syslookup"));
        case Win64 -> makeWindowsLookup(); // out of line to workaround javac crash
    };

    private static SymbolLookup makeWindowsLookup() {
        Path system32 = Path.of(System.getenv("SystemRoot"), "System32");
        Path ucrtbase = system32.resolve("ucrtbase.dll");
        Path msvcrt = system32.resolve("msvcrt.dll");

        boolean useUCRT = Files.exists(ucrtbase);
        Path stdLib = useUCRT ? ucrtbase : msvcrt;
        SymbolLookup lookup = libLookup(libs -> libs.loadLibrary(null, stdLib.toFile()));

        if (useUCRT) {
            // use a fallback lookup to look up inline functions from fallback lib

            SymbolLookup fallbackLibLookup = libLookup(libs -> libs.loadLibrary("WinFallbackLookup"));

            int numSymbols = WindowsFallbackSymbols.values().length;
            MemorySegment funcs = fallbackLibLookup.lookup("funcs").orElseThrow()
                .asSegment(C_POINTER.byteSize() * numSymbols, ResourceScope.newImplicitScope());

            SymbolLookup fallbackLookup = name -> Optional.ofNullable(WindowsFallbackSymbols.valueOfOrNull(name))
                .map(symbol -> MemoryAccess.getAddressAtIndex(funcs, symbol.ordinal()));

            final SymbolLookup finalLookup = lookup;
            lookup = name -> finalLookup.lookup(name).or(() -> fallbackLookup.lookup(name));
        }

        return lookup;
    }

    private static SymbolLookup libLookup(Function<NativeLibraries, NativeLibrary> loader) {
        NativeLibrary lib = loader.apply(NativeLibraries.rawNativeLibraries(SystemLookup.class, false));
        return name -> {
            Objects.requireNonNull(name);
            try {
                long addr = lib.lookup(name);
                return addr == 0 ?
                        Optional.empty() : Optional.of(MemoryAddress.ofLong(addr));
            } catch (NoSuchMethodException e) {
                return Optional.empty();
            }
        };
    }

    @Override
    public Optional<MemoryAddress> lookup(String name) {
        return syslookup.lookup(name);
    }

    public static SystemLookup getInstance() {
        return INSTANCE;
    }

    // fallback symbols missing from ucrtbase.dll
    // this list has to be kept in sync with the table in the companion native library
    private enum WindowsFallbackSymbols {
        // stdio
        fprintf,
        fprintf_s,
        fscanf,
        fscanf_s,
        fwprintf,
        fwprintf_s,
        fwscanf,
        fwscanf_s,
        printf,
        printf_s,
        scanf,
        scanf_s,
        snprintf,
        sprintf,
        sprintf_s,
        sscanf,
        sscanf_s,
        swprintf,
        swprintf_s,
        swscanf,
        swscanf_s,
        vfprintf,
        vfprintf_s,
        vfscanf,
        vfscanf_s,
        vfwprintf,
        vfwprintf_s,
        vfwscanf,
        vfwscanf_s,
        vprintf,
        vprintf_s,
        vscanf,
        vscanf_s,
        vsnprintf,
        vsnprintf_s,
        vsprintf,
        vsprintf_s,
        vsscanf,
        vsscanf_s,
        vswprintf,
        vswprintf_s,
        vswscanf,
        vswscanf_s,
        vwprintf,
        vwprintf_s,
        vwscanf,
        vwscanf_s,
        wprintf,
        wprintf_s,
        wscanf,
        wscanf_s,

        // time
        gmtime
        ;

        static WindowsFallbackSymbols valueOfOrNull(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
