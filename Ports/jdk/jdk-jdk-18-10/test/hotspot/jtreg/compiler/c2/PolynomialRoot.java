/*
 * (C) Vladislav Malyshkin 2010
 * This file is under GPL version 3.
 *
 */

/** Polynomial root.
 *  @version $Id: PolynomialRoot.java,v 1.105 2012/08/18 00:00:05 mal Exp $
 *  @author Vladislav Malyshkin mal@gromco.com
 */

/**
 * @test
 * @key randomness
 * @bug 8005956
 * @summary C2: assert(!def_outside->member(r)) failed: Use of external LRG overlaps the same LRG defined in this block
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main/timeout=300 compiler.c2.PolynomialRoot
 */

package compiler.c2;

import jdk.test.lib.Utils;

import java.util.Arrays;
import java.util.Random;

public class PolynomialRoot  {


public static int findPolynomialRoots(final int n,
              final double [] p,
              final double [] re_root,
              final double [] im_root)
{
    if(n==4)
    {
  return root4(p,re_root,im_root);
    }
    else if(n==3)
    {
  return root3(p,re_root,im_root);
    }
    else if(n==2)
    {
  return root2(p,re_root,im_root);
    }
    else if(n==1)
    {
  return root1(p,re_root,im_root);
    }
    else
    {
  throw new RuntimeException("n="+n+" is not supported yet");
    }
}



static final double SQRT3=Math.sqrt(3.0),SQRT2=Math.sqrt(2.0);


private static final boolean PRINT_DEBUG=false;

public static int root4(final double [] p,final double [] re_root,final double [] im_root)
{
  if (PRINT_DEBUG) { System.err.println("=====================root4:p=" + Arrays.toString(p)); }
  final double vs=p[4];
  if(PRINT_DEBUG) System.err.println("p[4]="+p[4]);
  if(!(Math.abs(vs)>EPS))
  {
      re_root[0]=re_root[1]=re_root[2]=re_root[3]=
    im_root[0]=im_root[1]=im_root[2]=im_root[3]=Double.NaN;
      return -1;
  }

/* zsolve_quartic.c - finds the complex roots of
 *  x^4 + a x^3 + b x^2 + c x + d = 0
 */
  final double a=p[3]/vs,b=p[2]/vs,c=p[1]/vs,d=p[0]/vs;
  if(PRINT_DEBUG) System.err.println("input a="+a+" b="+b+" c="+c+" d="+d);


  final double r4 = 1.0 / 4.0;
  final double q2 = 1.0 / 2.0, q4 = 1.0 / 4.0, q8 = 1.0 / 8.0;
  final double q1 = 3.0 / 8.0, q3 = 3.0 / 16.0;
  final int mt;

  /* Deal easily with the cases where the quartic is degenerate. The
   * ordering of solutions is done explicitly. */
  if (0 == b && 0 == c)
  {
      if (0 == d)
      {
    re_root[0]=-a;
    im_root[0]=im_root[1]=im_root[2]=im_root[3]=0;
    re_root[1]=re_root[2]=re_root[3]=0;
    return 4;
      }
      else if (0 == a)
      {
    if (d > 0)
    {
        final double sq4 = Math.sqrt(Math.sqrt(d));
        re_root[0]=sq4*SQRT2/2;
        im_root[0]=re_root[0];
        re_root[1]=-re_root[0];
        im_root[1]=re_root[0];
        re_root[2]=-re_root[0];
        im_root[2]=-re_root[0];
        re_root[3]=re_root[0];
        im_root[3]=-re_root[0];
        if(PRINT_DEBUG) System.err.println("Path a=0 d>0");
    }
    else
    {
        final double sq4 = Math.sqrt(Math.sqrt(-d));
        re_root[0]=sq4;
        im_root[0]=0;
        re_root[1]=0;
        im_root[1]=sq4;
        re_root[2]=0;
        im_root[2]=-sq4;
        re_root[3]=-sq4;
        im_root[3]=0;
        if(PRINT_DEBUG) System.err.println("Path a=0 d<0");
    }
    return 4;
      }
  }

  if (0.0 == c && 0.0 == d)
  {
      root2(new double []{p[2],p[3],p[4]},re_root,im_root);
      re_root[2]=im_root[2]=re_root[3]=im_root[3]=0;
      return 4;
  }

  if(PRINT_DEBUG) System.err.println("G Path c="+c+" d="+d);
  final double [] u=new double[3];

  if(PRINT_DEBUG) System.err.println("Generic Path");
  /* For non-degenerate solutions, proceed by constructing and
   * solving the resolvent cubic */
  final double aa = a * a;
  final double pp = b - q1 * aa;
  final double qq = c - q2 * a * (b - q4 * aa);
  final double rr = d - q4 * a * (c - q4 * a * (b - q3 * aa));
  final double rc = q2 * pp , rc3 = rc / 3;
  final double sc = q4 * (q4 * pp * pp - rr);
  final double tc = -(q8 * qq * q8 * qq);
  if(PRINT_DEBUG) System.err.println("aa="+aa+" pp="+pp+" qq="+qq+" rr="+rr+" rc="+rc+" sc="+sc+" tc="+tc);
  final boolean flag_realroots;

  /* This code solves the resolvent cubic in a convenient fashion
   * for this implementation of the quartic. If there are three real
   * roots, then they are placed directly into u[].  If two are
   * complex, then the real root is put into u[0] and the real
   * and imaginary part of the complex roots are placed into
   * u[1] and u[2], respectively. */
  {
      final double qcub = (rc * rc - 3 * sc);
      final double rcub = (rc*(2 * rc * rc - 9 * sc) + 27 * tc);

      final double Q = qcub / 9;
      final double R = rcub / 54;

      final double Q3 = Q * Q * Q;
      final double R2 = R * R;

      final double CR2 = 729 * rcub * rcub;
      final double CQ3 = 2916 * qcub * qcub * qcub;

      if(PRINT_DEBUG) System.err.println("CR2="+CR2+" CQ3="+CQ3+" R="+R+" Q="+Q);

      if (0 == R && 0 == Q)
      {
    flag_realroots=true;
    u[0] = -rc3;
    u[1] = -rc3;
    u[2] = -rc3;
      }
      else if (CR2 == CQ3)
      {
    flag_realroots=true;
    final double sqrtQ = Math.sqrt (Q);
    if (R > 0)
    {
        u[0] = -2 * sqrtQ - rc3;
        u[1] = sqrtQ - rc3;
        u[2] = sqrtQ - rc3;
    }
    else
    {
        u[0] = -sqrtQ - rc3;
        u[1] = -sqrtQ - rc3;
        u[2] = 2 * sqrtQ - rc3;
    }
      }
      else if (R2 < Q3)
      {
    flag_realroots=true;
    final double ratio = (R >= 0?1:-1) * Math.sqrt (R2 / Q3);
    final double theta = Math.acos (ratio);
    final double norm = -2 * Math.sqrt (Q);

    u[0] = norm * Math.cos (theta / 3) - rc3;
    u[1] = norm * Math.cos ((theta + 2.0 * Math.PI) / 3) - rc3;
    u[2] = norm * Math.cos ((theta - 2.0 * Math.PI) / 3) - rc3;
      }
      else
      {
    flag_realroots=false;
    final double A = -(R >= 0?1:-1)*Math.pow(Math.abs(R)+Math.sqrt(R2-Q3),1.0/3.0);
    final double B = Q / A;

    u[0] = A + B - rc3;
    u[1] = -0.5 * (A + B) - rc3;
    u[2] = -(SQRT3*0.5) * Math.abs (A - B);
      }
      if(PRINT_DEBUG) System.err.println("u[0]="+u[0]+" u[1]="+u[1]+" u[2]="+u[2]+" qq="+qq+" disc="+((CR2 - CQ3) / 2125764.0));
  }
  /* End of solution to resolvent cubic */

  /* Combine the square roots of the roots of the cubic
   * resolvent appropriately. Also, calculate 'mt' which
   * designates the nature of the roots:
   * mt=1 : 4 real roots
   * mt=2 : 0 real roots
   * mt=3 : 2 real roots
   */


  final double w1_re,w1_im,w2_re,w2_im,w3_re,w3_im,mod_w1w2,mod_w1w2_squared;
  if (flag_realroots)
  {
      mod_w1w2=-1;
      mt = 2;
      int jmin=0;
      double vmin=Math.abs(u[jmin]);
      for(int j=1;j<3;j++)
      {
    final double vx=Math.abs(u[j]);
    if(vx<vmin)
    {
        vmin=vx;
        jmin=j;
    }
      }
      final double u1=u[(jmin+1)%3],u2=u[(jmin+2)%3];
      mod_w1w2_squared=Math.abs(u1*u2);
      if(u1>=0)
      {
    w1_re=Math.sqrt(u1);
    w1_im=0;
      }
      else
      {
    w1_re=0;
    w1_im=Math.sqrt(-u1);
      }
      if(u2>=0)
      {
    w2_re=Math.sqrt(u2);
    w2_im=0;
      }
      else
      {
    w2_re=0;
    w2_im=Math.sqrt(-u2);
      }
      if(PRINT_DEBUG) System.err.println("u1="+u1+" u2="+u2+" jmin="+jmin);
  }
  else
  {
      mt = 3;
      final double w_mod2_sq=u[1]*u[1]+u[2]*u[2],w_mod2=Math.sqrt(w_mod2_sq),w_mod=Math.sqrt(w_mod2);
      if(w_mod2_sq<=0)
      {
    w1_re=w1_im=0;
      }
      else
      {
    // calculate square root of a complex number (u[1],u[2])
    // the result is in the (w1_re,w1_im)
    final double absu1=Math.abs(u[1]),absu2=Math.abs(u[2]),w;
    if(absu1>=absu2)
    {
        final double t=absu2/absu1;
        w=Math.sqrt(absu1*0.5 * (1.0 + Math.sqrt(1.0 + t * t)));
        if(PRINT_DEBUG) System.err.println(" Path1 ");
    }
    else
    {
        final double t=absu1/absu2;
        w=Math.sqrt(absu2*0.5 * (t + Math.sqrt(1.0 + t * t)));
        if(PRINT_DEBUG) System.err.println(" Path1a ");
    }
    if(u[1]>=0)
    {
        w1_re=w;
        w1_im=u[2]/(2*w);
        if(PRINT_DEBUG) System.err.println(" Path2 ");
    }
    else
    {
        final double vi = (u[2] >= 0) ? w : -w;
        w1_re=u[2]/(2*vi);
        w1_im=vi;
        if(PRINT_DEBUG) System.err.println(" Path2a ");
    }
      }
      final double absu0=Math.abs(u[0]);
      if(w_mod2>=absu0)
      {
    mod_w1w2=w_mod2;
    mod_w1w2_squared=w_mod2_sq;
    w2_re=w1_re;
    w2_im=-w1_im;
      }
      else
      {
    mod_w1w2=-1;
    mod_w1w2_squared=w_mod2*absu0;
    if(u[0]>=0)
    {
        w2_re=Math.sqrt(absu0);
        w2_im=0;
    }
    else
    {
        w2_re=0;
        w2_im=Math.sqrt(absu0);
    }
      }
      if(PRINT_DEBUG) System.err.println("u[0]="+u[0]+"u[1]="+u[1]+" u[2]="+u[2]+" absu0="+absu0+" w_mod="+w_mod+" w_mod2="+w_mod2);
  }

  /* Solve the quadratic in order to obtain the roots
   * to the quartic */
  if(mod_w1w2>0)
  {
      // a shorcut to reduce rounding error
      w3_re=qq/(-8)/mod_w1w2;
      w3_im=0;
  }
  else if(mod_w1w2_squared>0)
  {
      // regular path
      final double mqq8n=qq/(-8)/mod_w1w2_squared;
      w3_re=mqq8n*(w1_re*w2_re-w1_im*w2_im);
      w3_im=-mqq8n*(w1_re*w2_im+w2_re*w1_im);
  }
  else
  {
      // typically occur when qq==0
      w3_re=w3_im=0;
  }

  final double h = r4 * a;
  if(PRINT_DEBUG) System.err.println("w1_re="+w1_re+" w1_im="+w1_im+" w2_re="+w2_re+" w2_im="+w2_im+" w3_re="+w3_re+" w3_im="+w3_im+" h="+h);

  re_root[0]=w1_re+w2_re+w3_re-h;
  im_root[0]=w1_im+w2_im+w3_im;
  re_root[1]=-(w1_re+w2_re)+w3_re-h;
  im_root[1]=-(w1_im+w2_im)+w3_im;
  re_root[2]=w2_re-w1_re-w3_re-h;
  im_root[2]=w2_im-w1_im-w3_im;
  re_root[3]=w1_re-w2_re-w3_re-h;
  im_root[3]=w1_im-w2_im-w3_im;

  return 4;
}



