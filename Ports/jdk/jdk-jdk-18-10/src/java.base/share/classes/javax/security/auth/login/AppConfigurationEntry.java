/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.login;

import java.util.Map;
import java.util.Collections;

/**
 * This class represents a single {@code LoginModule} entry
 * configured for the application specified in the
 * {@code getAppConfigurationEntry(String appName)}
 * method in the {@code Configuration} class.  Each respective
 * {@code AppConfigurationEntry} contains a {@code LoginModule} name,
 * a control flag (specifying whether this {@code LoginModule} is
 * REQUIRED, REQUISITE, SUFFICIENT, or OPTIONAL), and LoginModule-specific
 * options.  Please refer to the {@code Configuration} class for
 * more information on the different control flags and their semantics.
 *
 * @since 1.4
 * @see javax.security.auth.login.Configuration
 */
public class AppConfigurationEntry {

    private String loginModuleName;
    private LoginModuleControlFlag controlFlag;
    private Map<String,?> options;

    /**
     * Default constructor for this class.
     *
     * <p> This entry represents a single {@code LoginModule}
     * entry configured for the application specified in the
     * {@code getAppConfigurationEntry(String appName)}
     * method from the {@code Configuration} class.
     *
     * @param loginModuleName String representing the class name of the
     *                  {@code LoginModule} configured for the
     *                  specified application.
     *
     * @param controlFlag either REQUIRED, REQUISITE, SUFFICIENT,
     *                  or OPTIONAL.
     *
     * @param options the options configured for this {@code LoginModule}.
     *
     * @exception IllegalArgumentException if {@code loginModuleName}
     *                  is null, if {@code LoginModuleName}
     *                  has a length of 0, if {@code controlFlag}
     *                  is not either REQUIRED, REQUISITE, SUFFICIENT
     *                  or OPTIONAL, or if {@code options} is null.
     */
    public AppConfigurationEntry(String loginModuleName,
                                LoginModuleControlFlag controlFlag,
                                Map<String,?> options)
    {
        if (loginModuleName == null || loginModuleName.isEmpty() ||
            (controlFlag != LoginModuleControlFlag.REQUIRED &&
                controlFlag != LoginModuleControlFlag.REQUISITE &&
                controlFlag != LoginModuleControlFlag.SUFFICIENT &&
                controlFlag != LoginModuleControlFlag.OPTIONAL) ||
            options == null)
            throw new IllegalArgumentException();

        this.loginModuleName = loginModuleName;
        this.controlFlag = controlFlag;
        this.options = Collections.unmodifiableMap(options);
    }

    /**
     * Get the class name of the configured {@code LoginModule}.
     *
     * @return the class name of the configured {@code LoginModule} as
     *          a String.
     */
    public String getLoginModuleName() {
        return loginModuleName;
    }

    /**
     * Return the controlFlag
     * (either REQUIRED, REQUISITE, SUFFICIENT, or OPTIONAL)
     * for this {@code LoginModule}.
     *
     * @return the controlFlag
     *          (either REQUIRED, REQUISITE, SUFFICIENT, or OPTIONAL)
     *          for this {@code LoginModule}.
     */
    public LoginModuleControlFlag getControlFlag() {
        return controlFlag;
    }

    /**
     * Get the options configured for this {@code LoginModule}.
     *
     * @return the options configured for this {@code LoginModule}
     *          as an unmodifiable {@code Map}.
     */
    public Map<String,?> getOptions() {
        return options;
    }

    /**
     * This class represents whether or not a {@code LoginModule}
     * is REQUIRED, REQUISITE, SUFFICIENT or OPTIONAL.
     */
    public static class LoginModuleControlFlag {

        private String controlFlag;

        /**
         * Required {@code LoginModule}.
         */
        public static final LoginModuleControlFlag REQUIRED =
                                new LoginModuleControlFlag("required");

        /**
         * Requisite {@code LoginModule}.
         */
        public static final LoginModuleControlFlag REQUISITE =
                                new LoginModuleControlFlag("requisite");

        /**
         * Sufficient {@code LoginModule}.
         */
        public static final LoginModuleControlFlag SUFFICIENT =
                                new LoginModuleControlFlag("sufficient");

        /**
         * Optional {@code LoginModule}.
         */
        public static final LoginModuleControlFlag OPTIONAL =
                                new LoginModuleControlFlag("optional");

        private LoginModuleControlFlag(String controlFlag) {
            this.controlFlag = controlFlag;
        }

        /**
         * Return a String representation of this controlFlag.
         *
         * <p> The String has the format, "LoginModuleControlFlag: <i>flag</i>",
         * where <i>flag</i> is either <i>required</i>, <i>requisite</i>,
         * <i>sufficient</i>, or <i>optional</i>.
         *
         * @return a String representation of this controlFlag.
         */
        public String toString() {
            return (sun.security.util.ResourcesMgr.getString
                ("LoginModuleControlFlag.") + controlFlag);
        }
    }
}
