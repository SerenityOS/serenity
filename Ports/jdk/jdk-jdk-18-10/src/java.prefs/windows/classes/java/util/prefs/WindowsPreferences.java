/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.prefs;

import java.util.StringTokenizer;
import java.io.ByteArrayOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.util.logging.PlatformLogger;

/**
 * Windows registry based implementation of  {@code Preferences}.
 * {@code Preferences}' {@code systemRoot} and {@code userRoot} are stored in
 * {@code HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Prefs} and
 * {@code HKEY_CURRENT_USER\Software\JavaSoft\Prefs} correspondingly.
 *
 * @author  Konstantin Kladko
 * @see Preferences
 * @see PreferencesFactory
 * @since 1.4
 */

class WindowsPreferences extends AbstractPreferences {

    static {
        loadPrefsLib();
    }

    @SuppressWarnings("removal")
    private static void loadPrefsLib() {
        PrivilegedAction<Void> load = () -> {
            System.loadLibrary("prefs");
            return null;
        };
        AccessController.doPrivileged(load);
    }

    /**
     * Logger for error messages
     */
    private static PlatformLogger logger;

    /**
     * Windows registry path to {@code Preferences}'s root nodes.
     */
    private static final byte[] WINDOWS_ROOT_PATH =
        stringToByteArray("Software\\JavaSoft\\Prefs");

    /**
     * Windows handles to {@code HKEY_CURRENT_USER} and
     * {@code HKEY_LOCAL_MACHINE} hives.
     */
    private static final int HKEY_CURRENT_USER = 0x80000001;
    private static final int HKEY_LOCAL_MACHINE = 0x80000002;

    /**
     * Mount point for {@code Preferences}'  user root.
     */
    private static final int USER_ROOT_NATIVE_HANDLE = HKEY_CURRENT_USER;

    /**
     * Mount point for {@code Preferences}'  system root.
     */
    private static final int SYSTEM_ROOT_NATIVE_HANDLE = HKEY_LOCAL_MACHINE;

    /**
     * Maximum byte-encoded path length for Windows native functions,
     * ending {@code null} character not included.
     */
    private static final int MAX_WINDOWS_PATH_LENGTH = 256;

    /**
     * User root node.
     */
    private static volatile Preferences userRoot;

    static Preferences getUserRoot() {
        Preferences root = userRoot;
        if (root == null) {
            synchronized (WindowsPreferences.class) {
                root = userRoot;
                if (root == null) {
                    root = new WindowsPreferences(USER_ROOT_NATIVE_HANDLE, WINDOWS_ROOT_PATH);
                    userRoot = root;
                }
            }
        }
        return root;
    }

    /**
     * System root node.
     */
    private static volatile Preferences systemRoot;

    static Preferences getSystemRoot() {
        Preferences root = systemRoot;
        if (root == null) {
            synchronized (WindowsPreferences.class) {
                root = systemRoot;
                if (root == null) {
                    root = new WindowsPreferences(SYSTEM_ROOT_NATIVE_HANDLE, WINDOWS_ROOT_PATH);
                    systemRoot = root;
                }
            }
        }
        return root;
    }

    /*  Windows error codes. */
    private static final int ERROR_SUCCESS = 0;
    private static final int ERROR_FILE_NOT_FOUND = 2;
    private static final int ERROR_ACCESS_DENIED = 5;

    /* Constants used to interpret returns of native functions    */
    private static final int NATIVE_HANDLE = 0;
    private static final int ERROR_CODE = 1;
    private static final int SUBKEYS_NUMBER = 0;
    private static final int VALUES_NUMBER = 2;
    private static final int MAX_KEY_LENGTH = 3;
    private static final int MAX_VALUE_NAME_LENGTH = 4;
    private static final int DISPOSITION = 2;
    private static final int REG_CREATED_NEW_KEY = 1;
    private static final int REG_OPENED_EXISTING_KEY = 2;
    private static final int NULL_NATIVE_HANDLE = 0;

    /* Windows security masks */
    private static final int DELETE = 0x10000;
    private static final int KEY_QUERY_VALUE = 1;
    private static final int KEY_SET_VALUE = 2;
    private static final int KEY_CREATE_SUB_KEY = 4;
    private static final int KEY_ENUMERATE_SUB_KEYS = 8;
    private static final int KEY_READ = 0x20019;
    private static final int KEY_WRITE = 0x20006;
    private static final int KEY_ALL_ACCESS = 0xf003f;

    /**
     * Initial time between registry access attempts, in ms. The time is doubled
     * after each failing attempt (except the first).
     */
    private static int INIT_SLEEP_TIME = 50;

    /**
     * Maximum number of registry access attempts.
     */
    private static int MAX_ATTEMPTS = 5;

    /**
     * BackingStore availability flag.
     */
    private boolean isBackingStoreAvailable = true;

