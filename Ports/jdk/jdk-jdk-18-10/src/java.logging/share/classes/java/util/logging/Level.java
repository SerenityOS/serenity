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

package java.util.logging;

import java.io.Serial;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import jdk.internal.loader.ClassLoaderValue;
import jdk.internal.access.JavaUtilResourceBundleAccess;
import jdk.internal.access.SharedSecrets;

/**
 * The Level class defines a set of standard logging levels that
 * can be used to control logging output.  The logging Level objects
 * are ordered and are specified by ordered integers.  Enabling logging
 * at a given level also enables logging at all higher levels.
 * <p>
 * Clients should normally use the predefined Level constants such
 * as Level.SEVERE.
 * <p>
 * The levels in descending order are:
 * <ul>
 * <li>SEVERE (highest value)
 * <li>WARNING
 * <li>INFO
 * <li>CONFIG
 * <li>FINE
 * <li>FINER
 * <li>FINEST  (lowest value)
 * </ul>
 * In addition there is a level OFF that can be used to turn
 * off logging, and a level ALL that can be used to enable
 * logging of all messages.
 * <p>
 * It is possible for third parties to define additional logging
 * levels by subclassing Level.  In such cases subclasses should
 * take care to chose unique integer level values and to ensure that
 * they maintain the Object uniqueness property across serialization
 * by defining a suitable readResolve method.
 *
 * @since 1.4
 */

public class Level implements java.io.Serializable {
    private static final String defaultBundle =
        "sun.util.logging.resources.logging";

    // Calling SharedSecrets.getJavaUtilResourceBundleAccess()
    // forces the initialization of ResourceBundle.class, which
    // can be too early if the VM has not finished booting yet.
    private static final class RbAccess {
        static final JavaUtilResourceBundleAccess RB_ACCESS =
            SharedSecrets.getJavaUtilResourceBundleAccess();
    }

    /**
     * @serial  The non-localized name of the level.
     */
    private final String name;

    /**
     * @serial  The integer value of the level.
     */
    private final int value;

    /**
     * @serial The resource bundle name to be used in localizing the level name.
     */
    private final String resourceBundleName;

    // localized level name
    private transient String localizedLevelName;
    private transient Locale cachedLocale;

    /**
     * OFF is a special level that can be used to turn off logging.
     * This level is initialized to <CODE>Integer.MAX_VALUE</CODE>.
     */
    public static final Level OFF = new Level("OFF",Integer.MAX_VALUE, defaultBundle);

    /**
     * SEVERE is a message level indicating a serious failure.
     * <p>
     * In general SEVERE messages should describe events that are
     * of considerable importance and which will prevent normal
     * program execution.   They should be reasonably intelligible
     * to end users and to system administrators.
     * This level is initialized to <CODE>1000</CODE>.
     */
    public static final Level SEVERE = new Level("SEVERE",1000, defaultBundle);

    /**
     * WARNING is a message level indicating a potential problem.
     * <p>
     * In general WARNING messages should describe events that will
     * be of interest to end users or system managers, or which
     * indicate potential problems.
     * This level is initialized to <CODE>900</CODE>.
     */
    public static final Level WARNING = new Level("WARNING", 900, defaultBundle);

    /**
     * INFO is a message level for informational messages.
     * <p>
     * Typically INFO messages will be written to the console
     * or its equivalent.  So the INFO level should only be
     * used for reasonably significant messages that will
     * make sense to end users and system administrators.
     * This level is initialized to <CODE>800</CODE>.
     */
    public static final Level INFO = new Level("INFO", 800, defaultBundle);

    /**
     * CONFIG is a message level for static configuration messages.
     * <p>
     * CONFIG messages are intended to provide a variety of static
     * configuration information, to assist in debugging problems
     * that may be associated with particular configurations.
     * For example, CONFIG message might include the CPU type,
     * the graphics depth, the GUI look-and-feel, etc.
     * This level is initialized to <CODE>700</CODE>.
     */
    public static final Level CONFIG = new Level("CONFIG", 700, defaultBundle);

