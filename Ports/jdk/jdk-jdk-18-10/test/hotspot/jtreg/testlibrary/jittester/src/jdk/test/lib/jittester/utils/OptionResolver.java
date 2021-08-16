/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class OptionResolver {
    private final Map<String, Option<?>> options = new LinkedHashMap<>(20);
    private Map<Option<?>, Object> values = Collections.emptyMap();

    public OptionResolver() {
    }

    public final void parse(String[] argv) {
        parse(argv, null);
    }

    public final void parse(String[] argv, Option<String> propertyFileOption) {
        int position = 0;
        this.values = new HashMap<>(argv.length / 2);

        while (position < argv.length) {
            String curArg = argv[position];
            if (curArg.startsWith("-")) {
                String valueArg = null;
                int opt;
                if (curArg.startsWith("--")) {
                    opt = curArg.indexOf("=");
                    if (opt != -1) {
                        valueArg = curArg.substring(opt + 1);
                        curArg = curArg.substring(0, opt);
                    }
                } else if (curArg.length() > 2) {
                    for (opt = 1; opt < curArg.length(); ++opt) {
                        final char key = curArg.charAt(opt);
                        Option<?> flagOption = this.options.get("-" + key);

                        if (!flagOption.isFlag()) {
                            throw new IllegalArgumentException("Unknown flag option " + key);
                        }

                        values.put(flagOption, Boolean.TRUE);
                    }

                    ++position;
                    continue;
                }

                Option<?> currentOption = this.options.get(curArg);
                if (currentOption == null) {
                    throw new IllegalArgumentException("Unknown option " + curArg);
                }

                Object value;
                if (!currentOption.isFlag()) {
                    if (valueArg == null) {
                        ++position;
                        if (position < argv.length) {
                            valueArg = argv[position];
                        }
                    }
                }
                try {
                    value = currentOption.parseFromString(valueArg);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Error parsing " + valueArg + ", option " + curArg, ex);
                }
                values.put(currentOption, value);
            }
            ++position;
        }

        if (propertyFileOption != null && values.containsKey(propertyFileOption)) {
            parseProperties(propertyFileOption.value());
        }
    }

    private void parseProperties(String fileName) {
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String optionName : properties.stringPropertyNames()) {
            Option<?> currentOption = this.options.get("--" + optionName);
            if (currentOption == null) {
                throw new IllegalArgumentException("Unknown option in property file" + optionName);
            }

            final String propertyValue = properties.getProperty(optionName);
            try {
                values.putIfAbsent(currentOption, currentOption.parseFromString(propertyValue));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error parsing " + propertyValue + ", property " + optionName, ex);
            }
        }
    }

    public Option<Integer> addIntegerOption(Character key, String name, int defaultValue, String description) {
        final Option<Integer> option = new IntOption(key, name, defaultValue, description);
        register(option);
        return option;
    }

    public Option<Long> addLongOption(Character key, String name, long defaultValue, String description) {
        final Option<Long> option = new LongOption(key, name, defaultValue, description);
        register(option);
        return option;
    }

    public Option<String> addStringOption(Character key, String name, String defaultValue, String description) {
        final Option<String> option = new StringOption(key, name, defaultValue, description);
        register(option);
        return option;
    }

    public Option<Boolean> addBooleanOption(Character key, String name, boolean defaultValue, String description) {
        final Option<Boolean> option = new BooleanOption(key, name, defaultValue, description);
        register(option);
        return option;
    }

    public Option<Integer> addIntegerOption(String name, int defaultValue, String description) {
        return addIntegerOption(null, name, defaultValue, description);
    }

    public Option<String> addStringOption(String name, String defaultValue, String description) {
        return addStringOption(null, name, defaultValue, description);
    }

    public Option<Boolean> addBooleanOption(String name, String description) {
        return addBooleanOption(null, name, false, description);
    }

    private void register(Option<?> option) {
        if (options.put("--" + option.longName, option) != null) {
            throw new RuntimeException("Option is already registered for key " + option.longName);
        }
        if (option.shortName != null && options.put("-" + option.shortName, option) != null) {
            throw new RuntimeException("Option is already registered for key " + option.shortName);
        }
    }

    public abstract class Option<T> {
        protected final Character shortName;
        protected final String longName;
        protected final T defaultValue;
        protected final String description;

        public Option(Character shortName, String longName, T defaultValue, String description) {
            this.shortName = shortName;
            this.longName = longName;
            this.defaultValue = defaultValue;
            this.description = description;
        }

        public Character getShortName() {
            return shortName;
        }

        public String getLongName() {
            return longName;
        }

        public T getDefaultValue() {
            return defaultValue;
        }

        public String getDescription() {
            return description;
        }

        @SuppressWarnings("unchecked")
        public T value() {
            return (T) values.getOrDefault(this, defaultValue);
        }

        public boolean isSet() {
            return values.containsKey(this);
        }

        public boolean isFlag() {
            return false;
        }

        public abstract T parseFromString(String arg);
    }

    private class StringOption extends Option<String> {

        StringOption(Character s, String l, String v, String d) {
            super(s, l, v, d);
        }

        @Override
        public String parseFromString(String arg) {
            return arg;
        }
    }

    private class LongOption extends Option<Long> {

        LongOption(Character s, String l, long v, String d) {
            super(s, l, v, d);
        }

        @Override
        public Long parseFromString(String arg) {
            return Long.valueOf(arg);
        }
    }

    private class IntOption extends Option<Integer> {

        IntOption(Character s, String l, int v, String d) {
            super(s, l, v, d);
        }

        @Override
        public Integer parseFromString(String arg) {
            return Integer.valueOf(arg);
        }
    }

    private class BooleanOption extends Option<Boolean> {

        BooleanOption(Character s, String l, boolean v, String d) {
            super(s, l, v, d);
        }

        @Override
        public boolean isFlag() {
            return true;
        }

        @Override
        public Boolean parseFromString(String arg) {
            //null and empty value is considered true, as option is flag and value could be absent
            return arg == null || "".equals(arg) || "1".equalsIgnoreCase(arg) || "true".equalsIgnoreCase(arg);
        }
    }

    public Collection<Option<?>> getRegisteredOptions() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(options.values()));
    }
}