    /**
     * Java wrapper for Windows registry API RegOpenKey()
     */
    private static native long[] WindowsRegOpenKey(long hKey, byte[] subKey,
                                                   int securityMask);
    /**
     * Retries RegOpenKey() MAX_ATTEMPTS times before giving up.
     */
    private static long[] WindowsRegOpenKey1(long hKey, byte[] subKey,
                                             int securityMask) {
        long[] result = WindowsRegOpenKey(hKey, subKey, securityMask);
        if (result[ERROR_CODE] == ERROR_SUCCESS) {
            return result;
        } else if (result[ERROR_CODE] == ERROR_FILE_NOT_FOUND) {
            logger().warning("Trying to recreate Windows registry node " +
            byteArrayToString(subKey) + " at root 0x" +
            Long.toHexString(hKey) + ".");
            // Try recreation
            long handle = WindowsRegCreateKeyEx(hKey, subKey)[NATIVE_HANDLE];
            WindowsRegCloseKey(handle);
            return WindowsRegOpenKey(hKey, subKey, securityMask);
        } else if (result[ERROR_CODE] != ERROR_ACCESS_DENIED) {
            long sleepTime = INIT_SLEEP_TIME;
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                try {
                    Thread.sleep(sleepTime);
                } catch(InterruptedException e) {
                    return result;
                }
                sleepTime *= 2;
                result = WindowsRegOpenKey(hKey, subKey, securityMask);
                if (result[ERROR_CODE] == ERROR_SUCCESS) {
                    return result;
                }
            }
        }
        return result;
    }

     /**
     * Java wrapper for Windows registry API RegCloseKey()
     */
    private static native int WindowsRegCloseKey(long hKey);

    /**
     * Java wrapper for Windows registry API RegCreateKeyEx()
     */
    private static native long[] WindowsRegCreateKeyEx(long hKey, byte[] subKey);

    /**
     * Retries RegCreateKeyEx() MAX_ATTEMPTS times before giving up.
     */
    private static long[] WindowsRegCreateKeyEx1(long hKey, byte[] subKey) {
        long[] result = WindowsRegCreateKeyEx(hKey, subKey);
        if (result[ERROR_CODE] == ERROR_SUCCESS) {
            return result;
        } else {
            long sleepTime = INIT_SLEEP_TIME;
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                try {
                    Thread.sleep(sleepTime);
                } catch(InterruptedException e) {
                    return result;
                }
                sleepTime *= 2;
                result = WindowsRegCreateKeyEx(hKey, subKey);
                if (result[ERROR_CODE] == ERROR_SUCCESS) {
                    return result;
                }
            }
        }
        return result;
    }
    /**
     * Java wrapper for Windows registry API RegDeleteKey()
     */
    private static native int WindowsRegDeleteKey(long hKey, byte[] subKey);

    /**
     * Java wrapper for Windows registry API RegFlushKey()
     */
    private static native int WindowsRegFlushKey(long hKey);

    /**
     * Retries RegFlushKey() MAX_ATTEMPTS times before giving up.
     */
    private static int WindowsRegFlushKey1(long hKey) {
        int result = WindowsRegFlushKey(hKey);
        if (result == ERROR_SUCCESS) {
            return result;
        } else {
            long sleepTime = INIT_SLEEP_TIME;
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                try {
                    Thread.sleep(sleepTime);
                } catch(InterruptedException e) {
                    return result;
                }
                sleepTime *= 2;
                result = WindowsRegFlushKey(hKey);
                if (result == ERROR_SUCCESS) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Java wrapper for Windows registry API RegQueryValueEx()
     */
    private static native byte[] WindowsRegQueryValueEx(long hKey,
                                                        byte[] valueName);
    /**
     * Java wrapper for Windows registry API RegSetValueEx()
     */
    private static native int WindowsRegSetValueEx(long hKey, byte[] valueName,
                                                   byte[] value);
    /**
     * Retries RegSetValueEx() MAX_ATTEMPTS times before giving up.
     */
    private static int WindowsRegSetValueEx1(long hKey, byte[] valueName,
                                             byte[] value) {
        int result = WindowsRegSetValueEx(hKey, valueName, value);
        if (result == ERROR_SUCCESS) {
            return result;
        } else {
            long sleepTime = INIT_SLEEP_TIME;
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                try {
                    Thread.sleep(sleepTime);
                } catch(InterruptedException e) {
                    return result;
                }
                sleepTime *= 2;
                result = WindowsRegSetValueEx(hKey, valueName, value);
                if (result == ERROR_SUCCESS) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Java wrapper for Windows registry API RegDeleteValue()
     */
    private static native int WindowsRegDeleteValue(long hKey, byte[] valueName);

    /**
     * Java wrapper for Windows registry API RegQueryInfoKey()
     */
    private static native long[] WindowsRegQueryInfoKey(long hKey);

    /**
     * Retries RegQueryInfoKey() MAX_ATTEMPTS times before giving up.
     */
    private static long[] WindowsRegQueryInfoKey1(long hKey) {
        long[] result = WindowsRegQueryInfoKey(hKey);
        if (result[ERROR_CODE] == ERROR_SUCCESS) {
            return result;
        } else {
            long sleepTime = INIT_SLEEP_TIME;
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                try {
                    Thread.sleep(sleepTime);
                } catch(InterruptedException e) {
                    return result;
                }
                sleepTime *= 2;
                result = WindowsRegQueryInfoKey(hKey);
                if (result[ERROR_CODE] == ERROR_SUCCESS) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Java wrapper for Windows registry API RegEnumKeyEx()
     */
    private static native byte[] WindowsRegEnumKeyEx(long hKey, int subKeyIndex,
                                                     int maxKeyLength);

    /**
     * Retries RegEnumKeyEx() MAX_ATTEMPTS times before giving up.
     */
    private static byte[] WindowsRegEnumKeyEx1(long hKey, int subKeyIndex,
                                               int maxKeyLength) {
        byte[] result = WindowsRegEnumKeyEx(hKey, subKeyIndex, maxKeyLength);
        if (result != null) {
            return result;
        } else {
            long sleepTime = INIT_SLEEP_TIME;
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                try {
                    Thread.sleep(sleepTime);
                } catch(InterruptedException e) {
                    return result;
                }
                sleepTime *= 2;
                result = WindowsRegEnumKeyEx(hKey, subKeyIndex, maxKeyLength);
                if (result != null) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Java wrapper for Windows registry API RegEnumValue()
     */
    private static native byte[] WindowsRegEnumValue(long hKey, int valueIndex,
                                                     int maxValueNameLength);
    /**
     * Retries RegEnumValueEx() MAX_ATTEMPTS times before giving up.
     */
    private static byte[] WindowsRegEnumValue1(long hKey, int valueIndex,
                                               int maxValueNameLength) {
        byte[] result = WindowsRegEnumValue(hKey, valueIndex,
                                            maxValueNameLength);
        if (result != null) {
            return result;
        } else {
            long sleepTime = INIT_SLEEP_TIME;
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                try {
                    Thread.sleep(sleepTime);
                } catch(InterruptedException e) {
                    return result;
                }
                sleepTime *= 2;
                result = WindowsRegEnumValue(hKey, valueIndex,
                                             maxValueNameLength);
                if (result != null) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Constructs a {@code WindowsPreferences} node, creating underlying
     * Windows registry node and all its Windows parents, if they are not yet
     * created.
     * Logs a warning message, if Windows Registry is unavailable.
     */
    private WindowsPreferences(WindowsPreferences parent, String name) {
        super(parent, name);
        long parentNativeHandle = parent.openKey(KEY_CREATE_SUB_KEY, KEY_READ);
        if (parentNativeHandle == NULL_NATIVE_HANDLE) {
            // if here, openKey failed and logged
            isBackingStoreAvailable = false;
            return;
        }
        long[] result =
               WindowsRegCreateKeyEx1(parentNativeHandle, toWindowsName(name));
        if (result[ERROR_CODE] != ERROR_SUCCESS) {
            logger().warning("Could not create windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" + Long.toHexString(parentNativeHandle) +
                    ". Windows RegCreateKeyEx(...) returned error code " +
                    result[ERROR_CODE] + ".");
            isBackingStoreAvailable = false;
            return;
        }
        newNode = (result[DISPOSITION] == REG_CREATED_NEW_KEY);
        closeKey(parentNativeHandle);
        closeKey(result[NATIVE_HANDLE]);
    }

    /**
     * Constructs a root node creating the underlying
     * Windows registry node and all of its parents, if they have not yet been
     * created.
     * Logs a warning message, if Windows Registry is unavailable.
     * @param rootNativeHandle Native handle to one of Windows top level keys.
     * @param rootDirectory Path to root directory, as a byte-encoded string.
     */
    private  WindowsPreferences(long rootNativeHandle, byte[] rootDirectory) {
        super(null, "");
        long[] result =
                WindowsRegCreateKeyEx1(rootNativeHandle, rootDirectory);
        if (result[ERROR_CODE] != ERROR_SUCCESS) {
            logger().warning("Could not open/create prefs root node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" + Long.toHexString(rootNativeHandle) +
                    ". Windows RegCreateKeyEx(...) returned error code " +
                    result[ERROR_CODE] + ".");
            isBackingStoreAvailable = false;
            return;
        }
        // Check if a new node
        newNode = (result[DISPOSITION] == REG_CREATED_NEW_KEY);
        closeKey(result[NATIVE_HANDLE]);
    }

    /**
     * Returns Windows absolute path of the current node as a byte array.
     * Java "/" separator is transformed into Windows "\".
     * @see Preferences#absolutePath()
     */
    private byte[] windowsAbsolutePath() {
        ByteArrayOutputStream bstream = new ByteArrayOutputStream();
        bstream.write(WINDOWS_ROOT_PATH, 0, WINDOWS_ROOT_PATH.length-1);
        StringTokenizer tokenizer = new StringTokenizer(absolutePath(), "/");
        while (tokenizer.hasMoreTokens()) {
            bstream.write((byte)'\\');
            String nextName = tokenizer.nextToken();
            byte[] windowsNextName = toWindowsName(nextName);
            bstream.write(windowsNextName, 0, windowsNextName.length-1);
        }
        bstream.write(0);
        return bstream.toByteArray();
    }

    /**
     * Opens current node's underlying Windows registry key using a
     * given security mask.
     * @param securityMask Windows security mask.
     * @return Windows registry key's handle.
     * @see #openKey(byte[], int)
     * @see #openKey(int, byte[], int)
     * @see #closeKey(int)
     */
    private long openKey(int securityMask) {
        return openKey(securityMask, securityMask);
    }

    /**
     * Opens current node's underlying Windows registry key using a
     * given security mask.
     * @param mask1 Preferred Windows security mask.
     * @param mask2 Alternate Windows security mask.
     * @return Windows registry key's handle.
     * @see #openKey(byte[], int)
     * @see #openKey(int, byte[], int)
     * @see #closeKey(int)
     */
    private long openKey(int mask1, int mask2) {
        return openKey(windowsAbsolutePath(), mask1,  mask2);
    }

     /**
     * Opens Windows registry key at a given absolute path using a given
     * security mask.
     * @param windowsAbsolutePath Windows absolute path of the
     *        key as a byte-encoded string.
     * @param mask1 Preferred Windows security mask.
     * @param mask2 Alternate Windows security mask.
     * @return Windows registry key's handle.
     * @see #openKey(int)
     * @see #openKey(int, byte[],int)
     * @see #closeKey(int)
     */
    private long openKey(byte[] windowsAbsolutePath, int mask1, int mask2) {
        /*  Check if key's path is short enough be opened at once
            otherwise use a path-splitting procedure */
        if (windowsAbsolutePath.length <= MAX_WINDOWS_PATH_LENGTH + 1) {
            long[] result = WindowsRegOpenKey1(rootNativeHandle(),
                                               windowsAbsolutePath, mask1);
            if (result[ERROR_CODE] == ERROR_ACCESS_DENIED && mask2 != mask1)
                result = WindowsRegOpenKey1(rootNativeHandle(),
                                            windowsAbsolutePath, mask2);

            if (result[ERROR_CODE] != ERROR_SUCCESS) {
                logger().warning("Could not open windows registry node " +
                        byteArrayToString(windowsAbsolutePath()) +
                        " at root 0x" +
                        Long.toHexString(rootNativeHandle()) +
                        ". Windows RegOpenKey(...) returned error code " +
                        result[ERROR_CODE] + ".");
                result[NATIVE_HANDLE] = NULL_NATIVE_HANDLE;
                if (result[ERROR_CODE] == ERROR_ACCESS_DENIED) {
                    throw new SecurityException(
                            "Could not open windows registry node " +
                            byteArrayToString(windowsAbsolutePath()) +
                            " at root 0x" +
                            Long.toHexString(rootNativeHandle()) +
                            ": Access denied");
                }
            }
            return result[NATIVE_HANDLE];
        } else {
            return openKey(rootNativeHandle(), windowsAbsolutePath, mask1, mask2);
        }
    }

     /**
     * Opens Windows registry key at a given relative path
     * with respect to a given Windows registry key.
     * @param windowsAbsolutePath Windows relative path of the
     *        key as a byte-encoded string.
     * @param nativeHandle handle to the base Windows key.
     * @param mask1 Preferred Windows security mask.
     * @param mask2 Alternate Windows security mask.
     * @return Windows registry key's handle.
     * @see #openKey(int)
     * @see #openKey(byte[],int)
     * @see #closeKey(int)
     */
    private long openKey(long nativeHandle, byte[] windowsRelativePath,
                         int mask1, int mask2) {
    /* If the path is short enough open at once. Otherwise split the path */
        if (windowsRelativePath.length <= MAX_WINDOWS_PATH_LENGTH + 1 ) {
            long[] result = WindowsRegOpenKey1(nativeHandle,
                                               windowsRelativePath, mask1);
            if (result[ERROR_CODE] == ERROR_ACCESS_DENIED && mask2 != mask1)
                result = WindowsRegOpenKey1(nativeHandle,
                                            windowsRelativePath, mask2);

            if (result[ERROR_CODE] != ERROR_SUCCESS) {
                logger().warning("Could not open windows registry node " +
                        byteArrayToString(windowsAbsolutePath()) +
                        " at root 0x" + Long.toHexString(nativeHandle) +
                        ". Windows RegOpenKey(...) returned error code " +
                        result[ERROR_CODE] + ".");
                result[NATIVE_HANDLE] = NULL_NATIVE_HANDLE;
            }
            return result[NATIVE_HANDLE];
        } else {
            int separatorPosition = -1;
            // Be greedy - open the longest possible path
            for (int i = MAX_WINDOWS_PATH_LENGTH; i > 0; i--) {
                if (windowsRelativePath[i] == ((byte)'\\')) {
                    separatorPosition = i;
                    break;
                }
            }
            // Split the path and do the recursion
            byte[] nextRelativeRoot = new byte[separatorPosition+1];
            System.arraycopy(windowsRelativePath, 0, nextRelativeRoot,0,
                                                      separatorPosition);
            nextRelativeRoot[separatorPosition] = 0;
            byte[] nextRelativePath = new byte[windowsRelativePath.length -
                                      separatorPosition - 1];
            System.arraycopy(windowsRelativePath, separatorPosition+1,
                             nextRelativePath, 0, nextRelativePath.length);
            long nextNativeHandle = openKey(nativeHandle, nextRelativeRoot,
                                           mask1, mask2);
            if (nextNativeHandle == NULL_NATIVE_HANDLE) {
                return NULL_NATIVE_HANDLE;
            }
            long result = openKey(nextNativeHandle, nextRelativePath,
                                  mask1,mask2);
            closeKey(nextNativeHandle);
            return result;
        }
    }

     /**
     * Closes Windows registry key.
     * Logs a warning if Windows registry is unavailable.
     * @param key's Windows registry handle.
     * @see #openKey(int)
     * @see #openKey(byte[],int)
     * @see #openKey(int, byte[],int)
    */
    private void closeKey(long nativeHandle) {
        int result = WindowsRegCloseKey(nativeHandle);
        if (result != ERROR_SUCCESS) {
            logger().warning("Could not close windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" +
                    Long.toHexString(rootNativeHandle()) +
                    ". Windows RegCloseKey(...) returned error code " +
                    result + ".");
        }
    }

     /**
     * Implements {@code AbstractPreferences} {@code putSpi()} method.
     * Puts name-value pair into the underlying Windows registry node.
     * Logs a warning, if Windows registry is unavailable.
     * @see #getSpi(String)
     */
    protected void putSpi(String javaName, String value) {
        long nativeHandle = openKey(KEY_SET_VALUE);
        if (nativeHandle == NULL_NATIVE_HANDLE) {
            isBackingStoreAvailable = false;
            return;
        }
        int result = WindowsRegSetValueEx1(nativeHandle,
                toWindowsName(javaName), toWindowsValueString(value));
        if (result != ERROR_SUCCESS) {
            logger().warning("Could not assign value to key " +
                    byteArrayToString(toWindowsName(javaName)) +
                    " at Windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" +
                    Long.toHexString(rootNativeHandle()) +
                    ". Windows RegSetValueEx(...) returned error code " +
                    result + ".");
            isBackingStoreAvailable = false;
        }
        closeKey(nativeHandle);
    }

    /**
     * Implements {@code AbstractPreferences} {@code getSpi()} method.
     * Gets a string value from the underlying Windows registry node.
     * Logs a warning, if Windows registry is unavailable.
     * @see #putSpi(String, String)
     */
    protected String getSpi(String javaName) {
        long nativeHandle = openKey(KEY_QUERY_VALUE);
        if (nativeHandle == NULL_NATIVE_HANDLE) {
            return null;
        }
        Object resultObject = WindowsRegQueryValueEx(nativeHandle,
                toWindowsName(javaName));
        if (resultObject == null) {
            closeKey(nativeHandle);
            return null;
        }
        closeKey(nativeHandle);
        return toJavaValueString((byte[]) resultObject);
    }

    /**
     * Implements {@code AbstractPreferences} {@code removeSpi()} method.
     * Deletes a string name-value pair from the underlying Windows registry
     * node, if this value still exists.
     * Logs a warning, if Windows registry is unavailable or key has already
     * been deleted.
     */
    protected void removeSpi(String key) {
        long nativeHandle = openKey(KEY_SET_VALUE);
        if (nativeHandle == NULL_NATIVE_HANDLE) {
        return;
        }
        int result =
            WindowsRegDeleteValue(nativeHandle, toWindowsName(key));
        if (result != ERROR_SUCCESS && result != ERROR_FILE_NOT_FOUND) {
            logger().warning("Could not delete windows registry value " +
                    byteArrayToString(windowsAbsolutePath()) + "\\" +
                    toWindowsName(key) + " at root 0x" +
                    Long.toHexString(rootNativeHandle()) +
                    ". Windows RegDeleteValue(...) returned error code " +
                    result + ".");
            isBackingStoreAvailable = false;
        }
        closeKey(nativeHandle);
    }

    /**
     * Implements {@code AbstractPreferences} {@code keysSpi()} method.
     * Gets value names from the underlying Windows registry node.
     * Throws a BackingStoreException and logs a warning, if
     * Windows registry is unavailable.
     */
    protected String[] keysSpi() throws BackingStoreException{
        // Find out the number of values
        long nativeHandle = openKey(KEY_QUERY_VALUE);
        if (nativeHandle == NULL_NATIVE_HANDLE) {
            throw new BackingStoreException(
                    "Could not open windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" +
                    Long.toHexString(rootNativeHandle()) + ".");
        }
        long[] result =  WindowsRegQueryInfoKey1(nativeHandle);
        if (result[ERROR_CODE] != ERROR_SUCCESS) {
            String info = "Could not query windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" +
                    Long.toHexString(rootNativeHandle()) +
                    ". Windows RegQueryInfoKeyEx(...) returned error code " +
                    result[ERROR_CODE] + ".";
            logger().warning(info);
            throw new BackingStoreException(info);
        }
        int maxValueNameLength = (int)result[MAX_VALUE_NAME_LENGTH];
        int valuesNumber = (int)result[VALUES_NUMBER];
        if (valuesNumber == 0) {
            closeKey(nativeHandle);
            return new String[0];
        }
        // Get the values
        String[] valueNames = new String[valuesNumber];
        for (int i = 0; i < valuesNumber; i++) {
            byte[] windowsName = WindowsRegEnumValue1(nativeHandle, i,
                                                      maxValueNameLength+1);
            if (windowsName == null) {
                String info =
                    "Could not enumerate value #" + i + "  of windows node " +
                    byteArrayToString(windowsAbsolutePath()) + " at root 0x" +
                    Long.toHexString(rootNativeHandle()) + ".";
                logger().warning(info);
                throw new BackingStoreException(info);
            }
            valueNames[i] = toJavaName(windowsName);
        }
        closeKey(nativeHandle);
        return valueNames;
    }

    /**
     * Implements {@code AbstractPreferences} {@code childrenNamesSpi()} method.
     * Calls Windows registry to retrive children of this node.
     * Throws a BackingStoreException and logs a warning message,
     * if Windows registry is not available.
     */
    protected String[] childrenNamesSpi() throws BackingStoreException {
        // Open key
        long nativeHandle = openKey(KEY_ENUMERATE_SUB_KEYS | KEY_QUERY_VALUE);
        if (nativeHandle == NULL_NATIVE_HANDLE) {
            throw new BackingStoreException(
                    "Could not open windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" +
                    Long.toHexString(rootNativeHandle()) + ".");
        }
        // Get number of children
        long[] result =  WindowsRegQueryInfoKey1(nativeHandle);
        if (result[ERROR_CODE] != ERROR_SUCCESS) {
            String info = "Could not query windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" + Long.toHexString(rootNativeHandle()) +
                    ". Windows RegQueryInfoKeyEx(...) returned error code " +
                    result[ERROR_CODE] + ".";
            logger().warning(info);
            throw new BackingStoreException(info);
        }
        int maxKeyLength = (int)result[MAX_KEY_LENGTH];
        int subKeysNumber = (int)result[SUBKEYS_NUMBER];
        if (subKeysNumber == 0) {
            closeKey(nativeHandle);
            return new String[0];
        }
        String[] subkeys = new String[subKeysNumber];
        String[] children = new String[subKeysNumber];
        // Get children
        for (int i = 0; i < subKeysNumber; i++) {
            byte[] windowsName = WindowsRegEnumKeyEx1(nativeHandle, i,
                                                      maxKeyLength+1);
            if (windowsName == null) {
                String info =
                    "Could not enumerate key #" + i + "  of windows node " +
                    byteArrayToString(windowsAbsolutePath()) + " at root 0x" +
                    Long.toHexString(rootNativeHandle()) + ". ";
                logger().warning(info);
                throw new BackingStoreException(info);
            }
            String javaName = toJavaName(windowsName);
            children[i] = javaName;
        }
        closeKey(nativeHandle);
        return children;
    }

    /**
     * Implements {@code Preferences} {@code flush()} method.
     * Flushes Windows registry changes to disk.
     * Throws a BackingStoreException and logs a warning message if Windows
     * registry is not available.
     */
    public void flush() throws BackingStoreException{

        if (isRemoved()) {
            parent.flush();
            return;
        }
        if (!isBackingStoreAvailable) {
            throw new BackingStoreException(
                    "flush(): Backing store not available.");
        }
        long nativeHandle = openKey(KEY_READ);
        if (nativeHandle == NULL_NATIVE_HANDLE) {
            throw new BackingStoreException(
                    "Could not open windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" +
                    Long.toHexString(rootNativeHandle()) + ".");
        }
        int result = WindowsRegFlushKey1(nativeHandle);
        if (result != ERROR_SUCCESS) {
            String info = "Could not flush windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" +
                    Long.toHexString(rootNativeHandle()) +
                    ". Windows RegFlushKey(...) returned error code " +
                    result + ".";
            logger().warning(info);
            throw new BackingStoreException(info);
        }
        closeKey(nativeHandle);
    }


    /**
     * Implements {@code Preferences} {@code sync()} method.
     * Flushes Windows registry changes to disk. Equivalent to flush().
     * @see flush()
     */
    public void sync() throws BackingStoreException{
        if (isRemoved())
            throw new IllegalStateException("Node has been removed");
        flush();
    }

    /**
     * Implements {@code AbstractPreferences} {@code childSpi()} method.
     * Constructs a child node with a
     * given name and creates its underlying Windows registry node,
     * if it does not exist.
     * Logs a warning message, if Windows Registry is unavailable.
     */
    protected AbstractPreferences childSpi(String name) {
        return new WindowsPreferences(this, name);
    }

    /**
     * Implements {@code AbstractPreferences} {@code removeNodeSpi()} method.
     * Deletes underlying Windows registry node.
     * Throws a BackingStoreException and logs a warning, if Windows registry
     * is not available.
     */
    public void removeNodeSpi() throws BackingStoreException {
        long parentNativeHandle =
                ((WindowsPreferences)parent()).openKey(DELETE);
        if (parentNativeHandle == NULL_NATIVE_HANDLE) {
            throw new BackingStoreException(
                    "Could not open parent windows registry node of " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" +
                    Long.toHexString(rootNativeHandle()) + ".");
        }
        int result =
                WindowsRegDeleteKey(parentNativeHandle, toWindowsName(name()));
        if (result != ERROR_SUCCESS) {
            String info = "Could not delete windows registry node " +
                    byteArrayToString(windowsAbsolutePath()) +
                    " at root 0x" + Long.toHexString(rootNativeHandle()) +
                    ". Windows RegDeleteKeyEx(...) returned error code " +
                    result + ".";
            logger().warning(info);
            throw new BackingStoreException(info);
        }
        closeKey(parentNativeHandle);
    }

    /**
     * Converts value's or node's name from its byte array representation to
     * java string. Two encodings, simple and altBase64 are used. See
     * {@link #toWindowsName(String) toWindowsName()} for a detailed
     * description of encoding conventions.
     * @param windowsNameArray Null-terminated byte array.
     */
    private static String toJavaName(byte[] windowsNameArray) {
        String windowsName = byteArrayToString(windowsNameArray);
        // check if Alt64
        if ((windowsName.length() > 1) &&
                (windowsName.substring(0, 2).equals("/!"))) {
            return toJavaAlt64Name(windowsName);
        }
        StringBuilder javaName = new StringBuilder();
        char ch;
        // Decode from simple encoding
        for (int i = 0; i < windowsName.length(); i++) {
            if ((ch = windowsName.charAt(i)) == '/') {
                char next = ' ';
                if ((windowsName.length() > i + 1) &&
                        ((next = windowsName.charAt(i+1)) >= 'A') &&
                        (next <= 'Z')) {
                    ch = next;
                    i++;
                } else if ((windowsName.length() > i + 1) &&
                           (next == '/')) {
                    ch = '\\';
                    i++;
                }
            } else if (ch == '\\') {
                ch = '/';
            }
            javaName.append(ch);
        }
        return javaName.toString();
    }

    /**
     * Converts value's or node's name from its Windows representation to java
     * string, using altBase64 encoding. See
     * {@link #toWindowsName(String) toWindowsName()} for a detailed
     * description of encoding conventions.
     */

    private static String toJavaAlt64Name(String windowsName) {
        byte[] byteBuffer =
                Base64.altBase64ToByteArray(windowsName.substring(2));
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < byteBuffer.length; i++) {
            int firstbyte = (byteBuffer[i++] & 0xff);
            int secondbyte =  (byteBuffer[i] & 0xff);
            result.append((char)((firstbyte << 8) + secondbyte));
        }
        return result.toString();
    }

    /**
     * Converts value's or node's name to its Windows representation
     * as a byte-encoded string.
     * Two encodings, simple and altBase64 are used.
     * <p>
     * <i>Simple</i> encoding is used, if java string does not contain
     * any characters less, than 0x0020, or greater, than 0x007f.
     * Simple encoding adds "/" character to capital letters, i.e.
     * "A" is encoded as "/A". Character '\' is encoded as '//',
     * '/' is encoded as '\'.
     * The constructed string is converted to byte array by truncating the
     * highest byte and adding the terminating {@code null} character.
     * <p>
     * <i>altBase64</i>  encoding is used, if java string does contain at least
     * one character less, than 0x0020, or greater, than 0x007f.
     * This encoding is marked by setting first two bytes of the
     * Windows string to '/!'. The java name is then encoded using
     * byteArrayToAltBase64() method from
     * Base64 class.
     */
    private static byte[] toWindowsName(String javaName) {
        StringBuilder windowsName = new StringBuilder();
        for (int i = 0; i < javaName.length(); i++) {
            char ch = javaName.charAt(i);
            if ((ch < 0x0020) || (ch > 0x007f)) {
                // If a non-trivial character encountered, use altBase64
                return toWindowsAlt64Name(javaName);
            }
            if (ch == '\\') {
                windowsName.append("//");
            } else if (ch == '/') {
                windowsName.append('\\');
            } else if ((ch >= 'A') && (ch <='Z')) {
                windowsName.append('/').append(ch);
            } else {
                windowsName.append(ch);
            }
        }
        return stringToByteArray(windowsName.toString());
    }

    /**
     * Converts value's or node's name to its Windows representation
     * as a byte-encoded string, using altBase64 encoding. See
     * {@link #toWindowsName(String) toWindowsName()} for a detailed
     * description of encoding conventions.
     */
    private static byte[] toWindowsAlt64Name(String javaName) {
        byte[] javaNameArray = new byte[2*javaName.length()];
        // Convert to byte pairs
        int counter = 0;
        for (int i = 0; i < javaName.length();i++) {
            int ch = javaName.charAt(i);
            javaNameArray[counter++] = (byte)(ch >>> 8);
            javaNameArray[counter++] = (byte)ch;
        }

        return stringToByteArray("/!" +
                Base64.byteArrayToAltBase64(javaNameArray));
    }

    /**
     * Converts value string from its Windows representation
     * to java string.  See
     * {@link #toWindowsValueString(String) toWindowsValueString()} for the
     * description of the encoding algorithm.
     */
     private static String toJavaValueString(byte[] windowsNameArray) {
        String windowsName = byteArrayToString(windowsNameArray);
        StringBuilder javaName = new StringBuilder();
        char ch;
        for (int i = 0; i < windowsName.length(); i++){
            if ((ch = windowsName.charAt(i)) == '/') {
                char next = ' ';

                if (windowsName.length() > i + 1 &&
                        (next = windowsName.charAt(i + 1)) == 'u') {
                    if (windowsName.length() < i + 6) {
                        break;
                    } else {
                        ch = (char)Integer.parseInt(
                                windowsName.substring(i + 2, i + 6), 16);
                        i += 5;
                    }
                } else
                if ((windowsName.length() > i + 1) &&
                        ((windowsName.charAt(i+1)) >= 'A') &&
                        (next <= 'Z')) {
                    ch = next;
                    i++;
                } else if ((windowsName.length() > i + 1) &&
                        (next == '/')) {
                    ch = '\\';
                    i++;
                }
            } else if (ch == '\\') {
                ch = '/';
            }
            javaName.append(ch);
        }
        return javaName.toString();
    }

    /**
     * Converts value string to it Windows representation.
     * as a byte-encoded string.
     * Encoding algorithm adds "/" character to capital letters, i.e.
     * "A" is encoded as "/A". Character '\' is encoded as '//',
     * '/' is encoded as  '\'.
     * Then convert java string to a byte array of ASCII characters.
     */
    private static byte[] toWindowsValueString(String javaName) {
        StringBuilder windowsName = new StringBuilder();
        for (int i = 0; i < javaName.length(); i++) {
            char ch = javaName.charAt(i);
            if ((ch < 0x0020) || (ch > 0x007f)){
                // write \udddd
                windowsName.append("/u");
                String hex = Long.toHexString(javaName.charAt(i));
                StringBuilder hex4 = new StringBuilder(hex);
                hex4.reverse();
                int len = 4 - hex4.length();
                for (int j = 0; j < len; j++){
                    hex4.append('0');
                }
                for (int j = 0; j < 4; j++){
                    windowsName.append(hex4.charAt(3 - j));
                }
            } else if (ch == '\\') {
                windowsName.append("//");
            } else if (ch == '/') {
                windowsName.append('\\');
            } else if ((ch >= 'A') && (ch <='Z')) {
                windowsName.append('/').append(ch);
            } else {
                windowsName.append(ch);
            }
        }
        return stringToByteArray(windowsName.toString());
    }

    /**
     * Returns native handle for the top Windows node for this node.
     */
    private long rootNativeHandle() {
        return (isUserNode()
                ? USER_ROOT_NATIVE_HANDLE
                : SYSTEM_ROOT_NATIVE_HANDLE);
    }

    /**
     * Returns this java string as a null-terminated byte array
     */
    private static byte[] stringToByteArray(String str) {
        byte[] result = new byte[str.length()+1];
        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }

    /**
     * Converts a null-terminated byte array to java string
     */
    private static String byteArrayToString(byte[] array) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.length - 1; i++) {
            result.append((char)array[i]);
        }
        return result.toString();
    }

   /**
    * Empty, never used implementation  of AbstractPreferences.flushSpi().
    */
    protected void flushSpi() throws BackingStoreException {
        // assert false;
    }

   /**
    * Empty, never used implementation  of AbstractPreferences.flushSpi().
    */
    protected void syncSpi() throws BackingStoreException {
        // assert false;
    }

    private static synchronized PlatformLogger logger() {
        if (logger == null) {
            logger = PlatformLogger.getLogger("java.util.prefs");
        }
        return logger;
    }
}
