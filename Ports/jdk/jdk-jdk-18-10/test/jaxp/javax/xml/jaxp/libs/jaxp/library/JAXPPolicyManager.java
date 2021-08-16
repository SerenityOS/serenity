/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package jaxp.library;


import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.StringJoiner;


/*
 * This is a base class that every test class must extend if it needs to be run
 * with security mode.
 */
public class JAXPPolicyManager {
    /*
     * Backing up policy.
     */
    private Policy policyBackup;

    /*
     * Backing up security manager.
     */
    private SecurityManager smBackup;

    /*
     * Current policy.
     */
    private TestPolicy policy = new TestPolicy();

    /*
     * JAXPPolicyManager singleton.
     */
    private static JAXPPolicyManager policyManager = null;

    /*
     * Install a SecurityManager along with a default Policy to allow testNG to
     * run when there is a security manager.
     */
    private JAXPPolicyManager() {
        // Backing up policy and security manager for restore
        policyBackup = Policy.getPolicy();
        smBackup = System.getSecurityManager();

        // Set customized policy
        setDefaultPermissions();
        Policy.setPolicy(policy);
        System.setSecurityManager(new SecurityManager());
    }

    static synchronized JAXPPolicyManager getJAXPPolicyManager(boolean createIfNone) {
        if (policyManager == null & createIfNone)
            policyManager = new JAXPPolicyManager();
        return policyManager;
    }

    private void teardown() throws Exception {
        System.setSecurityManager(smBackup);
        Policy.setPolicy(policyBackup);
    }

    /*
     * Restore the original Policy and SecurityManager.
     */
    static synchronized void teardownPolicyManager() throws Exception {
        if (policyManager != null) {
            policyManager.teardown();
            policyManager = null;
        }
    }

    /*
     * Set default permissions, sub-class of JAXPBaseTest should override this
     * method.
     */
    private void setDefaultPermissions() {
        //Permissions to set security manager and policy
        addPermission(new SecurityPermission("getPolicy"));
        addPermission(new SecurityPermission("setPolicy"));
        addPermission(new RuntimePermission("setSecurityManager"));
        addPermission(new PropertyPermission("test.src", "read"));
    }

    /*
     * Add permission to the TestPolicy.
     *
     * @param permission to be added.
     */
    void addPermission(Permission p) {
        policy.addPermission(p);
    }

    /*
     * Add a temporary permission in current thread context. This won't impact
     * global policy and doesn't support permission combination.
     *
     * @param permission
     *            to add.
     * @return index of the added permission.
     */
    int addTmpPermission(Permission p) {
        return policy.addTmpPermission(p);
    }

    /*
     * set allowAll in current thread context.
     */
    void setAllowAll(boolean allow) {
        policy.setAllowAll(allow);
    }

    /*
     * Remove a temporary permission from current thread context.
     *
     * @param index to remove.
     *
     * @throws RuntimeException if no temporary permission list in current
     *             thread context or no permission correlated to the index.
     */
    void removeTmpPermission(int index) {
        policy.removeTmpPermission(index);
    }


}

/*
 * Simple Policy class that supports the required Permissions to validate the
 * JAXP concrete classes.
 */
class TestPolicy extends Policy {
    private final static Set<String> TEST_JARS =
         Set.of("jtreg.jar", "javatest.jar", "testng.jar", "jcommander.jar");
    private final PermissionCollection permissions = new Permissions();

    private ThreadLocal<Map<Integer, Permission>> transientPermissions = new ThreadLocal<>();
    private ThreadLocal<Boolean> allowAll = new ThreadLocal<>();

    private static Policy defaultPolicy = Policy.getPolicy();

    /*
     * Add permission to this policy.
     *
     * @param permission to be added.
     */
    void addPermission(Permission p) {
        permissions.add(p);
    }

    /*
     * Set all permissions. Caution: this should not called carefully unless
     * it's really needed.
     *
     * private void setAllPermissions() { permissions.add(new AllPermission());
     * }
     */

    /*
     * Overloaded methods from the Policy class.
     */
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner("\n", "policy: ", "");
        Enumeration<Permission> perms = permissions.elements();
        while (perms.hasMoreElements()) {
            sj.add(perms.nextElement().toString());
        }
        return sj.toString();

    }

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return permissions;
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return permissions;
    }

    private boolean isTestMachineryDomain(ProtectionDomain domain) {
        CodeSource cs = (domain == null) ? null : domain.getCodeSource();
        URL loc = (cs == null) ? null : cs.getLocation();
        String path = (loc == null) ? null : loc.getPath();
        return path != null && TEST_JARS.stream()
                                .filter(path::endsWith)
                                .findAny()
                                .isPresent();
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission perm) {
        if (allowAll())
            return true;

        if (defaultPolicy.implies(domain, perm))
            return true;

        if (permissions.implies(perm))
            return true;

        if (isTestMachineryDomain(domain))
            return true;

        return tmpImplies(perm);
    }

    /*
     * Add a temporary permission in current thread context. This won't impact
     * global policy and doesn't support permission combination.
     *
     * @param permission to add.
     * @return index of the added permission.
     */
    int addTmpPermission(Permission p) {
        Map<Integer, Permission> tmpPermissions = transientPermissions.get();
        if (tmpPermissions == null)
            tmpPermissions = new HashMap<>();

        int id = tmpPermissions.size();
        tmpPermissions.put(id, p);
        transientPermissions.set(tmpPermissions);
        return id;
    }

    /*
     * Remove a temporary permission from current thread context.
     *
     * @param index to remove.
     *
     * @throws RuntimeException if no temporary permission list in current
     *             thread context or no permission correlated to the index.
     */
    void removeTmpPermission(int index) {
        try {
            Map<Integer, Permission> tmpPermissions = transientPermissions.get();
            tmpPermissions.remove(index);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            throw new RuntimeException("Tried to delete a non-existent temporary permission", e);
        }
    }

    /*
     * Checks to see if the specified permission is implied by temporary
     * permission list in current thread context.
     *
     * @param permission the Permission object to compare.
     *
     * @return true if "permission" is implied by any permission in the
     *         temporary permission list, false if not.
     */
    private boolean tmpImplies(Permission perm) {
        Map<Integer, Permission> tmpPermissions = transientPermissions.get();
        if (tmpPermissions != null) {
            for (Permission p : tmpPermissions.values()) {
                if (p.implies(perm))
                    return true;
            }
        }
        return false;
    }

    /*
     * Checks to see if allow all permission requests in current thread context.
     */
    private boolean allowAll() {
        Boolean allow = allowAll.get();
        if (allow != null) {
            return allow;
        }
        return false;
    }

    /*
     * set allowAll in current thread context.
     */
    void setAllowAll(boolean allow) {
        allowAll.set(allow);
    }
}
