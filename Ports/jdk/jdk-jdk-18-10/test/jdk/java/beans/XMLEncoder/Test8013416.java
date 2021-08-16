/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013416
 * @summary Tests public synthetic methods
 * @run main/othervm -Djava.security.manager=allow Test8013416
 * @author Sergey Malenkov
 */

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.Statement;
import java.beans.XMLEncoder;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class Test8013416 extends AbstractTest {
    public static void main(String[] args) {
        new Test8013416().test(true);
    }

    protected Object getObject() {
        Public<String, String> map = new Public<String, String>();
        map.put(" pz1 ", " pz2 ");
        map.put(" pz3 ", " pz4 ");
        return map;
    }

    @Override
    protected void initialize(XMLEncoder encoder) {
        super.initialize(encoder);
        encoder.setPersistenceDelegate(Public.class, new PublicPersistenceDelegate());
    }

    private static final class PublicPersistenceDelegate extends DefaultPersistenceDelegate {
        @Override
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance, oldInstance.getClass(), "new", null);
        }

        @Override
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            super.initialize(type, oldInstance, newInstance, out);

            Public<String, String> map = (Public) oldInstance;
            for (Entry<String, String> entry : map.getAll()) {
                String[] args = {entry.getKey(), entry.getValue()};
                out.writeStatement(new Statement(oldInstance, "put", args));
            }
        }
    }

    public static final class Public<K, V> extends Private<K, V> {
    }

    private static class Private<K, V> {
        private HashMap<K, V> map = new HashMap<K, V>();

        public void put(K key, V value) {
            this.map.put(key, value);
        }

        public Set<Entry<K, V>> getAll() {
            return this.map.entrySet();
        }
    }
}
