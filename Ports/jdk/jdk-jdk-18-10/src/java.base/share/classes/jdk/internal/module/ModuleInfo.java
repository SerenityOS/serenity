/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jdk.internal.access.JavaLangModuleAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.VM;

import static jdk.internal.module.ClassFileConstants.*;


/**
 * Read module information from a {@code module-info} class file.
 *
 * @implNote The rationale for the hand-coded reader is startup performance
 * and fine control over the throwing of InvalidModuleDescriptorException.
 */

public final class ModuleInfo {

    private static final JavaLangModuleAccess JLMA
        = SharedSecrets.getJavaLangModuleAccess();

    // supplies the set of packages when ModulePackages attribute not present
    private final Supplier<Set<String>> packageFinder;

    // indicates if the ModuleHashes attribute should be parsed
    private final boolean parseHashes;

    private ModuleInfo(Supplier<Set<String>> pf, boolean ph) {
        packageFinder = pf;
        parseHashes = ph;
    }

    private ModuleInfo(Supplier<Set<String>> pf) {
        this(pf, true);
    }

    /**
     * A holder class for the ModuleDescriptor that is created by reading the
     * Module and other standard class file attributes. It also holds the objects
     * that represent the non-standard class file attributes that are read from
     * the class file.
     */
    public static final class Attributes {
        private final ModuleDescriptor descriptor;
        private final ModuleTarget target;
        private final ModuleHashes recordedHashes;
        private final ModuleResolution moduleResolution;
        Attributes(ModuleDescriptor descriptor,
                   ModuleTarget target,
                   ModuleHashes recordedHashes,
                   ModuleResolution moduleResolution) {
            this.descriptor = descriptor;
            this.target = target;
            this.recordedHashes = recordedHashes;
            this.moduleResolution = moduleResolution;
        }
        public ModuleDescriptor descriptor() {
            return descriptor;
        }
        public ModuleTarget target() {
            return target;
        }
        public ModuleHashes recordedHashes() {
            return recordedHashes;
        }
        public ModuleResolution moduleResolution() {
            return moduleResolution;
        }
    }


    /**
     * Reads a {@code module-info.class} from the given input stream.
     *
     * @throws InvalidModuleDescriptorException
     * @throws IOException
     */
    public static Attributes read(InputStream in, Supplier<Set<String>> pf)
        throws IOException
    {
        try {
            return new ModuleInfo(pf).doRead(new DataInputStream(in));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw invalidModuleDescriptor(e.getMessage());
        } catch (EOFException x) {
            throw truncatedModuleDescriptor();
        }
    }

