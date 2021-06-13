package org.jdrupes.json.test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.jdrupes.json.JsonBeanEncoder;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MBeanTests {

    public static class Item {

        private int prop1;
        private int prop2;

        public Item(int prop1, int prop2) {
            super();
            this.prop1 = prop1;
            this.prop2 = prop2;
        }

        public int getProp1() {
            return prop1;
        }

        public void setProp1(int prop1) {
            this.prop1 = prop1;
        }

        public int getProp2() {
            return prop2;
        }

        public void setProp2(int prop2) {
            this.prop2 = prop2;
        }

    }

    public static interface TestMXBean {
        Item getItem();

        Item[] getItems();
    }

    public static class TestMXBeanImpl implements TestMXBean {

        @Override
        public Item getItem() {
            return new Item(3, 4);
        }

        @Override
        public Item[] getItems() {
            return new Item[] {
                new Item(1, 2),
                new Item(3, 4)
            };
        }

    }

    @Test
    void testToJson() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName mbeanName = new ObjectName(
            "org.jdrupes.json:type=" + TestMXBean.class.getSimpleName());
        mbs.registerMBean(new TestMXBeanImpl(), mbeanName);

        MBeanInfo info = mbs.getMBeanInfo(mbeanName);
        Map<String, Object> data = new TreeMap<>();
        for (MBeanAttributeInfo attr : info.getAttributes()) {
            data.put(attr.getName(),
                mbs.getAttribute(mbeanName, attr.getName()));
        }
        String result
            = JsonBeanEncoder.create().writeObject(data).toJson();
        assertEquals("{\"Item\":"
            + "{\"class\":\"org.jdrupes.json.test.MBeanTests$Item\","
            + "\"prop1\":3,\"prop2\":4},"
            + "\"Items\":[{\"class\":\"org.jdrupes.json.test.MBeanTests$Item\","
            + "\"prop1\":1,\"prop2\":2},{"
            + "\"class\":\"org.jdrupes.json.test.MBeanTests$Item\","
            + "\"prop1\":3,\"prop2\":4}]}",
            result);
    }

    @Test
    void testTabularData() throws OpenDataException, IOException {
        CompositeType rowType
            = new CompositeType("org.jdrupes.json.TestRow", "Test Row",
                new String[] { "column1", "column2" },
                new String[] { "Column 1", "Column 2" },
                new OpenType<?>[] { SimpleType.INTEGER,
                    SimpleType.INTEGER });
        TabularData data = new TabularDataSupport(
            new TabularType("org.jdrupes.json.TestTable", "Test table",
                rowType, new String[] { "column1", "column2" }));
        Map<String, Integer> row = new HashMap<>();
        row.put("column1", 1);
        row.put("column2", 2);
        data.put(new CompositeDataSupport(rowType, row));
        row.put("column1", 3);
        row.put("column2", 4);
        data.put(new CompositeDataSupport(rowType, row));

        String result
            = JsonBeanEncoder.create().writeObject(data).toJson();
        assertEquals("{\"class\":\"javax.management.openmbean.TabularData\","
            + "\"type\":\"org.jdrupes.json.TestTable\","
            + "\"description\":\"Test table\","
            + "\"columns\":[{\"name\":\"column1\","
            + "\"type\":\"java.lang.Integer\",\"description\":\"Column 1\"},"
            + "{\"name\":\"column2\",\"type\":\"java.lang.Integer\","
            + "\"description\":\"Column 2\"}],"
            + "\"indices\":[\"column1\",\"column2\"]"
            + ",\"rows\":[[1,2],[3,4]]}",
            result);
    }

}
