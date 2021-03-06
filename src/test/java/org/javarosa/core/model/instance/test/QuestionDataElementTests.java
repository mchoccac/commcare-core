package org.javarosa.core.model.instance.test;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QuestionDataElementTests {
    private static final String stringElementName = "String Data Element";

    static StringData stringData;
    static IntegerData integerData;

    static TreeElement stringElement;
    static TreeElement intElement;

    @BeforeClass
    public static void setUp() {
        stringData = new StringData("Answer Value");
        integerData = new IntegerData(4);

        intElement = new TreeElement("intElement");
        intElement.setValue(integerData);

        stringElement = new TreeElement(stringElementName);
        stringElement.setValue(stringData);

    }

    @Test
    public void testIsLeaf() {
        assertTrue("Question Data Element returned negative for being a leaf", stringElement.isLeaf());
    }

    @Test
    public void testGetName() {
        assertEquals("Question Data Element 'string' did not properly get its name", stringElement.getName(), stringElementName);
    }

    @Test
    public void testSetName() {
        String newName = "New Name";
        stringElement.setName(newName);

        assertEquals("Question Data Element 'string' did not properly set its name", stringElement.getName(), newName);
    }

    @Test
    public void testGetValue() {
        IAnswerData data = stringElement.getValue();
        assertEquals("Question Data Element did not return the correct value", data, stringData);
    }

    @Test
    public void testSetValue() {
        stringElement.setValue(integerData);
        assertEquals("Question Data Element did not set value correctly", stringElement.getValue(), integerData);

        try {
            stringElement.setValue(null);
        } catch (Exception e) {
            fail("Question Data Element did not allow for its value to be set as null");
        }

        assertEquals("Question Data Element did not return a null value correctly", stringElement.getValue(), null);

    }

    private static class MutableBoolean {
        private boolean bool;

        public MutableBoolean(boolean bool) {
            this.bool = bool;
        }

        void setValue(boolean bool) {
            this.bool = bool;
        }

        boolean getValue() {
            return bool;
        }
    }

    @Test
    public void testAcceptsVisitor() {
        final MutableBoolean visitorAccepted = new MutableBoolean(false);
        final MutableBoolean dispatchedWrong = new MutableBoolean(false);
        ITreeVisitor sampleVisitor = new ITreeVisitor() {

            @Override
            public void visit(FormInstance tree) {
                dispatchedWrong.bool = true;
            }

            @Override
            public void visit(AbstractTreeElement element) {
                visitorAccepted.bool = true;
            }
        };

        stringElement.accept(sampleVisitor);
        assertTrue("The visitor's visit method was not called correctly by the QuestionDataElement", visitorAccepted.getValue());

        assertTrue("The visitor was dispatched incorrectly by the QuestionDataElement", !dispatchedWrong.getValue());
    }
}
