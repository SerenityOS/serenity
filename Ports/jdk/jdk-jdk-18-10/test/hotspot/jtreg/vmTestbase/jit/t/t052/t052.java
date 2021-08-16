/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/t/t052.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t052.t052
 */

package jit.t.t052;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// This is a whittle-down of a tomcatv failure.  At the time it found
// its way in among the tests, it failed because of a failure correctly
// to reverse the sense of an integer branch when swapping the operands.

public class t052 {
    public static final GoldChecker goldChecker = new GoldChecker( "t052" );

    static double aa[][],dd[][],x[][],y[][],
           rx[][],ry[][],d[][];

    static double s1alt,alfa,relfa,eps,h,rel;
    static int lmax;
    static int N;
    static int i1p,j1p,i2m,j2m,ll,ixcm,jxcm,iycm,jycm,irxm,jrxm;
    static int irym,jrym,m,ip,im,jp,jm;
    static double dxcm,dycm,rxm,rym,xx,yx,xy,yy,a,b,c,qi,qj;
    static double pxx,pyy,qxx,qyy,pxy,qxy;
 public static void main (String args[]) {
   int i,j,l,k;
   double abx,aby,dmax,r;
        N = 3;
        s1alt = 0.0;
        alfa = 0.1;
        relfa = 0.98;
        lmax = 1;
        eps = 0.5e-7;
        h = 1.0/(N-1);
        rel = 2.0/relfa;

t052.goldChecker.println();
t052.goldChecker.println("init");
t052.goldChecker.println(N);
t052.goldChecker.println(s1alt);
t052.goldChecker.println(alfa);
t052.goldChecker.println(relfa);
t052.goldChecker.println(lmax);
t052.goldChecker.println(eps);
t052.goldChecker.println(h);
t052.goldChecker.println(rel);

        aa = new double [N][N];
        dd = new double [N][N];
         x = new double [N][N];
         y = new double [N][N];
        rx = new double [N][N];
        ry = new double [N][N];
         d = new double [N][N];

   for(i=0;i<N;i++) {
       x[i][0] = (i+1-1.0)/(N-1.0);
   }

   for(i=0;i<N;i++) {
       x[i][N-1] = x[i][0];
       x[0][i] = 0.0;
       x[N-1][i] = 1.0;
   }

   for(i=0;i<N;i++) {
       y[i][0] = 0.0;
       y[0][i] = x[i][0];
       y[N-1][i] = alfa*x[i][0];
   }

   for(i=0;i<N;i++) {
       y[i][N-1] = (1.0 - x[i][0])*y[0][N-1] + x[i][0]*y[N-1][N-1];
   }

t052.goldChecker.println();
t052.goldChecker.println("at init loop");
t052.goldChecker.println("N: " + N);
   for(j=1;j<(N-1);j++) {
t052.goldChecker.println("inside outer");
       for(i=1;i<(N-1);i++) {
t052.goldChecker.println("inside inner");
           x[i][j] = 0.9*x[i][0];
t052.goldChecker.println(i);
t052.goldChecker.println(j);
t052.goldChecker.println(x[i][j]);
           y[i][j] = 0.9*((1.0 - x[i][0])*y[0][j]+x[i][0]*y[N-1][j]);
           //System.out.println(i + " " + j + " " + x[i][j] + " " + y[i][j]);
       }
   }


                                //  the values are dec on purpose
   i1p = 1;
   j1p = 1;
   i2m = N-1;
   j2m = N-1;
   ll = 0;
   do {

   ixcm = jxcm = iycm = jycm = irxm = jrxm = irym = jrym = m = -1;
   dxcm = dycm = rxm = rym = 0.0;


t052.goldChecker.println();
t052.goldChecker.println("entering loop");
t052.goldChecker.println("x[1][1]: " + x[1][1]);

   for(j=j1p ; j < j2m ; j++) {

       jp = j+1;
       jm = j-1;
       m = m+1;
       for(i=i1p;i<i2m;i++) {
           ip = i+1;
           im = i-1;
           xx = x[ip][j]-x[im][j];
           yx = y[ip][j]-y[im][j];
           xy = x[i][jp]-x[i][jm];
           yy = y[i][jp]-y[i][jm];
           a = 0.25*(xy*xy+yy*yy);
           b = 0.25*(xx*xx+yx*yx);
           c = 0.125*(xx*xy+yx*yy);
           qi = 0.0;
           qj = 0.0;

t052.goldChecker.println();
t052.goldChecker.println("first blast in loop");
t052.goldChecker.println(ip);
t052.goldChecker.println(im);
t052.goldChecker.println(xx);
t052.goldChecker.println(yx);
t052.goldChecker.println(xy);
t052.goldChecker.println(yy);
t052.goldChecker.println(a);
t052.goldChecker.println(b);
t052.goldChecker.println(c);
t052.goldChecker.println(qi);
t052.goldChecker.println(qj);

           aa[i][m] = -b;
           dd[i][m] = b+b+a*rel;

t052.goldChecker.println();
t052.goldChecker.println("opnds of pxx");
t052.goldChecker.println(x[ip][j]);
t052.goldChecker.println(x[i][j]);
t052.goldChecker.println(x[im][j]);

           pxx = x[ip][j]-2.0*x[i][j]+x[im][j];
           qxx = y[ip][j]-2.0*y[i][j]+y[im][j];

           pyy = x[i][jp]-2.0*x[i][j]+x[i][jm];
           qyy = y[i][jp]-2.0*y[i][j]+y[i][jm];

           pxy = x[ip][jp]-x[ip][jm]-x[im][jp]+x[im][jm];
           qxy = y[ip][jp]-y[ip][jm]-y[im][jp]+y[im][jm];

t052.goldChecker.println();
t052.goldChecker.println("second blast in loop");
t052.goldChecker.println(aa[i][m]);
t052.goldChecker.println(dd[i][m]);
t052.goldChecker.println(pxx);
t052.goldChecker.println(qxx);
t052.goldChecker.println(pyy);
t052.goldChecker.println(qyy);
t052.goldChecker.println(pxy);
t052.goldChecker.println(qxy);

           rx[i][m] = a*pxx+b*pyy-c*pxy+xx*qi+xy*qj;
           ry[i][m] = a*qxx+b*qyy-c*qxy+yx*qi+yy*qj;
       }
   }

t052.goldChecker.println();
t052.goldChecker.println("first elts of rx[], ry[]");
t052.goldChecker.println("rx[1][0]: " + rx[1][0]);
t052.goldChecker.println("ry[1][0]: " + ry[1][0]);

   for (j=0; j<m; j++) {
      for (i=i1p; i<i2m; i++) {
        if (Math.abs(rx[i][j]) >= Math.abs(rxm)) {
           rxm = rx[i][j];
           irxm = i;
           jrxm = j;
        }
        if (Math.abs(ry[i][j]) >= Math.abs(rym)) {
           rym = ry[i][j];
           irym = i;
           jrym = j;
        }
      }
   }

t052.goldChecker.println();
t052.goldChecker.println("Rxm, rym");
t052.goldChecker.println(rxm);
t052.goldChecker.println(rym);

   for (i=i1p; i<i2m; i++) {
      d[i][0] = 1.0/dd[i][0];
   }

   for (j=1; j<=m; j++) {
      for (i=i1p; i<i2m; i++) {
         r = aa[i][j]*d[i][j-1];
         d[i][j] = 1.0/(dd[i][j]-aa[i][j-1]*r);
         rx[i][j] = rx[i][j] - rx[i][j-1]*r;
         ry[i][j] = ry[i][j] - ry[i][j-1]*r;
      }
   }

   for (i=i1p; i<i2m; i++) {
      rx[i][m] = rx[i][m]*d[i][m];
      ry[i][m] = ry[i][m]*d[i][m];
   }

   for (j=1; j<=m; j++) {
      k = m-j;
      for (i=i1p; i<i2m; i++) {
         rx[i][k] = (rx[i][k]-aa[i][k]*rx[i][k+1])*d[i][k];
         ry[i][k] = (ry[i][k]-aa[i][k]*ry[i][k+1])*d[i][k];
      }
   }

   l = -1;
   for (j=j1p; j<j2m; j++) {
      l = l+1;
      for (i=i1p; i<i2m; i++) {
         x[i][j] = x[i][j]+rx[i][l];
         y[i][j] = y[i][j]+ry[i][l];
      }
   }

   ll = ll+1;
   abx = Math.abs(rxm);
   aby = Math.abs(rym);
   dmax = (abx > aby) ? abx : aby;

//   System.out.println(ll+"  "+ixcm+"  "+jxcm+"  "+dxcm+"  "+iycm+"  "+jycm+"  "+dycm+"  "+irxm+"  "+jrxm+"  "+rxm+"  "+irym+"  "+jrym+"  "+rym);

   t052.goldChecker.println();
   t052.goldChecker.println(ll);
   t052.goldChecker.println(lmax);
   t052.goldChecker.println(dmax);
   t052.goldChecker.println(eps);
   } while ((ll < lmax) && (dmax > eps));


   t052.goldChecker.check();
 }
}
