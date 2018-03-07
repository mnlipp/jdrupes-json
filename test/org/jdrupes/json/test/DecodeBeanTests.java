/*
 * This file is part of the JDrupes JSON utilities project.
 * Copyright (C) 2017, 2018  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jdrupes.json.test;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonDecodeException;
import org.jdrupes.json.JsonObject;

import static org.junit.Assert.*;

import org.junit.Test;

public class DecodeBeanTests {

	public static class Person {

		private String name;
		private int age;
		private PhoneNumber[] numbers;

		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public int getAge() {
			return age;
		}
		
		public void setAge(int age) {
			this.age = age;
		}
		
		public PhoneNumber[] getNumbers() {
			return numbers;
		}
		
		public void setNumbers(PhoneNumber[] numbers) {
			this.numbers = numbers;
		}
	}

	public static class PhoneNumber {
		private String name;
		private String number;

		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getNumber() {
			return number;
		}
		
		public void setNumber(String number) {
			this.number = number;
		}
	}

	public static class SpecialNumber extends PhoneNumber {
	}

	@Test
	public void readBean() throws JsonDecodeException {
		String json = "{\"age\":42,\"name\":\"Simon Sample\",\""
				+ "numbers\":[{\"name\":\"Home\",\"number\":\"06751 51 56 57\"},"
				+ "{\"class\":\"SpecialNumber\",\"name\":\"Work\",\"number\":\"030 77 35 44\"}]}";
		Person result = JsonBeanDecoder.create(json)
				.addAlias(SpecialNumber.class, "SpecialNumber")
				.readObject(Person.class);
		assertNotEquals(null, result);
		assertEquals(result.age, 42);
		assertEquals(result.numbers.length, 2);
		assertTrue(result.numbers[0] instanceof PhoneNumber);
		assertTrue(result.numbers[1] instanceof SpecialNumber);
		assertEquals("Home", result.numbers[0].name);
	}

	@Test
	public void readAsMap() throws JsonDecodeException {
		String json = "{\"age\":42,\"name\":\"Simon Sample\",\""
				+ "numbers\":[{\"name\":\"Home\",\"number\":\"06751 51 56 57\"},"
				+ "{\"class\":\"SpecialNumber\",\"name\":\"Work\",\"number\":\"030 77 35 44\"}]}";
		JsonObject result = JsonBeanDecoder.create(json)
				.addAlias(SpecialNumber.class, "SpecialNumber")
				.readObject();
		assertNotEquals(null, result);
		assertEquals(42L, result.get("age"));
		assertEquals(((List<?>)result.get("numbers")).size(), 2);
		assertTrue(((List<?>)result.get("numbers")).get(0) instanceof Map);
		assertTrue(((List<?>)result.get("numbers")).get(1) instanceof SpecialNumber);
	}

	public static class RoBean {
		
		private int value = 0;
		
		public RoBean() {
		}

		public RoBean(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	@Test
	public void testRoBean() throws JsonDecodeException {
		String json = "{ \"value\": 42 }";
		RoBean result = JsonBeanDecoder.create(json)
			.readObject(RoBean.class);
		assertEquals(42, result.getValue());
	}
	
	public static class ImmutablePoint {
		
		private int posX = 0;
		private int posY = 0;
		private String name = null;
		public boolean xyConstCalled = false;
		public boolean allConstCalled = false;

		// Not to be picked
		@ConstructorProperties({"name", "x", "y"})
		public ImmutablePoint(String name, int posX, int posY) {
			this(posX, posY);
			this.name = name;
			allConstCalled = true;
		}

		@ConstructorProperties({"x", "y"})
		public ImmutablePoint(int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
			xyConstCalled = true;
		}

		public String getName() {
			return name;
		}
		
		public int getX() {
			return posX;
		}

		public int getY() {
			return posY;
		}
		
	}

	@Test
	public void testImmutablePoint() throws JsonDecodeException {
		String json = "{ \"x\": 1, \"y\": 2 }";
		ImmutablePoint result = JsonBeanDecoder.create(json)
			.readObject(ImmutablePoint.class);
		assertTrue(result.xyConstCalled);
		assertFalse(result.allConstCalled);
		assertEquals(1, result.getX());
		assertEquals(2, result.getY());
		assertNull(result.getName());
		
		json = "{ \"name\": \"point\", \"x\": 1, \"y\": 2 }";
		result = JsonBeanDecoder.create(json)
			.readObject(ImmutablePoint.class);
		assertEquals(1, result.getX());
		assertEquals(2, result.getY());
		assertEquals("point", result.getName());
		assertTrue(result.allConstCalled);
	}
	
}
