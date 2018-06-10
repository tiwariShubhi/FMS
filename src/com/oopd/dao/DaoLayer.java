package com.oopd.dao;

import java.sql.*;

public interface DaoLayer {


    public abstract Connection getConnection();
    public abstract PreparedStatement getPreparedStatement(String sql);
    public abstract ResultSet selectData(String selectQuery);
    public abstract int updateData(String updateQuery);
}