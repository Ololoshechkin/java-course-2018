package info.kgeorgiy.java.advanced.implementor.standard.full;
class LdapReferralExceptionImpl extends LdapReferralException {

    public javax.naming.Context getReferralContext(java.util.Hashtable arg0, javax.naming.ldap.Control[] arg1) throws javax.naming.NamingException {
        return null;
   }

    public javax.naming.Context getReferralContext() throws javax.naming.NamingException {
        return null;
   }

    public void retryReferral()  {
        return;
   }

    public javax.naming.Context getReferralContext(java.util.Hashtable arg0) throws javax.naming.NamingException {
        return null;
   }

    public boolean skipReferral()  {
        return false;
   }

    public java.lang.Object getReferralInfo()  {
        return null;
   }

    protected LdapReferralExceptionImpl(java.lang.String arg0)  {
        super(arg0);
   }

    protected LdapReferralExceptionImpl()  {
        super();
   }
}