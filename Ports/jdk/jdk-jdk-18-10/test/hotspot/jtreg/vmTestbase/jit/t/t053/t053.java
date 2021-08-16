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
 * @summary converted from VM Testbase jit/t/t053.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t053.t053
 */

package jit.t.t053;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

//
// Tomcatv in java, with prints active
//

public class t053 {
    public static final GoldChecker goldChecker = new GoldChecker( "t053" );

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
        N = 25;
        s1alt = 0.0;
        alfa = 0.1;
        relfa = 0.98;
        lmax = 100;
        eps = 0.5e-7;
        h = 1.0/(N-1);
        rel = 2.0/relfa;


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

   for(j=1;j<(N-1);j++) {
       for(i=1;i<(N-1);i++) {
           x[i][j] = 0.9*x[i][0];
           y[i][j] = 0.9*((1.0 - x[i][0])*y[0][j]+x[i][0]*y[N-1][j]);
           t053.goldChecker.println(i + " " + j + " " + x[i][j] + " " + y[i][j]);
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
           aa[i][m] = -b;
           dd[i][m] = b+b+a*rel;
           pxx = x[ip][j]-2.0*x[i][j]+x[im][j];
           qxx = y[ip][j]-2.0*y[i][j]+y[im][j];

           pyy = x[i][jp]-2.0*x[i][j]+x[i][jm];
           qyy = y[i][jp]-2.0*y[i][j]+y[i][jm];

           pxy = x[ip][jp]-x[ip][jm]-x[im][jp]+x[im][jm];
           qxy = y[ip][jp]-y[ip][jm]-y[im][jp]+y[im][jm];

           rx[i][m] = a*pxx+b*pyy-c*pxy+xx*qi+xy*qj;
           ry[i][m] = a*qxx+b*qyy-c*qxy+yx*qi+yy*qj;
       }
   }

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

   t053.goldChecker.println(ll+"  "+ixcm+"  "+jxcm+"  "+dxcm+"  "+iycm+"  "+jycm+"  "+dycm+"  "+irxm+"  "+jrxm+"  "+rxm+"  "+irym+"  "+jrym+"  "+rym);

   } while ((ll < lmax) && (dmax > eps));


   t053.goldChecker.check();
 }
}