    static void setRandomP(final double [] p, final int n, Random r)
    {
  if(r.nextDouble()<0.1)
  {
      // integer coefficiens
      for(int j=0;j<p.length;j++)
      {
    if(j<=n)
    {
        p[j]=(r.nextInt(2)<=0?-1:1)*r.nextInt(10);
    }
    else
    {
        p[j]=0;
    }
      }
  }
  else
  {
      // real coefficiens
      for(int j=0;j<p.length;j++)
      {
    if(j<=n)
    {
        p[j]=-1+2*r.nextDouble();
    }
    else
    {
        p[j]=0;
    }
      }
  }
  if(Math.abs(p[n])<1e-2)
  {
      p[n]=(r.nextInt(2)<=0?-1:1)*(0.1+r.nextDouble());
  }
    }


    static void checkValues(final double [] p,
          final int n,
          final double rex,
          final double imx,
          final double eps,
          final String txt)
    {
  double res=0,ims=0,sabs=0;
  final double xabs=Math.abs(rex)+Math.abs(imx);
  for(int k=n;k>=0;k--)
  {
      final double res1=(res*rex-ims*imx)+p[k];
      final double ims1=(ims*rex+res*imx);
      res=res1;
      ims=ims1;
      sabs+=xabs*sabs+p[k];
  }
  sabs=Math.abs(sabs);
  if(false && sabs>1/eps?
     (!(Math.abs(res/sabs)<=eps)||!(Math.abs(ims/sabs)<=eps))
     :
     (!(Math.abs(res)<=eps)||!(Math.abs(ims)<=eps)))
  {
      throw new RuntimeException(
    getPolinomTXT(p)+"\n"+
    "\t x.r="+rex+" x.i="+imx+"\n"+
    "res/sabs="+(res/sabs)+" ims/sabs="+(ims/sabs)+
    " sabs="+sabs+
    "\nres="+res+" ims="+ims+" n="+n+" eps="+eps+" "+
    " sabs>1/eps="+(sabs>1/eps)+
    " f1="+(!(Math.abs(res/sabs)<=eps)||!(Math.abs(ims/sabs)<=eps))+
    " f2="+(!(Math.abs(res)<=eps)||!(Math.abs(ims)<=eps))+
    " "+txt);
  }
    }

