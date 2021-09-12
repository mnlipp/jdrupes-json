package org.jdrupes.json.test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonObject;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MBeanTests {

    public static class Item {

        private int prop1;
        private Integer prop2;
        private long[] prop3;
        private Long[] prop4;

        public Item(int prop1, int prop2, long[] prop3, Long[] prop4) {
            super();
            this.prop1 = prop1;
            this.prop2 = prop2;
            this.prop3 = prop3;
            this.prop4 = prop4;
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

        public long[] getProp3() {
            return prop3;
        }

        public Long[] getProp4() {
            return prop4;
        }

    }

    public static interface TestMXBean {
        int getInt();

        Integer getInteger();

        Date getDate();

        Item getItem();

        Item[] getItemArray();

        List<Item> getItemList();
    }

    public static class TestMXBeanImpl implements TestMXBean {

        @Override
        public int getInt() {
            return 42;
        }

        @Override
        public Integer getInteger() {
            return 42;
        }

        @Override
        public Date getDate() {
            return new Date(1);
        }

        @Override
        public Item getItem() {
            return new Item(3, 4, new long[] { 1, 2 },
                new Long[] { Long.valueOf(3), Long.valueOf(4) });
        }

        @Override
        public Item[] getItemArray() {
            return new Item[] {
                new Item(1, 2, new long[] { 1, 2 },
                    new Long[] { Long.valueOf(3), Long.valueOf(4) }),
                new Item(3, 4, new long[] { 1, 2 },
                    new Long[] { Long.valueOf(3), Long.valueOf(4) })
            };
        }

        @Override
        public List<Item> getItemList() {
            return Arrays.asList(getItemArray());
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
        String result = JsonBeanEncoder.create().writeObject(data).toJson();
        String expected = ""
            + "{"
            + "  \"Date\": \"1970-01-01T00:00:00Z\","
            + "  \"Int\": 42,"
            + "  \"Integer\": 42,"
            + "  \"Item\": {"
            + "    \"class\": {"
            + "      \"type\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"keys\": {"
            + "        \"prop1\": {"
            + "          \"type\": \"java.lang.Integer\""
            + "        },"
            + "        \"prop2\": {"
            + "          \"type\": \"java.lang.Integer\""
            + "        },"
            + "        \"prop3\": {"
            + "          \"type\": {"
            + "            \"type\": \"long[]\","
            + "            \"elementType\": \"long\","
            + "            \"dimension\": 1,"
            + "            \"description\": \"1-dimension array of long\""
            + "          }"
            + "        },"
            + "        \"prop4\": {"
            + "          \"type\": {"
            + "            \"type\": \"java.lang.Long[]\","
            + "            \"elementType\": \"java.lang.Long\","
            + "            \"dimension\": 1,"
            + "            \"description\": \"1-dimension array of java.lang.Long\""
            + "          }"
            + "        }"
            + "      }"
            + "    },"
            + "    \"prop1\": 3,"
            + "    \"prop2\": 4,"
            + "    \"prop3\": ["
            + "      1,"
            + "      2"
            + "    ],"
            + "    \"prop4\": ["
            + "      3,"
            + "      4"
            + "    ]"
            + "  },"
            + "  \"ItemArray\": ["
            + "    {"
            + "      \"class\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"prop1\": 1,"
            + "      \"prop2\": 2,"
            + "      \"prop3\": ["
            + "        1,"
            + "        2"
            + "      ],"
            + "      \"prop4\": ["
            + "        3,"
            + "        4"
            + "      ]"
            + "    },"
            + "    {"
            + "      \"class\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"prop1\": 3,"
            + "      \"prop2\": 4,"
            + "      \"prop3\": ["
            + "        1,"
            + "        2"
            + "      ],"
            + "      \"prop4\": ["
            + "        3,"
            + "        4"
            + "      ]"
            + "    }"
            + "  ],"
            + "  \"ItemList\": ["
            + "    {"
            + "      \"class\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"prop1\": 1,"
            + "      \"prop2\": 2,"
            + "      \"prop3\": ["
            + "        1,"
            + "        2"
            + "      ],"
            + "      \"prop4\": ["
            + "        3,"
            + "        4"
            + "      ]"
            + "    },"
            + "    {"
            + "      \"class\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"prop1\": 3,"
            + "      \"prop2\": 4,"
            + "      \"prop3\": ["
            + "        1,"
            + "        2"
            + "      ],"
            + "      \"prop4\": ["
            + "        3,"
            + "        4"
            + "      ]"
            + "    }"
            + "  ]"
            + "}";
        assertEquals(expected.replaceAll("[ \n]", ""),
            result.replaceAll("[ \n]", ""));
    }

    @Test
    void testCompositeDataFromJson() throws Exception {
        String json = ""
            + "{"
            + "  \"Date\": \"1970-01-01T00:00:00Z\","
            + "  \"Int\": 42,"
            + "  \"Integer\": 42,"
            + "  \"Item\": {"
            + "    \"class\": {"
            + "      \"type\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"keys\": {"
            + "        \"prop1\": {"
            + "          \"type\": \"java.lang.Integer\""
            + "        },"
            + "        \"prop2\": {"
            + "          \"type\": \"java.lang.Integer\""
            + "        },"
            + "        \"prop3\": {"
            + "          \"type\": {"
            + "            \"type\": \"long[]\","
            + "            \"elementType\": \"long\","
            + "            \"dimension\": 1,"
            + "            \"description\": \"1-dimension array of long\""
            + "          }"
            + "        },"
            + "        \"prop4\": {"
            + "          \"type\": {"
            + "            \"type\": \"java.lang.Long[]\","
            + "            \"elementType\": \"java.lang.Long\","
            + "            \"dimension\": 1,"
            + "            \"description\": \"1-dimension array of java.lang.Long\""
            + "          }"
            + "        }"
            + "      }"
            + "    },"
            + "    \"prop1\": 3,"
            + "    \"prop2\": 4,"
            + "    \"prop3\": ["
            + "      1,"
            + "      2"
            + "    ],"
            + "    \"prop4\": ["
            + "      3,"
            + "      4"
            + "    ]"
            + "  },"
            + "  \"ItemArray\": ["
            + "    {"
            + "      \"class\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"prop1\": 1,"
            + "      \"prop2\": 2,"
            + "      \"prop3\": ["
            + "        1,"
            + "        2"
            + "      ],"
            + "      \"prop4\": ["
            + "        3,"
            + "        4"
            + "      ]"
            + "    },"
            + "    {"
            + "      \"class\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"prop1\": 3,"
            + "      \"prop2\": 4,"
            + "      \"prop3\": ["
            + "        1,"
            + "        2"
            + "      ],"
            + "      \"prop4\": ["
            + "        3,"
            + "        4"
            + "      ]"
            + "    }"
            + "  ],"
            + "  \"ItemList\": ["
            + "    {"
            + "      \"class\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"prop1\": 1,"
            + "      \"prop2\": 2,"
            + "      \"prop3\": ["
            + "        1,"
            + "        2"
            + "      ],"
            + "      \"prop4\": ["
            + "        3,"
            + "        4"
            + "      ]"
            + "    },"
            + "    {"
            + "      \"class\": \"org.jdrupes.json.test.MBeanTests$Item\","
            + "      \"prop1\": 3,"
            + "      \"prop2\": 4,"
            + "      \"prop3\": ["
            + "        1,"
            + "        2"
            + "      ],"
            + "      \"prop4\": ["
            + "        3,"
            + "        4"
            + "      ]"
            + "    }"
            + "  ]"
            + "}";
        JsonObject result = JsonBeanDecoder.create(json).readObject();
        assertEquals("1970-01-01T00:00:00Z", result.get("Date"));
        assertEquals(42L, result.get("Int"));
        assertEquals(42L, result.get("Integer"));
        CompositeData item = (CompositeData) result.get("Item");
        assertEquals(3, item.get("prop1"));
        assertEquals(4, item.get("prop2"));
        assertEquals(1L, ((long[]) item.get("prop3"))[0]);
        assertEquals(2L, ((long[]) item.get("prop3"))[1]);
        assertEquals(3L, ((Long[]) item.get("prop4"))[0]);
        assertEquals(4L, ((Long[]) item.get("prop4"))[1]);
        @SuppressWarnings("unchecked")
        List<CompositeData> itemList
            = (List<CompositeData>) result.get("ItemList");
        item = itemList.get(0);
        assertEquals(1, item.get("prop1"));
        assertEquals(2, item.get("prop2"));
        assertEquals(1L, ((long[]) item.get("prop3"))[0]);
        assertEquals(2L, ((long[]) item.get("prop3"))[1]);
        assertEquals(3L, ((Long[]) item.get("prop4"))[0]);
        assertEquals(4L, ((Long[]) item.get("prop4"))[1]);
        item = itemList.get(1);
        assertEquals(3, item.get("prop1"));
        assertEquals(4, item.get("prop2"));
        assertEquals(1L, ((long[]) item.get("prop3"))[0]);
        assertEquals(2L, ((long[]) item.get("prop3"))[1]);
        assertEquals(3L, ((Long[]) item.get("prop4"))[0]);
        assertEquals(4L, ((Long[]) item.get("prop4"))[1]);
    }

    @Test
    void testTabularDataToJson() throws OpenDataException, IOException {
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
        assertEquals(
            "{\"class\":{\"type\":\"org.jdrupes.json.TestTable\","
                + "\"description\":\"Test table\","
                + "\"row\":{\"column1\":{\"type\":\"java.lang.Integer\","
                + "\"description\":\"Column 1\"},"
                + "\"column2\":{\"type\":\"java.lang.Integer\","
                + "\"description\":\"Column 2\"}},"
                + "\"indices\":[\"column1\",\"column2\"]},\"rows\":[[1,2],[3,4]]}",
            result);
    }

    @Test
    void testTabularDataFromJson() throws Exception {
        String json = ""
            + "{"
            + "  \"class\": {"
            + "    \"type\": \"org.jdrupes.json.TestTable\","
            + "    \"description\": \"Test table\","
            + "    \"row\": {"
            + "      \"column1\": {"
            + "        \"type\": \"java.lang.Integer\","
            + "        \"description\": \"Column 1\""
            + "      },"
            + "      \"column2\": {"
            + "        \"type\": \"java.lang.Integer\","
            + "        \"description\": \"Column 2\""
            + "      }"
            + "    },"
            + "    \"indices\": ["
            + "      \"column1\""
            + "    ]"
            + "  },"
            + "  \"rows\": ["
            + "    ["
            + "      1,"
            + "      2"
            + "    ],"
            + "    ["
            + "      3,"
            + "      4"
            + "    ]"
            + "  ]"
            + "}";
        TabularData data
            = JsonBeanDecoder.create(json).readObject(TabularData.class);
        assertEquals(2, data.size());
        assertEquals(2, data.get(new Integer[] { 1 }).get("column2"));
        assertEquals(4, data.get(new Integer[] { 3 }).get("column2"));
    }

}
