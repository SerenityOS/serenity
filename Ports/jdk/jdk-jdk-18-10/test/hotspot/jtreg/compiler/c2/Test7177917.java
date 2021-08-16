/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 7177917
 * @summary Micro-benchmark for Math.pow() and Math.exp()
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @run main compiler.c2.Test7177917
 */

package compiler.c2;

import jdk.test.lib.Utils;

import java.util.Random;

public class Test7177917 {

  static double d;

  static final Random R = Utils.getRandomInstance();

  static long  m_pow(double[][] values) {
    double res = 0;
    long start = System.nanoTime();
    for (int i = 0; i < values.length; i++) {
      res += Math.pow(values[i][0], values[i][1]);
    }
    long stop = System.nanoTime();
    d = res;
    return (stop - start) / 1000;
  }

  static long  m_exp(double[] values) {
    double res = 0;
    long start = System.nanoTime();
    for (int i = 0; i < values.length; i++) {
      res += Math.exp(values[i]);
    }
    long stop = System.nanoTime();
    d = res;
    return (stop - start) / 1000;
  }

  static double[][] pow_values(int nb) {
    double[][] res = new double[nb][2];
    for (int i = 0; i < nb; i++) {
      double ylogx = (1 + (R.nextDouble() * 2045)) - 1023; // 2045 rather than 2046 as a safety margin
      double x = Math.abs(Double.longBitsToDouble(R.nextLong()));
      while (x != x) {
        x = Math.abs(Double.longBitsToDouble(R.nextLong()));
      }
      double logx = Math.log(x) / Math.log(2);
      double y = ylogx / logx;

      res[i][0] = x;
      res[i][1] = y;
    }
    return res;
  }

  static double[] exp_values(int nb) {
    double[] res = new double[nb];
    for (int i = 0; i < nb; i++) {
      double ylogx = (1 + (R.nextDouble() * 2045)) - 1023; // 2045 rather than 2046 as a safety margin
      double x = Math.E;
      double logx = Math.log(x) / Math.log(2);
      double y = ylogx / logx;
      res[i] = y;
    }
    return res;
  }

  static public void main(String[] args) {
    {
      // warmup
      double[][] warmup_values = pow_values(10);
      m_pow(warmup_values);

      for (int i = 0; i < 20000; i++) {
        m_pow(warmup_values);
      }
      // test pow perf
      double[][] values = pow_values(1000000);
      System.out.println("==> POW " + m_pow(values));

      // force uncommon trap
      double[][] nan_values = new double[1][2];
      nan_values[0][0] = Double.NaN;
      nan_values[0][1] = Double.NaN;
      m_pow(nan_values);

      // force recompilation
      for (int i = 0; i < 20000; i++) {
        m_pow(warmup_values);
      }

      // test pow perf again
      System.out.println("==> POW " + m_pow(values));
    }
    {
      // warmup
      double[] warmup_values = exp_values(10);
      m_exp(warmup_values);

      for (int i = 0; i < 20000; i++) {
        m_exp(warmup_values);
      }

      // test pow perf
      double[] values = exp_values(1000000);
      System.out.println("==> EXP " + m_exp(values));

      // force uncommon trap
      double[] nan_values = new double[1];
      nan_values[0] = Double.NaN;
      m_exp(nan_values);

      // force recompilation
      for (int i = 0; i < 20000; i++) {
        m_exp(warmup_values);
      }

      // test pow perf again
      System.out.println("==> EXP " + m_exp(values));
    }
  }
}
