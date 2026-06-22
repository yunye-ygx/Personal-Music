package com.moodtune.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis TypeHandler for converting between List<String> and PostgreSQL TEXT[] array
 */
@MappedTypes(List.class)
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setNull(i, Types.ARRAY);
        } else {
            Connection conn = ps.getConnection();
            Array array = conn.createArrayOf("text", parameter.toArray(new String[0]));
            ps.setArray(i, array);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    private List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return new ArrayList<>();
        }
        String[] strings = (String[]) array.getArray();
        List<String> list = new ArrayList<>();
        if (strings != null) {
            for (String s : strings) {
                list.add(s);
            }
        }
        return list;
    }
}
