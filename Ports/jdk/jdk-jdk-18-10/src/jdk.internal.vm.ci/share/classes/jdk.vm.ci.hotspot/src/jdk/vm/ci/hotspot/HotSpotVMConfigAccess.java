/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 */
package jdk.vm.ci.hotspot;

import java.util.Set;
import java.util.stream.Collectors;

import jdk.vm.ci.common.JVMCIError;

/**
 * Access to VM configuration data.
 */
public class HotSpotVMConfigAccess {

    /**
     * Gets the available configuration data.
     */
    public HotSpotVMConfigStore getStore() {
        return store;
    }

    /**
     * Gets the address of a C++ symbol.
     *
     * @param name name of C++ symbol
     * @param notPresent if non-null and the symbol is not present then this value is returned
     * @return the address of the symbol
     * @throws JVMCIError if the symbol is not present and {@code notPresent == null}
     */
    public long getAddress(String name, Long notPresent) {
        Long entry = store.vmAddresses.get(name);
        if (entry == null) {
            if (notPresent != null) {
                return notPresent;
            }
            throw missingEntry("address", name, store.vmFlags.keySet());

        }
        return entry;
    }

    /**
     * Gets the address of a C++ symbol.
     *
     * @param name name of C++ symbol
     * @return the address of the symbol
     * @throws JVMCIError if the symbol is not present
     */
    public long getAddress(String name) {
        return getAddress(name, null);
    }

    /**
     * Gets the value of a C++ constant.
     *
     * @param name name of the constant (e.g., {@code "frame::arg_reg_save_area_bytes"})
     * @param type the boxed type to which the constant value will be converted
     * @param notPresent if non-null and the constant is not present then this value is returned
     * @return the constant value converted to {@code type}
     * @throws JVMCIError if the constant is not present and {@code notPresent == null}
     */
    public <T> T getConstant(String name, Class<T> type, T notPresent) {
        Long c = store.vmConstants.get(name);
        if (c == null) {
            if (notPresent != null) {
                return notPresent;
            }
            throw missingEntry("constant", name, store.vmConstants.keySet());
        }
        return type.cast(convertValue(name, type, c, null));
    }

    /**
     * Gets the value of a C++ constant.
     *
     * @param name name of the constant (e.g., {@code "frame::arg_reg_save_area_bytes"})
     * @param type the boxed type to which the constant value will be converted
     * @return the constant value converted to {@code type}
     * @throws JVMCIError if the constant is not present
     */
    public <T> T getConstant(String name, Class<T> type) {
        return getConstant(name, type, null);
    }

    /**
     * Gets the offset of a non-static C++ field.
     *
     * @param name fully qualified name of the field
     * @param type the boxed type to which the offset value will be converted (must be
     *            {@link Integer} or {@link Long})
     * @param cppType if non-null, the expected C++ type of the field (e.g., {@code "HeapWord*"})
     * @param notPresent if non-null and the field is not present then this value is returned
     * @return the offset in bytes of the requested field
     * @throws JVMCIError if the field is static or not present and {@code notPresent} is null
     */
    public <T> T getFieldOffset(String name, Class<T> type, String cppType, T notPresent) {
        return getFieldOffset0(name, type, notPresent, cppType, null);
    }

    /**
     * Gets the offset of a non-static C++ field.
     *
     * @param name fully qualified name of the field
     * @param type the boxed type to which the offset value will be converted (must be
     *            {@link Integer} or {@link Long})
     * @param notPresent if non-null and the field is not present then this value is returned
     * @param outCppType if non-null, the C++ type of the field (e.g., {@code "HeapWord*"}) is
     *            returned in element 0 of this array
     * @return the offset in bytes of the requested field
     * @throws JVMCIError if the field is static or not present and {@code notPresent} is null
     */
    public <T> T getFieldOffset(String name, Class<T> type, T notPresent, String[] outCppType) {
        return getFieldOffset0(name, type, notPresent, null, outCppType);
    }

    /**
     * Gets the offset of a non-static C++ field.
     *
     * @param name fully qualified name of the field
     * @param type the boxed type to which the offset value will be converted (must be
     *            {@link Integer} or {@link Long})
     * @param cppType if non-null, the expected C++ type of the field (e.g., {@code "HeapWord*"})
     * @return the offset in bytes of the requested field
     * @throws JVMCIError if the field is static or not present
     */
    public <T> T getFieldOffset(String name, Class<T> type, String cppType) {
        return getFieldOffset0(name, type, null, cppType, null);
    }

