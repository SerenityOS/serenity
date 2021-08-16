/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4847959 6191402
 * @summary Test newly-generified APIs
 * @author Eamonn McManus
 *
 * @run clean GenericTest
 * @run build GenericTest
 * @run main GenericTest
 */

import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;
import javax.management.*;
import javax.management.openmbean.*;
import javax.management.relation.*;
import javax.management.timer.Timer;
import javax.management.timer.TimerMBean;

public class GenericTest {
    private static int failures;

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Check we are really using the generified version
        boolean generic;
        Method findmbs = MBeanServerFactory.class.getMethod("findMBeanServer",
                                                            String.class);
        Type findmbstype = findmbs.getGenericReturnType();
        if (!(findmbstype instanceof ParameterizedType)) {
            System.out.println("FAILURE: API NOT GENERIC!");
            System.out.println("  MBeanServerFactory.findMBeanServer -> " +
                               findmbstype);
            failures++;
            generic = false;
        } else {
            System.out.println("OK: this API is generic");
            generic = true;
        }

        ArrayList<MBeanServer> mbsList1 =
            MBeanServerFactory.findMBeanServer(null);
        checked(mbsList1, MBeanServer.class);
        ArrayList mbsList2 = MBeanServerFactory.findMBeanServer(null);
        check("ArrayList<MBeanServer> findMBeanServer", mbsList1.size() == 1);
        check("ArrayList findMBeanServer", mbsList1.equals(mbsList2));

        boolean isSecondAttempt = false;
        Set<ObjectName> names1 = null;
        while (true) {
            names1 = checked(mbs.queryNames(null, null), ObjectName.class);
            Set names2 = mbs.queryNames(null, null);
            Set<ObjectName> names3 =
                    checked(((MBeanServerConnection) mbs).queryNames(null, null),
                            ObjectName.class);
            // If new MBean (e.g. Graal MBean) is registered while the test is running, names1,
            // names2, and names3 will have different sizes. Repeat the test in this case.
            if (sameSize(names1, names2, names3) || isSecondAttempt) {
                check("Set<ObjectName> MBeanServer.queryNames", names1.size() >= 1);
                check("Set MBeanServer.queryNames", names2.size() >= 1);
                check("Set<ObjectName> MBeanServerConnection.queryNames",
                        names3.size() >= 1);
                check("queryNames sets same",
                        names1.equals(names2) && names2.equals(names3));
                break;
            }
            isSecondAttempt = true;
            System.out.println("queryNames sets have different size, retrying...");
        }

        isSecondAttempt = false;
        while (true) {
            Set<ObjectInstance> mbeans1 =
                    checked(mbs.queryMBeans(null, null), ObjectInstance.class);
            Set mbeans2 = mbs.queryMBeans(null, null);
            Set<ObjectInstance> mbeans3 =
                    checked(((MBeanServerConnection) mbs).queryMBeans(null, null),
                            ObjectInstance.class);
            // If new MBean (e.g. Graal MBean) is registered while the test is running, mbeans1,
            // mbeans2, and mbeans3 will have different sizes. Repeat the test in this case.
            if (sameSize(mbeans1, mbeans2, mbeans3) || isSecondAttempt) {
                check("Set<ObjectInstance> MBeanServer.queryMBeans",
                        mbeans1.size() >= 1);
                check("Set MBeanServer.queryMBeans", mbeans2.size() >= 1);
                check("Set<ObjectInstance> MBeanServerConnection.queryMBeans",
                        mbeans3.size() >= 1);
                check("queryMBeans sets same",
                        mbeans1.equals(mbeans2) && mbeans2.equals(mbeans3));
                break;
            }
            isSecondAttempt = true;
            System.out.println("queryMBeans sets have different size, retrying...");
        }

        AttributeChangeNotificationFilter acnf =
            new AttributeChangeNotificationFilter();
        acnf.enableAttribute("foo");
        Vector<String> acnfs = acnf.getEnabledAttributes();
        checked(acnfs, String.class);
        check("Vector<String> AttributeChangeNotificationFilter.getEnabled" +
              "Attributes", acnfs.equals(Arrays.asList(new String[] {"foo"})));

