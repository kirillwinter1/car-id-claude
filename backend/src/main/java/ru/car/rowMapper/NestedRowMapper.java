package ru.car.rowMapper;

import org.springframework.beans.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;

public class NestedRowMapper<T> implements RowMapper<T> {

    private Class<T> mappedClass;
    private String name;

    public NestedRowMapper(Class<T> mappedClass, String name) {
        this.mappedClass = mappedClass;
        this.name = name;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {

        T mappedObject = BeanUtils.instantiate(this.mappedClass);
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);

        bw.setAutoGrowNestedPaths(true);

        ResultSetMetaData meta_data = rs.getMetaData();
        int columnCount = meta_data.getColumnCount();

        boolean exists = false;

        for (int index = 1; index <= columnCount; index++) {

            try {
                String column = JdbcUtils.lookupColumnName(meta_data, index);
                if (!column.startsWith(name)) {
                    continue;
                }
                column = column.replace(name + ".", "");
                Object value = JdbcUtils.getResultSetValue(rs, index, Class.forName(meta_data.getColumnClassName(index)));

                bw.setPropertyValue(column, value);
                if (Objects.nonNull(value)) {
                    exists = true;
                }
            } catch (TypeMismatchException | NotWritablePropertyException | ClassNotFoundException e) {
                // Ignore
            }
        }

        return exists ? mappedObject : null;
    }
}