    /**
     * FINE is a message level providing tracing information.
     * <p>
     * All of FINE, FINER, and FINEST are intended for relatively
     * detailed tracing.  The exact meaning of the three levels will
     * vary between subsystems, but in general, FINEST should be used
     * for the most voluminous detailed output, FINER for somewhat
     * less detailed output, and FINE for the  lowest volume (and
     * most important) messages.
     * <p>
     * In general the FINE level should be used for information
     * that will be broadly interesting to developers who do not have
     * a specialized interest in the specific subsystem.
     * <p>
     * FINE messages might include things like minor (recoverable)
     * failures.  Issues indicating potential performance problems
     * are also worth logging as FINE.
     * This level is initialized to <CODE>500</CODE>.
     */
    public static final Level FINE = new Level("FINE", 500, defaultBundle);

    /**
     * FINER indicates a fairly detailed tracing message.
     * By default logging calls for entering, returning, or throwing
     * an exception are traced at this level.
     * This level is initialized to <CODE>400</CODE>.
     */
    public static final Level FINER = new Level("FINER", 400, defaultBundle);

    /**
     * FINEST indicates a highly detailed tracing message.
     * This level is initialized to <CODE>300</CODE>.
     */
    public static final Level FINEST = new Level("FINEST", 300, defaultBundle);

