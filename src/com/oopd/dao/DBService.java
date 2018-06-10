package com.oopd.dao;

import java.sql.*;



public class DBService implements DaoLayer
{
    public static Connection con;
    static
    {
      try
      {
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/fms";
        String user = "root";
        String pass = "mysql";
        con = DriverManager.getConnection(url, user, pass);
        }

      catch(Exception e)
      {
      System.out.println("Connecton Error "+e.getMessage());
      }
    }
    


    public Connection getConnection()
    {

        return con;


    }

    public PreparedStatement getPreparedStatement(String sql)
    {
     try
     {
      PreparedStatement ps = con.prepareStatement(sql);
     return ps;
     }
     catch(Exception e)
     {
     System.out.println("PreparedStatement Error : "+e.getMessage());
     return null;
     }
    }

    public ResultSet selectData(String selectQuery) 
    {
        try
        {
     Statement st = con.createStatement();
     ResultSet rs= st.executeQuery(selectQuery);
     return rs;
    }
    catch(Exception e)
    {
        System.out.println("Select Error :"+ e.getMessage() );
       return null;
    }
    }

    public int updateData(String updateQuery) {
        try
        {
          Statement st= con.createStatement();
          int ur = st.executeUpdate(updateQuery);
          return ur;        
       }
       catch(Exception e)
        {
            System.out.println("Update Error : "+ e.getMessage());
            return 0;

       }
    }

}