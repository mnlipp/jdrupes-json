package org.jdrupes.json.test;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.TreeMap;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
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

}
