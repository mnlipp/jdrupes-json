package org.jdrupes.json.test;

import java.beans.ConstructorProperties;
import java.util.Date;
import javax.management.ObjectName;

public class TestJavaBean {

    private int intProperty;
    private Date dateProperty;
    private ObjectName objectNameProperty;

    @ConstructorProperties({ "intProperty", "dateProperty",
        "objectNameProperty" })
    public TestJavaBean(int intProperty, Date dateProperty,
            ObjectName objectNameProperty) {
        this.intProperty = intProperty;
        this.dateProperty = dateProperty;
        this.objectNameProperty = objectNameProperty;
    }

    public int getIntProperty() {
        return intProperty;
    }

    public void setIntProperty(int intProperty) {
        this.intProperty = intProperty;
    }

    public Date getDateProperty() {
        return dateProperty;
    }

    public void setDateProperty(Date dateProperty) {
        this.dateProperty = dateProperty;
    }

    public ObjectName getObjectNameProperty() {
        return objectNameProperty;
    }

    public void setObjectNameProperty(ObjectName objectNameProperty) {
        this.objectNameProperty = objectNameProperty;
    }

}