    /**
     * Reads a {@code module-info.class} from the given byte buffer.
     *
     * @throws InvalidModuleDescriptorException
     * @throws UncheckedIOException
     */
    public static Attributes read(ByteBuffer bb, Supplier<Set<String>> pf) {
        try {
            return new ModuleInfo(pf).doRead(new DataInputWrapper(bb));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw invalidModuleDescriptor(e.getMessage());
        } catch (EOFException x) {
            throw truncatedModuleDescriptor();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Reads a {@code module-info.class} from the given byte buffer
     * but ignore the {@code ModuleHashes} attribute.
     *
     * @throws InvalidModuleDescriptorException
     * @throws UncheckedIOException
     */
    public static Attributes readIgnoringHashes(ByteBuffer bb, Supplier<Set<String>> pf) {
        try {
            return new ModuleInfo(pf, false).doRead(new DataInputWrapper(bb));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw invalidModuleDescriptor(e.getMessage());
        } catch (EOFException x) {
            throw truncatedModuleDescriptor();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Reads the input as a module-info class file.
     *
     * @throws IOException
     * @throws InvalidModuleDescriptorException
     * @throws IllegalArgumentException if thrown by the ModuleDescriptor.Builder
     *         because an identifier is not a legal Java identifier, duplicate
     *         exports, and many other reasons
     */
    private Attributes doRead(DataInput input) throws IOException {
        var in = new CountingDataInput(input);

        int magic = in.readInt();
        if (magic != 0xCAFEBABE)
            throw invalidModuleDescriptor("Bad magic number");

        int minor_version = in.readUnsignedShort();
        int major_version = in.readUnsignedShort();
        if (!VM.isSupportedModuleDescriptorVersion(major_version, minor_version)) {
            throw invalidModuleDescriptor("Unsupported major.minor version "
                                          + major_version + "." + minor_version);
        }

        ConstantPool cpool = new ConstantPool(in);

        int access_flags = in.readUnsignedShort();
        if (access_flags != ACC_MODULE)
            throw invalidModuleDescriptor("access_flags should be ACC_MODULE");

        int this_class = in.readUnsignedShort();
        String mn = cpool.getClassName(this_class);
        if (!"module-info".equals(mn))
            throw invalidModuleDescriptor("this_class should be module-info");

        int super_class = in.readUnsignedShort();
        if (super_class > 0)
            throw invalidModuleDescriptor("bad #super_class");

        int interfaces_count = in.readUnsignedShort();
        if (interfaces_count > 0)
            throw invalidModuleDescriptor("Bad #interfaces");

        int fields_count = in.readUnsignedShort();
        if (fields_count > 0)
            throw invalidModuleDescriptor("Bad #fields");

        int methods_count = in.readUnsignedShort();
        if (methods_count > 0)
            throw invalidModuleDescriptor("Bad #methods");

        int attributes_count = in.readUnsignedShort();

        // the names of the attributes found in the class file
        Set<String> attributes = new HashSet<>();

        Builder builder = null;
        Set<String> allPackages = null;
        String mainClass = null;
        ModuleTarget moduleTarget = null;
        ModuleHashes moduleHashes = null;
        ModuleResolution moduleResolution = null;

        for (int i = 0; i < attributes_count ; i++) {
            int name_index = in.readUnsignedShort();
            String attribute_name = cpool.getUtf8(name_index);
            int length = in.readInt();

            boolean added = attributes.add(attribute_name);
            if (!added && isAttributeAtMostOnce(attribute_name)) {
                throw invalidModuleDescriptor("More than one "
                                              + attribute_name + " attribute");
            }

            long initialPosition = in.count();

            switch (attribute_name) {
                case MODULE :
                    builder = readModuleAttribute(in, cpool, major_version);
                    break;

                case MODULE_PACKAGES :
                    allPackages = readModulePackagesAttribute(in, cpool);
                    break;

                case MODULE_MAIN_CLASS :
                    mainClass = readModuleMainClassAttribute(in, cpool);
                    break;

                case MODULE_TARGET :
                    moduleTarget = readModuleTargetAttribute(in, cpool);
                    break;

                case MODULE_HASHES :
                    if (parseHashes) {
                        moduleHashes = readModuleHashesAttribute(in, cpool);
                    } else {
                        in.skipBytes(length);
                    }
                    break;

                case MODULE_RESOLUTION :
                    moduleResolution = readModuleResolution(in, cpool);
                    break;

                default:
                    if (isAttributeDisallowed(attribute_name)) {
                        throw invalidModuleDescriptor(attribute_name
                                                      + " attribute not allowed");
                    } else {
                        in.skipBytes(length);
                    }
            }

            long newPosition = in.count();
            if ((newPosition - initialPosition) != length) {
                // attribute length does not match actual attribute size
                throw invalidModuleDescriptor("Attribute " + attribute_name
                        + " does not match its expected length");
            }

        }

        // the Module attribute is required
        if (builder == null) {
            throw invalidModuleDescriptor(MODULE + " attribute not found");
        }

        // ModuleMainClass  attribute
        if (mainClass != null) {
            builder.mainClass(mainClass);
        }

        // If the ModulePackages attribute is not present then the packageFinder
        // is used to find the set of packages
        boolean usedPackageFinder = false;
        if (allPackages == null && packageFinder != null) {
            try {
                allPackages = packageFinder.get();
            } catch (UncheckedIOException x) {
                throw x.getCause();
            }
            usedPackageFinder = true;
        }
        if (allPackages != null) {
            Set<String> knownPackages = JLMA.packages(builder);
            if (!allPackages.containsAll(knownPackages)) {
                Set<String> missingPackages = new HashSet<>(knownPackages);
                missingPackages.removeAll(allPackages);
                assert !missingPackages.isEmpty();
                String missingPackage = missingPackages.iterator().next();
                String tail;
                if (usedPackageFinder) {
                    tail = " not found in module";
                } else {
                    tail = " missing from ModulePackages class file attribute";
                }
                throw invalidModuleDescriptor("Package " + missingPackage + tail);

            }
            builder.packages(allPackages);
        }

        ModuleDescriptor descriptor = builder.build();
        return new Attributes(descriptor,
                              moduleTarget,
                              moduleHashes,
                              moduleResolution);
    }

    /**
     * Reads the Module attribute, returning the ModuleDescriptor.Builder to
     * build the corresponding ModuleDescriptor.
     */
    private Builder readModuleAttribute(DataInput in, ConstantPool cpool, int major)
        throws IOException
    {
        // module_name
        int module_name_index = in.readUnsignedShort();
        String mn = cpool.getModuleName(module_name_index);

        int module_flags = in.readUnsignedShort();

        Set<ModuleDescriptor.Modifier> modifiers = new HashSet<>();
        boolean open = ((module_flags & ACC_OPEN) != 0);
        if (open)
            modifiers.add(ModuleDescriptor.Modifier.OPEN);
        if ((module_flags & ACC_SYNTHETIC) != 0)
            modifiers.add(ModuleDescriptor.Modifier.SYNTHETIC);
        if ((module_flags & ACC_MANDATED) != 0)
            modifiers.add(ModuleDescriptor.Modifier.MANDATED);

        Builder builder = JLMA.newModuleBuilder(mn, false, modifiers);

        int module_version_index = in.readUnsignedShort();
        if (module_version_index != 0) {
            String vs = cpool.getUtf8(module_version_index);
            builder.version(vs);
        }

        int requires_count = in.readUnsignedShort();
        boolean requiresJavaBase = false;
        for (int i=0; i<requires_count; i++) {
            int requires_index = in.readUnsignedShort();
            String dn = cpool.getModuleName(requires_index);

            int requires_flags = in.readUnsignedShort();
            Set<Requires.Modifier> mods;
            if (requires_flags == 0) {
                mods = Set.of();
            } else {
                mods = new HashSet<>();
                if ((requires_flags & ACC_TRANSITIVE) != 0)
                    mods.add(Requires.Modifier.TRANSITIVE);
                if ((requires_flags & ACC_STATIC_PHASE) != 0)
                    mods.add(Requires.Modifier.STATIC);
                if ((requires_flags & ACC_SYNTHETIC) != 0)
                    mods.add(Requires.Modifier.SYNTHETIC);
                if ((requires_flags & ACC_MANDATED) != 0)
                    mods.add(Requires.Modifier.MANDATED);
            }

            int requires_version_index = in.readUnsignedShort();
            if (requires_version_index == 0) {
                builder.requires(mods, dn);
            } else {
                String vs = cpool.getUtf8(requires_version_index);
                JLMA.requires(builder, mods, dn, vs);
            }

            if (dn.equals("java.base")) {
                if (major >= 54
                    && (mods.contains(Requires.Modifier.TRANSITIVE)
                        || mods.contains(Requires.Modifier.STATIC))) {
                    String flagName;
                    if (mods.contains(Requires.Modifier.TRANSITIVE)) {
                        flagName = "ACC_TRANSITIVE";
                    } else {
                        flagName = "ACC_STATIC_PHASE";
                    }
                    throw invalidModuleDescriptor("The requires entry for java.base"
                                                  + " has " + flagName + " set");
                }
                requiresJavaBase = true;
            }
        }
        if (mn.equals("java.base")) {
            if (requires_count > 0) {
                throw invalidModuleDescriptor("The requires table for java.base"
                                              + " must be 0 length");
            }
        } else if (!requiresJavaBase) {
            throw invalidModuleDescriptor("The requires table must have"
                                          + " an entry for java.base");
        }

        int exports_count = in.readUnsignedShort();
        if (exports_count > 0) {
            for (int i=0; i<exports_count; i++) {
                int exports_index = in.readUnsignedShort();
                String pkg = cpool.getPackageName(exports_index);

                Set<Exports.Modifier> mods;
                int exports_flags = in.readUnsignedShort();
                if (exports_flags == 0) {
                    mods = Set.of();
                } else {
                    mods = new HashSet<>();
                    if ((exports_flags & ACC_SYNTHETIC) != 0)
                        mods.add(Exports.Modifier.SYNTHETIC);
                    if ((exports_flags & ACC_MANDATED) != 0)
                        mods.add(Exports.Modifier.MANDATED);
                }

                int exports_to_count = in.readUnsignedShort();
                if (exports_to_count > 0) {
                    Set<String> targets = new HashSet<>(exports_to_count);
                    for (int j=0; j<exports_to_count; j++) {
                        int exports_to_index = in.readUnsignedShort();
                        String target = cpool.getModuleName(exports_to_index);
                        if (!targets.add(target)) {
                            throw invalidModuleDescriptor(pkg + " exported to "
                                                          + target + " more than once");
                        }
                    }
                    builder.exports(mods, pkg, targets);
                } else {
                    builder.exports(mods, pkg);
                }
            }
        }

        int opens_count = in.readUnsignedShort();
        if (opens_count > 0) {
            if (open) {
                throw invalidModuleDescriptor("The opens table for an open"
                                              + " module must be 0 length");
            }
            for (int i=0; i<opens_count; i++) {
                int opens_index = in.readUnsignedShort();
                String pkg = cpool.getPackageName(opens_index);

                Set<Opens.Modifier> mods;
                int opens_flags = in.readUnsignedShort();
                if (opens_flags == 0) {
                    mods = Set.of();
                } else {
                    mods = new HashSet<>();
                    if ((opens_flags & ACC_SYNTHETIC) != 0)
                        mods.add(Opens.Modifier.SYNTHETIC);
                    if ((opens_flags & ACC_MANDATED) != 0)
                        mods.add(Opens.Modifier.MANDATED);
                }

                int open_to_count = in.readUnsignedShort();
                if (open_to_count > 0) {
                    Set<String> targets = new HashSet<>(open_to_count);
                    for (int j=0; j<open_to_count; j++) {
                        int opens_to_index = in.readUnsignedShort();
                        String target = cpool.getModuleName(opens_to_index);
                        if (!targets.add(target)) {
                            throw invalidModuleDescriptor(pkg + " opened to "
                                                          + target + " more than once");
                        }
                    }
                    builder.opens(mods, pkg, targets);
                } else {
                    builder.opens(mods, pkg);
                }
            }
        }

        int uses_count = in.readUnsignedShort();
        if (uses_count > 0) {
            for (int i=0; i<uses_count; i++) {
                int index = in.readUnsignedShort();
                String sn = cpool.getClassName(index);
                builder.uses(sn);
            }
        }

        int provides_count = in.readUnsignedShort();
        if (provides_count > 0) {
            for (int i=0; i<provides_count; i++) {
                int index = in.readUnsignedShort();
                String sn = cpool.getClassName(index);
                int with_count = in.readUnsignedShort();
                List<String> providers = new ArrayList<>(with_count);
                for (int j=0; j<with_count; j++) {
                    index = in.readUnsignedShort();
                    String pn = cpool.getClassName(index);
                    if (!providers.add(pn)) {
                        throw invalidModuleDescriptor(sn + " provides " + pn
                                                      + " more than once");
                    }
                }
                builder.provides(sn, providers);
            }
        }

        return builder;
    }

    /**
     * Reads the ModulePackages attribute
     */
    private Set<String> readModulePackagesAttribute(DataInput in, ConstantPool cpool)
        throws IOException
    {
        int package_count = in.readUnsignedShort();
        Set<String> packages = new HashSet<>(package_count);
        for (int i=0; i<package_count; i++) {
            int index = in.readUnsignedShort();
            String pn = cpool.getPackageName(index);
            boolean added = packages.add(pn);
            if (!added) {
                throw invalidModuleDescriptor("Package " + pn + " in ModulePackages"
                                              + "attribute more than once");
            }
        }
        return packages;
    }

    /**
     * Reads the ModuleMainClass attribute
     */
    private String readModuleMainClassAttribute(DataInput in, ConstantPool cpool)
        throws IOException
    {
        int index = in.readUnsignedShort();
        return cpool.getClassName(index);
    }

    /**
     * Reads the ModuleTarget attribute
     */
    private ModuleTarget readModuleTargetAttribute(DataInput in, ConstantPool cpool)
        throws IOException
    {
        String targetPlatform = null;

        int index = in.readUnsignedShort();
        if (index != 0)
            targetPlatform = cpool.getUtf8(index);

        return new ModuleTarget(targetPlatform);
    }

    /**
     * Reads the ModuleHashes attribute
     */
    private ModuleHashes readModuleHashesAttribute(DataInput in, ConstantPool cpool)
        throws IOException
    {
        int algorithm_index = in.readUnsignedShort();
        String algorithm = cpool.getUtf8(algorithm_index);

        int hash_count = in.readUnsignedShort();
        Map<String, byte[]> map = new HashMap<>(hash_count);
        for (int i=0; i<hash_count; i++) {
            int module_name_index = in.readUnsignedShort();
            String mn = cpool.getModuleName(module_name_index);
            int hash_length = in.readUnsignedShort();
            if (hash_length == 0) {
                throw invalidModuleDescriptor("hash_length == 0");
            }
            byte[] hash = new byte[hash_length];
            in.readFully(hash);
            map.put(mn, hash);
        }

        return new ModuleHashes(algorithm, map);
    }

    /**
     * Reads the ModuleResolution attribute.
     */
    private ModuleResolution readModuleResolution(DataInput in,
                                                  ConstantPool cpool)
        throws IOException
    {
        int flags = in.readUnsignedShort();

        int reason = 0;
        if ((flags & WARN_DEPRECATED) != 0)
            reason = WARN_DEPRECATED;
        if ((flags & WARN_DEPRECATED_FOR_REMOVAL) != 0) {
            if (reason != 0)
                throw invalidModuleDescriptor("Bad module resolution flags:" + flags);
            reason = WARN_DEPRECATED_FOR_REMOVAL;
        }
        if ((flags & WARN_INCUBATING) != 0) {
            if (reason != 0)
                throw invalidModuleDescriptor("Bad module resolution flags:" + flags);
        }

        return new ModuleResolution(flags);
    }

    /**
     * Returns true if the given attribute can be present at most once
     * in the class file. Returns false otherwise.
     */
    private static boolean isAttributeAtMostOnce(String name) {

        if (name.equals(MODULE) ||
                name.equals(SOURCE_FILE) ||
                name.equals(SDE) ||
                name.equals(MODULE_PACKAGES) ||
                name.equals(MODULE_MAIN_CLASS) ||
                name.equals(MODULE_TARGET) ||
                name.equals(MODULE_HASHES) ||
                name.equals(MODULE_RESOLUTION))
            return true;

        return false;
    }

    /**
     * Return true if the given attribute name is the name of a pre-defined
     * attribute in JVMS 4.7 that is not allowed in a module-info class.
     */
    private static boolean isAttributeDisallowed(String name) {
        Set<String> notAllowed = predefinedNotAllowed;
        if (notAllowed == null) {
            notAllowed = Set.of(
                    "ConstantValue",
                    "Code",
                    "Deprecated",
                    "StackMapTable",
                    "Exceptions",
                    "EnclosingMethod",
                    "Signature",
                    "LineNumberTable",
                    "LocalVariableTable",
                    "LocalVariableTypeTable",
                    "RuntimeVisibleParameterAnnotations",
                    "RuntimeInvisibleParameterAnnotations",
                    "RuntimeVisibleTypeAnnotations",
                    "RuntimeInvisibleTypeAnnotations",
                    "Synthetic",
                    "AnnotationDefault",
                    "BootstrapMethods",
                    "MethodParameters");
            predefinedNotAllowed = notAllowed;
        }
        return notAllowed.contains(name);
    }

    // lazily created set the pre-defined attributes that are not allowed
    private static volatile Set<String> predefinedNotAllowed;


    /**
     * The constant pool in a class file.
     */
    private static class ConstantPool {
        static final int CONSTANT_Utf8 = 1;
        static final int CONSTANT_Integer = 3;
        static final int CONSTANT_Float = 4;
        static final int CONSTANT_Long = 5;
        static final int CONSTANT_Double = 6;
        static final int CONSTANT_Class = 7;
        static final int CONSTANT_String = 8;
        static final int CONSTANT_Fieldref = 9;
        static final int CONSTANT_Methodref = 10;
        static final int CONSTANT_InterfaceMethodref = 11;
        static final int CONSTANT_NameAndType = 12;
        static final int CONSTANT_MethodHandle = 15;
        static final int CONSTANT_MethodType = 16;
        static final int CONSTANT_InvokeDynamic = 18;
        static final int CONSTANT_Module = 19;
        static final int CONSTANT_Package = 20;

        private static class Entry {
            protected Entry(int tag) {
                this.tag = tag;
            }
            final int tag;
        }

        private static class IndexEntry extends Entry {
            IndexEntry(int tag, int index) {
                super(tag);
                this.index = index;
            }
            final int index;
        }

        private static class Index2Entry extends Entry {
            Index2Entry(int tag, int index1, int index2) {
                super(tag);
                this.index1 = index1;
                this.index2 = index2;
            }
            final int index1,  index2;
        }

        private static class ValueEntry extends Entry {
            ValueEntry(int tag, Object value) {
                super(tag);
                this.value = value;
            }
            final Object value;
        }

        final Entry[] pool;

        ConstantPool(DataInput in) throws IOException {
            int count = in.readUnsignedShort();
            pool = new Entry[count];

            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {

                    case CONSTANT_Utf8:
                        String svalue = in.readUTF();
                        pool[i] = new ValueEntry(tag, svalue);
                        break;

                    case CONSTANT_Class:
                    case CONSTANT_Package:
                    case CONSTANT_Module:
                    case CONSTANT_String:
                        int index = in.readUnsignedShort();
                        pool[i] = new IndexEntry(tag, index);
                        break;

                    case CONSTANT_Double:
                        double dvalue = in.readDouble();
                        pool[i] = new ValueEntry(tag, dvalue);
                        i++;
                        break;

                    case CONSTANT_Fieldref:
                    case CONSTANT_InterfaceMethodref:
                    case CONSTANT_Methodref:
                    case CONSTANT_InvokeDynamic:
                    case CONSTANT_NameAndType:
                        int index1 = in.readUnsignedShort();
                        int index2 = in.readUnsignedShort();
                        pool[i] = new Index2Entry(tag, index1, index2);
                        break;

                    case CONSTANT_MethodHandle:
                        int refKind = in.readUnsignedByte();
                        index = in.readUnsignedShort();
                        pool[i] = new Index2Entry(tag, refKind, index);
                        break;

                    case CONSTANT_MethodType:
                        index = in.readUnsignedShort();
                        pool[i] = new IndexEntry(tag, index);
                        break;

                    case CONSTANT_Float:
                        float fvalue = in.readFloat();
                        pool[i] = new ValueEntry(tag, fvalue);
                        break;

                    case CONSTANT_Integer:
                        int ivalue = in.readInt();
                        pool[i] = new ValueEntry(tag, ivalue);
                        break;

                    case CONSTANT_Long:
                        long lvalue = in.readLong();
                        pool[i] = new ValueEntry(tag, lvalue);
                        i++;
                        break;

                    default:
                        throw invalidModuleDescriptor("Bad constant pool entry: "
                                                      + i);
                }
            }
        }

        String getClassName(int index) {
            checkIndex(index);
            Entry e = pool[index];
            if (e.tag != CONSTANT_Class) {
                throw invalidModuleDescriptor("CONSTANT_Class expected at entry: "
                                              + index);
            }
            String value = getUtf8(((IndexEntry) e).index);
            checkUnqualifiedName("CONSTANT_Class", index, value);
            return value.replace('/', '.');  // internal form -> binary name
        }

        String getPackageName(int index) {
            checkIndex(index);
            Entry e = pool[index];
            if (e.tag != CONSTANT_Package) {
                throw invalidModuleDescriptor("CONSTANT_Package expected at entry: "
                                              + index);
            }
            String value = getUtf8(((IndexEntry) e).index);
            checkUnqualifiedName("CONSTANT_Package", index, value);
            return value.replace('/', '.');  // internal form -> binary name
        }

        String getModuleName(int index) {
            checkIndex(index);
            Entry e = pool[index];
            if (e.tag != CONSTANT_Module) {
                throw invalidModuleDescriptor("CONSTANT_Module expected at entry: "
                                              + index);
            }
            String value = getUtf8(((IndexEntry) e).index);
            return decodeModuleName(index, value);
        }

        String getUtf8(int index) {
            checkIndex(index);
            Entry e = pool[index];
            if (e.tag != CONSTANT_Utf8) {
                throw invalidModuleDescriptor("CONSTANT_Utf8 expected at entry: "
                                              + index);
            }
            return (String) (((ValueEntry) e).value);
        }

        void checkIndex(int index) {
            if (index < 1 || index >= pool.length)
                throw invalidModuleDescriptor("Index into constant pool out of range");
        }

        void checkUnqualifiedName(String what, int index, String value) {
            int len = value.length();
            if (len == 0) {
                throw invalidModuleDescriptor(what + " at entry " + index
                                              + " has zero length");
            }
            for (int i=0; i<len; i++) {
                char c = value.charAt(i);
                if (c == '.' || c == ';' || c == '[') {
                    throw invalidModuleDescriptor(what + " at entry " + index
                                                  + " has illegal character: '"
                                                  + c + "'");
                }
            }
        }

        /**
         * "Decode" a module name that has been read from the constant pool.
         */
        String decodeModuleName(int index, String value) {
            int len = value.length();
            if (len == 0) {
                throw invalidModuleDescriptor("CONSTANT_Module at entry "
                                              + index + " is zero length");
            }
            int i = 0;
            while (i < len) {
                int cp = value.codePointAt(i);
                if (cp == ':' || cp == '@' || cp < 0x20) {
                    throw invalidModuleDescriptor("CONSTANT_Module at entry "
                                                  + index + " has illegal character: "
                                                  + Character.getName(cp));
                }

                // blackslash is the escape character
                if (cp == '\\')
                    return decodeModuleName(index, i, value);

                i += Character.charCount(cp);
            }
            return value;
        }

        /**
         * "Decode" a module name that has been read from the constant pool and
         * partly checked for illegal characters (up to position {@code i}).
         */
        String decodeModuleName(int index, int i, String value) {
            StringBuilder sb = new StringBuilder();

            // copy the code points that have been checked
            int j = 0;
            while (j < i) {
                int cp = value.codePointAt(j);
                sb.appendCodePoint(cp);
                j += Character.charCount(cp);
            }

            // decode from position {@code i} to end
            int len = value.length();
            while (i < len) {
                int cp = value.codePointAt(i);
                if (cp == ':' || cp == '@' || cp < 0x20) {
                    throw invalidModuleDescriptor("CONSTANT_Module at entry "
                                                  + index + " has illegal character: "
                                                  + Character.getName(cp));
                }

                // blackslash is the escape character
                if (cp == '\\') {
                    j = i + Character.charCount(cp);
                    if (j >= len) {
                        throw invalidModuleDescriptor("CONSTANT_Module at entry "
                                                       + index + " has illegal "
                                                       + "escape sequence");
                    }
                    int next = value.codePointAt(j);
                    if (next != '\\' && next != ':' && next != '@') {
                        throw invalidModuleDescriptor("CONSTANT_Module at entry "
                                                      + index + " has illegal "
                                                      + "escape sequence");
                    }
                    sb.appendCodePoint(next);
                    i += Character.charCount(next);
                } else {
                    sb.appendCodePoint(cp);
                }

                i += Character.charCount(cp);
            }
            return sb.toString();
        }
    }

    /**
     * A DataInput implementation that reads from a ByteBuffer.
     */
    private static class DataInputWrapper implements DataInput {
        private final ByteBuffer bb;

        DataInputWrapper(ByteBuffer bb) {
            this.bb = bb;
        }

        @Override
        public void readFully(byte b[]) throws IOException {
            readFully(b, 0, b.length);
        }

        @Override
        public void readFully(byte b[], int off, int len) throws IOException {
            try {
                bb.get(b, off, len);
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public int skipBytes(int n) {
            int skip = Math.min(n, bb.remaining());
            bb.position(bb.position() + skip);
            return skip;
        }

        @Override
        public boolean readBoolean() throws IOException {
            try {
                int ch = bb.get();
                return (ch != 0);
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public byte readByte() throws IOException {
            try {
                return bb.get();
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public int readUnsignedByte() throws IOException {
            try {
                return ((int) bb.get()) & 0xff;
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public short readShort() throws IOException {
            try {
                return bb.getShort();
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public int readUnsignedShort() throws IOException {
            try {
                return ((int) bb.getShort()) & 0xffff;
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public char readChar() throws IOException {
            try {
                return bb.getChar();
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public int readInt() throws IOException {
            try {
                return bb.getInt();
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public long readLong() throws IOException {
            try {
                return bb.getLong();
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public float readFloat() throws IOException {
            try {
                return bb.getFloat();
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public double readDouble() throws IOException {
            try {
                return bb.getDouble();
            } catch (BufferUnderflowException e) {
                throw new EOFException(e.getMessage());
            }
        }

        @Override
        public String readLine() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public String readUTF() throws IOException {
            // ### Need to measure the performance and feasibility of using
            // the UTF-8 decoder instead.
            return DataInputStream.readUTF(this);
        }
    }

    /**
     * A DataInput implementation that reads from another DataInput and counts
     * the number of bytes read.
     */
    private static class CountingDataInput implements DataInput {
        private final DataInput delegate;
        private long count;

        CountingDataInput(DataInput delegate) {
            this.delegate = delegate;
        }

        long count() {
            return count;
        }

        @Override
        public void readFully(byte b[]) throws IOException {
            delegate.readFully(b, 0, b.length);
            count += b.length;
        }

        @Override
        public void readFully(byte b[], int off, int len) throws IOException {
            delegate.readFully(b, off, len);
            count += len;
        }

        @Override
        public int skipBytes(int n) throws IOException {
            int skip = delegate.skipBytes(n);
            count += skip;
            return skip;
        }

        @Override
        public boolean readBoolean() throws IOException {
            boolean b = delegate.readBoolean();
            count++;
            return b;
        }

        @Override
        public byte readByte() throws IOException {
            byte b = delegate.readByte();
            count++;
            return b;
        }

        @Override
        public int readUnsignedByte() throws IOException {
            int i = delegate.readUnsignedByte();
            count++;
            return i;
        }

        @Override
        public short readShort() throws IOException {
            short s = delegate.readShort();
            count += 2;
            return s;
        }

        @Override
        public int readUnsignedShort() throws IOException {
            int s = delegate.readUnsignedShort();
            count += 2;
            return s;
        }

        @Override
        public char readChar() throws IOException {
            char c = delegate.readChar();
            count += 2;
            return c;
        }

        @Override
        public int readInt() throws IOException {
            int i = delegate.readInt();
            count += 4;
            return i;
        }

        @Override
        public long readLong() throws IOException {
            long l = delegate.readLong();
            count += 8;
            return l;
        }

        @Override
        public float readFloat() throws IOException {
            float f = delegate.readFloat();
            count += 4;
            return f;
        }

        @Override
        public double readDouble() throws IOException {
            double d = delegate.readDouble();
            count += 8;
            return d;
        }

        @Override
        public String readLine() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public String readUTF() throws IOException {
            return DataInputStream.readUTF(this);
        }
    }

    /**
     * Returns an InvalidModuleDescriptorException with the given detail
     * message
     */
    private static InvalidModuleDescriptorException invalidModuleDescriptor(String msg) {
        return new InvalidModuleDescriptorException(msg);
    }

    /**
     * Returns an InvalidModuleDescriptorException with a detail message to
     * indicate that the class file is truncated.
     */
    private static InvalidModuleDescriptorException truncatedModuleDescriptor() {
        return invalidModuleDescriptor("Truncated module-info.class");
    }

}