    /**
     * Gets the offset of a non-static C++ field.
     *
     * @param name fully qualified name of the field
     * @param type the boxed type to which the offset value will be converted (must be
     *            {@link Integer} or {@link Long})
     * @return the offset in bytes of the requested field
     * @throws JVMCIError if the field is static or not present
     */
    public <T> T getFieldOffset(String name, Class<T> type) {
        return getFieldOffset0(name, type, null, null, null);
    }

    private <T> T getFieldOffset0(String name, Class<T> type, T notPresent, String inCppType, String[] outCppType) {
        assert type == Integer.class || type == Long.class;
        VMField entry = getField(name, inCppType, notPresent == null);
        if (entry == null) {
            return notPresent;
        }
        if (entry.address != 0) {
            throw new JVMCIError("cannot get offset of static field " + name);
        }
        if (outCppType != null) {
            outCppType[0] = entry.type;
        }
        return type.cast(convertValue(name, type, entry.offset, inCppType));
    }

    /**
     * Gets the address of a static C++ field.
     *
     * @param name fully qualified name of the field
     * @param cppType if non-null, the expected C++ type of the field (e.g., {@code "HeapWord*"})
     * @param notPresent if non-null and the field is not present then this value is returned
     * @return the address of the requested field
     * @throws JVMCIError if the field is not static or not present and {@code notPresent} is null
     */
    public long getFieldAddress(String name, String cppType, Long notPresent) {
        return getFieldAddress0(name, notPresent, cppType, null);
    }

    /**
     * Gets the address of a static C++ field.
     *
     * @param name fully qualified name of the field
     * @param notPresent if non-null and the field is not present then this value is returned
     * @param outCppType if non-null, the C++ type of the field (e.g., {@code "HeapWord*"}) is
     *            returned in element 0 of this array
     * @return the address of the requested field
     * @throws JVMCIError if the field is not static or not present and {@code notPresent} is null
     */
    public long getFieldAddress(String name, Long notPresent, String[] outCppType) {
        return getFieldAddress0(name, notPresent, null, outCppType);
    }

    /**
     * Gets the address of a static C++ field.
     *
     * @param name fully qualified name of the field
     * @param cppType if non-null, the expected C++ type of the field (e.g., {@code "HeapWord*"})
     * @return the address of the requested field
     * @throws JVMCIError if the field is not static or not present
     */
    public long getFieldAddress(String name, String cppType) {
        return getFieldAddress0(name, null, cppType, null);
    }

    private long getFieldAddress0(String name, Long notPresent, String inCppType, String[] outCppType) {
        VMField entry = getField(name, inCppType, notPresent == null);
        if (entry == null) {
            return notPresent;
        }
        if (entry.address == 0) {
            throw new JVMCIError(name + " is not a static field");
        }
        if (outCppType != null) {
            outCppType[0] = entry.type;
        }
        return entry.address;
    }

    /**
     * Gets the value of a static C++ field.
     *
     * @param name fully qualified name of the field
     * @param type the boxed type to which the constant value will be converted
     * @param cppType if non-null, the expected C++ type of the field (e.g., {@code "HeapWord*"})
     * @param notPresent if non-null and the field is not present then this value is returned
     * @return the value of the requested field
     * @throws JVMCIError if the field is not static or not present and {@code notPresent} is null
     */
    public <T> T getFieldValue(String name, Class<T> type, String cppType, T notPresent) {
        return getFieldValue0(name, type, notPresent, cppType, null);
    }

    /**
     * Gets the value of a static C++ field.
     *
     * @param name fully qualified name of the field
     * @param type the boxed type to which the constant value will be converted
     * @param cppType if non-null, the expected C++ type of the field (e.g., {@code "HeapWord*"})
     * @return the value of the requested field
     * @throws JVMCIError if the field is not static or not present
     */
    public <T> T getFieldValue(String name, Class<T> type, String cppType) {
        return getFieldValue0(name, type, null, cppType, null);
    }

    /**
     * Gets the value of a static C++ field.
     *
     * @param name fully qualified name of the field
     * @param type the boxed type to which the constant value will be converted
     * @param notPresent if non-null and the field is not present then this value is returned
     * @param outCppType if non-null, the C++ type of the field (e.g., {@code "HeapWord*"}) is
     *            returned in element 0 of this array
     * @return the value of the requested field
     * @throws JVMCIError if the field is not static or not present and {@code notPresent} is null
     */
    public <T> T getFieldValue(String name, Class<T> type, T notPresent, String[] outCppType) {
        return getFieldValue0(name, type, notPresent, null, outCppType);
    }