        if (generic) {
            Attribute a = new Attribute("foo", "bar");
            AttributeList al1 = new AttributeList();
            al1.add(a);
            AttributeList al2 =
                new AttributeList(Arrays.asList(new Attribute[] {a}));
            check("new AttributeList(List<Attribute>)", al1.equals(al2));
            List<Attribute> al3 = checked(al1.asList(), Attribute.class);
            al3.remove(a);
            check("List<Attribute> AttributeList.asList()",
                  al1.equals(al3) && al1.isEmpty());
        }

        List<ObjectName> namelist1 = new ArrayList<ObjectName>(names1);
        Role role = new Role("rolename", namelist1);
        List<ObjectName> namelist2 =
            checked(role.getRoleValue(), ObjectName.class);
        check("new Role(String,List<ObjectName>).getRoleValue() -> " +
              "List<ObjectName>", namelist1.equals(namelist2));

        RoleList rl1 = new RoleList();
        rl1.add(role);
        RoleList rl2 = new RoleList(Arrays.asList(new Role[] {role}));
        check("new RoleList(List<Role>)", rl1.equals(rl2));
        if (generic) {
            List<Role> rl3 = checked(rl1.asList(), Role.class);
            rl3.remove(role);
            check("List<Role> RoleList.asList()",
                  rl1.equals(rl3) && rl1.isEmpty());
        }

        RoleUnresolved ru =
            new RoleUnresolved("rolename", namelist1,
                               RoleStatus.LESS_THAN_MIN_ROLE_DEGREE);
        List<ObjectName> namelist3 =
            checked(ru.getRoleValue(), ObjectName.class);
        check("new RoleUnresolved(...List<ObjectName>...).getRoleValue() -> " +
              "List<ObjectName>", namelist1.equals(namelist3));

        RoleUnresolvedList rul1 = new RoleUnresolvedList();
        rul1.add(ru);
        RoleUnresolvedList rul2 =
            new RoleUnresolvedList(Arrays.asList(new RoleUnresolved[] {ru}));
        check("new RoleUnresolvedList(List<RoleUnresolved>", rul1.equals(rul2));
        if (generic) {
            List<RoleUnresolved> rul3 =
                checked(rul1.asList(), RoleUnresolved.class);
            rul3.remove(ru);
            check("List<RoleUnresolved> RoleUnresolvedList.asList()",
                  rul1.equals(rul3) && rul1.isEmpty());
        }

        // This case basically just tests that we can compile this sort of thing
        OpenMBeanAttributeInfo ombai1 =
            new OpenMBeanAttributeInfoSupport("a", "a descr",
                                                SimpleType.INTEGER,
                                                true, true, false);
        CompositeType ct =
            new CompositeType("ct", "ct descr", new String[] {"item1"},
                              new String[] {"item1 descr"},
                              new OpenType[] {SimpleType.INTEGER});
        OpenMBeanAttributeInfo ombai2 =
            new OpenMBeanAttributeInfoSupport("a", "a descr",
                                                      ct, true, true, false);
        TabularType tt =
            new TabularType("tt", "tt descr", ct, new String[] {"item1"});
        OpenMBeanAttributeInfo ombai3 =
            new OpenMBeanAttributeInfoSupport("a", "a descr",
                                                    tt, true, true, false);
        ArrayType<String[][]> at =
            new ArrayType<String[][]>(2, SimpleType.STRING);
        OpenMBeanAttributeInfo ombai4 =
            new OpenMBeanAttributeInfoSupport("a", "a descr",
                                                   at, true, true, false);
        OpenMBeanAttributeInfo ombai4a =
            new OpenMBeanAttributeInfoSupport("a", "a descr",
                                              (ArrayType) at,
                                              true, true, false);
        OpenMBeanAttributeInfo ombai5 =
            new OpenMBeanAttributeInfoSupport("a", "a descr",
                                                       SimpleType.INTEGER,
                                                       true, true, false,
                                                       5, 1, 9);
        OpenMBeanAttributeInfo ombai6 =
            new OpenMBeanAttributeInfoSupport("a", "a descr",
                                                       SimpleType.INTEGER,
                                                       true, true, false,
                                                       5, new Integer[] {1, 5});

