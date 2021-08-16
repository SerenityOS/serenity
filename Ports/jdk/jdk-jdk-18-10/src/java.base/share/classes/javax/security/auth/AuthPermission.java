/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth;

/**
 * This class is for authentication permissions. An {@code AuthPermission}
 * contains a name (also referred to as a "target name") but no actions
 * list; you either have the named permission or you don't.
 *
 * <p> The target name is the name of a security configuration parameter
 * (see below).  Currently the {@code AuthPermission} object is used to
 * guard access to the {@link Subject},
 * {@link javax.security.auth.login.LoginContext}, and
 * {@link javax.security.auth.login.Configuration} objects.
 *
 * <p> The standard target names for an Authentication Permission are:
 *
 * <pre>
 *      doAs -                  allow the caller to invoke the
 *                              {@code Subject.doAs} methods.
 *
 *      doAsPrivileged -        allow the caller to invoke the
 *                              {@code Subject.doAsPrivileged} methods.
 *
 *      getSubject -            allow for the retrieval of the
 *                              Subject(s) associated with the
 *                              current Thread.
 *
 *      getSubjectFromDomainCombiner -  allow for the retrieval of the
 *                              Subject associated with the
 *                              a {@code SubjectDomainCombiner}.
 *
 *      setReadOnly -           allow the caller to set a Subject
 *                              to be read-only.
 *
 *      modifyPrincipals -      allow the caller to modify the {@code Set}
 *                              of Principals associated with a
 *                              {@code Subject}
 *
 *      modifyPublicCredentials - allow the caller to modify the
 *                              {@code Set} of public credentials
 *                              associated with a {@code Subject}
 *
 *      modifyPrivateCredentials - allow the caller to modify the
 *                              {@code Set} of private credentials
 *                              associated with a {@code Subject}
 *
 *      refreshCredential -     allow code to invoke the {@code refresh}
 *                              method on a credential which implements
 *                              the {@code Refreshable} interface.
 *
 *      destroyCredential -     allow code to invoke the {@code destroy}
 *                              method on a credential {@code object}
 *                              which implements the {@code Destroyable}
 *                              interface.
 *
 *      createLoginContext.{name} -  allow code to instantiate a
 *                              {@code LoginContext} with the
 *                              specified {@code name}.  {@code name}
 *                              is used as the index into the installed login
 *                              {@code Configuration}
 *                              (that returned by
 *                              {@code Configuration.getConfiguration()}).
 *                              <i>name</i> can be wildcarded (set to '*')
 *                              to allow for any name.
 *
 *      getLoginConfiguration - allow for the retrieval of the system-wide
 *                              login Configuration.
 *
 *      createLoginConfiguration.{type} - allow code to obtain a Configuration
 *                              object via
 *                              {@code Configuration.getInstance}.
 *
 *      setLoginConfiguration - allow for the setting of the system-wide
 *                              login Configuration.
 *
 *      refreshLoginConfiguration - allow for the refreshing of the system-wide
 *                              login Configuration.
 * </pre>
 *
 * <p>Please note that granting this permission with the "modifyPrincipals",
 * "modifyPublicCredentials" or "modifyPrivateCredentials" target allows
 * a JAAS login module to populate principal or credential objects into
 * the Subject. Although reading information inside the private credentials
 * set requires a {@link PrivateCredentialPermission} of the credential type to
 * be granted, reading information inside the principals set and the public
 * credentials set requires no additional permission. These objects can contain
 * potentially sensitive information. For example, login modules that read
 * local user information or perform a Kerberos login are able to add
 * potentially sensitive information such as user ids, groups and domain names
 * to the principals set.
 *
 * <p> The following target name has been deprecated in favor of
 * {@code createLoginContext.{name}}.
 *
 * <pre>
 *      createLoginContext -    allow code to instantiate a
 *                              {@code LoginContext}.
 * </pre>
 *
 * @implNote
 * Implementations may define additional target names, but should use naming
 * conventions such as reverse domain name notation to avoid name clashes.
 * @since 1.4
 */
public final class AuthPermission extends
java.security.BasicPermission {

    @java.io.Serial
    private static final long serialVersionUID = 5806031445061587174L;

    /**
     * Creates a new AuthPermission with the specified name.
     * The name is the symbolic name of the AuthPermission.
     *
     * @param name the name of the AuthPermission
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */
    public AuthPermission(String name) {
        // for backwards compatibility --
        // createLoginContext is deprecated in favor of createLoginContext.*
        super("createLoginContext".equals(name) ?
                "createLoginContext.*" : name);
    }

    /**
     * Creates a new AuthPermission object with the specified name.
     * The name is the symbolic name of the AuthPermission, and the
     * actions String is currently unused and should be null.
     *
     * @param name the name of the AuthPermission
     *
     * @param actions should be null.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */
    public AuthPermission(String name, String actions) {
        // for backwards compatibility --
        // createLoginContext is deprecated in favor of createLoginContext.*
        super("createLoginContext".equals(name) ?
                "createLoginContext.*" : name, actions);
    }
}
