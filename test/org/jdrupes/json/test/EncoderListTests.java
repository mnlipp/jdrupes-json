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
    public void testWithClass() throws IOException {

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

    @Test
    public void testWithoutClass() throws IOException {

        Bean2 item1 = new Bean2();
        item1.setName("Item1");
        Bean1 bean = new Bean1();
        bean.setItems(Arrays.asList(new Bean2[] { item1 }));

        String result
            = JsonBeanEncoder.create().omitClass().writeObject(bean).toJson();
        assertEquals("{\"items\":["
            + "{\"name\":\"Item1\"}]}",
            result);
    }

}