        OpenMBeanInfo ombi =
            new OpenMBeanInfoSupport("a.a", "a.a descr",
                                     new OpenMBeanAttributeInfo[] {
                                         ombai1, ombai2, ombai3, ombai4,
                                         ombai5, ombai6,
                                     },
                                     null, null, null);

        Map<String,Integer> itemMap =
            checked(singletonMap("item1", 5),
                    String.class, Integer.class);
        CompositeData cd =
            new CompositeDataSupport(ct, itemMap);
        check("CompositeDataSupport(CompositeType, Map<String,?>",
              cd.get("item1").equals(5));

        Set<String> ctkeys = checked(ct.keySet(), String.class);
        check("Set<String> CompositeType.keySet()",
              ctkeys.equals(singleton("item1")));

        List<String> ttindex = checked(tt.getIndexNames(), String.class);
        check("Set<String> TabularType.getIndexNames()",
              ttindex.equals(singletonList("item1")));

        TabularData td = new TabularDataSupport(tt);
        td.putAll(new CompositeData[] {cd});
        List<Integer> tdkey = checked(singletonList(5), Integer.class);
        Set<List<Integer>> tdkeys = checked(singleton(tdkey),
            (Class<List<Integer>>) tdkey.getClass());
        Collection<CompositeData> tdvalues = checked(singleton(cd),
            CompositeData.class);
        check("Set<List<?>> TabularDataSupport.keySet()",
              td.keySet().equals(tdkeys));
        check("Collection<CompositeData> TabularDataSupport.values()",
              td.values().iterator().next().equals(tdvalues.iterator().next()));

        ObjectName stupidName = new ObjectName("stupid:a=b");
        mbs.registerMBean(new Stupid(), stupidName);
        StupidMBean proxy =
            MBeanServerInvocationHandler.newProxyInstance(mbs,
                                                          stupidName,
                                                          StupidMBean.class,
                                                          false);
        check("MBeanServerInvocationHandler.newProxyInstance",
              proxy.getFive() == 5);
        mbs.unregisterMBean(stupidName);

        mbs.registerMBean(new StandardMBean(new Stupid(), StupidMBean.class),
                          stupidName);
        check("<T> StandardMBean(T impl, Class<T> intf)",
              proxy.getFive() == 5);

        // Following is based on the package.html for javax.management.relation
        // Create the Relation Service MBean
        ObjectName relSvcName = new ObjectName(":type=RelationService");
        RelationService relSvcObject = new RelationService(true);
        mbs.registerMBean(relSvcObject, relSvcName);

        // Create an MBean proxy for easier access to the Relation Service
        RelationServiceMBean relSvc =
        MBeanServerInvocationHandler.newProxyInstance(mbs, relSvcName,
                                                      RelationServiceMBean.class,
                                                      false);

        // Define the DependsOn relation type
        RoleInfo[] dependsOnRoles = {
            new RoleInfo("dependent", Module.class.getName()),
            new RoleInfo("dependedOn", Module.class.getName())
        };
        relSvc.createRelationType("DependsOn", dependsOnRoles);

        // Now define a relation instance "moduleA DependsOn moduleB"

        ObjectName moduleA = new ObjectName(":type=Module,name=A");
        ObjectName moduleB = new ObjectName(":type=Module,name=B");

        // Following two lines added to example:
        mbs.registerMBean(new Module(), moduleA);
        mbs.registerMBean(new Module(), moduleB);

        Role dependent = new Role("dependent", singletonList(moduleA));
        Role dependedOn = new Role("dependedOn", singletonList(moduleB));
        Role[] roleArray = {dependent, dependedOn};
        RoleList roles = new RoleList(Arrays.asList(roleArray));
        relSvc.createRelation("A-DependsOn-B", "DependsOn", roles);

        // Query the Relation Service to find what modules moduleA depends on
        Map<ObjectName,List<String>> dependentAMap =
        relSvc.findAssociatedMBeans(moduleA, "DependsOn", "dependent");
        Set<ObjectName> dependentASet = dependentAMap.keySet();
        dependentASet = checked(dependentASet, ObjectName.class);
        // Set of ObjectName containing moduleB
        check("Map<ObjectName,List<String>> RelationService.findAssociatedMBeans",
              dependentAMap.size() == 1 &&
              dependentASet.equals(singleton(moduleB)));

