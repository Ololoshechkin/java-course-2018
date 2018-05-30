package ru.ifmo.rain.brilyantov.rmi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPerson implements Person {

    protected final String name;
    protected final String lastName;
    protected final String oficialID;
    protected final ArrayList<String> accountSubIds;

    protected AbstractPerson() {
        name = "";
        lastName = "";
        oficialID = "";
        accountSubIds = null;
    }

    public AbstractPerson(String name, String lastName, String oficialID, ArrayList<String> accountSubIds) {
        this.name = name;
        this.lastName = lastName;
        this.oficialID = oficialID;
        this.accountSubIds = accountSubIds;
    }

    public AbstractPerson(String name, String lastName, String oficialID) {
        this(name, lastName, oficialID, new ArrayList<>());
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public String getLastName() throws RemoteException {
        return lastName;
    }

    @Override
    public String getOficialId() throws RemoteException {
        return oficialID;
    }

    protected List<String> getAccountSubIds() {
        return accountSubIds;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            return obj instanceof Person
                    && ((Person) obj).getName().equals(name)
                    && ((Person) obj).getLastName().equals(lastName)
                    && ((Person) obj).getOficialId().equals(oficialID);
        } catch (RemoteException e) {
            System.out.println("failed to lookup some remote fields");
            return false;
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 * 31 + lastName.hashCode() * 31 + oficialID.hashCode();
    }
}
