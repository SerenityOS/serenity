/* /nodynamiccopyright/ */
// combinations of methods defined in an interface
// and overridden in subtypes

// interface should compile with no warnings

interface I {
    /** @deprecated */ public void iDep_aDep_bDep();
    /** @deprecated */ public void iDep_aDep_bUnd();
    /** @deprecated */ public void iDep_aDep_bInh();
    /** @deprecated */ public void iDep_aUnd_bDep();
    /** @deprecated */ public void iDep_aUnd_bUnd();
    /** @deprecated */ public void iDep_aUnd_bInh();
    /** @deprecated */ public void iDep_aInh_bDep();
    /** @deprecated */ public void iDep_aInh_bUnd();
    //                 public void iDep_aInh_bInh();
                       public void iUnd_aDep_bDep();
                       public void iUnd_aDep_bUnd();
                       public void iUnd_aDep_bInh();
                       public void iUnd_aUnd_bDep();
                       public void iUnd_aUnd_bUnd();
                       public void iUnd_aUnd_bInh();
                       public void iUnd_aInh_bDep();
                       public void iUnd_aInh_bUnd();
    //                 public void iUnd_aInh_bInh();
}
