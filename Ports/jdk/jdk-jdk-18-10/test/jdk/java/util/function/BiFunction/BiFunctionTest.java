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
 * @bug 8024500
 * @run testng BiFunctionTest
 */

import java.util.function.BiFunction;
import java.util.function.Function;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test(groups = "unit")
public class BiFunctionTest {
    static class Quote {
        double unit_price;

        Quote(double price) {
            unit_price = price;
        }
    };

    static class Order {
        int quantity;

        Order(int quantity) {
            this.quantity = quantity;
        }
    };

    BiFunction<Quote, Order, Double> estimate = (quote, order) -> {
        if (quote.unit_price < 0) {
            throw new IllegalArgumentException("quote");
        }

        if (order.quantity < 0) {
            throw new IllegalArgumentException("order");
        }

        return quote.unit_price * order.quantity;
    };

    Function<Double, Long> creditcheck = total -> {
        if (total > 100.00) {
            throw new RuntimeException("overlimit");
        }
        return total.longValue();
    };

    public void testAndThen() {
        try {
            BiFunction<Quote, Order, Long> checkout = estimate.andThen(null);
            fail("Null argument should throw NPE");
        } catch (NullPointerException npe) {
            // ignore
        }

        BiFunction<Quote, Order, Long> checkout = estimate.andThen(creditcheck);
        try {
            checkout.apply(new Quote(20.0), new Order(-1));
            fail("First function delivers exception");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "order");
        }

        try {
            checkout.apply(new Quote(20.0), new Order(10));
            fail("Second function delivers exception");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "overlimit");
        }

        assertEquals(49, checkout.apply(new Quote(24.99), new Order(2)).longValue());
        assertEquals(50, checkout.apply(new Quote(25), new Order(2)).longValue());
    }
}