    static String getPolinomTXT(final double [] p)
    {
  final StringBuilder buf=new StringBuilder();
  buf.append("order="+(p.length-1)+"\t");
  for(int k=0;k<p.length;k++)
  {
      buf.append("p["+k+"]="+p[k]+";");
  }
  return buf.toString();
    }

    static String getRootsTXT(int nr,final double [] re,final double [] im)
    {
  final StringBuilder buf=new StringBuilder();
  for(int k=0;k<nr;k++)
  {
      buf.append("x."+k+"("+re[k]+","+im[k]+")\n");
  }
  return buf.toString();
    }

    static void testRoots(final int n,
        final int n_tests,
        final Random rn,
        final double eps)
    {
  final double [] p=new double [n+1];
  final double [] rex=new double [n],imx=new double [n];
  for(int i=0;i<n_tests;i++)
  {
    for(int dg=n;dg-->-1;)
    {
      for(int dr=3;dr-->0;)
      {
        setRandomP(p,n,rn);
        for(int j=0;j<=dg;j++)
        {
      p[j]=0;
        }
        if(dr==0)
        {
      p[0]=-1+2.0*rn.nextDouble();
        }
        else if(dr==1)
        {
      p[0]=p[1]=0;
        }

        findPolynomialRoots(n,p,rex,imx);

        for(int j=0;j<n;j++)
        {
      //System.err.println("j="+j);
      checkValues(p,n,rex[j],imx[j],eps," t="+i);
        }
      }
    }
  }
  System.err.println("testRoots(): n_tests="+n_tests+" OK, dim="+n);
    }