    /**
     * ALL indicates that all messages should be logged.
     * This level is initialized to <CODE>Integer.MIN_VALUE</CODE>.
     */
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE, defaultBundle);

    private static final Level[] standardLevels = {
        OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
    };

    /**
     * Create a named Level with a given integer value.
     * <p>
     * Note that this constructor is "protected" to allow subclassing.
     * In general clients of logging should use one of the constant Level
     * objects such as SEVERE or FINEST.  However, if clients need to
     * add new logging levels, they may subclass Level and define new
     * constants.
     * @param name  the name of the Level, for example "SEVERE".
     * @param value an integer value for the level.
     * @throws NullPointerException if the name is null
     */
    protected Level(String name, int value) {
        this(name, value, null);
    }

    /**
     * Create a named Level with a given integer value and a
     * given localization resource name.
     *
     * @param name  the name of the Level, for example "SEVERE".
     * @param value an integer value for the level.
     * @param resourceBundleName name of a resource bundle to use in
     *    localizing the given name. If the resourceBundleName is null
     *    or an empty string, it is ignored.
     * @throws NullPointerException if the name is null
     */
    protected Level(String name, int value, String resourceBundleName) {
        this(name, value, resourceBundleName, true);
    }

    // private constructor to specify whether this instance should be added
    // to the KnownLevel list from which Level.parse method does its look up
    private Level(String name, int value, String resourceBundleName, boolean visible) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.value = value;
        this.resourceBundleName = resourceBundleName;
        this.localizedLevelName = resourceBundleName == null ? name : null;
        this.cachedLocale = null;
        if (visible) {
            KnownLevel.add(this);
        }
    }

    /**
     * Return the level's localization resource bundle name, or
     * null if no localization bundle is defined.
     *
     * @return localization resource bundle name
     */
    public String getResourceBundleName() {
        return resourceBundleName;
    }

    /**
     * Return the non-localized string name of the Level.
     *
     * @return non-localized name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the localized string name of the Level, for
     * the current default locale.
     * <p>
     * If no localization information is available, the
     * non-localized name is returned.
     *
     * @return localized name
     */
    public String getLocalizedName() {
        return getLocalizedLevelName();
    }

    // package-private getLevelName() is used by the implementation
    // instead of getName() to avoid calling the subclass's version
    final String getLevelName() {
        return this.name;
    }

    private String computeLocalizedLevelName(Locale newLocale) {
        // Resource bundle should be loaded from the defining module
        // or its defining class loader, if it's unnamed module,
        // of this Level instance that can be a custom Level subclass;
        Module module = this.getClass().getModule();
        ResourceBundle rb = RbAccess.RB_ACCESS.getBundle(resourceBundleName,
                newLocale, module);

        final String localizedName = rb.getString(name);
        final boolean isDefaultBundle = defaultBundle.equals(resourceBundleName);
        if (!isDefaultBundle) return localizedName;

        // This is a trick to determine whether the name has been translated
        // or not. If it has not been translated, we need to use Locale.ROOT
        // when calling toUpperCase().
        final Locale rbLocale = rb.getLocale();
        final Locale locale =
                Locale.ROOT.equals(rbLocale)
                || name.equals(localizedName.toUpperCase(Locale.ROOT))
                ? Locale.ROOT : rbLocale;

        // ALL CAPS in a resource bundle's message indicates no translation
        // needed per Oracle translation guideline.  To workaround this
        // in Oracle JDK implementation, convert the localized level name
        // to uppercase for compatibility reason.
        return Locale.ROOT.equals(locale) ? name : localizedName.toUpperCase(locale);
    }

    // Avoid looking up the localizedLevelName twice if we already
    // have it.
    final String getCachedLocalizedLevelName() {

        if (localizedLevelName != null) {
            if (cachedLocale != null) {
                if (cachedLocale.equals(Locale.getDefault())) {
                    // OK: our cached value was looked up with the same
                    //     locale. We can use it.
                    return localizedLevelName;
                }
            }
        }

        if (resourceBundleName == null) {
            // No resource bundle: just use the name.
            return name;
        }

        // We need to compute the localized name.
        // Either because it's the first time, or because our cached
        // value is for a different locale. Just return null.
        return null;
    }

    final synchronized String getLocalizedLevelName() {

        // See if we have a cached localized name
        final String cachedLocalizedName = getCachedLocalizedLevelName();
        if (cachedLocalizedName != null) {
            return cachedLocalizedName;
        }

        // No cached localized name or cache invalid.
        // Need to compute the localized name.
        final Locale newLocale = Locale.getDefault();
        try {
            localizedLevelName = computeLocalizedLevelName(newLocale);
        } catch (Exception ex) {
            localizedLevelName = name;
        }
        cachedLocale = newLocale;
        return localizedLevelName;
    }

    // Returns a mirrored Level object that matches the given name as
    // specified in the Level.parse method.  Returns null if not found.
    //
    // It returns the same Level object as the one returned by Level.parse
    // method if the given name is a non-localized name or integer.
    //
    // If the name is a localized name, findLevel and parse method may
    // return a different level value if there is a custom Level subclass
    // that overrides Level.getLocalizedName() to return a different string
    // than what's returned by the default implementation.
    //
    static Level findLevel(String name) {
        if (name == null) {
            throw new NullPointerException();
        }

        Optional<Level> level;

        // Look for a known Level with the given non-localized name.
        level = KnownLevel.findByName(name, KnownLevel::mirrored);
        if (level.isPresent()) {
            return level.get();
        }

        // Now, check if the given name is an integer.  If so,
        // first look for a Level with the given value and then
        // if necessary create one.
        try {
            int x = Integer.parseInt(name);
            level = KnownLevel.findByValue(x, KnownLevel::mirrored);
            if (level.isPresent()) {
                return level.get();
            }
            // add new Level
            Level levelObject = new Level(name, x);
            // There's no need to use a reachability fence here because
            // KnownLevel keeps a strong reference on the level when
            // level.getClass() == Level.class.
            return KnownLevel.findByValue(x, KnownLevel::mirrored).get();
        } catch (NumberFormatException ex) {
            // Not an integer.
            // Drop through.
        }

        level = KnownLevel.findByLocalizedLevelName(name,
                KnownLevel::mirrored);
        if (level.isPresent()) {
            return level.get();
        }

        return null;
    }

    /**
     * Returns a string representation of this Level.
     *
     * @return the non-localized name of the Level, for example "INFO".
     */
    @Override
    public final String toString() {
        return name;
    }

    /**
     * Get the integer value for this level.  This integer value
     * can be used for efficient ordering comparisons between
     * Level objects.
     * @return the integer value for this level.
     */
    public final int intValue() {
        return value;
    }

    @Serial
    private static final long serialVersionUID = -8176160795706313070L;

    /**
     * Returns a {@code Level} instance with the same {@code name},
     * {@code value}, and {@code resourceBundleName} as the deserialized
     * object.
     * @return a {@code Level} instance corresponding to the deserialized
     * object.
     */
    @Serial
    private Object readResolve() {
        // Serialization magic to prevent "doppelgangers".
        // This is a performance optimization.
        Optional<Level> level = KnownLevel.matches(this);
        if (level.isPresent()) {
            return level.get();
        }
        // Woops.  Whoever sent us this object knows
        // about a new log level.  Add it to our list.
        return new Level(this.name, this.value, this.resourceBundleName);
    }

    /**
     * Parse a level name string into a Level.
     * <p>
     * The argument string may consist of either a level name
     * or an integer value.
     * <p>
     * For example:
     * <ul>
     * <li>     "SEVERE"
     * <li>     "1000"
     * </ul>
     *
     * @param  name   string to be parsed
     * @throws NullPointerException if the name is null
     * @throws IllegalArgumentException if the value is not valid.
     * Valid values are integers between <CODE>Integer.MIN_VALUE</CODE>
     * and <CODE>Integer.MAX_VALUE</CODE>, and all known level names.
     * Known names are the levels defined by this class (e.g., <CODE>FINE</CODE>,
     * <CODE>FINER</CODE>, <CODE>FINEST</CODE>), or created by this class with
     * appropriate package access, or new levels defined or created
     * by subclasses.
     *
     * @return The parsed value. Passing an integer that corresponds to a known name
     * (e.g., 700) will return the associated name (e.g., <CODE>CONFIG</CODE>).
     * Passing an integer that does not (e.g., 1) will return a new level name
     * initialized to that value.
     */
    public static synchronized Level parse(String name) throws IllegalArgumentException {
        // Check that name is not null.
        name.length();

        Optional<Level> level;

        // Look for a known Level with the given non-localized name.
        level = KnownLevel.findByName(name, KnownLevel::referent);
        if (level.isPresent()) {
            return level.get();
        }

        // Now, check if the given name is an integer.  If so,
        // first look for a Level with the given value and then
        // if necessary create one.
        try {
            int x = Integer.parseInt(name);
            level = KnownLevel.findByValue(x, KnownLevel::referent);
            if (level.isPresent()) {
                return level.get();
            }
            // add new Level.
            Level levelObject = new Level(name, x);
            // There's no need to use a reachability fence here because
            // KnownLevel keeps a strong reference on the level when
            // level.getClass() == Level.class.
            return KnownLevel.findByValue(x, KnownLevel::referent).get();
        } catch (NumberFormatException ex) {
            // Not an integer.
            // Drop through.
        }

        // Finally, look for a known level with the given localized name,
        // in the current default locale.
        // This is relatively expensive, but not excessively so.
        level = KnownLevel.findByLocalizedLevelName(name, KnownLevel::referent);
        if (level .isPresent()) {
            return level.get();
        }

        // OK, we've tried everything and failed
        throw new IllegalArgumentException("Bad level \"" + name + "\"");
    }

    /**
     * Compare two objects for value equality.
     * @return true if and only if the two objects have the same level value.
     */
    @Override
    public boolean equals(Object ox) {
        try {
            Level lx = (Level)ox;
            return (lx.value == this.value);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Generate a hashcode.
     * @return a hashcode based on the level value
     */
    @Override
    public int hashCode() {
        return this.value;
    }

    // KnownLevel class maintains the global list of all known levels.
    // The API allows multiple custom Level instances of the same name/value
    // be created. This class provides convenient methods to find a level
    // by a given name, by a given value, or by a given localized name.
    //
    // KnownLevel wraps the following Level objects:
    // 1. levelObject:   standard Level object or custom Level object
    // 2. mirroredLevel: Level object representing the level specified in the
    //                   logging configuration.
    //
    // Level.getName, Level.getLocalizedName, Level.getResourceBundleName methods
    // are non-final but the name and resource bundle name are parameters to
    // the Level constructor.  Use the mirroredLevel object instead of the
    // levelObject to prevent the logging framework to execute foreign code
    // implemented by untrusted Level subclass.
    //
    // Implementation Notes:
    // If Level.getName, Level.getLocalizedName, Level.getResourceBundleName methods
    // were final, the following KnownLevel implementation can be removed.
    // Future API change should take this into consideration.
    static final class KnownLevel extends WeakReference<Level> {
        private static Map<String, List<KnownLevel>> nameToLevels = new HashMap<>();
        private static Map<Integer, List<KnownLevel>> intToLevels = new HashMap<>();
        private static final ReferenceQueue<Level> QUEUE = new ReferenceQueue<>();

        // CUSTOM_LEVEL_CLV is used to register custom level instances with
        // their defining class loader, so that they are garbage collected
        // if and only if their class loader is no longer strongly
        // referenced.
        private static final ClassLoaderValue<List<Level>> CUSTOM_LEVEL_CLV =
                    new ClassLoaderValue<>();

        final Level mirroredLevel;   // mirror of the custom Level
        KnownLevel(Level l) {
            super(l, QUEUE);
            if (l.getClass() == Level.class) {
                this.mirroredLevel = l;
            } else {
                // this mirrored level object is hidden
                this.mirroredLevel = new Level(l.name, l.value,
                        l.resourceBundleName, false);
            }
        }

        Optional<Level> mirrored() {
            return Optional.of(mirroredLevel);
        }

        Optional<Level> referent() {
            return Optional.ofNullable(get());
        }

        private void remove() {
            Optional.ofNullable(nameToLevels.get(mirroredLevel.name))
                    .ifPresent((x) -> x.remove(this));
            Optional.ofNullable(intToLevels.get(mirroredLevel.value))
                    .ifPresent((x) -> x.remove(this));
        }

        // Remove all stale KnownLevel instances
        static synchronized void purge() {
            Reference<? extends Level> ref;
            while ((ref = QUEUE.poll()) != null) {
                if (ref instanceof KnownLevel) {
                    ((KnownLevel)ref).remove();
                }
            }
        }

        private static void registerWithClassLoader(Level customLevel) {
            PrivilegedAction<ClassLoader> pa = customLevel.getClass()::getClassLoader;
            @SuppressWarnings("removal")
            final ClassLoader cl = AccessController.doPrivileged(pa);
            CUSTOM_LEVEL_CLV.computeIfAbsent(cl, (c, v) -> new ArrayList<>())
                .add(customLevel);
        }

        static synchronized void add(Level l) {
            purge();
            // the mirroredLevel object is always added to the list
            // before the custom Level instance
            KnownLevel o = new KnownLevel(l);
            nameToLevels.computeIfAbsent(l.name, (k) -> new ArrayList<>())
                .add(o);
            intToLevels.computeIfAbsent(l.value, (k) -> new ArrayList<>())
                .add(o);

            // keep the custom level reachable from its class loader
            // This will ensure that custom level values are not GC'ed
            // until there class loader is GC'ed.
            if (o.mirroredLevel != l) {
                registerWithClassLoader(l);
            }

        }

        // Returns a KnownLevel with the given non-localized name.
        static synchronized Optional<Level> findByName(String name,
                Function<KnownLevel, Optional<Level>> selector) {
            purge();
            return nameToLevels.getOrDefault(name, Collections.emptyList())
                        .stream()
                        .map(selector)
                        .flatMap(Optional::stream)
                        .findFirst();
        }

        // Returns a KnownLevel with the given value.
        static synchronized Optional<Level> findByValue(int value,
                Function<KnownLevel, Optional<Level>> selector) {
            purge();
            return intToLevels.getOrDefault(value, Collections.emptyList())
                        .stream()
                        .map(selector)
                        .flatMap(Optional::stream)
                        .findFirst();
        }

        // Returns a KnownLevel with the given localized name matching
        // by calling the Level.getLocalizedLevelName() method (i.e. found
        // from the resourceBundle associated with the Level object).
        // This method does not call Level.getLocalizedName() that may
        // be overridden in a subclass implementation
        static synchronized Optional<Level> findByLocalizedLevelName(String name,
                Function<KnownLevel, Optional<Level>> selector) {
            purge();
            return nameToLevels.values().stream()
                         .flatMap(List::stream)
                         .map(selector)
                         .flatMap(Optional::stream)
                         .filter(l -> name.equals(l.getLocalizedLevelName()))
                         .findFirst();
        }

        static synchronized Optional<Level> matches(Level l) {
            purge();
            List<KnownLevel> list = nameToLevels.get(l.name);
            if (list != null) {
                for (KnownLevel ref : list) {
                    Level levelObject = ref.get();
                    if (levelObject == null) continue;
                    Level other = ref.mirroredLevel;
                    Class<? extends Level> type = levelObject.getClass();
                    if (l.value == other.value &&
                           (l.resourceBundleName == other.resourceBundleName ||
                               (l.resourceBundleName != null &&
                                l.resourceBundleName.equals(other.resourceBundleName)))) {
                        if (type == l.getClass()) {
                            return Optional.of(levelObject);
                        }
                    }
                }
            }
            return Optional.empty();
        }
    }

}
