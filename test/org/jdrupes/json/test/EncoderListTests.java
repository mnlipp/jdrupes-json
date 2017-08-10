package org.jdrupes.json.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdrupes.json.JsonBeanEncoder;

import static org.junit.Assert.*;

import org.junit.Test;

public class EncoderListTests {

	public class Bean1 {
		private List<Bean2> items = new ArrayList<>();

		public List<Bean2> getItems() {
			return items;
		}
		
		public void setItems(List<Bean2> items) {
			this.items = items;
		}
	}
	
	public class Bean2 {
		private String name;

		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
	}
	
	@Test
	public void test() throws IOException {
		
		Bean2 item1 = new Bean2();
		item1.setName("Item1");
		Bean1 bean = new Bean1();
		bean.setItems(Arrays.asList(new Bean2[] { item1 }));
		
		String result = JsonBeanEncoder.create().writeObject(bean).toJson();
		assertEquals("{\"items\":["
				+ "{\"class\":\"org.jdrupes.json.test.EncoderListTests$Bean2\","
				+ "\"name\":\"Item1\"}]}",
				result);
	}

}