    static final double EPS=0;

    public static int root1(final double [] p,final double [] re_root,final double [] im_root)
    {
  if(!(Math.abs(p[1])>EPS))
  {
      re_root[0]=im_root[0]=Double.NaN;
      return -1;
  }
  re_root[0]=-p[0]/p[1];
  im_root[0]=0;
  return 1;
    }

    public static int root2(final double [] p,final double [] re_root,final double [] im_root)
    {
  if(!(Math.abs(p[2])>EPS))
  {
      re_root[0]=re_root[1]=im_root[0]=im_root[1]=Double.NaN;
      return -1;
  }
  final double b2=0.5*(p[1]/p[2]),c=p[0]/p[2],d=b2*b2-c;
  if(d>=0)
  {
      final double sq=Math.sqrt(d);
      if(b2<0)
      {
    re_root[1]=-b2+sq;
    re_root[0]=c/re_root[1];
      }
      else if(b2>0)
      {
    re_root[0]=-b2-sq;
    re_root[1]=c/re_root[0];
      }
      else
      {
    re_root[0]=-b2-sq;
    re_root[1]=-b2+sq;
      }
      im_root[0]=im_root[1]=0;
  }
  else
  {
      final double sq=Math.sqrt(-d);
      re_root[0]=re_root[1]=-b2;
      im_root[0]=sq;
      im_root[1]=-sq;
  }
  return 2;
    }

