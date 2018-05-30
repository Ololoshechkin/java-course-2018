package ru.ifmo.rain.brilyantov.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;

public class Client {
    private static String LOCAL = "local";
    private static String REMOTE = "remote";

    static boolean checkPerson(Person person, String name, String lastName) throws RemoteException {
        return person.getName().equals(name) && person.getLastName().equals(lastName);
    }

    //имя, фамилия, номер паспорта физического лица, номер счета, изменение суммы счета
    public static void main(final String... args) throws RemoteException {
        if (args == null || args.length != 6) {
            System.out.println("expected 6 arguments: " +
                    "name, last name, oficial id, " +
                    "account subId, account balance change " +
                    "and type of person(\"" + LOCAL + "\"/\"" + REMOTE + "\")");
            return;
        }
        String name = args[0];
        String lastName = args[1];
        String oficialID = args[2];
        String accountSubId = args[3];
        int balanceChange;
        try {
            balanceChange = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("5th argument (balance change) expected to be a number, " + e.getMessage());
            return;
        }
        RemoteBank.PersonType personType;
        if (args[5].equals(LOCAL)) {
            personType = RemoteBank.PersonType.LOCAL;
        } else if (args[5].equals(REMOTE)) {
            personType = RemoteBank.PersonType.REMOTE;
        } else {
            System.out.println("last argument (person type) expected to be " +
                    "either : " + LOCAL + " or " + REMOTE);
            return;
        }
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }
        Person person = bank.lookupPerson(oficialID, personType);
        if (person == null) {
            System.out.println("registering new person");
            person = bank.registerPerson(name, lastName, oficialID, personType);
        }
        if (!checkPerson(person, name, lastName)) {
            System.out.println("person failed verification (name or surname differs)");
            return;
        }
        Account account = person.getAccount(accountSubId);
        if (account == null) {
            System.out.println("Creating account");
            account = person.createAccount(accountSubId);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + balanceChange);
        System.out.println("Money: " + account.getAmount());
    }
}
