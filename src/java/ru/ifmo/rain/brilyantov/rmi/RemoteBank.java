package ru.ifmo.rain.brilyantov.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RemotePerson> persons = new ConcurrentHashMap<>();

    public enum PersonType {
        LOCAL, REMOTE
    }

    public RemoteBank(final int port) {
        this.port = port;
    }

    public Account createAccount(final String id) throws RemoteException {
        System.out.println("Creating account " + id);
        final Account account = new RemoteAccount(id);
        if (accounts.putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(id);
        }
    }

    public Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }

    @Override
    public Person lookupPerson(String oficialId, PersonType type) throws RemoteException {
        System.out.println("Lookuping person " + oficialId);
        RemotePerson person = persons.get(oficialId);
        if (person == null) {
            return null;
        }
        switch (type) {
            case LOCAL:
                LocalPerson localPerson =  person.getLocalCopy();
                System.out.println("local person created");
                return localPerson;
            case REMOTE:
                return person;
            default:
                return null;
        }
    }

    @Override
    public Person registerPerson(
            String name,
            String lastName,
            String oficialId,
            RemoteBank.PersonType type
    ) throws RemoteException {
        System.out.println("Registering person " + name + " " + lastName + " " + oficialId);
        if (!persons.containsKey(oficialId)) {
            RemotePerson person = new RemotePerson(name, lastName, oficialId);
            UnicastRemoteObject.exportObject(person, port);
            persons.put(oficialId, person);
        }
        System.out.println("person created");
        switch (type) {
            case LOCAL:
                return persons.get(oficialId).getLocalCopy();
            case REMOTE:
                return persons.get(oficialId);
            default:
                return null;
        }
    }

}
