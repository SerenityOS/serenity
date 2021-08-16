/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4328406
 * @summary Tests NPE while removing a BeanContextServices
 * @author Mark Davidson
 */

import java.beans.beancontext.BeanContextChild;
import java.beans.beancontext.BeanContextChildSupport;
import java.beans.beancontext.BeanContextServiceProvider;
import java.beans.beancontext.BeanContextServices;
import java.beans.beancontext.BeanContextServicesSupport;
import java.util.Iterator;

public class Test4328406 {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            BeanContextServices container = new BeanContextServicesSupport();
            BeanContextChild ms1 = new MyService1();
            BeanContextServices ms2 = new MyService2();
            BeanContextChild mb = new MyBean();

            container.add(ms1);
            container.add(ms2);
            ms2.add(mb);

            // exception thrown here
            container.remove(ms2);
        }
    }
}

class MyService1 extends BeanContextChildSupport implements BeanContextServiceProvider {
    protected void initializeBeanContextResources() {
        super.initializeBeanContextResources();

        BeanContextServices bcs = (BeanContextServices) getBeanContext();
        bcs.addService(this.getClass(), this);
    }

    public Object getService(BeanContextServices bcs, Object requestor, Class serviceClass, Object serviceSelector) {
        return this;
    }

    public void releaseService(BeanContextServices bcs, Object requestor, Object
            service) {
    }

    public Iterator getCurrentServiceSelectors(BeanContextServices bcs, Class serviceClass) {
        return null;
    }
}

class MyService2 extends BeanContextServicesSupport implements BeanContextServiceProvider {
    protected void initializeBeanContextResources() {
        super.initializeBeanContextResources();

        BeanContextServicesSupport bcs = (BeanContextServicesSupport) getBeanContext();
        try {
            bcs.getService(this, this, MyService1.class, null, this);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        bcs.addService(this.getClass(), this);
    }

    protected void releaseBeanContextResources() {
        super.releaseBeanContextResources();

        BeanContextServices bcs = (BeanContextServices) getBeanContext();
        bcs.revokeService(this.getClass(), this, true);
    }

    public Object getService(BeanContextServices bcs, Object requestor, Class serviceClass, Object serviceSelector) {
        return this;
    }

    public void releaseService(BeanContextServices bcs, Object requestor, Object service) {
    }

    public Iterator getCurrentServiceSelectors(BeanContextServices bcs, Class serviceClass) {
        return null;
    }
}

class MyBean extends BeanContextChildSupport {
    protected void initializeBeanContextResources() {
        super.initializeBeanContextResources();

        BeanContextServices bcs = (BeanContextServices) getBeanContext();
        try {
            bcs.getService(this, this, MyService1.class, null, this);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            bcs.getService(this, this, MyService2.class, null, this);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