    public static int root3(final double [] p,final double [] re_root,final double [] im_root)
    {
  final double vs=p[3];
  if(!(Math.abs(vs)>EPS))
  {
      re_root[0]=re_root[1]=re_root[2]=
    im_root[0]=im_root[1]=im_root[2]=Double.NaN;
      return -1;
  }
  final double a=p[2]/vs,b=p[1]/vs,c=p[0]/vs;
  /* zsolve_cubic.c - finds the complex roots of x^3 + a x^2 + b x + c = 0
   */
  final double q = (a * a - 3 * b);
  final double r = (a*(2 * a * a - 9 * b) + 27 * c);

  final double Q = q / 9;
  final double R = r / 54;

  final double Q3 = Q * Q * Q;
  final double R2 = R * R;

  final double CR2 = 729 * r * r;
  final double CQ3 = 2916 * q * q * q;
  final double a3=a/3;

  if (R == 0 && Q == 0)
  {
      re_root[0]=re_root[1]=re_root[2]=-a3;
      im_root[0]=im_root[1]=im_root[2]=0;
      return 3;
  }
  else if (CR2 == CQ3)
  {
      /* this test is actually R2 == Q3, written in a form suitable
         for exact computation with integers */

      /* Due to finite precision some double roots may be missed, and
         will be considered to be a pair of complex roots z = x +/-
         epsilon i close to the real axis. */

      final double sqrtQ = Math.sqrt (Q);

      if (R > 0)
      {
    re_root[0] = -2 * sqrtQ - a3;
    re_root[1]=re_root[2]=sqrtQ - a3;
    im_root[0]=im_root[1]=im_root[2]=0;
      }
      else
      {
    re_root[0]=re_root[1] = -sqrtQ - a3;
    re_root[2]=2 * sqrtQ - a3;
    im_root[0]=im_root[1]=im_root[2]=0;
      }
      return 3;
  }
  else if (R2 < Q3)
  {
      final double sgnR = (R >= 0 ? 1 : -1);
      final double ratio = sgnR * Math.sqrt (R2 / Q3);
      final double theta = Math.acos (ratio);
      final double norm = -2 * Math.sqrt (Q);
      final double r0 = norm * Math.cos (theta/3) - a3;
      final double r1 = norm * Math.cos ((theta + 2.0 * Math.PI) / 3) - a3;
      final double r2 = norm * Math.cos ((theta - 2.0 * Math.PI) / 3) - a3;

      re_root[0]=r0;
      re_root[1]=r1;
      re_root[2]=r2;
      im_root[0]=im_root[1]=im_root[2]=0;
      return 3;
  }
  else
  {
      final double sgnR = (R >= 0 ? 1 : -1);
      final double A = -sgnR * Math.pow (Math.abs (R) + Math.sqrt (R2 - Q3), 1.0 / 3.0);
      final double B = Q / A;

      re_root[0]=A + B - a3;
      im_root[0]=0;
      re_root[1]=-0.5 * (A + B) - a3;
      im_root[1]=-(SQRT3*0.5) * Math.abs(A - B);
      re_root[2]=re_root[1];
      im_root[2]=-im_root[1];
      return 3;
  }

    }


