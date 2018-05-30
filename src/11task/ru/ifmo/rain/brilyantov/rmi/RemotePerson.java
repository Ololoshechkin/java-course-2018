package ru.ifmo.rain.brilyantov.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;

public class RemotePerson extends AbstractPerson {

    private Bank bank = null;

    private String resolveAccountId(String subId) {
        return oficialID + ":" + subId;
    }

    public RemotePerson(String name, String lastName, String oficialID) {
        super(name, lastName, oficialID);
    }

    @Override
    public synchronized Account getAccount(String subId) throws RemoteException {
        return getCachedBank().getAccount(resolveAccountId(subId));
    }

    @Override
    public Account createAccount(String subId) throws RemoteException {
        if (getAccount(subId) == null) {
            accountSubIds.add(subId);
        }
        return getCachedBank().createAccount(resolveAccountId(subId));
    }

    public synchronized LocalPerson getLocalCopy() throws RemoteException {
        System.out.println("creating local copy");
        HashMap<String, Account> localAccounts = new HashMap<>();
        RemoteException exception = new RemoteException("failed to get some accounts");
        getAccountSubIds().forEach(subId -> {
            try {
                localAccounts.put(subId, getAccount(subId));
            } catch (RemoteException e) {
                exception.addSuppressed(e);
            }
        });
        if (exception.getSuppressed().length != 0) {
            throw exception;
        }
        System.out.println("returning local copy");
        return new LocalPerson(
                this.name,
                this.lastName,
                this.oficialID,
                localAccounts,
                accountSubIds
        );
    }

    private synchronized Bank getCachedBank() throws RemoteException {
        if (bank == null) {
            try {
                bank = (Bank) Naming.lookup("//localhost/bank");
            } catch (NotBoundException | MalformedURLException cause) {
                RemoteException exception = new RemoteException();
                exception.addSuppressed(cause);
                throw exception;
            }
        }
        return bank;
    }
}
