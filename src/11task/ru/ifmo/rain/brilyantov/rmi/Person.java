package ru.ifmo.rain.brilyantov.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Person extends Remote, Serializable {

    String getName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getOficialId() throws RemoteException;

    Account getAccount(String subId) throws RemoteException;

    Account createAccount(String subId) throws RemoteException;

}
