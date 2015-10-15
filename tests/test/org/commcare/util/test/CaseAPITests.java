package org.commcare.util.test;

import org.commcare.api.persistence.SqlHelper;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import static org.junit.Assert.fail;

public class CaseAPITests {

    Case a, b, c, d, e;

    Ledger l;

    DummyIndexedStorageUtility<Case> storage;
    String owner;
    String groupOwner;
    String otherOwner;
    Vector<String> groupOwned;
    Vector<String> userOwned;


    @Before
    public void setUp() throws Exception {

        storage = new DummyIndexedStorageUtility<Case>(Case.class, new PrototypeFactory());

        owner = "owner";
        otherOwner = "otherowner";
        groupOwner = "groupowned";

        userOwned = new Vector<String>();
        userOwned.addElement(owner);

        groupOwned = new Vector<String>();
        groupOwned.addElement(owner);
        groupOwned.addElement(groupOwner);

        a = new Case("123", "a");
        a.setCaseId("a");
        a.setUserId(owner);
        a.setID(12345);
        b = new Case("b", "b");
        b.setCaseId("b");
        b.setUserId(owner);
        c = new Case("c", "c");
        c.setCaseId("c");
        c.setUserId(owner);
        d = new Case("d", "d");
        d.setCaseId("d");
        d.setUserId(owner);
        e = new Case("e", "e");
        e.setCaseId("e");
        e.setUserId(groupOwner);

        l = new Ledger("ledger_entity_id");
        l.setID(12345);
        l.setEntry("test_section_id", "test_entry_id", 2345);
    }

    @Test
    public void testOwnerPurge() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:test.db");

            SqlHelper.dropTable(connection, "TFLedger");

            SqlHelper.createTable(connection, "TFLedger", new Ledger());

            SqlHelper.insertToTable(connection, "TFLedger", l);

            preparedStatement = SqlHelper.prepareTableSelectStatement(connection, "TFLedger", new String[]{"entity-id"},
                    new String[]{"ledger_entity_id"}, new Ledger());
            if (preparedStatement == null) {
                fail("failed to prepare table select query");
            }
            ResultSet rs = preparedStatement.executeQuery();
            byte[] caseBytes = rs.getBytes("commcare_sql_record");
            DataInputStream is = new DataInputStream(new ByteArrayInputStream(caseBytes));

            Ledger readLedger = new Ledger();
            PrototypeFactory lPrototypeFactory = new PrototypeFactory();
            lPrototypeFactory.addClass(Ledger.class);
            readLedger.readExternal(is, lPrototypeFactory);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        } finally{
            try {
                if(preparedStatement != null) {
                    preparedStatement.close();
                }
                if(connection != null) {
                    connection.close();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
}