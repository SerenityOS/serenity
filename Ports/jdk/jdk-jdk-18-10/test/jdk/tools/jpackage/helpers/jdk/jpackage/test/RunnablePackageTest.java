/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class RunnablePackageTest {
    public final void run(Action... actions) {
        final List<Action> actionList = new ArrayList<>();
        actionList.add(Action.INITIALIZE);
        if (actions.length == 0) {
            actionList.addAll(DEFAULT_ACTIONS);
        } else {
            actionList.addAll(Stream.of(actions)
                    .filter(Predicate.not(Action.INITIALIZE::equals))
                    .filter(Predicate.not(Action.FINALIZE::equals))
                    .collect(Collectors.toList()));
        }
        actionList.add(Action.FINALIZE);

        var actionGroups = groupActions(actionList);
        TKit.trace(String.format("Actions: " + Arrays.deepToString(
                actionGroups.toArray(Action[][]::new))));

        runActions(actionGroups);
    }

    protected void runActions(List<Action[]> actions) {
        actions.forEach(this::runAction);
    }

    protected abstract void runAction(Action... action);

    /**
     * Test action.
     */
    static public enum Action {
        /**
         * Init test.
         */
        INITIALIZE,
        /**
         * Create bundle.
         */
        CREATE,
        /**
         * Verify unpacked/installed package.
         */
        VERIFY_INSTALL,
        /**
         * Verify uninstalled package.
         */
        VERIFY_UNINSTALL,
        /**
         * Unpack package bundle.
         */
        UNPACK,
        /**
         * Install package.
         */
        INSTALL,
        /**
         * Uninstall package.
         */
        UNINSTALL,
        /**
         * Finalize test.
         */
        FINALIZE;

        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '-');
        }

        public final static Action[] CREATE_AND_UNPACK = new Action[] {
            CREATE, UNPACK, VERIFY_INSTALL
        };
    };

    private List<Action[]> groupActions(List<Action> actions) {
        List<Action[]> groups = new ArrayList<>();
        List<Action> group = null;
        for (var action: actions) {
            if (group == null) {
                group = new ArrayList<>();
                group.add(action);
            } else if (group.get(group.size() - 1) == Action.INSTALL
                    && action == Action.VERIFY_INSTALL) {
                // Group `install` and `verify install` actions together
                group.add(action);
            } else {
                groups.add(group.toArray(Action[]::new));
                group.clear();
                group.add(action);
            }
        }
        if (group != null) {
            groups.add(group.toArray(Action[]::new));
        }

        return groups;
    }

    private final static List<Action> DEFAULT_ACTIONS;

    static {
        final String propertyName = "action";
        List<String> actions = TKit.tokenizeConfigPropertyAsList(propertyName);
        if (actions == null || actions.isEmpty()) {
            DEFAULT_ACTIONS = List.of(Action.CREATE_AND_UNPACK);
        } else {
            try {
                DEFAULT_ACTIONS = actions.stream()
                        .map(String::toUpperCase)
                        .map(v -> v.replace('-', '_'))
                        .map(Action::valueOf)
                        .collect(Collectors.toUnmodifiableList());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(String.format(
                        "Unrecognized value of %s property: [%s]",
                        TKit.getConfigPropertyName(propertyName),
                        TKit.getConfigProperty(propertyName)), ex);
            }
        }
    }
}