        Map<String,List<String>> refRels =
            relSvc.findReferencingRelations(moduleA, "DependsOn", "dependent");
        List<String> refRoles =
            checked(refRels.get("A-DependsOn-B"), String.class);
        check("Map<String,List<String>> RelationService.findReferencingRelations",
              refRoles.equals(singletonList("dependent")));

        List<String> relsOfType = relSvc.findRelationsOfType("DependsOn");
        relsOfType = checked(relsOfType, String.class);
        check("List<String> RelationService.findRelationsOfType",
              relsOfType.equals(singletonList("A-DependsOn-B")));

        List<String> allRelIds = relSvc.getAllRelationIds();
        allRelIds = checked(allRelIds, String.class);
        check("List<String> RelationService.getAllRelationIds()",
              allRelIds.equals(singletonList("A-DependsOn-B")));

        List<String> allRelTypes = relSvc.getAllRelationTypeNames();
        allRelTypes = checked(allRelTypes, String.class);
        check("List<String> RelationService.getAllRelationTypeNames",
              allRelTypes.equals(singletonList("DependsOn")));

        Map<ObjectName,List<String>> refdMBeans =
            relSvc.getReferencedMBeans("A-DependsOn-B");
        check("Map<ObjectName,List<String>> RelationService.getReferencedMBeans",
              refdMBeans.get(moduleA).equals(singletonList("dependent")) &&
              refdMBeans.get(moduleB).equals(singletonList("dependedOn")));

        List<ObjectName> roleContents =
            checked(relSvc.getRole("A-DependsOn-B", "dependent"),
                    ObjectName.class);
        check("List<ObjectName> RelationService.getRole",
              roleContents.equals(singletonList(moduleA)));

        RoleInfo roleInfoDependent =
            relSvc.getRoleInfo("DependsOn", "dependent");
        RoleInfo roleInfoDependedOn =
            relSvc.getRoleInfo("DependsOn", "dependedOn");
        List<RoleInfo> expectedRoleInfos =
            Arrays.asList(new RoleInfo[] {roleInfoDependent, roleInfoDependedOn});
        List<RoleInfo> roleInfos =
            checked(relSvc.getRoleInfos("DependsOn"), RoleInfo.class);
        check("List<RoleInfo> RelationService.getRoleInfos",
              equalListContents(expectedRoleInfos, roleInfos));

        RelationType relType =
            new RelationTypeSupport("DependsOn", dependsOnRoles);
        List<RoleInfo> relTypeRoleInfos =
            checked(relType.getRoleInfos(), RoleInfo.class);
        // Since there's no RoleInfo.equals and since the RelationTypeSupport
        // constructor clones the RoleInfos passed to it, it's tricky to
        // test equality here so we check type and size and have done with it
        check("List<RoleInfo> RelationType.getRoleInfos",
              relTypeRoleInfos.size() == 2);

        MBeanServerNotificationFilter mbsnf =
            new MBeanServerNotificationFilter();
        mbsnf.enableObjectName(moduleA);
        check("Vector<ObjectName> MBeanServerNotificationFilter." +
              "getEnabledObjectNames",
              mbsnf.getEnabledObjectNames().equals(Arrays.asList(moduleA)));
        mbsnf.enableAllObjectNames();
        mbsnf.disableObjectName(moduleB);
        check("Vector<ObjectName> MBeanServerNotificationFilter." +
              "getDisabledObjectNames",
              mbsnf.getDisabledObjectNames().equals(Arrays.asList(moduleB)));

        RelationService unusedRelSvc = new RelationService(false);
        RelationNotification rn1 =
            new RelationNotification(RelationNotification.RELATION_MBEAN_REMOVAL,
                                     unusedRelSvc, 0L, 0L, "yo!",
                                     "A-DependsOn-B", "DependsOn", null,
                                     singletonList(moduleA));
        List<ObjectName> toUnreg =
            checked(rn1.getMBeansToUnregister(), ObjectName.class);
        check("List<ObjectName> RelationNotification.getMBeansToUnregister",
              toUnreg.equals(singletonList(moduleA)));

