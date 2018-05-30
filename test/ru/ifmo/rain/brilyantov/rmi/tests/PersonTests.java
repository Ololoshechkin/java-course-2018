package ru.ifmo.rain.brilyantov.rmi.tests;

import org.junit.Test;
import ru.ifmo.rain.brilyantov.rmi.*;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class PersonTests {

    private static Bank bank;


    static {
        System.out.println("start");
        System.out.println("_main_");
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (Throwable e) {
            System.out.println("failed to run server! : " + e.getMessage());
        }
        System.out.println("exit start");
    }

    @Test
    public void testEmptyInit() throws RemoteException {
        assertNotNull("bank is not created", bank);
        assertNull(
                "expected null local person",
                bank.lookupPerson(
                        "abacaba",
                        RemoteBank.PersonType.LOCAL
                )
        );
        assertNull(
                "expected null remote person",
                bank.lookupPerson(
                        "abacaba",
                        RemoteBank.PersonType.REMOTE
                )
        );
    }

    @Test
    public void testRegisterRemote() throws RemoteException {
        Person person = bank.registerPerson(
                "A",
                "B",
                "0",
                RemoteBank.PersonType.REMOTE
        );
        assertNotNull("expected not-nullnull person", person);
        assertEquals(
                "person differs than person being registered",
                person,
                new LocalPerson(
                        "A",
                        "B",
                        "0",
                        new HashMap<>()
                )
        );
    }

    @Test
    public void testRegisterLocal() throws RemoteException {
        Person person = bank.registerPerson(
                "A",
                "B",
                "0",
                RemoteBank.PersonType.LOCAL
        );
        assertNotNull("expected not-nullnull person", person);
        assertEquals(
                "person differs than person being registered",
                person,
                new LocalPerson("A", "B", "0", new HashMap<>())
        );
    }


    @Test
    public void testLookup() throws RemoteException {
        Person person = bank.registerPerson(
                "A",
                "B",
                "0",
                RemoteBank.PersonType.REMOTE
        );
        assertNotNull("expected not-nullnull person", person);
        assertEquals(
                "lookup(remote) returned different person",
                person,
                bank.lookupPerson("0", RemoteBank.PersonType.REMOTE)
        );
        assertEquals(
                "lookup(local) returned different person",
                person,
                bank.lookupPerson("0", RemoteBank.PersonType.LOCAL)
        );
    }

    @Test
    public void testMultipleRegister() throws RemoteException {
        Person person1 = bank.registerPerson(
                "A",
                "B",
                "0",
                RemoteBank.PersonType.REMOTE
        );
        assertNotNull("expected not-nullnull person1", person1);
        Person person2 = bank.registerPerson(
                "A",
                "B",
                "0",
                RemoteBank.PersonType.REMOTE
        );
        assertNotNull("expected not-nullnull person2", person2);
        assertEquals("persons (1, 2) differ", person1, person2);
        Person person3 = bank.registerPerson(
                "A",
                "B",
                "0",
                RemoteBank.PersonType.LOCAL
        );
        assertNotNull("expected not-nullnull person3", person3);
        assertEquals("persons (1, 3) differ", person1, person3);
    }

    @Test
    public void testRemoteChangesRemoteState() throws RemoteException {
        Person person = bank.registerPerson(
                "A",
                "B",
                "id",
                RemoteBank.PersonType.REMOTE
        );
        assertNotNull("expected not-nullnull person1", person);
        Account acc1 = person.createAccount("acc1");
        assertNotNull("person account is null", acc1);
        Account acc2 = bank.getAccount(person.getOficialId() + ":acc1");
        assertNotNull("bank account is null (remote state didn't change)", acc2);
        assertEquals("accounts (1, 2) differ", acc1, acc2);
        Person person2 = bank.lookupPerson(
                "id",
                RemoteBank.PersonType.REMOTE
        );
        Account acc3 = person2.getAccount("acc1");
        assertNotNull("new person has no idea of the account1", acc3);
        assertEquals("accounts (1, 3) differ", acc1, acc3);
        Account acc4 = person2.createAccount("acc4");
        assertNotNull("account 4 is null", acc4);
        Account acc5 = person.getAccount("acc4");
        assertNotNull("old client has no idea ow new account4", acc5);
        assertEquals("accounts (1, 5) differ", acc5, acc4);
    }

    @Test
    public void testLocalNeverChangesRemoteState() throws RemoteException {
        Person person = bank.registerPerson(
                "A",
                "B",
                "id",
                RemoteBank.PersonType.REMOTE
        );
        Account acc0 = person.createAccount("acc0");
        Person localPerson = bank.lookupPerson("id", RemoteBank.PersonType.LOCAL);
        Account acc1 = localPerson.getAccount("acc0");
        assertNotNull("local person does not know about pre-created account acc0", acc1);
        assertEquals("accounts (0, 1) differ => local person got contrafact information", acc1, acc0);
        person.createAccount("acc2");
        assertNull("local person knows about remote updates", localPerson.getAccount("acc2"));
        localPerson.createAccount("acc3");
        assertNull("remote person knows about local updates", person.getAccount("acc3"));
    }


    @Test
    public void testMultipleQueries() throws RemoteException {
        Person person = bank.registerPerson(
                "A",
                "B",
                "id",
                RemoteBank.PersonType.REMOTE
        );
        for (int i = 0; i < 10000; ++i) {
            Account accI1 = person.createAccount("acc" + i);
            Account accI2 = person.getAccount("acc" + i);
            assertNotNull("newborn account is null at iteration : " + i, accI1);
            assertEquals("lookup returned different data at iteration : " + i, accI1, accI2);
        }
    }

    @FunctionalInterface
    private interface Function2<A, B, R> {
        R apply(A a, B b);
    }

    private void parallelTest(Function2<Integer, AtomicReference<Exception>, Runnable> runnableGenerator) {
        AtomicReference<Exception> ex = new AtomicReference<>(null);
        ArrayList<Thread> threads = new ArrayList<>();
        IntStream.range(0, 32).forEach(i -> {
            threads.add(new Thread(runnableGenerator.apply(i, ex)));
        });
        threads.forEach(Thread::start);
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (Exception e) {
                System.out.println("failed to join thread !");
                ex.set(e);
            }
        });
        assertNull("test failed with an exception\n" + ex.get().getMessage(), ex.get());
    }

    @Test
    public void testParallelCreates() {
        parallelTest((i, ex) -> () -> {
            for (int j = 0; j < 10; ++j) {
                try {
                    Person p = bank.registerPerson(
                            "koko",
                            "abacaba",
                            "id" + i,
                            RemoteBank.PersonType.REMOTE
                    );
                    Account acc = p.createAccount("acc" + j);
                    assertEquals(
                            "account differs at thread " + i,
                            acc,
                            p.getAccount("acc" + j)
                    );
                } catch (RemoteException e) {
                    ex.set(e);
                }
            }
        });
    }

    @Test
    public void testParallelQueries() throws RemoteException {
        Person p = bank.registerPerson(
                "a",
                "a",
                "id_15",
                RemoteBank.PersonType.REMOTE
        );
        parallelTest((i, ex) -> () -> {
            for (int j = 0; j < 10; ++j) {
                try {
                    Account acc = p.createAccount("acc" + j);
                    assertEquals(
                            "account differs at thread " + i,
                            acc,
                            p.getAccount("acc" + j)
                    );
                    if (j != 0) {
                        assertNotNull("account is null", p.getAccount("acc" + (j - 1)));
                    }
                } catch (RemoteException e) {
                    ex.set(e);
                }
            }
        });
    }

}

