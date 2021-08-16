/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

import java.util.*;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;

import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Messages;


/**
 * Process and manage grouping of elements, as specified by "-group" option on
 * the command line.
 * <p>
 * For example, if user has used -group option as
 * -group "Core Packages" "java.*" -group "CORBA Packages" "org.omg.*", then
 * the packages specified on the command line will be grouped according to their
 * names starting with either "java." or "org.omg.". All the other packages
 * which do not fall in the user given groups, are grouped in default group,
 * named as either "Other Packages" or "Packages" depending upon if "-group"
 * option used or not at all used respectively.
 * </p>
 * <p>
 * Also the packages are grouped according to the longest possible match of
 * their names with the grouping information provided. For example, if there
 * are two groups, like -group "Lang" "java.lang" and -group "Core" "java.*",
 * will put the package java.lang in the group "Lang" and not in group "Core".
 * </p>
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Group {

    /**
     * Map of regular expressions with the corresponding group name.
     */
    private Map<String,String> regExpGroupMap = new HashMap<>();

    /**
     * List of regular expressions sorted according to the length. Regular
     * expression with longest length will be first in the sorted order.
     */
    private List<String> sortedRegExpList = new ArrayList<>();

    /**
     * List of group names in the same order as given on the command line.
     */
    private List<String> groupList = new ArrayList<>();

    /**
     * Map of non-regular expressions(possible package or module names) with the
     * corresponding group name.
     */
    private Map<String,String> elementNameGroupMap = new HashMap<>();

    /**
     * The global configuration information for this run.
     */
    private final BaseConfiguration configuration;
    private Messages messages;

    /**
     * Since we need to sort the keys in the reverse order(longest key first),
     * the compare method in the implementing class is doing the reverse
     * comparison.
     */
    private static class MapKeyComparator implements Comparator<String> {
        @Override
        public int compare(String key1, String key2) {
            return key2.length() - key1.length();
        }
    }

    public Group(BaseConfiguration configuration) {
        this.configuration = configuration;
        messages = configuration.getMessages();
    }

    /**
     * Depending upon the format of the module name provided in the "-group"
     * option, generate two separate maps. There will be a map for mapping
     * regular expression(only meta character allowed is '*' and that is at the
     * end of the regular expression) on to the group name. And another map
     * for mapping (possible) module names(if the name format doesn't contain
     * meta character '*', then it is assumed to be a module name) on to the
     * group name. This will also sort all the regular expressions found in the
     * reverse order of their lengths, i.e. longest regular expression will be
     * first in the sorted list.
     *
     * @param groupname       The name of the group from -group option.
     * @param moduleNameFormList List of the module name formats.
     */
    public boolean checkModuleGroups(String groupname, String moduleNameFormList) {
        String[] mdlPatterns = moduleNameFormList.split(":");
        if (groupList.contains(groupname)) {
            initMessages();
            messages.warning("doclet.Groupname_already_used", groupname);
            return false;
        }
        groupList.add(groupname);
        for (String mdlPattern : mdlPatterns) {
            if (mdlPattern.length() == 0) {
                initMessages();
                messages.warning("doclet.Error_in_grouplist", groupname, moduleNameFormList);
                return false;
            }
            if (mdlPattern.endsWith("*")) {
                mdlPattern = mdlPattern.substring(0, mdlPattern.length() - 1);
                if (foundGroupFormat(regExpGroupMap, mdlPattern)) {
                    return false;
                }
                regExpGroupMap.put(mdlPattern, groupname);
                sortedRegExpList.add(mdlPattern);
            } else {
                if (foundGroupFormat(elementNameGroupMap, mdlPattern)) {
                    return false;
                }
                elementNameGroupMap.put(mdlPattern, groupname);
            }
        }
        Collections.sort(sortedRegExpList, new MapKeyComparator());
        return true;
    }

    /**
     * Depending upon the format of the package name provided in the "-group"
     * option, generate two separate maps. There will be a map for mapping
     * regular expression(only meta character allowed is '*' and that is at the
     * end of the regular expression) on to the group name. And another map
     * for mapping (possible) package names(if the name format doesn't contain
     * meta character '*', then it is assumed to be a package name) on to the
     * group name. This will also sort all the regular expressions found in the
     * reverse order of their lengths, i.e. longest regular expression will be
     * first in the sorted list.
     *
     * @param groupname       The name of the group from -group option.
     * @param pkgNameFormList List of the package name formats.
     */
    public boolean checkPackageGroups(String groupname, String pkgNameFormList) {
        String[] pkgPatterns = pkgNameFormList.split(":");
        if (groupList.contains(groupname)) {
            initMessages();
            messages.warning("doclet.Groupname_already_used", groupname);
            return false;
        }
        groupList.add(groupname);
        for (String pkgPattern : pkgPatterns) {
            if (pkgPattern.length() == 0) {
                initMessages();
                messages.warning("doclet.Error_in_grouplist", groupname, pkgNameFormList);
                return false;
            }
            if (pkgPattern.endsWith("*")) {
                pkgPattern = pkgPattern.substring(0, pkgPattern.length() - 1);
                if (foundGroupFormat(regExpGroupMap, pkgPattern)) {
                    return false;
                }
                regExpGroupMap.put(pkgPattern, groupname);
                sortedRegExpList.add(pkgPattern);
            } else {
                if (foundGroupFormat(elementNameGroupMap, pkgPattern)) {
                    return false;
                }
                elementNameGroupMap.put(pkgPattern, groupname);
            }
        }
        Collections.sort(sortedRegExpList, new MapKeyComparator());
        return true;
    }

    // Lazy init of the messages for now, because Group is created
    // in BaseConfiguration before configuration is fully initialized.
    private void initMessages() {
        if (messages == null) {
            messages = configuration.getMessages();
        }
    }

    /**
     * Search if the given map has the given element format.
     *
     * @param map Map to be searched.
     * @param elementFormat The format to search.
     *
     * @return true if element name format found in the map, else false.
     */
    boolean foundGroupFormat(Map<String,?> map, String elementFormat) {
        if (map.containsKey(elementFormat)) {
            initMessages();
            messages.error("doclet.Same_element_name_used", elementFormat);
            return true;
        }
        return false;
    }

    /**
     * Group the modules according the grouping information provided on the
     * command line. Given a list of modules, search each module name in
     * regular expression map as well as module name map to get the
     * corresponding group name. Create another map with mapping of group name
     * to the module list, which will fall under the specified group. If any
     * module doesn't belong to any specified group on the command line, then
     * a new group named "Other Modules" will be created for it. If there are
     * no groups found, in other words if "-group" option is not at all used,
     * then all the modules will be grouped under group "Modules".
     *
     * @param modules Specified modules.
     * @return map of group names and set of module elements.
     */
    public Map<String, SortedSet<ModuleElement>> groupModules(Set<ModuleElement> modules) {
        Map<String, SortedSet<ModuleElement>> groupModuleMap = new HashMap<>();
        String defaultGroupName =
            (elementNameGroupMap.isEmpty() && regExpGroupMap.isEmpty())?
                configuration.getDocResources().getText("doclet.Modules") :
                configuration.getDocResources().getText("doclet.Other_Modules");
        // if the user has not used the default group name, add it
        if (!groupList.contains(defaultGroupName)) {
            groupList.add(defaultGroupName);
        }
        for (ModuleElement mdl : modules) {
            String moduleName = mdl.isUnnamed() ? null : mdl.getQualifiedName().toString();
            String groupName = mdl.isUnnamed() ? null : elementNameGroupMap.get(moduleName);
            // if this module is not explicitly assigned to a group,
            // try matching it to group specified by regular expression
            if (groupName == null) {
                groupName = regExpGroupName(moduleName);
            }
            // if it is in neither group map, put it in the default
            // group
            if (groupName == null) {
                groupName = defaultGroupName;
            }
            getModuleList(groupModuleMap, groupName).add(mdl);
        }
        return groupModuleMap;
    }

    /**
     * Group the packages according the grouping information provided on the
     * command line. Given a list of packages, search each package name in
     * regular expression map as well as package name map to get the
     * corresponding group name. Create another map with mapping of group name
     * to the package list, which will fall under the specified group. If any
     * package doesn't belong to any specified group on the command line, then
     * a new group named "Other Packages" will be created for it. If there are
     * no groups found, in other words if "-group" option is not at all used,
     * then all the packages will be grouped under group "Packages".
     *
     * @param packages Packages specified on the command line.
     * @return map of group names and set of package elements
     */
    public Map<String, SortedSet<PackageElement>> groupPackages(Set<PackageElement> packages) {
        Map<String, SortedSet<PackageElement>> groupPackageMap = new HashMap<>();
        String defaultGroupName =
            (elementNameGroupMap.isEmpty() && regExpGroupMap.isEmpty())?
                configuration.getDocResources().getText("doclet.Packages") :
                configuration.getDocResources().getText("doclet.Other_Packages");
        // if the user has not used the default group name, add it
        if (!groupList.contains(defaultGroupName)) {
            groupList.add(defaultGroupName);
        }
        for (PackageElement pkg : packages) {
            String pkgName = configuration.utils.getPackageName(pkg);
            String groupName = pkg.isUnnamed() ? null : elementNameGroupMap.get(pkgName);
            // if this package is not explicitly assigned to a group,
            // try matching it to group specified by regular expression
            if (groupName == null) {
                groupName = regExpGroupName(pkgName);
            }
            // if it is in neither group map, put it in the default
            // group
            if (groupName == null) {
                groupName = defaultGroupName;
            }
            getPkgList(groupPackageMap, groupName).add(pkg);
        }
        return groupPackageMap;
    }

    /**
     * Search for element name in the sorted regular expression
     * list, if found return the group name.  If not, return null.
     *
     * @param elementName Name of element to be found in the regular
     * expression list.
     */
    String regExpGroupName(String elementName) {
        for (String regexp : sortedRegExpList) {
            if (elementName.startsWith(regexp)) {
                return regExpGroupMap.get(regexp);
            }
        }
        return null;
    }

    /**
     * For the given group name, return the package list, on which it is mapped.
     * Create a new list, if not found.
     *
     * @param map Map to be searched for group name.
     * @param groupname Group name to search.
     */
    SortedSet<PackageElement> getPkgList(Map<String, SortedSet<PackageElement>> map,
            String groupname) {
        return map.computeIfAbsent(groupname, g -> new TreeSet<>(configuration.utils.comparators.makePackageComparator()));
    }

    /**
     * For the given group name, return the module list, on which it is mapped.
     * Create a new list, if not found.
     *
     * @param map Map to be searched for group name.
     * @param groupname Group name to search.
     */
    SortedSet<ModuleElement> getModuleList(Map<String, SortedSet<ModuleElement>> map,
            String groupname) {
        return map.computeIfAbsent(groupname, g -> new TreeSet<>(configuration.utils.comparators.makeModuleComparator()));
    }

    /**
     * Return the list of groups, in the same order as specified
     * on the command line.
     */
    public List<String> getGroupList() {
        return groupList;
    }
}
