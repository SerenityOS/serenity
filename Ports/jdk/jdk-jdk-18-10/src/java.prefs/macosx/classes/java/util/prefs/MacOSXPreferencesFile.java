/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.ref.WeakReference;


/*
  MacOSXPreferencesFile synchronization:

  Everything is synchronized on MacOSXPreferencesFile.class. This prevents:
  * simultaneous updates to cachedFiles or changedFiles
  * simultaneous creation of two objects for the same name+user+host triplet
  * simultaneous modifications to the same file
  * modifications during syncWorld/flushWorld
  * (in MacOSXPreferences.removeNodeSpi()) modification or sync during
    multi-step node removal process
  ... among other things.
*/
/*
  Timers. There are two timers that control synchronization of prefs data to
  and from disk.

  * Sync timer periodically calls syncWorld() to force external disk changes
      (e.g. from another VM) into the memory cache. The sync timer runs even
      if there are no outstanding local changes. The sync timer syncs all live
      MacOSXPreferencesFile objects (the cachedFiles list).
    The sync timer period is controlled by the java.util.prefs.syncInterval
      property (same as FileSystemPreferences). By default there is *no*
      sync timer (unlike FileSystemPreferences); it is only enabled if the
      syncInterval property is set. The minimum interval is 5 seconds.

  * Flush timer calls flushWorld() to force local changes to disk.
      The flush timer is scheduled to fire some time after each pref change,
      unless it's already scheduled to fire before that. syncWorld and
      flushWorld will cancel any outstanding flush timer as unnecessary.
      The flush timer flushes all changed files (the changedFiles list).
    The time between pref write and flush timer call is controlled by the
      java.util.prefs.flushDelay property (unlike FileSystemPreferences).
      The default is 60 seconds and the minimum is 5 seconds.

  The flush timer's behavior is required by the Java Preferences spec
  ("changes will eventually propagate to the persistent backing store with
  an implementation-dependent delay"). The sync timer is not required by
  the spec (multiple VMs are only required to not corrupt the prefs), but
  the periodic sync is implemented by FileSystemPreferences and may be
  useful to some programs. The sync timer is disabled by default because
  it's expensive and is usually not necessary.
*/

class MacOSXPreferencesFile {

    static {
        loadPrefsLib();
    }

