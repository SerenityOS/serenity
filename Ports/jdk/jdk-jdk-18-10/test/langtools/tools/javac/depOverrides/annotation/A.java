/* /nodynamiccopyright/ */
// combinations of methods defined in an interface
// and overridden in subtypes

// class should compile with deprecation warnings as shown

abstract class A implements I {
    @Deprecated public void iDep_aDep_bDep() { }
    @Deprecated public void iDep_aDep_bUnd() { }
    @Deprecated public void iDep_aDep_bInh() { }
                public void iDep_aUnd_bDep() { } // warn
                public void iDep_aUnd_bUnd() { } // warn
                public void iDep_aUnd_bInh() { } // warn
    //          public void iDep_aInh_bDep() { }
    //          public void iDep_aInh_bUnd() { }
    //          public void iDep_aInh_bInh() { }
    @Deprecated public void iUnd_aDep_bDep() { }
    @Deprecated public void iUnd_aDep_bUnd() { }
    @Deprecated public void iUnd_aDep_bInh() { }
                public void iUnd_aUnd_bDep() { }
                public void iUnd_aUnd_bUnd() { }
                public void iUnd_aUnd_bInh() { }
    //          public void iUnd_aInh_bDep() { }
    //          public void iUnd_aInh_bUnd() { }
    //          public void iUnd_aInh_bInh() { }
}
