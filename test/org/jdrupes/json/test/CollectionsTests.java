package org.jdrupes.json.test;

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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.Test;

public class CollectionsTests {

    public static class BeanWithCollectionAttributes {
        private List<Object> listOfItems;
        private Set<Object> setOfItems;

        public List<Object> getListOfItems() {
            return listOfItems;
        }

        public void setListOfItems(List<Object> listOfItems) {
            this.listOfItems = listOfItems;
        }

        public Set<Object> getSetOfItems() {
            return setOfItems;
        }

        public void setSetOfItems(Set<Object> setOfItems) {
            this.setOfItems = setOfItems;
        }
    }

    @Test
    public void testWrite() throws IOException {
        BeanWithCollectionAttributes bean = new BeanWithCollectionAttributes();
        bean.setListOfItems(Arrays.asList(1, 2, 3));
        bean.setSetOfItems(new HashSet<>(Arrays.asList(4, 5, 6)));

        String result = JsonBeanEncoder.create().writeObject(bean).toJson();
        assertEquals("{\"listOfItems\":[1,2,3],\"setOfItems\":[4,5,6]}",
            result);
    }

    @Test
    public void testRead() throws JsonDecodeException {
        String json = "{\"listOfItems\":[1,2,3],\"setOfItems\":[4,5,6]}";
        BeanWithCollectionAttributes result = JsonBeanDecoder.create(json)
            .readObject(BeanWithCollectionAttributes.class);
        assertEquals(3, result.getListOfItems().size());
        assertTrue(result.getListOfItems().stream()
            .anyMatch(e -> ((Number) e).intValue() == 1));
        assertTrue(result.getListOfItems().stream()
            .anyMatch(e -> ((Number) e).intValue() == 2));
        assertTrue(result.getListOfItems().stream()
            .anyMatch(e -> ((Number) e).intValue() == 3));
        assertEquals(3, result.getSetOfItems().size());
        assertTrue(result.getSetOfItems().stream()
            .anyMatch(e -> ((Number) e).intValue() == 4));
        assertTrue(result.getSetOfItems().stream()
            .anyMatch(e -> ((Number) e).intValue() == 5));
        assertTrue(result.getSetOfItems().stream()
            .anyMatch(e -> ((Number) e).intValue() == 6));
    }
}
