/* /nodynamiccopyright/ */
// combinations of methods defined in an interface
// and overridden in subtypes

// class overrides deprecated mthods as shown, but should not give warnings by
// virtue of being deprecated itself

/** @deprecated */
abstract class B3 extends A implements I {
    /** @deprecated */ public void iDep_aDep_bDep() { }
                       public void iDep_aDep_bUnd() { } // potential warning x 2, suppressed
    //                 public void iDep_aDep_bInh() { }
    /** @deprecated */ public void iDep_aUnd_bDep() { }
                       public void iDep_aUnd_bUnd() { } // potential warning, suppressed
    //                 public void iDep_aUnd_bInh() { } // potential warning, suppressed
    /** @deprecated */ public void iDep_aInh_bDep() { }
                       public void iDep_aInh_bUnd() { } // potential warning, suppressed
    //                 public void iDep_aInh_bInh() { }
    /** @deprecated */ public void iUnd_aDep_bDep() { }
                       public void iUnd_aDep_bUnd() { } // potential warning, suppressed
    //                 public void iUnd_aDep_bInh() { }
    /** @deprecated */ public void iUnd_aUnd_bDep() { }
                       public void iUnd_aUnd_bUnd() { }
    //                 public void iUnd_aUnd_bInh() { }
    /** @deprecated */ public void iUnd_aInh_bDep() { }
                       public void iUnd_aInh_bUnd() { }
    //                 public void iUnd_aInh_bInh() { }
}