    @SuppressWarnings("removal")
    private static void loadPrefsLib() {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("prefs");
                    return null;
                }
            });
    }

    private class FlushTask extends TimerTask {
        public void run() {
            MacOSXPreferencesFile.flushWorld();
        }
    }

    private class SyncTask extends TimerTask {
        public void run() {
            MacOSXPreferencesFile.syncWorld();
        }
    }

    // Maps string -> weak reference to MacOSXPreferencesFile
    private static HashMap<String, WeakReference<MacOSXPreferencesFile>>
            cachedFiles;
    // Files that may have unflushed changes
    private static HashSet<MacOSXPreferencesFile> changedFiles;


    // Timer and pending sync and flush tasks (which are both scheduled
    // on the same timer)
    private static Timer timer = null;
    private static FlushTask flushTimerTask = null;
    private static long flushDelay = -1; // in seconds (min 5, default 60)
    private static long syncInterval = -1; // (min 5, default negative == off)

    private String appName;
    private long user;
    private long host;

    String name() { return appName; }
    long user() { return user; }
    long host() { return host; }

    // private constructor - use factory method getFile() instead
    private MacOSXPreferencesFile(String newName, long newUser, long newHost)
    {
        appName = newName;
        user = newUser;
        host = newHost;
    }

    // Factory method
    // Always returns the same object for the given name+user+host
    static synchronized MacOSXPreferencesFile
        getFile(String newName, boolean isUser)
    {
        MacOSXPreferencesFile result = null;

        if (cachedFiles == null)
            cachedFiles = new HashMap<>();

        String hashkey =
            newName + String.valueOf(isUser);
        WeakReference<MacOSXPreferencesFile> hashvalue = cachedFiles.get(hashkey);
        if (hashvalue != null) {
            result = hashvalue.get();
        }
        if (result == null) {
            // Java user node == CF current user, any host
            // Java system node == CF any user, current host
            result = new MacOSXPreferencesFile(newName,
                                         isUser ? cfCurrentUser : cfAnyUser,
                                         isUser ? cfAnyHost : cfCurrentHost);
            cachedFiles.put(hashkey, new WeakReference<MacOSXPreferencesFile>(result));
        }

        // Don't schedule this file for flushing until some nodes or
        // keys are added to it.

        // Do set up the sync timer if requested; sync timer affects reads
        // as well as writes.
        initSyncTimerIfNeeded();

        return result;
    }


    // Write all prefs changes to disk and clear all cached prefs values
    // (so the next read will read from disk).
    static synchronized boolean syncWorld()
    {
        boolean ok = true;

        if (cachedFiles != null  &&  !cachedFiles.isEmpty()) {
            Iterator<WeakReference<MacOSXPreferencesFile>> iter =
                    cachedFiles.values().iterator();
            while (iter.hasNext()) {
                WeakReference<MacOSXPreferencesFile> ref = iter.next();
                MacOSXPreferencesFile f = ref.get();
                if (f != null) {
                    if (!f.synchronize()) ok = false;
                } else {
                    iter.remove();
                }
            }
        }

        // Kill any pending flush
        if (flushTimerTask != null) {
            flushTimerTask.cancel();
            flushTimerTask = null;
        }

        // Clear changed file list. The changed files were guaranteed to
        // have been in the cached file list (because there was a strong
        // reference from changedFiles.
        if (changedFiles != null) changedFiles.clear();

        return ok;
    }


    // Sync only current user preferences
    static synchronized boolean syncUser() {
        boolean ok = true;
        if (cachedFiles != null  &&  !cachedFiles.isEmpty()) {
            Iterator<WeakReference<MacOSXPreferencesFile>> iter =
                    cachedFiles.values().iterator();
            while (iter.hasNext()) {
                WeakReference<MacOSXPreferencesFile> ref = iter.next();
                MacOSXPreferencesFile f = ref.get();
                if (f != null && f.user == cfCurrentUser) {
                    if (!f.synchronize()) {
                        ok = false;
                    }
                } else {
                    iter.remove();
                }
            }
        }
        // Remove synchronized file from changed file list. The changed files were
        // guaranteed to have been in the cached file list (because there was a strong
        // reference from changedFiles.
        if (changedFiles != null) {
            Iterator<MacOSXPreferencesFile> iterChanged = changedFiles.iterator();
            while (iterChanged.hasNext()) {
                MacOSXPreferencesFile f = iterChanged.next();
                if (f != null && f.user == cfCurrentUser)
                    iterChanged.remove();
             }
        }
        return ok;
    }

    //Flush only current user preferences
    static synchronized boolean flushUser() {
        boolean ok = true;
        if (changedFiles != null  &&  !changedFiles.isEmpty()) {
            Iterator<MacOSXPreferencesFile> iterator = changedFiles.iterator();
            while(iterator.hasNext()) {
                MacOSXPreferencesFile f = iterator.next();
                if (f.user == cfCurrentUser) {
                    if (!f.synchronize())
                        ok = false;
                    else
                        iterator.remove();
                }
            }
        }
        return ok;
    }

    // Write all prefs changes to disk, but do not clear all cached prefs
    // values. Also kills any scheduled flush task.
    // There's no CFPreferencesFlush() (<rdar://problem/3049129>), so lots of cached prefs
    // are cleared anyway.
    static synchronized boolean flushWorld()
    {
        boolean ok = true;

        if (changedFiles != null  &&  !changedFiles.isEmpty()) {
            for (MacOSXPreferencesFile f : changedFiles) {
                if (!f.synchronize())
                    ok = false;
            }
            changedFiles.clear();
        }

        if (flushTimerTask != null) {
            flushTimerTask.cancel();
            flushTimerTask = null;
        }

        return ok;
    }

    // Mark this prefs file as changed. The changes will be flushed in
    // at most flushDelay() seconds.
    // Must be called when synchronized on MacOSXPreferencesFile.class
    private void markChanged()
    {
        // Add this file to the changed file list
        if (changedFiles == null)
            changedFiles = new HashSet<>();
        changedFiles.add(this);

        // Schedule a new flush and a shutdown hook, if necessary
        if (flushTimerTask == null) {
            flushTimerTask = new FlushTask();
            timer().schedule(flushTimerTask, flushDelay() * 1000);
        }
    }

    // Return the flush delay, initializing from a property if necessary.
    private static synchronized long flushDelay()
    {
        if (flushDelay == -1) {
            try {
                // flush delay >= 5, default 60
                flushDelay = Math.max(5, Integer.parseInt(System.getProperty("java.util.prefs.flushDelay", "60")));
            } catch (NumberFormatException e) {
                flushDelay = 60;
            }
        }
        return flushDelay;
    }

    // Initialize and run the sync timer, if the sync timer property is set
    // and the sync timer hasn't already been started.
    private static synchronized void initSyncTimerIfNeeded()
    {
        // syncInterval: -1 is uninitialized, other negative is off,
        // positive is seconds between syncs (min 5).

        if (syncInterval == -1) {
            try {
                syncInterval = Integer.parseInt(System.getProperty("java.util.prefs.syncInterval", "-2"));
                if (syncInterval >= 0) {
                    // minimum of 5 seconds
                    syncInterval = Math.max(5, syncInterval);
                } else {
                    syncInterval = -2; // default off
                }
            } catch (NumberFormatException e) {
                syncInterval = -2; // bad property value - default off
            }

            if (syncInterval > 0) {
                timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        MacOSXPreferencesFile.syncWorld();}
                    }, syncInterval * 1000, syncInterval * 1000);
            } else {
                // syncInterval property not set. No sync timer ever.
            }
        }
    }

    // Return the timer used for flush and sync, creating it if necessary.
    private static synchronized Timer timer()
    {
        if (timer == null) {
            timer = new Timer(true); // daemon
            Thread flushThread =
                new Thread(null, null, "Flush Thread", 0, false) {
                @Override
                public void run() {
                    flushWorld();
                }
            };
            /* Set context class loader to null in order to avoid
             * keeping a strong reference to an application classloader.
             */
            flushThread.setContextClassLoader(null);
            Runtime.getRuntime().addShutdownHook(flushThread);
        }
        return timer;
    }


    // Node manipulation
    boolean addNode(String path)
    {
        synchronized(MacOSXPreferencesFile.class) {
            markChanged();
            return addNode(path, appName, user, host);
        }
    }

    void removeNode(String path)
    {
        synchronized(MacOSXPreferencesFile.class) {
            markChanged();
            removeNode(path, appName, user, host);
        }
    }

    boolean addChildToNode(String path, String child)
    {
        synchronized(MacOSXPreferencesFile.class) {
            markChanged();
            return addChildToNode(path, child+"/", appName, user, host);
        }
    }

    void removeChildFromNode(String path, String child)
    {
        synchronized(MacOSXPreferencesFile.class) {
            markChanged();
            removeChildFromNode(path, child+"/", appName, user, host);
        }
    }


    // Key manipulation
    void addKeyToNode(String path, String key, String value)
    {
        synchronized(MacOSXPreferencesFile.class) {
            markChanged();
            addKeyToNode(path, key, value, appName, user, host);
        }
    }

    void removeKeyFromNode(String path, String key)
    {
        synchronized(MacOSXPreferencesFile.class) {
            markChanged();
            removeKeyFromNode(path, key, appName, user, host);
        }
    }

    String getKeyFromNode(String path, String key)
    {
        synchronized(MacOSXPreferencesFile.class) {
            return getKeyFromNode(path, key, appName, user, host);
        }
    }


    // Enumerators
    String[] getChildrenForNode(String path)
    {
        synchronized(MacOSXPreferencesFile.class) {
            return getChildrenForNode(path, appName, user, host);
        }
    }

    String[] getKeysForNode(String path)
    {
        synchronized(MacOSXPreferencesFile.class) {
            return getKeysForNode(path, appName, user, host);
        }
    }


    // Synchronization
    boolean synchronize()
    {
        synchronized(MacOSXPreferencesFile.class) {
            return synchronize(appName, user, host);
        }
    }


    // CF functions
    // Must be called when synchronized on MacOSXPreferencesFile.class
    private static final native boolean
        addNode(String path, String name, long user, long host);
    private static final native void
        removeNode(String path, String name, long user, long host);
    private static final native boolean
        addChildToNode(String path, String child,
                       String name, long user, long host);
    private static final native void
        removeChildFromNode(String path, String child,
                            String name, long user, long host);
    private static final native void
        addKeyToNode(String path, String key, String value,
                     String name, long user, long host);
    private static final native void
        removeKeyFromNode(String path, String key,
                          String name, long user, long host);
    private static final native String
        getKeyFromNode(String path, String key,
                       String name, long user, long host);
    private static final native String[]
        getChildrenForNode(String path, String name, long user, long host);
    private static final native String[]
        getKeysForNode(String path, String name, long user, long host);
    private static final native boolean
        synchronize(String name, long user, long host);

    // CFPreferences host and user values (CFStringRefs)
    private static long cfCurrentUser = currentUser();
    private static long cfAnyUser = anyUser();
    private static long cfCurrentHost = currentHost();
    private static long cfAnyHost = anyHost();

    // CFPreferences constant accessors
    private static final native long currentUser();
    private static final native long anyUser();
    private static final native long currentHost();
    private static final native long anyHost();
}