    /**
     * Gets the value of a static C++ field.
     *
     * @param name fully qualified name of the field
     * @param type the boxed type to which the constant value will be converted
     * @return the value of the requested field
     * @throws JVMCIError if the field is not static or not present
     */
    public <T> T getFieldValue(String name, Class<T> type) {
        return getFieldValue0(name, type, null, null, null);
    }

    private <T> T getFieldValue0(String name, Class<T> type, T notPresent, String inCppType, String[] outCppType) {
        VMField entry = getField(name, inCppType, notPresent == null);
        if (entry == null) {
            return notPresent;
        }
        if (entry.value == null) {
            throw new JVMCIError(name + " is not a static field ");
        }
        if (outCppType != null) {
            outCppType[0] = entry.type;
        }
        return type.cast(convertValue(name, type, entry.value, inCppType));
    }

    /**
     * Gets a C++ field.
     *
     * @param name fully qualified name of the field
     * @param cppType if non-null, the expected C++ type of the field (e.g., {@code "HeapWord*"})
     * @param required specifies if the field must be present
     * @return the field
     * @throws JVMCIError if the field is not present and {@code required == true}
     */
    private VMField getField(String name, String cppType, boolean required) {
        VMField entry = store.vmFields.get(name);
        if (entry == null) {
            if (!required) {
                return null;
            }
            throw missingEntry("field", name, store.vmFields.keySet());
        }

        // Make sure the native type is still the type we expect.
        if (cppType != null && !cppType.equals(entry.type)) {
            throw new JVMCIError("expected type " + cppType + " but VM field " + name + " is of type " + entry.type);
        }
        return entry;
    }

    /**
     * Gets a VM flag value.
     *
     * @param name name of the flag (e.g., {@code "CompileTheWorldStartAt"})
     * @param type the boxed type to which the flag's value will be converted
     * @return the flag's value converted to {@code type} or {@code notPresent} if the flag is not
     *         present
     * @throws JVMCIError if the flag is not present
     */
    public <T> T getFlag(String name, Class<T> type) {
        return getFlag(name, type, null);
    }

    /**
     * Gets a VM flag value.
     *
     * @param name name of the flag (e.g., {@code "CompileTheWorldStartAt"})
     * @param type the boxed type to which the flag's value will be converted
     * @param notPresent if non-null and the flag is not present then this value is returned
     * @return the flag's value converted to {@code type} or {@code notPresent} if the flag is not
     *         present
     * @throws JVMCIError if the flag is not present and {@code notPresent == null}
     */
    public <T> T getFlag(String name, Class<T> type, T notPresent) {
        VMFlag entry = store.vmFlags.get(name);
        Object value;
        String cppType;
        if (entry == null) {
            // Fall back to VM call
            value = store.compilerToVm.getFlagValue(name);
            if (value == store.compilerToVm) {
                if (notPresent != null) {
                    return notPresent;
                }
                throw missingEntry("flag", name, store.vmFlags.keySet());
            } else {
                cppType = null;
            }
        } else {
            value = entry.value;
            cppType = entry.type;
        }
        return type.cast(convertValue(name, type, value, cppType));
    }

    private JVMCIError missingEntry(String category, String name, Set<String> keys) {
        throw new JVMCIError("expected VM %s not found in %s: %s%nAvailable values:%n    %s", category, store, name,
                        keys.stream().sorted().collect(Collectors.joining(System.lineSeparator() + "    ")));
    }

    private static <T> Object convertValue(String name, Class<T> toType, Object value, String cppType) throws JVMCIError {
        if (toType == Boolean.class) {
            if (value instanceof String) {
                return Boolean.valueOf((String) value);
            } else if (value instanceof Boolean) {
                return value;
            } else if (value instanceof Long) {
                return ((long) value) != 0;
            }
        } else if (toType == Byte.class) {
            if (value instanceof Long) {
                return (byte) (long) value;
            }
        } else if (toType == Integer.class) {
            if (value instanceof Integer) {
                return value;
            } else if (value instanceof Long) {
                return (int) (long) value;
            }
        } else if (toType == String.class) {
            if (value == null || value instanceof String) {
                return value;
            }
        } else if (toType == Long.class) {
            return value;
        }

        throw new JVMCIError("cannot convert " + name + " of type " + value.getClass().getSimpleName() + (cppType == null ? "" : " [" + cppType + "]") + " to " + toType.getSimpleName());
    }

    private final HotSpotVMConfigStore store;

    public HotSpotVMConfigAccess(HotSpotVMConfigStore store) {
        this.store = store;
    }
}
