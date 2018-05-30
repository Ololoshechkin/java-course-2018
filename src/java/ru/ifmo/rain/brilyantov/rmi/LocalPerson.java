package ru.ifmo.rain.brilyantov.rmi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public class LocalPerson extends AbstractPerson {
    private final HashMap<String, Account> accounts;

    protected LocalPerson() {
        accounts = null;
    }

    public LocalPerson(
            String name,
            String lastName,
            String oficialID,
            HashMap<String, Account> accounts,
            ArrayList<String> accountSubIds
    ) {
        super(name, lastName, oficialID, accountSubIds);
        this.accounts = accounts;
    }

    public LocalPerson(String name, String lastName, String oficialID, HashMap<String, Account> accounts) {
        this(name, lastName, oficialID, accounts, new ArrayList<>());
    }

    @Override
    public Account getAccount(String subId) throws RemoteException {
        return accounts.get(subId);
    }

    @Override
    public Account createAccount(String subId) throws RemoteException {
        return accounts.computeIfAbsent(subId, accId -> {
            accountSubIds.add(subId);
            return new RemoteAccount(accId);
        });
    }

}
