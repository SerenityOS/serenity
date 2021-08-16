/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     8022640
 * @summary Test verifies whether ServiceProvider API's of
 *          ServiceRegistry are safe for concurrent access.
 * @run     main ServiceRegistrySyncTest
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;

public class ServiceRegistrySyncTest {
    public static void main(String[] args) throws InterruptedException {

        final ArrayList<Class<?>> services = new ArrayList<Class<?>>();
        services.add(ImageInputStreamSpi.class);

        final ServiceRegistry reg = new ServiceRegistry(services.iterator());

        //create new thread for Registerer and Consumer Class
        Thread registerer = new Thread(new Registerer(reg));
        Thread consumer = new Thread(new Consumer(reg));

        //run both registerer and consumer thread parallely
        registerer.start();
        consumer.start();
    }

    static class Consumer implements Runnable {
        private final ServiceRegistry reg;
        boolean go = true;
        int duration;
        long start, end;

        public Consumer(ServiceRegistry r) {
            reg = r;
            //set 5000ms duration to run the test
            duration = 5000;
        }

        @Override
        public void run() {
            start = System.currentTimeMillis();
            end = start + duration;
            while (start < end) {
                //access the ServiceProvider API
                reg.getServiceProviders(ImageInputStreamSpi.class, true);
                start = System.currentTimeMillis();
            }
        }
    }

    static class Registerer implements Runnable {
        private final ServiceRegistry reg;
        boolean go = true;
        int duration;
        long start, end;

        public Registerer(ServiceRegistry r) {
            reg = r;
            //set 5000ms duration to run the test
            duration = 5000;
        }

        @Override
        public void run() {
            final int N = 20;

            MyService[] services = new MyService[N];
            for (int i = 0; i < N; i++) {
                services[i] = new MyService();
            }
            start = System.currentTimeMillis();
            end = start + duration;
            while (start < end) {
                //access the ServiceProvider API's
                for (int i = 0; i < N; i++) {
                    reg.registerServiceProvider(services[i]);
                }
                for (int i = 0; i < N; i++) {
                    reg.deregisterServiceProvider(services[i]);
                }
                start = System.currentTimeMillis();
            }
        }
    }
}

class MyService extends ImageInputStreamSpi {
    public MyService() {
    }

    @Override
    public String getDescription(Locale locale) {
        return null;
    }

    @Override
    public ImageInputStream createInputStreamInstance
        (Object input, boolean useCache, File cacheDir) throws IOException {
        return null;
    }
}
