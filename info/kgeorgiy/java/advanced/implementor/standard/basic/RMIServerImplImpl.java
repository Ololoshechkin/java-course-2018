package info.kgeorgiy.java.advanced.implementor.standard.basic;
class RMIServerImplImpl extends RMIServerImpl {

    public java.rmi.Remote toStub() throws java.io.IOException {
        return null;
   }

    protected java.lang.String getProtocol()  {
        return null;
   }

    protected javax.management.remote.rmi.RMIConnection makeClient(java.lang.String arg0, javax.security.auth.Subject arg1) throws java.io.IOException {
        return null;
   }

    protected void export() throws java.io.IOException {
        return;
   }

    protected void closeServer() throws java.io.IOException {
        return;
   }

    protected void closeClient(javax.management.remote.rmi.RMIConnection arg0) throws java.io.IOException {
        return;
   }

    public RMIServerImplImpl(java.util.Map arg0)  {
        super(arg0);
   }
}