    static void root3a(final double [] p,final double [] re_root,final double [] im_root)
    {
  if(Math.abs(p[3])>EPS)
  {
      final double v=p[3],
    a=p[2]/v,b=p[1]/v,c=p[0]/v,
    a3=a/3,a3a=a3*a,
    pd3=(b-a3a)/3,
    qd2=a3*(a3a/3-0.5*b)+0.5*c,
    Q=pd3*pd3*pd3+qd2*qd2;
      if(Q<0)
      {
    // three real roots
    final double SQ=Math.sqrt(-Q);
    final double th=Math.atan2(SQ,-qd2);
    im_root[0]=im_root[1]=im_root[2]=0;
    final double f=2*Math.sqrt(-pd3);
    re_root[0]=f*Math.cos(th/3)-a3;
    re_root[1]=f*Math.cos((th+2*Math.PI)/3)-a3;
    re_root[2]=f*Math.cos((th+4*Math.PI)/3)-a3;
    //System.err.println("3r");
      }
      else
      {
    // one real & two complex roots
    final double SQ=Math.sqrt(Q);
    final double r1=-qd2+SQ,r2=-qd2-SQ;
    final double v1=Math.signum(r1)*Math.pow(Math.abs(r1),1.0/3),
        v2=Math.signum(r2)*Math.pow(Math.abs(r2),1.0/3),
        sv=v1+v2;
    // real root
    re_root[0]=sv-a3;
    im_root[0]=0;
    // complex roots
    re_root[1]=re_root[2]=-0.5*sv-a3;
    im_root[1]=(v1-v2)*(SQRT3*0.5);
    im_root[2]=-im_root[1];
    //System.err.println("1r2c");
      }
  }
  else
  {
      re_root[0]=re_root[1]=re_root[2]=im_root[0]=im_root[1]=im_root[2]=Double.NaN;
  }
    }


    static void printSpecialValues()
    {
  for(int st=0;st<6;st++)
  {
      //final double [] p=new double []{8,1,3,3.6,1};
      final double [] re_root=new double [4],im_root=new double [4];
      final double [] p;
      final int n;
      if(st<=3)
      {
    if(st<=0)
    {
        p=new double []{2,-4,6,-4,1};
        //p=new double []{-6,6,-6,8,-2};
    }
    else if(st==1)
    {
        p=new double []{0,-4,8,3,-9};
    }
    else if(st==2)
    {
        p=new double []{-1,0,2,0,-1};
    }
    else
    {
        p=new double []{-5,2,8,-2,-3};
    }
    root4(p,re_root,im_root);
    n=4;
      }
      else
      {
    p=new double []{0,2,0,1};
    if(st==4)
    {
        p[1]=-p[1];
    }
    root3(p,re_root,im_root);
    n=3;
      }
      System.err.println("======== n="+n);
      for(int i=0;i<=n;i++)
      {
    if(i<n)
    {
        System.err.println(String.valueOf(i)+"\t"+
               p[i]+"\t"+
               re_root[i]+"\t"+
               im_root[i]);
    }
    else
    {
        System.err.println(String.valueOf(i)+"\t"+p[i]+"\t");
    }
      }
  }
    }



    public static void main(final String [] args)
    {
      if (System.getProperty("os.arch").equals("x86") ||
         System.getProperty("os.arch").equals("amd64") ||
         System.getProperty("os.arch").equals("x86_64")){
        final long t0=System.currentTimeMillis();
        final double eps=1e-6;
        //checkRoots();
        final Random r = Utils.getRandomInstance();
        printSpecialValues();

        final int n_tests=100000;
        //testRoots(2,n_tests,r,eps);
        //testRoots(3,n_tests,r,eps);
        testRoots(4,n_tests,r,eps);
        final long t1=System.currentTimeMillis();
        System.err.println("PolynomialRoot.main: "+n_tests+" tests OK done in "+(t1-t0)+" milliseconds. ver=$Id: PolynomialRoot.java,v 1.105 2012/08/18 00:00:05 mal Exp $");
        System.out.println("PASSED");
     } else {
       System.out.println("PASS test for non-x86");
     }
   }



}