        RelationNotification rn2 =
            new RelationNotification(RelationNotification.RELATION_MBEAN_UPDATE,
                                     unusedRelSvc, 0L, 0L, "yo!",
                                     "A-DependsOn-B", "DependsOn", null,
                                     "dependent", singletonList(moduleA),
                                     singletonList(moduleB));
        check("List<ObjectName> RelationNotification.getOldRoleValue",
              checked(rn2.getOldRoleValue(), ObjectName.class)
              .equals(singletonList(moduleB)));
        check("List<ObjectName> RelationNotification.getNewRoleValue",
              checked(rn2.getNewRoleValue(), ObjectName.class)
              .equals(singletonList(moduleA)));

        ObjectName timerName = new ObjectName(":type=timer");
        mbs.registerMBean(new Timer(), timerName);
        TimerMBean timer =
            MBeanServerInvocationHandler.newProxyInstance(mbs,
                                                          timerName,
                                                          TimerMBean.class,
                                                          false);
        Date doomsday = new Date(Long.MAX_VALUE);
        int timer1 = timer.addNotification("one", "one", null, doomsday);
        int timer2 = timer.addNotification("two", "two", null, doomsday);
        Vector<Integer> idsOne = timer.getNotificationIDs("one");
        check("Vector<Integer> TimerMBean.getNotificationIDs",
              idsOne.equals(singletonList(timer1)));
        Vector<Integer> allIds = timer.getAllNotificationIDs();
        check("Vector<Integer> TimerMBean.getAllNotificationIDs",
              equalListContents(allIds,
                                Arrays.asList(new Integer[]{timer1, timer2})));

        // ADD NEW TEST CASES ABOVE THIS COMMENT

        if (failures == 0)
            System.out.println("All tests passed");
        else {
            System.out.println("TEST FAILURES: " + failures);
            System.exit(1);
        }

        // DO NOT ADD NEW TEST CASES HERE, ADD THEM ABOVE THE PREVIOUS COMMENT
    }

    public static interface StupidMBean {
        public int getFive();
    }

    public static class Stupid implements StupidMBean {
        public int getFive() {
            return 5;
        }
    }

    public static class Module extends StandardMBean implements StupidMBean {
        public Module() throws NotCompliantMBeanException {
            super(StupidMBean.class);
        }

        public int getFive() {
            return 5;
        }
    }

    private static <E> List<E> singletonList(E value) {
        return Collections.singletonList(value);
    }

    private static <E> Set<E> singleton(E value) {
        return Collections.singleton(value);
    }

    private static <K,V> Map<K,V> singletonMap(K key, V value) {
        return Collections.singletonMap(key, value);
    }

    private static <E> List<E> checked(List<E> c, Class<E> type) {
        List<E> unchecked = new ArrayList<E>();
        List<E> checked = Collections.checkedList(unchecked, type);
        checked.addAll(c);
        return Collections.checkedList(c, type);
    }

    private static <E> Set<E> checked(Set<E> c, Class<E> type) {
        Set<E> unchecked = new HashSet<E>();
        Set<E> checked = Collections.checkedSet(unchecked, type);
        checked.addAll(c);
        return Collections.checkedSet(c, type);
    }

    private static <K,V> Map<K,V> checked(Map<K,V> m,
                                          Class<K> keyType,
                                          Class<V> valueType) {
        Map<K,V> unchecked = new HashMap<K,V>();
        Map<K,V> checked = Collections.checkedMap(unchecked, keyType, valueType);
        checked.putAll(m);
        return Collections.checkedMap(m, keyType, valueType);
    }

    /* The fact that we have to call this method is a clear signal that
     * the API says List where it means Set.
     */
    private static <E> boolean equalListContents(List<E> l1, List<E> l2) {
        return new HashSet<E>(l1).equals(new HashSet<E>(l2));
    }

    private static void check(String what, boolean cond) {
        if (cond)
            System.out.println("OK: " + what);
        else {
            System.out.println("FAILED: " + what);
            failures++;
        }
    }

    private static boolean sameSize(Set ... sets) {
        return Stream.of(sets).map(s -> s.size()).distinct().count() == 1;
    }
}
